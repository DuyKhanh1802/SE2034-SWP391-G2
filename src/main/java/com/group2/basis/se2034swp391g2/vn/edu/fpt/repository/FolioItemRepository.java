package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FolioItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface FolioItemRepository extends JpaRepository<FolioItem, Long> {
    List<FolioItem> findByBookingIdAndIsVoidedFalseOrderByPostedAtAsc(Long bookingId);

    @Query("""
            SELECT f.description, COALESCE(SUM(f.quantity), 0), COALESCE(SUM(f.amount), 0)
            FROM FolioItem f
            WHERE f.isVoided = false
            AND f.service IS NOT NULL
            GROUP BY f.description
            ORDER BY COALESCE(SUM(f.quantity), 0) DESC
            """)
    List<Object[]> findTopServiceSales(Pageable pageable);

    List<FolioItem> findByBookingDetail_IdAndServiceIsNotNullAndIsVoidedFalseOrderByPostedAtAsc(Long bookingDetailId);

    Optional<FolioItem> findByBookingDetail_IdAndService_IdAndIsVoidedFalse(Long bookingDetailId, Long serviceId);

    Optional<FolioItem> findByIdAndBookingDetail_IdAndServiceIsNotNullAndIsVoidedFalse(Long folioItemId, Long bookingDetailId);


}
