package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionSourceType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.TransactionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.TransactionListResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CashTransactionService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class ManagerTransactionController {
    private final CashTransactionService cashTransactionService;
    private final ProfileService profileService;

    @GetMapping("/manager/transactions")
    public String listTransactions(@ModelAttribute TransactionRequest searchRequest,
                                   Model model,
                                   Authentication authentication,
                                   HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Lịch sử giao dịch");

        TransactionListResponse response = cashTransactionService.getTransactionListResponse(searchRequest);

        model.addAttribute("transactions", response.getTransactions());
        model.addAttribute("searchRequest", searchRequest);
        model.addAttribute("transactionTypes", CashTransactionType.values());
        model.addAttribute("transactionCategories", CashTransactionCategory.values());
        model.addAttribute("sourceTypes", CashTransactionSourceType.values());

        return "manager/list_transactions";
    }

    @GetMapping("/manager/transactions/create")
    public String showCreateTransactionForm(Model model,
                                            Authentication authentication,
                                            HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Lập phiếu thu/chi");
        return "manager/create_manual_voucher";
    }

    @PostMapping("/manager/transactions/create")
    public String createManualTransaction(@RequestParam CashTransactionType type,
                                          @RequestParam BigDecimal amount,
                                          @RequestParam(required = false) String description,
                                          Authentication authentication,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        try {
            CashTransactionCategory category = type == CashTransactionType.INCOME
                    ? CashTransactionCategory.MANUAL_INCOME
                    : CashTransactionCategory.MANUAL_EXPENSE;

            cashTransactionService.createManualTransaction(
                    type, category, amount, description, resolveCurrentUser(authentication, session));

            redirectAttributes.addFlashAttribute("successMessage", "Đã lập phiếu thu/chi thủ công.");
            return "redirect:/manager/transactions";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/transactions/create";
        }
    }

    @GetMapping("/manager/transactions/{id}")
    public String transactionDetail(@PathVariable Long id,
                                    Model model,
                                    Authentication authentication,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "Chi tiết dòng tiền");
            model.addAttribute("transaction", cashTransactionService.getTransaction(id));
            return "manager/transaction_detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/transactions";
        }
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
