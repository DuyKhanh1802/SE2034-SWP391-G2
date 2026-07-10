package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    List<PaymentAllocation> findByBookingDetailId(Long bookingDetailId);

    @Query("""
        SELECT COALESCE(SUM(pa.amount), 0)
        FROM PaymentAllocation pa
        JOIN pa.payment p
        WHERE pa.bookingDetail.id = :bookingDetailId
          AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS
          AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND
        """)
    BigDecimal sumSuccessfulCollectionsByBookingDetailId(@Param("bookingDetailId") Long bookingDetailId);

    @Query("""
        SELECT COALESCE(SUM(pa.amount), 0)
        FROM PaymentAllocation pa
        JOIN pa.payment p
        WHERE pa.bookingDetail.id = :bookingDetailId
          AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS
          AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND
        """)
    BigDecimal sumSuccessfulRefundsByBookingDetailId(@Param("bookingDetailId") Long bookingDetailId);
}
