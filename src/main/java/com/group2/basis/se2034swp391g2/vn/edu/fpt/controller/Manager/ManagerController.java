package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CashTransactionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.HotelFundService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.InventoryManagementService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
public class ManagerController {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ProfileService profileService;
    private final CashTransactionService cashTransactionService;
    private final HotelFundService hotelFundService;
    private final InventoryManagementService inventoryManagementService;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final FolioItemRepository folioItemRepository;

    @GetMapping("/manager/dashboard")
    public String dashboard(Model model,
                            Authentication authentication,
                            HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Tổng quan");

        LocalDate today = LocalDate.now(APP_ZONE);
        Instant startOfDay = today.atStartOfDay(APP_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant();

        long totalRooms = roomRepository.countByIsDeletedFalse();
        long occupiedRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.OCCUPIED);

        model.addAttribute("todayRevenue", cashTransactionService.getIncomeForDay(today));
        model.addAttribute("monthRevenue", cashTransactionService.getIncomeForMonth(today));
        model.addAttribute("newBookingsToday", bookingRepository.countByCreatedAtBetween(startOfDay, endOfDay));
        model.addAttribute("occupiedRooms", occupiedRooms);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("occupancyRate", totalRooms == 0 ? 0 : Math.round((occupiedRooms * 100.0) / totalRooms));
        model.addAttribute("checkedInBookings",
                bookingRepository.countByStatusAndIsDeletedFalseAndCheckInDateLessThanEqualAndCheckOutDateAfter(
                        BookingStatus.CHECKED_IN, today, today));
        model.addAttribute("topServices", folioItemRepository.findTopServiceSales(PageRequest.of(0, 5)));
        model.addAttribute("recentTransactions", cashTransactionService.getRecentTransactions(5));
        model.addAttribute("lowStockCount", inventoryManagementService.countLowStockItems());
        model.addAttribute("lowStockItems", inventoryManagementService.getLowStockItems());
        return "manager/Dashboard";
    }

    @GetMapping("/manager/transactions")
    public String transactions(@RequestParam(defaultValue = "ALL") String type,
                               @RequestParam(required = false) String keyword,
                               Model model,
                               Authentication authentication,
                               HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Lịch sử giao dịch");
        model.addAttribute("transactions", cashTransactionService.searchTransactions(type, keyword));
        model.addAttribute("selectedType", type);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "manager/transactions";
    }

