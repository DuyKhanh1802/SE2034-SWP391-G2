package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.HotelAdmin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.HotelAdminDashboardService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HotelAdminDashboardController {

    private final ProfileService profileService;
    private final HotelAdminDashboardService hotelAdminDashboardService;

    @GetMapping("/hotel-admin/dashboard")
    public String dashboard(Model model,
                            Authentication authentication,
                            HttpSession session,
                            HttpServletRequest request) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("dashboard", hotelAdminDashboardService.getDashboard());

        return "hotel_admin/Dashboard";
    }
}