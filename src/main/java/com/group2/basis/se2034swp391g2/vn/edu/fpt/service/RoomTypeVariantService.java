package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.RoomVariantDetailProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomTypeVariantService {

    private static final String DEFAULT_SORT = "recommended";

    private static final Set<String> ALLOWED_SORTS = Set.of(
            "recommended",
            "priceAsc",
            "priceDesc"
    );

    private static final Set<String> ALLOWED_VIEW_TYPES = Set.of(
            "CITY",
            "SEA",
            "POOL",
            "GARDEN"
    );

    private static final int MIN_ROOM_COUNT = 1;
    private static final int MAX_ONLINE_ROOM_COUNT = 9;

    private static final int MIN_ADULT_PER_ROOM = 1;
    private static final int MAX_ADULT_PER_ROOM = 6;

    private static final int MIN_CHILD_PER_ROOM = 0;
    private static final int MAX_CHILD_PER_ROOM = 4;

    private static final int MAX_TOTAL_ADULTS = MAX_ONLINE_ROOM_COUNT * MAX_ADULT_PER_ROOM;
    private static final int MAX_TOTAL_CHILDREN = MAX_ONLINE_ROOM_COUNT * MAX_CHILD_PER_ROOM;

    private static final long MAX_STAY_NIGHTS = 30;

    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final RoomTypeRepository roomTypeRepository;

    public List<RoomType> getRoomTypes() {
        return roomTypeRepository.findByIsDeletedFalse();
    }

    public List<GuestRoomVariantProjection> getGuestRoomVariants(
            Long roomTypeId,
            String viewType,
            String sort,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer adults,
            Integer children,
            Integer roomCount
    ) {
        return getGuestRoomVariants(
                roomTypeId,
                viewType,
                sort,
                checkInDate,
                checkOutDate,
                adults,
                children,
                roomCount,
                null
        );
    }


    public List<GuestRoomVariantProjection> getGuestRoomVariants(
            Long roomTypeId,
            String viewType,
            String sort,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer adults,
            Integer children,
            Integer roomCount,
            String roomGuests
    ) {
        RoomSearchCriteria criteria = normalizeSearchCriteria(
                roomTypeId,
                viewType,
                sort,
                checkInDate,
                checkOutDate,
                adults,
                children,
                roomCount,
                roomGuests
        );

        return getGuestRoomVariants(criteria);
    }

    public List<GuestRoomVariantProjection> getGuestRoomVariants(RoomSearchCriteria criteria) {
        if (criteria == null) {
            criteria = normalizeSearchCriteria(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return roomTypeVariantRepository.findGuestRoomVariants(
                criteria.getRoomTypeId(),
                criteria.getViewType(),
                criteria.getSort(),
                criteria.getCheckInDate(),
                criteria.getCheckOutDate(),
                criteria.getAdults(),
                criteria.getChildren(),
                criteria.getRoomCount()
        );
    }

    public RoomSearchCriteria normalizeSearchCriteria(
            Long roomTypeId,
            String viewType,
            String sort,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer adults,
            Integer children,
            Integer roomCount,
            String roomGuests
    ) {
        List<String> warningMessages = new ArrayList<>();

        Long safeRoomTypeId = normalizeRoomTypeId(roomTypeId);
        String safeViewType = normalizeViewType(viewType);
        String safeSort = normalizeSort(sort);

        StayDateResult stayDateResult = normalizeStayDate(checkInDate, checkOutDate);
        if (stayDateResult.getWarningMessage() != null) {
            warningMessages.add(stayDateResult.getWarningMessage());
        }

        GuestResult guestResult = normalizeGuestInfo(adults, children, roomCount, roomGuests);
        if (guestResult.getWarningMessage() != null) {
            warningMessages.add(guestResult.getWarningMessage());
        }

        String warningMessage = warningMessages.isEmpty()
                ? null
                : String.join(" ", warningMessages);

        return new RoomSearchCriteria(
                safeRoomTypeId,
                safeViewType,
                safeSort,
                stayDateResult.getCheckInDate(),
                stayDateResult.getCheckOutDate(),
                guestResult.getAdults(),
                guestResult.getChildren(),
                guestResult.getRoomCount(),
                guestResult.getRoomGuests(),
                warningMessage
        );
    }

    public RoomVariantDetailProjection getRoomVariantDetail(
            Long variantId,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (variantId == null || variantId <= 0) {
            throw new IllegalArgumentException("Hạng phòng không hợp lệ.");
        }

        StayDateResult stayDateResult = normalizeStayDate(checkInDate, checkOutDate);

        return roomTypeVariantRepository
                .findRoomVariantDetailById(
                        variantId,
                        stayDateResult.getCheckInDate(),
                        stayDateResult.getCheckOutDate()
                )
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng phòng."));
    }

    private Long normalizeRoomTypeId(Long roomTypeId) {
        if (roomTypeId == null || roomTypeId <= 0) {
            return null;
        }

        boolean exists = roomTypeRepository.findByIsDeletedFalse()
                .stream()
                .anyMatch(roomType -> Objects.equals(roomType.getId(), roomTypeId));

        return exists ? roomTypeId : null;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_SORT;
        }

        String normalizedSort = sort.trim();

        if (!ALLOWED_SORTS.contains(normalizedSort)) {
            return DEFAULT_SORT;
        }

        return normalizedSort;
    }

    private String normalizeViewType(String viewType) {
        if (viewType == null || viewType.isBlank()) {
            return null;
        }

        String normalizedViewType = viewType.trim().toUpperCase();

        if (!ALLOWED_VIEW_TYPES.contains(normalizedViewType)) {
            return null;
        }

        return normalizedViewType;
    }

    private StayDateResult normalizeStayDate(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null && checkOutDate == null) {
            return new StayDateResult(null, null, null);
        }

        if (checkInDate == null || checkOutDate == null) {
            return new StayDateResult(
                    null,
                    null,
                    "Vui lòng chọn đầy đủ ngày nhận phòng và ngày trả phòng."
            );
        }

        LocalDate today = LocalDate.now();

        if (checkInDate.isBefore(today)) {
            return new StayDateResult(
                    null,
                    null,
                    "Ngày nhận phòng không được ở quá khứ."
            );
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            return new StayDateResult(
                    null,
                    null,
                    "Ngày trả phòng phải sau ngày nhận phòng."
            );
        }

        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        if (nights > MAX_STAY_NIGHTS) {
            return new StayDateResult(
                    null,
                    null,
                    "Thời gian lưu trú online tối đa là " + MAX_STAY_NIGHTS + " đêm."
            );
        }

        return new StayDateResult(checkInDate, checkOutDate, null);
    }

    private GuestResult normalizeGuestInfo(
            Integer adults,
            Integer children,
            Integer roomCount,
            String roomGuests
    ) {
        int safeRoomCount = clamp(
                roomCount,
                MIN_ROOM_COUNT,
                MAX_ONLINE_ROOM_COUNT
        );

        if (roomGuests != null && !roomGuests.isBlank()) {
            GuestResult parsedResult = parseRoomGuests(roomGuests);

            if (parsedResult != null) {
                return parsedResult;
            }
        }

        int minAdults = safeRoomCount * MIN_ADULT_PER_ROOM;

        int safeAdults = clamp(
                adults,
                minAdults,
                MAX_TOTAL_ADULTS
        );

        int safeChildren = clamp(
                children,
                MIN_CHILD_PER_ROOM,
                MAX_TOTAL_CHILDREN
        );

        String normalizedRoomGuests = buildDefaultRoomGuests(
                safeAdults,
                safeChildren,
                safeRoomCount
        );

        String warningMessage = null;

        if (roomCount != null && roomCount > MAX_ONLINE_ROOM_COUNT) {
            warningMessage = "Số phòng đặt online tối đa là " + MAX_ONLINE_ROOM_COUNT + " phòng.";
        }

        return new GuestResult(
                safeAdults,
                safeChildren,
                safeRoomCount,
                normalizedRoomGuests,
                warningMessage
        );
    }

    private GuestResult parseRoomGuests(String roomGuests) {
        String[] rawRooms = roomGuests.split("\\|");

        List<String> normalizedRooms = new ArrayList<>();

        int totalAdults = 0;
        int totalChildren = 0;

        for (String rawRoom : rawRooms) {
            if (rawRoom == null || rawRoom.isBlank()) {
                continue;
            }

            if (normalizedRooms.size() >= MAX_ONLINE_ROOM_COUNT) {
                break;
            }

            String[] parts = rawRoom.trim().split("-");

            int adults = parseInteger(parts.length > 0 ? parts[0] : null, MIN_ADULT_PER_ROOM);
            int children = parseInteger(parts.length > 1 ? parts[1] : null, MIN_CHILD_PER_ROOM);

            adults = clamp(adults, MIN_ADULT_PER_ROOM, MAX_ADULT_PER_ROOM);
            children = clamp(children, MIN_CHILD_PER_ROOM, MAX_CHILD_PER_ROOM);

            totalAdults += adults;
            totalChildren += children;

            normalizedRooms.add(adults + "-" + children);
        }

        if (normalizedRooms.isEmpty()) {
            return null;
        }

        String warningMessage = null;

        if (rawRooms.length > MAX_ONLINE_ROOM_COUNT) {
            warningMessage = "Số phòng đặt online tối đa là " + MAX_ONLINE_ROOM_COUNT + " phòng.";
        }

        return new GuestResult(
                totalAdults,
                totalChildren,
                normalizedRooms.size(),
                String.join("|", normalizedRooms),
                warningMessage
        );
    }

    private String buildDefaultRoomGuests(int totalAdults, int totalChildren, int roomCount) {
        List<String> rooms = new ArrayList<>();

        int adultsLeft = totalAdults;
        int childrenLeft = totalChildren;

        for (int i = 1; i <= roomCount; i++) {
            int roomsLeftAfterThisRoom = roomCount - i;

            int adultsForRoom = Math.max(
                    MIN_ADULT_PER_ROOM,
                    Math.min(MAX_ADULT_PER_ROOM, adultsLeft - roomsLeftAfterThisRoom)
            );

            adultsLeft -= adultsForRoom;

            int childrenForRoom = Math.min(MAX_CHILD_PER_ROOM, childrenLeft);
            childrenLeft -= childrenForRoom;

            rooms.add(adultsForRoom + "-" + childrenForRoom);
        }

        return String.join("|", rooms);
    }

    private int parseInteger(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private int clamp(Integer value, int min, int max) {
        if (value == null) {
            return min;
        }

        return Math.max(min, Math.min(max, value));
    }

}