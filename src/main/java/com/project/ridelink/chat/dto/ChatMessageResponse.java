package com.project.ridelink.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long matchId;
    private String senderUsername;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
}
