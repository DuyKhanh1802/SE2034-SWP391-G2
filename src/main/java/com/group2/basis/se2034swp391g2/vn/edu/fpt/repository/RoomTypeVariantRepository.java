package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomTypeVariantRepository extends JpaRepository<RoomTypeVariant, Long> {

    @Query("""
        SELECT v
        FROM RoomTypeVariant v
        JOIN FETCH v.roomType rt
        WHERE v.isDeleted = false
        ORDER BY rt.name ASC, v.variantName ASC
    """)
    List<RoomTypeVariant> findByIsDeletedFalseOrderByRoomType_NameAscVariantNameAsc();

    @Query(
            value = """
            SELECT v
            FROM RoomTypeVariant v
            JOIN v.roomType rt
            WHERE v.isDeleted = false
              AND (
                    :keyword IS NULL OR :keyword = ''
                    OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
              AND (:viewType IS NULL OR v.viewType = :viewType)
            ORDER BY rt.name ASC, v.variantName ASC
        """,
            countQuery = """
            SELECT COUNT(v)
            FROM RoomTypeVariant v
            JOIN v.roomType rt
            WHERE v.isDeleted = false
              AND (
                    :keyword IS NULL OR :keyword = ''
                    OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(rt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
              AND (:viewType IS NULL OR v.viewType = :viewType)
        """
    )
    Page<RoomTypeVariant> searchVariants(@Param("keyword") String keyword,
                                         @Param("viewType") ViewType viewType,
                                         Pageable pageable);
}