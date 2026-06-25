package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.BookingConfirmRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.SelectedRoomServiceRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingCompleteResult;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingConfirmView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.BookingSuccessView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionApplyResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.*;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class OnlineBookingService {

    private static final BigDecimal SERVICE_CHARGE_RATE = BigDecimal.valueOf(0.05);
    private static final BigDecimal VAT_RATE = BigDecimal.valueOf(0.08);

    private static final int MAX_FIRST_NAME_LENGTH = 50;
    private static final int MAX_LAST_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 150;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_SPECIAL_REQUEST_LENGTH = 500;
    private static final int MAX_BOOKING_NIGHTS = 30;
    private static final String BANK_BIN = "970418";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9()+\\s-]{8,20}$");


    private static final String BANK_NAME = "BIDV";

    private static final String BANK_ACCOUNT_NUMBER = "8830016176";
    private static final String BANK_ACCOUNT_NAME = "V'HOTEL HANOI";

    private final BookingSelectionService bookingSelectionService;
    private final PromotionService promotionService;
    private final MailService mailService;

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final ServiceRepository serviceRepository;
    private final PromotionRepository promotionRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public BookingConfirmView prepareConfirmView(BookingConfirmRequest request) {
        validateBasicBookingRequest(request);

        if (request.getBookingReference() == null || request.getBookingReference().isBlank()) {
            request.setBookingReference(generateBookingReference());
        }

        List<Long> variantIds = bookingSelectionService.parseVariantIds(request.getVariantIds());

        List<GuestRoomVariantProjection> selectedRooms =
                bookingSelectionService.getSelectRoomsForService(
                        request.getVariantIds(),
                        request.getCheckInDate(),
                        request.getCheckOutDate()
                );

        long nights = ChronoUnit.DAYS.between(
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        Map<Integer, RoomGuestInfo> guestInfoByRoom =
                parseRoomGuests(
                        request.getRoomGuests(),
                        selectedRooms.size(),
                        request.getAdults(),
                        request.getChildren()
                );

        Map<Integer, List<BookingConfirmView.ServiceLine>> addOnServicesByRoom =
                buildAddOnServicesByRoom(
                        request.getRoomServices(),
                        selectedRooms.size()
                );

        List<BookingConfirmView.RoomLine> roomLines = new ArrayList<>();

        BigDecimal roomSubtotal = BigDecimal.ZERO;
        BigDecimal serviceSubtotal = BigDecimal.ZERO;

        for (int i = 0; i < selectedRooms.size(); i++) {
            GuestRoomVariantProjection room = selectedRooms.get(i);
            int roomIndex = i + 1;

            BigDecimal pricePerNight = safeMoney(room.getPricePerNight());

            BigDecimal roomLineSubtotal = pricePerNight.multiply(BigDecimal.valueOf(nights));
            roomLineSubtotal = money(roomLineSubtotal);

            List<BookingConfirmView.ServiceLine> addOnServices =
                    addOnServicesByRoom.getOrDefault(roomIndex, new ArrayList<>());

            BigDecimal addOnSubtotal = BigDecimal.ZERO;

            for (BookingConfirmView.ServiceLine serviceLine : addOnServices) {
                if (serviceLine.getLineTotal() != null) {
                    addOnSubtotal = addOnSubtotal.add(serviceLine.getLineTotal());
                }
            }

            String addOnSummary = buildServiceSummary(addOnServices);

            RoomGuestInfo guestInfo = guestInfoByRoom.getOrDefault(
                    roomIndex,
                    new RoomGuestInfo(1, 0)
            );

            BookingConfirmView.RoomLine roomLine = new BookingConfirmView.RoomLine();

            roomLine.setRoomIndex(roomIndex);
            roomLine.setVariantId(room.getVariantId());
            roomLine.setRoomTypeName(room.getRoomTypeName());
            roomLine.setVariantName(room.getVariantName());
            roomLine.setViewType(room.getViewType());
            roomLine.setAdults(guestInfo.adults());
            roomLine.setChildren(guestInfo.children());
            roomLine.setImageUrl(room.getPrimaryImageUrl());
            roomLine.setPricePerNight(pricePerNight);
            roomLine.setRoomSubtotal(roomLineSubtotal);
            roomLine.setIncludedServiceSummary(room.getServiceSummary());
            roomLine.setAddOnServices(addOnServices);
            roomLine.setAddOnServiceSubtotal(addOnSubtotal);
            roomLine.setAddOnServiceSummary(addOnSummary);

            roomLines.add(roomLine);

            roomSubtotal = roomSubtotal.add(roomLineSubtotal);
            serviceSubtotal = serviceSubtotal.add(addOnSubtotal);
        }

        BigDecimal subtotalBeforeFees = roomSubtotal.add(serviceSubtotal);
        BigDecimal serviceChargeTotal = money(subtotalBeforeFees.multiply(SERVICE_CHARGE_RATE));
        BigDecimal vatTotal = money(subtotalBeforeFees.add(serviceChargeTotal).multiply(VAT_RATE));
        BigDecimal totalBeforeDiscount = subtotalBeforeFees.add(serviceChargeTotal).add(vatTotal);

        PromotionApplyResponse promotionResult =
                promotionService.applyPromotionCode(
                        request.getPromoCode(),
                        totalBeforeDiscount
                );

        BigDecimal discountAmount = BigDecimal.ZERO;

        if (promotionResult.isValid()) {
            discountAmount = safeMoney(promotionResult.getDiscountAmount());
        }

        if (discountAmount.compareTo(totalBeforeDiscount) > 0) {
            discountAmount = totalBeforeDiscount;
        }

        BigDecimal grandTotal = money(totalBeforeDiscount.subtract(discountAmount));

        BookingConfirmView.PriceSummary priceSummary = new BookingConfirmView.PriceSummary();

        priceSummary.setRoomSubtotal(roomSubtotal);
        priceSummary.setServiceSubtotal(serviceSubtotal);
        priceSummary.setSubtotalBeforeFees(subtotalBeforeFees);
        priceSummary.setServiceChargeTotal(serviceChargeTotal);
        priceSummary.setVatTotal(vatTotal);
        priceSummary.setDiscountAmount(discountAmount);
        priceSummary.setGrandTotal(grandTotal);

        BookingConfirmView.BankTransferInfo bankTransferInfo = new BookingConfirmView.BankTransferInfo();

        bankTransferInfo.setBankName(BANK_NAME);
        bankTransferInfo.setAccountNumber(BANK_ACCOUNT_NUMBER);
        bankTransferInfo.setAccountName(BANK_ACCOUNT_NAME);
        bankTransferInfo.setTransferContent(request.getBookingReference());
        bankTransferInfo.setAmount(grandTotal);
        String qrImageUrl = buildVietQrUrl(
                BANK_BIN,
                BANK_ACCOUNT_NUMBER,
                grandTotal,
                request.getBookingReference(),
                BANK_ACCOUNT_NAME
        );
        bankTransferInfo.setQrImageUrl(qrImageUrl);

        BookingConfirmView confirmView = new BookingConfirmView();

        confirmView.setBookingReference(request.getBookingReference());
        confirmView.setCheckInDate(request.getCheckInDate());
        confirmView.setCheckOutDate(request.getCheckOutDate());
        confirmView.setNights(nights);
        confirmView.setAdults(request.getAdults());
        confirmView.setChildren(request.getChildren());
        confirmView.setRoomCount(selectedRooms.size());
        confirmView.setPromoCode(request.getPromoCode());
        confirmView.setRooms(roomLines);
        confirmView.setPriceSummary(priceSummary);
        confirmView.setBankTransferInfo(bankTransferInfo);

        return confirmView;
    }

    @Transactional
    public BookingCompleteResult completeOnlineBooking(BookingConfirmRequest request) {
        validateGuestInformation(request);

        if (!Boolean.TRUE.equals(request.getPaymentAcknowledged())) {
            throw new IllegalArgumentException("Vui lòng xác nhận rằng bạn đã thực hiện chuyển khoản.");
        }

        BookingConfirmView confirmView = prepareConfirmView(request);

        String bookingReference = confirmView.getBookingReference();

        if (bookingRepository.existsByBookingReference(bookingReference)) {
            bookingReference = generateBookingReference();
            request.setBookingReference(bookingReference);
            confirmView = prepareConfirmView(request);
        }

        List<Long> variantIds = bookingSelectionService.parseVariantIds(request.getVariantIds());

        List<RoomTypeVariant> variants = roomTypeVariantRepository.findAllById(variantIds);

        Map<Long, RoomTypeVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(RoomTypeVariant::getId, variant -> variant));

        Promotion promotion = null;

        BigDecimal totalBeforeDiscount =
                confirmView.getPriceSummary().getSubtotalBeforeFees()
                        .add(confirmView.getPriceSummary().getServiceChargeTotal())
                        .add(confirmView.getPriceSummary().getVatTotal());

        PromotionApplyResponse promotionResult =
                promotionService.applyPromotionCode(
                        request.getPromoCode(),
                        totalBeforeDiscount
                );

        if (promotionResult.isValid() && promotionResult.getPromotionId() != null) {
            promotion = promotionRepository.findById(promotionResult.getPromotionId()).orElse(null);

            if (promotion != null) {
                Integer usageCount = promotion.getUsageCount();

                if (usageCount == null) {
                    usageCount = 0;
                }

                promotion.setUsageCount(usageCount + 1);
            }
        }

        BigDecimal grandTotal = confirmView.getPriceSummary().getGrandTotal();

        Booking booking = new Booking();

        booking.setGuestFirstName(request.getGuestFirstName().trim());
        booking.setGuestLastName(request.getGuestLastName().trim());
        booking.setGuestPhone(request.getGuestPhone().trim());
        booking.setGuestEmail(request.getGuestEmail().trim());

        booking.setGuest(null);
        booking.setPromotion(promotion);
        booking.setDiscountAmount(confirmView.getPriceSummary().getDiscountAmount());
      fdcsdsds
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());

        booking.setNumAdults(request.getAdults());
        booking.setNumChildren(request.getChildren());
        booking.setTotalRooms(confirmView.getRooms().size());

        booking.setSpecialRequests(normalizeBlankToNull(request.getSpecialRequests()));
        booking.setBookingReference(bookingReference);

        booking.setDepositStatus(DepositStatus.UNPAID);

        /*
         * Online QR flow: yêu cầu khách chuyển tổng tiền.
         * Nếu sau này muốn chỉ cọc 50%, đổi thành grandTotal.multiply(BigDecimal.valueOf(0.5)).
         */
        booking.setDepositAmount(grandTotal);

        booking.setStatus(BookingStatus.PENDING);

        booking.setRoomSubtotal(confirmView.getPriceSummary().getRoomSubtotal());
        booking.setServiceSubtotal(confirmView.getPriceSummary().getServiceSubtotal());
        booking.setServiceChargeTotal(confirmView.getPriceSummary().getServiceChargeTotal());
        booking.setVatTotal(confirmView.getPriceSummary().getVatTotal());
        booking.setTotalAmount(grandTotal);
        booking.setGrandTotal(grandTotal);

        booking.setAmountCalculatedAt(Instant.now());
        booking.setIsDeleted(false);

        Booking savedBooking = bookingRepository.save(booking);

        Payment payment = new Payment();

        payment.setBooking(savedBooking);
        payment.setAmount(savedBooking.getGrandTotal());
        payment.setMethod(PaymentMethod.TRANSFER);
        payment.setPaymentType(PaymentType.DEPOSIT);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef(savedBooking.getBookingReference());
        payment.setCreatedAt(Instant.now());
        paymentRepository.save(payment);

        List<BookingDetail> details = new ArrayList<>();

        for (BookingConfirmView.RoomLine roomLine : confirmView.getRooms()) {
            RoomTypeVariant variant = variantMap.get(roomLine.getVariantId());

            if (variant == null) {
                throw new IllegalArgumentException("Không tìm thấy hạng phòng đã chọn.");
            }

            BigDecimal addOnServiceSubtotal = roomLine.getAddOnServiceSubtotal();

            if (addOnServiceSubtotal == null) {
                addOnServiceSubtotal = BigDecimal.ZERO;
            }

            BigDecimal detailBaseAmount = roomLine.getRoomSubtotal().add(addOnServiceSubtotal);

            BigDecimal detailServiceCharge = money(detailBaseAmount.multiply(SERVICE_CHARGE_RATE));
            BigDecimal detailVat = money(detailBaseAmount.add(detailServiceCharge).multiply(VAT_RATE));
            BigDecimal detailTotal = money(detailBaseAmount.add(detailServiceCharge).add(detailVat));

            BookingDetail detail = new BookingDetail();

            detail.setBooking(savedBooking);
            detail.setVariant(variant);
            detail.setRoom(null);

            detail.setCheckInDate(request.getCheckInDate());
            detail.setCheckOutDate(request.getCheckOutDate());

            detail.setPricePerNight(roomLine.getPricePerNight());
            detail.setNumNights(Math.toIntExact(confirmView.getNights()));

            detail.setNumAdults(roomLine.getAdults());
            detail.setNumChildren(roomLine.getChildren());

            detail.setSubtotal(roomLine.getRoomSubtotal());

            detail.setServiceChargeRate(SERVICE_CHARGE_RATE);
            detail.setServiceChargeAmount(detailServiceCharge);

            detail.setVatRate(VAT_RATE);
            detail.setVatAmount(detailVat);

            detail.setTotalAmount(detailTotal);

            detail.setServiceSummary(roomLine.getAddOnServiceSummary());

            detail.setExtraBedCount(0);
            detail.setExtraBedPrice(BigDecimal.ZERO);
            detail.setExtraBedTotal(BigDecimal.ZERO);

            details.add(detail);
        }

        bookingDetailRepository.saveAll(details);

        String guestName = savedBooking.getGuestFirstName() + " " + savedBooking.getGuestLastName();
        guestName = guestName.trim();

        mailService.sendBookingPendingPaymentEmail(
                savedBooking.getGuestEmail(),
                guestName,
                savedBooking.getBookingReference(),
                String.valueOf(savedBooking.getCheckInDate()),
                String.valueOf(savedBooking.getCheckOutDate()),
                savedBooking.getTotalRooms(),
                formatMoney(savedBooking.getGrandTotal()),
                BANK_NAME,
                BANK_ACCOUNT_NUMBER,
                BANK_ACCOUNT_NAME,
                savedBooking.getBookingReference()
        );

        BookingCompleteResult result = new BookingCompleteResult();

        result.setBookingId(savedBooking.getId());
        result.setBookingReference(savedBooking.getBookingReference());

        return result;    }

    public String generateBookingReference() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String reference;

        do {
            int randomNumber = (int) (Math.random() * 9000) + 1000;
            reference = "BK-" + datePart + randomNumber;
        } while (bookingRepository.existsByBookingReference(reference));

        return reference;
    }

    private void validateBasicBookingRequest(BookingConfirmRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin đặt phòng không được để trống.");
        }

        if (request.getVariantIds() == null || request.getVariantIds().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn phòng trước khi xác nhận đặt phòng.");
        }

        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        if (request.getAdults() == null || request.getAdults() < 1) {
            request.setAdults(1);
        }

        if (request.getChildren() == null || request.getChildren() < 0) {
            request.setChildren(0);
        }

        if (request.getRoomCount() == null || request.getRoomCount() < 1) {
            request.setRoomCount(1);
        }
    }

    public BookingSuccessView getBookingSuccessView(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking."));

        BigDecimal amount = booking.getGrandTotal();

        if (amount == null) {
            amount = booking.getTotalAmount();
        }

        String transferContent = booking.getBookingReference();

        String qrImageUrl = buildVietQrUrl(
                BANK_BIN,
                BANK_ACCOUNT_NUMBER,
                amount,
                transferContent,
                BANK_ACCOUNT_NAME
        );

        BookingSuccessView view = new BookingSuccessView();

        view.setBookingReference(booking.getBookingReference());
        view.setCheckInDate(booking.getCheckInDate());
        view.setCheckOutDate(booking.getCheckOutDate());
        view.setTotalRooms(booking.getTotalRooms());

        view.setAmount(amount);

        view.setBankName(BANK_NAME);
        view.setAccountNumber(BANK_ACCOUNT_NUMBER);
        view.setAccountName(BANK_ACCOUNT_NAME);
        view.setTransferContent(transferContent);
        view.setQrImageUrl(qrImageUrl);

        return view;
    }

    private void validateGuestInformation(BookingConfirmRequest request) {
        validateBasicBookingRequest(request);

        if (request.getGuestFirstName() == null || request.getGuestFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên của khách.");
        }

        if (request.getGuestLastName() == null || request.getGuestLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập họ của khách.");
        }

        if (request.getGuestFirstName().trim().length() > MAX_FIRST_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên của khách không được vượt quá 50 ký tự.");
        }

        if (request.getGuestLastName().trim().length() > MAX_LAST_NAME_LENGTH) {
            throw new IllegalArgumentException("Họ của khách không được vượt quá 50 ký tự.");
        }

        if (request.getGuestPhone() == null || request.getGuestPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại của khách.");
        }

        String phone = request.getGuestPhone().trim();

        if (phone.length() > MAX_PHONE_LENGTH) {
            throw new IllegalArgumentException("Số điện thoại không được vượt quá 20 ký tự.");
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng.");
        }

        if (request.getGuestEmail() == null || request.getGuestEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập email của khách.");
        }

        String email = request.getGuestEmail().trim();

        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Email không được vượt quá 150 ký tự.");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email không đúng định dạng.");
        }

        if (request.getSpecialRequests() != null
                && request.getSpecialRequests().length() > MAX_SPECIAL_REQUEST_LENGTH) {
            throw new IllegalArgumentException("Yêu cầu đặc biệt không được vượt quá 500 ký tự.");
        }
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

    private Map<Integer, List<BookingConfirmView.ServiceLine>> buildAddOnServicesByRoom(
            List<SelectedRoomServiceRequest> roomServices,
            int roomCount
    ) {
        Map<Integer, List<BookingConfirmView.ServiceLine>> result = new LinkedHashMap<>();

        for (int i = 1; i <= roomCount; i++) {
            result.put(i, new ArrayList<>());
        }

        if (roomServices == null || roomServices.isEmpty()) {
            return result;
        }

        Map<String, Integer> aggregatedQuantity = new LinkedHashMap<>();

        for (SelectedRoomServiceRequest item : roomServices) {
            if (item == null) {
                continue;
            }

            Integer roomIndex = item.getRoomIndex();
            Long serviceId = item.getServiceId();
            Integer quantity = item.getQuantity();

            if (roomIndex == null || roomIndex < 1 || roomIndex > roomCount) {
                throw new IllegalArgumentException("Dịch vụ được chọn không khớp với phòng.");
            }

            if (serviceId == null) {
                continue;
            }

            if (quantity == null || quantity <= 0) {
                continue;
            }

            String key = roomIndex + "-" + serviceId;

            Integer currentQuantity = aggregatedQuantity.get(key);

            if (currentQuantity == null) {
                currentQuantity = 0;
            }

            aggregatedQuantity.put(key, currentQuantity + quantity);
        }

        if (aggregatedQuantity.isEmpty()) {
            return result;
        }

        List<Long> serviceIds = aggregatedQuantity.keySet()
                .stream()
                .map(key -> Long.parseLong(key.split("-")[1]))
                .distinct()
                .toList();

        List<com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service> services =
                serviceRepository.findAllById(serviceIds);

        Map<Long, com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service> serviceMap =
                services.stream()
                        .filter(service -> !Boolean.TRUE.equals(service.getIsDeleted()))
                        .filter(service -> Boolean.TRUE.equals(service.getIsAvailable()))
                        .collect(Collectors.toMap(
                                com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service::getId,
                                service -> service
                        ));

        for (Map.Entry<String, Integer> entry : aggregatedQuantity.entrySet()) {
            String[] parts = entry.getKey().split("-");

            int roomIndex = Integer.parseInt(parts[0]);
            Long serviceId = Long.parseLong(parts[1]);
            Integer quantity = entry.getValue();

            com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Service service =
                    serviceMap.get(serviceId);

            if (service == null) {
                throw new IllegalArgumentException("Có dịch vụ không tồn tại hoặc không còn khả dụng.");
            }

            BigDecimal unitPrice = safeMoney(service.getPrice());
            BigDecimal lineTotal = money(unitPrice.multiply(BigDecimal.valueOf(quantity)));

            BookingConfirmView.ServiceLine line = new BookingConfirmView.ServiceLine();

            line.setServiceId(service.getId());
            line.setServiceName(service.getName());
            line.setQuantity(quantity);
            line.setUnitPrice(unitPrice);
            line.setLineTotal(lineTotal);

            result.get(roomIndex).add(line);
        }

        return result;
    }

    private Map<Integer, RoomGuestInfo> parseRoomGuests(
            String roomGuests,
            int roomCount,
            Integer totalAdults,
            Integer totalChildren
    ) {
        Map<Integer, RoomGuestInfo> result = new LinkedHashMap<>();

        if (roomGuests != null && !roomGuests.trim().isEmpty()) {
            String[] rooms = roomGuests.split("\\|");

            for (int i = 0; i < rooms.length && i < roomCount; i++) {
                String[] parts = rooms[i].split("-");

                int adults = 1;
                int children = 0;

                if (parts.length > 0) {
                    adults = parseIntOrDefault(parts[0], 1);
                }

                if (parts.length > 1) {
                    children = parseIntOrDefault(parts[1], 0);
                }

                if (adults < 1) {
                    adults = 1;
                }

                if (children < 0) {
                    children = 0;
                }

                result.put(i + 1, new RoomGuestInfo(adults, children));
            }
        }

        if (result.size() < roomCount) {
            for (int i = 1; i <= roomCount; i++) {
                if (!result.containsKey(i)) {
                    if (roomCount == 1) {
                        Integer adults = totalAdults;
                        Integer children = totalChildren;

                        if (adults == null || adults < 1) {
                            adults = 1;
                        }

                        if (children == null || children < 0) {
                            children = 0;
                        }

                        result.put(i, new RoomGuestInfo(adults, children));
                    } else {
                        result.put(i, new RoomGuestInfo(1, 0));
                    }
                }
            }
        }

        return result;
    }

    private String buildServiceSummary(List<BookingConfirmView.ServiceLine> services) {
        if (services == null || services.isEmpty()) {
            return null;
        }

        List<String> summaryLines = new ArrayList<>();

        for (BookingConfirmView.ServiceLine service : services) {
            String line =
                    service.getServiceName()
                            + " x" + service.getQuantity()
                            + " - " + formatMoney(service.getLineTotal());

            summaryLines.add(line);
        }

        return String.join("\n", summaryLines);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal safeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return money(value);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safeValue = value;

        if (safeValue == null) {
            safeValue = BigDecimal.ZERO;
        }

        return String.format("%,.0f VND", safeValue);
    }

    private record RoomGuestInfo(Integer adults, Integer children) {
    }

    private String buildVietQrUrl(
            String bankBin,
            String accountNumber,
            BigDecimal amount,
            String transferContent,
            String accountName
    ) {
        String safeAmount = "0";

        if (amount != null) {
            safeAmount = amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }

        String encodedContent = URLEncoder.encode(
                transferContent == null ? "" : transferContent,
                StandardCharsets.UTF_8
        );

        String encodedAccountName = URLEncoder.encode(
                accountName == null ? "" : accountName,
                StandardCharsets.UTF_8
        );

        return "https://img.vietqr.io/image/"
                + bankBin + "-" + accountNumber + "-compact2.png"
                + "?amount=" + safeAmount
                + "&addInfo=" + encodedContent
                + "&accountName=" + encodedAccountName;
    }
}