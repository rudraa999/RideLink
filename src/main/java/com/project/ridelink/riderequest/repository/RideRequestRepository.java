package com.project.ridelink.riderequest.repository;

import com.project.ridelink.riderequest.entity.RequestStatus;
import com.project.ridelink.riderequest.entity.RideRequest;
import com.project.ridelink.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {

    Optional<RideRequest> findBySenderAndReceiverAndStatus(User sender, User receiver, RequestStatus status);

    List<RideRequest> findByReceiverAndStatusOrderByCreatedAtDesc(User receiver, RequestStatus status);

    List<RideRequest> findBySenderAndStatusOrderByCreatedAtDesc(User sender, RequestStatus status);

    @Query("SELECT r FROM RideRequest r WHERE r.status = :status AND (r.sender = :user OR r.receiver = :user)")
    List<RideRequest> findAllPendingForUser(@Param("status") RequestStatus status, @Param("user") User user);
}
