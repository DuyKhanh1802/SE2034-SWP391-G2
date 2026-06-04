package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DiscountType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.PromotionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

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

    @GetMapping("/manager/promotions/add")
    public String showAddPromotionForm(Model model) {
        model.addAttribute("promotion", new PromotionRequest());
        model.addAttribute("discountTypes", DiscountType.values());

        return "manager/add_promotion";
    }

    @PostMapping("/manager/promotions/add")
    public String addPromotion(@ModelAttribute("promotion") PromotionRequest request,
                               Authentication authentication,
                               Model model) {
        try {
            promotionService.createPromotion(request, authentication);
            return "redirect:/manager/promotions";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("promotion", request);
            model.addAttribute("discountTypes", DiscountType.values());

            return "manager/add_promotion";
        }
    }
}