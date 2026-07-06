package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.BookingServiceProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class BookingSelectionService {

    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 24;
    private static final int MAX_SELECTED_VARIANTS = 20;

    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final ServiceRepository serviceRepository;

    public List<Long> parseVariantIds(String variantIds) {
        if (variantIds == null || variantIds.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn phòng trước khi chọn dịch vụ");
        }

        List<Long> results = new ArrayList<>();
        String[] idArray = variantIds.split(",");

        for (String idText : idArray) {
            if (idText == null || idText.trim().isEmpty()) {
                continue;
            }

            Long id;

            try {
                id = Long.parseLong(idText.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Danh sách phòng đã chọn không hợp lệ");
            }

            if (id <= 0) {
                throw new IllegalArgumentException("Mã phòng đã chọn không hợp lệ");
            }

            results.add(id);
        }

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn phòng trước khi chọn dịch vụ");
        }

        if (results.size() > MAX_SELECTED_VARIANTS) {
            throw new IllegalArgumentException("Số lượng phòng được chọn vượt quá giới hạn cho phép");
        }

        return results;
    }

    public List<GuestRoomVariantProjection> getSelectRoomsForService(
            String variantIds,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        validateStayDates(checkInDate, checkOutDate);

        List<Long> ids = parseVariantIds(variantIds);

        List<GuestRoomVariantProjection> rooms =
                roomTypeVariantRepository.findSelectedRoomVariantsByIds(
                        ids,
                        checkInDate,
                        checkOutDate
                );

        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy phòng đã chọn");
        }

        Map<Long, GuestRoomVariantProjection> roomMap = new LinkedHashMap<>();

        for (GuestRoomVariantProjection room : rooms) {
            if (room == null || room.getVariantId() == null) {
                continue;
            }

            roomMap.putIfAbsent(room.getVariantId(), room);
        }

        List<GuestRoomVariantProjection> selectedRooms = new ArrayList<>();

        for (Long id : ids) {
            GuestRoomVariantProjection room = roomMap.get(id);

            if (room == null) {
                throw new IllegalArgumentException("Có phòng đã chọn không tồn tại hoặc không còn khả dụng");
            }

            selectedRooms.add(room);
        }

        return selectedRooms;
    }

    public Page<BookingServiceProjection> getBookingService(
            String category,
            String sort,
            String priceFilter,
            int page,
            int size
    ) {
        String finalCategory = normalizeCategory(category);
        String finalSort = normalizeSort(sort);
        String finalPriceFilter = normalizePriceFilter(priceFilter);

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }

        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size);

        return serviceRepository.findBookingServices(
                finalCategory,
                finalSort,
                finalPriceFilter,
                pageable
        );
    }

    public BigDecimal calculateRoomSubtotal(
            List<GuestRoomVariantProjection> selectedRooms,
            long nights
    ) {
        if (selectedRooms == null || selectedRooms.isEmpty()) {
            throw new IllegalArgumentException("Không có phòng nào được chọn để tính tiền");
        }

        if (nights <= 0) {
            throw new IllegalArgumentException("Số đêm lưu trú không hợp lệ");
        }

        BigDecimal total = BigDecimal.ZERO;

        for (GuestRoomVariantProjection room : selectedRooms) {
            if (room == null) {
                throw new IllegalArgumentException("Thông tin phòng không hợp lệ");
            }

            BigDecimal pricePerNight = room.getPricePerNight();

            if (pricePerNight == null || pricePerNight.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Giá phòng không hợp lệ");
            }

            BigDecimal roomTotal = pricePerNight.multiply(BigDecimal.valueOf(nights));
            total = total.add(roomTotal);
        }

        return total;
    }

    public long calculateNights(LocalDate checkInDate, LocalDate checkOutDate) {
        validateStayDates(checkInDate, checkOutDate);
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    private void validateStayDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày nhận phòng và ngày trả phòng");
        }

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày nhận phòng không được nhỏ hơn ngày hiện tại");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng");
        }
    }

    public String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "ALL";
        }

        String value = category.trim().toUpperCase();

        if (value.equals("DINING")) {
            return "DINING";
        }

        if (value.equals("WELLNESS")) {
            return "WELLNESS";
        }

        if (value.equals("ALL")) {
            return "ALL";
        }

        return "ALL";
    }

    public String normalizeSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "recommended";
        }

        String value = sort.trim();

        if (value.equalsIgnoreCase("priceAsc") || value.equalsIgnoreCase("price_asc")) {
            return "priceAsc";
        }

        if (value.equalsIgnoreCase("priceDesc") || value.equalsIgnoreCase("price_desc")) {
            return "priceDesc";
        }

        if (value.equalsIgnoreCase("recommended")) {
            return "recommended";
        }

        return "recommended";
    }

    public String normalizePriceFilter(String priceFilter) {
        if (priceFilter == null || priceFilter.trim().isEmpty()) {
            return "ALL";
        }

        String value = priceFilter.trim()
                .toUpperCase()
                .replace("-", "_");

        if (value.equals("UNDER_200")) {
            return "UNDER_200";
        }

        if (value.equals("FROM_200_TO_500")) {
            return "FROM_200_TO_500";
        }

        if (value.equals("OVER_500")) {
            return "OVER_500";
        }

        if (value.equals("ALL")) {
            return "ALL";
        }

        return "ALL";
    }
}