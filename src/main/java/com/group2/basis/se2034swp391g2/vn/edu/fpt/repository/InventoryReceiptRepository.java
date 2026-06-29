package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long> {
    boolean existsByCode(String code);

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
}
