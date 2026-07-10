package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByTransactionRef(String transactionRef);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findFirstByBookingIdAndPaymentTypeAndStatus(
            Long bookingId,
            PaymentType paymentType,
            PaymentStatus status
    );

    @Query("""
    SELECT p.booking.id, COALESCE(SUM(p.amount), 0)
    FROM Payment p
    WHERE p.booking.id IN :bookingIds
      AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS
      AND p.paymentType <> com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND
    GROUP BY p.booking.id
    """)
    List<Object[]> findSuccessfulCollectionTotalsByBookingIds(@Param("bookingIds") Collection<Long> bookingIds);

    @Query("""
    SELECT p.booking.id, COALESCE(SUM(p.amount), 0)
    FROM Payment p
    WHERE p.booking.id IN :bookingIds
      AND p.status = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus.SUCCESS
      AND p.paymentType = com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType.REFUND
    GROUP BY p.booking.id
    """)
    List<Object[]> findSuccessfulRefundTotalsByBookingIds(@Param("bookingIds") Collection<Long> bookingIds);

}
