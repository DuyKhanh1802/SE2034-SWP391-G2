package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils.DisplayUtils;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.AdminUserViewService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CustomerUserDetails;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/list-user")
@RequiredArgsConstructor
public class AdminUserController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final UserService userService;
    private final AdminUserViewService adminUserViewService;

    @GetMapping
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
                            Model model) {
        Page<User> userPage = userService.getAccountPage(keyword, pageable);
        List<User> users = userPage.getContent();

        model.addAttribute("users", users);
        model.addAttribute("roleLabelsByUserId", adminUserViewService.buildRoleLabels(users));
        model.addAttribute("displayNamesByUserId", adminUserViewService.buildDisplayNames(users));
        model.addAttribute("genderLabelsByUserId", adminUserViewService.buildGenderLabels(users));
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", userPage.getNumber() + 1);
        model.addAttribute("pageSize", userPage.getSize());
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalElements", userPage.getTotalElements());
        model.addAttribute("hasPrevious", userPage.hasPrevious());
        model.addAttribute("hasNext", userPage.hasNext());
        model.addAttribute("pageTitle", "Quản lý người dùng");
        return "admin/account/ListUser";
    }

    @GetMapping("/{id}")
    public String viewUserDetail(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("displayName", DisplayUtils.formatDisplayName(user));
        model.addAttribute("roleLabel", adminUserViewService.getRoleLabel(user));
        model.addAttribute("statusLabel", adminUserViewService.getStatusLabel(user));
        model.addAttribute("approvalLabel", adminUserViewService.getApprovalLabel(user));
        model.addAttribute("genderLabel", adminUserViewService.getGenderLabel(user));
        model.addAttribute("pageTitle", "Chi tiết người dùng");
        return "admin/account/UserDetail";
    }

    @PostMapping("/{id}/review")
    public String processReviewUser(@PathVariable("id") Long id,
                                    @RequestParam("action") String action,
                                    Authentication authentication) {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setCurrentAdminId(getCurrentUserId(authentication));

        if ("approve".equalsIgnoreCase(action)) {
            request.setApprovalStatus(ApprovalStatus.APPROVED);
        } else if ("reject".equalsIgnoreCase(action)) {
            request.setApprovalStatus(ApprovalStatus.REJECTED);
            request.setApprovalNote("Từ chối bởi Quản trị hệ thống");
        }

        userService.updateUser(id, request);
        return "redirect:/admin/list-user/" + id + "?reviewed=true";
    }

    @GetMapping("/{id}/edit")
    public String showEditUserForm(@PathVariable("id") Long id,
                                   Authentication authentication,
                                   Model model) {
        User user = userService.getUserById(id);

        AccountUpdateRequest updateRequest = new AccountUpdateRequest();
        updateRequest.setIsActive(user.getIsActive());
        updateRequest.setRoleIds(adminUserViewService.getSelectedRoleIds(user));

        model.addAttribute("user", user);
        model.addAttribute("displayName", DisplayUtils.formatDisplayName(user));
        model.addAttribute("roleLabel", adminUserViewService.getRoleLabel(user));
        model.addAttribute("roleOptions", adminUserViewService.getRoleOptions());
        model.addAttribute("roleCodesById", adminUserViewService.getRoleCodesById());
        Long currentAdminId = getCurrentUserId(authentication);
        model.addAttribute("isCurrentUser", currentAdminId != null && currentAdminId.equals(user.getId()));
        model.addAttribute("updateRequest", updateRequest);
        model.addAttribute("pageTitle", "Chỉnh sửa người dùng");
        return "admin/account/EditUser";
    }

    @PostMapping("/{id}/edit")
    public String processEditUser(@PathVariable("id") Long id,
                                  @Valid @ModelAttribute("updateRequest") AccountUpdateRequest request,
                                  BindingResult bindingResult,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Long currentAdminId = getCurrentUserId(authentication);
        if (currentAdminId != null) {
            request.setCurrentAdminId(currentAdminId);
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "Dữ liệu cập nhật không hợp lệ.");
            return "redirect:/admin/list-user/" + id + "/edit";
        }

        try {
            userService.updateUser(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin tài khoản.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/list-user/" + id + "/edit";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id,
                                RedirectAttributes redirectAttributes) {
        String temporaryPassword = userService.resetPassword(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đặt lại mật khẩu thành công.");
        redirectAttributes.addFlashAttribute("temporaryPassword", temporaryPassword);
        return "redirect:/admin/list-user/" + id + "/edit";
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails principal)) {
            return null;
        }
        return principal.getUser().getId();
    }
}
