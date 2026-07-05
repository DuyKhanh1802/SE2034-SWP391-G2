package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.PromotionProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    boolean existsByCode(String code);

    boolean existsByFeaturedTrueAndIsActiveTrueAndValidToAfter(Instant now);

    boolean existsByFeaturedTrueAndIsActiveTrueAndValidToAfterAndIdNot(Instant now, Long id);

    @Query(
            value = """
                SELECT
                    p.promotion_id AS promotionId,
                    p.code AS code,
                    p.name AS name,
                    p.description AS description,
                    p.discount_amount AS discountAmount,
                    p.usage_count AS usageCount,
                    p.usage_limit AS usageLimit,

                    CAST(p.valid_from AS DATETIME2) AS validFrom,
                    CAST(p.valid_to AS DATETIME2) AS validTo,

                    p.is_active AS isActive,
                    p.featured AS featured,
                    p.image_url AS imageUrl,
                    p.show_on_homepage AS showOnHomepage
                FROM promotions p
                WHERE p.is_active = 1
                  AND (p.valid_from IS NULL OR p.valid_from <= SYSDATETIMEOFFSET())
                  AND (p.valid_to IS NULL OR p.valid_to >= SYSDATETIMEOFFSET())
                  AND (
                        p.usage_limit IS NULL
                        OR ISNULL(p.usage_count, 0) < p.usage_limit
                      )
                ORDER BY
                    p.featured DESC,
                    p.created_at DESC,
                    p.promotion_id DESC
                """,
            nativeQuery = true
    )
    List<PromotionProjection> findActivePromotionList();


    @Query(
            value = """
                SELECT
                    p.promotion_id AS promotionId,
                    p.code AS code,
                    p.name AS name,
                    p.description AS description,
                    p.discount_amount AS discountAmount,
                    p.usage_count AS usageCount,
                    p.usage_limit AS usageLimit,

                    CAST(p.valid_from AS DATETIME2) AS validFrom,
                    CAST(p.valid_to AS DATETIME2) AS validTo,

                    p.is_active AS isActive,
                    p.featured AS featured,
                    p.image_url AS imageUrl,
                    p.show_on_homepage AS showOnHomepage
                FROM promotions p
                WHERE LOWER(p.code) = LOWER(:code)
                  AND p.is_active = 1
                  AND (p.valid_from IS NULL OR p.valid_from <= SYSDATETIMEOFFSET())
                  AND (p.valid_to IS NULL OR p.valid_to >= SYSDATETIMEOFFSET())
                  AND (
                        p.usage_limit IS NULL
                        OR ISNULL(p.usage_count, 0) < p.usage_limit
                      )
                """,
            nativeQuery = true
    )
    Optional<PromotionProjection> findValidPromotionByCode(
            @Param("code") String code
    );

    @Query(
            value = """
            SELECT TOP (1)
                p.promotion_id AS promotionId,
                p.code AS code,
                p.name AS name,
                p.description AS description,
                p.discount_amount AS discountAmount,
                p.usage_count AS usageCount,
                p.usage_limit AS usageLimit,

                CAST(p.valid_from AS DATETIME2) AS validFrom,
                CAST(p.valid_to AS DATETIME2) AS validTo,

                p.is_active AS isActive,
                p.featured AS featured,
                p.image_url AS imageUrl,
                p.show_on_homepage AS showOnHomepage
            FROM promotions p
            WHERE p.is_active = 1
              AND p.show_on_homepage = 1
              AND p.featured = 1
              AND (p.valid_from IS NULL OR p.valid_from <= SYSDATETIMEOFFSET())
              AND (p.valid_to IS NULL OR p.valid_to >= SYSDATETIMEOFFSET())
              AND (
                    p.usage_limit IS NULL
                    OR ISNULL(p.usage_count, 0) < p.usage_limit
                  )
            ORDER BY
                p.created_at DESC,
                p.promotion_id DESC
            """,
            nativeQuery = true
    )
    Optional<PromotionProjection> findTopHomepagePromotion();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") Long id);

}
