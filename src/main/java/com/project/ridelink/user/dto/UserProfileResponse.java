package com.project.ridelink.user.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String collegeName;
    private String collegeCity;
    private String role;
    private LocalDateTime createdAt;
}
