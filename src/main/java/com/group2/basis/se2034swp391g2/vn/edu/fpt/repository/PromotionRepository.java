package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    boolean existsByCode(String code);

    boolean existsByFeaturedTrueAndIsActiveTrueAndValidToAfter(Instant now);

    boolean existsByFeaturedTrueAndIsActiveTrueAndValidToAfterAndIdNot(Instant now, Long id);
}
