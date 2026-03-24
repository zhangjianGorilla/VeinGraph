package com.veingraph.auth.jwt;

import com.veingraph.auth.autoconfigure.VeinGraphAuthProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具类
 */
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(VeinGraphAuthProperties properties) {
        byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        // 确保密钥长度至少 256 位
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = properties.getJwt().getExpiration();
    }

    /**
     * 签发 JWT
     */
    public String generateToken(String userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 校验 JWT 签名和过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 JWT 提取用户 ID (subject)
     */
    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 从 JWT 提取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).get("username", String.class);
    }

    /**
     * 从 JWT 提取角色
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
