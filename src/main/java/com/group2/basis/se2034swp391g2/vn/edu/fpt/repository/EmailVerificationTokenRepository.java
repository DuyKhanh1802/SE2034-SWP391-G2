package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.EmailVerificationToken;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken,Long> {
    Optional<EmailVerificationToken> findByToken(String token);

    void deleteByUserAndIsUsedFalse(User user);

    Optional<EmailVerificationToken> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);


}
