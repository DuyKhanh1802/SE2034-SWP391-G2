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
import org.springframework.stereotype.Service;

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

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found!"));
    }

    public void createRoom(String roomNumber,
                           Long roomTypeId,
                           Integer floor,
                           ViewType viewType,
                           RoomStatus status,
                           List<String> imageUrls,
                           Integer primaryImageIndex) {

        if (roomRepository.existsByRoomNumberAndIsDeletedFalse(roomNumber)) {
            throw new IllegalArgumentException("Room number already exists!");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found!"));

        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setFloor(floor);
        room.setViewType(viewType);
        room.setStatus(status);
        room.setIsDeleted(false);

        Room savedRoom = roomRepository.save(room);

        saveRoomImages(savedRoom.getId(), imageUrls, primaryImageIndex);
    }

    public void updateRoom(Long id,
                           String roomNumber,
                           Long roomTypeId,
                           Integer floor,
                           ViewType viewType,
                           RoomStatus status) {

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found!"));

        if (!room.getRoomNumber().equalsIgnoreCase(roomNumber)
                && roomRepository.existsByRoomNumberAndIsDeletedFalse(roomNumber)) {
            throw new IllegalArgumentException("Room number already exists!");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found!"));

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

        if (primaryImageIndex == null || primaryImageIndex < 0 || primaryImageIndex >= imageUrls.size()) {
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