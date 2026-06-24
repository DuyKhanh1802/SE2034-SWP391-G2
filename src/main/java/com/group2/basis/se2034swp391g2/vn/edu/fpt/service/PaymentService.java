package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils.PaymentCodeGenerator;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CashTransactionService cashTransactionService;

    @Transactional
    public Payment createPayment(Booking booking,
                                 PaymentType paymentType,
                                 PaymentMethod method,
                                 BigDecimal amount,
                                 User currentStaff) {

        validatePaymentInput(booking, paymentType, method, amount, currentStaff);

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentType(paymentType)
                .method(method)
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .transactionRef(generateUniqueTransactionRef(paymentType))
                .processedBy(currentStaff)
                .paidAt(Instant.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        cashTransactionService.createFromPayment(savedPayment);

        return savedPayment;
    }

    @Transactional
    public Payment createRefundPayment(Booking booking,
                                       Payment originalPayment,
                                       BigDecimal refundAmount,
                                       User currentStaff) {

        if (originalPayment == null) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch gốc để hoàn tiền.");
        }

        validatePaymentInput(
                booking,
                PaymentType.REFUND,
                originalPayment.getMethod(),
                refundAmount,
                currentStaff
        );

        Payment refundPayment = Payment.builder()
                .booking(booking)
                .paymentType(PaymentType.REFUND)
                .method(originalPayment.getMethod())
                .amount(refundAmount)
                .status(PaymentStatus.SUCCESS)
                .transactionRef(generateUniqueTransactionRef(PaymentType.REFUND))
                .processedBy(currentStaff)
                .paidAt(Instant.now())
                .originalPayment(originalPayment)
                .build();

        Payment savedRefundPayment = paymentRepository.save(refundPayment);

        cashTransactionService.createFromPayment(savedRefundPayment);

        return savedRefundPayment;
    }

    private String generateUniqueTransactionRef(PaymentType paymentType) {
        String transactionRef;

        do {
            transactionRef = PaymentCodeGenerator.generate(paymentType);
        } while (paymentRepository.existsByTransactionRef(transactionRef));

        return transactionRef;
    }

    private void validatePaymentInput(Booking booking,
                                      PaymentType paymentType,
                                      PaymentMethod method,
                                      BigDecimal amount,
                                      User currentStaff) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking không được để trống.");
        }

        if (paymentType == null) {
            throw new IllegalArgumentException("Loại thanh toán không được để trống.");
        }

        if (method == null) {
            throw new IllegalArgumentException("Phương thức thanh toán không được để trống.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        }

        if (currentStaff == null) {
            throw new IllegalArgumentException("Nhân viên xử lý không được để trống.");
        }
    }
}