package com.project.ridelink.availability.service;

import com.project.ridelink.availability.dto.AvailabilityRequest;
import com.project.ridelink.availability.dto.AvailabilityResponse;
import com.project.ridelink.availability.entity.RideAvailability;
import com.project.ridelink.availability.repository.RideAvailabilityRepository;
import com.project.ridelink.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RideAvailabilityService {

    private final RideAvailabilityRepository rideAvailabilityRepository;

    public RideAvailabilityService(RideAvailabilityRepository rideAvailabilityRepository) {
        this.rideAvailabilityRepository = rideAvailabilityRepository;
    }

    @Transactional
    public AvailabilityResponse activate(User user, AvailabilityRequest request) {
        // Deactivate previous active availabilities to ensure only one is active at a time
        rideAvailabilityRepository.deactivateAllActiveForUser(user);

        RideAvailability availability = RideAvailability.builder()
                .user(user)
                .destination(request.getDestination())
                .departureTime(request.getDepartureTime() == null ? LocalDateTime.now() : request.getDepartureTime())
                .transportType(request.getTransportType() == null ? "ANY" : request.getTransportType())
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .build();

        RideAvailability saved = rideAvailabilityRepository.save(availability);
        return mapToResponse(saved);
    }

    @Transactional
    public void deactivate(User user) {
        rideAvailabilityRepository.deactivateAllActiveForUser(user);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getMe(User user) {
        return rideAvailabilityRepository.findByUserAndIsActiveTrue(user)
                .map(this::mapToResponse)
                .orElse(AvailabilityResponse.builder()
                        .username(user.getUsername())
                        .name(user.getName())
                        .isActive(false)
                        .build());
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> findCompatible(User user) {
        RideAvailability active = rideAvailabilityRepository.findByUserAndIsActiveTrue(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "You must activate your availability before searching for matches"
                ));

        // Check if the user's availability itself has expired
        if (active.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Your availability has expired. Please activate it again."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        List<RideAvailability> matches = rideAvailabilityRepository.findCompatibleAvailabilities(
                user.getCollege().getId(),
                user.getId(),
                active.getDestination(),
                now
        );

        return matches.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AvailabilityResponse mapToResponse(RideAvailability availability) {
        return AvailabilityResponse.builder()
                .id(availability.getId())
                .userId(availability.getUser().getId())
                .username(availability.getUser().getUsername())
                .name(availability.getUser().getName())
                .destination(availability.getDestination())
                .departureTime(availability.getDepartureTime())
                .transportType(availability.getTransportType())
                .isActive(availability.isActive())
                .expiresAt(availability.getExpiresAt())
                .build();
    }
}
