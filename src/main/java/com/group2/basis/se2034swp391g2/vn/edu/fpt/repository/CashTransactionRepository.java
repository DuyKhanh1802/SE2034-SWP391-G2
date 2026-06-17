package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionSourceType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.CashTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {
    boolean existsByCode(String code);

    boolean existsByDocumentCode(String documentCode);

    boolean existsBySourceTypeAndSourceId(CashTransactionSourceType sourceType, Long sourceId);

    java.util.Optional<CashTransaction> findBySourceTypeAndSourceId(CashTransactionSourceType sourceType, Long sourceId);

    List<CashTransaction> findByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT ct
            FROM CashTransaction ct
            WHERE (:type IS NULL OR ct.type = :type)
            AND (:category IS NULL OR ct.category = :category)
            AND (:sourceType IS NULL OR ct.sourceType = :sourceType)
            AND (:fromDate IS NULL OR ct.createdAt >= :fromDate)
            AND (:toDate IS NULL OR ct.createdAt < :toDate)
            AND (:keyword = '' OR LOWER(ct.documentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(ct.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY ct.createdAt DESC
            """)
    List<CashTransaction> search(@Param("type") CashTransactionType type,
                                 @Param("category") CashTransactionCategory category,
                                 @Param("sourceType") CashTransactionSourceType sourceType,
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
