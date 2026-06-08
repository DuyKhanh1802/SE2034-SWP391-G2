package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Receptionist;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/receptionist/dashboard")
    public String dashboard() {
        return "receptionist/Dashboard";
    }
}