package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.InventoryTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findTop30ByOrderByCreatedAtDesc();

    List<InventoryTransaction> findByItem_IdOrderByCreatedAtDesc(Long itemId);

    Page<InventoryTransaction> findByItem_Id(Long itemId, Pageable pageable);

    Page<InventoryTransaction> findAll(Pageable pageable);

    @Query("""
            SELECT transaction
            FROM InventoryTransaction transaction
            WHERE (:itemId IS NULL OR transaction.item.id = :itemId)
            AND (:type IS NULL OR transaction.type = :type)
            AND (:fromTime IS NULL OR transaction.createdAt >= :fromTime)
            AND (:toTime IS NULL OR transaction.createdAt < :toTime)
            """)
    Page<InventoryTransaction> search(@Param("itemId") Long itemId,
                                      @Param("type") InventoryTransactionType type,
                                      @Param("fromTime") Instant fromTime,
                                      @Param("toTime") Instant toTime,
                                      Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.quantity), 0)
            FROM InventoryTransaction t
            WHERE t.item.id = :itemId
            AND t.type = :type
            """)
    BigDecimal sumQuantityByItemAndType(@Param("itemId") Long itemId,
                                        @Param("type") InventoryTransactionType type);
}
