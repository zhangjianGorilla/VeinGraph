package com.veingraph.controller;

import com.veingraph.auth.model.SysUser;
import com.veingraph.auth.repository.SysUserRepository;
import com.veingraph.auth.util.SecurityUtils;
import com.veingraph.common.result.Result;
import com.veingraph.model.dto.RegisterRequest;
import com.veingraph.model.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 认证控制器 — 本地注册 + 当前用户信息
 * 登录请使用 OAuth2 Password Grant: POST /oauth2/token
 */
@Tag(name = "认证管理", description = "用户注册、获取当前用户信息（登录请使用 /oauth2/token）")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 本地注册
     */
    @Operation(summary = "用户注册", description = "使用用户名和密码注册新用户账号，注册后需通过 OAuth2 登录")
    @PostMapping("/register")
    public Result<UserInfoVO> register(@RequestBody RegisterRequest request) {
        String username = request.username();
        String password = request.password();
        String nickname = request.nickname();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.fail(400, "用户名和密码不能为空");
        }

        if (sysUserRepository.existsByUsername(username)) {
            return Result.fail(400, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        user.setRole(SysUser.ROLE_USER);
        user.setProvider(SysUser.PROVIDER_LOCAL);
        user.setCreatedAt(LocalDateTime.now());
        user = sysUserRepository.save(user);

        return Result.ok(new UserInfoVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                null,
                user.getRole(),
                user.getProvider()
        ));
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户", description = "根据 JWT Token 获取当前登录用户的详细信息")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }

        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        return Result.ok(new UserInfoVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getRole(),
                user.getProvider()
        ));
    }
}