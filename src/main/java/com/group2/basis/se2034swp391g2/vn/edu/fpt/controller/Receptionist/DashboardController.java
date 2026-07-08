package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ReceptionistDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ReceptionistDashboardService receptionistDashboardService;

    public DashboardController(ReceptionistDashboardService receptionistDashboardService) {
        this.receptionistDashboardService = receptionistDashboardService;
    }

    @GetMapping("/receptionist/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "TỔNG QUAN LỄ TÂN");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("dashboard", receptionistDashboardService.getDashboard());

        return "receptionist/Dashboard";
    }
}