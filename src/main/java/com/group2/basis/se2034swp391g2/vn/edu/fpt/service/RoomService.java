package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Image;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ImageRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoomService {

    private static final int TOTAL_FLOORS = 6;
    private static final int ROOMS_PER_FLOOR = 16;
    private static final int MAX_NOTE_LENGTH = 500;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeVariantRepository roomTypeVariantRepository;
    private final ImageRepository imageRepository;
    private final BookingDetailRepository bookingDetailRepository;

    public RoomService(RoomRepository roomRepository,
                       RoomTypeRepository roomTypeRepository,
                       RoomTypeVariantRepository roomTypeVariantRepository,
                       ImageRepository imageRepository,
                       BookingDetailRepository bookingDetailRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomTypeVariantRepository = roomTypeVariantRepository;
        this.imageRepository = imageRepository;
        this.bookingDetailRepository = bookingDetailRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findByIsDeletedFalse();
    }

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findByIsDeletedFalse();
    }

    public List<RoomTypeVariant> getAllRoomTypeVariants() {
        return roomTypeVariantRepository.findByIsDeletedFalseOrderByRoomType_NameAscVariantNameAsc();
    }

    public Page<Room> getRoomsPage(Pageable pageable) {
        return roomRepository.findByIsDeletedFalse(pageable);
    }

    public Page<Room> searchRoomsForAdmin(String keyword,
                                          String roomType,
                                          Integer floor,
                                          ViewType viewType,
                                          RoomStatus status,
                                          String operatingStatus,
                                          Integer page,
                                          Integer size) {

        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);

        Pageable pageable = PageRequest.of(validPage, validSize);

        return roomRepository.searchForAdmin(
                keyword,
                roomType,
                floor,
                viewType,
                status,
                operatingStatus,
                pageable
        );
    }

    public List<Integer> getRoomFloors() {
        List<Integer> floors = new ArrayList<>();

        for (int floor = 1; floor <= TOTAL_FLOORS; floor++) {
            floors.add(floor);
        }

        return floors;
    }

    public List<String> getRoomTypeNamesForAdminFilter() {
        return roomRepository.findDistinctRoomTypeNamesForAdmin();
    }

    public Room getRoomById(Long id) {
        return roomRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng!"));
    }

    public List<RoomStatus> getInitialRoomStatusesForAddRoom() {
        return List.of(
                RoomStatus.AVAILABLE,
                RoomStatus.MAINTENANCE
        );
    }

    public List<Map<String, Object>> getAvailableRoomNumberOptions() {
        Set<String> existingRoomNumbers = new HashSet<>(roomRepository.findExistingRoomNumbers());

        List<Map<String, Object>> options = new ArrayList<>();

        for (int floor = 1; floor <= TOTAL_FLOORS; floor++) {
            for (int roomIndex = 1; roomIndex <= ROOMS_PER_FLOOR; roomIndex++) {
                String roomNumber = floor + String.format("%02d", roomIndex);

                if (!existingRoomNumbers.contains(roomNumber)) {
                    options.add(Map.of(
                            "roomNumber", roomNumber,
                            "floor", floor,
                            "roomTypeName", getRoomTypeNameByFloor(floor)
                    ));
                }
            }
        }

        return options;
    }

    public Integer getFloorForDisplay(String roomNumber) {
        String normalizedRoomNumber = normalizeRoomNumber(roomNumber);
        validateRoomNumberInHotelRange(normalizedRoomNumber);

        return getFloorFromRoomNumber(normalizedRoomNumber);
    }

    public String getRoomTypeNameForDisplay(String roomNumber) {
        Integer floor = getFloorForDisplay(roomNumber);

        return getRoomTypeNameByFloor(floor);
    }

    public List<RoomTypeVariant> getRoomTypeVariantsByRoomNumber(String roomNumber) {
        String roomTypeName = getRoomTypeNameForDisplay(roomNumber);

        return getRoomTypeVariantsByRoomTypeName(roomTypeName);
    }

    public List<RoomTypeVariant> getRoomTypeVariantsByRoomTypeName(String roomTypeName) {
        if (roomTypeName == null || roomTypeName.trim().isEmpty()) {
            return List.of();
        }

        String normalizedRoomTypeName = roomTypeName.trim();

        return getAllRoomTypeVariants()
                .stream()
                .filter(variant -> variant.getRoomType() != null)
                .filter(variant -> variant.getRoomType().getName() != null)
                .filter(variant -> variant.getRoomType().getName().equalsIgnoreCase(normalizedRoomTypeName))
                .toList();
    }

    @Transactional
    public void createRoom(String roomNumber,
                           Long variantId,
                           RoomStatus status,
                           String note) {

        createRoomInternal(roomNumber, variantId, status, note);
    }

    @Transactional
    public void createRoom(String roomNumber,
                           Long variantId,
                           Integer floor,
                           RoomStatus status,
                           String note,
                           List<String> imageUrls,
                           Integer primaryImageIndex) {

        Room savedRoom = createRoomInternal(roomNumber, variantId, status, note);

        try {
            saveRoomImages(savedRoom.getId(), imageUrls, primaryImageIndex);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Lưu ảnh phòng thất bại. Vui lòng kiểm tra lại dữ liệu ảnh.");
        }
    }

    private Room createRoomInternal(String roomNumber,
                                    Long variantId,
                                    RoomStatus status,
                                    String note) {

        String normalizedRoomNumber = normalizeRoomNumber(roomNumber);

        validateRoomNumberInHotelRange(normalizedRoomNumber);
        validateRoomNumberNotExists(normalizedRoomNumber);

        int floor = getFloorFromRoomNumber(normalizedRoomNumber);
        String expectedRoomTypeName = getRoomTypeNameByFloor(floor);

        RoomTypeVariant variant = validateAndGetRoomTypeVariant(variantId);
        validateVariantMatchesRoomType(variant, expectedRoomTypeName);

        validateInitialRoomStatus(status);

        String normalizedNote = validateAndNormalizeNote(note, status);

        Room room = new Room();
        room.setRoomNumber(normalizedRoomNumber);
        room.setVariant(variant);
        room.setFloor(floor);
        room.setStatus(status);
        room.setNote(normalizedNote);
        room.setIsDeleted(false);

        try {
            return roomRepository.saveAndFlush(room);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Số phòng này đã tồn tại trong hệ thống.");
        }
    }

    @Transactional
    public void updateRoom(Long id,
                           String roomNumber,
                           Long variantId,
                           Integer floor,
                           RoomStatus status,
                           String note) {

        Room room = roomRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng!"));

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Số phòng không được để trống!");
        }

        String normalizedRoomNumber = roomNumber.trim();

        if (!isValidRoomNumber(normalizedRoomNumber)) {
            throw new IllegalArgumentException("Số phòng không hợp lệ!");
        }

        Integer calculatedFloor = getFloorFromRoomNumber(normalizedRoomNumber);

        boolean roomNumberExists = roomRepository.findExistingRoomNumbers()
                .stream()
                .anyMatch(existingRoomNumber -> existingRoomNumber != null
                        && existingRoomNumber.equalsIgnoreCase(normalizedRoomNumber));

        if (!room.getRoomNumber().equalsIgnoreCase(normalizedRoomNumber) && roomNumberExists) {
            throw new IllegalArgumentException("Số phòng đã tồn tại!");
        }

        if (variantId == null) {
            throw new IllegalArgumentException("Vui lòng chọn hạng phòng!");
        }

        RoomTypeVariant variant = roomTypeVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng phòng!"));

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            throw new IllegalArgumentException("Hạng phòng này đã bị xóa!");
        }

        if (status == null) {
            status = RoomStatus.AVAILABLE;
        }

        room.setRoomNumber(normalizedRoomNumber);
        room.setVariant(variant);
        room.setFloor(calculatedFloor);
        room.setStatus(status);
        room.setNote(normalizeNote(note));

        roomRepository.save(room);
    }

    @Transactional
    public void toggleRoomOperatingStatus(Long id) {
        Room room = roomRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng!"));

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            validateCanActivate(room);
            room.setStatus(RoomStatus.AVAILABLE);
        } else {
            validateCanDeactivate(room);
            room.setStatus(RoomStatus.MAINTENANCE);
        }

        roomRepository.save(room);
    }

    private String normalizeRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn số phòng hợp lệ.");
        }

        String value = roomNumber.trim();

        if (!value.matches("\\d+")) {
            throw new IllegalArgumentException("Vui lòng chọn số phòng hợp lệ.");
        }

        return value;
    }

    private void validateRoomNumberInHotelRange(String roomNumber) {
        if (!isValidRoomNumber(roomNumber)) {
            throw new IllegalArgumentException("Số phòng không nằm trong phạm vi hợp lệ của khách sạn.");
        }
    }

    private void validateRoomNumberNotExists(String roomNumber) {
        boolean exists = roomRepository.findExistingRoomNumbers()
                .stream()
                .anyMatch(existingRoomNumber -> existingRoomNumber != null
                        && existingRoomNumber.equalsIgnoreCase(roomNumber));

        if (exists) {
            throw new IllegalArgumentException("Số phòng này đã tồn tại trong hệ thống.");
        }
    }

    private RoomTypeVariant validateAndGetRoomTypeVariant(Long variantId) {
        if (variantId == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại phòng chi tiết.");
        }

        RoomTypeVariant variant = roomTypeVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn loại phòng chi tiết."));

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            throw new IllegalArgumentException("Loại phòng chi tiết không còn hoạt động.");
        }

        if (variant.getRoomType() == null || Boolean.TRUE.equals(variant.getRoomType().getIsDeleted())) {
            throw new IllegalArgumentException("Loại phòng chi tiết không còn hoạt động.");
        }

        return variant;
    }

    private void validateVariantMatchesRoomType(RoomTypeVariant variant, String expectedRoomTypeName) {
        String actualRoomTypeName = variant.getRoomType().getName();

        if (actualRoomTypeName == null
                || !actualRoomTypeName.trim().equalsIgnoreCase(expectedRoomTypeName)) {
            throw new IllegalArgumentException("Loại phòng chi tiết không phù hợp với hạng phòng của tầng đã chọn.");
        }
    }

    private void validateInitialRoomStatus(RoomStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái phòng.");
        }

        if (status != RoomStatus.AVAILABLE && status != RoomStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Trạng thái ban đầu của phòng chỉ có thể là AVAILABLE hoặc MAINTENANCE.");
        }
    }

    private String validateAndNormalizeNote(String note, RoomStatus status) {
        String value = normalizeNote(note);

        if (value != null && value.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Số ký tự đã vượt quá giới hạn cho phép.");
        }

        if (status == RoomStatus.MAINTENANCE && (value == null || value.isBlank())) {
            throw new IllegalArgumentException("Vui lòng nhập lý do bảo trì.");
        }

        return value;
    }

    private String getRoomTypeNameByFloor(int floor) {
        return switch (floor) {
            case 1 -> "Standard Room";
            case 2 -> "Superior Room";
            case 3 -> "Deluxe Room";
            case 4 -> "Executive Room";
            case 5 -> "Family Room";
            case 6 -> "Suite Room";
            default -> throw new IllegalArgumentException("Tầng phải nằm trong phạm vi từ 1 đến 6.");
        };
    }

    private boolean isValidRoomNumber(String roomNumber) {
        if (roomNumber == null || !roomNumber.matches("\\d{3}")) {
            return false;
        }

        int number = Integer.parseInt(roomNumber);
        int floor = number / 100;
        int roomIndex = number % 100;

        return floor >= 1
                && floor <= TOTAL_FLOORS
                && roomIndex >= 1
                && roomIndex <= ROOMS_PER_FLOOR;
    }

    private Integer getFloorFromRoomNumber(String roomNumber) {
        int number = Integer.parseInt(roomNumber);

        return number / 100;
    }

    private String normalizeNote(String note) {
        if (note == null || note.trim().isEmpty()) {
            return null;
        }

        return note.trim();
    }

    private void validateCanDeactivate(Room room) {
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Không thể tạm ngưng phòng vì phòng đang có khách hoặc đang liên kết với booking đang hoạt động.");
        }

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN
        );

        boolean hasActiveOrUpcomingBooking = bookingDetailRepository
                .existsActiveOrUpcomingBookingByRoomId(room.getId(), blockingStatuses);

        if (hasActiveOrUpcomingBooking) {
            throw new IllegalStateException("Không thể tạm ngưng phòng vì phòng đang có khách hoặc đang liên kết với booking đang hoạt động.");
        }
    }

    private void validateCanActivate(Room room) {
        if (Boolean.TRUE.equals(room.getIsDeleted())) {
            throw new IllegalStateException("Không thể bật phòng vì phòng đã bị xóa mềm.");
        }

        if (room.getVariant() == null || Boolean.TRUE.equals(room.getVariant().getIsDeleted())) {
            throw new IllegalStateException("Không thể bật phòng vì hạng phòng của phòng này đang không hoạt động.");
        }
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }

        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void saveRoomImages(Long roomId,
                                List<String> imageUrls,
                                Integer primaryImageIndex) {

        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        imageUrls = imageUrls.stream()
                .filter(url -> url != null && !url.trim().isEmpty())
                .map(String::trim)
                .toList();

        if (imageUrls.isEmpty()) {
            return;
        }

        if (primaryImageIndex == null
                || primaryImageIndex < 0
                || primaryImageIndex >= imageUrls.size()) {
            primaryImageIndex = 0;
        }

        for (int i = 0; i < imageUrls.size(); i++) {
            Image image = new Image();
            image.setEntityType(ImageEntityType.ROOM);
            image.setEntityId(roomId);
            image.setImageUrl(imageUrls.get(i));
            image.setIsPrimary(i == primaryImageIndex);
            image.setSortOrder(i + 1);
            image.setUploadedBy(null);

            imageRepository.save(image);
        }
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng!"));

        room.setIsDeleted(true);
        roomRepository.save(room);
    }

    public List<Image> getRoomImages(Long roomId) {
        return imageRepository.findByEntityTypeAndEntityIdOrderBySortOrderAsc(
                ImageEntityType.ROOM,
                roomId
        );
    }
}