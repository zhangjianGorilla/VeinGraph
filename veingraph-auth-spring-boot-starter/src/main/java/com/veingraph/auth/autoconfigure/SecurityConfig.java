package com.veingraph.auth.autoconfigure;

import com.veingraph.auth.filter.JwtAuthenticationFilter;
import com.veingraph.auth.jwt.JwtTokenProvider;
import com.veingraph.auth.oauth2.AuthorizationServerConfig;
import com.veingraph.auth.oauth2.ResourceOwnerPasswordAuthenticationFilter;
import com.veingraph.auth.oauth2.ResourceOwnerPasswordAuthenticationProvider;
import com.veingraph.auth.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * VeinGraph 认证自动配置
 * <p>
 * 提供 JWT 基础设施、Spring Authorization Server 集成和 Password Grant 支持。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "veingraph.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VeinGraphAuthProperties.class)
@EnableMongoRepositories(basePackages = "com.veingraph.auth.repository")
@Import(AuthorizationServerConfig.class)
public class SecurityConfig {

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(VeinGraphAuthProperties properties) {
        return new JwtTokenProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public ResourceOwnerPasswordAuthenticationProvider resourceOwnerPasswordAuthenticationProvider(
            UserDetailsService userDetailsService,
            SysUserRepository sysUserRepository,
            PasswordEncoder passwordEncoder,
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            JwtTokenProvider jwtTokenProvider) {
        return new ResourceOwnerPasswordAuthenticationProvider(
                userDetailsService,
                sysUserRepository,
                passwordEncoder,
                registeredClientRepository,
                authorizationService,
                tokenGenerator,
                jwtTokenProvider
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config,
            ResourceOwnerPasswordAuthenticationProvider passwordAuthProvider) throws Exception {
        // 创建 ProviderManager，包含我们的 Password Grant Provider
        List<AuthenticationProvider> providers = new ArrayList<>();
        providers.add(passwordAuthProvider);
        // 添加默认的 DaoAuthenticationProvider（用于其他认证方式）
        try {
            AuthenticationManager defaultManager = config.getAuthenticationManager();
            if (defaultManager instanceof ProviderManager) {
                providers.addAll(((ProviderManager) defaultManager).getProviders());
            }
        } catch (Exception ignored) {
        }
        return new ProviderManager(providers);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            VeinGraphAuthProperties properties,
            AuthenticationManager authenticationManager,
            @Autowired(required = false) AuthenticationSuccessHandler oAuth2SuccessHandler) throws Exception {

        // 组装公开路径
        List<String> publicPaths = new ArrayList<>(Arrays.asList(
                "/auth/**",
                "/oauth2/**",
                "/login/oauth2/**",
                "/actuator/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/doc.html",
                "/webjars/**"
        ));
        publicPaths.addAll(properties.getPublicPaths());
        String[] publicPathArray = publicPaths.toArray(new String[0]);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(publicPathArray).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            )
            // JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 仅当使用方提供了 OAuth2 成功处理器时，才启用 OAuth2 登录
        if (oAuth2SuccessHandler != null) {
            http.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler));
        }

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource(VeinGraphAuthProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                properties.getFrontendRedirectUrl(),
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}