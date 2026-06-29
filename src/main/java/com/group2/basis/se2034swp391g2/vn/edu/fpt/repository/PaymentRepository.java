package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByTransactionRef(String transactionRef);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findFirstByBookingIdAndPaymentTypeAndStatusOrderByPaidAtDesc(
            Long bookingId,
            PaymentType paymentType,
            PaymentStatus status
    );

    List<Payment> findByBooking_IdAndStatusOrderByPaidAtAsc(Long bookingId, PaymentStatus status);
}