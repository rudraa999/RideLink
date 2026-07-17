package com.project.ridelink.auth.controller;

import com.project.ridelink.auth.dto.AuthResponse;
import com.project.ridelink.auth.dto.LoginRequest;
import com.project.ridelink.auth.dto.RegisterRequest;
import com.project.ridelink.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request, new HttpServletResponseWrapperImpl(response));
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is missing");
        }
        AuthResponse authResponse = authService.refresh(refreshToken, new HttpServletResponseWrapperImpl(response));
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken, new HttpServletResponseWrapperImpl(response));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private static class HttpServletResponseWrapperImpl implements AuthService.HttpServletResponseResponseWrapper {
        private final HttpServletResponse response;

        public HttpServletResponseWrapperImpl(HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public void setCookie(String token, long maxAgeMs) {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .path("/api/auth")
                    .maxAge(maxAgeMs / 1000)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        @Override
        public void clearCookie() {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(false)
                    .path("/api/auth")
                    .maxAge(0)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }
}
