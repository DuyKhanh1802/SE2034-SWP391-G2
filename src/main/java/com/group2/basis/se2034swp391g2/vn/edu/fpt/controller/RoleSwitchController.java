package com.group2.basis.se2034swp391g2.vn.edu.fpt.controller;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class RoleSwitchController {

    public static final String CURRENT_ACTIVE_ROLE = "CURRENT_ACTIVE_ROLE";
    public static final String CURRENT_ACTIVE_ROLE_LABEL = "CURRENT_ACTIVE_ROLE_LABEL";
    public static final String AVAILABLE_ACTIVE_ROLE_OPTIONS = "AVAILABLE_ACTIVE_ROLE_OPTIONS";

    public record ActiveRoleOption(String name, String label) {
    }

    @GetMapping("/api/user/switch-role")
    public String switchRole(@RequestParam("role") RoleName role,
                             Authentication authentication,
                             HttpSession session) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (!authorities.contains("ROLE_" + role.name())) {
            return "redirect:/error";
        }

        session.setAttribute(CURRENT_ACTIVE_ROLE, role.name());
        session.setAttribute(CURRENT_ACTIVE_ROLE_LABEL, getRoleLabel(role));
        return "redirect:" + getDashboardPath(role);
    }

    public static RoleName resolveDefaultActiveRole(Set<String> authorities) {
        if (authorities.contains("ROLE_SYSTEM_ADMIN")) {
            return RoleName.SYSTEM_ADMIN;
        }
        if (authorities.contains("ROLE_HOTEL_ADMIN")) {
            return RoleName.HOTEL_ADMIN;
        }
        if (authorities.contains("ROLE_MANAGER")) {
            return RoleName.MANAGER;
        }
        if (authorities.contains("ROLE_STOREKEEPER")) {
            return RoleName.STOREKEEPER;
        }
        if (authorities.contains("ROLE_RECEPTIONIST")) {
            return RoleName.RECEPTIONIST;
        }
        return RoleName.GUEST;
    }

    public static String getDashboardPath(RoleName role) {
        return switch (role) {
            case SYSTEM_ADMIN -> "/system-admin/list-user";
            case HOTEL_ADMIN -> "/hotel-admin/dashboard";
            case MANAGER -> "/manager/dashboard";
            case STOREKEEPER -> "/storekeeper/inventory";
            case RECEPTIONIST -> "/receptionist/dashboard";
            case GUEST -> "/home";
        };
    }

    public static List<ActiveRoleOption> getAvailableActiveRoleOptions(Set<String> authorities) {
        return List.of(
                        RoleName.SYSTEM_ADMIN,
                        RoleName.HOTEL_ADMIN,
                        RoleName.MANAGER,
                        RoleName.STOREKEEPER,
                        RoleName.RECEPTIONIST
                ).stream()
                .filter(role -> authorities.contains("ROLE_" + role.name()))
                .map(role -> new ActiveRoleOption(role.name(), getRoleLabel(role)))
                .toList();
    }

    public static String getRoleLabel(RoleName role) {
        return switch (role) {
            case SYSTEM_ADMIN -> "Quản Trị Hệ Thống";
            case HOTEL_ADMIN -> "Quản Trị Khách Sạn";
            case MANAGER -> "Quản Lý";
            case STOREKEEPER -> "Thủ Kho";
            case RECEPTIONIST -> "Lễ Tân";
            case GUEST -> "Khách Hàng";
        };
    }
}
