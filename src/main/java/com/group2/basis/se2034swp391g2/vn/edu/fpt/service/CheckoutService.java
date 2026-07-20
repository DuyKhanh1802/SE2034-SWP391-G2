package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.CheckoutRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckoutDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final FolioItemRepository folioItemRepository;
    private final PaymentApplicationRepository paymentApplicationRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Value("${vietqr.bank-code:BIDV}")
    private String vietQrBankCode;

    @Value("${vietqr.account-number:3711308057}")
    private String vietQrAccountNumber;

    @Value("${vietqr.account-name:VHotel}")
    private String vietQrAccountName;

    @Transactional(readOnly = true)
    public CheckoutDetailResponse getCheckoutDetail(Long bookingDetailId) {
        BookingDetail detail = getBookingDetail(bookingDetailId);
        Booking booking = getActiveBooking(detail);
        List<BookingDetail> bookingDetails = bookingDetailRepository.findDetailsWithRoomsByBookingId(booking.getId());
        List<FolioItem> folioItems = folioItemRepository
                .findByBookingDetail_IdAndIsVoidedFalseOrderByPostedAtAsc(detail.getId());
        List<PaymentApplication> applications = paymentApplicationRepository.findByBookingDetailId(detail.getId());

        return toResponse(booking, detail, bookingDetails, folioItems, applications);
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

        BookingDetail detail = getBookingDetailForUpdate(bookingDetailId);
        Booking booking = getActiveBooking(detail);
        validateCheckoutTarget(detail, booking);
        if (resolveStayStatus(detail, booking) != BookingDetailStatus.CHECKED_IN) {
            throw new IllegalStateException("Chỉ có thể trả phòng cho phòng đang ở trạng thái đã nhận phòng.");
        }
        if (hasRequestedServices(detail)) {
            throw new IllegalStateException("Vui lòng xử lý toàn bộ dịch vụ đang chờ trước khi checkout.");
        }

        List<FolioItem> folioItems = folioItemRepository
                .findByBookingDetail_IdAndIsVoidedFalseOrderByPostedAtAsc(detail.getId());
        List<PaymentApplication> applications = paymentApplicationRepository.findByBookingDetailId(detail.getId());

        BigDecimal totalAmount = calculateRoomFolioTotal(detail, folioItems);
        BigDecimal paidAmount = calculateAppliedPaidAmount(applications);
        BigDecimal balance = totalAmount.subtract(paidAmount).setScale(0, RoundingMode.HALF_UP);
        BigDecimal paymentAmount = money(request.getPaymentAmount());
        User currentStaff = getCurrentStaffUser();

        validateSettlementRequest(balance, paymentAmount, request.getPaymentMethod());

        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentAmount.compareTo(balance) != 0) {
                throw new IllegalArgumentException("Số tiền thanh toán checkout phải bằng số dư còn lại của phòng.");
            }
            if (request.getPaymentMethod() == null) {
                throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
            }
            paymentService.createPayment(booking, detail, PaymentType.BALANCE, request.getPaymentMethod(), paymentAmount, currentStaff);
        } else if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Phòng đã cân bằng thanh toán, không cần ghi thêm giao dịch.");
        }

        RoomStatus nextRoomStatus = RoomStatus.MAINTENANCE;

        Room room = detail.getRoom();
        if (room != null) {
            room.setStatus(nextRoomStatus);
            room.setNote(null);
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
                                              List<PaymentApplication> applications) {
        BigDecimal totalAmount = calculateRoomFolioTotal(detail, folioItems);
        BigDecimal netPaid = calculateAppliedPaidAmount(applications);
        BigDecimal balance = totalAmount.subtract(netPaid).setScale(0, RoundingMode.HALF_UP);
        BigDecimal payable = balance.max(BigDecimal.ZERO);
        boolean canCheckout = canCheckoutTarget(detail, booking) && !hasRequestedServices(detail);
        Room room = detail.getRoom();
        RoomTypeVariant variant = detail.getVariant();

        return CheckoutDetailResponse.builder()
                .bookingId(booking.getId())
                .bookingDetailId(detail.getId())
                .bookingReference(booking.getBookingReference())
                .guestName(buildGuestName(booking))
                .guestPhone(booking.getGuestPhone())
                .guestEmail(booking.getGuestEmail())
                .roomNumber(room == null ? "Không rõ" : room.getRoomNumber())
                .roomTypeName(variant != null && variant.getRoomType() != null ? variant.getRoomType().getName() : "N/A")
                .variantName(variant == null ? "N/A" : variant.getVariantName())
                .roomCount(bookingDetails == null ? 1 : bookingDetails.size())
                .checkInDate(detail.getCheckInDate())
                .checkOutDate(detail.getCheckOutDate())
                .actualCheckinAt(detail.getActualCheckinAt())
                .actualCheckoutAt(detail.getActualCheckoutAt())
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
                .settlementType(resolveSettlementType(balance))
                .paymentStatusLabel(resolvePaymentStatusLabel(totalAmount, netPaid))
                .canCheckout(canCheckout)
                .blockReason(canCheckout ? null : resolveCheckoutBlockReason(detail, booking))
                .vietQrImageUrl(buildVietQrImageUrl(booking, detail, payable))
                .transferBankCode(vietQrBankCode)
                .transferAccountNumber(vietQrAccountNumber)
                .transferAccountName(vietQrAccountName)
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
                && bookingStatus != BookingStatus.PARTIALLY_CHECKED_OUT) {
            return "Booking không ở trạng thái cho phép trả phòng.";
        }

        if (resolveStayStatus(detail, booking) != BookingDetailStatus.CHECKED_IN) {
            return "Chỉ có thể trả phòng cho phòng đang ở trạng thái đã nhận phòng.";
        }

        if (hasRequestedServices(detail)) {
            return "Vui lòng xử lý toàn bộ dịch vụ đang chờ trước khi checkout.";
        }

        Room room = detail.getRoom();
        if (room == null) {
            return "Phòng này chưa được gán phòng thực tế nên không thể checkout.";
        }

        return "Chỉ có thể checkout phòng đang ở trạng thái Đang ở.";
    }

    private void validateSettlementRequest(BigDecimal balance,
                                           BigDecimal paymentAmount,
                                           PaymentMethod paymentMethod) {
        if (paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không được âm.");
        }

        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentAmount.compareTo(balance) != 0) {
                throw new IllegalArgumentException("Số tiền thanh toán checkout phải bằng số dư còn lại của phòng.");
            }
            validateReceptionistPaymentMethod(paymentMethod);
            return;
        }

        if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Phòng đã cân bằng thanh toán, không cần ghi thêm giao dịch.");
        }
    }

    private void validateReceptionistPaymentMethod(PaymentMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
        }
        if (method != PaymentMethod.CASH && method != PaymentMethod.TRANSFER) {
            throw new IllegalArgumentException("Checkout tại lễ tân chỉ hỗ trợ tiền mặt hoặc chuyển khoản.");
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
        } else {
            booking.setStatus(BookingStatus.PARTIALLY_CHECKED_OUT);
        }

        bookingRepository.save(booking);
    }

    private BookingDetail getBookingDetail(Long bookingDetailId) {
        if (bookingDetailId == null) {
            throw new IllegalArgumentException("Thiếu mã phòng cần checkout.");
        }
        return bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng trong booking."));
    }

    private BookingDetail getBookingDetailForUpdate(Long bookingDetailId) {
        if (bookingDetailId == null || bookingDetailId <= 0) {
            throw new IllegalArgumentException("Thiếu mã phòng cần checkout.");
        }
        return bookingDetailRepository.findByIdForUpdate(bookingDetailId)
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
            // Older check-ins only updated the booking and room statuses, leaving
            // the detail RESERVED. Treat those occupied details as checked in so
            // they can still enter the checkout flow.
            if (detail.getStayStatus() == BookingDetailStatus.RESERVED
                    && booking.getStatus() == BookingStatus.CHECKED_IN
                    && detail.getRoom() != null
                    && detail.getRoom().getStatus() == RoomStatus.OCCUPIED) {
                return BookingDetailStatus.CHECKED_IN;
            }
            return detail.getStayStatus();
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN
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
                        .filter(this::isChargeableFolioItem)
                        .map(FolioItem::getTotalAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        ).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFolioSubtotal(List<FolioItem> folioItems) {
        return folioItems.stream()
                .filter(this::isChargeableFolioItem)
                .map(FolioItem::getBaseAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFolioServiceCharge(List<FolioItem> folioItems) {
        return folioItems.stream()
                .filter(this::isChargeableFolioItem)
                .map(FolioItem::getServiceChargeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFolioVat(List<FolioItem> folioItems) {
        return folioItems.stream()
                .filter(this::isChargeableFolioItem)
                .map(FolioItem::getVatAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private boolean hasRequestedServices(BookingDetail detail) {
        return detail.getId() != null
                && folioItemRepository.existsUnresolvedService(
                detail.getId(),
                FolioItemStatus.REQUESTED
        );
    }

    private boolean isChargeableFolioItem(FolioItem item) {
        return item.getService() == null || item.getServiceStatus() != FolioItemStatus.CANCELLED;
    }

    private BigDecimal calculateAppliedPaidAmount(List<PaymentApplication> applications) {
        return applications.stream()
                .filter(application -> application.getPayment() != null)
                .filter(application -> application.getPayment().getStatus() == PaymentStatus.SUCCESS)
                .map(PaymentApplication::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private String resolveSettlementType(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            return "PAYMENT";
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

    private String buildVietQrImageUrl(Booking booking, BookingDetail detail, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return "https://img.vietqr.io/image/"
                + encodePath(vietQrBankCode)
                + "-"
                + encodePath(vietQrAccountNumber)
                + "-compact2.png?amount="
                + money(amount).toPlainString()
                + "&addInfo="
                + encodeQuery(buildTransferContent(booking, detail))
                + "&accountName="
                + encodeQuery(vietQrAccountName);
    }

    private String buildTransferContent(Booking booking, BookingDetail detail) {
        String bookingReference = booking == null ? "" : booking.getBookingReference();
        String roomNumber = detail == null || detail.getRoom() == null ? "" : detail.getRoom().getRoomNumber();
        return normalizeTransferContent(bookingReference + " P" + roomNumber + " CHECKOUT");
    }

    private String normalizeTransferContent(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase()
                .replaceAll("[^A-Z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String encodeQuery(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private User getCurrentStaffUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên đang đăng nhập."));
    }
}
