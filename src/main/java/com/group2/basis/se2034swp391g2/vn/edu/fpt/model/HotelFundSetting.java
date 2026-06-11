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
@Table(name = "hotel_fund_settings")
public class HotelFundSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fund_setting_id")
    private Long id;

    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configured_by")
    private User configuredBy;

    @Column(name = "configured_at", nullable = false)
    private Instant configuredAt;

    @PrePersist
    protected void onCreate() {
        if (this.configuredAt == null) {
            this.configuredAt = Instant.now();
        }
    }
}
