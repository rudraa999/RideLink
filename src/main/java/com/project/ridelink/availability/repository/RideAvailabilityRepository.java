package com.project.ridelink.availability.repository;

import com.project.ridelink.availability.entity.RideAvailability;
import com.project.ridelink.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideAvailabilityRepository extends JpaRepository<RideAvailability, Long> {

    Optional<RideAvailability> findByUserAndIsActiveTrue(User user);

    Optional<RideAvailability> findFirstByUserOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("UPDATE RideAvailability r SET r.isActive = false WHERE r.user = :user AND r.isActive = true")
    void deactivateAllActiveForUser(@Param("user") User user);

    @Query("SELECT r FROM RideAvailability r WHERE r.user.college.id = :collegeId " +
           "AND r.user.id != :userId " +
           "AND LOWER(r.destination) = LOWER(:destination) " +
           "AND r.isActive = true " +
           "AND r.expiresAt > :now " +
           "AND r.departureTime >= :startTime " +
           "AND r.departureTime <= :endTime")
    List<RideAvailability> findCompatibleAvailabilities(
            @Param("collegeId") Long collegeId,
            @Param("userId") Long userId,
            @Param("destination") String destination,
            @Param("now") LocalDateTime now,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
