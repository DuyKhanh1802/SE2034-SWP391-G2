package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /*
     * Kiểm tra mã khuyến mãi đã tồn tại.
     */
    boolean existsByCode(String code);

    /*
     * Kiểm tra banner nổi bật đang hoạt động.
     */
    boolean existsByFeaturedTrueAndIsActiveTrueAndValidToAfter(Instant now);
}