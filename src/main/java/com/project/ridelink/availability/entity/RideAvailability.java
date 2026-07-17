package com.project.ridelink.availability.entity;

import com.project.ridelink.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ride_availabilities", indexes = {
    @Index(name = "idx_active_expiry", columnList = "is_active, expires_at"),
    @Index(name = "idx_destination", columnList = "destination")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String destination;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "transport_type")
    private String transportType;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
