package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionSourceType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
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
@Table(name = "cash_transactions")
public class CashTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_transaction_id")
    private Long id;

    @Column(name = "document_code", nullable = false, unique = true, length = 30)
    private String documentCode;

    @Column(name = "payment_code", length = 50, columnDefinition = "NVARCHAR(50)")
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10)
    private CashTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private CashTransactionCategory category;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CashTransactionStatus status;

    @Column(name = "description", length = 300, columnDefinition = "NVARCHAR(300)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private CashTransactionSourceType sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    // Nếu đây là giao dịch đảo chiều thì field này trỏ về giao dịch gốc.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id")
    private CashTransaction originalTransaction;

    // Nếu đây là giao dịch gốc đã hủy thì field này trỏ sang giao dịch đảo chiều.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_transaction_id")
    private CashTransaction reversalTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 300, columnDefinition = "NVARCHAR(300)")
    private String cancellationReason;

    // Gán mặc định cho transaction mới trước khi lưu vào database.
    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = CashTransactionStatus.COMPLETED;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
