package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ProfileService profileService;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model,
                            Authentication authentication,
                            HttpSession session) {

        User currentUser = profileService.resolveCurrentUser(authentication, session);

        model.addAttribute("currentUser", currentUser);

        return "admin/Dashboard";
    }
}
