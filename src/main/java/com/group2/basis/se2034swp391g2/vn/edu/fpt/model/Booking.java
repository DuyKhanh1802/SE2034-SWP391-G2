package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DepositStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uq_bookings_reference", columnNames = "booking_reference"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    // ── Guest snapshot (walk-in / anonymous) ───────────────
    @Nationalized
    @Column(name = "guest_first_name", nullable = false, length = 50)
    private String guestFirstName;

    @Nationalized
    @Column(name = "guest_last_name", nullable = false, length = 50)
    private String guestLastName;

    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;

    @Column(name = "guest_email", length = 150)
    private String guestEmail;

    // ── Linked account (nullable) ──────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id",
            foreignKey = @ForeignKey(name = "fk_bookings_guest"))
    private User guest;

    // ── Promotion ──────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id",
            foreignKey = @ForeignKey(name = "fk_bookings_promo"))
    private Promotion promotion;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // ── Dates & guests ─────────────────────────────────────
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "num_adults", nullable = false)
    private Byte numAdults = 1;

    @Column(name = "num_children", nullable = false)
    private Byte numChildren = 0;

    @Nationalized
    @Column(name = "special_requests", length = 500)
    private String specialRequests;

    @Nationalized
    @Column(name = "booking_reference", nullable = false, length = 20)
    private String bookingReference;

    // ── Status ─────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false, length = 15)
    private DepositStatus depositStatus = DepositStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private BookingStatus status = BookingStatus.PENDING;

    // ── Financials ─────────────────────────────────────────
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_calculated_at")
    private Instant amountCalculatedAt;

    // ── Cancellation ───────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by",
            foreignKey = @ForeignKey(name = "fk_bookings_cancelled"))
    private User cancelledBy;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // ── Audit ──────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",
            foreignKey = @ForeignKey(name = "fk_bookings_created"))
    private User createdBy;

    @Column(name = "actual_checkout_at")
    private Instant actualCheckoutAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
