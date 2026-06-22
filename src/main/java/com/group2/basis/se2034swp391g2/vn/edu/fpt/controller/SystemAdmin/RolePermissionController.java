package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.SystemAdmin;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.constants.PermissionCode;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.RoleSwitchController;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/system-admin/role-permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCode.ROLE_PERMISSION_MANAGE + "')")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    public String showRolePermissions(
            @RequestParam(value = "role", defaultValue = "SYSTEM_ADMIN") RoleName roleName,
            Model model) {
        model.addAttribute("roles", roleOptions());
        model.addAttribute("selectedRole", roleName);
        model.addAttribute("permissions", rolePermissionService.getAllPermissions());
        model.addAttribute("selectedPermissionIds", rolePermissionService.getPermissionIds(roleName));
        model.addAttribute("requiredPermissionIds", rolePermissionService.getRequiredPermissionIds(roleName));
        model.addAttribute("pageTitle", "Phân quyền vai trò");
        return "system_admin/RolePermissions";
    }

    @PostMapping
    public String updateRolePermissions(
            @RequestParam("role") RoleName roleName,
            @RequestParam(value = "permissionIds", required = false) List<Long> permissionIds,
            RedirectAttributes redirectAttributes) {
        try {
            rolePermissionService.updateRolePermissions(roleName, permissionIds);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã cập nhật quyền cho vai trò " + RoleSwitchController.getRoleLabel(roleName) + "."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/system-admin/role-permissions?role=" + roleName.name();
    }

    private Map<RoleName, String> roleOptions() {
        Map<RoleName, String> options = new LinkedHashMap<>();
        Arrays.stream(RoleName.values())
                .forEach(role -> options.put(role, RoleSwitchController.getRoleLabel(role)));
        return options;
    }
}
