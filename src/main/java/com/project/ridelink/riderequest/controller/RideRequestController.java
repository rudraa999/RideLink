package com.project.ridelink.riderequest.controller;

import com.project.ridelink.riderequest.dto.RideRequestResponse;
import com.project.ridelink.riderequest.service.RideRequestService;
import com.project.ridelink.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RideRequestController {

    private final RideRequestService rideRequestService;

    public RideRequestController(RideRequestService rideRequestService) {
        this.rideRequestService = rideRequestService;
    }

    @PostMapping("/send/{receiverId}")
    public ResponseEntity<RideRequestResponse> sendRequest(
            @PathVariable Long receiverId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        RideRequestResponse response = rideRequestService.sendRequest(userDetails.getUser(), receiverId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancelRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        rideRequestService.cancelRequest(userDetails.getUser(), requestId);
        return ResponseEntity.ok(Map.of("message", "Request cancelled successfully"));
    }

    @PutMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        rideRequestService.rejectRequest(userDetails.getUser(), requestId);
        return ResponseEntity.ok(Map.of("message", "Request rejected successfully"));
    }

    @PutMapping("/{requestId}/accept")
    public ResponseEntity<?> acceptRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        rideRequestService.acceptRequest(userDetails.getUser(), requestId);
        return ResponseEntity.ok(Map.of("message", "Request accepted successfully"));
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<RideRequestResponse>> getIncomingRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<RideRequestResponse> responses = rideRequestService.getIncomingRequests(userDetails.getUser());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/outgoing")
    public ResponseEntity<List<RideRequestResponse>> getOutgoingRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<RideRequestResponse> responses = rideRequestService.getOutgoingRequests(userDetails.getUser());
        return ResponseEntity.ok(responses);
    }
}
