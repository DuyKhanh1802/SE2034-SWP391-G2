package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HouseKeepingController {

    @GetMapping("/housekeeping/dashboard")
    public String dashboard() {
        return "HouseKeeping/dashboard";
    }
}