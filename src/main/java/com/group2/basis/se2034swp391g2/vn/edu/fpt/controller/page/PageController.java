package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping({"/","/home"})
    public String index(Model model){
        model.addAttribute("pageTitle","Audo Residence");
        return "Guest/HomePage";
    }
}
