package com.project.ridelink.user.controller;

import com.project.ridelink.security.CustomUserDetails;
import com.project.ridelink.user.dto.UserProfileResponse;
import com.project.ridelink.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .collegeName(user.getCollege().getName())
                .collegeCity(user.getCollege().getCity())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
        return ResponseEntity.ok(response);
    }
}
