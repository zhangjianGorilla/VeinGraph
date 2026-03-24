package com.veingraph.auth.oauth2;

import com.veingraph.auth.jwt.JwtTokenProvider;
import com.veingraph.auth.model.SysUser;
import com.veingraph.auth.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

/**
 * Resource Owner Password Credentials Grant 认证提供者
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceOwnerPasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ResourceOwnerPasswordAuthenticationToken passwordToken = (ResourceOwnerPasswordAuthenticationToken) authentication;

        String username = passwordToken.getUsername();
        String password = passwordToken.getPassword();

        if (username == null || password == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"), "用户名和密码不能为空");
        }

        // 1. 加载用户
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant"), "用户名或密码错误");
        }

        // 2. 验证密码
        SysUser user = sysUserRepository.findByUsername(username).orElse(null);
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_grant"), "用户名或密码错误");
        }

        // 3. 获取客户端
        String clientId = getClientId(passwordToken);
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_client"), "无效的客户端");
        }

        // 4. 使用自定义 JwtTokenProvider 生成 Token（保持与现有系统兼容）
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(24, ChronoUnit.HOURS);

        // 5. 构建 OAuth2AccessToken
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                token,
                issuedAt,
                expiresAt,
                Set.of("read", "write")
        );

        // 6. 创建 OAuth2Authorization 并保存
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(user.getId())
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .attribute("username", user.getUsername())
                .attribute("role", user.getRole());

        authorizationBuilder.accessToken(accessToken);
        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        log.info("Password Grant 登录成功: username={}, userId={}", username, user.getId());

        // 7. 返回认证结果
        return new ResourceOwnerPasswordAuthenticationToken(
                AuthorizationGrantType.PASSWORD,
                authentication,
                Map.of(
                        "access_token", token,
                        "token_type", "Bearer",
                        "expires_in", 86400L,
                        "userId", user.getId(),
                        "nickname", user.getNickname() != null ? user.getNickname() : ""
                )
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ResourceOwnerPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String getClientId(ResourceOwnerPasswordAuthenticationToken token) {
        // 从 Token 或请求参数中获取 client_id
        if (token.getClientId() != null) {
            return token.getClientId();
        }
        // 默认客户端
        return "veingraph-web";
    }
}