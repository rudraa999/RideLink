package com.project.ridelink.ridematch.repository;

import com.project.ridelink.ridematch.entity.RideMatch;
import com.project.ridelink.ridematch.entity.RideMatchMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideMatchMemberRepository extends JpaRepository<RideMatchMember, Long> {
    List<RideMatchMember> findByRideMatch(RideMatch rideMatch);
}
