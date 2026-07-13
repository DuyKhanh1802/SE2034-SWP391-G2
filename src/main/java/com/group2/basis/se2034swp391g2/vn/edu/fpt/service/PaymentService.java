package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils.PaymentCodeGenerator;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.PaymentApplication;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PaymentApplicationRepository;
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
    private final PaymentApplicationRepository paymentApplicationRepository;
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
    public Payment createPayment(Booking booking,
                                 BookingDetail bookingDetail,
                                 PaymentType paymentType,
                                 PaymentMethod method,
                                 BigDecimal amount,
                                 User currentStaff) {
        Payment savedPayment = createPayment(booking, paymentType, method, amount, currentStaff);
        createApplication(savedPayment, booking, bookingDetail, amount);
        return savedPayment;
    }

    @Transactional
    public Payment createPendingTransferPayment(Booking booking,
                                                BookingDetail bookingDetail,
                                                PaymentType paymentType,
                                                BigDecimal amount,
                                                User currentStaff) {
        validatePaymentInput(booking, paymentType, PaymentMethod.TRANSFER, amount, currentStaff);
        if (bookingDetail == null) {
            throw new IllegalArgumentException("Phòng áp dụng thanh toán không được để trống.");
        }
        if (paymentType != PaymentType.BALANCE) {
            throw new IllegalArgumentException("Bill chuyển khoản tại checkout chỉ được dùng cho phần tiền còn lại.");
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentType(paymentType)
                .method(PaymentMethod.TRANSFER)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .transactionRef(generateUniqueTransactionRef(paymentType))
                .processedBy(currentStaff)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        createApplication(savedPayment, booking, bookingDetail, amount);
        return savedPayment;
    }

    @Transactional
    public Payment completePendingTransferPayment(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            throw new IllegalArgumentException("Mã giao dịch không hợp lệ.");
        }

        Payment payment = paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán."));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            cashTransactionService.createFromPayment(payment);
            return payment;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Giao dịch không còn ở trạng thái chờ thanh toán.");
        }

        if (payment.getMethod() != PaymentMethod.TRANSFER) {
            throw new IllegalStateException("Chỉ giao dịch chuyển khoản mới được xác nhận qua cổng thanh toán.");
        }

        if (payment.getPaymentType() != PaymentType.BALANCE) {
            throw new IllegalStateException("Giao dịch này không phải bill thanh toán checkout.");
        }

        BigDecimal appliedAmount = paymentApplicationRepository.sumAppliedAmountByPaymentId(payment.getId());
        BigDecimal paymentAmount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        if (appliedAmount.compareTo(paymentAmount) != 0) {
            throw new IllegalStateException("Số tiền áp dụng của giao dịch không khớp với số tiền thanh toán.");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        Payment savedPayment = paymentRepository.save(payment);

        cashTransactionService.createFromPayment(savedPayment);

        return savedPayment;
    }

    @Transactional
    public void markPendingTransferPaymentFailed(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            throw new IllegalArgumentException("Mã giao dịch không hợp lệ.");
        }

        Payment payment = paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán."));

        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    @Transactional
    public Payment createRefundPayment(Booking booking,
                                       Payment originalPayment,
                                       BigDecimal refundAmount,
                                       User currentStaff) {

        return createRefundPayment(
                booking,
                originalPayment,
                originalPayment == null ? null : originalPayment.getMethod(),
                refundAmount,
                currentStaff
        );
    }

    @Transactional
    public Payment createRefundPayment(Booking booking,
                                       Payment originalPayment,
                                       PaymentMethod refundMethod,
                                       BigDecimal refundAmount,
                                       User currentStaff) {

        if (originalPayment == null) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch gốc để hoàn tiền.");
        }

        validatePaymentInput(
                booking,
                PaymentType.REFUND,
                refundMethod,
                refundAmount,
                currentStaff
        );

        Payment refundPayment = Payment.builder()
                .booking(booking)
                .paymentType(PaymentType.REFUND)
                .method(refundMethod)
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

    @Transactional
    public Payment createRefundPayment(Booking booking,
                                       BookingDetail bookingDetail,
                                       Payment originalPayment,
                                       PaymentMethod refundMethod,
                                       BigDecimal refundAmount,
                                       User currentStaff) {
        Payment savedRefundPayment = createRefundPayment(booking, originalPayment, refundMethod, refundAmount, currentStaff);
        createApplication(savedRefundPayment, booking, bookingDetail, refundAmount);
        return savedRefundPayment;
    }

    private void createApplication(Payment payment,
                                   Booking booking,
                                   BookingDetail bookingDetail,
                                   BigDecimal amount) {
        if (payment == null || payment.getId() == null) {
            throw new IllegalArgumentException("Giao dịch thanh toán không hợp lệ.");
        }

        if (bookingDetail == null) {
            throw new IllegalArgumentException("Phòng áp dụng thanh toán không được để trống.");
        }

        if (booking == null
                || booking.getId() == null
                || bookingDetail.getBooking() == null
                || !booking.getId().equals(bookingDetail.getBooking().getId())) {
            throw new IllegalArgumentException("Phòng áp dụng thanh toán không thuộc booking này.");
        }

        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount;
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền áp dụng phải lớn hơn 0.");
        }

        BigDecimal appliedAmount = paymentApplicationRepository.sumAppliedAmountByPaymentId(payment.getId());
        BigDecimal paymentAmount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        if (appliedAmount.add(normalizedAmount).compareTo(paymentAmount) > 0) {
            throw new IllegalArgumentException("Tổng tiền áp dụng không được vượt quá số tiền giao dịch.");
        }

        paymentApplicationRepository.save(PaymentApplication.builder()
                .payment(payment)
                .booking(booking)
                .bookingDetail(bookingDetail)
                .amount(normalizedAmount)
                .build());
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
