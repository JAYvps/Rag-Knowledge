package com.ragkb.controller;

import com.ragkb.common.Result;
import com.ragkb.dto.LoginRequest;
import com.ragkb.dto.RegisterRequest;
import com.ragkb.security.UserDetailsImpl;
import com.ragkb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return Result.ok();
    }

    /**
     * POST /api/auth/login
     *
     * Returns: { token, userId, username, role }
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    /**
     * GET /api/auth/me
     * Requires: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail(401, "未认证");
        }
        return Result.ok(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }
}
