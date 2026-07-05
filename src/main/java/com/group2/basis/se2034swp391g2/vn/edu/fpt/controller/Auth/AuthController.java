package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Auth;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RegisterRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.AuthService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

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
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           Model model) {

        model.addAttribute("countries", authService.getAllCountries());

        if (!request.getPasswordHash().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Confirm password không khớp với password"
            );
        }

        if (bindingResult.hasErrors()) {
            return "auth/Register";
        }

        try {
            authService.register(request);
            model.addAttribute("successMessage", "Đăng ký tài khoản thành công");
            model.addAttribute("registerRequest", new RegisterRequest());
            return "auth/Login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/Register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetOtp(@RequestParam("email") String email,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== POST /auth/forgot-password ===");
            System.out.println("EMAIL INPUT = " + email);

            passwordResetService.sendResetOtp(email);

            System.out.println("=== OTP SERVICE DONE ===");

            session.setAttribute("resetEmailInput", email);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Nếu email tồn tại trong hệ thống, mã OTP đã được gửi.");

            return "redirect:/auth/verify-reset-otp";

        } catch (RuntimeException e) {
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể gửi OTP. Vui lòng kiểm tra cấu hình email.");

            return "redirect:/auth/forgot-password";
        }
    }

    @GetMapping("/verify-reset-otp")
    public String verifyOtpPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmailInput");
        model.addAttribute("email", email);
        return "auth/verify-reset-otp";
    }

    @PostMapping("/verify-reset-otp")
    public String verifyOtp(@RequestParam("email") String email,
                            @RequestParam("otp") String otp,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        boolean valid = passwordResetService.verifyOtp(email, otp, session);

        if (!valid) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            return "auth/verify-reset-otp";
        }

        return "redirect:/auth/reset-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session) {
        Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");

        if (!Boolean.TRUE.equals(otpVerified)) {
            return "redirect:/auth/forgot-password";
        }

        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            passwordResetService.resetPasswordAfterOtp(newPassword, confirmPassword, session);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");

            return "redirect:/auth/login";
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        }
    }
}
