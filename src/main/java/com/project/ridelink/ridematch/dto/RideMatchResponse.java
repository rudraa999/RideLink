package com.project.ridelink.ridematch.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideMatchResponse {
    private Long id;
    private String status;
    private LocalDateTime createdAt;
    private List<ParticipantInfo> participants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantInfo {
        private String username;
        private String name;
        private String collegeName;
    }
}
