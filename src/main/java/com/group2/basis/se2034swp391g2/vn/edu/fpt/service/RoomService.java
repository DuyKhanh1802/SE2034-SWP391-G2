package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Image;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomNumberOption;
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
import java.util.Set;

@Service
public class RoomService {

    private static final int TOTAL_FLOORS = 6;
    private static final int ROOMS_PER_FLOOR = 4;
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

    public List<RoomNumberOption> getAvailableRoomNumberOptions() {
        Set<String> existingRoomNumbers = new HashSet<>(roomRepository.findExistingRoomNumbers());

        List<RoomNumberOption> options = new ArrayList<>();

        for (int floor = 1; floor <= TOTAL_FLOORS; floor++) {
            for (int roomIndex = 1; roomIndex <= ROOMS_PER_FLOOR; roomIndex++) {

                String roomNumber = floor + String.format("%02d", roomIndex);

                if (!existingRoomNumbers.contains(roomNumber)) {
                    options.add(new RoomNumberOption(roomNumber, floor));
                }
            }
        }

        return options;
    }

    @Transactional
    public void createRoom(String roomNumber,
                           Long variantId,
                           Integer floor,
                           RoomStatus status,
                           String note,
                           List<String> imageUrls,
                           Integer primaryImageIndex) {

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Số phòng không được để trống!");
        }

        roomNumber = roomNumber.trim();

        if (!isValidRoomNumber(roomNumber)) {
            throw new IllegalArgumentException("Số phòng không hợp lệ!");
        }

        floor = getFloorFromRoomNumber(roomNumber);

        if (roomRepository.existsByRoomNumberAndIsDeletedFalse(roomNumber)) {
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

        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setVariant(variant);
        room.setFloor(floor);
        room.setStatus(status);
        room.setNote(normalizeNote(note));
        room.setIsDeleted(false);

        Room savedRoom;

        try {
            savedRoom = roomRepository.saveAndFlush(room);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Số phòng đã tồn tại!");
        }

        try {
            saveRoomImages(savedRoom.getId(), imageUrls, primaryImageIndex);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Lưu ảnh phòng thất bại. Vui lòng kiểm tra lại dữ liệu ảnh.");
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

        roomNumber = roomNumber.trim();

        if (!isValidRoomNumber(roomNumber)) {
            throw new IllegalArgumentException("Số phòng không hợp lệ!");
        }

        floor = getFloorFromRoomNumber(roomNumber);

        if (!room.getRoomNumber().equalsIgnoreCase(roomNumber)
                && roomRepository.existsByRoomNumberAndIsDeletedFalse(roomNumber)) {
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

        room.setRoomNumber(roomNumber);
        room.setVariant(variant);
        room.setFloor(floor);
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

    private boolean isValidRoomNumber(String roomNumber) {
        if (roomNumber == null || !roomNumber.matches("\\d{3}")) {
            return false;
        }

        int floor = Integer.parseInt(roomNumber.substring(0, 1));
        int roomIndex = Integer.parseInt(roomNumber.substring(1));

        return floor >= 1
                && floor <= TOTAL_FLOORS
                && roomIndex >= 1
                && roomIndex <= ROOMS_PER_FLOOR;
    }

    private Integer getFloorFromRoomNumber(String roomNumber) {
        return Integer.parseInt(roomNumber.substring(0, 1));
    }

    private String normalizeNote(String note) {
        if (note == null || note.trim().isEmpty()) {
            return null;
        }

        return note.trim();
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