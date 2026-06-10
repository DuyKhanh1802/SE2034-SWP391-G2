package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.PasswordResetToken;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.PasswordResetTokenRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PasswordResetService {

    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final int MAX_ATTEMPT = 5;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                MailService mailService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendResetOtp(String email) {
        System.out.println("SERVICE EMAIL = " + email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            System.out.println("USER NOT FOUND");
            return;
        }

        System.out.println("USER FOUND = " + user.getEmail());

        if (Boolean.FALSE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            System.out.println("USER INACTIVE OR DELETED");
            return;
        }

        tokenRepository.deleteByUser(user);
        System.out.println("OLD OTP DELETED");

        String otp = generateOtp();
        System.out.println("OTP GENERATED = " + otp);

        PasswordResetToken resetOtp = PasswordResetToken.builder()
                .token(otp)
                .user(user)
                .expiryTime(Instant.now().plus(5, ChronoUnit.MINUTES))
                .isUsed(false)
                .attemptCount(0)
                .createdAt(Instant.now())
                .build();

        tokenRepository.save(resetOtp);
        System.out.println("OTP SAVED");

        mailService.sendResetPasswordOtpEmail(user.getEmail(), otp);
        System.out.println("OTP EMAIL SENT");
    }

    @Transactional
    public boolean verifyOtp(String email, String otp, HttpSession session) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return false;
        }

        PasswordResetToken resetOtp = tokenRepository
                .findByUserAndTokenAndIsUsedFalse(user, otp)
                .orElse(null);

        if (resetOtp == null) {
            return false;
        }

        if (resetOtp.getAttemptCount() >= MAX_ATTEMPT) {
            return false;
        }

        if (Instant.now().isAfter(resetOtp.getExpiryTime())) {
            return false;
        }

        resetOtp.setIsUsed(true);
        resetOtp.setUsedAt(Instant.now());
        tokenRepository.save(resetOtp);

        /*
         * Lưu vào session để cho phép user vào bước đổi mật khẩu.
         */
        session.setAttribute("resetEmail", email);
        session.setAttribute("otpVerified", true);

        return true;
    }

    @Transactional
    public void resetPasswordAfterOtp(String newPassword,
                                      String confirmPassword,
                                      HttpSession session) {
        Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");
        String resetEmail = (String) session.getAttribute("resetEmail");

        if (!Boolean.TRUE.equals(otpVerified) || resetEmail == null) {
            throw new RuntimeException("OTP verification is required.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Confirm password does not match.");
        }

        User user = userRepository.findByEmail(resetEmail)
                .orElseThrow(() -> new RuntimeException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        session.removeAttribute("resetEmail");
        session.removeAttribute("otpVerified");
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}