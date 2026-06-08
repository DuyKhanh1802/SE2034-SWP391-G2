package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Optional<User> findByPhoneAndIsDeletedFalse(String phone);

    Optional<User> findByIdentityNumberAndIsDeletedFalse(String identityNumber);

    boolean existsByEmailAndIsDeletedFalse(String email);

    boolean existsByPhoneAndIsDeletedFalse(String phone);

    boolean existsByIdentityNumberAndIsDeletedFalse(String identityNumber);
}
