package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariantService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.VariantServiceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomTypeVariantServiceRepository extends JpaRepository<RoomTypeVariantService, Long> {

    @Query(value = """
        SELECT 
            rvs.variant_id AS variantId,
            s.name AS serviceName,
            rvs.quantity AS quantity,
            rvs.included_type AS includedType
        FROM room_type_variant_services rvs
        JOIN services s ON s.service_id = rvs.service_id
        WHERE rvs.is_deleted = 0
          AND s.is_deleted = 0
          AND s.is_available = 1
          AND rvs.variant_id IN (:variantIds)
        ORDER BY rvs.variant_id, s.name
        """, nativeQuery = true)
    List<VariantServiceProjection> findIncludedServicesByVariantIds(@Param("variantIds") List<Long> variantIds);
}