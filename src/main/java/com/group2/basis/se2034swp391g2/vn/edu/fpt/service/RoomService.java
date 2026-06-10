package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ImageEntityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Image;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ImageRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ImageRepository imageRepository;

    public RoomService(RoomRepository roomRepository,
                       RoomTypeRepository roomTypeRepository,
                       ImageRepository imageRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.imageRepository = imageRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findByIsDeletedFalse();
    }

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findByIsDeletedFalse();
    }

    public Page<Room> getRoomsPage(Pageable pageable) {
        return roomRepository.findByIsDeletedFalse(pageable);
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng!"));
    }

    @Transactional
    public void createRoom(String roomNumber,
                           Long roomTypeId,
                           Integer floor,
                           ViewType viewType,
                           RoomStatus status,
                           List<String> imageUrls,
                           Integer primaryImageIndex) {

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Số phòng không được để trống!");
        }

        roomNumber = roomNumber.trim();

        if (roomRepository.existsByRoomNumberAndIsDeletedFalse(roomNumber)) {
            throw new IllegalArgumentException("Số phòng đã tồn tại!");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng phòng!"));

        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setFloor(floor);
        room.setViewType(viewType);
        room.setStatus(status);
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

    public void updateRoom(Long id,
                           String roomNumber,
                           Long roomTypeId,
                           Integer floor,
                           ViewType viewType,
                           RoomStatus status) {

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng!"));

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Số phòng không được để trống!");
        }

        roomNumber = roomNumber.trim();

        if (!room.getRoomNumber().equalsIgnoreCase(roomNumber)
                && roomRepository.existsByRoomNumberAndIsDeletedFalse(roomNumber)) {
            throw new IllegalArgumentException("Số phòng đã tồn tại!");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng phòng!"));

        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setFloor(floor);
        room.setViewType(viewType);
        room.setStatus(status);

        roomRepository.save(room);
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

    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
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