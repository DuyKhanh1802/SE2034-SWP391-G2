package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "folio_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folio_item_id")
    private Long folioItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_folio_booking"))
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id",
            foreignKey = @ForeignKey(name = "fk_folio_wo"))
    private WorkOrder workOrder;

    @Nationalized
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 15)
    private ItemType itemType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by",
            foreignKey = @ForeignKey(name = "fk_folio_posted_by"))
    private User postedBy;

    @Nationalized
    @Column(name = "adjustment_reason", length = 300)
    private String adjustmentReason;

    // ── Void ───────────────────────────────────────────────
    @Column(name = "is_voided", nullable = false)
    private Boolean isVoided = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by",
            foreignKey = @ForeignKey(name = "fk_folio_voided_by"))
    private User voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Nationalized
    @Column(name = "voided_reason", length = 200)
    private String voidedReason;
}