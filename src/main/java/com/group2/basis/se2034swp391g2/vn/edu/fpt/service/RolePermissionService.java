package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.constants.PermissionCode;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Permission;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RolePermission;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RolePermissionId;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PermissionRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RolePermissionRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public Set<Long> getPermissionIds(RoleName roleName) {
        Role role = getRole(roleName);
        Set<Long> permissionIds = new HashSet<>();
        role.getRolePermissions().forEach(rolePermission -> {
            if (rolePermission.getPermission() != null) {
                permissionIds.add(rolePermission.getPermission().getId());
            }
        });
        return permissionIds;
    }

    @Transactional(readOnly = true)
    public Set<Long> getRequiredPermissionIds(RoleName roleName) {
        Set<Long> requiredIds = new HashSet<>();
        requiredPermissionCodes(roleName).forEach(
                code -> requiredIds.add(getPermissionByCode(code).getId())
        );
        return requiredIds;
    }

    @Transactional
    public void updateRolePermissions(RoleName roleName, List<Long> submittedPermissionIds) {
        Role role = getRole(roleName);
        Set<Long> permissionIds = new HashSet<>(
                submittedPermissionIds == null ? List.of() : submittedPermissionIds
        );

        permissionIds.addAll(getRequiredPermissionIds(roleName));

        List<Permission> submittedPermissions = permissionRepository.findAllById(permissionIds);
        if (submittedPermissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("Danh sách quyền chứa giá trị không hợp lệ.");
        }
        addDependentViewPermissions(permissionIds, submittedPermissions);
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);

        rolePermissionRepository.deleteByRoleId(role.getId());
        Role roleReference = entityManager.getReference(Role.class, role.getId());
        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setId(new RolePermissionId(role.getId(), permission.getId()));
            rolePermission.setRole(roleReference);
            rolePermission.setPermission(entityManager.getReference(Permission.class, permission.getId()));
            rolePermissionRepository.save(rolePermission);
        }
        rolePermissionRepository.flush();
    }

    private Role getRole(RoleName roleName) {
        Role role = roleRepository.findByRoleName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Không tìm thấy vai trò " + roleName.name());
        }
        return role;
    }

    private Permission getPermissionByCode(String code) {
        return permissionRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Chưa khởi tạo quyền " + code));
    }

    private Set<String> requiredPermissionCodes(RoleName roleName) {
        return switch (roleName) {
            case SYSTEM_ADMIN -> Set.of(
                    PermissionCode.USER_VIEW,
                    PermissionCode.ROLE_PERMISSION_MANAGE
            );
            case HOTEL_ADMIN -> Set.of(PermissionCode.HOTEL_DASHBOARD_VIEW);
            case MANAGER -> Set.of(PermissionCode.MANAGER_DASHBOARD_VIEW);
            case STOREKEEPER -> Set.of(PermissionCode.INVENTORY_VIEW);
            case RECEPTIONIST -> Set.of(PermissionCode.RECEPTION_DASHBOARD_VIEW);
            case GUEST -> Set.of(PermissionCode.PROFILE_VIEW);
        };
    }

    private void addDependentViewPermissions(Set<Long> permissionIds, List<Permission> permissions) {
        Set<String> codes = permissions.stream()
                .map(Permission::getCode)
                .collect(java.util.stream.Collectors.toSet());

        addDependency(permissionIds, codes, PermissionCode.USER_MANAGE, PermissionCode.USER_VIEW);
        addDependency(permissionIds, codes, PermissionCode.USER_APPROVE, PermissionCode.USER_VIEW);
        addDependency(permissionIds, codes, PermissionCode.ROOM_MANAGE, PermissionCode.ROOM_VIEW);
        addDependency(permissionIds, codes, PermissionCode.PROMOTION_MANAGE, PermissionCode.PROMOTION_VIEW);
        addDependency(permissionIds, codes, PermissionCode.FINANCE_MANAGE, PermissionCode.FINANCE_VIEW);
        addDependency(permissionIds, codes, PermissionCode.INVENTORY_MANAGE, PermissionCode.INVENTORY_VIEW);
        addDependency(permissionIds, codes, PermissionCode.BOOKING_MANAGE, PermissionCode.BOOKING_VIEW);
        addDependency(permissionIds, codes, PermissionCode.CHECK_IN, PermissionCode.BOOKING_VIEW);
        addDependency(permissionIds, codes, PermissionCode.PROFILE_EDIT, PermissionCode.PROFILE_VIEW);
    }

    private void addDependency(Set<Long> permissionIds,
                               Set<String> selectedCodes,
                               String actionCode,
                               String viewCode) {
        if (selectedCodes.contains(actionCode)) {
            permissionIds.add(getPermissionByCode(viewCode).getId());
        }
    }
}
