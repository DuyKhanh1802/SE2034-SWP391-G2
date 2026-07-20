package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingDetailStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FolioItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.PaymentApplication;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.FolioAdjustmentRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.FolioDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.FolioListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PaymentApplicationRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FolioService {

    private static final int DESCRIPTION_MAX_LENGTH = 200;
    private static final int ADJUSTMENT_REASON_MAX_LENGTH = 300;
    private static final int VOIDED_REASON_MAX_LENGTH = 200;
    private static final BigDecimal MAX_ADJUSTMENT_ABS_AMOUNT = new BigDecimal("1000000000");
    private static final Set<String> PAYMENT_STATUS_FILTERS = Set.of("PAID", "UNPAID", "PARTIAL");

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final FolioItemRepository folioItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentApplicationRepository paymentApplicationRepository;

    @Transactional(readOnly = true)
    public Page<FolioListResponse> searchFolios(String keyword,
                                                String bookingStatus,
                                                String paymentStatus,
                                                LocalDate checkIn,
                                                LocalDate checkOut,
                                                Pageable pageable) {
        validateSearchInput(paymentStatus, checkIn, checkOut, pageable);

        String searchKeyword = keyword == null ? "" : keyword.trim();
        String searchBookingStatus = bookingStatus == null ? "" : bookingStatus.trim();
        String searchPaymentStatus = normalizePaymentStatus(paymentStatus);
        boolean searchGuestName = searchKeyword.length() >= 2;

        Page<BookingDetail> details = bookingDetailRepository.searchFolioBookingDetails(
                searchKeyword,
                searchGuestName,
                searchBookingStatus,
                searchPaymentStatus.toUpperCase(),
                checkIn,
                checkOut,
                pageable
        );

        return toListResponsePage(details);
    }

    @Transactional(readOnly = true)
    public FolioDetailResponse getFolioDetail(Long bookingId, Long bookingDetailId) {
        Booking booking = getActiveBooking(bookingId);
        List<BookingDetail> details = bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);
        List<FolioItem> items = folioItemRepository.findByBookingIdAndIsVoidedFalseOrderByPostedAtAsc(bookingId);
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        List<PaymentApplication> applications = List.of();

        if (bookingDetailId != null) {
            details = details.stream()
                    .filter(detail -> bookingDetailId.equals(detail.getId()))
                    .toList();
            if (details.isEmpty()) {
                throw new IllegalArgumentException("Phòng được chọn không thuộc booking này.");
            }
            items = items.stream()
                    .filter(item -> item.getBookingDetail() != null && bookingDetailId.equals(item.getBookingDetail().getId()))
                    .toList();
            applications = paymentApplicationRepository.findByBookingDetailId(bookingDetailId);
            payments = applications.stream()
                    .map(PaymentApplication::getPayment)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }

        FolioTotals totals = calculateFolioTotals(details, items);
        BigDecimal totalAmount = totals.totalAmount();
        BigDecimal paidAmount = bookingDetailId == null
                ? calculatePaidAmount(payments)
                : calculateAppliedPaidAmount(applications);
        BigDecimal balanceAmount = calculateBalance(totalAmount, paidAmount);
        PaymentState paymentState = resolvePaymentState(totalAmount, paidAmount);

        List<FolioDetailResponse.RoomLine> rooms = details.stream()
                .map(this::toRoomLine)
                .toList();

        boolean canManageServices = booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.PARTIALLY_CHECKED_OUT;
        List<FolioDetailResponse.FolioLine> lines = items.stream()
                .map(item -> toFolioLine(item, isEditableBooking(booking), canManageServices))
                .toList();

        List<FolioDetailResponse.PaymentLine> paymentLines = payments.stream()
                .map(this::toPaymentLine)
                .toList();

        return FolioDetailResponse.builder()
                .bookingId(booking.getId())
                .bookingDetailId(bookingDetailId)
                .bookingReference(booking.getBookingReference())
                .roomNumber(resolveFolioRoomNumber(rooms))
                .guestName(buildGuestName(booking))
                .guestPhone(booking.getGuestPhone())
                .guestEmail(booking.getGuestEmail())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .bookingStatus(booking.getStatus() == null ? "" : booking.getStatus().name())
                .roomSubtotal(totals.roomSubtotal())
                .serviceSubtotal(totals.serviceSubtotal())
                .serviceChargeTotal(totals.serviceChargeTotal())
                .vatTotal(totals.vatTotal())
                .discountAmount(bookingDetailId == null ? money(booking.getDiscountAmount()) : BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .balanceAmount(balanceAmount)
                .paymentStatus(paymentState.status())
                .paymentStatusLabel(paymentState.label())
                .editable(isEditableBooking(booking))
                .rooms(rooms)
                .invoiceCharges(lines)
                .payments(paymentLines)
                .build();
    }

    @Transactional
    public void addAdjustment(Long bookingId, FolioAdjustmentRequest request) {
        Booking booking = getActiveBooking(bookingId);
        validateEditableBooking(booking);

        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin điều chỉnh.");
        }

        if (request.getBookingDetailId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phòng áp dụng điều chỉnh.");
        }

        BigDecimal normalizedAmount = validateAndNormalizeAdjustmentAmount(request.getAmount(), booking);
        String description = validateAndNormalizeRequiredText(
                request.getDescription(),
                "Vui lòng nhập mô tả điều chỉnh.",
                DESCRIPTION_MAX_LENGTH,
                "Mô tả điều chỉnh"
        );
        String adjustmentReason = validateAndNormalizeOptionalText(
                request.getAdjustmentReason(),
                ADJUSTMENT_REASON_MAX_LENGTH,
                "Lý do điều chỉnh"
        );

        BookingDetail detail = null;
        if (request.getBookingDetailId() != null) {
            detail = bookingDetailRepository.findById(request.getBookingDetailId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng được chọn."));
            if (detail.getBooking() == null || !booking.getId().equals(detail.getBooking().getId())) {
                throw new IllegalArgumentException("Phòng được chọn không thuộc booking này.");
            }
        }

        validateEditableBookingDetail(detail);
        validateNegativeAdjustmentWithinRoomTotal(normalizedAmount, detail);

        FolioItemType itemType = normalizedAmount.compareTo(BigDecimal.ZERO) < 0
                ? FolioItemType.DISCOUNT
                : FolioItemType.ADJUSTMENT;

        FolioItem item = FolioItem.builder()
                .booking(booking)
                .bookingDetail(detail)
                .description(description)
                .itemType(itemType)
                .serviceStatus(FolioItemStatus.COMPLETED)
                .baseAmount(normalizedAmount)
                .serviceChargeRate(BigDecimal.ZERO)
                .serviceChargeAmount(BigDecimal.ZERO)
                .vatRate(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .totalAmount(normalizedAmount)
                .quantity(1)
                .unitPrice(normalizedAmount)
                .postedAt(Instant.now())
                .adjustmentReason(adjustmentReason)
                .isVoided(false)
                .build();

        folioItemRepository.save(item);
        applyAmountToBooking(booking, normalizedAmount, itemType);
    }

    @Transactional
    public void voidAdjustment(Long bookingId, Long bookingDetailId, Long folioItemId, String reason) {
        Booking booking = getActiveBooking(bookingId);
        validateEditableBooking(booking);
        BookingDetail detail = getBookingDetailForFolio(booking, bookingDetailId);
        validateEditableBookingDetail(detail);

        if (folioItemId == null) {
            throw new IllegalArgumentException("Thiếu mã dòng folio cần huỷ.");
        }

        FolioItem item = folioItemRepository.findById(folioItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng folio."));

        if (item.getBooking() == null || !booking.getId().equals(item.getBooking().getId())) {
            throw new IllegalArgumentException("Dòng folio không thuộc booking này.");
        }

        if (item.getBookingDetail() == null || !detail.getId().equals(item.getBookingDetail().getId())) {
            throw new IllegalArgumentException("Dòng folio không thuộc phòng đang chỉnh sửa.");
        }

        if (Boolean.TRUE.equals(item.getIsVoided())) {
            throw new IllegalArgumentException("Dòng folio này đã bị huỷ.");
        }

        if (item.getItemType() != FolioItemType.ADJUSTMENT && item.getItemType() != FolioItemType.DISCOUNT) {
            throw new IllegalArgumentException("Chỉ có thể huỷ các dòng điều chỉnh hoặc giảm trừ.");
        }

        String normalizedReason = validateAndNormalizeOptionalText(
                reason,
                VOIDED_REASON_MAX_LENGTH,
                "Lý do huỷ dòng folio"
        );

        item.setIsVoided(true);
        item.setVoidedAt(Instant.now());
        item.setVoidedReason(normalizedReason == null ? "Huỷ điều chỉnh folio" : normalizedReason);
        folioItemRepository.save(item);

        BigDecimal reverseAmount = money(item.getTotalAmount()).negate();
        applyAmountToBooking(booking, reverseAmount, item.getItemType());
    }

    private Page<FolioListResponse> toListResponsePage(Page<BookingDetail> details) {
        return details.map(this::toListResponse);
    }

    private FolioListResponse toListResponse(BookingDetail detail) {
        Booking booking = detail.getBooking();
        List<FolioItem> items = folioItemRepository
                .findByBookingDetail_IdAndIsVoidedFalseOrderByPostedAtAsc(detail.getId());
        List<PaymentApplication> applications = paymentApplicationRepository.findByBookingDetailId(detail.getId());

        BigDecimal totalAmount = calculateFolioTotals(List.of(detail), items).totalAmount();
        BigDecimal paidAmount = calculateAppliedPaidAmount(applications);
        BigDecimal balanceAmount = calculateBalance(totalAmount, paidAmount);
        PaymentState paymentState = resolvePaymentState(totalAmount, paidAmount);

        return new FolioListResponse(
                booking.getId(),
                detail.getId(),
                booking.getBookingReference(),
                buildGuestName(booking),
                buildRoomSummary(detail),
                detail.getCheckInDate(),
                detail.getCheckOutDate(),
                booking.getStatus() == null ? "" : booking.getStatus().name(),
                totalAmount,
                paidAmount,
                balanceAmount,
                paymentState.status(),
                paymentState.label()
        );
    }

    private String buildRoomSummary(BookingDetail detail) {
        String variantName = detail.getVariant() == null ? "Phòng" : detail.getVariant().getVariantName();
        String roomNumber = detail.getRoom() == null ? "Không rõ" : "Phòng " + detail.getRoom().getRoomNumber();
        return roomNumber + " - " + variantName;
    }

    private String resolveFolioRoomNumber(List<FolioDetailResponse.RoomLine> rooms) {
        return rooms.stream()
                .map(FolioDetailResponse.RoomLine::getRoomNumber)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(roomNumber -> !roomNumber.isBlank())
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("Không rõ");
    }

    private FolioDetailResponse.RoomLine toRoomLine(BookingDetail detail) {
        return FolioDetailResponse.RoomLine.builder()
                .bookingDetailId(detail.getId())
                .roomNumber(detail.getRoom() == null ? null : detail.getRoom().getRoomNumber())
                .roomName(detail.getVariant() == null
                        ? "Chưa xác định"
                        : detail.getVariant().getVariantName())
                .totalAmount(money(detail.getTotalAmount()))
                .actualCheckinAt(detail.getActualCheckinAt())
                .actualCheckoutAt(detail.getActualCheckoutAt())
                .build();
    }

    private FolioDetailResponse.FolioLine toFolioLine(FolioItem item,
                                                      boolean editableBooking,
                                                      boolean canManageServices) {
        FolioItemStatus status = item.getServiceStatus() == null
                ? FolioItemStatus.REQUESTED
                : item.getServiceStatus();
        boolean serviceItem = item.getService() != null;
        BookingDetailStatus stayStatus = item.getBookingDetail() == null
                ? null
                : item.getBookingDetail().getStayStatus();
        boolean legacyCheckedInDetail = item.getBookingDetail() != null
                && stayStatus == null
                && item.getBookingDetail().getActualCheckinAt() != null
                && item.getBookingDetail().getActualCheckoutAt() == null;
        boolean activeRoom = item.getBookingDetail() != null
                && item.getBookingDetail().getActualCheckoutAt() == null
                && (stayStatus == BookingDetailStatus.CHECKED_IN || legacyCheckedInDetail);
        boolean voidable = editableBooking
                && (item.getItemType() == FolioItemType.ADJUSTMENT || item.getItemType() == FolioItemType.DISCOUNT);

        return FolioDetailResponse.FolioLine.builder()
                .folioItemId(item.getId())
                .roomNumber(item.getBookingDetail() != null && item.getBookingDetail().getRoom() != null
                        ? item.getBookingDetail().getRoom().getRoomNumber()
                        : "Chung")
                .description(item.getDescription())
                .itemType(item.getItemType() == null ? "N/A" : item.getItemType().name())
                .serviceStatus(serviceItem ? status.name() : null)
                .statusLabel(serviceItem ? status.getLabel() : null)
                .quantity(item.getQuantity())
                .unitPrice(money(item.getUnitPrice()))
                .serviceChargeAmount(money(item.getServiceChargeAmount()))
                .vatAmount(money(item.getVatAmount()))
                .totalAmount(money(item.getTotalAmount()))
                .adjustmentReason(item.getAdjustmentReason())
                .serviceItem(serviceItem)
                .serviceActionable(serviceItem
                        && status == FolioItemStatus.REQUESTED
                        && canManageServices
                        && activeRoom)
                .voidable(voidable)
                .build();
    }

    private FolioDetailResponse.PaymentLine toPaymentLine(Payment payment) {
        return FolioDetailResponse.PaymentLine.builder()
                .transactionRef(payment.getTransactionRef())
                .paymentType(payment.getPaymentType() == null ? "N/A" : payment.getPaymentType().getLabel())
                .method(payment.getMethod() == null ? "N/A" : payment.getMethod().getLabel())
                .amount(money(payment.getAmount()))
                .status(payment.getStatus() == null ? "N/A" : payment.getStatus().getLabel())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private Booking getActiveBooking(Long bookingId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Thiếu mã booking.");
        }

        return bookingRepository.findByIdAndIsDeletedFalse(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking."));
    }

    private void validateEditableBooking(Booking booking) {
        if (!isEditableBooking(booking)) {
            throw new IllegalStateException("Chỉ có thể chỉnh folio trước khi booking trả phòng, huỷ hoặc no-show.");
        }
    }

    private void validateEditableBookingDetail(BookingDetail detail) {
        BookingDetailStatus stayStatus = detail.getStayStatus();
        if (stayStatus == BookingDetailStatus.CHECKED_OUT || stayStatus == BookingDetailStatus.CANCELLED) {
            throw new IllegalStateException("Không thể chỉnh hoá đơn của phòng đã trả, đã huỷ hoặc no-show.");
        }
    }

    private BookingDetail getBookingDetailForFolio(Booking booking, Long bookingDetailId) {
        if (bookingDetailId == null) {
            throw new IllegalArgumentException("Thiếu phòng áp dụng điều chỉnh.");
        }

        BookingDetail detail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng được chọn."));
        if (detail.getBooking() == null || !booking.getId().equals(detail.getBooking().getId())) {
            throw new IllegalArgumentException("Phòng được chọn không thuộc booking này.");
        }
        return detail;
    }

    private void validateNegativeAdjustmentWithinRoomTotal(BigDecimal amount, BookingDetail detail) {
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            return;
        }

        List<FolioItem> currentRoomItems = folioItemRepository
                .findByBookingDetail_IdAndIsVoidedFalseOrderByPostedAtAsc(detail.getId());
        BigDecimal currentRoomTotal = calculateFolioTotals(List.of(detail), currentRoomItems).totalAmount();
        if (amount.abs().compareTo(currentRoomTotal) > 0) {
            throw new IllegalArgumentException("Số tiền giảm trừ không được lớn hơn tổng tiền hiện tại của phòng.");
        }
    }

    private boolean isEditableBooking(Booking booking) {
        return booking.getStatus() != BookingStatus.CHECKED_OUT
                && booking.getStatus() != BookingStatus.CANCELLED
                && booking.getStatus() != BookingStatus.NO_SHOW;
    }

    private void validateSearchInput(String paymentStatus,
                                     LocalDate checkIn,
                                     LocalDate checkOut,
                                     Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Thiếu thông tin phân trang.");
        }
        if (checkIn != null && checkOut != null && checkIn.isAfter(checkOut)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        normalizePaymentStatus(paymentStatus);
    }

    private String normalizePaymentStatus(String paymentStatus) {
        String normalizedStatus = paymentStatus == null ? "" : paymentStatus.trim().toUpperCase();
        if (!normalizedStatus.isBlank() && !PAYMENT_STATUS_FILTERS.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ.");
        }
        return normalizedStatus;
    }

    private BigDecimal validateAndNormalizeAdjustmentAmount(BigDecimal amount, Booking booking) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Số tiền điều chỉnh phải khác 0.");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Số tiền điều chỉnh phải là số nguyên VND.");
        }

        BigDecimal normalizedAmount = amount.setScale(0);
        if (normalizedAmount.abs().compareTo(MAX_ADJUSTMENT_ABS_AMOUNT) > 0) {
            throw new IllegalArgumentException("Số tiền điều chỉnh không được vượt quá 1,000,000,000 VND.");
        }
        if (normalizedAmount.compareTo(BigDecimal.ZERO) < 0
                && normalizedAmount.abs().compareTo(money(booking.getGrandTotal())) > 0) {
            throw new IllegalArgumentException("Số tiền giảm trừ không được lớn hơn tổng tiền hiện tại của hoá đơn.");
        }

        return normalizedAmount;
    }

    private String validateAndNormalizeRequiredText(String value,
                                                    String requiredMessage,
                                                    int maxLength,
                                                    String fieldName) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " không được vượt quá " + maxLength + " ký tự.");
        }
        return normalizedValue;
    }

    private String validateAndNormalizeOptionalText(String value, int maxLength, String fieldName) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isBlank()) {
            return null;
        }
        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " không được vượt quá " + maxLength + " ký tự.");
        }
        return normalizedValue;
    }

    private void applyAmountToBooking(Booking booking, BigDecimal amount, FolioItemType sourceType) {
        BigDecimal newTotal = money(booking.getGrandTotal()).add(money(amount));
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            newTotal = BigDecimal.ZERO;
        }

        booking.setGrandTotal(newTotal);
        booking.setTotalAmount(newTotal);

        if (sourceType == FolioItemType.DISCOUNT) {
            BigDecimal discountDelta = money(amount).abs();
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                booking.setDiscountAmount(money(booking.getDiscountAmount()).add(discountDelta));
            } else {
                BigDecimal adjustedDiscount = money(booking.getDiscountAmount()).subtract(discountDelta);
                booking.setDiscountAmount(adjustedDiscount.max(BigDecimal.ZERO));
            }
        }

        booking.setAmountCalculatedAt(Instant.now());
        bookingRepository.save(booking);
    }

    private BigDecimal calculatePaidAmount(List<Payment> payments) {
        return money(payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal calculateAppliedPaidAmount(List<PaymentApplication> applications) {
        return money(applications.stream()
                .filter(application -> application.getPayment() != null)
                .filter(application -> application.getPayment().getStatus() == PaymentStatus.SUCCESS)
                .map(PaymentApplication::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private FolioTotals calculateFolioTotals(List<BookingDetail> details, List<FolioItem> items) {
        BigDecimal roomSubtotal = details.stream()
                .map(BookingDetail::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FolioItem> chargeableItems = items.stream()
                .filter(this::isChargeableFolioItem)
                .toList();

        BigDecimal serviceSubtotal = chargeableItems.stream()
                .map(FolioItem::getBaseAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal serviceChargeTotal = chargeableItems.stream()
                .map(FolioItem::getServiceChargeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vatTotal = chargeableItems.stream()
                .map(FolioItem::getVatAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal itemTotal = chargeableItems.stream()
                .map(FolioItem::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FolioTotals(
                money(roomSubtotal),
                money(serviceSubtotal),
                money(serviceChargeTotal),
                money(vatTotal),
                money(roomSubtotal.add(itemTotal))
        );
    }

    private boolean isChargeableFolioItem(FolioItem item) {
        return item.getService() == null || item.getServiceStatus() != FolioItemStatus.CANCELLED;
    }

    private BigDecimal calculateBalance(BigDecimal totalAmount, BigDecimal paidAmount) {
        BigDecimal balance = money(totalAmount).subtract(money(paidAmount));
        return balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : money(balance);
    }

    private PaymentState resolvePaymentState(BigDecimal totalAmount, BigDecimal paidAmount) {
        if (money(totalAmount).compareTo(BigDecimal.ZERO) == 0) {
            return new PaymentState("PAID", "Đã thanh toán");
        }
        if (money(paidAmount).compareTo(BigDecimal.ZERO) == 0) {
            return new PaymentState("UNPAID", "Chưa thanh toán");
        }
        if (money(paidAmount).compareTo(money(totalAmount)) >= 0) {
            return new PaymentState("PAID", "Đã thanh toán");
        }
        return new PaymentState("PARTIAL", "Thanh toán một phần");
    }

    private String buildGuestName(Booking booking) {
        String name = ((booking.getGuestLastName() == null ? "" : booking.getGuestLastName()) + " "
                + (booking.getGuestFirstName() == null ? "" : booking.getGuestFirstName())).trim();
        return name.isBlank() ? booking.getGuestEmail() : name;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private record PaymentState(String status, String label) {
    }

    private record FolioTotals(BigDecimal roomSubtotal,
                               BigDecimal serviceSubtotal,
                               BigDecimal serviceChargeTotal,
                               BigDecimal vatTotal,
                               BigDecimal totalAmount) {
    }
}
