package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final UserService userService;

    // ==========================================
    // 1. MÀN HÌNH DANH SÁCH USER
    // ==========================================
    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllStaffUsers();
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Accounts");

        // Trỏ đúng vào thư mục 'account' không có 's' của bạn
        return "admin/account/ListUser";
    }

    // ==========================================
    // 2. MÀN HÌNH CHI TIẾT USER (KÈM CHỨC NĂNG DUYỆT)
    // ==========================================
    @GetMapping("/{id}")
    public String viewUserDetail(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Account Detail");

        return "admin/account/UserDetail";
    }

    @PostMapping("/{id}/review")
    public String processReviewUser(@PathVariable("id") Long id,
                                    @RequestParam("action") String action,
                                    @AuthenticationPrincipal User adminUser) {
        AccountUpdateRequest request = new AccountUpdateRequest();

        // Lấy ID Admin để lưu Audit (Ai là người duyệt)
        if (adminUser != null) {
            request.setCurrentAdminId(adminUser.getId());
        }

        if ("approve".equalsIgnoreCase(action)) {
            request.setApprovalStatus(ApprovalStatus.APPROVED);
        } else if ("reject".equalsIgnoreCase(action)) {
            request.setApprovalStatus(ApprovalStatus.REJECTED);
            request.setApprovalNote("Rejected by Admin during review");
        }

        userService.updateUser(id, request);
        return "redirect:/admin/account/" + id + "?reviewed=true";
    }

    // ==========================================
    // 3. MÀN HÌNH THÊM MỚI USER
    // ==========================================
    @GetMapping("/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("accountRequest", new AccountCreateRequest());
        model.addAttribute("pageTitle", "Add Account");

        return "admin/account/AddUser";
    }

    @PostMapping("/add")
    public String processAddUser(@ModelAttribute("accountRequest") AccountCreateRequest request,
                                 @AuthenticationPrincipal User adminUser) {
        // Lấy ID Admin lưu vào vết
        if (adminUser != null) {
            request.setCurrentAdminId(adminUser.getId());
        }

        userService.createUser(request);
        return "redirect:/admin/account/add?success=true";
    }

    // ==========================================
    // 4. MÀN HÌNH CẬP NHẬT (EDIT) USER
    // ==========================================
    @GetMapping("/{id}/edit")
    public String showEditUserForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);

        AccountUpdateRequest updateRequest = new AccountUpdateRequest();
        updateRequest.setFirstName(user.getFirstName());
        updateRequest.setLastName(user.getLastName());
        updateRequest.setPhone(user.getPhone());
        updateRequest.setIsActive(user.getIsActive());

        model.addAttribute("user", user);
        model.addAttribute("updateRequest", updateRequest);
        model.addAttribute("pageTitle", "Edit Account");

        return "admin/account/EditUser";
    }

    @PostMapping("/{id}/edit")
    public String processEditUser(@PathVariable("id") Long id,
                                  @ModelAttribute("updateRequest") AccountUpdateRequest request,
                                  @AuthenticationPrincipal User adminUser) {

        if (adminUser != null) {
            request.setCurrentAdminId(adminUser.getId());
        }

        userService.updateUser(id, request);
        return "redirect:/admin/account/" + id + "?updated=true";
    }
}