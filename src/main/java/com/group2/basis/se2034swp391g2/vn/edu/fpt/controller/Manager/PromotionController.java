package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.PromotionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CloudinaryService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Controller
public class PromotionController {

    private final PromotionService promotionService;
    private final CloudinaryService cloudinaryService;

    public PromotionController(PromotionService promotionService,
                               CloudinaryService cloudinaryService) {
        this.promotionService = promotionService;
        this.cloudinaryService = cloudinaryService;
    }

    /*
     * Hiển thị danh sách chiến dịch khuyến mãi cho Manager.
     */
    @GetMapping("/manager/promotions")
    public String listPromotions(Model model) {
        PromotionListResponse response = promotionService.getPromotionListResponse();

        model.addAttribute("promotions", response.getPromotions());
        model.addAttribute("totalPromotions", response.getTotalPromotions());
        model.addAttribute("activePromotions", response.getActivePromotions());
        model.addAttribute("scheduledPromotions", response.getScheduledPromotions());
        model.addAttribute("expiredPromotions", response.getExpiredPromotions());
        model.addAttribute("inactivePromotions", response.getInactivePromotions());

        return "manager/list_promotions";
    }

    /*
     * Hiển thị form thêm chiến dịch khuyến mãi.
     */
    @GetMapping("/manager/promotions/add")
    public String showAddPromotionForm(Model model) {
        model.addAttribute("promotion", new PromotionRequest());

        return "manager/add_promotion";
    }

    /*
     * Upload ảnh khuyến mãi lên Cloudinary trước khi lưu form.
     * Cách này giống flow upload ảnh phòng: ảnh được upload riêng,
     * sau đó form chỉ lưu imageUrl và imagePublicId.
     */
    @PostMapping("/manager/promotion-images/upload")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadPromotionImage(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("===== PROMOTION IMAGE UPLOAD CONTROLLER CALLED =====");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("Content type: " + file.getContentType());
            System.out.println("File size: " + file.getSize());

            Map uploadResult = cloudinaryService.uploadPromotionImage(file);

            System.out.println("Upload success: " + uploadResult.get("secure_url"));

            return ResponseEntity.ok(Map.of(
                    "imageUrl", uploadResult.get("secure_url").toString(),
                    "imagePublicId", uploadResult.get("public_id").toString()
            ));

        } catch (IllegalArgumentException e) {
            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Tải ảnh khuyến mãi thất bại: " + e.getMessage()));
        }
    }

    /*
     * Xử lý lưu chiến dịch khuyến mãi mới.
     * Controller chỉ gọi service, validate nghiệp vụ nằm trong service.
     */
    @PostMapping("/manager/promotions/add")
    public String addPromotion(@ModelAttribute("promotion") PromotionRequest request,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            promotionService.createPromotion(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Thêm khuyến mãi thành công."
            );

            return "redirect:/manager/promotions";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("promotion", request);

            return "manager/add_promotion";
        }
    }
}