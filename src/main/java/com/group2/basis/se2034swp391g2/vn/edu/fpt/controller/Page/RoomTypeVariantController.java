package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RoomSearchCriteria;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.PromotionApplyResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.GuestRoomVariantProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection.RoomVariantDetailProjection;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PromotionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomTypeVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/page")
@Controller
public class RoomTypeVariantController {

    private final RoomTypeVariantService roomTypeVariantService;
    private final PromotionService promotionService;

    @GetMapping("/rooms")
    public String listRoomTypeVariant(
            @RequestParam(name = "roomTypeId", required = false) Long roomTypeId,

            @RequestParam(name = "viewType", required = false) String viewType,

            @RequestParam(name = "sort", defaultValue = "recommended") String sort,

            @RequestParam(name = "checkInDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkInDate,

            @RequestParam(name = "checkOutDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOutDate,

            @RequestParam(name = "adults", required = false, defaultValue = "1") Integer adults,

            @RequestParam(name = "children", required = false, defaultValue = "0") Integer children,

            @RequestParam(name = "roomCount", required = false, defaultValue = "1") Integer roomCount,

            @RequestParam(name = "roomGuests", required = false) String roomGuests,

            @RequestParam(name = "promoCode", required = false) String promoCode,

            @RequestParam(name = "variantIds", required = false) String variantIds,

            Model model
    ) {
        RoomSearchCriteria criteria = roomTypeVariantService.normalizeSearchCriteria(
                roomTypeId,
                viewType,
                sort,
                checkInDate,
                checkOutDate,
                adults,
                children,
                roomCount,
                roomGuests
        );

        List<RoomType> roomTypes = roomTypeVariantService.getRoomTypes();

        List<GuestRoomVariantProjection> roomTypeVariants =
                roomTypeVariantService.getGuestRoomVariants(criteria);

        PromotionApplyResponse promotionResult =
                promotionService.checkPromotionCode(promoCode);

        BigDecimal promotionDiscountAmount = promotionResult.isValid()
                ? promotionResult.getDiscountAmount()
                : BigDecimal.ZERO;

        List<Long> selectedVariantIds = Collections.emptyList();

        if (variantIds != null && !variantIds.isBlank()) {
            selectedVariantIds = Arrays.stream(variantIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(Long::valueOf)
                    .toList();
        }

        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("roomOptions", roomTypeVariants);

        model.addAttribute("selectedRoomTypeId", criteria.getRoomTypeId());
        model.addAttribute("selectedViewType", criteria.getViewType());
        model.addAttribute("selectedSort", criteria.getSort());

        model.addAttribute("checkInDate", criteria.getCheckInDate());
        model.addAttribute("checkOutDate", criteria.getCheckOutDate());
        model.addAttribute("adults", criteria.getAdults());
        model.addAttribute("children", criteria.getChildren());
        model.addAttribute("roomCount", criteria.getRoomCount());
        model.addAttribute("roomGuests", criteria.getRoomGuests());

        model.addAttribute("promoCode", promoCode);
        model.addAttribute("promotionResult", promotionResult);
        model.addAttribute("promotionDiscountAmount", promotionDiscountAmount);
        model.addAttribute("promotionMessage", promotionResult.getMessage());
        model.addAttribute("promotionValid", promotionResult.isValid());

        model.addAttribute("variantIds", variantIds);
        model.addAttribute("selectedVariantIds", selectedVariantIds);

        model.addAttribute("searchWarning", criteria.getWarningMessage());

        return "page/RoomTypeVariant";
    }

    @GetMapping("/room-variants/{variantId}")
    public String viewRoomTypeVariantDetail(
            @PathVariable Long variantId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkInDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOutDate,

            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            RoomVariantDetailProjection room =
                    roomTypeVariantService.getRoomVariantDetail(
                            variantId,
                            checkInDate,
                            checkOutDate
                    );

            model.addAttribute("room", room);
            model.addAttribute("checkInDate", checkInDate);
            model.addAttribute("checkOutDate", checkOutDate);

            return "page/RoomTypeVariantDetail";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/page/rooms";
        }
    }
}