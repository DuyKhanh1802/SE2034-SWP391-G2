package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PriceDisplayMode;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FolioItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.FolioAdjustmentRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.FolioDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.FolioListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        Page<Booking> bookings = searchPaymentStatus.isBlank()
                ? bookingRepository.searchFolioBookings(
                searchKeyword, searchGuestName, searchBookingStatus, checkIn, checkOut, pageable)
                : bookingRepository.searchFolioBookingsByPaymentStatus(
                searchKeyword, searchGuestName, searchBookingStatus, searchPaymentStatus.toUpperCase(), checkIn, checkOut, pageable);

        return toListResponsePage(bookings);
    }

    @Transactional(readOnly = true)
    public FolioDetailResponse getFolioDetail(Long bookingId) {
        Booking booking = getActiveBooking(bookingId);
        List<BookingDetail> details = bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);
        List<FolioItem> items = folioItemRepository.findByBookingIdAndIsVoidedFalseOrderByPostedAtAsc(bookingId);
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);

        BigDecimal totalAmount = money(booking.getGrandTotal());
        BigDecimal paidAmount = calculatePaidAmount(payments);
        BigDecimal balanceAmount = calculateBalance(totalAmount, paidAmount);
        PaymentState paymentState = resolvePaymentState(totalAmount, paidAmount);

        List<FolioDetailResponse.RoomLine> rooms = details.stream()
                .map(this::toRoomLine)
                .toList();

        List<FolioDetailResponse.FolioLine> lines = items.stream()
                .map(item -> toFolioLine(item, isEditableBooking(booking)))
                .toList();

        List<FolioDetailResponse.PaymentLine> paymentLines = payments.stream()
                .map(this::toPaymentLine)
                .toList();

        return FolioDetailResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .guestName(buildGuestName(booking))
                .guestPhone(booking.getGuestPhone())
                .guestEmail(booking.getGuestEmail())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .bookingStatus(booking.getStatus() == null ? "" : booking.getStatus().name())
                .roomSubtotal(money(booking.getRoomSubtotal()))
                .serviceSubtotal(money(booking.getServiceSubtotal()))
                .serviceChargeTotal(money(booking.getServiceChargeTotal()))
                .vatTotal(money(booking.getVatTotal()))
                .discountAmount(money(booking.getDiscountAmount()))
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .balanceAmount(balanceAmount)
                .paymentStatus(paymentState.status())
                .paymentStatusLabel(paymentState.label())
                .editable(isEditableBooking(booking))
                .rooms(rooms)
                .roomCharges(lines.stream()
                        .filter(line -> "ROOM_CHARGE".equals(line.getItemType()))
                        .toList())
                .serviceCharges(lines.stream()
                        .filter(line -> !"ROOM_CHARGE".equals(line.getItemType())
                                && !"ADJUSTMENT".equals(line.getItemType())
                                && !"DISCOUNT".equals(line.getItemType()))
                        .toList())
                .adjustments(lines.stream()
                        .filter(line -> "ADJUSTMENT".equals(line.getItemType())
                                || "DISCOUNT".equals(line.getItemType()))
                        .toList())
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

        FolioItemType itemType = normalizedAmount.compareTo(BigDecimal.ZERO) < 0
                ? FolioItemType.DISCOUNT
                : FolioItemType.ADJUSTMENT;

        FolioItem item = FolioItem.builder()
                .booking(booking)
                .bookingDetail(detail)
                .description(description)
                .itemType(itemType)
                .serviceStatus(FolioItemStatus.COMPLETED)
                .amount(normalizedAmount)
                .baseAmount(normalizedAmount)
                .serviceChargeRate(BigDecimal.ZERO)
                .serviceChargeAmount(BigDecimal.ZERO)
                .vatRate(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .totalAmount(normalizedAmount)
                .priceDisplayMode(PriceDisplayMode.PLUS_PLUS)
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
    public void voidAdjustment(Long bookingId, Long folioItemId, String reason) {
        Booking booking = getActiveBooking(bookingId);
        validateEditableBooking(booking);

        if (folioItemId == null) {
            throw new IllegalArgumentException("Thiếu mã dòng folio cần huỷ.");
        }

        FolioItem item = folioItemRepository.findById(folioItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng folio."));

        if (item.getBooking() == null || !booking.getId().equals(item.getBooking().getId())) {
            throw new IllegalArgumentException("Dòng folio không thuộc booking này.");
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

    private Page<FolioListResponse> toListResponsePage(Page<Booking> bookings) {
        List<Long> bookingIds = bookings.getContent().stream()
                .map(Booking::getId)
                .toList();

        Map<Long, String> roomNumbersByBookingId = loadRoomNumbersByBookingId(bookingIds);
        Map<Long, BigDecimal> paidAmountsByBookingId = loadPaidAmountsByBookingId(bookingIds);

        return bookings.map(booking -> toListResponse(
                booking,
                roomNumbersByBookingId.getOrDefault(booking.getId(), "Chưa phân phòng"),
                paidAmountsByBookingId.getOrDefault(booking.getId(), BigDecimal.ZERO)
        ));
    }

    private Map<Long, String> loadRoomNumbersByBookingId(List<Long> bookingIds) {
        if (bookingIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> roomsByBookingId = new LinkedHashMap<>();
        for (Object[] row : bookingDetailRepository.findRoomNumbersByBookingIds(bookingIds)) {
            Long bookingId = (Long) row[0];
            String roomNumber = row[1] == null ? "" : row[1].toString().trim();
            if (!roomNumber.isBlank()) {
                roomsByBookingId.computeIfAbsent(bookingId, ignored -> new ArrayList<>()).add(roomNumber);
            }
        }

        Map<Long, String> result = new HashMap<>();
        roomsByBookingId.forEach((bookingId, roomNumbers) -> result.put(
                bookingId,
                roomNumbers.stream().distinct().reduce((left, right) -> left + ", " + right).orElse("Chưa phân phòng")
        ));
        return result;
    }

    private Map<Long, BigDecimal> loadPaidAmountsByBookingId(List<Long> bookingIds) {
        if (bookingIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> collectedAmounts = mapAmountRows(
                paymentRepository.findSuccessfulCollectionTotalsByBookingIds(bookingIds));
        Map<Long, BigDecimal> refundedAmounts = mapAmountRows(
                paymentRepository.findSuccessfulRefundTotalsByBookingIds(bookingIds));

        Map<Long, BigDecimal> result = new HashMap<>();
        for (Long bookingId : bookingIds) {
            BigDecimal netPaid = money(collectedAmounts.get(bookingId)).subtract(money(refundedAmounts.get(bookingId)));
            result.put(bookingId, netPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : money(netPaid));
        }
        return result;
    }

    private Map<Long, BigDecimal> mapAmountRows(List<Object[]> rows) {
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Object[] row : rows) {
            Long bookingId = (Long) row[0];
            BigDecimal amount = toBigDecimal(row[1]);
            result.put(bookingId, money(amount));
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal amount) {
            return amount;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return BigDecimal.ZERO;
    }

    private FolioListResponse toListResponse(Booking booking, String roomSummary, BigDecimal paidAmount) {
        BigDecimal totalAmount = money(booking.getGrandTotal());
        BigDecimal balanceAmount = calculateBalance(totalAmount, paidAmount);
        PaymentState paymentState = resolvePaymentState(totalAmount, paidAmount);

        return new FolioListResponse(
                booking.getId(),
                booking.getBookingReference(),
                buildGuestName(booking),
                roomSummary,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getStatus() == null ? "" : booking.getStatus().name(),
                totalAmount,
                paidAmount,
                balanceAmount,
                paymentState.status(),
                paymentState.label()
        );
    }

    private FolioDetailResponse.RoomLine toRoomLine(BookingDetail detail) {
        return FolioDetailResponse.RoomLine.builder()
                .bookingDetailId(detail.getId())
                .roomNumber(detail.getRoom() == null ? null : detail.getRoom().getRoomNumber())
                .roomName(detail.getVariant() == null
                        ? "Chưa xác định"
                        : detail.getVariant().getVariantName())
                .totalAmount(money(detail.getTotalAmount()))
                .build();
    }

    private FolioDetailResponse.FolioLine toFolioLine(FolioItem item, boolean editableBooking) {
        FolioItemStatus status = item.getServiceStatus() == null
                ? FolioItemStatus.REQUESTED
                : item.getServiceStatus();
        boolean voidable = editableBooking
                && (item.getItemType() == FolioItemType.ADJUSTMENT || item.getItemType() == FolioItemType.DISCOUNT);

        return FolioDetailResponse.FolioLine.builder()
                .folioItemId(item.getId())
                .bookingDetailId(item.getBookingDetail() == null ? null : item.getBookingDetail().getId())
                .roomNumber(item.getBookingDetail() != null && item.getBookingDetail().getRoom() != null
                        ? item.getBookingDetail().getRoom().getRoomNumber()
                        : "Chung")
                .description(item.getDescription())
                .itemType(item.getItemType() == null ? "N/A" : item.getItemType().name())
                .statusLabel(status.getLabel())
                .quantity(item.getQuantity())
                .unitPrice(money(item.getUnitPrice()))
                .baseAmount(money(item.getBaseAmount()))
                .serviceChargeAmount(money(item.getServiceChargeAmount()))
                .vatAmount(money(item.getVatAmount()))
                .totalAmount(money(item.getTotalAmount()))
                .postedAt(item.getPostedAt())
                .adjustmentReason(item.getAdjustmentReason())
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
        BigDecimal collected = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .filter(payment -> payment.getPaymentType() != PaymentType.REFUND)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refunded = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .filter(payment -> payment.getPaymentType() == PaymentType.REFUND)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netPaid = collected.subtract(refunded);
        return netPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : money(netPaid);
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

    private String buildRoomNumbers(List<BookingDetail> details) {
        if (details == null || details.isEmpty()) {
            return "Chưa phân phòng";
        }

        return details.stream()
                .map(detail -> detail.getRoom() == null ? null : detail.getRoom().getRoomNumber())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(roomNumber -> !roomNumber.isBlank())
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("Chưa phân phòng");
    }

    private String buildRoomSummary(List<BookingDetail> details) {
        if (details == null || details.isEmpty()) {
            return "Chưa có phòng";
        }

        return details.stream()
                .map(detail -> {
                    String roomName = detail.getVariant() == null ? "Phòng" : detail.getVariant().getVariantName();
                    String roomNumber = detail.getRoom() == null ? "chưa phân phòng" : "Phòng " + detail.getRoom().getRoomNumber();
                    return roomName + " - " + roomNumber;
                })
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("Chưa có phòng");
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private record PaymentState(String status, String label) {
    }
}
