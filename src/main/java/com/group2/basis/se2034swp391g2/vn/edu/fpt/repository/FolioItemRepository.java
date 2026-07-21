package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FolioItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistNotificationView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceReportRowResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

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
            SELECT f.description, COALESCE(SUM(f.quantity), 0), COALESCE(SUM(f.totalAmount), 0)
            FROM FolioItem f
            WHERE f.isVoided = false
            AND f.service IS NOT NULL
            AND f.serviceStatus <> :excludedStatus
            GROUP BY f.description
            ORDER BY COALESCE(SUM(f.quantity), 0) DESC
            """)
    List<Object[]> findTopServiceSalesExcludingStatus(@Param("excludedStatus") FolioItemStatus excludedStatus,
                                                      Pageable pageable);

    @SuppressWarnings("JpaQlInspection")
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

    List<FolioItem> findByBookingDetail_IdAndIsVoidedFalseOrderByPostedAtAsc(Long bookingDetailId);

    @Query("""
            SELECT COUNT(f) > 0
            FROM FolioItem f
            WHERE f.bookingDetail.id = :bookingDetailId
              AND f.service IS NOT NULL
              AND f.isVoided = false
              AND (f.serviceStatus IS NULL OR f.serviceStatus = :serviceStatus)
            """)
    boolean existsUnresolvedService(@Param("bookingDetailId") Long bookingDetailId,
                                    @Param("serviceStatus") FolioItemStatus serviceStatus);

    @Query("""
            SELECT COUNT(f)
            FROM FolioItem f
            JOIN f.booking b
            JOIN f.bookingDetail bd
            WHERE f.service IS NOT NULL
              AND f.isVoided = false
              AND (f.serviceStatus IS NULL OR f.serviceStatus = :serviceStatus)
              AND b.isDeleted = false
              AND bd.actualCheckinAt IS NOT NULL
              AND bd.actualCheckoutAt IS NULL
            """)
    long countActiveServiceRequests(@Param("serviceStatus") FolioItemStatus serviceStatus);

    @SuppressWarnings("JpaQlInspection")
    @Query("""
            SELECT new com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ReceptionistNotificationView$ServiceRequestRow(
                f.id,
                b.id,
                bd.id,
                b.bookingReference,
                CONCAT(b.guestLastName, ' ', b.guestFirstName),
                r.roomNumber,
                f.description,
                f.quantity,
                f.postedAt
            )
            FROM FolioItem f
            JOIN f.booking b
            JOIN f.bookingDetail bd
            LEFT JOIN bd.room r
            WHERE f.service IS NOT NULL
              AND f.isVoided = false
              AND (f.serviceStatus IS NULL OR f.serviceStatus = :serviceStatus)
              AND b.isDeleted = false
              AND bd.actualCheckinAt IS NOT NULL
              AND bd.actualCheckoutAt IS NULL
            ORDER BY f.postedAt DESC, f.id DESC
            """)
    List<ReceptionistNotificationView.ServiceRequestRow> findRecentActiveServiceRequests(
            @Param("serviceStatus") FolioItemStatus serviceStatus,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT f
            FROM FolioItem f
            WHERE f.id = :folioItemId
              AND f.bookingDetail.id = :bookingDetailId
              AND f.service IS NOT NULL
              AND f.isVoided = false
            """)
    Optional<FolioItem> findServiceItemForUpdate(@Param("folioItemId") Long folioItemId,
                                                 @Param("bookingDetailId") Long bookingDetailId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FolioItem f WHERE f.id = :folioItemId")
    Optional<FolioItem> findByIdForUpdate(@Param("folioItemId") Long folioItemId);
}
