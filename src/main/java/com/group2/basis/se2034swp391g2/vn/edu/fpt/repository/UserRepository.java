package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByIdAndIsDeletedFalse(Long id);
    Optional<User> findByEmailAndIsDeletedFalse(String email);


    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByIdentityNumberAndIdNot(String identityNumber, Long id);

    Optional<User> findByPhoneAndIsDeletedFalse(String phone);

    Optional<User> findByIdentityNumberAndIsDeletedFalse(String identityNumber);

    @Query("Select u From User u " +
            "Left join fetch u.userRoles ur " +
            "Left join fetch ur.role " +
            "Left join fetch ur.role.rolePermissions rp " +
            "Left join fetch rp.permission " +
            "Left join fetch u.country " +
            "Where u.email = :email")
    Optional<User> findByEmailDetail(String email);

    @Query("Select u From User u " +
            "Left join fetch u.userRoles ur " +
            "Left join fetch ur.role " +
            "Left join fetch u.country " +
            "Where u.id = :id And u.isDeleted = false")
    Optional<User> findUserWithRoleById(Long id);

    @Query("""
            Select Count(Distinct u.id)
            From User u
            Join u.userRoles ur
            Join ur.role r
            Where u.isDeleted = false
              And u.isActive = true
              And r.roleName = :roleName
            """)
    long countActiveUsersByRoleName(@Param("roleName") RoleName roleName);

    Optional<User> findByEmailIgnoreCase(String email);

}
