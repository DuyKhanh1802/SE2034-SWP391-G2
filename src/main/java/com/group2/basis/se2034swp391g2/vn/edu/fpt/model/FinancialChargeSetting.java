package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PriceDisplayMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "financial_charge_settings")
public class FinancialChargeSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long id;

    @Column(name = "service_charge_rate", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) default 5")
    private BigDecimal serviceChargeRate = BigDecimal.valueOf(5);

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) default 8")
    private BigDecimal vatRate = BigDecimal.valueOf(8);

    @Column(name = "inventory_vat_rate", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) default 8")
    private BigDecimal inventoryVatRate = BigDecimal.valueOf(8);

    @Column(name = "tax_on_service_charge", nullable = false, columnDefinition = "bit default 1")
    private Boolean taxOnServiceCharge = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_display_mode", nullable = false, length = 15, columnDefinition = "varchar(15) default 'PLUS_PLUS'")
    private PriceDisplayMode priceDisplayMode = PriceDisplayMode.PLUS_PLUS;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false, columnDefinition = "bit default 1")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.serviceChargeRate == null) {
            this.serviceChargeRate = BigDecimal.valueOf(5);
        }
        if (this.vatRate == null) {
            this.vatRate = BigDecimal.valueOf(8);
        }
        if (this.inventoryVatRate == null) {
            this.inventoryVatRate = BigDecimal.valueOf(8);
        }
        if (this.taxOnServiceCharge == null) {
            this.taxOnServiceCharge = true;
        }
        if (this.priceDisplayMode == null) {
            this.priceDisplayMode = PriceDisplayMode.PLUS_PLUS;
        }
        if (this.effectiveFrom == null) {
            this.effectiveFrom = LocalDate.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
