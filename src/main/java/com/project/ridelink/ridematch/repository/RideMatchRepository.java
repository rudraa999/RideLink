package com.project.ridelink.ridematch.repository;

import com.project.ridelink.ridematch.entity.MatchStatus;
import com.project.ridelink.ridematch.entity.RideMatch;
import com.project.ridelink.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideMatchRepository extends JpaRepository<RideMatch, Long> {

    @Query("SELECT m.rideMatch FROM RideMatchMember m WHERE m.user = :user AND m.rideMatch.status = :status")
    Optional<RideMatch> findActiveMatchForUser(@Param("user") User user, @Param("status") MatchStatus status);

    @Query("SELECT m.rideMatch FROM RideMatchMember m WHERE m.user = :user AND m.rideMatch.status != :status ORDER BY m.rideMatch.createdAt DESC")
    List<RideMatch> findMatchHistoryForUser(@Param("user") User user, @Param("status") MatchStatus status);
}
