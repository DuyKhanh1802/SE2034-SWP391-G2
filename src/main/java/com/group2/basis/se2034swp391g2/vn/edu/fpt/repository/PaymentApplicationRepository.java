package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.PaymentApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentApplicationRepository extends JpaRepository<PaymentApplication, Long> {

    List<PaymentApplication> findByBookingDetailId(Long bookingDetailId);

    @Query("""
            SELECT COALESCE(SUM(application.amount), 0)
            FROM PaymentApplication application
            JOIN application.payment payment
            WHERE payment.id = :paymentId
            """)
    BigDecimal sumAppliedAmountByPaymentId(@Param("paymentId") Long paymentId);

}
