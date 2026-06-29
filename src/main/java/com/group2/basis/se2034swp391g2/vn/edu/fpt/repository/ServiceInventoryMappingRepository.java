package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.ServiceInventoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceInventoryMappingRepository extends JpaRepository<ServiceInventoryMapping, Long> {
    List<ServiceInventoryMapping> findByService_Id(Long serviceId);

    List<ServiceInventoryMapping> findByItem_Id(Long itemId);

    Optional<ServiceInventoryMapping> findByService_IdAndItem_Id(Long serviceId, Long itemId);

    Optional<ServiceInventoryMapping> findByIdAndItem_Id(Long id, Long itemId);

    boolean existsByItem_Id(Long itemId);
}
