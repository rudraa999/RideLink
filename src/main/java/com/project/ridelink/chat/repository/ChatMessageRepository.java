package com.project.ridelink.chat.repository;

import com.project.ridelink.chat.entity.ChatMessage;
import com.project.ridelink.ridematch.entity.RideMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRideMatchOrderBySentAtAsc(RideMatch rideMatch);
}
