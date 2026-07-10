package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CashTransactionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class ManagerFundController {
    private final ProfileService profileService;
    private final CashTransactionService cashTransactionService;

    @GetMapping("/manager/fund")
    public String showFund(Model model,
                           Authentication authentication,
                           HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Quản lý quỹ");

        BigDecimal totalIncome = cashTransactionService.getTotalIncome();
        BigDecimal totalExpense = cashTransactionService.getTotalExpense();
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("currentBalance", totalIncome.subtract(totalExpense));
        model.addAttribute("recentTransactions", cashTransactionService.getRecentTransactions(10));

        return "manager/fund";
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
