package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.FolioAdjustmentRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.FolioDetailResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.FolioListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.FolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
@Controller
@RequiredArgsConstructor
@RequestMapping("/receptionist/folios")
public class FolioController {

    private final FolioService folioService;

    @GetMapping
    public String listFolios(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String paymentStatus,
                             @RequestParam(required = false)
                             @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate checkIn,
                             @RequestParam(required = false)
                             @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate checkOut,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, 10);
        Page<FolioListResponse> folioPage;
        try {
            folioPage = folioService.searchFolios(keyword, null, paymentStatus, checkIn, checkOut, pageable);
        } catch (IllegalArgumentException e) {
            folioPage = Page.empty(pageable);
            model.addAttribute("errorMessage", e.getMessage());
        }

        model.addAttribute("pageTitle", "Quản lý hoá đơn");
        model.addAttribute("folioPage", folioPage);
        model.addAttribute("folios", folioPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", folioPage.getTotalPages());
        model.addAttribute("totalItems", folioPage.getTotalElements());
        List<Integer> visiblePages = buildVisiblePages(page, folioPage.getTotalPages());
        model.addAttribute("visiblePages", visiblePages);
        model.addAttribute("showLeadingEllipsis", !visiblePages.isEmpty() && visiblePages.getFirst() > 0);
        model.addAttribute("showTrailingEllipsis", !visiblePages.isEmpty() && visiblePages.getLast() < folioPage.getTotalPages() - 1);

        return "receptionist/ListFolio";
    }

    private List<Integer> buildVisiblePages(int currentPage, int totalPages) {
        if (totalPages <= 0) {
            return List.of();
        }

        if (totalPages <= 5) {
            return IntStream.range(0, totalPages).boxed().toList();
        }

        int start;
        int end;

        if (currentPage <= 2) {
            start = 0;
            end = 3;
        } else if (currentPage >= totalPages - 3) {
            start = totalPages - 4;
            end = totalPages - 1;
        } else {
            start = currentPage - 1;
            end = currentPage + 2;
        }

        return IntStream.rangeClosed(start, end).boxed().toList();
    }

    @GetMapping("/{bookingId}")
    public String viewFolio(@PathVariable Long bookingId,
                            @RequestParam(required = false) Long bookingDetailId,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pageTitle", "Chi tiết hoá đơn");
            model.addAttribute("folio", folioService.getFolioDetail(bookingId, bookingDetailId));
            return "receptionist/ViewFolio";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/receptionist/folios";
        }
    }

    @GetMapping("/{bookingId}/edit")
    public String editFolio(@PathVariable Long bookingId,
                            @RequestParam(required = false) Long bookingDetailId,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            FolioDetailResponse folio = folioService.getFolioDetail(bookingId, bookingDetailId);
            FolioAdjustmentRequest request = new FolioAdjustmentRequest();
            request.setBookingDetailId(bookingDetailId);
            model.addAttribute("pageTitle", "Chỉnh sửa hoá đơn");
            model.addAttribute("folio", folio);
            model.addAttribute("request", request);
            return "receptionist/EditFolio";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/receptionist/folios";
        }
    }

    @PostMapping("/{bookingId}/adjustments")
    public String addAdjustment(@PathVariable Long bookingId,
                                @ModelAttribute("request") FolioAdjustmentRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            folioService.addAdjustment(bookingId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm điều chỉnh vào hoá đơn.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataAccessException | TransactionException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật hoá đơn do lỗi ghi dữ liệu. Vui lòng thử lại.");
        }

        if (request != null && request.getBookingDetailId() != null) {
            return "redirect:/receptionist/folios/" + bookingId + "/edit?bookingDetailId=" + request.getBookingDetailId();
        }
        return "redirect:/receptionist/folios/" + bookingId + "/edit";
    }

    @PostMapping("/{bookingId}/items/{folioItemId}/void")
    public String voidAdjustment(@PathVariable Long bookingId,
                                 @PathVariable Long folioItemId,
                                 @RequestParam(required = false) Long bookingDetailId,
                                 @RequestParam(required = false) String voidedReason,
                                 RedirectAttributes redirectAttributes) {
        try {
            folioService.voidAdjustment(bookingId, bookingDetailId, folioItemId, voidedReason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã huỷ dòng điều chỉnh.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataAccessException | TransactionException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể huỷ dòng hoá đơn do lỗi ghi dữ liệu. Vui lòng thử lại.");
        }

        if (bookingDetailId != null) {
            return "redirect:/receptionist/folios/" + bookingId + "/edit?bookingDetailId=" + bookingDetailId;
        }
        return "redirect:/receptionist/folios/" + bookingId + "/edit";
    }
}
