package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariantService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestServiceView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class GuestBookingService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ServiceRepository serviceRepository;
    private final FolioItemRepository folioItemRepository;
    private final ImageRepository imageRepository;

    private static final BigDecimal SERVICE_CHARGE_RATE = BigDecimal.valueOf(0.05);
    private static final BigDecimal VAT_RATE = BigDecimal.valueOf(0.08);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private static final boolean TAX_ON_SERVICE_CHARGE = true;

    @Transactional(readOnly = true)
    public GuestMyBookingView getMyBooking(Long bookingDetailId) {
        BookingDetail detail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phòng"));

        Booking booking = detail.getBooking();

        if (booking == null || Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Không tìm thấy booking");
        }

        GuestMyBookingView view = new GuestMyBookingView();

        view.setBookingId(booking.getId());
        view.setBookingDetailId(detail.getId());

        view.setBookingReference(booking.getBookingReference());
        view.setBookingStatus(booking.getStatus() == null ? null : booking.getStatus().name());
        view.setDepositStatus(booking.getDepositStatus() == null ? null : booking.getDepositStatus().name());
        view.setBookingDate(booking.getCreatedAt());

        String guestName = ((booking.getGuestFirstName() == null ? "" : booking.getGuestFirstName())
                + " "
                + (booking.getGuestLastName() == null ? "" : booking.getGuestLastName())).trim();

        view.setGuestName(guestName.isBlank() ? booking.getGuestEmail() : guestName);
        view.setGuestEmail(booking.getGuestEmail());
        view.setGuestPhone(booking.getGuestPhone());

        view.setRoomCode(detail.getRoomCode());

        Room room = detail.getRoom();

        if (room != null) {
            view.setRoomNumber(room.getRoomNumber());
            view.setFloor(room.getFloor());
            view.setRoomStatus(room.getStatus() == null ? null : room.getStatus().name());
        } else {
            view.setRoomNumber("Chưa phân phòng");
            view.setRoomStatus("Chưa phân phòng");
        }

        RoomTypeVariant variant = detail.getVariant();

        if (variant != null) {
            view.setVariantName(variant.getVariantName());
            view.setRoomTypeDescription(variant.getDescription());
            view.setViewType(variant.getViewType() == null ? null : variant.getViewType().name());
            view.setRoomSize(variant.getRoomSize());
            view.setCapacity(variant.getCapacity());
            view.setMaxAdults(variant.getMaxAdults());
            view.setMaxChildren(variant.getMaxChildren());
            view.setAllowExtraBed(variant.getAllowExtraBed());
            view.setMaxExtraBeds(variant.getMaxExtraBeds());
            view.setExtraBedPrice(variant.getExtraBedPrice());
            view.setExtraBedNote(variant.getExtraBedNote());

            RoomType roomType = variant.getRoomType();

            if (roomType != null) {
                view.setRoomTypeName(roomType.getName());
                view.setAmenities(mapAmenities(roomType));
            }

            view.setBeds(mapBeds(variant));
            view.setBedSummary(buildBedSummary(view.getBeds()));
            view.setIncludedServices(mapIncludedServices(variant));

            String roomImageUrl = findImageUrl(ImageEntityType.ROOM_TYPE_VARIANT, variant.getId());

            if (roomImageUrl == null && variant.getRoomType() != null) {
                roomImageUrl = findImageUrl(ImageEntityType.ROOM_TYPE, variant.getRoomType().getId());
            }

            view.setRoomImageUrl(roomImageUrl);
        }

        view.setCheckInDate(detail.getCheckInDate());
        view.setCheckOutDate(detail.getCheckOutDate());
        view.setNumNights(detail.getNumNights());

        view.setNumAdults(detail.getNumAdults());
        view.setNumChildren(detail.getNumChildren());
        view.setTotalRooms(booking.getTotalRooms());

        view.setPricePerNight(detail.getPricePerNight());
        view.setRoomSubtotal(detail.getSubtotal());
        view.setServiceChargeAmount(detail.getServiceChargeAmount());
        view.setVatAmount(detail.getVatAmount());
        view.setTotalAmount(detail.getTotalAmount());

        view.setServiceSummary(detail.getServiceSummary());
        view.setSpecialRequests(booking.getSpecialRequests());

        List<FolioItem> selectedItems =
                folioItemRepository.findByBookingDetail_IdAndServiceIsNotNullAndIsVoidedFalseOrderByPostedAtAsc(
                        detail.getId()
                );

        view.setSelectedServices(mapSelectedServices(selectedItems));

        return view;

    }

    @Transactional(readOnly = true)
    public List<GuestServiceView> getAvailableService(String category) {
        List<com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service> services =
                serviceRepository.findByIsDeletedFalseAndIsAvailableTrueOrderByNameAsc();

        String selectedCategory = category == null || category.isBlank()
                ? "ALL"
                : category.trim().toUpperCase();

        if (!"ALL".equals(selectedCategory)) {
            services = services.stream()
                    .filter(service -> service.getCategory() != null)
                    .filter(service -> service.getCategory().getType() != null)
                    .filter(service -> service.getCategory().getType().name().equals(selectedCategory))
                    .toList();
        }

        List<GuestServiceView> result = new ArrayList<>();

        for (com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service : services) {
            GuestServiceView view = new GuestServiceView();

            view.setServiceId(service.getId());
            view.setName(service.getName());
            view.setDescription(service.getDescription());
            view.setPrice(service.getPrice());

            if (service.getCategory() != null) {
                view.setCategoryName(service.getCategory().getName());
                view.setCategoryType(service.getCategory().getType());
            }

            view.setImageUrl(findImageUrl(ImageEntityType.SERVICE, service.getId()));

            result.add(view);
        }

        return result;
    }
    @Transactional
    public void addService(Long bookingDetailId, Long serviceId) {
        BookingDetail detail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phòng"));

        Booking booking = detail.getBooking();

        validateCanModifyService(booking);

        if (booking == null || Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Booking không hợp lệ");
        }

        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ"));

        if (Boolean.TRUE.equals(service.getIsDeleted()) || !Boolean.TRUE.equals(service.getIsAvailable())) {
            throw new IllegalArgumentException("Dịch vụ không khả dụng");
        }

        FolioItem item = folioItemRepository
                .findByBookingDetail_IdAndService_IdAndIsVoidedFalse(bookingDetailId, serviceId)
                .orElse(null);

        if (item == null) {
            item = new FolioItem();
            item.setBooking(booking);
            item.setBookingDetail(detail);
            item.setService(service);
            item.setDescription(service.getName());
            item.setItemType(resolveFolioItemType(service));
            item.setQuantity(1);
            item.setUnitPrice(zeroIfNull(service.getPrice()));
            item.setPostedAt(Instant.now());
            item.setIsVoided(false);
        } else {
            item.setQuantity(item.getQuantity() == null ? 1 : item.getQuantity() + 1);
        }

        recalculateFolioItem(item);
        folioItemRepository.save(item);
    }

    @Transactional
    public void increaseService(Long bookingDetailId, Long folioItemId) {
        FolioItem item = getValidGuestServiceItem(bookingDetailId, folioItemId);

        item.setQuantity(item.getQuantity() == null ? 1 : item.getQuantity() + 1);

        recalculateFolioItem(item);
        folioItemRepository.save(item);
    }

    @Transactional
    public void decreaseService(Long bookingDetailId, Long folioItemId) {
        FolioItem item = getValidGuestServiceItem(bookingDetailId, folioItemId);

        Integer currentQuantity = item.getQuantity() == null ? 1 : item.getQuantity();

        if (currentQuantity <= 1) {
            return;
        }

        item.setQuantity(currentQuantity - 1);

        recalculateFolioItem(item);
        folioItemRepository.save(item);
    }

    @Transactional
    public void removeService(Long bookingDetailId, Long folioItemId) {
        FolioItem item = getValidGuestServiceItem(bookingDetailId, folioItemId);

        item.setIsVoided(true);
        item.setVoidedAt(Instant.now());
        item.setVoidedReason("Khách xóa dịch vụ khỏi My Booking");

        folioItemRepository.save(item);
    }

    private FolioItem getValidGuestServiceItem(Long bookingDetailId, Long folioItemId) {
        if (bookingDetailId == null || bookingDetailId <= 0) {
            throw new IllegalArgumentException("Phiên phòng không hợp lệ.");
        }

        if (folioItemId == null || folioItemId <= 0) {
            throw new IllegalArgumentException("Dịch vụ đã chọn không hợp lệ.");
        }

        FolioItem item = folioItemRepository
                .findByIdAndBookingDetail_IdAndServiceIsNotNullAndIsVoidedFalse(
                        folioItemId,
                        bookingDetailId
                )
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ đã chọn."));

        Booking booking = item.getBooking();

        if (booking == null && item.getBookingDetail() != null) {
            booking = item.getBookingDetail().getBooking();
        }

        validateCanModifyService(booking);

        return item;
    }

    private void validateCanModifyService(Booking booking) {
        if (booking == null || Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Booking không hợp lệ.");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Chỉ có thể thêm hoặc chỉnh sửa dịch vụ trong thời gian đang lưu trú.");
        }
    }
    private void recalculateFolioItem(FolioItem item) {
        BigDecimal unitPrice = zeroIfNull(item.getUnitPrice());
        int quantity = item.getQuantity() == null ? 1 : item.getQuantity();

        BigDecimal baseAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        BigDecimal serviceChargeAmount = calculatePercent(baseAmount, SERVICE_CHARGE_RATE);

        BigDecimal vatBaseAmount = TAX_ON_SERVICE_CHARGE
                ? baseAmount.add(serviceChargeAmount)
                : baseAmount;

        BigDecimal vatAmount = calculatePercent(vatBaseAmount, VAT_RATE);

        BigDecimal totalAmount = baseAmount
                .add(serviceChargeAmount)
                .add(vatAmount);

        item.setBaseAmount(baseAmount);

        item.setServiceChargeRate(SERVICE_CHARGE_RATE);
        item.setServiceChargeAmount(serviceChargeAmount);

        item.setVatRate(VAT_RATE);
        item.setVatAmount(vatAmount);

        item.setTotalAmount(totalAmount);
    }
    private FolioItemType resolveFolioItemType(com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service) {
        if (service.getCategory() == null || service.getCategory().getType() == null) {
            return FolioItemType.ADJUSTMENT;
        }

        String type = service.getCategory().getType().name();

        if ("FOOD".equalsIgnoreCase(type)) {
            return FolioItemType.FOOD;
        }

        if ("WELLNESS".equalsIgnoreCase(type) || "SPA".equalsIgnoreCase(type)) {
            return FolioItemType.SPA;
        }

        return FolioItemType.ADJUSTMENT;
    }

    private List<GuestAmenityView> mapAmenities(RoomType roomType) {
        if (roomType == null || roomType.getAmenities() == null) {
            return new ArrayList<>();
        }

        return roomType.getAmenities()
                .stream()
                .filter(item -> item.getAmenity() != null)
                .filter(item -> !Boolean.TRUE.equals(item.getAmenity().getIsDeleted()))
                .sorted(Comparator.comparing(
                        RoomTypeAmenity::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .map(item -> {
                    GuestAmenityView view = new GuestAmenityView();
                    view.setAmenityId(item.getAmenity().getId());
                    view.setName(item.getAmenity().getName());
                    view.setIcon(item.getAmenity().getIcon());
                    view.setHighlighted(item.getIsHighlighted());
                    view.setSortOrder(item.getSortOrder());
                    return view;
                })
                .collect(Collectors.toList());
    }

    private List<GuestIncludedServiceView> mapIncludedServices(RoomTypeVariant variant) {
        if (variant == null || variant.getIncludedServices() == null) {
            return new ArrayList<>();
        }

        List<GuestIncludedServiceView> result = new ArrayList<>();

        for (RoomTypeVariantService item : variant.getIncludedServices()) {
            // Kiểm tra các điều kiện filter (loại bỏ null và các mục đã xóa)
            if (item == null) {
                continue;
            }
            if (Boolean.TRUE.equals(item.getIsDeleted())) {
                continue;
            }
            if (item.getService() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(item.getService().getIsDeleted())) {
                continue;
            }

            Service service = item.getService();
            GuestIncludedServiceView view = new GuestIncludedServiceView();

            view.setServiceId(service.getId());
            view.setServiceName(service.getName());
            view.setQuantity(item.getQuantity());
            view.setIncludedType(item.getIncludedType());
            view.setNote(item.getNote());

            if (service.getCategory() != null) {
                view.setCategoryName(service.getCategory().getName());
            }

            result.add(view);
        }

        return result;
    }

    private List<GuestBedView> mapBeds(RoomTypeVariant variant) {
        List<GuestBedView> result = new ArrayList<>();

        if (variant == null || variant.getBeds() == null) {
            return result;
        }

        for (RoomTypeVariantBed item : variant.getBeds()) {
            if (item == null) {
                continue;
            }

            if (item.getBedType() == null) {
                continue;
            }

            if (Boolean.TRUE.equals(item.getBedType().getIsDeleted())) {
                continue;
            }

            GuestBedView view = new GuestBedView();

            view.setBedTypeId(item.getBedType().getId());
            view.setBedTypeName(item.getBedType().getName());
            view.setQuantity(item.getQuantity());

            result.add(view);
        }

        return result;
    }

    private String buildBedSummary(List<GuestBedView> beds) {
        if (beds == null || beds.isEmpty()) {
            return "Chưa cập nhật";
        }

        return beds.stream()
                .map(bed -> bed.getQuantity() + " " + bed.getBedTypeName())
                .collect(Collectors.joining(", "));
    }

    private List<GuestSelectedServiceView> mapSelectedServices(List<FolioItem> items) {
        List<GuestSelectedServiceView> result = new ArrayList<>();

        if (items == null) {
            return result;
        }

        for (FolioItem item : items) {
            GuestSelectedServiceView view = new GuestSelectedServiceView();

            view.setFolioItemId(item.getId());

            if (item.getService() != null) {
                view.setServiceId(item.getService().getId());
                view.setServiceName(item.getService().getName());

                if (item.getService().getCategory() != null) {
                    view.setCategoryName(item.getService().getCategory().getName());
                }
            } else {
                view.setServiceName(item.getDescription());
            }

            view.setQuantity(item.getQuantity());
            view.setUnitPrice(item.getUnitPrice());
            view.setTotalAmount(item.getTotalAmount());

            result.add(view);
        }

        return result;
    }

    private List<GuestFolioTransactionView> buildFolioTransactions(
            Booking booking,
            BigDecimal baseBookingTotal,
            List<FolioItem> selectedItems,
            List<Payment> payments
    ) {
        List<GuestFolioTransactionView> transactions = new ArrayList<>();

        if (baseBookingTotal != null && baseBookingTotal.compareTo(BigDecimal.ZERO) > 0) {
            GuestFolioTransactionView roomCharge = new GuestFolioTransactionView();

            roomCharge.setId(null);
            roomCharge.setPostedAt(booking.getCreatedAt() == null ? Instant.now() : booking.getCreatedAt());
            roomCharge.setDescription("Tiền phòng");
            roomCharge.setCategory("Phòng");
            roomCharge.setChargeAmount(baseBookingTotal);
            roomCharge.setPaymentAmount(BigDecimal.ZERO);

            transactions.add(roomCharge);
        }

        if (selectedItems != null) {
            for (FolioItem item : selectedItems) {
                GuestFolioTransactionView tx = new GuestFolioTransactionView();

                tx.setId(item.getId());
                tx.setPostedAt(item.getPostedAt());
                tx.setDescription(item.getDescription());

                if (item.getService() != null && item.getService().getCategory() != null) {
                    tx.setCategory(item.getService().getCategory().getName());
                } else {
                    tx.setCategory("Dịch vụ");
                }

                tx.setChargeAmount(zeroIfNull(item.getTotalAmount()));
                tx.setPaymentAmount(BigDecimal.ZERO);

                transactions.add(tx);
            }
        }

        if (payments != null) {
            for (Payment payment : payments) {
                GuestFolioTransactionView tx = new GuestFolioTransactionView();

                tx.setId(payment.getId());
                tx.setPostedAt(payment.getPaidAt() == null ? Instant.now() : payment.getPaidAt());
                tx.setDescription(buildPaymentTransactionDescription(payment));
                tx.setCategory("Thanh toán");
                tx.setChargeAmount(BigDecimal.ZERO);
                tx.setPaymentAmount(zeroIfNull(payment.getAmount()));

                transactions.add(tx);
            }
        }

        transactions.sort(Comparator.comparing(
                GuestFolioTransactionView::getPostedAt,
                Comparator.nullsLast(Instant::compareTo)
        ));

        BigDecimal balance = BigDecimal.ZERO;

        for (GuestFolioTransactionView tx : transactions) {
            balance = balance
                    .add(zeroIfNull(tx.getChargeAmount()))
                    .subtract(zeroIfNull(tx.getPaymentAmount()));

            tx.setBalance(balance);
        }

        return transactions;
    }

    private String findImageUrl(ImageEntityType type, Long entityId) {
        if (type == null || entityId == null) {
            return null;
        }

        return imageRepository
                .findFirstByEntityTypeAndEntityIdAndIsPrimaryTrueOrderBySortOrderAsc(type, entityId)
                .or(() -> imageRepository.findFirstByEntityTypeAndEntityIdAndIsPrimaryTrueOrderBySortOrderAsc(type, entityId))
                .map(Image::getImageUrl)
                .orElse(null);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal firstNonNull(BigDecimal... values) {
        if (values == null) {
            return BigDecimal.ZERO;
        }

        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calculatePercent(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null) {
            return BigDecimal.ZERO;
        }

        return amount
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private String buildPaymentTransactionDescription(Payment payment) {
        if (payment == null || payment.getPaymentType() == null) {
            return "Thanh toán";
        }

        String methodLabel = payment.getMethod() == null
                ? ""
                : " - " + payment.getMethod().getLabel();

        return payment.getPaymentType().getLabel() + methodLabel;
    }



}
