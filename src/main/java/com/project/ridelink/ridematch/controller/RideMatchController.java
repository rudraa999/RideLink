package com.project.ridelink.ridematch.controller;

import com.project.ridelink.ridematch.dto.RideMatchResponse;
import com.project.ridelink.ridematch.service.RideMatchService;
import com.project.ridelink.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
public class RideMatchController {

    private final RideMatchService rideMatchService;

    public RideMatchController(RideMatchService rideMatchService) {
        this.rideMatchService = rideMatchService;
    }

    @GetMapping("/active")
    public ResponseEntity<RideMatchResponse> getActiveMatch(@AuthenticationPrincipal CustomUserDetails userDetails) {
        RideMatchResponse response = rideMatchService.getActiveMatch(userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{matchId}/complete")
    public ResponseEntity<?> completeMatch(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        rideMatchService.completeMatch(matchId, userDetails.getUser());
        return ResponseEntity.ok(Map.of("message", "Match completed successfully"));
    }

    @PutMapping("/{matchId}/cancel")
    public ResponseEntity<?> cancelMatch(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        rideMatchService.cancelMatch(matchId, userDetails.getUser());
        return ResponseEntity.ok(Map.of("message", "Match cancelled successfully"));
    }

    @GetMapping("/history")
    public ResponseEntity<List<RideMatchResponse>> getMatchHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RideMatchResponse> responses = rideMatchService.getMatchHistory(userDetails.getUser());
        return ResponseEntity.ok(responses);
    }
}
