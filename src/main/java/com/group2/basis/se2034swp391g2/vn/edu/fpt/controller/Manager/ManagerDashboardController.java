package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ManagerDashboardService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ManagerDashboardController {
    private final ProfileService profileService;
    private final ManagerDashboardService managerDashboardService;

    @GetMapping("/manager/dashboard")
    public String showDashboard(Model model, Authentication authentication, HttpSession session) {
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("currentUser", profileService.resolveCurrentUser(authentication, session));
        model.addAttribute("dashboard", managerDashboardService.getDashboard());
        return "manager/Dashboard";
    }
}
