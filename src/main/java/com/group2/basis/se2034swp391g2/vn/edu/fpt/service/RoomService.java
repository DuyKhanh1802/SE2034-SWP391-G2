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
    private static final int ADD_ROOM_TOTAL_FLOORS = 4;
    private static final int ADD_ROOM_ROOMS_PER_FLOOR = 18;
    private static final int MAX_NOTE_LENGTH = 500;

    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 20;

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
                                          Long variantId,
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
                variantId,
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

    public List<RoomStatus> getEditableRoomStatuses() {
        return List.of(
                RoomStatus.AVAILABLE,
                RoomStatus.MAINTENANCE
        );
    }

    public List<Map<String, Object>> getAvailableRoomNumberOptions() {
        Set<String> existingRoomNumbers = new HashSet<>(roomRepository.findExistingRoomNumbers());

        List<Map<String, Object>> options = new ArrayList<>();

        for (int floor = 1; floor <= ADD_ROOM_TOTAL_FLOORS; floor++) {
            for (int roomIndex = 1; roomIndex <= ADD_ROOM_ROOMS_PER_FLOOR; roomIndex++) {
                String roomNumber = floor + String.format("%02d", roomIndex);

                if (!existingRoomNumbers.contains(roomNumber)) {
                    options.add(Map.of(
                            "roomNumber", roomNumber,
                            "floor", floor,
                            "roomTypeName", getRoomTypeNameByRoomIndex(roomIndex),
                            "viewType", getViewTypeByFloor(floor).name()
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
        String normalizedRoomNumber = normalizeRoomNumber(roomNumber);
        validateRoomNumberInHotelRange(normalizedRoomNumber);

        return getRoomTypeNameByRoomIndex(getRoomIndexFromRoomNumber(normalizedRoomNumber));
    }

    public ViewType getViewTypeForDisplay(String roomNumber) {
        return getViewTypeByFloor(getFloorForDisplay(roomNumber));
    }

    public List<RoomTypeVariant> getRoomTypeVariantsByRoomNumber(String roomNumber) {
        String roomTypeName = getRoomTypeNameForDisplay(roomNumber);
        ViewType viewType = getViewTypeForDisplay(roomNumber);

        return getRoomTypeVariantsByRoomTypeName(roomTypeName)
                .stream()
                .filter(variant -> variant.getViewType() == viewType)
                .toList();
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
        String expectedRoomTypeName = getRoomTypeNameByRoomIndex(getRoomIndexFromRoomNumber(normalizedRoomNumber));
        ViewType expectedViewType = getViewTypeByFloor(floor);

        RoomTypeVariant variant = validateAndGetRoomTypeVariant(variantId);
        validateVariantMatchesRoomConfiguration(variant, expectedRoomTypeName, expectedViewType);

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

    /*
     * Giữ lại method cũ để tránh lỗi nếu có chỗ khác trong project đang gọi.
     * Tuy nhiên, từ nay method này chỉ cập nhật thông tin vận hành: status + note.
     * Không update roomNumber, floor, variant, viewType ở Edit Room nữa.
     */
    @Transactional
    public void updateRoom(Long id,
                           String roomNumber,
                           Long variantId,
                           Integer floor,
                           RoomStatus status,
                           String note) {

        updateRoomOperationalInfo(
                id,
                status == null ? null : status.name(),
                note
        );
    }

    @Transactional
    public void updateRoomOperationalInfo(Long id,
                                          String statusValue,
                                          String note) {

        Room room = roomRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng!"));

        validateRoomNotOccupiedForStatusUpdate(room);

        validateRoomHasNoActiveOrUpcomingBooking(
                room,
                "Không thể thay đổi trạng thái vì phòng đang liên kết với booking đang hoạt động."
        );

        RoomStatus status = parseEditableRoomStatus(statusValue);
        String normalizedNote = validateAndNormalizeNote(note, status);

        room.setStatus(status);
        room.setNote(normalizedNote);

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

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng!"));

        validateRoomNotOccupiedForDelete(room);

        validateRoomHasNoActiveOrUpcomingBooking(
                room,
                "Không thể xóa phòng vì phòng đang liên kết với booking đang hoạt động."
        );

        room.setIsDeleted(true);
        roomRepository.save(room);
    }

    /*
     * Giữ lại method cũ vì có thể các màn khác vẫn đang dùng ảnh theo phòng vật lý.
     * Riêng View Room Details của Room Management mới sẽ dùng ảnh theo RoomTypeVariant.
     */
    public List<Image> getRoomImages(Long roomId) {
        if (roomId == null || roomId <= 0) {
            return List.of();
        }

        return imageRepository.findByEntityTypeAndEntityIdOrderBySortOrderAsc(
                ImageEntityType.ROOM,
                roomId
        );
    }

    public List<Image> getRoomTypeVariantImages(Long variantId) {
        if (variantId == null || variantId <= 0) {
            return List.of();
        }

        return imageRepository.findByEntityTypeAndEntityIdOrderBySortOrderAsc(
                ImageEntityType.ROOM_TYPE_VARIANT,
                variantId
        );
    }

    private RoomStatus parseEditableRoomStatus(String statusValue) {
        if (statusValue == null || statusValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái phòng.");
        }

        String value = statusValue.trim().toUpperCase();

        if (!Set.of(RoomStatus.AVAILABLE.name(), RoomStatus.MAINTENANCE.name()).contains(value)) {
            throw new IllegalArgumentException("Trạng thái chỉ được phép cập nhật là AVAILABLE hoặc MAINTENANCE.");
        }

        return RoomStatus.valueOf(value);
    }

    private void validateRoomNotOccupiedForStatusUpdate(Room room) {
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Không thể thay đổi trạng thái vì phòng đang có khách.");
        }
    }

    private void validateRoomNotOccupiedForDelete(Room room) {
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Không thể xóa phòng vì phòng đang có khách.");
        }
    }

    private void validateRoomHasNoActiveOrUpcomingBooking(Room room, String errorMessage) {
        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED,
                BookingStatus.CHECKED_IN
        );

        boolean hasActiveOrUpcomingBooking = bookingDetailRepository
                .existsActiveOrUpcomingBookingByRoomId(room.getId(), blockingStatuses);

        if (hasActiveOrUpcomingBooking) {
            throw new IllegalStateException(errorMessage);
        }
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

    private void validateVariantMatchesRoomConfiguration(RoomTypeVariant variant,
                                                         String expectedRoomTypeName,
                                                         ViewType expectedViewType) {
        String actualRoomTypeName = variant.getRoomType().getName();

        if (actualRoomTypeName == null
                || !actualRoomTypeName.trim().equalsIgnoreCase(expectedRoomTypeName)) {
            throw new IllegalArgumentException("Loại phòng chi tiết không phù hợp với hạng phòng của số phòng đã chọn.");
        }

        if (variant.getViewType() != expectedViewType) {
            throw new IllegalArgumentException("Hướng phòng không phù hợp với tầng của số phòng đã chọn.");
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

    private String getRoomTypeNameByRoomIndex(int roomIndex) {
        return switch ((roomIndex - 1) / 3) {
            case 0 -> "Standard Room";
            case 1 -> "Superior Room";
            case 2 -> "Deluxe Room";
            case 3 -> "Executive Room";
            case 4 -> "Family Room";
            case 5 -> "Suite Room";
            default -> throw new IllegalArgumentException("Số thứ tự phòng phải nằm trong phạm vi từ 01 đến 18.");
        };
    }

    private ViewType getViewTypeByFloor(int floor) {
        return switch (floor) {
            case 1 -> ViewType.GARDEN;
            case 2 -> ViewType.CITY;
            case 3 -> ViewType.SEA;
            case 4 -> ViewType.POOL;
            default -> throw new IllegalArgumentException("Tầng phải nằm trong phạm vi từ 1 đến 4.");
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
                && floor <= ADD_ROOM_TOTAL_FLOORS
                && roomIndex >= 1
                && roomIndex <= ADD_ROOM_ROOMS_PER_FLOOR;
    }

    private Integer getFloorFromRoomNumber(String roomNumber) {
        int number = Integer.parseInt(roomNumber);

        return number / 100;
    }

    private Integer getRoomIndexFromRoomNumber(String roomNumber) {
        int number = Integer.parseInt(roomNumber);

        return number % 100;
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
}
