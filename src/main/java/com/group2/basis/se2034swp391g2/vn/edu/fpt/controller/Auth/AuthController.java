package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Auth;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.EmailVerificationResult;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.exception.EmailVerificationResentException;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RegisterRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.AuthService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.EmailVerificationService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }

        model.addAttribute("countries", authService.getAllCountries());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes,
                           HttpSession session) {

        model.addAttribute("countries", authService.getAllCountries());

        if (!Objects.equals(request.getPasswordHash(), request.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Confirm password không khớp với password"
            );
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        String email = request.getEmail() != null
                ? request.getEmail().trim().toLowerCase()
                : "";

        try {
            authService.register(request);

            session.setAttribute("registerEmailInput", email);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đăng ký thành công. Mã OTP xác thực đã được gửi đến email của quý khách."
            );

            return "redirect:/auth/verify-register-otp";

        } catch (EmailVerificationResentException e) {
            session.setAttribute("registerEmailInput", email);

            redirectAttributes.addFlashAttribute("successMessage", e.getMessage());
            return "redirect:/auth/verify-register-otp";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";

        } catch (RuntimeException e) {
            e.printStackTrace();

            model.addAttribute(
                    "errorMessage",
                    "Không thể hoàn tất đăng ký hoặc gửi OTP xác thực. Vui lòng thử lại."
            );

            return "auth/register";
        }
    }

    @GetMapping("/verify-register-otp")
    public String verifyRegisterOtpPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("registerEmailInput");

        if (email == null || email.trim().isEmpty()) {
            return "redirect:/auth/register";
        }

        model.addAttribute("email", email);
        return "auth/verify-register-otp";
    }

    @PostMapping("/verify-register-otp")
    public String verifyRegisterOtp(@RequestParam("email") String email,
                                    @RequestParam("otp") String otp,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {

        EmailVerificationResult result = emailVerificationService.verifyRegisterOtp(email, otp);

        switch (result) {
            case SUCCESS:
                session.removeAttribute("registerEmailInput");

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Xác thực email thành công. Quý khách có thể đăng nhập."
                );

                return "redirect:/auth/login";

            case ALREADY_VERIFIED:
                session.removeAttribute("registerEmailInput");

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Tài khoản đã được xác thực trước đó. Quý khách có thể đăng nhập."
                );

                return "redirect:/auth/login";

            case EXPIRED_OTP:
                model.addAttribute("email", email);
                model.addAttribute("errorMessage", "Mã OTP đã hết hạn. Vui lòng gửi lại mã OTP mới.");
                return "auth/verify-register-otp";

            case MAX_ATTEMPT_EXCEEDED:
                model.addAttribute("email", email);
                model.addAttribute("errorMessage", "Quý khách đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã OTP mới.");
                return "auth/verify-register-otp";

            case DELETED_USER:
                session.removeAttribute("registerEmailInput");

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Tài khoản này đã bị vô hiệu hóa hoặc xóa khỏi hệ thống."
                );

                return "redirect:/auth/register";

            case INVALID_EMAIL:
                session.removeAttribute("registerEmailInput");

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Email xác thực không hợp lệ. Vui lòng đăng ký lại."
                );

                return "redirect:/auth/register";

            case INVALID_OTP:
            default:
                model.addAttribute("email", email);
                model.addAttribute("errorMessage", "Mã OTP không hợp lệ. Vui lòng kiểm tra lại.");
                return "auth/verify-register-otp";
        }
    }

    @PostMapping("/resend-register-otp")
    public String resendRegisterOtp(HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("registerEmailInput");

        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Phiên xác thực đã hết hạn. Vui lòng đăng ký lại."
            );
            return "redirect:/auth/register";
        }

        try {
            emailVerificationService.resendRegisterOtp(email);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Mã OTP mới đã được gửi đến email của quý khách."
            );

            return "redirect:/auth/verify-register-otp";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/register";

        } catch (RuntimeException e) {
            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể gửi lại mã OTP. Vui lòng thử lại."
            );

            return "redirect:/auth/verify-register-otp";
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
            passwordResetService.sendResetOtp(email);

            session.setAttribute("resetEmailInput", email);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Nếu email tồn tại trong hệ thống, mã OTP đã được gửi."
            );

            return "redirect:/auth/verify-reset-otp";

        } catch (RuntimeException e) {
            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể gửi OTP. Vui lòng kiểm tra cấu hình email."
            );

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

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."
            );

            return "redirect:/auth/login";

        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        }
    }
}