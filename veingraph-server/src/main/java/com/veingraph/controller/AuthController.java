package com.veingraph.controller;

import com.veingraph.auth.jwt.JwtTokenProvider;
import com.veingraph.auth.model.SysUser;
import com.veingraph.auth.repository.SysUserRepository;
import com.veingraph.auth.util.SecurityUtils;
import com.veingraph.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证控制器 — 本地注册/登录 + 当前用户信息
 */
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
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");

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

        return Result.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "nickname", user.getNickname()
        ));
    }

    /**
     * 本地登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.fail(400, "用户名和密码不能为空");
        }

        SysUser user = sysUserRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        return Result.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "nickname", user.getNickname(),
                "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }

        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        return Result.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername() != null ? user.getUsername() : "",
                "nickname", user.getNickname(),
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "role", user.getRole(),
                "provider", user.getProvider()
        ));
    }
}
