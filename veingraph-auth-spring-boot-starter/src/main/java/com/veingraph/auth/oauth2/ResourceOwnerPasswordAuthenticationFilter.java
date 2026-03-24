package com.veingraph.auth.oauth2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Password Grant Token 端点过滤器
 * 处理 POST /oauth2/token?grant_type=password 请求
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceOwnerPasswordAuthenticationFilter extends OncePerRequestFilter {

    private static final String DEFAULT_TOKEN_ENDPOINT_URI = "/oauth2/token";
    private static final String PASSWORD_GRANT = "password";

    private final AuthenticationManager authenticationManager;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!isTokenRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        MultiValueMap<String, String> parameters = getParameters(request);
        String grantType = parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE);

        if (!PASSWORD_GRANT.equals(grantType)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Processing password grant token request");

        String clientId = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID);
        String clientSecret = parameters.getFirst(OAuth2ParameterNames.CLIENT_SECRET);

        // 验证客户端
        if (clientId == null || clientId.isBlank()) {
            clientId = "veingraph-web";
        }

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            sendErrorResponse(response, new OAuth2Error("invalid_client", "无效的客户端", null));
            return;
        }

        // 创建客户端认证对象
        ClientPrincipalAuthenticationToken clientPrincipal = new ClientPrincipalAuthenticationToken(
                clientId,
                clientSecret,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
        );

        try {
            // 构建认证 Token
            Map<String, Object> additionalParameters = new HashMap<>();
            parameters.forEach((key, values) -> {
                if (values.size() > 0) {
                    additionalParameters.put(key, values.get(0));
                }
            });

            ResourceOwnerPasswordAuthenticationToken authenticationToken =
                    new ResourceOwnerPasswordAuthenticationToken(
                            AuthorizationGrantType.PASSWORD,
                            clientPrincipal,
                            additionalParameters
                    );

            // 执行认证
            Authentication authenticationResult = authenticationManager.authenticate(authenticationToken);

            // 返回 Token 响应
            sendSuccessResponse(response, authenticationResult);

        } catch (OAuth2AuthenticationException ex) {
            sendErrorResponse(response, ex.getError());
        } catch (AuthenticationException ex) {
            sendErrorResponse(response, new OAuth2Error("invalid_grant", ex.getMessage(), null));
        }
    }

    private boolean isTokenRequest(HttpServletRequest request) {
        String requestURI = new UrlPathHelper().getPathWithinApplication(request);
        return DEFAULT_TOKEN_ENDPOINT_URI.equals(requestURI) &&
                HttpMethod.POST.matches(request.getMethod());
    }

    private MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((key, values) -> {
            for (String value : values) {
                parameters.add(key, value);
            }
        });
        return parameters;
    }

    private void sendSuccessResponse(HttpServletResponse response, Authentication authentication)
            throws IOException {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", authentication.getDetails());
        tokenResponse.put("token_type", "Bearer");
        tokenResponse.put("expires_in", 86400);

        // 从认证信息中提取用户信息
        if (authentication instanceof ResourceOwnerPasswordAuthenticationToken) {
            Map<String, Object> additionalParams = ((ResourceOwnerPasswordAuthenticationToken) authentication)
                    .getAdditionalParameters();
            tokenResponse.putAll(additionalParams);
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), tokenResponse);
    }

    private void sendErrorResponse(HttpServletResponse response, OAuth2Error error) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error.getErrorCode());
        errorResponse.put("error_description", error.getDescription());

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}