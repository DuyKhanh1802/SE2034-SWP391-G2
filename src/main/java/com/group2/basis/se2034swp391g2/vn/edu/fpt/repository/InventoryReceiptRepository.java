package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long> {
    boolean existsByCode(String code);

    List<InventoryReceipt> findTop20ByOrderByCreatedAtDesc();

    List<InventoryReceipt> findTop20ByItem_IdOrderByReceiptDateDescCreatedAtDesc(Long itemId);

    Optional<InventoryReceipt> findFirstByItem_IdAndExpiryDateIsNotNullOrderByReceiptDateDescCreatedAtDesc(Long itemId);
}
