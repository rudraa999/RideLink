package com.project.ridelink.riderequest.service;

import com.project.ridelink.availability.entity.RideAvailability;
import com.project.ridelink.availability.repository.RideAvailabilityRepository;
import com.project.ridelink.ridematch.service.RideMatchService;
import com.project.ridelink.riderequest.dto.RideRequestResponse;
import com.project.ridelink.riderequest.entity.RequestStatus;
import com.project.ridelink.riderequest.entity.RideRequest;
import com.project.ridelink.riderequest.repository.RideRequestRepository;
import com.project.ridelink.user.entity.User;
import com.project.ridelink.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RideRequestService {

    private final RideRequestRepository rideRequestRepository;
    private final RideAvailabilityRepository rideAvailabilityRepository;
    private final UserRepository userRepository;
    private final RideMatchService rideMatchService;

    public RideRequestService(
            RideRequestRepository rideRequestRepository,
            RideAvailabilityRepository rideAvailabilityRepository,
            UserRepository userRepository,
            RideMatchService rideMatchService
    ) {
        this.rideRequestRepository = rideRequestRepository;
        this.rideAvailabilityRepository = rideAvailabilityRepository;
        this.userRepository = userRepository;
        this.rideMatchService = rideMatchService;
    }

    @Transactional
    public RideRequestResponse sendRequest(User sender, Long receiverId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver student not found"));

        if (sender.getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a ride request to yourself");
        }

        // Verify sender availability is active
        rideAvailabilityRepository.findByUserAndIsActiveTrue(sender)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must have active availability to send requests"));

        // Verify receiver availability is active
        rideAvailabilityRepository.findByUserAndIsActiveTrue(receiver)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receiver student is not currently available"));

        // Verify no pending request already exists
        rideRequestRepository.findBySenderAndReceiverAndStatus(sender, receiver, RequestStatus.PENDING)
                .ifPresent(r -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A request is already pending to this student");
                });

        RideRequest request = RideRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(RequestStatus.PENDING)
                .build();

        RideRequest saved = rideRequestRepository.save(request);
        return mapToResponse(saved);
    }

    @Transactional
    public void cancelRequest(User sender, Long requestId) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!request.getSender().getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request status is not PENDING");
        }

        request.setStatus(RequestStatus.CANCELLED);
        rideRequestRepository.save(request);
    }

    @Transactional
    public void rejectRequest(User receiver, Long requestId) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot reject this request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request status is not PENDING");
        }

        request.setStatus(RequestStatus.REJECTED);
        rideRequestRepository.save(request);
    }

    @Transactional
    public void acceptRequest(User receiver, Long requestId) {
        RideRequest request = rideRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot accept this request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request status is not PENDING");
        }

        request.setStatus(RequestStatus.ACCEPTED);
        rideRequestRepository.save(request);

        // Create active ride match
        rideMatchService.createMatch(request.getSender(), receiver);

        // Deactivate both users' availabilities
        rideAvailabilityRepository.deactivateAllActiveForUser(request.getSender());
        rideAvailabilityRepository.deactivateAllActiveForUser(receiver);

        // Auto-cleanup all other pending requests for both users
        List<RideRequest> otherRequests = rideRequestRepository.findAllPendingForUser(RequestStatus.PENDING, request.getSender());
        otherRequests.addAll(rideRequestRepository.findAllPendingForUser(RequestStatus.PENDING, receiver));

        for (RideRequest r : otherRequests) {
            if (r.getId().equals(request.getId())) {
                continue;
            }
            if (r.getSender().getId().equals(request.getSender().getId()) || r.getSender().getId().equals(receiver.getId())) {
                r.setStatus(RequestStatus.CANCELLED);
            } else {
                r.setStatus(RequestStatus.REJECTED);
            }
            rideRequestRepository.save(r);
        }
    }

    @Transactional(readOnly = true)
    public List<RideRequestResponse> getIncomingRequests(User user) {
        List<RideRequest> requests = rideRequestRepository.findByReceiverAndStatusOrderByCreatedAtDesc(user, RequestStatus.PENDING);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RideRequestResponse> getOutgoingRequests(User user) {
        List<RideRequest> requests = rideRequestRepository.findBySenderAndStatusOrderByCreatedAtDesc(user, RequestStatus.PENDING);
        return requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private RideRequestResponse mapToResponse(RideRequest request) {
        String destination = "Unknown";
        LocalDateTime departureTime = request.getCreatedAt();

        // Query the sender's most recent availability record to fetch destination/departureTime
        var availabilityOpt = rideAvailabilityRepository.findFirstByUserOrderByCreatedAtDesc(request.getSender());
        if (availabilityOpt.isPresent()) {
            destination = availabilityOpt.get().getDestination();
            departureTime = availabilityOpt.get().getDepartureTime();
        }

        return RideRequestResponse.builder()
                .id(request.getId())
                .senderUsername(request.getSender().getUsername())
                .senderName(request.getSender().getName())
                .receiverUsername(request.getReceiver().getUsername())
                .receiverName(request.getReceiver().getName())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .destination(destination)
                .departureTime(departureTime)
                .build();
    }
}
