package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.PasswordResetToken;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByUserAndTokenAndIsUsedFalse(User user, String token);

    Optional<PasswordResetToken> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}