package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;
// Import UserService của bạn...
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    // Inject UserService vào đây (bạn cần tạo UserService và UserRepository)
    // private final UserService userService;

    // 1. LIST USER
    @GetMapping
    public String listUsers(Model model) {
        // List<User> users = userService.getAllStaffUsers();
        // model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Accounts");
        return "admin/accounts/ListUser"; // Trỏ đến file ListUser.html
    }

    // 2. VIEW USER DETAIL
    @GetMapping("/{id}")
    public String viewUserDetail(@PathVariable Long id, Model model) {
        // User user = userService.getUserById(id);
        // model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Account Detail");
        return "admin/accounts/UserDetail";
    }

    // 3. ADD USER (Hiển thị form & Xử lý submit)
    @GetMapping("/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("accountRequest", new AccountCreateRequest());
        model.addAttribute("pageTitle", "Add Account");
        return "admin/accounts/AddUser";
    }

    @PostMapping("/add")
    public String processAddUser(@ModelAttribute("accountRequest") AccountCreateRequest request) {
        // userService.createUser(request);
        return "redirect:/admin/accounts?success=true";
    }

    // 4. EDIT USER (Hiển thị form & Xử lý submit)
    @GetMapping("/{id}/edit")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        // User user = userService.getUserById(id);
        // Map user qua AccountUpdateRequest rồi đẩy xuống form
        model.addAttribute("updateRequest", new AccountUpdateRequest());
        model.addAttribute("pageTitle", "Edit Account");
        return "admin/accounts/EditUser";
    }

    @PostMapping("/{id}/edit")
    public String processEditUser(@PathVariable Long id, @ModelAttribute("updateRequest") AccountUpdateRequest request) {
        // userService.updateUser(id, request);
        return "redirect:/admin/accounts?updated=true";
    }
}