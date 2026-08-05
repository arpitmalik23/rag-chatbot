package com.ragbot.controller;

import com.ragbot.model.AuthRequest;
import com.ragbot.model.AuthResponse;
import com.ragbot.service.UserService;
import com.ragbot.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        boolean created = userService.register(request.getUsername(), request.getPassword());
        if (!created) {
            return ResponseEntity.status(409).body(Map.of("error", "username_taken", "message", "That username is already registered."));
        }
        String token = jwtUtil.generateToken(request.getUsername().toLowerCase());
        return ResponseEntity.ok(new AuthResponse(token, request.getUsername().toLowerCase()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        boolean valid = userService.verifyLogin(request.getUsername(), request.getPassword());
        if (!valid) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials", "message", "Incorrect username or password."));
        }
        String token = jwtUtil.generateToken(request.getUsername().toLowerCase());
        return ResponseEntity.ok(new AuthResponse(token, request.getUsername().toLowerCase()));
    }
}