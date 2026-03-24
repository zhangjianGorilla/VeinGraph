package com.veingraph.auth.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 系统用户模型
 */
@Data
@Document("sys_user")
public class SysUser {

    @Id
    private String id;

    /** 用户名（本地注册，OAuth2 用户可为空） */
    @Indexed(unique = true, sparse = true)
    private String username;

    /** BCrypt 加密密码（OAuth2 用户为 null） */
    private String password;

    /** 显示昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 角色：ROLE_USER / ROLE_ADMIN */
    private String role;

    /** 认证来源：local / github / gitee */
    private String provider;

    /** 第三方平台用户 ID */
    private String providerId;

    /** 注册时间 */
    private LocalDateTime createdAt;

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String PROVIDER_LOCAL = "local";
    public static final String PROVIDER_GITHUB = "github";
    public static final String PROVIDER_GITEE = "gitee";
}
