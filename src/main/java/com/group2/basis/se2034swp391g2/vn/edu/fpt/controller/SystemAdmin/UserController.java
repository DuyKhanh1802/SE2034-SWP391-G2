package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.SystemAdmin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils.DisplayUtils;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.AdminUserViewService;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CustomerUserDetails;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/system-admin/list-user")
@RequiredArgsConstructor
public class UserController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final UserService userService;
    private final AdminUserViewService adminUserViewService;

    @GetMapping
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "role", required = false) RoleName role,
                            @RequestParam(value = "activeStatus", defaultValue = "ALL") String activeStatus,
                            @RequestParam(value = "approvalStatus", required = false) ApprovalStatus approvalStatus,
                            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
                            Authentication authentication,
                            Model model) {
        Page<User> userPage = userService.getAccountPage(
                keyword, role, activeStatus, approvalStatus, pageable);
        List<User> users = userPage.getContent();

        model.addAttribute("users", users);
        model.addAttribute("roleLabelsByUserId", adminUserViewService.buildRoleLabels(users));
        model.addAttribute("displayNamesByUserId", adminUserViewService.buildDisplayNames(users));
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleFilterOptions", adminUserViewService.getRoleFilterOptions());
        model.addAttribute("selectedRole", role);
        model.addAttribute("activeStatus", activeStatus);
        model.addAttribute("selectedApprovalStatus", approvalStatus);
        model.addAttribute("currentPage", userPage.getNumber() + 1);
        model.addAttribute("pageSize", userPage.getSize());
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalElements", userPage.getTotalElements());
        model.addAttribute("hasPrevious", userPage.hasPrevious());
        model.addAttribute("hasNext", userPage.hasNext());
        model.addAttribute("currentUserId", getCurrentUserId(authentication));
        model.addAttribute("pageTitle", "Quản lý người dùng");
        return "system_admin/ListUser";
    }

    @GetMapping("/{id}")
    public String viewUserDetail(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("displayName", DisplayUtils.formatDisplayName(user));
        model.addAttribute("roleLabel", adminUserViewService.getRoleLabel(user));
        model.addAttribute("genderLabel", adminUserViewService.getGenderLabel(user));
        model.addAttribute("pageTitle", "Chi tiết người dùng");
        return "system_admin/UserDetail";
    }

    @PostMapping("/{id}/review")
    public String processReviewUser(@PathVariable("id") Long id,
                                    @RequestParam("action") String action,
                                    @RequestParam(value = "approvalNote", required = false) String approvalNote,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setCurrentAdminId(getCurrentUserId(authentication));

        if ("approve".equalsIgnoreCase(action)) {
            request.setApprovalStatus(ApprovalStatus.APPROVED);
        } else if ("reject".equalsIgnoreCase(action)) {
            request.setApprovalStatus(ApprovalStatus.REJECTED);
            request.setApprovalNote(approvalNote);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Thao tác duyệt tài khoản không hợp lệ.");
            return "redirect:/system-admin/list-user/" + id + "/edit";
        }

        try {
            userService.updateUser(id, request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    request.getApprovalStatus() == ApprovalStatus.APPROVED
                            ? "Đã duyệt và kích hoạt tài khoản."
                            : "Đã từ chối và vô hiệu hóa tài khoản."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/system-admin/list-user/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String showEditUserForm(@PathVariable("id") Long id,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(id);
        Long currentAdminId = getCurrentUserId(authentication);

        if (currentAdminId != null && currentAdminId.equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể chỉnh sửa tài khoản đang đăng nhập.");
            return "redirect:/system-admin/list-user";
        }

        AccountUpdateRequest updateRequest = new AccountUpdateRequest();
        updateRequest.setIsActive(user.getIsActive());
        updateRequest.setRoleIds(adminUserViewService.getSelectedRoleIds(user));

        model.addAttribute("user", user);
        model.addAttribute("displayName", DisplayUtils.formatDisplayName(user));
        model.addAttribute("roleLabel", adminUserViewService.getRoleLabel(user));
        model.addAttribute("roleOptions", adminUserViewService.getRoleOptions());
        model.addAttribute("roleCodesById", adminUserViewService.getRoleCodesById());
        model.addAttribute("updateRequest", updateRequest);
        model.addAttribute("pageTitle", "Chỉnh sửa người dùng");
        return "system_admin/EditUser";
    }

    @PostMapping("/{id}/edit")
    public String processEditUser(@PathVariable("id") Long id,
                                  @ModelAttribute("updateRequest") AccountUpdateRequest request,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Long currentAdminId = getCurrentUserId(authentication);
        if (currentAdminId != null) {
            request.setCurrentAdminId(currentAdminId);
        }

        try {
            userService.updateUser(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin tài khoản.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/system-admin/list-user/" + id + "/edit";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Long currentAdminId = getCurrentUserId(authentication);
        if (currentAdminId != null && currentAdminId.equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể chỉnh sửa tài khoản đang đăng nhập.");
            return "redirect:/system-admin/list-user";
        }

        String temporaryPassword = userService.resetPassword(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đặt lại mật khẩu thành công.");
        redirectAttributes.addFlashAttribute("temporaryPassword", temporaryPassword);
        return "redirect:/system-admin/list-user/" + id + "/edit";
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails principal)) {
            return null;
        }
        return principal.getUser().getId();
    }
}
