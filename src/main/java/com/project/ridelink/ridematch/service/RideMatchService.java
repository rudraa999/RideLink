package com.project.ridelink.ridematch.service;

import com.project.ridelink.ridematch.dto.RideMatchResponse;
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
public class RideMatchService {

    private final RideMatchRepository rideMatchRepository;
    private final RideMatchMemberRepository rideMatchMemberRepository;

    public RideMatchService(
            RideMatchRepository rideMatchRepository,
            RideMatchMemberRepository rideMatchMemberRepository
    ) {
        this.rideMatchRepository = rideMatchRepository;
        this.rideMatchMemberRepository = rideMatchMemberRepository;
    }

    @Transactional
    public RideMatch createMatch(User user1, User user2) {
        // Create the match container
        RideMatch match = RideMatch.builder()
                .status(MatchStatus.ACTIVE)
                .build();
        RideMatch savedMatch = rideMatchRepository.save(match);

        // Add both members
        RideMatchMember member1 = RideMatchMember.builder()
                .rideMatch(savedMatch)
                .user(user1)
                .build();
        RideMatchMember member2 = RideMatchMember.builder()
                .rideMatch(savedMatch)
                .user(user2)
                .build();

        rideMatchMemberRepository.save(member1);
        rideMatchMemberRepository.save(member2);

        return savedMatch;
    }

    @Transactional(readOnly = true)
    public RideMatchResponse getActiveMatch(User user) {
        RideMatch match = rideMatchRepository.findActiveMatchForUser(user, MatchStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active match found"));
        return mapToResponse(match);
    }

    @Transactional
    public void completeMatch(Long matchId, User user) {
        RideMatch match = getVerifiedMatch(matchId, user);
        match.setStatus(MatchStatus.COMPLETED);
        rideMatchRepository.save(match);
    }

    @Transactional
    public void cancelMatch(Long matchId, User user) {
        RideMatch match = getVerifiedMatch(matchId, user);
        match.setStatus(MatchStatus.CANCELLED);
        rideMatchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public List<RideMatchResponse> getMatchHistory(User user) {
        List<RideMatch> matches = rideMatchRepository.findMatchHistoryForUser(user, MatchStatus.ACTIVE);
        return matches.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RideMatch getVerifiedMatch(Long matchId, User user) {
        RideMatch match = rideMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        List<RideMatchMember> members = rideMatchMemberRepository.findByRideMatch(match);
        boolean isMember = members.stream().anyMatch(m -> m.getUser().getId().equals(user.getId()));

        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant in this match");
        }
        return match;
    }

    public RideMatchResponse mapToResponse(RideMatch match) {
        List<RideMatchMember> members = rideMatchMemberRepository.findByRideMatch(match);
        List<RideMatchResponse.ParticipantInfo> participants = members.stream()
                .map(m -> RideMatchResponse.ParticipantInfo.builder()
                        .username(m.getUser().getUsername())
                        .name(m.getUser().getName())
                        .collegeName(m.getUser().getCollege().getName())
                        .build())
                .collect(Collectors.toList());

        return RideMatchResponse.builder()
                .id(match.getId())
                .status(match.getStatus().name())
                .createdAt(match.getCreatedAt())
                .participants(participants)
                .build();
    }
}
