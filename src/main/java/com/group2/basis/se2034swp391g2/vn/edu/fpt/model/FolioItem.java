package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PriceDisplayMode;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "folio_items")
public class FolioItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folio_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id") //
    private Service service;
    @Column(name = "description", nullable = false, length = 200, columnDefinition = "NVARCHAR(200)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 15)
    private FolioItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_status", nullable = false, length = 15)
    private FolioItemStatus serviceStatus = FolioItemStatus.REQUESTED;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal amount;

    @Column(name = "base_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(name = "service_charge_rate", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) default 0")
    private BigDecimal serviceChargeRate = BigDecimal.ZERO;

    @Column(name = "service_charge_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal serviceChargeAmount = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) default 0")
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_display_mode", nullable = false, length = 15, columnDefinition = "varchar(15) default 'PLUS_PLUS'")
    private PriceDisplayMode priceDisplayMode = PriceDisplayMode.PLUS_PLUS;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal unitPrice;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_detail_id")
    private BookingDetail bookingDetail;

    @Column(name = "adjustment_reason", length = 300, columnDefinition = "NVARCHAR(300)")
    private String adjustmentReason;

    @Column(name = "is_voided", nullable = false)
    private Boolean isVoided = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private User voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_reason", length = 200, columnDefinition = "NVARCHAR(200)")
    private String voidedReason;

    @PrePersist
    protected void onPost() {
        if (this.postedAt == null) {
            this.postedAt = Instant.now();
        }
        if (this.baseAmount == null) {
            this.baseAmount = this.amount == null ? BigDecimal.ZERO : this.amount;
        }
        if (this.serviceChargeRate == null) {
            this.serviceChargeRate = BigDecimal.ZERO;
        }
        if (this.serviceChargeAmount == null) {
            this.serviceChargeAmount = BigDecimal.ZERO;
        }
        if (this.vatRate == null) {
            this.vatRate = BigDecimal.ZERO;
        }
        if (this.vatAmount == null) {
            this.vatAmount = BigDecimal.ZERO;
        }
        if (this.totalAmount == null) {
            this.totalAmount = this.amount == null ? BigDecimal.ZERO : this.amount;
        }
        if (this.amount == null) {
            this.amount = this.totalAmount;
        }
        if (this.priceDisplayMode == null) {
            this.priceDisplayMode = PriceDisplayMode.PLUS_PLUS;
        }
        if (this.serviceStatus == null) {
            this.serviceStatus = FolioItemStatus.REQUESTED;
        }
    }
}
