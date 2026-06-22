package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ServiceCategoryType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeService;
// IMPORT CHÍNH XÁC ENTITY SERVICE CỦA BẠN Ở ĐÂY, VÍ DỤ:
// import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.BookingServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.ServiceProjection;
import org.springframework.data.domain.Page;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface ServiceRepository extends JpaRepository<com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service, Long> {

    List<com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service>
    findByIsDeletedFalseAndIsAvailableTrueOrderByNameAsc();

    @Query("""
            SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeService(
                s.id,
                s.name,
                s.description,
                s.price,
                c.type,
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
              AND s.isAvailable = true
              AND c.isDeleted = false
            ORDER BY s.id DESC
            """)
    List<HomeService> findServiceForHome(@Param("entityType") ImageEntityType entityType,
                                         Pageable pageable);

    @Query("""
            SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.HomeService(
                s.id,
                s.name,
                s.description,
                s.price,
                c.type,
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
              AND s.isAvailable = true
              AND c.isDeleted = false
              AND c.type = :categoryType
            ORDER BY s.id DESC
            """)
    List<HomeService> findServiceByCategoryType(@Param("categoryType") ServiceCategoryType serviceCategoryType,
                                                @Param("entityType") ImageEntityType imageEntityType,
                                                Pageable pageable);

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
}