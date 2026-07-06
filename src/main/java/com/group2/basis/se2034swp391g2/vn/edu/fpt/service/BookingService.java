package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DepositStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Booking;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CheckInProcedureResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.IdentityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Country;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import java.time.DayOfWeek;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ViewBookingDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Payment;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ServiceCategoryType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.FolioItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionApplyResponse;

import java.time.LocalTime;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.time.ZoneId;
import java.time.Instant;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.VariantServiceProjection;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 8;
    private static final int MAX_FULL_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 150;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_NOTES_LENGTH = 500;
    private static final int MAX_BOOKING_NIGHTS = 30;
    private static final BigDecimal VAT_RATE = new BigDecimal("0.08");
    private static final BigDecimal SERVICE_CHARGE_RATE = new BigDecimal("0.05");
    private static final boolean TAX_ON_SERVICE_CHARGE = true;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9()+\\s-]{8,20}$");
    private static final int MAX_IDENTITY_LENGTH = 30;
    private static final int MAX_CANCEL_REASON_LENGTH = 300;
    private static final int MAX_LATE_REASON_LENGTH = 300;

    private static final Pattern IDENTITY_PATTERN =
            Pattern.compile("^[A-Za-z0-9-]{6,30}$");

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final MailService mailService;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final RoomTypeVariantServiceRepository roomTypeVariantServiceRepository;
    private final ServiceRepository serviceRepository;
    private final FolioItemRepository folioItemRepository;
    private final InventoryManagementService inventoryManagementService;
    private final PromotionRepository promotionRepository;
    private final PromotionService promotionService;
    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          BookingDetailRepository bookingDetailRepository,
                          UserRepository userRepository,
                          CountryRepository countryRepository,
                          MailService mailService,
                          PaymentRepository paymentRepository,
                          RoomTypeVariantServiceRepository roomTypeVariantServiceRepository,
                          ServiceRepository serviceRepository,
                          FolioItemRepository folioItemRepository,
                          InventoryManagementService inventoryManagementService,
                          PromotionRepository promotionRepository,
                          PromotionService promotionService,
                          PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.bookingDetailRepository = bookingDetailRepository;
        this.userRepository = userRepository;
        this.countryRepository = countryRepository;
        this.mailService = mailService;
        this.paymentRepository = paymentRepository;
        this.roomTypeVariantServiceRepository = roomTypeVariantServiceRepository;
        this.serviceRepository = serviceRepository;
        this.folioItemRepository = folioItemRepository;
        this.inventoryManagementService = inventoryManagementService;
        this.promotionRepository = promotionRepository;
        this.promotionService = promotionService;
        this.paymentService = paymentService;
    }

    public List<BookingResponse> searchBookings(String keyword,
                                                String status,
                                                LocalDate checkIn,
                                                LocalDate checkOut) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        String searchStatus = status == null ? "" : status.trim();

        return bookingRepository.searchBookingList(searchKeyword, searchStatus, checkIn, checkOut);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> searchBookingsPaging(String keyword,
                                                      String status,
                                                      LocalDate checkIn,
                                                      LocalDate checkOut,
                                                      Pageable pageable) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        String searchStatus = status == null ? "" : status.trim();

        return bookingRepository.searchBookingListPagingRaw(
                searchKeyword,
                searchStatus,
                checkIn,
                checkOut,
                pageable
        ).map(row -> new BookingResponse(
                row.getBookingId(),
                row.getBookingReference(),
                row.getGuestName(),
                buildRoomDisplay(row.getVariantName(), row.getRoomNumber()),
                row.getCheckInDate(),
                row.getCheckOutDate(),
                row.getStatus(),
                row.getTotalAmount()
        ));
    }
    private String buildRoomDisplay(String variantName, String roomNumber) {
        String roomText = roomNumber == null || roomNumber.isBlank()
                ? "Chưa phân phòng"
                : "Phòng " + roomNumber;

        return variantName + " - " + roomText;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate checkInDate, LocalDate checkOutDate) {
        validateBookingDates(checkInDate, checkOutDate);

        List<RoomResponse> rooms = roomRepository.findAvailableRooms(
                checkInDate,
                checkOutDate,
                RoomStatus.AVAILABLE,
                List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );

        List<Long> variantIds = rooms.stream()
                .map(RoomResponse::getVariantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (variantIds.isEmpty()) {
            return rooms;
        }

        List<VariantServiceProjection> services =
                roomTypeVariantServiceRepository.findIncludedServicesByVariantIds(variantIds);

        Map<Long, List<String>> serviceMap = services.stream()
                .collect(Collectors.groupingBy(
                        VariantServiceProjection::getVariantId,
                        Collectors.mapping(service -> {
                            String serviceName = service.getServiceName();
                            Integer quantity = service.getQuantity() == null ? 1 : service.getQuantity();
                            String includedType = service.getIncludedType();

                            if (includedType == null || includedType.isBlank()) {
                                return serviceName + " x" + quantity;
                            }

                            return serviceName + " x" + quantity + " (" + includedType + ")";
                        }, Collectors.toList())
                ));

        for (RoomResponse room : rooms) {
            room.setIncludedServices(
                    serviceMap.getOrDefault(room.getVariantId(), Collections.emptyList())
            );
        }

        return rooms;
    }

    @Transactional
    public Long addWalkInBooking(BookingCreateRequest request) {
        validateCreateBookingRequest(request);

        LocalDate checkInDate = request.getCheckInDate();
        LocalDate checkOutDate = request.getCheckOutDate();
        User currentStaff = getCurrentStaffUser();
        BigDecimal vatRate = VAT_RATE;
        BigDecimal serviceChargeRate = SERVICE_CHARGE_RATE;

        BigDecimal vatRateDecimal = vatRate;
        BigDecimal serviceChargeRateDecimal = serviceChargeRate;
        List<Room> selectedRooms = roomRepository.findAvailableRoomsByIds(
                request.getRoomIds(),
                checkInDate,
                checkOutDate,
                RoomStatus.AVAILABLE,
                List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );

        if (selectedRooms.size() != request.getRoomIds().size()) {
            throw new IllegalArgumentException("Một hoặc nhiều phòng đã chọn không còn khả dụng. Vui lòng chọn lại phòng.");
        }

        validateRoomCapacity(selectedRooms, request);
        User guest = createGuestFromRequest(request);


        BookingStatus bookingStatus = BookingStatus.CHECKED_IN;

        Booking booking = Booking.builder()
                .guestFirstName(request.getFirstName().trim())
                .guestLastName(request.getLastName().trim())
                .guestPhone(request.getPhoneNumber().trim())
                .guestEmail(request.getEmail().trim())
                .guest(guest)
                .createdBy(currentStaff)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(request.getAdults())
                .numChildren(request.getChildren())
                .totalRooms(selectedRooms.size())
                .specialRequests(request.getNotes())
                .bookingReference(generateBookingReference())
                .depositStatus(DepositStatus.UNPAID)
                .status(bookingStatus)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .isDeleted(false)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        

        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        List<BookingDetail> bookingDetails = new ArrayList<>();
        BigDecimal roomSubtotalTotal = BigDecimal.ZERO;
        BigDecimal roomServiceChargeTotal = BigDecimal.ZERO;
        BigDecimal roomVatTotal = BigDecimal.ZERO;

        for (Room room : selectedRooms) {
            BigDecimal pricePerNight = room.getVariant().getPricePerNight();

            BigDecimal roomSubtotal = calculateRoomAmount(room, checkInDate, checkOutDate);

            int extraBedCount = getExtraBedCount(request, room.getId());

            if (extraBedCount > 0 && !Boolean.TRUE.equals(room.getVariant().getAllowExtraBed())) {
                throw new IllegalArgumentException("Phòng " + room.getRoomNumber() + " không hỗ trợ giường phụ.");
            }

            if (extraBedCount > 0 && room.getVariant().getMaxExtraBeds() < extraBedCount) {
                throw new IllegalArgumentException("Phòng " + room.getRoomNumber() + " vượt quá số lượng giường phụ cho phép.");
            }

            BigDecimal extraBedPrice = room.getVariant().getExtraBedPrice() == null
                    ? BigDecimal.ZERO
                    : room.getVariant().getExtraBedPrice();

            BigDecimal extraBedTotal = extraBedPrice
                    .multiply(BigDecimal.valueOf(extraBedCount))
                    .multiply(BigDecimal.valueOf(nights));

            // Tổng tiền phòng + giường phụ trước phí và thuế
            BigDecimal detailBaseAmount = roomSubtotal.add(extraBedTotal);

            // Phí dịch vụ cho dòng phòng
            BigDecimal detailServiceChargeAmount = detailBaseAmount
                    .multiply(serviceChargeRateDecimal)
                    .setScale(0, RoundingMode.HALF_UP);

            // VAT base
            BigDecimal detailVatBase = detailBaseAmount;

            if (TAX_ON_SERVICE_CHARGE) {
                detailVatBase = detailVatBase.add(detailServiceChargeAmount);
            }

            // VAT cho dòng phòng
            BigDecimal detailVatAmount = detailVatBase
                    .multiply(vatRateDecimal)
                    .setScale(0, RoundingMode.HALF_UP);

            // Tổng từng dòng sau phí dịch vụ và VAT
            BigDecimal detailTotalWithVat = detailBaseAmount
                    .add(detailServiceChargeAmount)
                    .add(detailVatAmount);

            BookingDetail.BookingDetailBuilder detailBuilder = BookingDetail.builder()
                    .booking(savedBooking)
                    .room(room)
                    .variant(room.getVariant())
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .pricePerNight(pricePerNight)
                    .numNights((int) nights)
                    .numAdults(request.getAdults())
                    .numChildren(request.getChildren())
                    .childAges(formatChildAges(request.getChildAges()))
                    .subtotal(roomSubtotal)
                    .extraBedCount(extraBedCount)
                    .extraBedPrice(extraBedPrice)
                    .extraBedTotal(extraBedTotal)
                    .serviceChargeRate(serviceChargeRate)
                    .serviceChargeAmount(detailServiceChargeAmount)
                    .vatRate(vatRate)
                    .vatAmount(detailVatAmount)
                    .totalAmount(detailTotalWithVat);

            if (bookingStatus == BookingStatus.CHECKED_IN) {
                detailBuilder.roomCode(generateRoomCode());
                detailBuilder.roomCodeExpiresAt(generateRoomCodeExpiresAt(checkOutDate));

                room.setStatus(RoomStatus.OCCUPIED);
            }

            BookingDetail detail = detailBuilder.build();

            bookingDetails.add(detail);
            roomSubtotalTotal = roomSubtotalTotal.add(detailBaseAmount);
            roomServiceChargeTotal = roomServiceChargeTotal.add(detailServiceChargeAmount);
            roomVatTotal = roomVatTotal.add(detailVatAmount);
        }

        bookingDetailRepository.saveAll(bookingDetails);

        BigDecimal serviceSubtotal = createAdditionalServiceFolioItems(
                savedBooking,
                bookingDetails,
                request,
                currentStaff
        );

        BigDecimal additionalServiceChargeTotal = serviceSubtotal
                .multiply(serviceChargeRateDecimal)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal serviceVatBase = serviceSubtotal;

        if (TAX_ON_SERVICE_CHARGE) {
            serviceVatBase = serviceVatBase.add(additionalServiceChargeTotal);
        }

        BigDecimal serviceVatTotal = serviceVatBase
                .multiply(vatRateDecimal)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal serviceChargeTotal = roomServiceChargeTotal
                .add(additionalServiceChargeTotal);

        BigDecimal vatTotal = roomVatTotal
                .add(serviceVatTotal);

        BigDecimal totalBeforeDiscount = roomSubtotalTotal
                .add(serviceSubtotal)
                .add(serviceChargeTotal)
                .add(vatTotal);

        Promotion promotion = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
            PromotionApplyResponse promotionResult = promotionService.applyPromotionCode(
                    request.getPromoCode(),
                    totalBeforeDiscount
            );

            if (!promotionResult.isValid()) {
                throw new IllegalArgumentException(promotionResult.getMessage());
            }

            discountAmount = promotionResult.getDiscountAmount() == null
                    ? BigDecimal.ZERO
                    : promotionResult.getDiscountAmount().setScale(0, RoundingMode.HALF_UP);

            if (discountAmount.compareTo(totalBeforeDiscount) > 0) {
                discountAmount = totalBeforeDiscount;
            }

            promotion = promotionRepository.findById(promotionResult.getPromotionId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã ưu đãi đã chọn."));

            if (guest != null && guest.getId() != null) {
                boolean alreadyUsed = bookingRepository.existsUsedPromotionByGuest(
                        guest.getId(),
                        promotion.getId()
                );

                if (alreadyUsed) {
                    throw new IllegalArgumentException("Khách hàng này đã sử dụng mã ưu đãi này trước đó.");
                }
            }

            int usageCount = promotion.getUsageCount() == null ? 0 : promotion.getUsageCount();
            promotion.setUsageCount(usageCount + 1);
            promotionRepository.save(promotion);
        }

        BigDecimal grandTotal = totalBeforeDiscount.subtract(discountAmount);

        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }


        savedBooking.setPromotion(promotion);
        savedBooking.setDiscountAmount(discountAmount);
        savedBooking.setRoomSubtotal(roomSubtotalTotal);
        savedBooking.setServiceSubtotal(serviceSubtotal);
        savedBooking.setServiceChargeTotal(serviceChargeTotal);
        savedBooking.setVatTotal(vatTotal);
        savedBooking.setTotalAmount(grandTotal);
        savedBooking.setGrandTotal(grandTotal);
        savedBooking.setDepositAmount(BigDecimal.ZERO);
        savedBooking.setDepositStatus(DepositStatus.UNPAID);
        savedBooking.setAmountCalculatedAt(Instant.now());

        bookingRepository.save(savedBooking);


        if (bookingStatus == BookingStatus.CHECKED_IN) {
            roomRepository.saveAll(selectedRooms);
        }
        return savedBooking.getId();
    }

    public User getCurrentStaffUser() {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin nhân viên đang đăng nhập."));
    }
    @Transactional
    public Booking getBookingEntityById(Long bookingId){
        if (bookingId == null){
            throw new IllegalArgumentException("Thiếu mã đặt phòng");
        }
        return bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));
    }

    @Transactional
    public void confirmServiceServed(Long bookingId, Long folioItemId) {
        FolioItem folioItem = getServiceFolioItem(bookingId, folioItemId);
        FolioItemStatus currentStatus = getEffectiveServiceStatus(folioItem);

        if (currentStatus == FolioItemStatus.COMPLETED) {
            throw new IllegalArgumentException("Dịch vụ này đã được xác nhận phục vụ.");
        }
        if (currentStatus == FolioItemStatus.CANCELLED) {
            throw new IllegalArgumentException("Không thể xác nhận dịch vụ đã hủy.");
        }
        if (currentStatus == FolioItemStatus.NOT_USED_NO_REFUND) {
            throw new IllegalArgumentException("Không thể xác nhận dịch vụ khách đã báo không sử dụng.");
        }
        inventoryManagementService.consumeForService(
                folioItem.getService(),
                BigDecimal.valueOf(folioItem.getQuantity() == null ? 1 : folioItem.getQuantity()),
                folioItem.getId(),
                getCurrentStaffUser()
        );

        folioItem.setServiceStatus(FolioItemStatus.COMPLETED);
        folioItemRepository.save(folioItem);
    }

    @Transactional
    public void markServiceNotUsedNoRefund(Long bookingId, Long folioItemId) {
        FolioItem folioItem = getServiceFolioItem(bookingId, folioItemId);
        FolioItemStatus currentStatus = getEffectiveServiceStatus(folioItem);

        if (currentStatus == FolioItemStatus.COMPLETED) {
            throw new IllegalArgumentException("Dịch vụ đã phục vụ nên không thể ghi nhận không sử dụng.");
        }
        if (currentStatus == FolioItemStatus.CANCELLED) {
            throw new IllegalArgumentException("Dịch vụ này đã được hủy.");
        }
        if (currentStatus == FolioItemStatus.NOT_USED_NO_REFUND) {
            throw new IllegalArgumentException("Dịch vụ này đã được ghi nhận không sử dụng và không hoàn tiền.");
        }

        folioItem.setServiceStatus(FolioItemStatus.NOT_USED_NO_REFUND);
        folioItemRepository.save(folioItem);
    }

    @Transactional
    public void cancelRequestedService(Long bookingId, Long folioItemId) {
        FolioItem folioItem = getServiceFolioItem(bookingId, folioItemId);
        FolioItemStatus currentStatus = getEffectiveServiceStatus(folioItem);

        if (currentStatus == FolioItemStatus.COMPLETED) {
            throw new IllegalArgumentException("Dịch vụ đã phục vụ nên không thể hủy.");
        }
        if (currentStatus == FolioItemStatus.CANCELLED) {
            throw new IllegalArgumentException("Dịch vụ này đã được hủy.");
        }
        if (currentStatus == FolioItemStatus.NOT_USED_NO_REFUND) {
            throw new IllegalArgumentException("Dịch vụ này đã được ghi nhận không sử dụng và không hoàn tiền.");
        }

        folioItem.setServiceStatus(FolioItemStatus.CANCELLED);
        folioItemRepository.save(folioItem);
    }

    private FolioItem getServiceFolioItem(Long bookingId, Long folioItemId) {
        if (bookingId == null || folioItemId == null) {
            throw new IllegalArgumentException("Thiếu thông tin dịch vụ cần xử lý.");
        }

        FolioItem folioItem = folioItemRepository.findById(folioItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ trong booking."));

        if (Boolean.TRUE.equals(folioItem.getIsVoided())
                || folioItem.getBooking() == null
                || !bookingId.equals(folioItem.getBooking().getId())
                || folioItem.getService() == null) {
            throw new IllegalArgumentException("Dịch vụ không thuộc booking này hoặc đã bị hủy chứng từ.");
        }

        Booking booking = folioItem.getBooking();
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Chỉ có thể xử lý dịch vụ sau khi khách đã nhận phòng.");
        }

        return folioItem;
    }

    private FolioItemStatus getEffectiveServiceStatus(FolioItem folioItem) {
        return folioItem.getServiceStatus() == null ? FolioItemStatus.REQUESTED : folioItem.getServiceStatus();
    }

    @Transactional
    public void markDepositPaid(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        booking.setDepositStatus(DepositStatus.PAID);
        bookingRepository.save(booking);
    }

    @Transactional
    public void collectBookingPayment(Long bookingId,
                                      PaymentType paymentType,
                                      com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod method,
                                      BigDecimal amount) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Thiếu mã đặt phòng.");
        }

        if (paymentType == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại thanh toán.");
        }

        if (paymentType == PaymentType.REFUND) {
            throw new IllegalArgumentException("Không thể ghi nhận hoàn tiền bằng chức năng thu tiền.");
        }

        if (method == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Đặt phòng này đã bị xóa.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.NO_SHOW
                || booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("Không thể thu tiền cho booking đã hủy, no-show hoặc đã trả phòng.");
        }

        BigDecimal grandTotal = booking.getGrandTotal() == null
                ? BigDecimal.ZERO
                : booking.getGrandTotal();

        BigDecimal paidAmount = paymentRepository.findByBookingId(bookingId).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .filter(payment -> payment.getPaymentType() != PaymentType.REFUND)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = grandTotal.subtract(paidAmount);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Booking này đã được thanh toán đủ.");
        }

        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không được vượt quá số tiền còn lại.");
        }

        paymentService.createPayment(
                booking,
                paymentType,
                method,
                amount.setScale(0, RoundingMode.HALF_UP),
                getCurrentStaffUser()
        );
    }

    private void validateCreateBookingRequest(BookingCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin đặt phòng không được để trống.");
        }

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên của khách.");
        }

        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập họ của khách.");
        }

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();

        if (firstName.length() > 50) {
            throw new IllegalArgumentException("Tên của khách không được vượt quá 50 ký tự.");
        }

        if (lastName.length() > 50) {
            throw new IllegalArgumentException("Họ của khách không được vượt quá 50 ký tự.");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại của khách.");
        }

        String phoneNumber = request.getPhoneNumber().trim();
        if (phoneNumber.length() > MAX_PHONE_LENGTH) {
            throw new IllegalArgumentException("Số điện thoại không được vượt quá 20 ký tự.");
        }

        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng.");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email của khách.");
        }

        String email = request.getEmail().trim();

        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Email không được vượt quá 150 ký tự.");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email không đúng định dạng.");
        }

        if (request.getCountryId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn quốc gia của khách.");
        }
        if (request.getGender() == null) {
            throw new IllegalArgumentException("Vui lòng chọn giới tính của khách.");
        }
        if (request.getIdentityNumber() == null || request.getIdentityNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số giấy tờ tùy thân.");
        }

        String identityNumber = request.getIdentityNumber().trim();

        if (identityNumber.length() > MAX_IDENTITY_LENGTH) {
            throw new IllegalArgumentException("Số giấy tờ tùy thân không được vượt quá 30 ký tự.");
        }

        if (!IDENTITY_PATTERN.matcher(identityNumber).matches()) {
            throw new IllegalArgumentException("Số giấy tờ tùy thân chỉ được chứa chữ, số hoặc dấu gạch ngang và phải từ 6 đến 30 ký tự.");
        }

        if (request.getBirthYear() != null) {
            int currentYear = LocalDate.now().getYear();

            if (request.getBirthYear() < 1900 || request.getBirthYear() > currentYear) {
                throw new IllegalArgumentException("Năm sinh không hợp lệ.");
            }
        }

        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        validatePassportExpiryDate(
                request.getCountryId(),
                request.getPassportExpiryDate(),
                request.getCheckOutDate()
        );

        if (request.getAdults() == null || request.getAdults() < 1) {
            throw new IllegalArgumentException("Số người lớn phải ít nhất là 1.");
        }

        if (request.getChildren() == null || request.getChildren() < 0) {
            throw new IllegalArgumentException("Số trẻ em không được nhỏ hơn 0.");
        }
        if (request.getChildren() != null && request.getChildren() > 0) {
            if (request.getChildAges() == null || request.getChildAges().size() != request.getChildren()) {
                throw new IllegalArgumentException("Vui lòng nhập đủ độ tuổi cho từng trẻ em.");
            }

            for (Integer age : request.getChildAges()) {
                if (age == null || age < 0 || age > 12) {
                    throw new IllegalArgumentException("Tuổi trẻ em phải nằm trong khoảng từ 0 đến 12.");
                }
            }
        }

        int totalGuests = request.getAdults() + request.getChildren();
        if (totalGuests < 1) {
            throw new IllegalArgumentException("Tổng số khách phải ít nhất là 1.");
        }

        if (request.getRoomIds() == null || request.getRoomIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một phòng.");
        }

        Set<Long> uniqueRoomIds = new HashSet<>(request.getRoomIds());
        if (uniqueRoomIds.size() != request.getRoomIds().size()) {
            throw new IllegalArgumentException("Không được chọn trùng phòng.");
        }

        if (request.getNotes() != null && request.getNotes().length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }

        if (!"create-check-in".equals(request.getAction())) {
            throw new IllegalArgumentException("Đặt phòng trực tiếp chỉ cho phép tạo và nhận phòng ngay.");
        }

        if ("create-check-in".equals(request.getAction())
                && request.getCheckInDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể nhận phòng trước ngày nhận phòng.");
        }

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            if (request.getServiceRoomIds() == null
                    || request.getServiceQuantities() == null
                    || request.getServiceRoomIds().size() != request.getServiceIds().size()
                    || request.getServiceQuantities().size() != request.getServiceIds().size()) {
                throw new IllegalArgumentException("Thông tin dịch vụ theo phòng không hợp lệ.");
            }

            for (Integer quantity : request.getServiceQuantities()) {
                if (quantity == null || quantity < 1) {
                    throw new IllegalArgumentException("Số lượng dịch vụ phải ít nhất là 1.");
                }
            }
        }
    }
    private String formatChildAges(List<Integer> childAges) {
        if (childAges == null || childAges.isEmpty()) {
            return null;
        }

        return childAges.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
    private void validateBookingDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày nhận phòng và ngày trả phòng.");
        }

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày nhận phòng không được là ngày trong quá khứ.");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng.");
        }

        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        if (nights > MAX_BOOKING_NIGHTS) {
            throw new IllegalArgumentException("Thời gian đặt phòng không được vượt quá 30 đêm.");
        }
    }

    private User createGuestFromRequest(BookingCreateRequest request) {
        String email = request.getEmail().trim();
        String phone = request.getPhoneNumber().trim();
        String identityNumber = request.getIdentityNumber().trim();

        User existingUser = userRepository
                .findByIdentityNumberAndIsDeletedFalse(identityNumber)
                .or(() -> userRepository.findByEmailAndIsDeletedFalse(email))
                .or(() -> userRepository.findByPhoneAndIsDeletedFalse(phone))
                .orElse(null);

        if (existingUser != null) {
            return existingUser;
        }

        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quốc gia đã chọn."));

        IdentityType identityType = resolveIdentityType(country);

        User guest = User.builder()
                .userType(UserType.GUEST)
                .approvalStatus(ApprovalStatus.APPROVED)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(phone)
                .email(email)
                .gender(request.getGender())
                .birthYear(request.getBirthYear())
                .country(country)
                .identityType(identityType)
                .identityNumber(identityNumber)
                .passportExpiryDate(identityType == IdentityType.PASSPORT
                        ? request.getPassportExpiryDate()
                        : null)
                .isActive(true)
                .isDeleted(false)
                .totalStays(0)
                .totalSpent(BigDecimal.ZERO)
                .build();

        return userRepository.save(guest);
    }

    private IdentityType resolveIdentityType(Country country) {
        if (country == null || country.getCountryName() == null) {
            return IdentityType.PASSPORT;
        }

        String countryName = country.getCountryName().trim().toLowerCase();

        if (countryName.equals("vietnam") || countryName.equals("việt nam")) {
            return IdentityType.CCCD;
        }

        return IdentityType.PASSPORT;
    }

    private void validateRoomCapacity(List<Room> selectedRooms, BookingCreateRequest request) {
        int requiredAdults = request.getAdults() == null ? 0 : request.getAdults();
        int requiredChildren = request.getChildren() == null ? 0 : request.getChildren();

        int totalAdultCapacity = selectedRooms.stream()
                .map(Room::getVariant)
                .mapToInt(variant -> variant.getMaxAdults() == null ? 0 : variant.getMaxAdults())
                .sum();

        int totalChildCapacity = selectedRooms.stream()
                .map(Room::getVariant)
                .mapToInt(variant -> variant.getMaxChildren() == null ? 0 : variant.getMaxChildren())
                .sum();

        if (totalAdultCapacity < requiredAdults || totalChildCapacity < requiredChildren) {
            throw new IllegalArgumentException(
                    "Các phòng đã chọn chưa đủ sức chứa. Cần "
                            + requiredAdults + " người lớn, "
                            + requiredChildren + " trẻ em. Sức chứa đã chọn: "
                            + totalAdultCapacity + " người lớn, "
                            + totalChildCapacity + " trẻ em."
            );
        }
    }


    private String generateBookingReference() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String reference;

        do {
            int randomNumber = (int) (Math.random() * 9000) + 1000;
            reference = "BK-" + datePart + randomNumber;
        } while (bookingRepository.existsByBookingReference(reference));

        return reference;
    }

    private String generateRoomCode(){
       String code;

       do {
           StringBuilder builder = new StringBuilder();

           for (int i = 0 ; i <ROOM_CODE_LENGTH; i++){
               int index = (int) (Math.random() * ROOM_CODE_CHARS.length());
               builder.append(ROOM_CODE_CHARS.charAt(index));
           }
           code = builder.toString();
       } while (bookingDetailRepository.existsByRoomCode(code));
       return  code;
    }

    private Instant generateRoomCodeExpiresAt(LocalDate checkOutDate){
        return checkOutDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @Transactional(readOnly = true)
    public List<BookingDetailResponse> getCheckInCompleteInfo(Long bookingId) {
        List<BookingDetailResponse> roomCodes =
                bookingDetailRepository.findRoomCodesByBookingId(bookingId);

        if (roomCodes.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã phòng cho mã đặt phòng: " + bookingId);
        }

        return roomCodes;
    }

    @Transactional(readOnly = true)
    public CheckInProcedureResponse getCheckInProcedure(Long bookingId) {
        CheckInProcedureResponse response = bookingRepository.findCheckInProcedureByBookingId(bookingId);

        if (response == null) {
            throw new IllegalArgumentException("Không tìm thấy đặt phòng.");
        }

        if (!"CONFIRMED".equals(response.getStatus())) {
            throw new IllegalArgumentException("Chỉ có đặt phòng đã xác nhận mới được nhận phòng.");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAssignedRoomsForCheckIn(Long bookingId) {
        return bookingDetailRepository.findAssignedRoomsByBookingId(bookingId);
    }

    @Transactional
    public Long confirmCheckIn(Long bookingId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Thiếu mã đặt phòng.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Đặt phòng này đã bị xóa.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Chỉ có đặt phòng đã xác nhận mới được nhận phòng.");
        }

        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể nhận phòng trước ngày nhận phòng.");
        }

        List<BookingDetail> details = bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);

        if (details.isEmpty()) {
            throw new IllegalArgumentException("Đặt phòng này chưa có phòng được gán.");
        }

        for (BookingDetail detail : details) {
            Room room = detail.getRoom();

            if (room == null) {
                throw new IllegalArgumentException("Chi tiết đặt phòng chưa được gán phòng.");
            }

            if (Boolean.TRUE.equals(room.getIsDeleted())) {
                throw new IllegalArgumentException("Phòng " + room.getRoomNumber() + " đã bị xóa.");
            }

            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new IllegalArgumentException("Phòng " + room.getRoomNumber() + " hiện không khả dụng.");
            }

            if (detail.getRoomCode() == null || detail.getRoomCode().isBlank()) {
                detail.setRoomCode(generateRoomCode());
            }

            detail.setRoomCodeExpiresAt(generateRoomCodeExpiresAt(booking.getCheckOutDate()));

            room.setStatus(RoomStatus.OCCUPIED);
        }

        booking.setStatus(BookingStatus.CHECKED_IN);

        bookingRepository.save(booking);
        bookingDetailRepository.saveAll(details);

        return booking.getId();
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getConfirmedBookingsForCheckIn(
            String keyword,
            Long roomTypeId,
            LocalDate checkInDate,
            String status,
            Pageable pageable
    ) {
        return bookingRepository.findConfirmedBookingsForCheckIn(
                keyword == null ? "" : keyword.trim(),
                roomTypeId,
                checkInDate,
                status == null || status.isBlank() ? "CONFIRMED" : status,
                pageable
        );
    }

    @Transactional
    public void sendRoomCodeEmail(Long bookingDetailId) {
        if (bookingDetailId == null) {
            throw new IllegalArgumentException("Thiếu mã chi tiết đặt phòng.");
        }

        BookingDetail detail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết đặt phòng."));

        Booking booking = detail.getBooking();

        if (booking == null) {
            throw new IllegalArgumentException("Không tìm thấy đặt phòng.");
        }

        if (booking.getGuestEmail() == null || booking.getGuestEmail().isBlank()) {
            throw new IllegalArgumentException("Khách chưa có email để gửi thông tin phòng.");
        }

        if (detail.getRoom() == null) {
            throw new IllegalArgumentException("Phòng chưa được gán.");
        }

        if (detail.getRoomCode() == null || detail.getRoomCode().isBlank()) {
            detail.setRoomCode(generateRoomCode());
        }

        if (detail.getRoomCodeExpiresAt() == null) {
            detail.setRoomCodeExpiresAt(generateRoomCodeExpiresAt(detail.getCheckOutDate()));
        }

        bookingDetailRepository.save(detail);

        String guestName = (booking.getGuestFirstName() + " " + booking.getGuestLastName()).trim();

        mailService.sendRoomCodeEmail(
                booking.getGuestEmail(),
                guestName,
                booking.getBookingReference(),
                detail.getRoom().getRoomNumber(),
                detail.getRoomCode(),
                String.valueOf(detail.getCheckInDate()),
                String.valueOf(detail.getCheckOutDate())
        );
    }

    @Transactional
    public void sendRoomCodesEmail(Long bookingId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Thiếu mã đặt phòng.");
        }

        List<BookingDetail> details = bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);

        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy mã phòng cho đặt phòng này.");
        }

        Booking booking = details.get(0).getBooking();

        if (booking == null) {
            throw new IllegalArgumentException("Không tìm thấy đặt phòng.");
        }

        if (booking.getGuestEmail() == null || booking.getGuestEmail().isBlank()) {
            throw new IllegalArgumentException("Khách chưa có email để gửi thông tin phòng.");
        }

        StringBuilder emailContent = new StringBuilder();
        emailContent.append("Thông tin truy cập phòng của bạn:\n\n");

        for (BookingDetail detail : details) {
            if (detail.getRoom() == null) {
                throw new IllegalArgumentException("Phòng chưa được gán.");
            }

            if (detail.getRoomCode() == null || detail.getRoomCode().isBlank()) {
                detail.setRoomCode(generateRoomCode());
            }

            if (detail.getRoomCodeExpiresAt() == null) {
                detail.setRoomCodeExpiresAt(generateRoomCodeExpiresAt(detail.getCheckOutDate()));
            }

            emailContent
                    .append("Số phòng: ")
                    .append(detail.getRoom().getRoomNumber())
                    .append("\n")

                    .append("Loại phòng: ")
                    .append(detail.getVariant().getRoomType().getName());

            if (detail.getVariant().getViewType() != null) {
                emailContent
                        .append(" - ")
                        .append(detail.getVariant().getViewType());
            }

            emailContent
                    .append("\n")
                    .append("Mã phòng: ")
                    .append(detail.getRoomCode())
                    .append("\n")
                    .append("Ngày nhận phòng: ")
                    .append(detail.getCheckInDate())
                    .append("\n")
                    .append("Ngày trả phòng: ")
                    .append(detail.getCheckOutDate())
                    .append("\n")
                    .append("Có hiệu lực đến: ")
                    .append(detail.getRoomCodeExpiresAt())
                    .append("\n\n");
        }

        bookingDetailRepository.saveAll(details);

        String guestName = (booking.getGuestFirstName() + " " + booking.getGuestLastName()).trim();

        mailService.sendRoomCodesEmail(
                booking.getGuestEmail(),
                guestName,
                booking.getBookingReference(),
                emailContent.toString()
        );
    }

    private BigDecimal calculateRoomAmount(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = checkInDate;

        while (date.isBefore(checkOutDate)) {
            BigDecimal nightlyPrice = room.getVariant().getPricePerNight();

            DayOfWeek day = date.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                nightlyPrice = nightlyPrice.multiply(BigDecimal.valueOf(1.10));
            }

            total = total.add(nightlyPrice);
            date = date.plusDays(1);
        }

        return total.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private int getExtraBedCount(BookingCreateRequest request, Long roomId) {
        if (request.getExtraBedCounts() == null) {
            return 0;
        }

        Integer count = request.getExtraBedCounts().get(roomId);

        if (count == null || count < 0) {
            return 0;
        }

        return count;
    }

    @Transactional(readOnly = true)
    public ViewBookingDetailResponse getBookingDetail(Long bookingId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Thiếu mã đặt phòng.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Đặt phòng này đã bị xóa.");
        }

        List<BookingDetail> details =
                bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);

        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        List<FolioItem> folioItems =
                folioItemRepository.findByBookingIdAndIsVoidedFalseOrderByPostedAtAsc(bookingId);
        List<ViewBookingDetailResponse.ServiceLine> serviceLines = folioItems.stream()
                .map(item -> {
                    FolioItemStatus serviceStatus = getEffectiveServiceStatus(item);
                    return ViewBookingDetailResponse.ServiceLine.builder()
                        .folioItemId(item.getId())
                        .bookingDetailId(item.getBookingDetail() != null
                                ? item.getBookingDetail().getId()
                                : null)
                        .roomNumber(item.getBookingDetail() != null
                                && item.getBookingDetail().getRoom() != null
                                ? item.getBookingDetail().getRoom().getRoomNumber()
                                : "Chưa xác định")
                        .serviceName(item.getDescription())
                        .itemType(item.getItemType() != null ? item.getItemType().name() : "N/A")
                        .serviceStatus(serviceStatus.name())
                        .serviceStatusLabel(serviceStatus.getLabel())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .amount(item.getAmount())
                        .postedAt(item.getPostedAt())
                        .postedBy(item.getPostedBy() != null
                                ? item.getPostedBy().getFirstName() + " " + item.getPostedBy().getLastName()
                                : "N/A")
                        .build();
                })
                .toList();

        BigDecimal roomTotal = booking.getRoomSubtotal() == null
                ? BigDecimal.ZERO
                : booking.getRoomSubtotal();

        BigDecimal vatTotal = booking.getVatTotal() == null
                ? BigDecimal.ZERO
                : booking.getVatTotal();

        BigDecimal grandTotal = booking.getGrandTotal() == null
                ? BigDecimal.ZERO
                : booking.getGrandTotal();

        BigDecimal depositRequired = booking.getDepositAmount() == null
                ? BigDecimal.ZERO
                : booking.getDepositAmount();

        BigDecimal serviceSubtotal = booking.getServiceSubtotal() == null
                ? BigDecimal.ZERO
                : booking.getServiceSubtotal();

        BigDecimal serviceChargeTotal = booking.getServiceChargeTotal() == null
                ? BigDecimal.ZERO
                : booking.getServiceChargeTotal();

        BigDecimal discountAmount = booking.getDiscountAmount() == null
                ? BigDecimal.ZERO
                : booking.getDiscountAmount();

        BigDecimal totalBeforeDiscount = roomTotal
                .add(serviceSubtotal)
                .add(serviceChargeTotal)
                .add(vatTotal);

        String promotionCode = booking.getPromotion() != null
                ? booking.getPromotion().getCode()
                : null;

        String promotionName = booking.getPromotion() != null
                ? booking.getPromotion().getName()
                : null;

        BigDecimal depositPaid = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .filter(payment -> payment.getPaymentType() != PaymentType.REFUND)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingEstimate = grandTotal.subtract(depositPaid);

        if (remainingEstimate.compareTo(BigDecimal.ZERO) < 0) {
            remainingEstimate = BigDecimal.ZERO;
        }

        User guest = booking.getGuest();

        String guestName = (booking.getGuestLastName() + " " + booking.getGuestFirstName()).trim();

        List<ViewBookingDetailResponse.RoomLine> roomLines = details.stream()
                .map(detail -> ViewBookingDetailResponse.RoomLine.builder()
                        .bookingDetailId(detail.getId())
                        .roomNumber(detail.getRoom() != null
                                ? detail.getRoom().getRoomNumber()
                                : "Chưa phân phòng")
                        .roomTypeName(
                                detail.getVariant() != null && detail.getVariant().getRoomType() != null
                                        ? detail.getVariant().getRoomType().getName()
                                        : null
                        )
                        .variantName(
                                detail.getVariant() != null
                                        ? detail.getVariant().getVariantName()
                                        : null
                        )
                        .viewType(
                                detail.getVariant() != null && detail.getVariant().getViewType() != null
                                        ? String.valueOf(detail.getVariant().getViewType())
                                        : null
                        )
                        .variantId(detail.getVariant() != null ? detail.getVariant().getId() : null)
                        .pricePerNight(detail.getPricePerNight())
                        .numNights(detail.getNumNights())
                        .subtotal(detail.getSubtotal())
                        .extraBedCount(detail.getExtraBedCount())
                        .extraBedTotal(detail.getExtraBedTotal())
                        .totalAmount(detail.getTotalAmount())
                        .roomCode(detail.getRoomCode())
                        .roomCodeExpiresAt(detail.getRoomCodeExpiresAt())
                        .checkOutDate(detail.getCheckOutDate())
                        .build())
                .toList();

        List<ViewBookingDetailResponse.PaymentLine> paymentLines = payments.stream()
                .map(payment -> ViewBookingDetailResponse.PaymentLine.builder()
                        .transactionRef(payment.getTransactionRef())
                        .paymentType(payment.getPaymentType() != null
                                ? payment.getPaymentType().getLabel()
                                : "N/A")
                        .method(payment.getMethod() != null
                                ? payment.getMethod().getLabel()
                                : "N/A")
                        .amount(payment.getAmount())
                        .status(payment.getStatus() != null
                                ? payment.getStatus().getLabel()
                                : "N/A")
                        .paidAt(payment.getPaidAt())
                        .build())
                .toList();

        return ViewBookingDetailResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .bookingStatus(booking.getStatus())
                .depositStatus(booking.getDepositStatus())
                .cancelReason(booking.getCancelReason())
                .cancelledAt(booking.getCancelledAt())
                .cancelledByName(
                        booking.getCancelledBy() != null
                                ? booking.getCancelledBy().getFirstName() + " " + booking.getCancelledBy().getLastName()
                                : "N/A"
                )

                .guestName(guestName)
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .gender(guest != null && guest.getGender() != null
                        ? guest.getGender().getLabel()
                        : "N/A")
                .birthYear(guest != null ? guest.getBirthYear() : null)
                .countryName(guest != null && guest.getCountry() != null ? guest.getCountry().getCountryName() : "N/A")
                .identityType(guest != null && guest.getIdentityType() != null
                        ? guest.getIdentityType().getLabel()
                        : "N/A")
                .identityNumber(guest != null ? guest.getIdentityNumber() : "N/A")
                .passportExpiryDate(guest != null ? guest.getPassportExpiryDate() : null)

                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .nights(ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()))
                .adults(booking.getNumAdults())
                .children(booking.getNumChildren())
                .specialRequests(booking.getSpecialRequests())
                .createdAt(booking.getCreatedAt())
                .promotionCode(promotionCode)
                .promotionName(promotionName)
                .discountAmount(discountAmount)
                .totalBeforeDiscount(totalBeforeDiscount)
                .roomTotal(roomTotal)
                .vatTotal(vatTotal)
                .grandTotal(grandTotal)
                .depositRequired(depositRequired)
                .depositPaid(depositPaid)
                .remainingEstimate(remainingEstimate)
                .serviceSubtotal(serviceSubtotal)
                .serviceChargeTotal(serviceChargeTotal)
                .services(serviceLines)
                .rooms(roomLines)
                .payments(paymentLines)
                .build();
    }

    private void validateDepositRequest(BookingCreateRequest request){
        if(request.getDepositMethod() == null){
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán tiền đặt cọc.");
        }
    }
    private void createDepositPayment(Booking booking,
                                      BigDecimal depositAmount,
                                      BookingCreateRequest request,
                                      User processBy){

        paymentService.createPayment(
                booking,
                PaymentType.DEPOSIT,
                request.getDepositMethod(),
                depositAmount,
                processBy
        );
    }

    private BigDecimal createAdditionalServiceFolioItems(Booking booking,
                                                         List<BookingDetail> bookingDetails,
                                                         BookingCreateRequest request,
                                                         User currentStaff) {
        if (request.getServiceIds() == null || request.getServiceIds().isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (request.getServiceRoomIds() == null
                || request.getServiceQuantities() == null
                || request.getServiceRoomIds().size() != request.getServiceIds().size()
                || request.getServiceQuantities().size() != request.getServiceIds().size()) {
            throw new IllegalArgumentException("Thông tin dịch vụ theo phòng không hợp lệ.");
        }

        List<com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service> services =
                serviceRepository.findAllById(request.getServiceIds());

        Map<Long, com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service> serviceMap =
                services.stream().collect(Collectors.toMap(
                        com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service::getId,
                        service -> service
                ));

        BigDecimal serviceSubtotal = BigDecimal.ZERO;
        List<FolioItem> folioItems = new ArrayList<>();

        for (int i = 0; i < request.getServiceIds().size(); i++) {
            Long roomId = request.getServiceRoomIds().get(i);
            Long serviceId = request.getServiceIds().get(i);
            Integer quantity = request.getServiceQuantities().get(i);

            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException("Số lượng dịch vụ phải ít nhất là 1.");
            }

            BookingDetail detail = bookingDetails.stream()
                    .filter(d -> d.getRoom() != null && d.getRoom().getId().equals(roomId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không khớp với phòng đã chọn."));

            com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service = serviceMap.get(serviceId);

            if (service == null
                    || Boolean.TRUE.equals(service.getIsDeleted())
                    || !Boolean.TRUE.equals(service.getIsAvailable())) {
                throw new IllegalArgumentException("Dịch vụ không tồn tại hoặc không còn khả dụng.");
            }

            BigDecimal unitPrice = service.getPrice();
            BigDecimal baseAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

            String newServiceText = service.getName() + " x" + quantity
                    + " - " + unitPrice.setScale(0, RoundingMode.HALF_UP) + " VND";

            if (detail.getServiceSummary() == null || detail.getServiceSummary().isBlank()) {
                detail.setServiceSummary(newServiceText);
            } else {
                detail.setServiceSummary(detail.getServiceSummary() + ", " + newServiceText);
            }

            BigDecimal serviceChargeAmount = baseAmount
                    .multiply(SERVICE_CHARGE_RATE)
                    .setScale(0, RoundingMode.HALF_UP);

            BigDecimal vatBase = TAX_ON_SERVICE_CHARGE
                    ? baseAmount.add(serviceChargeAmount)
                    : baseAmount;

            BigDecimal vatAmount = vatBase
                    .multiply(VAT_RATE)
                    .setScale(0, RoundingMode.HALF_UP);

            BigDecimal totalAmount = baseAmount
                    .add(serviceChargeAmount)
                    .add(vatAmount);

            FolioItem item = FolioItem.builder()
                    .booking(booking)
                    .bookingDetail(detail)
                    .service(service)
                    .itemType(resolveServiceFolioItemType(service))
                    .description(service.getName())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .baseAmount(baseAmount)
                    .serviceChargeRate(SERVICE_CHARGE_RATE)
                    .serviceChargeAmount(serviceChargeAmount)
                    .vatRate(VAT_RATE)
                    .vatAmount(vatAmount)
                    .totalAmount(totalAmount)
                    .amount(totalAmount)
                    .postedBy(currentStaff)
                    .postedAt(Instant.now())
                    .isVoided(false)
                    .build();

            folioItems.add(item);
            serviceSubtotal = serviceSubtotal.add(baseAmount);
        }

        folioItemRepository.saveAll(folioItems);
        bookingDetailRepository.saveAll(bookingDetails);
        return serviceSubtotal;
    }
    private FolioItemType resolveServiceFolioItemType(
            com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service
    ) {
        if (service == null || service.getCategory() == null || service.getCategory().getType() == null) {
            throw new IllegalArgumentException("Dịch vụ chưa có loại danh mục hợp lệ.");
        }

        ServiceCategoryType type = service.getCategory().getType();

        if (type == ServiceCategoryType.FOOD) {
            return FolioItemType.FOOD;
        }

        if (type == ServiceCategoryType.SPA) {
            return FolioItemType.SPA;
        }

        throw new IllegalArgumentException("Loại dịch vụ không hợp lệ.");
    }

    @Transactional(readOnly = true)
    public BookingUpdateRequest getBookingUpdateForm(Long bookingId){
        if ( bookingId == null){
            throw new IllegalArgumentException("Thiếu mã đặt phòng");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if(Boolean.TRUE.equals(booking.getIsDeleted())){
            throw new IllegalArgumentException("Đặt phòng này bị xóa");
        }

        User guest = booking.getGuest();

        return BookingUpdateRequest.builder()
                .bookingId(booking.getId())
                .firstName(booking.getGuestFirstName())
                .lastName(booking.getGuestLastName())
                .phoneNumber(booking.getGuestPhone())
                .email(booking.getGuestEmail())
                .gender(guest != null ? guest.getGender() : null)
                .birthYear(guest != null ? guest.getBirthYear() : null)
                .countryId(guest != null && guest.getCountry() != null
                        ? guest.getCountry().getId()
                        : null)
                .identityNumber(guest != null ? guest.getIdentityNumber() : null)
                .passportExpiryDate(guest != null ? guest.getPassportExpiryDate() : null)
                .notes(booking.getSpecialRequests())
                .build();
    }

    @Transactional
    public void updateBookingGuestInfo(Long bookingId, BookingUpdateRequest request) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Thiếu mã đặt phòng.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        validateUpdateBookingRequest(request, booking.getCheckOutDate());

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Đặt phòng này đã bị xóa.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.NO_SHOW
                || booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("Không thể chỉnh sửa booking đã hủy, no-show hoặc đã trả phòng.");
        }

        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quốc gia đã chọn."));

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        String phone = request.getPhoneNumber().trim();
        String email = request.getEmail().trim();
        String identityNumber = request.getIdentityNumber().trim();

        booking.setGuestFirstName(firstName);
        booking.setGuestLastName(lastName);
        booking.setGuestPhone(phone);
        booking.setGuestEmail(email);
        booking.setSpecialRequests(request.getNotes());

        User guest = booking.getGuest();

        if (guest == null) {
            guest = new User();
            booking.setGuest(guest);
        }

        guest.setFirstName(firstName);
        guest.setLastName(lastName);
        guest.setPhone(phone);
        guest.setEmail(email);
        guest.setGender(request.getGender());
        guest.setBirthYear(request.getBirthYear());
        guest.setCountry(country);
        IdentityType identityType = resolveIdentityType(country);

        guest.setIdentityNumber(identityNumber);
        guest.setIdentityType(identityType);
        guest.setPassportExpiryDate(identityType == IdentityType.PASSPORT
                ? request.getPassportExpiryDate()
                : null);

        userRepository.save(guest);
        bookingRepository.save(booking);
    }

    private void validateUpdateBookingRequest(BookingUpdateRequest request, LocalDate checkOutDate) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin chỉnh sửa không được để trống.");
        }

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên của khách.");
        }

        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập họ của khách.");
        }

        if (request.getFirstName().trim().length() > 50) {
            throw new IllegalArgumentException("Tên của khách không được vượt quá 50 ký tự.");
        }

        if (request.getLastName().trim().length() > 50) {
            throw new IllegalArgumentException("Họ của khách không được vượt quá 50 ký tự.");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại của khách.");
        }

        String phone = request.getPhoneNumber().trim();

        if (phone.length() > MAX_PHONE_LENGTH) {
            throw new IllegalArgumentException("Số điện thoại không được vượt quá 20 ký tự.");
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng.");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email của khách.");
        }

        String email = request.getEmail().trim();

        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Email không được vượt quá 150 ký tự.");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email không đúng định dạng.");
        }

        if (request.getCountryId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn quốc gia của khách.");
        }

        validatePassportExpiryDate(
                request.getCountryId(),
                request.getPassportExpiryDate(),
                checkOutDate
        );

        if (request.getGender() == null) {
            throw new IllegalArgumentException("Vui lòng chọn giới tính của khách.");
        }

        if (request.getIdentityNumber() == null || request.getIdentityNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số giấy tờ tùy thân.");
        }

        String identityNumber = request.getIdentityNumber().trim();

        if (identityNumber.length() > MAX_IDENTITY_LENGTH) {
            throw new IllegalArgumentException("Số giấy tờ tùy thân không được vượt quá 30 ký tự.");
        }

        if (!IDENTITY_PATTERN.matcher(identityNumber).matches()) {
            throw new IllegalArgumentException("Số giấy tờ tùy thân chỉ được chứa chữ, số hoặc dấu gạch ngang và phải từ 6 đến 30 ký tự.");
        }

        if (request.getBirthYear() != null) {
            int currentYear = LocalDate.now().getYear();

            if (request.getBirthYear() < 1900 || request.getBirthYear() > currentYear) {
                throw new IllegalArgumentException("Năm sinh không hợp lệ.");
            }
        }

        if (request.getNotes() != null && request.getNotes().length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự.");
        }
    }

    @Transactional
    public void cancelBooking(Long bookingId, String cancelReason) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Thiếu mã đặt phòng.");
        }

        if (cancelReason == null || cancelReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy đặt phòng.");
        }

        String reason = cancelReason.trim();

        if (reason.length() > MAX_CANCEL_REASON_LENGTH) {
            throw new IllegalArgumentException("Lý do hủy không được vượt quá 300 ký tự.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Đặt phòng này đã bị xóa.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Đặt phòng này đã bị hủy trước đó.");
        }

        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new IllegalArgumentException("Không thể hủy booking đã được đánh dấu no-show.");
        }

        if (booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("Không thể hủy đặt phòng đã nhận phòng hoặc đã trả phòng.");
        }

        User currentStaff = getCurrentStaffUser();

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelReason(reason);
        booking.setCancelledAt(Instant.now());
        booking.setCancelledBy(currentStaff);

        if (booking.getDepositStatus() == DepositStatus.PAID) {
            booking.setDepositStatus(DepositStatus.FORFEITED);
        }

        bookingRepository.save(booking);
    }

    @Transactional
    public void markNoShow(Long bookingId, String reason) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Thiếu mã đặt phòng.");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do no-show.");
        }

        String noShowReason = reason.trim();

        if (noShowReason.length() > MAX_CANCEL_REASON_LENGTH) {
            throw new IllegalArgumentException("Lý do no-show không được vượt quá 300 ký tự.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new IllegalArgumentException("Đặt phòng này đã bị xóa.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Chỉ đặt phòng đã xác nhận mới có thể đánh dấu no-show.");
        }

        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể đánh dấu no-show trước ngày nhận phòng.");
        }

        if (booking.getCheckInDate().isEqual(LocalDate.now())
                && LocalTime.now().isBefore(LocalTime.of(18, 0))) {
            throw new IllegalArgumentException("Chỉ có thể đánh dấu no-show sau 18:00 ngày nhận phòng.");
        }

        User currentStaff = getCurrentStaffUser();

        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setCancelReason(noShowReason);
        booking.setCancelledAt(Instant.now());
        booking.setCancelledBy(currentStaff);

        if (booking.getDepositStatus() == DepositStatus.PAID) {
            booking.setDepositStatus(DepositStatus.FORFEITED);
        }

        bookingRepository.save(booking);
    }

    @Transactional
    public void confirmOnlineBooking(Long bookingId,
                                     List<Long> bookingDetailIds,
                                     List<Long> roomIds) {
        Booking booking = bookingRepository.findByIdAndIsDeletedFalse(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể xác nhận đặt phòng đang chờ xác nhận.");
        }

        if (booking.getGuestEmail() == null || booking.getGuestEmail().isBlank()) {
            throw new IllegalStateException("Đặt phòng chưa có email khách, không thể gửi xác nhận.");
        }

        if (bookingDetailIds == null || roomIds == null || bookingDetailIds.size() != roomIds.size()) {
            throw new IllegalArgumentException("Vui lòng phân phòng đầy đủ.");
        }

        if (bookingDetailIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách chi tiết đặt phòng không được để trống.");
        }

        Set<Long> uniqueDetailIds = new HashSet<>(bookingDetailIds);

        if (uniqueDetailIds.size() != bookingDetailIds.size()) {
            throw new IllegalArgumentException("Không được gửi trùng chi tiết đặt phòng.");
        }

        Set<Long> uniqueRoomIds = new HashSet<>(roomIds);

        if (uniqueRoomIds.size() != roomIds.size()) {
            throw new IllegalArgumentException("Không được phân trùng một phòng cho nhiều chi tiết đặt phòng.");
        }

        List<Room> availableRooms = roomRepository.findAvailableRoomsByIds(
                roomIds,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                RoomStatus.AVAILABLE,
                List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );

        if (availableRooms.size() != roomIds.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều phòng đã được đặt trong khoảng thời gian này. Vui lòng chọn lại phòng.");
        }

        List<BookingDetail> details =
                bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);

        for (int i = 0; i < bookingDetailIds.size(); i++) {
            Long detailId = bookingDetailIds.get(i);
            Long roomId = roomIds.get(i);

            BookingDetail detail = details.stream()
                    .filter(d -> d.getId().equals(detailId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Chi tiết đặt phòng không hợp lệ."));

            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng đã chọn."));

            if (Boolean.TRUE.equals(room.getIsDeleted())) {
                throw new IllegalStateException("Phòng đã chọn đã bị xóa.");
            }

            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new IllegalStateException("Phòng " + room.getRoomNumber() + " không khả dụng.");
            }

            if (room.getVariant() == null || detail.getVariant() == null
                    || !room.getVariant().getId().equals(detail.getVariant().getId())) {
                throw new IllegalStateException(
                        "Phòng " + room.getRoomNumber() + " không đúng hạng phòng khách đã đặt."
                );
            }

            detail.setRoom(room);
        }

        Payment depositPayment = paymentRepository
                .findFirstByBookingIdAndPaymentTypeAndStatus(
                        bookingId,
                        PaymentType.DEPOSIT,
                        PaymentStatus.PENDING
                )
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy giao dịch cọc đang chờ xác nhận."));

        depositPayment.setStatus(PaymentStatus.SUCCESS);
        depositPayment.setPaidAt(Instant.now());
        depositPayment.setProcessedBy(getCurrentStaffUser());

        booking.setDepositStatus(DepositStatus.PAID);
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingDetailRepository.saveAll(details);
        paymentRepository.save(depositPayment);
        bookingRepository.save(booking);

        mailService.sendBookingConfirmedEmail(booking, details);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Room>> getAvailableRoomsForPendingBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdAndIsDeletedFalse(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            return Collections.emptyMap();
        }

        List<BookingDetail> details =
                bookingDetailRepository.findDetailsWithRoomsByBookingId(bookingId);

        return details.stream()
                .filter(detail -> detail.getVariant() != null)
                .collect(Collectors.toMap(
                        BookingDetail::getId,
                        detail -> roomRepository.findAvailableRoomsByVariantId(detail.getVariant().getId())
                ));
    }
    @Transactional(readOnly = true)
    public List<Booking> getTodayConfirmedBookingsForReminder() {
        return bookingRepository.findByStatusAndCheckInDateAndIsDeletedFalse(
                BookingStatus.CONFIRMED,
                LocalDate.now()
        );
    }

    private void validatePassportExpiryDate(Long countryId,
                                            LocalDate passportExpiryDate,
                                            LocalDate checkOutDate) {
        if (countryId == null) {
            return;
        }

        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quốc gia đã chọn."));

        IdentityType identityType = resolveIdentityType(country);

        if (identityType != IdentityType.PASSPORT) {
            return;
        }

        if (passportExpiryDate == null) {
            throw new IllegalArgumentException("Vui lòng nhập ngày hết hạn hộ chiếu.");
        }

        if (passportExpiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Hộ chiếu đã hết hạn.");
        }

        if (checkOutDate != null && passportExpiryDate.isBefore(checkOutDate)) {
            throw new IllegalArgumentException("Hộ chiếu phải còn hạn đến hết ngày trả phòng.");
        }
    }
}
