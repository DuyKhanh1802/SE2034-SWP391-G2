package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DiscountType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Promotion;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
public class PromotionController {

    // TODO: Tạm thời dùng manager id = 4 để test chức năng CRUD Promotion.
    // Sau khi module User/Login được tích hợp, createdBy sẽ được lấy từ manager đang đăng nhập.
    private static final Long TEMP_MANAGER_ID = 4L;

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/manager/promotions")
    public String listPromotions(Model model) {
        List<Promotion> promotions = promotionService.getAllPromotions();
        Instant now = Instant.now();

        long totalPromotions = promotions.size();

        long inactivePromotions = promotions.stream()
                .filter(promotion -> Boolean.FALSE.equals(promotion.getIsActive()))
                .count();

        long scheduledPromotions = promotions.stream()
                .filter(promotion -> Boolean.TRUE.equals(promotion.getIsActive()))
                .filter(promotion -> promotion.getValidFrom() != null
                        && promotion.getValidFrom().isAfter(now))
                .count();

        long expiredPromotions = promotions.stream()
                .filter(promotion -> Boolean.TRUE.equals(promotion.getIsActive()))
                .filter(promotion -> promotion.getValidTo() != null
                        && promotion.getValidTo().isBefore(now))
                .count();

        long activePromotions = promotions.stream()
                .filter(promotion -> Boolean.TRUE.equals(promotion.getIsActive()))
                .filter(promotion -> promotion.getValidFrom() == null
                        || !promotion.getValidFrom().isAfter(now))
                .filter(promotion -> promotion.getValidTo() == null
                        || !promotion.getValidTo().isBefore(now))
                .count();

        model.addAttribute("promotions", promotions);
        model.addAttribute("totalPromotions", totalPromotions);
        model.addAttribute("activePromotions", activePromotions);
        model.addAttribute("scheduledPromotions", scheduledPromotions);
        model.addAttribute("expiredPromotions", expiredPromotions);
        model.addAttribute("inactivePromotions", inactivePromotions);

        return "manager/list_promotions";
    }

    @GetMapping("/manager/promotions/{id}")
    public String viewPromotionDetail(@PathVariable Long id, Model model) {
        Promotion promotion = promotionService.getPromotionById(id);

        model.addAttribute("promotion", promotion);

        return "manager/promotion_detail";
    }

    @GetMapping("/manager/promotions/add")
    public String showAddPromotionForm(Model model) {
        Promotion promotion = new Promotion();
        promotion.setIsActive(true);
        promotion.setUsageCount(0);

        model.addAttribute("promotion", promotion);
        model.addAttribute("discountTypes", DiscountType.values());

        return "manager/add_promotion";
    }

    @PostMapping("/manager/promotions/add")
    public String addPromotion(@ModelAttribute("promotion") Promotion promotion,
                               @RequestParam(value = "validFromInput", required = false) String validFromInput,
                               @RequestParam(value = "validToInput", required = false) String validToInput,
                               Model model) {

        try {
            if (validFromInput == null || validFromInput.isBlank()) {
                throw new IllegalArgumentException("Valid from is required.");
            }

            if (validToInput == null || validToInput.isBlank()) {
                throw new IllegalArgumentException("Valid to is required.");
            }

            promotion.setValidFrom(convertToInstant(validFromInput));
            promotion.setValidTo(convertToInstant(validToInput));

            if (promotion.getUsageCount() == null) {
                promotion.setUsageCount(0);
            }

            if (promotion.getIsActive() == null) {
                promotion.setIsActive(false);
            }

            User createdBy = new User();
            createdBy.setId(TEMP_MANAGER_ID);
            promotion.setCreatedBy(createdBy);

            promotion.setCreatedAt(Instant.now());

            promotionService.savePromotion(promotion);

            return "redirect:/manager/promotions";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("promotion", promotion);
            model.addAttribute("discountTypes", DiscountType.values());
            return "manager/add_promotion";
        }
    }

    private Instant convertToInstant(String dateTimeInput) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeInput);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date time format.");
        }
    }
}