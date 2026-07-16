package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.time.Instant;
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
        BigDecimal todayIncome,
        BigDecimal todayExpense,
        BigDecimal monthIncome,
        BigDecimal monthExpense,
        BigDecimal monthNetCashFlow,
        List<LowStockItem> lowStockItems,
        List<TopServiceItem> topServices,
        List<RecentTransactionItem> recentTransactions
) {
    public record LowStockItem(String code, String name, BigDecimal currentQuantity,
                               BigDecimal minimumQuantity, String unit) {
    }

    public record TopServiceItem(String name, long quantity, BigDecimal revenue) {
    }

    public record RecentTransactionItem(Long id, String documentCode, String type,
                                        String category, String paymentMethod,
                                        BigDecimal amount, Instant createdAt) {
    }
}
