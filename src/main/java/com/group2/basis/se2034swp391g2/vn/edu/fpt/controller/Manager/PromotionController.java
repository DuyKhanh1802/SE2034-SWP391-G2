package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.PromotionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class PromotionController {

    private final PromotionService promotionService;
    private final ProfileService profileService;

    public PromotionController(PromotionService promotionService,
                               ProfileService profileService) {
        this.promotionService = promotionService;
        this.profileService = profileService;
    }

    /*
     * Hiển thị danh sách khuyến mãi có phân trang, tìm kiếm và lọc trạng thái.
     */
    @GetMapping("/manager/promotions")
    public String listPromotions(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "all") String status,
                                 Model model,
                                 Authentication authentication,
                                 HttpSession session) {
        addHeaderAttributes(model, authentication, session, "KHUYẾN MÃI");

        PromotionListResponse response =
                promotionService.getPromotionListResponse(page, size, keyword, status);

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
        model.addAttribute("selectedStatus", status);

        return "manager/list_promotions";
    }

    /*
     * Hiển thị form thêm khuyến mãi.
     */
    @GetMapping("/manager/promotions/add")
    public String showAddPromotionForm(Model model,
                                       Authentication authentication,
                                       HttpSession session) {
        addHeaderAttributes(model, authentication, session, "THÊM KHUYẾN MÃI");
        model.addAttribute("promotion", new PromotionRequest());

        return "manager/add_promotion";
    }

    /*
     * Upload ảnh khuyến mãi lên Cloudinary.
     */
    @PostMapping("/manager/promotion-images/upload")
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

    /*
     * Xử lý thêm khuyến mãi mới.
     */
    @PostMapping("/manager/promotions/add")
    public String addPromotion(@ModelAttribute("promotion") PromotionRequest request,
                               Model model,
                               Authentication authentication,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            promotionService.createPromotion(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Thêm khuyến mãi thành công."
            );

            return "redirect:/manager/promotions";

        } catch (IllegalArgumentException e) {
            addHeaderAttributes(model, authentication, session, "THÊM KHUYẾN MÃI");

            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("promotion", request);

            return "manager/add_promotion";
        }
    }

    /*
     * Truyền dữ liệu cho header dùng chung của staff.
     */
    private void addHeaderAttributes(Model model,
                                     Authentication authentication,
                                     HttpSession session,
                                     String pageTitle) {
        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentUser", currentUser);
    }
}