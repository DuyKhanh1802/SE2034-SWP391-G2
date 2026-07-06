package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.EmailVerificationResult;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.EmailVerificationToken;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.EmailVerificationTokenRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EmailVerificationService {

    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final int MAX_ATTEMPT = 5;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    MailService mailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Transactional
    public void createAndSendVerificationOtp(User user) {
        validateUserForSendingOtp(user);

        tokenRepository.deleteByUserAndIsUsedFalse(user);

        String otp = generateOtp();

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(otp);
        verificationToken.setUser(user);
        verificationToken.setExpiryTime(Instant.now().plus(OTP_EXPIRE_MINUTES, ChronoUnit.MINUTES));
        verificationToken.setIsUsed(false);
        verificationToken.setAttemptCount(0);
        verificationToken.setCreatedAt(Instant.now());
        verificationToken.setUsedAt(null);

        tokenRepository.save(verificationToken);

        mailService.sendRegisterVerificationOtpEmail(
                user.getEmail(),
                buildGuestName(user),
                otp
        );
    }

    @Transactional
    public EmailVerificationResult verifyRegisterOtp(String email, String otp) {
        if (isBlank(email)) {
            return EmailVerificationResult.INVALID_EMAIL;
        }

        if (isBlank(otp)) {
            return EmailVerificationResult.INVALID_OTP;
        }

        String cleanedEmail = normalizeEmail(email);
        String cleanedOtp = otp.trim();

        if (!cleanedOtp.matches("^\\d{6}$")) {
            return EmailVerificationResult.INVALID_OTP;
        }

        User user = userRepository.findByEmail(cleanedEmail).orElse(null);

        if (user == null) {
            return EmailVerificationResult.INVALID_EMAIL;
        }

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            return EmailVerificationResult.DELETED_USER;
        }

        if (Boolean.TRUE.equals(user.getIsActive())) {
            return EmailVerificationResult.ALREADY_VERIFIED;
        }

        EmailVerificationToken verificationToken = tokenRepository
                .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElse(null);

        if (verificationToken == null) {
            return EmailVerificationResult.INVALID_OTP;
        }

        Integer attemptCount = verificationToken.getAttemptCount();

        if (attemptCount == null) {
            attemptCount = 0;
        }

        if (attemptCount >= MAX_ATTEMPT) {
            markTokenAsUsed(verificationToken);
            return EmailVerificationResult.MAX_ATTEMPT_EXCEEDED;
        }

        if (verificationToken.getExpiryTime() == null
                || Instant.now().isAfter(verificationToken.getExpiryTime())) {
            markTokenAsUsed(verificationToken);
            return EmailVerificationResult.EXPIRED_OTP;
        }

        if (!verificationToken.getToken().equals(cleanedOtp)) {
            attemptCount++;

            verificationToken.setAttemptCount(attemptCount);

            if (attemptCount >= MAX_ATTEMPT) {
                markTokenAsUsed(verificationToken);
                return EmailVerificationResult.MAX_ATTEMPT_EXCEEDED;
            }

            tokenRepository.save(verificationToken);
            return EmailVerificationResult.INVALID_OTP;
        }

        user.setIsActive(true);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        markTokenAsUsed(verificationToken);

        return EmailVerificationResult.SUCCESS;
    }

    @Transactional
    public void resendRegisterOtp(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }

        String cleanedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(cleanedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        validateUserForSendingOtp(user);

        createAndSendVerificationOtp(user);
    }

    private void validateUserForSendingOtp(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User không hợp lệ.");
        }

        if (isBlank(user.getEmail())) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new IllegalArgumentException("Tài khoản đã bị xóa hoặc vô hiệu hóa.");
        }

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Tài khoản đã được xác thực.");
        }
    }

    private void markTokenAsUsed(EmailVerificationToken verificationToken) {
        verificationToken.setIsUsed(true);
        verificationToken.setUsedAt(Instant.now());
        tokenRepository.save(verificationToken);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }

    private String buildGuestName(User user) {
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";

        String fullName = (lastName + " " + firstName).trim();

        if (fullName.isEmpty()) {
            return "Quý khách";
        }

        return fullName;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}