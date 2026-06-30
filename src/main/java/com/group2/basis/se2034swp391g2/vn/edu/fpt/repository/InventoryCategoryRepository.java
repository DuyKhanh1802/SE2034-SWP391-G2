package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {
    List<InventoryCategory> findByIsActiveTrueOrderByNameAsc();

    Optional<InventoryCategory> findByIdAndIsActiveTrue(Long id);

    Optional<InventoryCategory> findByNameIgnoreCaseAndIsActiveTrue(String name);
}
