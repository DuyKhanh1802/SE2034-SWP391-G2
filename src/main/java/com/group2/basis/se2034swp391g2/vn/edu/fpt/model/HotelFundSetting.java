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

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal openingBalance;

    @Column(name = "opening_cash_balance", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal openingCashBalance;

    @Column(name = "opening_transfer_balance", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal openingTransferBalance;

    @Column(name = "opening_card_balance", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal openingCardBalance;

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
        if (this.openingCashBalance == null) {
            this.openingCashBalance = BigDecimal.ZERO;
        }
        if (this.openingTransferBalance == null) {
            this.openingTransferBalance = BigDecimal.ZERO;
        }
        if (this.openingCardBalance == null) {
            this.openingCardBalance = BigDecimal.ZERO;
        }
        if (this.openingBalance == null) {
            this.openingBalance = openingCashBalance.add(openingTransferBalance).add(openingCardBalance);
        }
    }
}
