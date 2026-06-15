package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.HotelAdmin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.PromotionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class PromotionController {

    private static final String LIST_VIEW = "hotel_admin/list_promotions";
    private static final String DETAIL_VIEW = "hotel_admin/view_promotion_detail";
    private static final String ADD_VIEW = "hotel_admin/add_promotion";
    private static final String EDIT_VIEW = "hotel_admin/edit_promotion";
    private static final String REDIRECT_LIST = "redirect:/hotel-admin/promotions";

    private static final String PAGE_TITLE_LIST = "KHUYẾN MÃI";
    private static final String PAGE_TITLE_DETAIL = "CHI TIẾT KHUYẾN MÃI";
    private static final String PAGE_TITLE_ADD = "THÊM KHUYẾN MÃI";
    private static final String PAGE_TITLE_EDIT = "CHỈNH SỬA KHUYẾN MÃI";

    private final PromotionService promotionService;
    private final ProfileService profileService;

    public PromotionController(PromotionService promotionService,
                               ProfileService profileService) {
        this.promotionService = promotionService;
        this.profileService = profileService;
    }

    // Hiển thị danh sách khuyến mãi, kèm lọc theo từ khóa, trạng thái và phân trang.
    @GetMapping("/hotel-admin/promotions")
    public String listPromotions(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "all") String status,
                                 Model model,
                                 Authentication authentication,
                                 HttpSession session) {
        addHeaderAttributes(model, authentication, session, PAGE_TITLE_LIST);

        PromotionListResponse response = promotionService.getPromotionListResponse(page, size, keyword, status);
        addListScreenAttributes(model, response, keyword, status);

        return LIST_VIEW;
    }

    // Mở màn hình xem chi tiết. Nếu id không hợp lệ thì quay về danh sách.
    @GetMapping("/hotel-admin/promotions/{id}")
    public String showPromotionDetail(@PathVariable Long id,
                                      Model model,
                                      Authentication authentication,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, PAGE_TITLE_DETAIL);
            model.addAttribute("promotion", promotionService.getPromotionDetail(id));
            return DETAIL_VIEW;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return REDIRECT_LIST;
        }
    }

    // Mở form thêm mới với dữ liệu rỗng ban đầu.
    @GetMapping("/hotel-admin/promotions/add")
    public String showAddPromotionForm(Model model,
                                       Authentication authentication,
                                       HttpSession session) {
        addHeaderAttributes(model, authentication, session, PAGE_TITLE_ADD);
        model.addAttribute("promotion", new PromotionRequest());
        return ADD_VIEW;
    }

    // Mở form chỉnh sửa và đổ dữ liệu hiện tại của khuyến mãi lên giao diện.
    @GetMapping("/hotel-admin/promotions/edit/{id}")
    public String showEditPromotionForm(@PathVariable Long id,
                                        Model model,
                                        Authentication authentication,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, PAGE_TITLE_EDIT);
            model.addAttribute("promotion", promotionService.getPromotionEditRequest(id));
            model.addAttribute("promotionId", id);
            return EDIT_VIEW;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return REDIRECT_LIST;
        }
    }

    // Upload ảnh riêng để form add/edit xem trước ảnh trước khi lưu khuyến mãi.
    @PostMapping("/hotel-admin/promotion-images/upload")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadPromotionImage(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, String> result = promotionService.uploadPromotionImage(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Tải ảnh khuyến mãi thất bại. Vui lòng thử lại."));
        }
    }

    // Controller nhận request, chạy bean validation cơ bản, rồi mới chuyển sang service xử lý nghiệp vụ.
    @PostMapping("/hotel-admin/promotions/add")
    public String addPromotion(@Valid @ModelAttribute("promotion") PromotionRequest request,
                               BindingResult bindingResult,
                               Model model,
                               Authentication authentication,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addHeaderAttributes(model, authentication, session, PAGE_TITLE_ADD);
            model.addAttribute("errorMessage", getFirstValidationMessage(bindingResult));
            model.addAttribute("promotion", request);
            return ADD_VIEW;
        }

        try {
            promotionService.createPromotion(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm khuyến mãi thành công.");
            return REDIRECT_LIST;
        } catch (IllegalArgumentException e) {
            addHeaderAttributes(model, authentication, session, PAGE_TITLE_ADD);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("promotion", request);
            return ADD_VIEW;
        }
    }

    // Controller nhận request, chạy bean validation cơ bản, rồi mới chuyển sang service xử lý nghiệp vụ.
    @PostMapping("/hotel-admin/promotions/edit/{id}")
    public String editPromotion(@PathVariable Long id,
                                @Valid @ModelAttribute("promotion") PromotionRequest request,
                                BindingResult bindingResult,
                                Model model,
                                Authentication authentication,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addHeaderAttributes(model, authentication, session, PAGE_TITLE_EDIT);
            model.addAttribute("errorMessage", getFirstValidationMessage(bindingResult));
            model.addAttribute("promotion", request);
            model.addAttribute("promotionId", id);
            return EDIT_VIEW;
        }

        try {
            promotionService.updatePromotion(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khuyến mãi thành công.");
            return "redirect:/hotel-admin/promotions/" + id;
        } catch (IllegalArgumentException e) {
            addHeaderAttributes(model, authentication, session, PAGE_TITLE_EDIT);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("promotion", request);
            model.addAttribute("promotionId", id);
            return EDIT_VIEW;
        }
    }

    // Xóa khuyến mãi nếu service xác nhận khuyến mãi đó chưa được dùng trong booking.
    @PostMapping("/hotel-admin/promotions/delete/{id}")
    public String deletePromotion(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa khuyến mãi thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return REDIRECT_LIST;
    }

    // Gắn thông tin chung cho header của các màn promotion.
    // Bật hoặc tắt trạng thái hoạt động của khuyến mãi ngay trên màn danh sách.
    @PostMapping("/hotel-admin/promotions/toggle-status/{id}")
    public String togglePromotionStatus(@PathVariable Long id,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "5") int size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(defaultValue = "all") String status,
                                        RedirectAttributes redirectAttributes) {
        try {
            boolean isActive = promotionService.togglePromotionStatus(id);

            if (isActive) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã bật khuyến mãi thành công.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã tắt khuyến mãi thành công.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        String redirectUrl = String.format(
                "redirect:/hotel-admin/promotions?page=%d&size=%d&status=%s",
                page,
                size,
                status == null || status.isBlank() ? "all" : status
        );

        if (keyword != null && !keyword.isBlank()) {
            redirectUrl += "&keyword=" + keyword.trim();
        }

        return redirectUrl;
    }

    private void addHeaderAttributes(Model model,
                                     Authentication authentication,
                                     HttpSession session,
                                     String pageTitle) {
        User currentUser = profileService.resolveCurrentUser(authentication, session);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentUser", currentUser);
    }

    // Gom phần dữ liệu của màn danh sách vào một chỗ để method list ngắn và dễ đọc hơn.
    private void addListScreenAttributes(Model model,
                                         PromotionListResponse response,
                                         String keyword,
                                         String status) {
        model.addAttribute("promotions", response.getPromotions());
        model.addAttribute("totalPromotions", response.getTotalPromotions());
        model.addAttribute("activePromotions", response.getActivePromotions());
        model.addAttribute("scheduledPromotions", response.getScheduledPromotions());
        model.addAttribute("expiredPromotions", response.getExpiredPromotions());
        model.addAttribute("inactivePromotions", response.getInactivePromotions());

        model.addAttribute("currentPage", response.getCurrentPage());
        model.addAttribute("totalPages", response.getTotalPages());
        model.addAttribute("pageSize", response.getPageSize());
        model.addAttribute("hasPrevious", response.isHasPrevious());
        model.addAttribute("hasNext", response.isHasNext());

        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("selectedStatus", status == null || status.isBlank() ? "all" : status);
    }

    // Lấy lỗi bean validation đầu tiên để tiếp tục hiển thị lại theo cách màn hình hiện tại đang dùng.
    private String getFirstValidationMessage(BindingResult bindingResult) {
        if (bindingResult.getFieldError() != null) {
            return bindingResult.getFieldError().getDefaultMessage();
        }

        if (bindingResult.getGlobalError() != null) {
            return bindingResult.getGlobalError().getDefaultMessage();
        }

        return "Dữ liệu không hợp lệ.";
    }
}
