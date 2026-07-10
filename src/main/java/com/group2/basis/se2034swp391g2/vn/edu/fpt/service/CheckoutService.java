package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.CheckoutRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckoutDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final FolioItemRepository folioItemRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public CheckoutDetailResponse getCheckoutDetail(Long bookingDetailId) {
        BookingDetail detail = getBookingDetail(bookingDetailId);
        Booking booking = getActiveBooking(detail);
        List<BookingDetail> bookingDetails = bookingDetailRepository.findDetailsWithRoomsByBookingId(booking.getId());
        List<FolioItem> folioItems = folioItemRepository
                .findByBookingDetail_IdAndIsVoidedFalseOrderByPostedAtAsc(detail.getId());
        List<PaymentAllocation> allocations = paymentAllocationRepository.findByBookingDetailId(detail.getId());

        return toResponse(booking, detail, bookingDetails, folioItems, allocations);
    }

    @Transactional(readOnly = true)
    public Long findFirstCheckoutDetailId(Long bookingId) {
        Booking booking = bookingRepository.findByIdAndIsDeletedFalse(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        return bookingDetailRepository.findDetailsWithRoomsByBookingId(booking.getId()).stream()
                .filter(detail -> canCheckoutTarget(detail, booking))
                .map(BookingDetail::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Booking không còn phòng nào đang ở để checkout."));
    }

    @Transactional
    public void completeCheckout(Long bookingDetailId, CheckoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin trả phòng.");
        }

        BookingDetail detail = getBookingDetail(bookingDetailId);
        Booking booking = getActiveBooking(detail);
        validateCheckoutTarget(detail, booking);
        if (resolveStayStatus(detail, booking) != BookingDetailStatus.CHECKED_IN) {
            throw new IllegalStateException("Chỉ có thể trả phòng cho phòng đang ở trạng thái đã nhận phòng.");
        }

        List<FolioItem> folioItems = folioItemRepository
                .findByBookingDetail_IdAndIsVoidedFalseOrderByPostedAtAsc(detail.getId());
        List<PaymentAllocation> allocations = paymentAllocationRepository.findByBookingDetailId(detail.getId());

        BigDecimal totalAmount = calculateRoomFolioTotal(detail, folioItems);
        BigDecimal paidAmount = calculateAllocatedPaidAmount(allocations);
        BigDecimal balance = totalAmount.subtract(paidAmount).setScale(0, RoundingMode.HALF_UP);
        BigDecimal paymentAmount = money(request.getPaymentAmount());
        BigDecimal refundAmount = money(request.getRefundAmount());
        User currentStaff = getCurrentStaffUser();

        validateSettlementRequest(balance, paymentAmount, refundAmount, request);

        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentAmount.compareTo(balance) != 0) {
                throw new IllegalArgumentException("Số tiền thanh toán checkout phải bằng số dư còn lại của phòng.");
            }
            if (request.getPaymentMethod() == null) {
                throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
            }
            paymentService.createPayment(booking, detail, PaymentType.BALANCE, request.getPaymentMethod(), paymentAmount, currentStaff);
        } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal expectedRefund = balance.abs();
            if (refundAmount.compareTo(expectedRefund) != 0) {
                throw new IllegalArgumentException("Số tiền hoàn phải bằng số tiền khách đã trả dư cho phòng.");
            }
            Payment originalPayment = allocations.stream()
                    .map(PaymentAllocation::getPayment)
                    .filter(Objects::nonNull)
                    .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                    .filter(payment -> payment.getPaymentType() != PaymentType.REFUND)
                    .max(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch gốc của phòng để hoàn tiền."));
            if (request.getRefundMethod() == null) {
                throw new IllegalArgumentException("Vui lòng chọn phương thức hoàn tiền.");
            }
            paymentService.createRefundPayment(booking, detail, originalPayment, request.getRefundMethod(), expectedRefund, currentStaff);
        } else if (paymentAmount.compareTo(BigDecimal.ZERO) > 0 || refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Phòng đã cân bằng thanh toán, không cần ghi thêm giao dịch.");
        }

        RoomStatus nextRoomStatus = RoomStatus.MAINTENANCE;

        Room room = detail.getRoom();
        if (room != null) {
            room.setStatus(nextRoomStatus);
            room.setNote(normalizeNote(request.getNote(), nextRoomStatus));
        }

        detail.setStayStatus(BookingDetailStatus.CHECKED_OUT);
        detail.setActualCheckoutAt(Instant.now());
        detail.setCheckedOutBy(currentStaff);

        updateBookingCheckoutStatus(booking);
    }

    private CheckoutDetailResponse toResponse(Booking booking,
                                              BookingDetail detail,
                                              List<BookingDetail> bookingDetails,
                                              List<FolioItem> folioItems,
                                              List<PaymentAllocation> allocations) {
        BigDecimal totalAmount = calculateRoomFolioTotal(detail, folioItems);
        BigDecimal netPaid = calculateAllocatedPaidAmount(allocations);
        BigDecimal balance = totalAmount.subtract(netPaid).setScale(0, RoundingMode.HALF_UP);
        BigDecimal payable = balance.max(BigDecimal.ZERO);
        BigDecimal refundable = balance.compareTo(BigDecimal.ZERO) < 0 ? balance.abs() : BigDecimal.ZERO;
        boolean canCheckout = canCheckoutTarget(detail, booking);
        Room room = detail.getRoom();
        RoomTypeVariant variant = detail.getVariant();

        return CheckoutDetailResponse.builder()
                .bookingId(booking.getId())
                .bookingDetailId(detail.getId())
                .bookingReference(booking.getBookingReference())
                .guestName(buildGuestName(booking))
                .guestPhone(booking.getGuestPhone())
                .guestEmail(booking.getGuestEmail())
                .roomNumber(room == null ? "Chưa phân phòng" : room.getRoomNumber())
                .roomTypeName(variant != null && variant.getRoomType() != null ? variant.getRoomType().getName() : "N/A")
                .variantName(variant == null ? "N/A" : variant.getVariantName())
                .roomCount(bookingDetails == null ? 1 : bookingDetails.size())
                .checkInDate(detail.getCheckInDate())
                .checkOutDate(detail.getCheckOutDate())
                .bookingStatus(booking.getStatus() == null ? "" : booking.getStatus().name())
                .bookingStatusLabel(booking.getStatus() == null ? "N/A" : booking.getStatus().getLabel())
                .roomSubtotal(money(detail.getTotalAmount()))
                .serviceSubtotal(calculateFolioSubtotal(folioItems))
                .serviceChargeTotal(calculateFolioServiceCharge(folioItems))
                .vatTotal(calculateFolioVat(folioItems))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .paidAmount(netPaid)
                .balanceAmount(payable)
                .refundAmount(refundable)
                .settlementType(resolveSettlementType(balance))
                .paymentStatusLabel(resolvePaymentStatusLabel(totalAmount, netPaid))
                .canCheckout(canCheckout)
                .blockReason(canCheckout ? null : resolveCheckoutBlockReason(detail, booking))
                .folioLines(folioItems.stream().map(this::toFolioLine).toList())
                .payments(allocations.stream()
                        .map(PaymentAllocation::getPayment)
                        .filter(Objects::nonNull)
                        .distinct()
                        .map(this::toPaymentLine)
                        .toList())
                .build();
    }

    private void validateCheckoutTarget(BookingDetail detail, Booking booking) {
        if (!canCheckoutTarget(detail, booking)) {
            throw new IllegalStateException(resolveCheckoutBlockReason(detail, booking));
        }
    }

    private boolean canCheckoutTarget(BookingDetail detail, Booking booking) {
        BookingStatus bookingStatus = booking.getStatus();
        if (bookingStatus != BookingStatus.CHECKED_IN
                && bookingStatus != BookingStatus.PARTIALLY_CHECKED_IN
                && bookingStatus != BookingStatus.PARTIALLY_CHECKED_OUT) {
            return false;
        }

        if (resolveStayStatus(detail, booking) != BookingDetailStatus.CHECKED_IN) {
            return false;
        }

        Room room = detail.getRoom();
        return room != null && room.getStatus() == RoomStatus.OCCUPIED;
    }

    private String resolveCheckoutBlockReason(BookingDetail detail, Booking booking) {
        BookingStatus bookingStatus = booking.getStatus();
        if (bookingStatus != BookingStatus.CHECKED_IN
                && bookingStatus != BookingStatus.PARTIALLY_CHECKED_IN
                && bookingStatus != BookingStatus.PARTIALLY_CHECKED_OUT) {
            return "Booking không ở trạng thái cho phép trả phòng.";
        }

        if (resolveStayStatus(detail, booking) != BookingDetailStatus.CHECKED_IN) {
            return "Chỉ có thể trả phòng cho phòng đang ở trạng thái đã nhận phòng.";
        }

        Room room = detail.getRoom();
        if (room == null) {
            return "Phòng này chưa được gán phòng thực tế nên không thể checkout.";
        }

        return "Chỉ có thể checkout phòng đang ở trạng thái Đang ở.";
    }

    private void validateSettlementRequest(BigDecimal balance,
                                           BigDecimal paymentAmount,
                                           BigDecimal refundAmount,
                                           CheckoutRequest request) {
        if (paymentAmount.compareTo(BigDecimal.ZERO) < 0 || refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền thanh toán hoặc hoàn tiền không được âm.");
        }

        if (paymentAmount.compareTo(BigDecimal.ZERO) > 0 && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Không thể vừa thu thêm vừa hoàn tiền trong cùng một lần checkout.");
        }

        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("Phòng còn thiếu tiền, không thể ghi hoàn tiền.");
            }
            if (paymentAmount.compareTo(balance) != 0) {
                throw new IllegalArgumentException("Số tiền thanh toán checkout phải bằng số dư còn lại của phòng.");
            }
            validateReceptionistPaymentMethod(request.getPaymentMethod(), "thanh toán");
            return;
        }

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal expectedRefund = balance.abs();
            if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("Phòng đang dư tiền, không thể ghi thu thêm.");
            }
            if (refundAmount.compareTo(expectedRefund) != 0) {
                throw new IllegalArgumentException("Số tiền hoàn phải bằng số tiền khách đã trả dư cho phòng.");
            }
            validateReceptionistPaymentMethod(request.getRefundMethod(), "hoàn tiền");
            return;
        }

        if (paymentAmount.compareTo(BigDecimal.ZERO) > 0 || refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Phòng đã cân bằng thanh toán, không cần ghi thêm giao dịch.");
        }
    }

    private void validateReceptionistPaymentMethod(PaymentMethod method, String actionLabel) {
        if (method == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức " + actionLabel + ".");
        }
        if (method == PaymentMethod.VNPAY) {
            throw new IllegalArgumentException("Checkout tại lễ tân chưa xử lý qua cổng VNPAY, vui lòng chọn tiền mặt, thẻ hoặc chuyển khoản.");
        }
    }

    private void updateBookingCheckoutStatus(Booking booking) {
        List<BookingDetail> details = bookingDetailRepository.findDetailsWithRoomsByBookingId(booking.getId());
        boolean allCheckedOut = details.stream()
                .map(detail -> resolveStayStatus(detail, booking))
                .allMatch(status -> status == BookingDetailStatus.CHECKED_OUT || status == BookingDetailStatus.CANCELLED);

        if (allCheckedOut) {
            booking.setStatus(BookingStatus.CHECKED_OUT);
            booking.setActualCheckoutAt(Instant.now());
            updateGuestLastStayAt(booking, booking.getActualCheckoutAt());
        } else {
            booking.setStatus(BookingStatus.PARTIALLY_CHECKED_OUT);
        }

        bookingRepository.save(booking);
    }

    private void updateGuestLastStayAt(Booking booking, Instant checkoutAt) {
        User guest = resolveGuestUser(booking);
        if (guest != null) {
            booking.setGuest(guest);
            guest.setLastStayAt(checkoutAt);
        }
    }

    private User resolveGuestUser(Booking booking) {
        User linkedGuest = booking.getGuest();
        if (isGuestUser(linkedGuest)) {
            return linkedGuest;
        }

        String email = normalizeLookup(booking.getGuestEmail());
        if (email != null) {
            User guestByEmail = userRepository.findByEmailAndIsDeletedFalse(email)
                    .filter(this::isGuestUser)
                    .orElse(null);
            if (guestByEmail != null) {
                return guestByEmail;
            }
        }

        String phone = normalizeLookup(booking.getGuestPhone());
        if (phone != null) {
            return userRepository.findByPhoneAndIsDeletedFalse(phone)
                    .filter(this::isGuestUser)
                    .orElse(null);
        }

        return null;
    }

    private boolean isGuestUser(User user) {
        return user != null && user.getUserType() == UserType.GUEST;
    }

    private String normalizeLookup(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private BookingDetail getBookingDetail(Long bookingDetailId) {
        if (bookingDetailId == null) {
            throw new IllegalArgumentException("Thiếu mã phòng cần checkout.");
        }
        return bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng trong booking."));
    }

    private Booking getActiveBooking(BookingDetail detail) {
        Booking booking = detail.getBooking();
        if (booking == null || booking.getId() == null || Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Không tìm thấy đặt phòng.");
        }
        return booking;
    }

    private BookingDetailStatus resolveStayStatus(BookingDetail detail, Booking booking) {
        if (detail.getStayStatus() != null) {
            return detail.getStayStatus();
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.PARTIALLY_CHECKED_IN
                || booking.getStatus() == BookingStatus.PARTIALLY_CHECKED_OUT) {
            return BookingDetailStatus.CHECKED_IN;
        }
        if (booking.getStatus() == BookingStatus.CHECKED_OUT) {
            return BookingDetailStatus.CHECKED_OUT;
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.NO_SHOW) {
            return BookingDetailStatus.CANCELLED;
        }
        return BookingDetailStatus.RESERVED;
    }

    private BigDecimal calculateRoomFolioTotal(BookingDetail detail, List<FolioItem> folioItems) {
        return money(detail.getTotalAmount()).add(
                folioItems.stream()
                        .map(FolioItem::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        ).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFolioSubtotal(List<FolioItem> folioItems) {
        return folioItems.stream()
                .map(FolioItem::getBaseAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFolioServiceCharge(List<FolioItem> folioItems) {
        return folioItems.stream()
                .map(FolioItem::getServiceChargeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFolioVat(List<FolioItem> folioItems) {
        return folioItems.stream()
                .map(FolioItem::getVatAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAllocatedPaidAmount(List<PaymentAllocation> allocations) {
        BigDecimal collected = allocations.stream()
                .filter(allocation -> allocation.getPayment() != null)
                .filter(allocation -> allocation.getPayment().getStatus() == PaymentStatus.SUCCESS)
                .filter(allocation -> allocation.getPayment().getPaymentType() != PaymentType.REFUND)
                .map(PaymentAllocation::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunded = allocations.stream()
                .filter(allocation -> allocation.getPayment() != null)
                .filter(allocation -> allocation.getPayment().getStatus() == PaymentStatus.SUCCESS)
                .filter(allocation -> allocation.getPayment().getPaymentType() == PaymentType.REFUND)
                .map(PaymentAllocation::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return collected.subtract(refunded).setScale(0, RoundingMode.HALF_UP);
    }

    private CheckoutDetailResponse.FolioLine toFolioLine(FolioItem item) {
        FolioItemStatus status = item.getServiceStatus() == null ? FolioItemStatus.COMPLETED : item.getServiceStatus();
        return CheckoutDetailResponse.FolioLine.builder()
                .itemType(item.getItemType() == null ? "N/A" : item.getItemType().name())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(money(item.getUnitPrice()))
                .amount(money(item.getAmount()))
                .statusLabel(status.getLabel())
                .build();
    }

    private CheckoutDetailResponse.PaymentLine toPaymentLine(Payment payment) {
        return CheckoutDetailResponse.PaymentLine.builder()
                .transactionRef(payment.getTransactionRef())
                .paymentType(payment.getPaymentType() == null ? "N/A" : payment.getPaymentType().getLabel())
                .method(payment.getMethod() == null ? "N/A" : payment.getMethod().getLabel())
                .status(payment.getStatus() == null ? "N/A" : payment.getStatus().getLabel())
                .amount(money(payment.getAmount()))
                .paidAt(payment.getPaidAt())
                .build();
    }

    private String resolveSettlementType(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            return "PAYMENT";
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            return "REFUND";
        }
        return "SETTLED";
    }

    private String resolvePaymentStatusLabel(BigDecimal totalAmount, BigDecimal paidAmount) {
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Chưa thanh toán";
        }
        if (paidAmount.compareTo(totalAmount) >= 0) {
            return "Đã thanh toán";
        }
        return "Thanh toán một phần";
    }

    private String buildGuestName(Booking booking) {
        String name = ((booking.getGuestLastName() == null ? "" : booking.getGuestLastName()) + " "
                + (booking.getGuestFirstName() == null ? "" : booking.getGuestFirstName())).trim();
        return name.isBlank() ? booking.getGuestEmail() : name;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
    }

    private String normalizeNote(String note, RoomStatus status) {
        String value = note == null ? "" : note.trim();
        if (status == RoomStatus.MAINTENANCE && value.isBlank()) {
            return "Cần dọn phòng sau checkout";
        }
        return value.isBlank() ? null : value;
    }

    private User getCurrentStaffUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên đang đăng nhập."));
    }
}
