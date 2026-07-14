package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ManagerDashboardView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerDashboardService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int ALERT_ITEM_LIMIT = 5;

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final InventoryManagementService inventoryManagementService;

    public ManagerDashboardView getDashboard() {
        LocalDate today = LocalDate.now(APP_ZONE);
        Instant startOfDay = today.atStartOfDay(APP_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant();

        long totalRooms = roomRepository.countByIsDeletedFalse();
        long occupiedRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.OCCUPIED);
        long availableRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.AVAILABLE);
        long maintenanceRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.MAINTENANCE);
        long pendingBookings = bookingRepository.countByStatusAndIsDeletedFalse(BookingStatus.PENDING);
        long arrivals = bookingRepository.countByStatusAndCheckInDateAndIsDeletedFalse(BookingStatus.CONFIRMED, today);
        long departures = bookingRepository.countByStatusAndCheckOutDateAndIsDeletedFalse(BookingStatus.CHECKED_IN, today);
        long stayingBookings = bookingRepository
                .countByStatusAndIsDeletedFalseAndCheckInDateLessThanEqualAndCheckOutDateAfter(
                        BookingStatus.CHECKED_IN, today, today);
        long lowStockCount = inventoryManagementService.countLowStockItems();

        List<InventoryItem> lowStockItems = inventoryManagementService.getLowStockItems().stream()
                .limit(ALERT_ITEM_LIMIT)
                .toList();

        return new ManagerDashboardView(
                today, arrivals, departures, stayingBookings, pendingBookings,
                occupiedRooms, availableRooms, maintenanceRooms, totalRooms,
                percentage(occupiedRooms, totalRooms),
                bookingRepository.countByCreatedAtBetween(startOfDay, endOfDay),
                lowStockCount,
                buildPriorities(pendingBookings, arrivals, availableRooms, maintenanceRooms, lowStockCount),
                lowStockItems.stream().map(this::toLowStockItem).toList()
        );
    }

    private List<ManagerDashboardView.PriorityItem> buildPriorities(long pendingBookings,
                                                                     long arrivals,
                                                                     long availableRooms,
                                                                     long maintenanceRooms,
                                                                     long lowStockCount) {
        List<ManagerDashboardView.PriorityItem> items = new ArrayList<>();

        addPriority(items, pendingBookings > 0, "urgent", "fa-clock", "Booking chờ xác nhận",
                pendingBookings + " booking cần được kiểm tra và xác nhận.", "/manager/reports");
        addPriority(items, arrivals > availableRooms, "urgent", "fa-bed",
                "Nguy cơ thiếu phòng sẵn sàng",
                arrivals + " lượt đến nhưng chỉ còn " + availableRooms + " phòng trống.",
                "/manager/reports/occupancy");
        addPriority(items, maintenanceRooms > 0, "warning", "fa-screwdriver-wrench", "Phòng đang bảo trì",
                maintenanceRooms + " phòng chưa thể đưa vào kinh doanh.", "/manager/reports/occupancy");
        addPriority(items, lowStockCount > 0, "warning", "fa-box-open", "Vật tư dưới định mức",
                lowStockCount + " mặt hàng cần kế hoạch bổ sung.", "/manager/reports/inventory");

        return List.copyOf(items);
    }

    private void addPriority(List<ManagerDashboardView.PriorityItem> items, boolean condition,
                             String level, String icon, String title, String description, String actionUrl) {
        if (condition) {
            items.add(new ManagerDashboardView.PriorityItem(level, icon, title, description, actionUrl));
        }
    }

    private ManagerDashboardView.LowStockItem toLowStockItem(InventoryItem item) {
        return new ManagerDashboardView.LowStockItem(item.getCode(), item.getName(), item.getCurrentQuantity(),
                item.getMinimumQuantity(), item.getUnit());
    }

    private long percentage(long value, long total) {
        return total == 0 ? 0 : Math.round(value * 100.0 / total);
    }
}
