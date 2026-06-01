package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Entity
@Table(name = "booking_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bsh_booking"))
    private Booking booking;

    @Column(name = "old_status", nullable = false, length = 15)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 15)
    private String newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by",
            foreignKey = @ForeignKey(name = "fk_bsh_user"))
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt = Instant.now();

    @Column(name = "reason", length = 300)
    @Nationalized
    private String reason;
}