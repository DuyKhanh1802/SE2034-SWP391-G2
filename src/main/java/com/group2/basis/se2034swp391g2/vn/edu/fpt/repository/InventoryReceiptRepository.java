package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long> {
    boolean existsByBatchCode(String batchCode);

    Optional<InventoryReceipt> findTopByBatchCodeStartingWithOrderByBatchCodeDesc(String prefix);

    List<InventoryReceipt> findTop20ByOrderByCreatedAtDesc();

    // Lấy phiếu nhập kèm vật tư và người tạo để màn dòng tiền hiển thị đúng nguồn nhập kho.
    @Query("""
            SELECT receipt
            FROM InventoryReceipt receipt
            LEFT JOIN FETCH receipt.item
            LEFT JOIN FETCH receipt.createdBy
            WHERE receipt.id = :id
            """)
    Optional<InventoryReceipt> findDetailById(@Param("id") Long id);

    List<InventoryReceipt> findTop20ByItem_IdOrderByReceiptDateDescCreatedAtDesc(Long itemId);

    Optional<InventoryReceipt> findFirstByItem_IdAndExpiryDateIsNotNullOrderByReceiptDateDescCreatedAtDesc(Long itemId);

    @Query(value = """
            SELECT ranked.inventory_item_id, ranked.expiry_date
            FROM (
                SELECT receipt.inventory_item_id,
                       receipt.expiry_date,
                       ROW_NUMBER() OVER (
                           PARTITION BY receipt.inventory_item_id
                           ORDER BY receipt.receipt_date DESC, receipt.created_at DESC
                       ) AS row_number
                FROM inventory_receipts receipt
                WHERE receipt.inventory_item_id IN (:itemIds)
                  AND receipt.expiry_date IS NOT NULL
            ) ranked
            WHERE ranked.row_number = 1
            """, nativeQuery = true)
    List<Object[]> findLatestExpiryDatesByItemIds(@Param("itemIds") Collection<Long> itemIds);

    @Query(value = """
            SELECT ranked.inventory_item_id
            FROM (
                SELECT receipt.inventory_item_id,
                       receipt.expiry_date,
                       ROW_NUMBER() OVER (
                           PARTITION BY receipt.inventory_item_id
                           ORDER BY receipt.receipt_date DESC, receipt.created_at DESC
                       ) AS row_number
                FROM inventory_receipts receipt
                JOIN inventory_items item
                  ON item.inventory_item_id = receipt.inventory_item_id
                WHERE item.is_deleted = 0
                  AND receipt.expiry_date IS NOT NULL
            ) ranked
            WHERE ranked.row_number = 1
              AND ranked.expiry_date >= :today
              AND ranked.expiry_date <= :warningDate
            ORDER BY ranked.inventory_item_id
            """, nativeQuery = true)
    List<Long> findItemIdsWithLatestExpiryDateBetween(@Param("today") LocalDate today,
                                                      @Param("warningDate") LocalDate warningDate);
}
