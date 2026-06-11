package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    boolean existsByCode(String code);

    Optional<InventoryItem> findByIdAndIsDeletedFalse(Long id);

    List<InventoryItem> findByIsDeletedFalseOrderByNameAsc();

    long countByIsDeletedFalse();

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
