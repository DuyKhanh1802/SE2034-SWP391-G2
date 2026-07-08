package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ServiceCategoryType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeService;

 import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.BookingServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.HomeServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.ServiceProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



import java.util.List;

@Repository

public interface ServiceRepository extends JpaRepository<com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service, Long> {
    List<com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service> findByIsDeletedFalseAndIsAvailableTrueOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(String name, Long id);

    @Query("""
        SELECT s
        FROM Service s
        JOIN s.category c
        WHERE c.id = :categoryId
          AND s.isAvailable = true
          AND s.isDeleted = false
          AND c.isDeleted = false
        ORDER BY s.name ASC
    """)
    List<Service> findAvailableByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
        SELECT 
            s.id AS id,
            s.name AS name,
            s.description AS description,
            s.price AS price,
            c.type AS categoryType,
            c.name AS categoryName,
            img.imageUrl AS imageUrl
        FROM Service s
        JOIN s.category c
        LEFT JOIN Image img
            ON img.entityType = :entityType
            AND img.entityId = s.id
            AND img.isPrimary = true
        WHERE s.isDeleted = false
          AND s.isAvailable = true
          AND c.isDeleted = false
          AND c.type = :categoryType
        ORDER BY s.id DESC
        """)
    List<HomeServiceProjection> findServiceByCategoryType(
            @Param("categoryType") ServiceCategoryType serviceCategoryType,
            @Param("entityType") ImageEntityType imageEntityType,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceResponse(
                        s.id,
                        s.name,
                        s.description,
                        s.price,
                        s.isAvailable,
                        c.id,
                        c.name,
                        img.imageUrl
                    )
                    FROM Service s
                    JOIN s.category c
                    LEFT JOIN Image img
                        ON img.entityType = :entityType
                        AND img.entityId = s.id
                        AND img.isPrimary = true
                    WHERE s.isDeleted = false
                      AND c.isDeleted = false
                      AND (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          )
                      AND (:categoryId IS NULL OR c.id = :categoryId)
                      AND (:availability IS NULL OR s.isAvailable = :availability)
                    ORDER BY s.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(s.id)
                    FROM Service s
                    JOIN s.category c
                    WHERE s.isDeleted = false
                      AND c.isDeleted = false
                      AND (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          )
                      AND (:categoryId IS NULL OR c.id = :categoryId)
                      AND (:availability IS NULL OR s.isAvailable = :availability)
                    """
    )
    Page<ServiceResponse> searchServicesForAdmin(@Param("keyword") String keyword,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("availability") Boolean availability,
                                                 @Param("entityType") ImageEntityType entityType,
                                                 Pageable pageable);

    @Query(
            value = """
                SELECT
                    s.service_id AS id,
                    s.name AS name,
                    s.price AS price,
                    s.description AS description,
                    img.image_url AS imageUrl
                FROM services s
                LEFT JOIN images img
                    ON img.entity_type = 'SERVICE'
                    AND img.entity_id = s.service_id
                    AND img.is_primary = 1
                WHERE s.is_deleted = 0
                    AND s.is_available = 1
                    AND s.category_id = 1
                    AND (
                        :priceFilter = 'ALL'
                        OR (:priceFilter = 'UNDER_200' AND s.price < 200000)
                        OR (:priceFilter = 'FROM_200_TO_500' AND s.price >= 200000 AND s.price <= 500000)
                        OR (:priceFilter = 'OVER_500' AND s.price > 500000)
                    )
                ORDER BY
                    CASE WHEN :priceSort = 'ASC' THEN s.price END ASC,
                    CASE WHEN :priceSort = 'DESC' THEN s.price END DESC,
                    s.service_id ASC
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM services s
                WHERE s.is_deleted = 0
                    AND s.is_available = 1
                    AND s.category_id = 1
                    AND (
                        :priceFilter = 'ALL'
                        OR (:priceFilter = 'UNDER_200' AND s.price < 200000)
                        OR (:priceFilter = 'FROM_200_TO_500' AND s.price >= 200000 AND s.price <= 500000)
                        OR (:priceFilter = 'OVER_500' AND s.price > 500000)
                    )
                """,
            nativeQuery = true
    )
    Page<ServiceProjection> findListDining(
            @Param("priceFilter") String priceFilter,
            @Param("priceSort") String priceSort,
            Pageable pageable);

    @Query(
            value = """
                SELECT
                    s.service_id AS id,
                    s.name AS name,
                    s.price AS price,
                    s.description AS description,
                    img.image_url AS imageUrl
                FROM services s
                LEFT JOIN images img
                    ON img.entity_type = 'SERVICE'
                    AND img.entity_id = s.service_id
                    AND img.is_primary = 1
                WHERE s.is_deleted = 0
                    AND s.is_available = 1
                    AND s.category_id = 2
                    AND (
                        :priceFilter = 'ALL'
                        OR (:priceFilter = 'UNDER_200' AND s.price < 200000)
                        OR (:priceFilter = 'FROM_200_TO_500' AND s.price >= 200000 AND s.price <= 500000)
                        OR (:priceFilter = 'OVER_500' AND s.price > 500000)
                    )
                ORDER BY
                    CASE WHEN :priceSort = 'ASC' THEN s.price END ASC,
                    CASE WHEN :priceSort = 'DESC' THEN s.price END DESC,
                    s.service_id ASC
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM services s
                WHERE s.is_deleted = 0
                    AND s.is_available = 1
                    AND s.category_id = 2
                    AND (
                        :priceFilter = 'ALL'
                        OR (:priceFilter = 'UNDER_200' AND s.price < 200000)
                        OR (:priceFilter = 'FROM_200_TO_500' AND s.price >= 200000 AND s.price <= 500000)
                        OR (:priceFilter = 'OVER_500' AND s.price > 500000)
                    )
                """,
            nativeQuery = true
    )
    Page<ServiceProjection> findListWellness(
            @Param("priceFilter") String priceFilter,
            @Param("priceSort") String priceSort,
            Pageable pageable);

    @Query(
            value = """
            SELECT
                s.service_id AS serviceId,
                s.name AS name,
                s.description AS description,
                s.price AS price,
                s.category_id AS categoryId,
                sc.name AS categoryName,
                img.image_url AS imageUrl
            FROM services s
            LEFT JOIN service_categories sc
                ON sc.service_category_id = s.category_id
            LEFT JOIN images img
                ON img.entity_type = 'SERVICE'
                AND img.entity_id = s.service_id
                AND img.is_primary = 1
            WHERE s.is_deleted = 0
              AND s.is_available = 1
              AND (
                    :category = 'ALL'
                    OR (:category = 'DINING' AND s.category_id = 1)
                    OR (:category = 'WELLNESS' AND s.category_id = 2)
                  )
              AND (
                    :priceFilter = 'ALL'
                    OR (:priceFilter = 'UNDER_200' AND s.price < 200000)
                    OR (:priceFilter = 'FROM_200_TO_500' AND s.price >= 200000 AND s.price <= 500000)
                    OR (:priceFilter = 'OVER_500' AND s.price > 500000)
                  )
            ORDER BY
                CASE WHEN :sort = 'priceAsc' THEN s.price END ASC,
                CASE WHEN :sort = 'priceDesc' THEN s.price END DESC,
                s.service_id ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM services s
            WHERE s.is_deleted = 0
              AND s.is_available = 1
              AND (
                    :category = 'ALL'
                    OR (:category = 'DINING' AND s.category_id = 1)
                    OR (:category = 'WELLNESS' AND s.category_id = 2)
                  )
              AND (
                    :priceFilter = 'ALL'
                    OR (:priceFilter = 'UNDER_200' AND s.price < 200000)
                    OR (:priceFilter = 'FROM_200_TO_500' AND s.price >= 200000 AND s.price <= 500000)
                    OR (:priceFilter = 'OVER_500' AND s.price > 500000)
                  )
            """,
            nativeQuery = true
    )
    Page<BookingServiceProjection> findBookingServices(
            @Param("category") String category,
            @Param("sort") String sort,
            @Param("priceFilter") String priceFilter,
            Pageable pageable
    );
    @Query("""
    SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
    FROM Service s
    WHERE s.isDeleted = false
      AND LOWER(TRIM(s.name)) = LOWER(:name)
""")
    boolean existsActiveByNormalizedName(@Param("name") String name);

    @Query("""
    SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
    FROM Service s
    WHERE s.isDeleted = false
      AND s.id <> :serviceId
      AND LOWER(TRIM(s.name)) = LOWER(:name)
""")
    boolean existsActiveByNormalizedNameAndIdNot(@Param("name") String name,
                                                 @Param("serviceId") Long serviceId);

}