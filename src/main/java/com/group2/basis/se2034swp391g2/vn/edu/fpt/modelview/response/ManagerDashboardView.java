package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ManagerDashboardView(
        LocalDate today,
        long arrivals,
        long departures,
        long stayingBookings,
        long pendingBookings,
        long occupiedRooms,
        long availableRooms,
        long maintenanceRooms,
        long totalRooms,
        long occupancyRate,
        long newBookings,
        long lowStockCount,
        List<PriorityItem> priorities,
        List<LowStockItem> lowStockItems
) {
    public record PriorityItem(String level, String icon, String title, String description, String actionUrl) {
    }

    public record LowStockItem(String code, String name, BigDecimal currentQuantity,
                               BigDecimal minimumQuantity, String unit) {
    }
}
