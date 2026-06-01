package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ManagerController {
    @GetMapping("/manager/dashboard")
    public String dashboard() {
        return "Manager/Dashboard";
    }
}
