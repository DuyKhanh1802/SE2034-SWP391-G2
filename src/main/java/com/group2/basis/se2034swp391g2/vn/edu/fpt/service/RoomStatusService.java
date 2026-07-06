package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RoomStatusBoardResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomStatusService {

    private static final int MAX_STATUS_NOTE_LENGTH = 300;

    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;

    @Transactional(readOnly = true)
    public List<RoomStatusBoardResponse> getRoomStatusBoard(Integer floor,
                                                            String roomTypeName,
                                                            RoomStatus status,
                                                            String keyword) {
        String searchKeyword = keyword == null ? "" : keyword.trim();

        if (roomTypeName != null && roomTypeName.isBlank()) {
            roomTypeName = null;
        }

        return roomRepository.searchRoomStatusBoard(
                floor,
                roomTypeName,
                status,
                searchKeyword
        );
    }

    @Transactional(readOnly = true)
    public List<Integer> getFloors() {
        return roomRepository.findDistinctFloors();
    }

    @Transactional
    public void updateRoomStatus(Long roomId, RoomStatus newStatus, String note) {
        if (roomId == null) {
            throw new IllegalArgumentException("Thiếu mã phòng.");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái mới.");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng."));

        if (Boolean.TRUE.equals(room.getIsDeleted())) {
            throw new IllegalArgumentException("Phòng này đã bị xóa.");
        }

        RoomStatus currentStatus = room.getStatus();

        if (currentStatus == null) {
            throw new IllegalArgumentException("Phòng chưa có trạng thái hợp lệ.");
        }

        if (currentStatus == RoomStatus.OCCUPIED) {
            throw new IllegalArgumentException("Không thể đổi trạng thái phòng đang có khách.");
        }

        if (newStatus == RoomStatus.OCCUPIED) {
            throw new IllegalArgumentException("Không thể chuyển phòng sang đang có khách thủ công. Vui lòng thực hiện check-in.");
        }

        if (currentStatus == newStatus) {
            throw new IllegalArgumentException("Trạng thái mới không thay đổi so với trạng thái hiện tại.");
        }

        if (currentStatus == RoomStatus.AVAILABLE && newStatus != RoomStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Phòng trống chỉ có thể chuyển sang bảo trì.");
        }

        if (currentStatus == RoomStatus.MAINTENANCE && newStatus != RoomStatus.AVAILABLE) {
            throw new IllegalArgumentException("Phòng bảo trì chỉ có thể chuyển về sẵn sàng.");
        }

        String cleanNote = note == null ? "" : note.trim();

        if (cleanNote.length() > MAX_STATUS_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú trạng thái phòng không được vượt quá 300 ký tự.");
        }

        if (currentStatus == RoomStatus.AVAILABLE && newStatus == RoomStatus.MAINTENANCE) {
            boolean hasActiveOrFutureBooking =
                    bookingDetailRepository.existsActiveOrFutureBookingByRoomId(roomId, LocalDate.now());

            if (hasActiveOrFutureBooking) {
                throw new IllegalArgumentException("Không thể đưa phòng vào bảo trì vì phòng đang được gán cho booking hiện tại hoặc tương lai.");
            }

            if (cleanNote.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do bảo trì.");
            }

            room.setStatus(RoomStatus.MAINTENANCE);
            room.setNote(cleanNote);
        }

        if (currentStatus == RoomStatus.MAINTENANCE && newStatus == RoomStatus.AVAILABLE) {
            room.setStatus(RoomStatus.AVAILABLE);
            room.setNote(cleanNote.isEmpty() ? null : cleanNote);
        }

        roomRepository.save(room);
    }
}