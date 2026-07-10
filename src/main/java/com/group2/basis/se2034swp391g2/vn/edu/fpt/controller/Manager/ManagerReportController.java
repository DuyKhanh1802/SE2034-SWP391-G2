package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.OccupancyReportRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.OccupancyReportResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceReportRowResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceReportSummaryResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.ServiceCategoryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CashTransactionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.InventoryManagementService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ManagerOccupancyReportService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ManagerServiceReportService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ManagerReportController {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int SERVICE_REPORT_PAGE_SIZE = 5;

    private final ProfileService profileService;
    private final CashTransactionService cashTransactionService;
    private final InventoryManagementService inventoryManagementService;
    private final FolioItemRepository folioItemRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ManagerServiceReportService managerServiceReportService;
    private final ManagerOccupancyReportService managerOccupancyReportService;

    @GetMapping("/manager/reports")
    public String showReports(Model model,
                              Authentication authentication,
                              HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Báo cáo");

        LocalDate today = LocalDate.now(APP_ZONE);

        OccupancyReportResponse occupancyReport = managerOccupancyReportService.getOccupancyReport(null, null);

        model.addAttribute("todayRevenue", cashTransactionService.getIncomeForDay(today));
        model.addAttribute("monthRevenue", cashTransactionService.getIncomeForMonth(today));
        model.addAttribute("totalIncome", cashTransactionService.getTotalIncome());
        model.addAttribute("totalExpense", cashTransactionService.getTotalExpense());
        model.addAttribute("occupancyRate", occupancyReport.getSummary().getOccupancyRate());
        model.addAttribute("topServices", folioItemRepository.findTopServiceSales(PageRequest.of(0, 5)));
        model.addAttribute("lowStockItems", inventoryManagementService.getLowStockItems());

        return "manager/reports";
    }

    @GetMapping("/manager/reports/services")
    public String showServiceReport(@RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                    LocalDate fromDate,

                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                    LocalDate toDate,

                                    @RequestParam(required = false)
                                    Long categoryId,

                                    @RequestParam(required = false)
                                    String keyword,

                                    @RequestParam(required = false, defaultValue = "revenueDesc")
                                    String sortBy,

                                    @RequestParam(required = false, defaultValue = "0")
                                    int page,

                                    Model model,
                                    Authentication authentication,
                                    HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Báo cáo dịch vụ");

        LocalDate resolvedFromDate = managerServiceReportService.resolveFromDate(fromDate);
        LocalDate resolvedToDate = managerServiceReportService.resolveToDate(toDate);

        if (resolvedFromDate.isAfter(resolvedToDate)) {
            model.addAttribute("errorMessage", "Ngày bắt đầu không được sau ngày kết thúc.");
            resolvedFromDate = LocalDate.now(APP_ZONE).withDayOfMonth(1);
            resolvedToDate = LocalDate.now(APP_ZONE);
        }

        List<ServiceReportRowResponse> allRows = managerServiceReportService.getServiceReportRows(
                resolvedFromDate,
                resolvedToDate,
                categoryId,
                keyword,
                sortBy
        );

        ServiceReportSummaryResponse summary = managerServiceReportService.buildSummary(allRows);

        int totalItems = allRows.size();
        int totalPages = totalItems == 0
                ? 1
                : (int) Math.ceil((double) totalItems / SERVICE_REPORT_PAGE_SIZE);

        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = currentPage * SERVICE_REPORT_PAGE_SIZE;
        int endIndex = Math.min(startIndex + SERVICE_REPORT_PAGE_SIZE, totalItems);

        List<ServiceReportRowResponse> pageRows = totalItems == 0
                ? List.of()
                : allRows.subList(startIndex, endIndex);

        model.addAttribute("rows", pageRows);
        model.addAttribute("summary", summary);
        model.addAttribute("categories", serviceCategoryRepository.findByIsDeletedFalseOrderByNameAsc());

        model.addAttribute("fromDate", resolvedFromDate);
        model.addAttribute("toDate", resolvedToDate);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageSize", SERVICE_REPORT_PAGE_SIZE);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startItem", totalItems == 0 ? 0 : startIndex + 1);
        model.addAttribute("endItem", endIndex);

        return "manager/service-report";
    }

    @GetMapping("/manager/reports/occupancy")
    public String showOccupancyReport(@ModelAttribute OccupancyReportRequest request,
                                      Model model,
                                      Authentication authentication,
                                      HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Báo cáo công suất");

        OccupancyReportResponse report = managerOccupancyReportService.getOccupancyReport(
                request.getFromDate(),
                request.getToDate(),
                request.getVariantId(),
                request.getSafePage()
        );

        model.addAttribute("report", report);
        model.addAttribute("fromDate", request.getFromDate());
        model.addAttribute("toDate", request.getToDate());
        model.addAttribute("selectedVariantId", request.getVariantId());
        model.addAttribute("roomTypeVariants", managerOccupancyReportService.getRoomTypeVariantsForFilter());

        return "manager/occupancy-report";
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
