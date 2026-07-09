package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FolioItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceReportRowResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FolioItemRepository extends JpaRepository<FolioItem, Long> {

    List<FolioItem> findByBookingIdAndIsVoidedFalseOrderByPostedAtAsc(Long bookingId);

    default List<Object[]> findTopServiceSales(Pageable pageable) {
        return findTopServiceSalesExcludingStatus(FolioItemStatus.CANCELLED, pageable);
    }

    @Query("""
            SELECT f.description, COALESCE(SUM(f.quantity), 0), COALESCE(SUM(f.amount), 0)
            FROM FolioItem f
            WHERE f.isVoided = false
            AND f.service IS NOT NULL
            AND f.serviceStatus <> :excludedStatus
            GROUP BY f.description
            ORDER BY COALESCE(SUM(f.quantity), 0) DESC
            """)
    List<Object[]> findTopServiceSalesExcludingStatus(@Param("excludedStatus") FolioItemStatus excludedStatus,
                                                      Pageable pageable);

    @Query("""
            SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceReportRowResponse(
                s.id,
                s.name,
                c.name,
                SUM(f.quantity),
                COALESCE(SUM(f.baseAmount), 0),
                COALESCE(SUM(f.serviceChargeAmount), 0),
                COALESCE(SUM(f.vatAmount), 0),
                COALESCE(SUM(f.totalAmount), 0),
                MAX(f.postedAt)
            )
            FROM FolioItem f
            JOIN f.service s
            JOIN s.category c
            WHERE f.isVoided = false
              AND f.serviceStatus <> :excludedStatus
              AND f.postedAt >= :fromInstant
              AND f.postedAt < :toExclusiveInstant
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:keyword IS NULL OR LOWER(s.name) LIKE CONCAT('%', :keyword, '%'))
            GROUP BY s.id, s.name, c.name
            """)
    List<ServiceReportRowResponse> findServiceReportRows(@Param("fromInstant") Instant fromInstant,
                                                         @Param("toExclusiveInstant") Instant toExclusiveInstant,
                                                         @Param("categoryId") Long categoryId,
                                                         @Param("keyword") String keyword,
                                                         @Param("excludedStatus") FolioItemStatus excludedStatus);

    List<FolioItem> findByBookingDetail_IdAndServiceIsNotNullAndIsVoidedFalseOrderByPostedAtAsc(Long bookingDetailId);

    Optional<FolioItem> findByBookingDetail_IdAndService_IdAndIsVoidedFalse(Long bookingDetailId, Long serviceId);

    Optional<FolioItem> findByIdAndBookingDetail_IdAndServiceIsNotNullAndIsVoidedFalse(Long folioItemId, Long bookingDetailId);
}