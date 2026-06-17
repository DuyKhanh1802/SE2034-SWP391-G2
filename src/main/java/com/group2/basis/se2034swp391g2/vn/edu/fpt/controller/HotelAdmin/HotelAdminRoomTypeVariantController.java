package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.HotelAdmin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RoomTypeVariantManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/hotel-admin/room-type-variants")
public class HotelAdminRoomTypeVariantController {

    private final RoomTypeVariantManagementService roomTypeVariantManagementService;
    private final ProfileService profileService;

    public HotelAdminRoomTypeVariantController(RoomTypeVariantManagementService roomTypeVariantManagementService,
                                               ProfileService profileService) {
        this.roomTypeVariantManagementService = roomTypeVariantManagementService;
        this.profileService = profileService;
    }

    private void addLayoutData(Model model,
                               Authentication authentication,
                               HttpSession session,
                               HttpServletRequest request,
                               String pageTitle) {
        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", pageTitle);
    }

    @GetMapping
    public String listRoomTypeVariants(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) ViewType viewType,
                                       Model model,
                                       Authentication authentication,
                                       HttpSession session,
                                       HttpServletRequest request) {

        addLayoutData(model, authentication, session, request, "Hạng phòng / Variant");

        int pageSize = 5;

        Page<RoomTypeVariant> variantPage = roomTypeVariantManagementService.searchVariants(
                keyword,
                viewType,
                PageRequest.of(page, pageSize)
        );

        model.addAttribute("variants", variantPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", variantPage.getTotalPages());
        model.addAttribute("totalItems", variantPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedViewType", viewType);
        model.addAttribute("viewTypes", ViewType.values());

        return "hotel_admin/ListRoomTypeVariant";
    }
}