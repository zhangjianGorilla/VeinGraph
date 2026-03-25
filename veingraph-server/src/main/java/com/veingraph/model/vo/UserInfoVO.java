package com.veingraph.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户信息响应
 */
@Schema(description = "用户信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfoVO(
        @Schema(description = "用户ID")
        String userId,
        @Schema(description = "用户名")
        String username,
        @Schema(description = "昵称")
        String nickname,
        @Schema(description = "头像URL")
        String avatar,
        @Schema(description = "角色")
        String role,
        @Schema(description = "认证提供者")
        String provider
) {
}