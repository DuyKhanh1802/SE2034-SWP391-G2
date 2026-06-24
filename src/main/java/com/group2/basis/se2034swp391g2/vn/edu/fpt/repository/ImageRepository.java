package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByEntityTypeAndEntityIdOrderBySortOrderAsc(
            ImageEntityType entityType,
            Long entityId
    );

    Optional<Image> findFirstByEntityTypeAndEntityIdAndIsPrimaryTrueOrderBySortOrderAsc(
            ImageEntityType entityType,
            Long entityId
    );
}