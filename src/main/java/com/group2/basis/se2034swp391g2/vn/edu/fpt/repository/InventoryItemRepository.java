package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByIdAndIsDeletedFalse(Long id);

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
}
