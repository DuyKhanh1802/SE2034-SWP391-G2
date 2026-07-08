package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PriceDisplayMode;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomMoveFeePolicy;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomMoveReason;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FolioItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomMoveLog;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RoomMoveRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomMoveHistoryResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomMoveOptionResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomMoveLogRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RoomMoveService {

    private static final int MAX_REASON_NOTE_LENGTH = 500;
    private static final BigDecimal VAT_RATE = new BigDecimal("0.08");
    private static final BigDecimal SERVICE_CHARGE_RATE = new BigDecimal("0.05");
    private static final boolean TAX_ON_SERVICE_CHARGE = true;

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomRepository roomRepository;
    private final FolioItemRepository folioItemRepository;
    private final RoomMoveLogRepository roomMoveLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<Long, List<RoomMoveOptionResponse>> getAvailableRoomsByDetail(Long bookingId) {
        if (bookingId == null) {
            return Collections.emptyMap();
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null
                || Boolean.TRUE.equals(booking.getIsDeleted())
                || booking.getStatus() != BookingStatus.CHECKED_IN) {
            return Collections.emptyMap();
        }

        List<BookingDetail> details = bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);
        Map<Long, List<RoomMoveOptionResponse>> result = new HashMap<>();

        for (BookingDetail detail : details) {
            if (detail.getRoom() == null) {
                result.put(detail.getId(), Collections.emptyList());
                continue;
            }

            BigDecimal currentPrice = normalizeMoney(detail.getPricePerNight());

            List<RoomMoveOptionResponse> options = roomRepository.findAvailableRoomsForRoomMove(
                            bookingId,
                            detail.getRoom().getId(),
                            detail.getCheckInDate(),
                            detail.getCheckOutDate(),
                            RoomStatus.AVAILABLE,
                            List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
                    )
                    .stream()
                    .filter(room -> canFitGuests(detail, room))
                    .filter(room -> normalizeMoney(room.getVariant().getPricePerNight()).compareTo(currentPrice) >= 0)
                    .map(room -> toRoomMoveOption(room, currentPrice))
                    .toList();

            result.put(detail.getId(), options);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<RoomMoveHistoryResponse> getRoomMoveHistory(Long bookingId) {
        if (bookingId == null) {
            return Collections.emptyList();
        }

        return roomMoveLogRepository.findByBookingIdWithRooms(bookingId)
                .stream()
                .map(log -> RoomMoveHistoryResponse.builder()
                        .movedAt(log.getMovedAt())
                        .oldRoomNumber(log.getOldRoom() != null ? log.getOldRoom().getRoomNumber() : "N/A")
                        .newRoomNumber(log.getNewRoom() != null ? log.getNewRoom().getRoomNumber() : "N/A")
                        .reasonLabel(log.getReasonType() != null ? log.getReasonType().getLabel() : "N/A")
                        .feePolicyLabel(log.getFeePolicy() != null ? log.getFeePolicy().getLabel() : "N/A")
                        .extraChargeAmount(normalizeMoney(log.getExtraChargeAmount()))
                        .oldRoomStatusAfterMove(log.getOldRoomStatusAfterMove())
                        .movedByName(log.getMovedBy() != null
                                ? (log.getMovedBy().getFirstName() + " " + log.getMovedBy().getLastName()).trim()
                                : "N/A")
                        .build())
                .toList();
    }

    @Transactional
    public void moveRoom(RoomMoveRequest request) {
        validateBasicRequest(request);

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Đặt phòng này đã bị xóa.");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Chỉ có thể đổi phòng khi khách đang lưu trú.");
        }

        BookingDetail detail = bookingDetailRepository.findDetailsWithRoomsByBookingId(booking.getId())
                .stream()
                .filter(item -> Objects.equals(item.getId(), request.getBookingDetailId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng đang lưu trú trong booking."));

        Room oldRoom = detail.getRoom();
        if (oldRoom == null) {
            throw new IllegalStateException("Phòng hiện tại chưa được gán, không thể đổi phòng.");
        }

        Room newRoom = roomRepository.findByIdAndIsDeletedFalse(request.getNewRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng mới."));

        validateRoomMove(request, booking, detail, oldRoom, newRoom);

        User currentStaff = getCurrentStaffUser();
        RoomTypeVariant newVariant = newRoom.getVariant();

        BigDecimal oldPricePerNight = normalizeMoney(detail.getPricePerNight());
        BigDecimal newPricePerNight = normalizeMoney(newVariant.getPricePerNight());
        BigDecimal priceDifferencePerNight = newPricePerNight.subtract(oldPricePerNight);

        if (priceDifferencePerNight.compareTo(BigDecimal.ZERO) < 0) {
            priceDifferencePerNight = BigDecimal.ZERO;
        }

        int chargedNights = calculateChargedNights(detail);

        BigDecimal upgradeBaseAmount = BigDecimal.ZERO;
        BigDecimal upgradeServiceChargeAmount = BigDecimal.ZERO;
        BigDecimal upgradeVatAmount = BigDecimal.ZERO;
        BigDecimal upgradeTotalAmount = BigDecimal.ZERO;
        FolioItem upgradeFolioItem = null;

        boolean hasCharge = priceDifferencePerNight.compareTo(BigDecimal.ZERO) > 0
                && request.getFeePolicy() == RoomMoveFeePolicy.GUEST_UPGRADE_CHARGE;

        if (hasCharge) {
            upgradeBaseAmount = priceDifferencePerNight
                    .multiply(BigDecimal.valueOf(chargedNights))
                    .setScale(0, RoundingMode.HALF_UP);

            upgradeServiceChargeAmount = upgradeBaseAmount
                    .multiply(SERVICE_CHARGE_RATE)
                    .setScale(0, RoundingMode.HALF_UP);

            BigDecimal vatBase = TAX_ON_SERVICE_CHARGE
                    ? upgradeBaseAmount.add(upgradeServiceChargeAmount)
                    : upgradeBaseAmount;

            upgradeVatAmount = vatBase
                    .multiply(VAT_RATE)
                    .setScale(0, RoundingMode.HALF_UP);

            upgradeTotalAmount = upgradeBaseAmount
                    .add(upgradeServiceChargeAmount)
                    .add(upgradeVatAmount)
                    .setScale(0, RoundingMode.HALF_UP);

            upgradeFolioItem = createUpgradeFolioItem(
                    booking,
                    detail,
                    oldRoom,
                    newRoom,
                    priceDifferencePerNight,
                    chargedNights,
                    upgradeBaseAmount,
                    upgradeServiceChargeAmount,
                    upgradeVatAmount,
                    upgradeTotalAmount,
                    currentStaff
            );

            applyUpgradeTotalsToBooking(
                    booking,
                    upgradeBaseAmount,
                    upgradeServiceChargeAmount,
                    upgradeVatAmount,
                    upgradeTotalAmount
            );

            applyUpgradeTotalsToBookingDetail(
                    detail,
                    newPricePerNight,
                    upgradeBaseAmount,
                    upgradeServiceChargeAmount,
                    upgradeVatAmount,
                    upgradeTotalAmount
            );
        }

        oldRoom.setStatus(request.getOldRoomStatusAfterMove());

        if (request.getOldRoomStatusAfterMove() == RoomStatus.MAINTENANCE
                && (oldRoom.getNote() == null || oldRoom.getNote().isBlank())) {
            oldRoom.setNote("Đổi phòng từ booking " + booking.getBookingReference() + ". " + normalizeText(request.getReasonNote()));
        }

        newRoom.setStatus(RoomStatus.OCCUPIED);

        detail.setRoom(newRoom);
        detail.setVariant(newVariant);

        RoomMoveLog log = RoomMoveLog.builder()
                .booking(booking)
                .bookingDetail(detail)
                .oldRoom(oldRoom)
                .newRoom(newRoom)
                .reasonType(request.getReasonType())
                .reasonNote(normalizeText(request.getReasonNote()))
                .feePolicy(request.getFeePolicy())
                .priceDifferencePerNight(priceDifferencePerNight.setScale(0, RoundingMode.HALF_UP))
                .chargedNights(hasCharge ? chargedNights : 0)
                .extraChargeAmount(upgradeTotalAmount)
                .oldRoomStatusAfterMove(request.getOldRoomStatusAfterMove())
                .folioItem(upgradeFolioItem)
                .movedBy(currentStaff)
                .movedAt(Instant.now())
                .build();

        roomRepository.save(oldRoom);
        roomRepository.save(newRoom);
        bookingDetailRepository.save(detail);
        bookingRepository.save(booking);
        roomMoveLogRepository.save(log);
    }

    private void validateBasicRequest(RoomMoveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin đổi phòng không được để trống.");
        }
        if (request.getBookingId() == null) {
            throw new IllegalArgumentException("Thiếu mã đặt phòng.");
        }
        if (request.getBookingDetailId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phòng hiện tại cần đổi.");
        }
        if (request.getNewRoomId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phòng mới.");
        }
        if (request.getReasonType() == null) {
            throw new IllegalArgumentException("Vui lòng chọn lý do đổi phòng.");
        }
        if (request.getFeePolicy() == null) {
            throw new IllegalArgumentException("Vui lòng chọn chính sách phí.");
        }
        if (request.getOldRoomStatusAfterMove() == null) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái phòng cũ sau khi đổi.");
        }

        String note = normalizeText(request.getReasonNote());

        if (note.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập ghi chú/lý do chi tiết khi đổi phòng.");
        }
        if (note.length() > MAX_REASON_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú đổi phòng không được vượt quá 500 ký tự.");
        }
    }

    private void validateRoomMove(RoomMoveRequest request,
                                  Booking booking,
                                  BookingDetail detail,
                                  Room oldRoom,
                                  Room newRoom) {
        if (Objects.equals(oldRoom.getId(), newRoom.getId())) {
            throw new IllegalArgumentException("Phòng mới không được trùng với phòng hiện tại.");
        }

        if (oldRoom.getStatus() != RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Phòng hiện tại không ở trạng thái đang có khách.");
        }

        if (newRoom.getStatus() != RoomStatus.AVAILABLE) {
            throw new IllegalStateException("Phòng mới không khả dụng hoặc đang được sử dụng.");
        }

        if (request.getOldRoomStatusAfterMove() != RoomStatus.AVAILABLE
                && request.getOldRoomStatusAfterMove() != RoomStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Trạng thái phòng cũ sau khi đổi chỉ được là Available hoặc Maintenance.");
        }

        if (request.getReasonType() == RoomMoveReason.HOTEL_FAULT
                && request.getOldRoomStatusAfterMove() != RoomStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Nếu lỗi do phòng/khách sạn, phòng cũ cần chuyển sang Maintenance.");
        }

        if (!canFitGuests(detail, newRoom)) {
            throw new IllegalArgumentException("Phòng mới không đủ sức chứa cho số khách của phòng hiện tại.");
        }

        boolean blocked = roomRepository.existsRoomMoveBlockingBooking(
                newRoom.getId(),
                booking.getId(),
                detail.getCheckInDate(),
                detail.getCheckOutDate(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );

        if (blocked) {
            throw new IllegalStateException("Phòng mới đã được gán cho booking khác trong thời gian lưu trú.");
        }

        BigDecimal oldPricePerNight = normalizeMoney(detail.getPricePerNight());
        BigDecimal newPricePerNight = normalizeMoney(newRoom.getVariant().getPricePerNight());
        boolean isUpgrade = newPricePerNight.compareTo(oldPricePerNight) > 0;

        boolean isDowngrade = newPricePerNight.compareTo(oldPricePerNight) < 0;

        if (isDowngrade) {
            throw new IllegalArgumentException(
                    "Hiện tại chưa hỗ trợ đổi xuống hạng phòng. Vui lòng chọn phòng cùng hạng/cùng giá hoặc phòng nâng hạng có tính phụ thu."
            );
        }

        if (request.getReasonType() == RoomMoveReason.HOTEL_FAULT && isUpgrade) {
            throw new IllegalArgumentException("Hiện tại chỉ cho đổi phòng lỗi khách sạn sang phòng cùng hạng/cùng giá. Nếu khách muốn nâng hạng, hãy chọn lý do khách yêu cầu nâng hạng và tính phụ thu.");
        }

        if (isUpgrade && request.getFeePolicy() != RoomMoveFeePolicy.GUEST_UPGRADE_CHARGE) {
            throw new IllegalArgumentException("Phòng mới có giá cao hơn. Vui lòng chọn chính sách tính phụ thu nâng hạng.");
        }
    }

    private boolean canFitGuests(BookingDetail detail, Room room) {
        if (detail == null || room == null || room.getVariant() == null) {
            return false;
        }

        RoomTypeVariant variant = room.getVariant();

        int adults = detail.getNumAdults() == null ? 0 : detail.getNumAdults();
        int children = detail.getNumChildren() == null ? 0 : detail.getNumChildren();

        int maxAdults = variant.getMaxAdults() == null ? Integer.MAX_VALUE : variant.getMaxAdults();
        int maxChildren = variant.getMaxChildren() == null ? Integer.MAX_VALUE : variant.getMaxChildren();

        return adults <= maxAdults && children <= maxChildren;
    }

    private RoomMoveOptionResponse toRoomMoveOption(Room room, BigDecimal currentPrice) {
        RoomTypeVariant variant = room.getVariant();

        BigDecimal newPrice = normalizeMoney(variant.getPricePerNight());
        BigDecimal difference = newPrice.subtract(currentPrice);

        if (difference.compareTo(BigDecimal.ZERO) < 0) {
            difference = BigDecimal.ZERO;
        }

        return RoomMoveOptionResponse.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .variantId(variant.getId())
                .roomTypeName(variant.getRoomType() != null ? variant.getRoomType().getName() : null)
                .variantName(variant.getVariantName())
                .viewType(variant.getViewType() != null ? variant.getViewType().name() : null)
                .pricePerNight(newPrice)
                .priceDifferencePerNight(difference.setScale(0, RoundingMode.HALF_UP))
                .upgrade(difference.compareTo(BigDecimal.ZERO) > 0)
                .build();
    }

    private int calculateChargedNights(BookingDetail detail) {
        LocalDate today = LocalDate.now();
        LocalDate checkOutDate = detail.getCheckOutDate();

        long remainingNights = ChronoUnit.DAYS.between(today, checkOutDate);

        if (remainingNights <= 0) {
            return 1;
        }

        if (detail.getNumNights() != null && detail.getNumNights() > 0) {
            return (int) Math.min(remainingNights, detail.getNumNights());
        }

        return (int) remainingNights;
    }

    private FolioItem createUpgradeFolioItem(Booking booking,
                                             BookingDetail detail,
                                             Room oldRoom,
                                             Room newRoom,
                                             BigDecimal priceDifferencePerNight,
                                             int chargedNights,
                                             BigDecimal baseAmount,
                                             BigDecimal serviceChargeAmount,
                                             BigDecimal vatAmount,
                                             BigDecimal totalAmount,
                                             User currentStaff) {
        String description = "Phụ thu nâng hạng phòng "
                + oldRoom.getRoomNumber()
                + " -> "
                + newRoom.getRoomNumber();

        FolioItem item = FolioItem.builder()
                .booking(booking)
                .bookingDetail(detail)
                .service(null)
                .itemType(FolioItemType.ROOM_CHARGE)
                .serviceStatus(FolioItemStatus.COMPLETED)
                .description(description)
                .quantity(chargedNights)
                .unitPrice(priceDifferencePerNight.setScale(0, RoundingMode.HALF_UP))
                .baseAmount(baseAmount)
                .serviceChargeRate(SERVICE_CHARGE_RATE)
                .serviceChargeAmount(serviceChargeAmount)
                .vatRate(VAT_RATE)
                .vatAmount(vatAmount)
                .totalAmount(totalAmount)
                .amount(totalAmount)
                .priceDisplayMode(PriceDisplayMode.PLUS_PLUS)
                .postedBy(currentStaff)
                .postedAt(Instant.now())
                .adjustmentReason("Phụ thu nâng hạng phát sinh từ nghiệp vụ đổi phòng.")
                .isVoided(false)
                .build();

        return folioItemRepository.save(item);
    }

    private void applyUpgradeTotalsToBooking(Booking booking,
                                             BigDecimal baseAmount,
                                             BigDecimal serviceChargeAmount,
                                             BigDecimal vatAmount,
                                             BigDecimal totalAmount) {
        booking.setRoomSubtotal(normalizeMoney(booking.getRoomSubtotal()).add(baseAmount));
        booking.setServiceChargeTotal(normalizeMoney(booking.getServiceChargeTotal()).add(serviceChargeAmount));
        booking.setVatTotal(normalizeMoney(booking.getVatTotal()).add(vatAmount));
        booking.setTotalAmount(normalizeMoney(booking.getTotalAmount()).add(totalAmount));
        booking.setGrandTotal(normalizeMoney(booking.getGrandTotal()).add(totalAmount));
        booking.setAmountCalculatedAt(Instant.now());
    }

    private void applyUpgradeTotalsToBookingDetail(BookingDetail detail,
                                                   BigDecimal newPricePerNight,
                                                   BigDecimal baseAmount,
                                                   BigDecimal serviceChargeAmount,
                                                   BigDecimal vatAmount,
                                                   BigDecimal totalAmount) {
        detail.setPricePerNight(newPricePerNight.setScale(0, RoundingMode.HALF_UP));
        detail.setSubtotal(normalizeMoney(detail.getSubtotal()).add(baseAmount));
        detail.setServiceChargeAmount(normalizeMoney(detail.getServiceChargeAmount()).add(serviceChargeAmount));
        detail.setVatAmount(normalizeMoney(detail.getVatAmount()).add(vatAmount));
        detail.setTotalAmount(normalizeMoney(detail.getTotalAmount()).add(totalAmount));
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private User getCurrentStaffUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin nhân viên đang đăng nhập."));
    }
}