package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Auth;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RegisterRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "/auth/Login";        // ← sửa
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("nationalities", authService.getAllCountries()); // ← thêm dòng này
        return "/auth/Register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest req,
                           BindingResult result,
                           Model model) {
        if (result.hasErrors()) {
            // Phải truyền lại object vào model khi có lỗi
            model.addAttribute("registerRequest", req);
            model.addAttribute("nationalities",authService.getAllCountries());
            return "auth/register";
        }
        try {
            authService.register(req);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("registerRequest", req); // ← giữ lại data user đã nhập
            return "auth/register";
        }
        return "redirect:/auth/login?registered";
    }
}
