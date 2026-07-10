package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Gender;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils.DisplayUtils;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserViewService {

    private final RoleRepository roleRepository;

    public Map<Long, String> buildRoleLabels(List<User> users) {
        return users.stream()
                .collect(Collectors.toMap(User::getId, this::getRoleLabel));
    }

    public Map<Long, String> buildDisplayNames(List<User> users) {
        return users.stream()
                .collect(Collectors.toMap(User::getId, DisplayUtils::formatDisplayName));
    }

    public String getRoleLabel(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return user.getUserType() != null && "GUEST".equals(user.getUserType().name())
                    ? "Guest"
                    : "No role assigned";
        }

        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(role -> role != null && role.getRoleName() != null)
                .sorted((left, right) -> Integer.compare(getRoleOrder(left.getRoleName()), getRoleOrder(right.getRoleName())))
                .map(role -> toRoleLabel(role.getRoleName()))
                .findFirst()
                .orElse("No role assigned");
    }

    public Long getSelectedRoleId(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return null;
        }

        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(role -> role != null && role.getId() != null)
                .sorted((left, right) -> Integer.compare(getRoleOrder(left.getRoleName()), getRoleOrder(right.getRoleName())))
                .map(role -> role.getId())
                .findFirst()
                .orElse(null);
    }

    public String getGenderLabel(User user) {
        if (user.getGender() == null) {
            return "None";
        }
        return toGenderLabel(user.getGender());
    }

    public Map<Long, String> getRoleOptions() {
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

    public Map<RoleName, String> getRoleFilterOptions() {
        Map<RoleName, String> options = new LinkedHashMap<>();
        for (RoleName roleName : RoleName.values()) {
            options.put(roleName, toRoleLabel(roleName));
        }
        return options;
    }

    private int getRoleOrder(RoleName roleName) {
        return switch (roleName) {
            case SYSTEM_ADMIN -> 1;
            case HOTEL_ADMIN -> 2;
            case MANAGER -> 3;
            case STOREKEEPER -> 4;
            case RECEPTIONIST -> 5;
            case GUEST -> 6;
        };
    }

    private String toRoleLabel(RoleName roleName) {
        return switch (roleName) {
            case SYSTEM_ADMIN -> "Quản trị Hệ Thống";
            case HOTEL_ADMIN -> "Quản trị khách Sạn";
            case MANAGER -> "Quản Lý";
            case STOREKEEPER -> "Thủ Kho";
            case RECEPTIONIST -> "Lễ Tân";
            case GUEST -> "Khách Hàng";
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
