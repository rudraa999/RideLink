package com.project.ridelink.chat.controller;

import com.project.ridelink.chat.dto.ChatMessageRequest;
import com.project.ridelink.chat.dto.ChatMessageResponse;
import com.project.ridelink.chat.service.ChatService;
import com.project.ridelink.security.CustomUserDetails;
import com.project.ridelink.user.entity.User;
import com.project.ridelink.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(
            ChatService chatService,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/{matchId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ChatMessageResponse> history = chatService.getChatHistory(userDetails.getUser(), matchId);
        return ResponseEntity.ok(history);
    }

    @MessageMapping("/chat/{matchId}")
    public void handleChatMessage(
            @DestinationVariable Long matchId,
            @Payload ChatMessageRequest request,
            Principal principal
    ) {
        if (principal == null) {
            return;
        }

        String username = principal.getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ChatMessageResponse response = chatService.saveMessage(sender, matchId, request.getContent());

        // Broadcast to the match-specific chat topic
        messagingTemplate.convertAndSend("/topic/match/" + matchId + "/chat", response);
    }
}
