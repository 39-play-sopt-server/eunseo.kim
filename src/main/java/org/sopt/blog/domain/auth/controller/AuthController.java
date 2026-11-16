package org.sopt.blog.domain.auth.controller;

import org.sopt.blog.domain.auth.controller.dto.request.LoginRequest;
import org.sopt.blog.domain.auth.controller.dto.request.SignupRequest;
import org.sopt.blog.domain.auth.controller.dto.response.LoginResponse;
import org.sopt.blog.domain.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request.name(), request.email(), request.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String accessToken = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new LoginResponse(accessToken));
    }
}
