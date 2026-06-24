package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
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

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
public class ManagerReportController {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ProfileService profileService;
    private final CashTransactionService cashTransactionService;
    private final InventoryManagementService inventoryManagementService;
    private final RoomRepository roomRepository;
    private final FolioItemRepository folioItemRepository;

    @GetMapping("/manager/reports")
    public String showReports(Model model,
                              Authentication authentication,
                              HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Báo cáo");

        LocalDate today = LocalDate.now(APP_ZONE);
        long totalRooms = roomRepository.countByIsDeletedFalse();
        long occupiedRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.OCCUPIED);
        long occupancyRate = totalRooms == 0 ? 0 : Math.round((occupiedRooms * 100.0) / totalRooms);

        model.addAttribute("todayRevenue", cashTransactionService.getIncomeForDay(today));
        model.addAttribute("monthRevenue", cashTransactionService.getIncomeForMonth(today));
        model.addAttribute("totalIncome", cashTransactionService.getTotalIncome());
        model.addAttribute("totalExpense", cashTransactionService.getTotalExpense());
        model.addAttribute("occupancyRate", occupancyRate);
        model.addAttribute("topServices", folioItemRepository.findTopServiceSales(PageRequest.of(0, 10)));
        model.addAttribute("lowStockItems", inventoryManagementService.getLowStockItems());

        return "manager/reports";
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
