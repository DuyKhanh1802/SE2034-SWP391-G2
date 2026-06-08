package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

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
}
