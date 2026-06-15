package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.HotelAdmin;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class PromotionController_old {

    private final PromotionService promotionService;
    private final ProfileService profileService;

    public PromotionController_old(PromotionService promotionService,
                                   ProfileService profileService) {
        this.promotionService = promotionService;
        this.profileService = profileService;
    }

    @GetMapping("/hotel-admin/promotions")
    public String listPromotions(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "all") String status,
                                 Model model,
                                 Authentication authentication,
                                 HttpSession session) {
        addHeaderAttributes(model, authentication, session, "KHUYáº¾N MÃƒI");

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

        return "list_promotions_old";
    }

    @GetMapping("/hotel-admin/promotions/{id}")
    public String showPromotionDetail(@PathVariable Long id,
                                      Model model,
                                      Authentication authentication,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "CHI TIáº¾T KHUYáº¾N MÃƒI");
            model.addAttribute("promotion", promotionService.getPromotionDetail(id));

            return "promotion_detail_old";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/hotel-admin/promotions";
        }
    }

    @GetMapping("/hotel-admin/promotions/add")
    public String showAddPromotionForm(Model model,
                                       Authentication authentication,
                                       HttpSession session) {
        addHeaderAttributes(model, authentication, session, "THÃŠM KHUYáº¾N MÃƒI");
        model.addAttribute("promotion", new PromotionRequest());

        return "add_promotion_old";
    }

    @GetMapping("/hotel-admin/promotions/edit/{id}")
    public String showEditPromotionForm(@PathVariable Long id,
                                        Model model,
                                        Authentication authentication,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "CHá»ˆNH Sá»¬A KHUYáº¾N MÃƒI");
            model.addAttribute("promotion", promotionService.getPromotionEditRequest(id));
            model.addAttribute("promotionId", id);

            return "edit_promotion_old";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/hotel-admin/promotions";
        }
    }

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
                    .body(Map.of("error", "Táº£i áº£nh khuyáº¿n mÃ£i tháº¥t báº¡i. Vui lÃ²ng thá»­ láº¡i."));
        }
    }

    @PostMapping("/hotel-admin/promotions/add")
    public String addPromotion(@ModelAttribute("promotion") PromotionRequest request,
                               Model model,
                               Authentication authentication,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            promotionService.createPromotion(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "ThÃªm khuyáº¿n mÃ£i thÃ nh cÃ´ng."
            );

            return "redirect:/hotel-admin/promotions";

        } catch (IllegalArgumentException e) {
            addHeaderAttributes(model, authentication, session, "THÃŠM KHUYáº¾N MÃƒI");
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("promotion", request);

            return "add_promotion_old";
        }
    }

    @PostMapping("/hotel-admin/promotions/edit/{id}")
    public String editPromotion(@PathVariable Long id,
                                @ModelAttribute("promotion") PromotionRequest request,
                                Model model,
                                Authentication authentication,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            promotionService.updatePromotion(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cáº­p nháº­t khuyáº¿n mÃ£i thÃ nh cÃ´ng.");

            return "redirect:/hotel-admin/promotions/" + id;

        } catch (IllegalArgumentException e) {
            addHeaderAttributes(model, authentication, session, "CHá»ˆNH Sá»¬A KHUYáº¾N MÃƒI");
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("promotion", request);
            model.addAttribute("promotionId", id);

            return "edit_promotion_old";
        }
    }

    @PostMapping("/hotel-admin/promotions/delete/{id}")
    public String deletePromotion(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("successMessage", "XÃ³a khuyáº¿n mÃ£i thÃ nh cÃ´ng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/hotel-admin/promotions";
    }

    private void addHeaderAttributes(Model model,
                                     Authentication authentication,
                                     HttpSession session,
                                     String pageTitle) {
        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentUser", currentUser);
    }
}

