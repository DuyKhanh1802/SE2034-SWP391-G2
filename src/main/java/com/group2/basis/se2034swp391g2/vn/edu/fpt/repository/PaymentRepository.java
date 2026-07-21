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
    SELECT p
    FROM Payment p
    LEFT JOIN FETCH p.booking
    WHERE p.id = :id
    """)
    Optional<Payment> findDetailById(@Param("id") Long id);

    @Query("""
    SELECT p.id, p.method
    FROM Payment p
    WHERE p.id IN :paymentIds
    """)
    List<Object[]> findPaymentMethodsByIds(@Param("paymentIds") Collection<Long> paymentIds);

    Optional<Payment> findByTransactionRef(String transactionRef);

}