    @GetMapping("/manager/transactions/{id}")
    public String transactionDetail(@PathVariable Long id,
                                    Model model,
                                    Authentication authentication,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "Chi tiết dòng tiền");
            model.addAttribute("transaction", cashTransactionService.getTransaction(id));
            return "manager/transaction_detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/transactions";
        }
    }

    @GetMapping("/manager/fund")
    public String fund(Model model,
                       Authentication authentication,
                       HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Quản lý quỹ");
        model.addAttribute("fundSetting", hotelFundService.getCurrentSetting());
        model.addAttribute("openingBalance", hotelFundService.getOpeningBalance());
        model.addAttribute("totalIncome", cashTransactionService.getTotalIncome());
        model.addAttribute("totalExpense", cashTransactionService.getTotalExpense());
        model.addAttribute("currentBalance", hotelFundService.getCurrentBalance());
        model.addAttribute("recentTransactions", cashTransactionService.getRecentTransactions(10));
        return "manager/fund";
    }

    @PostMapping("/manager/fund/opening-balance")
    public String configureOpeningBalance(@RequestParam BigDecimal openingBalance,
                                          Authentication authentication,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        try {
            hotelFundService.configureOpeningBalance(openingBalance, resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage", "Đã cấu hình vốn đầu kỳ.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/fund";
    }

    @PostMapping("/manager/fund/manual-transaction")
    public String createManualTransaction(@RequestParam CashTransactionType type,
                                          @RequestParam BigDecimal amount,
                                          @RequestParam(required = false) String description,
                                          Authentication authentication,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        try {
            CashTransactionCategory category = type == CashTransactionType.INCOME
                    ? CashTransactionCategory.MANUAL_INCOME
                    : CashTransactionCategory.MANUAL_EXPENSE;
            cashTransactionService.createManualTransaction(
                    type, category, amount, description, resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage", "Đã lập phiếu thu/chi thủ công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/fund";
    }

    @GetMapping("/manager/inventory")
    public String inventory(Model model,
                            Authentication authentication,
                            HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Quản lý kho hàng");
        model.addAttribute("items", inventoryManagementService.getItems());
        model.addAttribute("services", inventoryManagementService.getAvailableServices());
        model.addAttribute("roomTypes", inventoryManagementService.getRoomTypes());
        model.addAttribute("recentTransactions", inventoryManagementService.getRecentTransactions());
        model.addAttribute("recentReceipts", inventoryManagementService.getRecentReceipts());
        return "manager/inventory";
    }

    @PostMapping("/manager/inventory/items")
    public String createInventoryItem(@RequestParam String name,
                                      @RequestParam(required = false) String category,
                                      @RequestParam String unit,
                                      @RequestParam(required = false) BigDecimal openingQuantity,
                                      @RequestParam(required = false) BigDecimal minimumQuantity,
                                      RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.createItem(name, category, unit, openingQuantity, minimumQuantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm hàng hóa.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/inventory";
    }

    @PostMapping("/manager/inventory/receipts")
    public String createInventoryReceipt(@RequestParam Long itemId,
                                         @RequestParam BigDecimal quantity,
                                         @RequestParam BigDecimal unitCost,
                                         @RequestParam(required = false) String supplier,
                                         @RequestParam(required = false) String note,
                                         Authentication authentication,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.createReceipt(
                    itemId, quantity, unitCost, supplier, note, resolveCurrentUser(authentication, session));
            redirectAttributes.addFlashAttribute("successMessage", "Đã lập phiếu nhập hàng và ghi nhận chi quỹ.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/inventory";
    }

    @PostMapping("/manager/inventory/service-mappings")
    public String linkInventoryToService(@RequestParam Long serviceId,
                                         @RequestParam Long itemId,
                                         @RequestParam BigDecimal quantityPerUse,
                                         RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.linkItemToService(serviceId, itemId, quantityPerUse);
            redirectAttributes.addFlashAttribute("successMessage", "Đã liên kết hàng hóa với dịch vụ.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/inventory/" + itemId;
    }

    @PostMapping("/manager/inventory/room-refresh-mappings")
    public String linkInventoryToRoomRefresh(@RequestParam Long roomTypeId,
                                             @RequestParam Long itemId,
                                             @RequestParam BigDecimal quantityPerRefresh,
                                             RedirectAttributes redirectAttributes) {
        try {
            inventoryManagementService.linkItemToRoomRefresh(roomTypeId, itemId, quantityPerRefresh);
            redirectAttributes.addFlashAttribute("successMessage", "Đã liên kết hàng hóa với refresh phòng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/inventory/" + itemId;
    }

    @GetMapping("/manager/inventory/{id}")
    public String inventoryDetail(@PathVariable Long id,
                                  Model model,
                                  Authentication authentication,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "Chi tiết hàng hóa");
            model.addAttribute("item", inventoryManagementService.getItem(id));
            model.addAttribute("totalIn", inventoryManagementService.getItemTotalIn(id));
            model.addAttribute("totalOut", inventoryManagementService.getItemTotalOut(id));
            model.addAttribute("mappings", inventoryManagementService.getMappingsForItem(id));
            model.addAttribute("refreshMappings", inventoryManagementService.getRefreshMappingsForItem(id));
            model.addAttribute("transactions", inventoryManagementService.getTransactionsForItem(id));
            model.addAttribute("services", inventoryManagementService.getAvailableServices());
            model.addAttribute("roomTypes", inventoryManagementService.getRoomTypes());
            return "manager/inventory_detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/inventory";
        }
    }

    @GetMapping("/manager/reports")
    public String reports(Model model,
                          Authentication authentication,
                          HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Báo cáo");
        LocalDate today = LocalDate.now(APP_ZONE);
        long totalRooms = roomRepository.countByIsDeletedFalse();
        long occupiedRooms = roomRepository.countByStatusAndIsDeletedFalse(RoomStatus.OCCUPIED);

        model.addAttribute("todayRevenue", cashTransactionService.getIncomeForDay(today));
        model.addAttribute("monthRevenue", cashTransactionService.getIncomeForMonth(today));
        model.addAttribute("totalIncome", cashTransactionService.getTotalIncome());
        model.addAttribute("totalExpense", cashTransactionService.getTotalExpense());
        model.addAttribute("occupancyRate", totalRooms == 0 ? 0 : Math.round((occupiedRooms * 100.0) / totalRooms));
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
