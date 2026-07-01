package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.CashTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {
    boolean existsByDocumentCode(String documentCode);

    // Lấy các mã chứng từ dạng PT/PC để service sinh số tiếp theo.
    @Query("""
            SELECT ct.documentCode
            FROM CashTransaction ct
            WHERE ct.documentCode LIKE 'PT-%'
            OR ct.documentCode LIKE 'PC-%'
            """)
    List<String> findSimpleDocumentCodes();

    boolean existsByCategoryAndSourceId(CashTransactionCategory category, Long sourceId);

    Optional<CashTransaction> findByCategoryAndSourceId(CashTransactionCategory category, Long sourceId);

    boolean existsBySourceIdAndCategoryIn(Long sourceId, List<CashTransactionCategory> categories);

    Optional<CashTransaction> findFirstBySourceIdAndCategoryIn(Long sourceId, List<CashTransactionCategory> categories);

    // Lấy chi tiết kèm người tạo, người hủy và giao dịch liên quan để Thymeleaf hiển thị.
    @Query("""
            SELECT ct
            FROM CashTransaction ct
            LEFT JOIN FETCH ct.createdBy
            LEFT JOIN FETCH ct.cancelledBy
            LEFT JOIN FETCH ct.originalTransaction originalTransaction
            LEFT JOIN FETCH originalTransaction.cancelledBy
            LEFT JOIN FETCH ct.reversalTransaction
            WHERE ct.id = :id
            """)
    Optional<CashTransaction> findDetailById(@Param("id") Long id);

    List<CashTransaction> findByOrderByCreatedAtDesc(Pageable pageable);

    // Tìm giao dịch theo bộ lọc trên màn list.
    @Query("""
            SELECT ct
            FROM CashTransaction ct
            WHERE (:type IS NULL OR ct.type = :type)
            AND (:category IS NULL OR ct.category = :category)
            AND (:paymentMethod IS NULL OR (
                ct.paymentMethod = :paymentMethod
                OR (
                    ct.paymentMethod IS NULL
                    AND ct.category IN :paymentCategories
                    AND EXISTS (
                        SELECT payment.id
                        FROM Payment payment
                        WHERE payment.id = ct.sourceId
                        AND payment.method = :paymentMethod
                    )
                )
            ))
            AND (:fromDate IS NULL OR ct.createdAt >= :fromDate)
            AND (:toDate IS NULL OR ct.createdAt < :toDate)
            AND (:keyword = '' OR LOWER(ct.documentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(ct.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY ct.createdAt DESC
            """)
    List<CashTransaction> search(@Param("type") CashTransactionType type,
                                 @Param("category") CashTransactionCategory category,
                                 @Param("paymentMethod") PaymentMethod paymentMethod,
                                 @Param("paymentCategories") List<CashTransactionCategory> paymentCategories,
                                 @Param("fromDate") Instant fromDate,
                                 @Param("toDate") Instant toDate,
                                 @Param("keyword") String keyword);

    @Query("""
            SELECT COALESCE(SUM(ct.amount), 0)
            FROM CashTransaction ct
            WHERE ct.type = :type
            """)
    BigDecimal sumByType(@Param("type") CashTransactionType type);

    @Query("""
            SELECT COALESCE(SUM(ct.amount), 0)
            FROM CashTransaction ct
            WHERE ct.type = :type
            AND ct.createdAt >= :from
            AND ct.createdAt < :to
            """)
    BigDecimal sumByTypeBetween(@Param("type") CashTransactionType type,
                                @Param("from") Instant from,
                                @Param("to") Instant to);
}
