package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.InventoryTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.InventoryReportRowResponse;

import java.time.Instant;
import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByIdAndIsDeletedFalse(Long id);

    Optional<InventoryItem> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(String name, Long id);

    List<InventoryItem> findByIsDeletedFalseOrderByNameAsc();

    List<InventoryItem> findAllByIsDeletedFalse(Sort sort);

    long countByIsDeletedFalse();

    @Query("""
            SELECT i
            FROM InventoryItem i
            WHERE i.isDeleted = false
            AND (
                :keyword IS NULL
                OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:categoryId IS NULL OR i.category.id = :categoryId)
            AND (
                :stockStatus IS NULL
                OR (:stockStatus = 'OUT_OF_STOCK' AND i.currentQuantity = 0)
                OR (:stockStatus = 'LOW' AND i.currentQuantity > 0 AND i.currentQuantity <= i.minimumQuantity)
                OR (:stockStatus = 'NORMAL' AND i.currentQuantity > i.minimumQuantity)
            )
            """)
    Page<InventoryItem> searchItems(@Param("keyword") String keyword,
                                    @Param("categoryId") Long categoryId,
                                    @Param("stockStatus") String stockStatus,
                                    Pageable pageable);

    @Query("""
            SELECT i
            FROM InventoryItem i
            WHERE i.isDeleted = false
            AND (
                :keyword IS NULL
                OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:categoryId IS NULL OR i.category.id = :categoryId)
            AND (
                :stockStatus IS NULL
                OR (:stockStatus = 'OUT_OF_STOCK' AND i.currentQuantity = 0)
                OR (:stockStatus = 'LOW' AND i.currentQuantity > 0 AND i.currentQuantity <= i.minimumQuantity)
                OR (:stockStatus = 'NORMAL' AND i.currentQuantity > i.minimumQuantity)
            )
            ORDER BY i.id ASC
            """)
    List<InventoryItem> searchItemsForExpiryFilter(@Param("keyword") String keyword,
                                                   @Param("categoryId") Long categoryId,
                                                   @Param("stockStatus") String stockStatus);

    @Query("""
            SELECT COUNT(i)
            FROM InventoryItem i
            WHERE i.isDeleted = false
            AND i.currentQuantity <= i.minimumQuantity
            """)
    long countLowStockItems();

    @Query("""
            SELECT i
            FROM InventoryItem i
            WHERE i.isDeleted = false
            AND i.currentQuantity <= i.minimumQuantity
            ORDER BY i.currentQuantity ASC
            """)
    List<InventoryItem> findLowStockItems();
    @Query("""
        SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.InventoryReportRowResponse(
            i.id,
            i.name,
            c.name,
            i.unit,
            i.currentQuantity,
            i.minimumQuantity,
            i.unitCost,
            COALESCE((
                SELECT SUM(t.quantity)
                FROM InventoryTransaction t
                WHERE t.item.id = i.id
                  AND t.type = :inType
                  AND t.createdAt >= :fromInstant
                  AND t.createdAt < :toInstant
            ), 0),
            COALESCE((
                SELECT SUM(t.quantity)
                FROM InventoryTransaction t
                WHERE t.item.id = i.id
                  AND t.type = :outType
                  AND t.createdAt >= :fromInstant
                  AND t.createdAt < :toInstant
            ), 0),
            COALESCE((
                SELECT SUM(t.quantity)
                FROM InventoryTransaction t
                WHERE t.item.id = i.id
                  AND t.type = :disposalType
                  AND t.createdAt >= :fromInstant
                  AND t.createdAt < :toInstant
            ), 0),
            (
                SELECT MAX(r.receiptDate)
                FROM InventoryReceipt r
                WHERE r.item.id = i.id
                  AND r.receiptDate >= :fromDate
                  AND r.receiptDate <= :toDate
            )
        )
        FROM InventoryItem i
        LEFT JOIN i.category c
        WHERE i.isDeleted = false
          AND (:categoryId IS NULL OR c.id = :categoryId)
          AND (
                :keyword IS NULL
                OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
          AND (
                :stockStatus IS NULL
                OR (:stockStatus = 'OUT_OF_STOCK' AND i.currentQuantity <= 0)
                OR (:stockStatus = 'LOW' AND i.currentQuantity > 0 AND i.currentQuantity <= i.minimumQuantity)
                OR (:stockStatus = 'NORMAL' AND i.currentQuantity > i.minimumQuantity)
          )
        """)
    List<InventoryReportRowResponse> findInventoryReportRows(@Param("fromInstant") Instant fromInstant,
                                                             @Param("toInstant") Instant toInstant,
                                                             @Param("fromDate") LocalDate fromDate,
                                                             @Param("toDate") LocalDate toDate,
                                                             @Param("categoryId") Long categoryId,
                                                             @Param("keyword") String keyword,
                                                             @Param("stockStatus") String stockStatus,
                                                             @Param("inType") InventoryTransactionType inType,
                                                             @Param("outType") InventoryTransactionType outType,
                                                             @Param("disposalType") InventoryTransactionType disposalType);
}
