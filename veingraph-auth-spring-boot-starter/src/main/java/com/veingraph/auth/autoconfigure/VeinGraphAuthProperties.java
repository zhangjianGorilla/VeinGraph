package com.veingraph.auth.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * VeinGraph 认证配置属性
 */
@Data
@ConfigurationProperties(prefix = "veingraph.auth")
public class VeinGraphAuthProperties {

    /** 是否启用认证 */
    private boolean enabled = true;

    /** JWT 配置 */
    private Jwt jwt = new Jwt();

    /** OAuth2 登录成功后重定向的前端地址 */
    private String frontendRedirectUrl = "http://localhost:5173";

    /** 额外的公开路径（无需认证） */
    private List<String> publicPaths = new ArrayList<>();

    @Data
    public static class Jwt {
        /** JWT 签名密钥 */
        private String secret = "veingraph-default-secret-key-please-change-in-production";
        /** JWT 过期时间（毫秒），默认 24 小时 */
        private long expiration = 86400000;
    }
}
