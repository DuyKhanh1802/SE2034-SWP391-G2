package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.InventoryTransactionType;
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
@Table(name = "inventory_transactions")
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 15)
    private InventoryTransactionType type;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "remaining_quantity", precision = 12, scale = 2)
    private BigDecimal remainingQuantity;

    @Column(name = "description", length = 300, columnDefinition = "NVARCHAR(300)")
    private String description;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

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
    }

    @Transient
    public String getSourceLabel() {
        if (sourceType == null || sourceType.isBlank()) {
            return "Thủ công";
        }
        return switch (sourceType) {
            case "OPENING" -> "Tồn đầu kỳ";
            case "INVENTORY_RECEIPT" -> "Phiếu nhập kho";
            case "INVENTORY_DISPOSAL" -> "Hủy hàng hóa";
            case "FOLIO_ITEM" -> "Tiêu hao dịch vụ";
            case "ROOM_REFRESH" -> "Lấp đồ phòng";
            default -> sourceType;
        };
    }

    @Transient
    public String getDescriptionLabel() {
        if (description == null || description.isBlank()) {
            return "-";
        }
        if ("INVENTORY_RECEIPT".equals(sourceType) && description.startsWith("Nhập hàng IR-")) {
            return "Nhập kho từ phiếu nhập";
        }
        return description;
    }
}
