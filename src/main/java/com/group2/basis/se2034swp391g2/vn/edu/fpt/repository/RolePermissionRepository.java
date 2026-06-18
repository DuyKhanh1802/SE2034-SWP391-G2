package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RolePermission;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    long countByRoleId(Long roleId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RolePermission rp where rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
