package com.veingraph.controller.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应
 */
@Schema(description = "登录响应")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        @Schema(description = "JWT Token")
        String token,
        @Schema(description = "用户ID")
        String userId,
        @Schema(description = "昵称")
        String nickname,
        @Schema(description = "头像URL")
        String avatar
) {
}