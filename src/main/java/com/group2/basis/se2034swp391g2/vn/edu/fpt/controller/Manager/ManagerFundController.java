package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CashTransactionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.HotelFundService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class ManagerFundController {
    private final ProfileService profileService;
    private final CashTransactionService cashTransactionService;
    private final HotelFundService hotelFundService;

    @GetMapping("/manager/fund")
    public String showFund(Model model,
                           Authentication authentication,
                           HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Quản lý quỹ");

        model.addAttribute("fundSetting", hotelFundService.getCurrentSetting());
        model.addAttribute("openingBalance", hotelFundService.getOpeningBalance());
        model.addAttribute("totalIncome", cashTransactionService.getTotalIncome());
        model.addAttribute("totalExpense", cashTransactionService.getTotalExpense());
        model.addAttribute("currentBalance", hotelFundService.getCurrentBalance());
        model.addAttribute("recentTransactions", cashTransactionService.getRecentTransactions(10));

        return "manager/fund";
    }

    @PostMapping("/manager/fund/add-capital")
    public String addCapital(@RequestParam BigDecimal amount,
                             Authentication authentication,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            boolean isFirstCapital = hotelFundService.addCapital(amount, resolveCurrentUser(authentication, session));
            String message = isFirstCapital ? "Đã cấu hình vốn đầu kỳ." : "Đã ghi nhận rót thêm vốn.";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/fund";
    }

    private void addHeaderAttributes(Model model,
                                     Authentication authentication,
                                     HttpSession session,
                                     String pageTitle) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentUser", resolveCurrentUser(authentication, session));
    }

    private User resolveCurrentUser(Authentication authentication, HttpSession session) {
        return profileService.resolveCurrentUser(authentication, session);
    }
}
