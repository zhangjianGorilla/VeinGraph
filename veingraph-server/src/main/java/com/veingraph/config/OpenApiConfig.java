package com.veingraph.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Knife4j) 配置
 * 配置 OAuth2 Password Grant 认证
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "OAuth2";

    @Value("${server.port:9999}")
    private int port;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, oAuth2SecurityScheme()))
                .security(List.of(new SecurityRequirement().addList(SECURITY_SCHEME_NAME)));
    }

    private Info apiInfo() {
        return new Info()
                .title("VeinGraph API")
                .description("基于 LLM 的实体关系抽取与图谱问答系统")
                .version("0.1.0-SNAPSHOT")
                .contact(new Contact()
                        .name("VeinGraph Team")
                        .url("https://github.com/veingraph"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme oAuth2SecurityScheme() {
        String tokenUrl = "http://localhost:" + port + contextPath + "/oauth2/token";

        return new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .password(new OAuthFlow()
                                .tokenUrl(tokenUrl)
                                .scopes(new Scopes()
                                        .addString("read", "读取权限")
                                        .addString("write", "写入权限"))))
                .description("OAuth2 Password Grant 认证");
    }
}