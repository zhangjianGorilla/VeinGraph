package com.veingraph.handler;

import com.veingraph.auth.jwt.JwtTokenProvider;
import com.veingraph.auth.model.SysUser;
import com.veingraph.auth.repository.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * OAuth2 社交登录成功后的回调处理器
 * 自动创建/更新用户 → 签发 JWT → 重定向前端
 * <p>
 * 跳转地址由业务应用自己配置（veingraph.auth.frontend-redirect-url），
 * 前端收到 token 后自行决定跳转到哪个页面。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SysUserRepository sysUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${veingraph.auth.frontend-redirect-url:http://localhost:5173}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = String.valueOf(attributes.get("id"));
        String login = (String) attributes.get("login");
        String rawNickname = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("avatar_url");

        final String nickname = (rawNickname == null || rawNickname.isBlank()) ? login : rawNickname;
        final String avatar = avatarUrl;

        log.info("OAuth2 登录成功: provider={}, login={}, providerId={}", provider, login, providerId);

        // 查找或创建用户
        SysUser user = sysUserRepository.findByProviderAndProviderId(provider, providerId)
                .map(existing -> {
                    existing.setNickname(nickname);
                    existing.setAvatar(avatar);
                    return sysUserRepository.save(existing);
                })
                .orElseGet(() -> {
                    SysUser newUser = new SysUser();
                    newUser.setNickname(nickname);
                    newUser.setAvatar(avatar);
                    newUser.setRole(SysUser.ROLE_USER);
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setCreatedAt(LocalDateTime.now());
                    return sysUserRepository.save(newUser);
                });

        // 签发 JWT
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getNickname(),
                user.getRole()
        );

        // 重定向到前端 — 前端自行决定落地页面和后续跳转逻辑
        String redirectUrl = frontendRedirectUrl
                + "/oauth/callback?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }
}
