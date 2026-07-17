package com.project.ridelink.availability.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String destination;
    private LocalDateTime departureTime;
    private String transportType;
    private boolean isActive;
    private LocalDateTime expiresAt;
}
