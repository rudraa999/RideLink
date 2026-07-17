package com.project.ridelink.auth.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String username;
    private String name;
    private String role;
    private String collegeName;
}
