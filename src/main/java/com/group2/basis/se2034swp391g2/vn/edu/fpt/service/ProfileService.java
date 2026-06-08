package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.IdentityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.exception.ProfileValidationException;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Country;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ChangePasswordRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ProfileUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final String VIETNAM_COUNTRY_CODE = "VN";

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    public User resolveCurrentUser(Authentication authentication, HttpSession session) {
        String email = authentication != null ? authentication.getName() : null;

        if (email != null && !email.isBlank() && !"anonymousUser".equalsIgnoreCase(email)) {
            return userRepository.findByEmailDetail(email).orElse(null);
        }

        Long loginUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (loginUserId != null) {
            return userRepository.findUserWithRoleById(loginUserId).orElse(null);
        }

        return null;
    }

    public boolean isGuest(User user) {
        if (user == null) {
            return false;
        }

        if (user.getUserType() == UserType.GUEST) {
            return true;
        }

        if (user.getUserRoles() == null) {
            return false;
        }

        for (UserRole userRole : user.getUserRoles()){
            if(userRole != null){
                Role role = userRole.getRole();
                if(role != null && role.getRoleName() == RoleName.GUEST){
                    return true;
                }
            }
        }
        return false;
    }

    public ProfileUpdateRequest toProfileUpdateRequest(User user) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();

        request.setFirstName(user.getFirstName());
        request.setLastName(user.getLastName());
        request.setPhone(user.getPhone());
        request.setEmail(user.getEmail());
        request.setGender(user.getGender());
        request.setDateOfBirth(user.getDateOfBirth());
        request.setIdentityType(user.getIdentityType());
        request.setIdentityNumber(user.getIdentityNumber());
        request.setAvatarUrl(user.getAvatarUrl());
        request.setGuest(isGuest(user));

        if (user.getCountry() != null) {
            request.setCountryId(user.getCountry().getId());
            request.setCountryName(user.getCountry().getCountryName());
            request.setCountryCode(user.getCountry().getCountryCode());
        }

        return request;
    }

    public void validateProfileBusinessRules(ProfileUpdateRequest form) {
        ProfileValidationException exception = new ProfileValidationException();

        if (form.getGender() == null) {
            exception.addFieldError("gender", "Hãy chọn giới tính của bạn");
        }

        validateCountryAndIdentity(form, exception);
        validateAvatar(form.getAvatarFile(), exception);

        if (exception.hasErrors()) {
            throw exception;
        }
    }

    private void validateCountryAndIdentity(ProfileUpdateRequest form,
                                            ProfileValidationException exception) {

        if (form.getCountryId() == null) {
            exception.addFieldError("countryId", "Hãy chọn quốc gia của bạn");
            return;
        }

        Country country = countryRepository.findById(form.getCountryId()).orElse(null);

        if (country == null) {
            exception.addFieldError("countryId", "Quốc gia được chọn ko hợp lệ");
            return;
        }

        String identityNumber = form.getIdentityNumber();

        if (identityNumber == null || identityNumber.isBlank()) {
            if (isVietnamCountry(country)) {
                exception.addFieldError("identityNumber", "Yêu cầu căn cước công dân");
            } else {
                exception.addFieldError("identityNumber", "Passport number is required.");
            }
            return;
        }

        identityNumber = identityNumber.trim();

        if (isVietnamCountry(country)) {
            if (!identityNumber.matches("^\\d{12}$")) {
                exception.addFieldError("identityNumber", "Số căn cước công dân phải đúng 12 chữ số");
            }
        } else {
            if (!identityNumber.matches("^[A-Za-z0-9]{6,20}$")) {
                exception.addFieldError("identityNumber", "Passport number must contain 6 to 20 letters or digits.");
            }
        }
    }

    private void validateAvatar(MultipartFile avatarFile,
                                ProfileValidationException exception) {

        if (avatarFile == null || avatarFile.isEmpty()) {
            return;
        }

        String contentType = avatarFile.getContentType();

        boolean validType = "image/jpeg".equalsIgnoreCase(contentType)
                || "image/jpg".equalsIgnoreCase(contentType)
                || "image/png".equalsIgnoreCase(contentType);

        if (!validType) {
            exception.addFieldError("avatarFile", "Ảnh đại diện phải là JPG hoặc PNG.");
        }

        if (avatarFile.getSize() > MAX_AVATAR_SIZE) {
            exception.addFieldError("avatarFile", "Ảnh đại diện phải bé hơn 2MB.");
        }
    }

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest form) {
        User user = getUserOrThrow(userId);

        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        user.setPhone(form.getPhone());
        user.setGender(form.getGender());
        user.setDateOfBirth(form.getDateOfBirth());

        updateCountryAndIdentity(user, form);
        updateAvatarIfPresent(user, form.getAvatarFile());

        userRepository.save(user);
    }

    private void updateCountryAndIdentity(User user, ProfileUpdateRequest form) {
        Country country = countryRepository.findById(form.getCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Quốc gia không tồn tại"));

        user.setCountry(country);

        if (isVietnamCountry(country)) {
            user.setIdentityType(IdentityType.CCCD);
        } else {
            user.setIdentityType(IdentityType.PASSPORT);
        }

        user.setIdentityNumber(form.getIdentityNumber().trim());
    }

    private void updateAvatarIfPresent(User user, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return;
        }

        String avatarUrl = cloudinaryService.uploadAvatar(avatarFile);
        user.setAvatarUrl(avatarUrl);
    }

    public void validateChangePasswordBusinessRules(ChangePasswordRequest form, User currentUser) {
        ProfileValidationException exception = new ProfileValidationException();

        if (isGuest(currentUser)) {
            exception.addFieldError("currentPassword", "Tài khoản quý khách không thể đổi mật khẩu");
            throw exception;
        }

        if (currentUser.getPasswordHash() == null || currentUser.getPasswordHash().isBlank()) {
            exception.addFieldError("currentPassword", "Tài khoản không hỗ trợ đổi mật khẩu");
            throw exception;
        }

        if (!passwordEncoder.matches(form.getCurrentPassword(), currentUser.getPasswordHash())) {
            exception.addFieldError("currentPassword", "Mật khẩu gần đây không đúng");
        }

        if (form.getNewPassword() != null
                && form.getConfirmPassword() != null
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            exception.addFieldError("confirmPassword", "Mật khẩu xác nhận không khớp");
        }

        if (form.getNewPassword() != null
                && passwordEncoder.matches(form.getNewPassword(), currentUser.getPasswordHash())) {
            exception.addFieldError("newPassword", "Mật khẩu mới phải khác mật khẩu cũ");
        }

        if (exception.hasErrors()) {
            throw exception;
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest form) {
        User user = getUserOrThrow(userId);

        if (isGuest(user)) {
            throw new IllegalArgumentException("Tài khoản quý khách không thể đổi mật khẩu");
        }

        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
    }

    private boolean isVietnamCountry(Country country) {
        return country != null
                && country.getCountryCode() != null
                && VIETNAM_COUNTRY_CODE.equalsIgnoreCase(country.getCountryCode());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findUserWithRoleById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}