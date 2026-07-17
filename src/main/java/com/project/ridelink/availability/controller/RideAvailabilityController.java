package com.project.ridelink.availability.controller;

import com.project.ridelink.availability.dto.AvailabilityRequest;
import com.project.ridelink.availability.dto.AvailabilityResponse;
import com.project.ridelink.availability.service.RideAvailabilityService;
import com.project.ridelink.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/availabilities")
public class RideAvailabilityController {

    private final RideAvailabilityService rideAvailabilityService;

    public RideAvailabilityController(RideAvailabilityService rideAvailabilityService) {
        this.rideAvailabilityService = rideAvailabilityService;
    }

    @PostMapping
    public ResponseEntity<AvailabilityResponse> activate(
            @Valid @RequestBody AvailabilityRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AvailabilityResponse response = rideAvailabilityService.activate(userDetails.getUser(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/deactivate")
    public ResponseEntity<?> deactivate(@AuthenticationPrincipal CustomUserDetails userDetails) {
        rideAvailabilityService.deactivate(userDetails.getUser());
        return ResponseEntity.ok(Map.of("message", "Availability deactivated successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<AvailabilityResponse> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        AvailabilityResponse response = rideAvailabilityService.getMe(userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/compatible")
    public ResponseEntity<List<AvailabilityResponse>> getCompatible(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<AvailabilityResponse> responses = rideAvailabilityService.findCompatible(userDetails.getUser());
        return ResponseEntity.ok(responses);
    }
}
