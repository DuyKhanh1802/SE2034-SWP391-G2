package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.ServiceInventoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceInventoryMappingRepository extends JpaRepository<ServiceInventoryMapping, Long> {
    List<ServiceInventoryMapping> findByService_Id(Long serviceId);

    List<ServiceInventoryMapping> findByItem_Id(Long itemId);
}
