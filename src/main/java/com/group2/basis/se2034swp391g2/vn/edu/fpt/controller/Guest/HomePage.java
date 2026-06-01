package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Guest;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePage {
    @GetMapping({"/home"})
    public String index(Model model,
                        @AuthenticationPrincipal User user){
        model.addAttribute("email",user.getEmail());
        return "guest/HomePage";
    }
}