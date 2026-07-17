package com.project.ridelink.riderequest.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequestResponse {
    private Long id;
    private String senderUsername;
    private String senderName;
    private String receiverUsername;
    private String receiverName;
    private String status;
    private LocalDateTime createdAt;
    private String destination;
    private LocalDateTime departureTime;
}
