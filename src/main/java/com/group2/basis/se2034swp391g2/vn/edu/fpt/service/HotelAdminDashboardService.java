package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.AdminDashboardResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.DashboardAlertResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.DashboardRoomItemResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelAdminDashboardService {

    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public AdminDashboardResponse getDashboard() {
        long totalRooms = roomRepository.countByIsDeletedFalse();
        long availableRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.AVAILABLE);
        long occupiedRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.OCCUPIED);
        long maintenanceRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.MAINTENANCE);

        long totalServices = serviceRepository.countByIsDeletedFalse();
        long availableServices = serviceRepository.countByIsDeletedFalseAndIsAvailableTrue();
        long unavailableServices = serviceRepository.countByIsDeletedFalseAndIsAvailableFalse();

        long activeStaff = userRepository.countStaffByActiveStatus(true, RoleName.GUEST);
        long inactiveStaff = userRepository.countStaffByActiveStatus(false, RoleName.GUEST);
        long totalStaff = activeStaff + inactiveStaff;

        List<DashboardRoomItemResponse> roomOverview = roomRepository
                .findTop16ByIsDeletedFalseOrderByRoomNumberAsc()
                .stream()
                .map(this::toRoomItem)
                .toList();

        List<DashboardAlertResponse> alerts = buildAlerts();

        return AdminDashboardResponse.builder()
                .totalStaff(totalStaff)
                .activeStaff(activeStaff)
                .inactiveStaff(inactiveStaff)
                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .occupiedRooms(occupiedRooms)
                .maintenanceRooms(maintenanceRooms)
                .totalServices(totalServices)
                .availableServices(availableServices)
                .unavailableServices(unavailableServices)
                .roomOverview(roomOverview)
                .alerts(alerts)
                .build();
    }

    private DashboardRoomItemResponse toRoomItem(Room room) {
        return DashboardRoomItemResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomTypeName(resolveRoomTypeName(room))
                .status(room.getStatus())
                .statusLabel(resolveRoomStatusLabel(room.getStatus()))
                .statusClass(resolveRoomStatusClass(room.getStatus()))
                .detailUrl("/hotel-admin/rooms/view/" + room.getId())
                .build();
    }

    private List<DashboardAlertResponse> buildAlerts() {
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        roomRepository.findTop5ByStatusAndIsDeletedFalseOrderByUpdatedAtDesc(RoomStatus.MAINTENANCE)
                .forEach(room -> alerts.add(DashboardAlertResponse.builder()
                        .type("Phòng")
                        .title("Phòng " + room.getRoomNumber() + " đang bảo trì")
                        .message(room.getNote() == null || room.getNote().isBlank()
                                ? "Cần kiểm tra và cập nhật trạng thái phòng."
                                : room.getNote())
                        .actionLabel("Cập nhật")
                        .actionUrl("/hotel-admin/rooms/edit/" + room.getId())
                        .level("warning")
                        .build()));

        serviceRepository.findTop5ByIsDeletedFalseAndIsAvailableFalseOrderByUpdatedAtDesc()
                .forEach(service -> alerts.add(DashboardAlertResponse.builder()
                        .type("Dịch vụ")
                        .title(service.getName() + " đang tạm ngừng")
                        .message("Dịch vụ hiện chưa sẵn sàng để khách sử dụng.")
                        .actionLabel("Cập nhật")
                        .actionUrl("/hotel-admin/services/edit/" + service.getId())
                        .level("danger")
                        .build()));

        return alerts.stream()
                .limit(6)
                .toList();
    }

    private String resolveRoomTypeName(Room room) {
        RoomTypeVariant variant = room.getVariant();

        if (variant == null) {
            return "Chưa có hạng phòng";
        }

        RoomType roomType = variant.getRoomType();

        if (roomType == null || roomType.getName() == null || roomType.getName().isBlank()) {
            return variant.getVariantName();
        }

        return roomType.getName();
    }

    private String resolveRoomStatusLabel(RoomStatus status) {
        if (status == null) {
            return "Không xác định";
        }

        return switch (status) {
            case AVAILABLE -> "Sẵn sàng";
            case OCCUPIED -> "Đang ở";
            case MAINTENANCE -> "Bảo trì";
        };
    }

    private String resolveRoomStatusClass(RoomStatus status) {
        if (status == null) {
            return "room-unknown";
        }

        return switch (status) {
            case AVAILABLE -> "room-available";
            case OCCUPIED -> "room-occupied";
            case MAINTENANCE -> "room-maintenance";
        };
    }
}