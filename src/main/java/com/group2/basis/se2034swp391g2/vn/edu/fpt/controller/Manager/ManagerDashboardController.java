package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CashTransactionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.InventoryManagementService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class ManagerDashboardController {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ProfileService profileService;
    private final CashTransactionService cashTransactionService;
    private final InventoryManagementService inventoryManagementService;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final FolioItemRepository folioItemRepository;

    @GetMapping("/manager/dashboard")
    public String showDashboard(Model model,
                                Authentication authentication,
                                HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Tổng quan");

        LocalDate today = LocalDate.now(APP_ZONE);
        Instant startOfDay = today.atStartOfDay(APP_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant();

        long totalRooms = roomRepository.countByIsDeletedFalse();
        long occupiedRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.OCCUPIED);
        long availableRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.AVAILABLE);
        long maintenanceRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.MAINTENANCE);
        long occupancyRate = totalRooms == 0 ? 0 : Math.round((occupiedRooms * 100.0) / totalRooms);
        BigDecimal monthRevenue = cashTransactionService.getIncomeForMonth(today);
        BigDecimal totalIncome = cashTransactionService.getTotalIncome();
        BigDecimal totalExpense = cashTransactionService.getTotalExpense();

        model.addAttribute("todayRevenue", cashTransactionService.getIncomeForDay(today));
        model.addAttribute("today", today);
        model.addAttribute("monthRevenue", monthRevenue);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("netCashFlow", totalIncome.subtract(totalExpense));
        model.addAttribute("newBookingsToday", bookingRepository.countByCreatedAtBetween(startOfDay, endOfDay));
        model.addAttribute("occupiedRooms", occupiedRooms);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("maintenanceRooms", maintenanceRooms);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("occupancyRate", occupancyRate);
        model.addAttribute("checkedInBookings",
                bookingRepository.countByStatusAndIsDeletedFalseAndCheckInDateLessThanEqualAndCheckOutDateAfter(
                        BookingStatus.CHECKED_IN, today, today));
        model.addAttribute("todayCheckIns",
                bookingRepository.countByStatusAndCheckInDateAndIsDeletedFalse(BookingStatus.CONFIRMED, today));
        model.addAttribute("todayCheckOuts",
                bookingRepository.countByStatusAndCheckOutDateAndIsDeletedFalse(BookingStatus.CHECKED_IN, today));
        model.addAttribute("topServices", folioItemRepository.findTopServiceSales(PageRequest.of(0, 5)));
        model.addAttribute("recentTransactions", cashTransactionService.getRecentTransactions(5));
        model.addAttribute("lowStockCount", inventoryManagementService.countLowStockItems());
        model.addAttribute("lowStockItems", inventoryManagementService.getLowStockItems().stream().limit(5).toList());

        return "manager/Dashboard";
    }

    private void addHeaderAttributes(Model model,
                                     Authentication authentication,
                                     HttpSession session,
                                     String pageTitle) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentUser", resolveCurrentUser(authentication, session));
    }

    private User resolveCurrentUser(Authentication authentication, HttpSession session) {
        return profileService.resolveCurrentUser(authentication, session);
    }
}
