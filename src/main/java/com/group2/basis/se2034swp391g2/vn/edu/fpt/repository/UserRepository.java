package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByIdentityNumber(String identityNumber);
    Optional<User> findByPhone(String phone);

    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByIsDeletedFalseOrderByCreatedAtDesc();

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
