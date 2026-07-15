package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.InventoryItem;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.CashTransaction;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ManagerDashboardView;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RevenueReportSummaryResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerDashboardService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int ALERT_ITEM_LIMIT = 5;
    private static final int OVERVIEW_ITEM_LIMIT = 5;

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final FolioItemRepository folioItemRepository;
    private final InventoryManagementService inventoryManagementService;
    private final CashTransactionService cashTransactionService;
    private final ManagerRevenueReportService managerRevenueReportService;

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
        List<InventoryItem> lowStockItems = inventoryManagementService.getLowStockItems().stream()
                .limit(ALERT_ITEM_LIMIT)
                .toList();
        RevenueReportSummaryResponse todayCashFlow = managerRevenueReportService
                .getRevenueReport(today, today, "netDesc")
                .getSummary();
        RevenueReportSummaryResponse monthCashFlow = managerRevenueReportService
                .getRevenueReport(today.withDayOfMonth(1), today, "netDesc")
                .getSummary();

        return new ManagerDashboardView(
                today, arrivals, departures, stayingBookings, pendingBookings,
                occupiedRooms, availableRooms, maintenanceRooms, totalRooms,
                percentage(occupiedRooms, totalRooms),
                bookingRepository.countByCreatedAtBetween(startOfDay, endOfDay),
                todayCashFlow.getTotalIncome(), todayCashFlow.getTotalExpense(),
                monthCashFlow.getTotalIncome(), monthCashFlow.getTotalExpense(), monthCashFlow.getNetAmount(),
                lowStockItems.stream().map(this::toLowStockItem).toList(),
                folioItemRepository.findTopServiceSales(PageRequest.of(0, OVERVIEW_ITEM_LIMIT))
                        .stream().map(this::toTopServiceItem).toList(),
                cashTransactionService.getRecentTransactions(OVERVIEW_ITEM_LIMIT)
                        .stream().map(this::toRecentTransactionItem).toList()
        );
    }

    private ManagerDashboardView.LowStockItem toLowStockItem(InventoryItem item) {
        return new ManagerDashboardView.LowStockItem(item.getCode(), item.getName(), item.getCurrentQuantity(),
                item.getMinimumQuantity(), item.getUnit());
    }

    private ManagerDashboardView.TopServiceItem toTopServiceItem(Object[] row) {
        Number quantity = (Number) row[1];
        BigDecimal revenue = row[2] instanceof BigDecimal value
                ? value
                : BigDecimal.valueOf(((Number) row[2]).doubleValue());
        return new ManagerDashboardView.TopServiceItem((String) row[0], quantity.longValue(), revenue);
    }

    private ManagerDashboardView.RecentTransactionItem toRecentTransactionItem(CashTransaction transaction) {
        String paymentMethod = transaction.getPaymentMethod() == null
                ? "Chưa có"
                : transaction.getPaymentMethod().getLabel();
        return new ManagerDashboardView.RecentTransactionItem(
                transaction.getId(), transaction.getDocumentCode(), transaction.getType().name(),
                transaction.getCategory().getDisplayName(), paymentMethod,
                transaction.getAmount(), transaction.getCreatedAt());
    }

    private long percentage(long value, long total) {
        return total == 0 ? 0 : Math.round(value * 100.0 / total);
    }
}
