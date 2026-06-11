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
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_item_id")
    private Long id;

    @Column(name = "item_code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "opening_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingQuantity = BigDecimal.ZERO;

    @Column(name = "current_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @Column(name = "minimum_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumQuantity = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.openingQuantity == null) {
            this.openingQuantity = BigDecimal.ZERO;
        }
        if (this.currentQuantity == null) {
            this.currentQuantity = BigDecimal.ZERO;
        }
        if (this.minimumQuantity == null) {
            this.minimumQuantity = BigDecimal.ZERO;
        }
        if (this.unitCost == null) {
            this.unitCost = BigDecimal.ZERO;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
