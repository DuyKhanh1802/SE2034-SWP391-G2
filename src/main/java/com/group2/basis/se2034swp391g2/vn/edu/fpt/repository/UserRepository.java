package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Optional<User> findByPhoneAndIsDeletedFalse(String phone);

    Optional<User> findByIdentityNumberAndIsDeletedFalse(String identityNumber);

    boolean existsByEmailAndIsDeletedFalse(String email);

    @Query("Select u From User u " +
            "Left join fetch u.userRoles ur " +
            "Left join fetch ur.role " +
            "Left join fetch u.country " +
            "Where u.email = :email")
    Optional<User> findByEmailDetail(String email);

    @Query("Select u From User u " +
            "Left join fetch u.userRoles ur " +
            "Left join fetch ur.role " +
            "Left join fetch u.country " +
            "Where u.id = :id")
    Optional<User> findUserWithRoleById(Long id);
    boolean existsByPhoneAndIsDeletedFalse(String phone);

    boolean existsByIdentityNumberAndIsDeletedFalse(String identityNumber);
    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByIsDeletedFalseOrderByCreatedAtDesc();

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
