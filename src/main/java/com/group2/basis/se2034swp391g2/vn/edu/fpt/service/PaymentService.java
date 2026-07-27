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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Transactional
    public void applyPaymentToBookingDetails(Payment payment,
                                             Booking booking,
                                             List<BookingDetail> bookingDetails) {
        if (payment == null || payment.getId() == null || payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Giao dịch thành công không hợp lệ.");
        }
        if (booking == null || booking.getId() == null
                || payment.getBooking() == null
                || !booking.getId().equals(payment.getBooking().getId())) {
            throw new IllegalArgumentException("Giao dịch không thuộc booking này.");
        }
        if (bookingDetails == null || bookingDetails.isEmpty()) {
            throw new IllegalArgumentException("Booking chưa có phòng để phân bổ thanh toán.");
        }

        List<BookingDetail> orderedDetails = bookingDetails.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BookingDetail::getId))
                .toList();
        if (orderedDetails.isEmpty() || orderedDetails.stream().anyMatch(detail ->
                detail.getId() == null
                        || detail.getBooking() == null
                        || !booking.getId().equals(detail.getBooking().getId()))) {
            throw new IllegalArgumentException("Danh sách phòng không hợp lệ hoặc không thuộc booking.");
        }

        List<PaymentApplication> existingApplications =
                paymentApplicationRepository.findByPaymentId(payment.getId());
        Map<Long, BigDecimal> appliedByDetail = existingApplications.stream()
                .filter(application -> application.getBookingDetail() != null)
                .collect(Collectors.toMap(
                        application -> application.getBookingDetail().getId(),
                        application -> normalizeAmount(application.getAmount()),
                        BigDecimal::add
                ));

        BigDecimal paymentAmount = normalizeAmount(payment.getAmount());
        BigDecimal alreadyApplied = existingApplications.stream()
                .map(PaymentApplication::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingAmount = paymentAmount.subtract(alreadyApplied);
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal remainingCapacity = orderedDetails.stream()
                .map(detail -> normalizeAmount(detail.getTotalAmount())
                        .subtract(appliedByDetail.getOrDefault(detail.getId(), BigDecimal.ZERO))
                        .max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (remainingAmount.compareTo(remainingCapacity) > 0) {
            throw new IllegalStateException("Số tiền giao dịch vượt quá tổng tiền chưa phân bổ của các phòng.");
        }

        List<PaymentApplication> newApplications = new ArrayList<>();
        for (BookingDetail detail : orderedDetails) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal detailCapacity = normalizeAmount(detail.getTotalAmount())
                    .subtract(appliedByDetail.getOrDefault(detail.getId(), BigDecimal.ZERO))
                    .max(BigDecimal.ZERO);
            BigDecimal appliedAmount = remainingAmount.min(detailCapacity);
            if (appliedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            newApplications.add(PaymentApplication.builder()
                    .payment(payment)
                    .booking(booking)
                    .bookingDetail(detail)
                    .amount(appliedAmount)
                    .build());
            remainingAmount = remainingAmount.subtract(appliedAmount);
        }

        paymentApplicationRepository.saveAll(newApplications);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
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
