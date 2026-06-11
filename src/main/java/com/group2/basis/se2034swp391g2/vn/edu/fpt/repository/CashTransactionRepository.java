package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionSourceType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
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
            AND (:keyword = '' OR LOWER(ct.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(ct.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY ct.createdAt DESC
            """)
    List<CashTransaction> search(@Param("type") CashTransactionType type,
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
            AND ct.fundMethod = :fundMethod
            """)
    BigDecimal sumByTypeAndFundMethod(@Param("type") CashTransactionType type,
                                      @Param("fundMethod") PaymentMethod fundMethod);

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
