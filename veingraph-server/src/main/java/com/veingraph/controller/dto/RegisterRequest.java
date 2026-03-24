package com.veingraph.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户注册请求
 */
@Schema(description = "用户注册请求")
public record RegisterRequest(
        @Schema(description = "用户名", example = "test")
        String username,
        @Schema(description = "密码", example = "123456")
        String password,
        @Schema(description = "昵称", example = "测试用户")
        String nickname
) {
}