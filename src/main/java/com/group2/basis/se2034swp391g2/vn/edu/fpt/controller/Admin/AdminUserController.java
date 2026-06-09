package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Admin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Gender;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils.DisplayUtils;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.CustomerUserDetails;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/list-user")
@RequiredArgsConstructor
public class AdminUserController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final UserService userService;
    private final RoleRepository roleRepository;

    @GetMapping
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            Model model) {
        int requestedPage = Math.max(page, 1);
        PageRequest pageRequest = PageRequest.of(requestedPage - 1, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        Page<User> userPage = userService.getAccountPage(keyword, pageRequest);
        List<User> users = userPage.getContent();

        model.addAttribute("users", users);
        model.addAttribute("roleLabelsByUserId", buildRoleLabels(users));
        model.addAttribute("displayNamesByUserId", buildDisplayNames(users));
        model.addAttribute("genderLabelsByUserId", buildGenderLabels(users));
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", userPage.getNumber() + 1);
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
        model.addAttribute("roleLabel", getRoleLabel(user));
        model.addAttribute("statusLabel", getStatusLabel(user));
        model.addAttribute("approvalLabel", getApprovalLabel(user));
        model.addAttribute("genderLabel", getGenderLabel(user));
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
        updateRequest.setRoleIds(getSelectedRoleIds(user));

        model.addAttribute("user", user);
        model.addAttribute("displayName", DisplayUtils.formatDisplayName(user));
        model.addAttribute("roleLabel", getRoleLabel(user));
        model.addAttribute("roleOptions", getRoleOptions());
        model.addAttribute("roleCodesById", getRoleCodesById());
        Long currentAdminId = getCurrentUserId(authentication);
        model.addAttribute("currentAdminId", currentAdminId);
        model.addAttribute("canDeactivateSelf", currentAdminId != null && currentAdminId.equals(user.getId()));
        model.addAttribute("updateRequest", updateRequest);
        model.addAttribute("pageTitle", "Chỉnh sửa người dùng");
        return "admin/account/EditUser";
    }

    @PostMapping("/{id}/edit")
    public String processEditUser(@PathVariable("id") Long id,
                                  @ModelAttribute("updateRequest") AccountUpdateRequest request,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Long currentAdminId = getCurrentUserId(authentication);
        if (currentAdminId != null) {
            request.setCurrentAdminId(currentAdminId);
            if (currentAdminId.equals(id) && Boolean.FALSE.equals(request.getIsActive())) {
                request.setIsActive(Boolean.TRUE);
            }
            if (currentAdminId.equals(id)) {
                request.setRoleIds(null);
            }
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

    private boolean matchesKeyword(User user, String keyword) {
        return contains(user.getFirstName(), keyword)
                || contains(user.getLastName(), keyword)
                || contains(DisplayUtils.formatDisplayName(user), keyword)
                || contains(user.getEmail(), keyword)
                || contains(user.getPhone(), keyword)
                || contains(user.getIdentityNumber(), keyword)
                || contains(getRoleLabel(user), keyword)
                || contains(user.getUserType() != null ? user.getUserType().name() : null, keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Map<Long, String> buildRoleLabels(List<User> users) {
        return users.stream()
                .collect(Collectors.toMap(User::getId, this::getRoleLabel));
    }

    private Map<Long, String> buildDisplayNames(List<User> users) {
        return users.stream()
                .collect(Collectors.toMap(User::getId, DisplayUtils::formatDisplayName));
    }

    private Map<Long, String> buildGenderLabels(List<User> users) {
        return users.stream()
                .collect(Collectors.toMap(User::getId, this::getGenderLabel));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerUserDetails principal)) {
            return null;
        }
        return principal.getUser().getId();
    }

    private String getRoleLabel(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return user.getUserType() != null && "GUEST".equals(user.getUserType().name())
                    ? "Khách hàng"
                    : "Chưa gán vai trò";
        }

        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(role -> role != null && role.getRoleName() != null)
                .sorted((left, right) -> Integer.compare(getRoleOrder(left.getRoleName()), getRoleOrder(right.getRoleName())))
                .map(role -> toRoleLabel(role.getRoleName()))
                .collect(Collectors.joining(", "));
    }

    private List<Long> getSelectedRoleIds(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return List.of();
        }

        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(role -> role != null && role.getId() != null)
                .map(role -> role.getId())
                .toList();
    }

    private String getStatusLabel(User user) {
        return Boolean.TRUE.equals(user.getIsActive()) ? "Đang hoạt động" : "Đã vô hiệu hóa";
    }

    private String getApprovalLabel(User user) {
        if (user.getApprovalStatus() == null) {
            return "Chưa có";
        }
        return switch (user.getApprovalStatus()) {
            case APPROVED -> "Đã duyệt";
            case PENDING -> "Chờ duyệt";
            case REJECTED -> "Từ chối";
        };
    }

    private String getGenderLabel(User user) {
        if (user.getGender() == null) {
            return "Chưa có";
        }
        return toGenderLabel(user.getGender());
    }

    private Map<Long, String> getRoleOptions() {
        return roleRepository.findAll().stream()
                .filter(role -> role.getId() != null && role.getRoleName() != null)
                .sorted((left, right) -> Integer.compare(getRoleOrder(left.getRoleName()), getRoleOrder(right.getRoleName())))
                .collect(Collectors.toMap(
                        role -> role.getId(),
                        role -> toRoleLabel(role.getRoleName()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, String> getRoleCodesById() {
        return roleRepository.findAll().stream()
                .filter(role -> role.getId() != null && role.getRoleName() != null)
                .collect(Collectors.toMap(
                        role -> role.getId(),
                        role -> role.getRoleName().name()
                ));
    }

    private int getRoleOrder(RoleName roleName) {
        return switch (roleName) {
            case SYSTEM_ADMIN -> 1;
            case HOTEL_ADMIN -> 2;
            case MANAGER -> 3;
            case RECEPTIONIST -> 4;
            case GUEST -> 5;
        };
    }

    private String toRoleLabel(RoleName roleName) {
        return switch (roleName) {
            case SYSTEM_ADMIN -> "Quản trị hệ thống";
            case HOTEL_ADMIN -> "Quản trị khách sạn";
            case MANAGER -> "Quản lý";
            case RECEPTIONIST -> "Lễ tân";
            case GUEST -> "Khách hàng";
        };
    }

    private String toGenderLabel(Gender gender) {
        return switch (gender) {
            case MALE -> "Nam";
            case FEMALE -> "Nữ";
            case OTHER -> "Khác";
        };
    }
}
