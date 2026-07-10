package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_applications")
public class PaymentApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_detail_id", nullable = false)
    private BookingDetail bookingDetail;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal amount;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.appliedAt == null) {
            this.appliedAt = now;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
    }
}
