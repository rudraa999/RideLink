package com.project.ridelink.chat.service;

import com.project.ridelink.chat.dto.ChatMessageResponse;
import com.project.ridelink.chat.entity.ChatMessage;
import com.project.ridelink.chat.repository.ChatMessageRepository;
import com.project.ridelink.ridematch.entity.MatchStatus;
import com.project.ridelink.ridematch.entity.RideMatch;
import com.project.ridelink.ridematch.entity.RideMatchMember;
import com.project.ridelink.ridematch.repository.RideMatchMemberRepository;
import com.project.ridelink.ridematch.repository.RideMatchRepository;
import com.project.ridelink.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final RideMatchRepository rideMatchRepository;
    private final RideMatchMemberRepository rideMatchMemberRepository;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            RideMatchRepository rideMatchRepository,
            RideMatchMemberRepository rideMatchMemberRepository
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.rideMatchRepository = rideMatchRepository;
        this.rideMatchMemberRepository = rideMatchMemberRepository;
    }

    @Transactional
    public ChatMessageResponse saveMessage(User sender, Long matchId, String content) {
        RideMatch match = rideMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride match not found"));

        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot chat on an inactive match session");
        }

        // Verify membership
        List<RideMatchMember> members = rideMatchMemberRepository.findByRideMatch(match);
        boolean isMember = members.stream().anyMatch(m -> m.getUser().getId().equals(sender.getId()));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to send messages to this match session");
        }

        ChatMessage message = ChatMessage.builder()
                .rideMatch(match)
                .sender(sender)
                .content(content)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(User user, Long matchId) {
        RideMatch match = rideMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride match not found"));

        // Verify membership
        List<RideMatchMember> members = rideMatchMemberRepository.findByRideMatch(match);
        boolean isMember = members.stream().anyMatch(m -> m.getUser().getId().equals(user.getId()));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this match chat history");
        }

        List<ChatMessage> messages = chatMessageRepository.findByRideMatchOrderBySentAtAsc(match);
        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ChatMessageResponse mapToResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .matchId(msg.getRideMatch().getId())
                .senderUsername(msg.getSender().getUsername())
                .senderName(msg.getSender().getName())
                .content(msg.getContent())
                .sentAt(msg.getSentAt())
                .build();
    }
}
