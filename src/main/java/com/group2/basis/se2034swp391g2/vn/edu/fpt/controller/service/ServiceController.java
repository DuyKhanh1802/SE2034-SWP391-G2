package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServiceController {

    @GetMapping("/service-staff/dashboard")
    public String dashboard() {

        return "ServiceStaff/Dashboard";

    }

}