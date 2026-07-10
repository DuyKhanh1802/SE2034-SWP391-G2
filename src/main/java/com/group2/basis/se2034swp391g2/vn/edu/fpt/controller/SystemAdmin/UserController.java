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
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.stream.IntStream;

@Controller
@RequestMapping("/system-admin/list-user")
@RequiredArgsConstructor
public class UserController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final UserService userService;
    private final AdminUserViewService adminUserViewService;

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
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
        model.addAttribute("currentPageIndex", userPage.getNumber());
        List<Integer> visiblePages = buildVisiblePages(userPage.getNumber(), userPage.getTotalPages());
        model.addAttribute("visiblePages", visiblePages);
        model.addAttribute("showLeadingEllipsis", !visiblePages.isEmpty() && visiblePages.getFirst() > 0);
        model.addAttribute("showTrailingEllipsis", !visiblePages.isEmpty() && visiblePages.getLast() < userPage.getTotalPages() - 1);
        model.addAttribute("currentUserId", getCurrentUserId(authentication));
        model.addAttribute("pageTitle", "Quản lý người dùng");
        return "system_admin/ListUser";
    }

    private List<Integer> buildVisiblePages(int currentPage, int totalPages) {
        if (totalPages <= 0) {
            return List.of();
        }

        if (totalPages <= 5) {
            return IntStream.range(0, totalPages).boxed().toList();
        }

        int start;
        int end;

        if (currentPage <= 2) {
            start = 0;
            end = 3;
        } else if (currentPage >= totalPages - 3) {
            start = totalPages - 4;
            end = totalPages - 1;
        } else {
            start = currentPage - 1;
            end = currentPage + 2;
        }

        return IntStream.rangeClosed(start, end).boxed().toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String viewUserDetail(@PathVariable("id") Long id,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User user;
        try {
            user = userService.getUserById(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/system-admin/list-user";
        }
        model.addAttribute("user", user);
        model.addAttribute("displayName", DisplayUtils.formatDisplayName(user));
        model.addAttribute("roleLabel", adminUserViewService.getRoleLabel(user));
        model.addAttribute("genderLabel", adminUserViewService.getGenderLabel(user));
        model.addAttribute("pageTitle", "Chi tiết người dùng");
        return "system_admin/UserDetail";
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
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
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String showEditUserForm(@PathVariable("id") Long id,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        User user;
        try {
            user = userService.getUserById(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/system-admin/list-user";
        }
        Long currentAdminId = getCurrentUserId(authentication);

        if (currentAdminId != null && currentAdminId.equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể chỉnh sửa tài khoản đang đăng nhập.");
            return "redirect:/system-admin/list-user";
        }

        AccountUpdateRequest updateRequest = new AccountUpdateRequest();
        updateRequest.setIsActive(user.getIsActive());
        updateRequest.setRoleId(adminUserViewService.getSelectedRoleId(user));

        model.addAttribute("user", user);
        model.addAttribute("displayName", DisplayUtils.formatDisplayName(user));
        model.addAttribute("roleLabel", adminUserViewService.getRoleLabel(user));
        model.addAttribute("roleOptions", adminUserViewService.getRoleOptions());
        model.addAttribute("updateRequest", updateRequest);
        model.addAttribute("pageTitle", "Chỉnh sửa người dùng");
        return "system_admin/EditUser";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String processEditUser(@PathVariable("id") Long id,
                                  @ModelAttribute("updateRequest") AccountUpdateRequest request,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Long currentAdminId = getCurrentUserId(authentication);
        if (currentAdminId != null) {
            request.setCurrentAdminId(currentAdminId);
        }
        request.setApprovalStatus(null);
        request.setApprovalNote(null);
        if (Boolean.TRUE.equals(request.getRoleUpdateRequested())) {
            request.setIsActive(null);
        } else {
            request.setRoleId(null);
            request.setRoleUpdateRequested(false);
        }

        try {
            userService.updateUser(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin tài khoản.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/system-admin/list-user/" + id + "/edit";
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails principal)) {
            return null;
        }
        return principal.getUser().getId();
    }
}
