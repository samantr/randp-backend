package com.app.controller;

import com.app.dto.common.ErrorResponse;
import com.app.repository.AdminUserRepository;
import com.app.security.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AdminUserRepository userRepo;

    public AuthController(JwtUtil jwtUtil, PasswordEncoder passwordEncoder, AdminUserRepository userRepo) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
    }

    @PostMapping(
            value = "/login",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {

        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("AUTH_FAILED", "نام کاربری یا رمز عبور اشتباه است."));
        }

        var user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("AUTH_FAILED", "نام کاربری یا رمز عبور اشتباه است."));
        }

        String token = jwtUtil.generateToken(username);

        // JSON استاندارد برای فرانت
        return ResponseEntity.ok(Map.of("token", token));
    }
}
