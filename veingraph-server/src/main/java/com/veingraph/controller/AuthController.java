package com.veingraph.controller;

import com.veingraph.auth.jwt.JwtTokenProvider;
import com.veingraph.auth.model.SysUser;
import com.veingraph.auth.repository.SysUserRepository;
import com.veingraph.auth.util.SecurityUtils;
import com.veingraph.common.result.Result;
import com.veingraph.controller.dto.LoginRequest;
import com.veingraph.controller.dto.RegisterRequest;
import com.veingraph.controller.vo.LoginResponse;
import com.veingraph.controller.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 认证控制器 — 本地注册/登录 + 当前用户信息
 */
@Tag(name = "认证管理", description = "用户注册、登录、获取当前用户信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserRepository sysUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 本地注册
     */
    @Operation(summary = "用户注册", description = "使用用户名和密码注册新用户账号")
    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
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

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        return Result.ok(new LoginResponse(token, user.getId(), user.getNickname(), null));
    }

    /**
     * 本地登录
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        String username = request.username();
        String password = request.password();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.fail(400, "用户名和密码不能为空");
        }

        SysUser user = sysUserRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        return Result.ok(new LoginResponse(
                token,
                user.getId(),
                user.getNickname(),
                user.getAvatar()
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