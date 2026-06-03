package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Auth;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.LoginRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RegisterRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.Banner;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("countries", authService.getAllCountries());
        return "auth/register";
    }

    @PostMapping("/register")
    public String Register(
            @ModelAttribute("registerRequest") RegisterRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            authService.register(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tài khoản đã được tạo thành công");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("countries", authService.getAllCountries());
            return "auth/register";
        }
    }
}
