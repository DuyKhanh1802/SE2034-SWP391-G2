package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Manager;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.CashTransaction;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.CashTransactionCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.CashTransactionRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.CashTransactionListResponse;
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

@Controller
@RequiredArgsConstructor
public class CashTransactionController {
    private final CashTransactionService cashTransactionService;
    private final ProfileService profileService;

    // Hiển thị danh sách dòng tiền, bộ lọc và phân trang.
    @GetMapping("/manager/transactions")
    public String listTransactions(@ModelAttribute CashTransactionRequest searchRequest,
                                   Model model,
                                   Authentication authentication,
                                   HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Lịch sử giao dịch");

        // Lấy danh sách giao dịch theo các điều kiện lọc trên màn hình.
        CashTransactionListResponse response = cashTransactionService.getCashTransactionListResponse(searchRequest);

        model.addAttribute("transactions", response.getTransactions());
        model.addAttribute("searchRequest", searchRequest);
        model.addAttribute("transactionTypes", CashTransactionType.values());
        model.addAttribute("transactionCategories", CashTransactionCategory.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("totalTransactions", response.getTotalTransactions());
        model.addAttribute("currentPage", response.getCurrentPage());
        model.addAttribute("totalPages", response.getTotalPages());
        model.addAttribute("pageSize", response.getPageSize());
        model.addAttribute("hasPrevious", response.isHasPrevious());
        model.addAttribute("hasNext", response.isHasNext());

        return "manager/list_transactions";
    }

    // Mở form lập phiếu thu/chi thủ công.
    @GetMapping("/manager/transactions/create")
    public String showCreateTransactionForm(Model model,
                                            Authentication authentication,
                                            HttpSession session) {
        addHeaderAttributes(model, authentication, session, "Lập phiếu thu/chi");
        addCreateScreenAttributes(model, new CashTransactionCreateRequest());
        return "manager/create_manual_voucher";
    }

    // Nhận dữ liệu form và nhờ service tạo phiếu thu/chi.
    @PostMapping("/manager/transactions/create")
    public String createManualTransaction(@ModelAttribute("voucher") CashTransactionCreateRequest request,
                                          Model model,
                                          Authentication authentication,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        try {
            cashTransactionService.createManualTransaction(request, resolveCurrentUser(authentication, session));

            redirectAttributes.addFlashAttribute("successMessage", "Đã lập phiếu thu/chi thủ công.");
            return "redirect:/manager/transactions";
        } catch (IllegalArgumentException e) {
            addHeaderAttributes(model, authentication, session, "Lập phiếu thu/chi");
            addCreateScreenAttributes(model, request);
            model.addAttribute("errorMessage", e.getMessage());
            return "manager/create_manual_voucher";
        }
    }

    // Xem chi tiết một dòng tiền.
    @GetMapping("/manager/transactions/{id}")
    public String transactionDetail(@PathVariable Long id,
                                    Model model,
                                    Authentication authentication,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            addHeaderAttributes(model, authentication, session, "Chi tiết dòng tiền");
            CashTransaction transaction = cashTransactionService.getTransaction(id);
            model.addAttribute("transaction", transaction);
            model.addAttribute("inventoryReceipt", cashTransactionService.getInventoryReceiptForTransaction(transaction));
            model.addAttribute("isPaymentTransaction", isPaymentCategory(transaction.getCategory()));
            model.addAttribute("isInventoryTransaction", transaction.getCategory() == CashTransactionCategory.INVENTORY_PURCHASE);
            // Chỉ hiện form hủy khi transaction đủ điều kiện hủy.
            model.addAttribute("canCancel", cashTransactionService.canCancelManualVoucher(transaction));
            return "manager/transaction_detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/transactions";
        }
    }

    // Hủy phiếu thủ công và quay lại màn chi tiết.
    @PostMapping("/manager/transactions/{id}/cancel")
    public String cancelManualVoucher(@PathVariable Long id,
                                      @RequestParam String cancellationReason,
                                      Authentication authentication,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            // Controller chỉ nhận request, phần kiểm tra và tạo đảo chiều nằm trong service.
            cashTransactionService.cancelManualVoucher(
                    id, resolveCurrentUser(authentication, session), cancellationReason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy phiếu và tạo giao dịch đảo chiều.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể hủy phiếu. Vui lòng kiểm tra lại dữ liệu bảng cash_transactions.");
        }

        return "redirect:/manager/transactions/" + id;
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

    private void addCreateScreenAttributes(Model model, CashTransactionCreateRequest request) {
        model.addAttribute("voucher", request);
        model.addAttribute("transactionTypes", CashTransactionType.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
    }

    private boolean isPaymentCategory(CashTransactionCategory category) {
        return category == CashTransactionCategory.DEPOSIT
                || category == CashTransactionCategory.BOOKING_PAYMENT
                || category == CashTransactionCategory.REFUND;
    }
}
