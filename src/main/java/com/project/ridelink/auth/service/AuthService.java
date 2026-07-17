package com.project.ridelink.auth.service;

import com.project.ridelink.auth.dto.AuthResponse;
import com.project.ridelink.auth.dto.LoginRequest;
import com.project.ridelink.auth.dto.RegisterRequest;
import com.project.ridelink.auth.entity.RefreshToken;
import com.project.ridelink.auth.repository.RefreshTokenRepository;
import com.project.ridelink.college.entity.College;
import com.project.ridelink.college.repository.CollegeRepository;
import com.project.ridelink.security.CustomUserDetails;
import com.project.ridelink.security.JwtService;
import com.project.ridelink.user.entity.Role;
import com.project.ridelink.user.entity.User;
import com.project.ridelink.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CollegeRepository collegeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${ridelink.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            CollegeRepository collegeRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.collegeRepository = collegeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
        }

        College college = collegeRepository.findById(request.getCollegeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));

        User user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .college(college)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponseResponseWrapper responseWrapper) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = createOrUpdateRefreshToken(user);
        
        responseWrapper.setCookie(refreshToken.getToken(), refreshTokenExpirationMs);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole().name())
                .collegeName(user.getCollege().getName())
                .build();
    }

    @Transactional
    public RefreshToken createOrUpdateRefreshToken(User user) {
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public AuthResponse refresh(String token, HttpServletResponseResponseWrapper responseWrapper) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        User user = refreshToken.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateToken(userDetails);

        // Rotate Refresh Token
        RefreshToken newRefreshToken = createOrUpdateRefreshToken(user);
        responseWrapper.setCookie(newRefreshToken.getToken(), refreshTokenExpirationMs);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole().name())
                .collegeName(user.getCollege().getName())
                .build();
    }

    @Transactional
    public void logout(String token, HttpServletResponseResponseWrapper responseWrapper) {
        if (token != null) {
            refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
        }
        responseWrapper.clearCookie();
    }

    // Helper interface to abstract HttpServletResponse cookie setting for cleaner service logic
    public interface HttpServletResponseResponseWrapper {
        void setCookie(String token, long maxAgeMs);
        void clearCookie();
    }
}
