package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

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
@Table(name = "inventory_receipts")
public class InventoryReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long id;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 30)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal unitCost;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal subtotal;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) default 0")
    private BigDecimal vatRate;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal vatAmount;

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal totalCost;

    @Column(name = "supplier", length = 150, columnDefinition = "NVARCHAR(150)")
    private String supplier;

    @Column(name = "note", length = 300, columnDefinition = "NVARCHAR(300)")
    private String note;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.subtotal == null) {
            this.subtotal = BigDecimal.ZERO;
        }
        if (this.vatRate == null) {
            this.vatRate = BigDecimal.ZERO;
        }
        if (this.vatAmount == null) {
            this.vatAmount = BigDecimal.ZERO;
        }
        if (this.totalCost == null) {
            this.totalCost = BigDecimal.ZERO;
        }
        if (this.receiptDate == null) {
            this.receiptDate = LocalDate.now();
        }
    }
}
