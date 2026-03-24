package com.veingraph.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户登录请求
 */
@Schema(description = "用户登录请求")
public record LoginRequest(
        @Schema(description = "用户名", example = "test")
        String username,
        @Schema(description = "密码", example = "123456")
        String password
) {
}