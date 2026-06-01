package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "booking_details",
        uniqueConstraints = @UniqueConstraint(name = "uq_bd_room_code", columnNames = "room_code"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BookingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_detail_id")
    private Long bookingDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bd_booking"))
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bd_room_type"))
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",
            foreignKey = @ForeignKey(name = "fk_bd_room"))
    private Room room;

    // ── Actual check-in/out (assigned after room is given) ─
    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    // ── Pricing snapshot ───────────────────────────────────
    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "num_nights", nullable = false)
    private Integer numNights;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // ── Room access code ───────────────────────────────────
    @Column(name = "room_code", length = 8)
    private String roomCode;

    @Column(name = "room_code_expires_at")
    private Instant roomCodeExpiresAt;
}