package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.IdentityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.Page.GuestSessionAdvice;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.exception.ProfileValidationException;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Country;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ChangePasswordRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.ProfileUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.GuestRoomSession;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

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

        GuestRoomSession guestSession =
                (GuestRoomSession) session.getAttribute(GuestSessionAdvice.GUEST_ROOM_SESSION);

        if (guestSession != null && guestSession.getGuestId() != null) {
            return userRepository.findUserWithRoleById(guestSession.getGuestId()).orElse(null);
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

        for (UserRole userRole : user.getUserRoles()) {
            if (userRole != null) {
                Role role = userRole.getRole();
                if (role != null && role.getRoleName() == RoleName.GUEST) {
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
        request.setPhone(toDisplayPhone(user.getPhone(), user.getCountry()));
        request.setEmail(user.getEmail());
        request.setGender(user.getGender());

        request.setBirthYear(user.getBirthYear());

        request.setIdentityType(user.getIdentityType());
        request.setIdentityNumber(user.getIdentityNumber());
        request.setAvatarUrl(user.getAvatarUrl());
        request.setGuest(isGuest(user));

        if (user.getCountry() != null) {
            request.setCountryId(user.getCountry().getId());
            request.setCountryName(user.getCountry().getCountryName());
            request.setCountryCode(user.getCountry().getCountryCode());
            request.setPhoneCode(user.getCountry().getPhoneCode());
        }

        return request;
    }

    public void validateProfileBusinessRules(ProfileUpdateRequest form, Long currentUserId) {
        ProfileValidationException exception = new ProfileValidationException();

        if (form.getGender() == null) {
            exception.addFieldError("gender", "Hãy chọn giới tính của bạn");
        }

        validateName(form, exception);
        validateBirthYear(form, exception);
        validateCountryPhoneAndIdentity(form, currentUserId, exception);
        validateAvatar(form.getAvatarFile(), exception);

        if (exception.hasErrors()) {
            throw exception;
        }
    }

    private void validateName(ProfileUpdateRequest form,
                              ProfileValidationException exception) {

        if (form.getFirstName() != null) {
            String firstName = form.getFirstName().trim();

            if (!firstName.matches("^[\\p{L} ]{2,50}$")) {
                exception.addFieldError(
                        "firstName",
                        "Tên phải từ 2 đến 50 ký tự và chỉ được chứa chữ cái, khoảng trắng"
                );
            }
        }

        if (form.getLastName() != null) {
            String lastName = form.getLastName().trim();

            if (!lastName.matches("^[\\p{L} ]{2,50}$")) {
                exception.addFieldError(
                        "lastName",
                        "Họ phải từ 2 đến 50 ký tự và chỉ được chứa chữ cái, khoảng trắng"
                );
            }
        }
    }

    private void validateBirthYear(ProfileUpdateRequest form,
                                   ProfileValidationException exception) {

        Integer birthYear = form.getBirthYear();
        int currentYear = LocalDate.now().getYear();

        if (birthYear == null) {
            exception.addFieldError("birthYear", "Năm sinh không được để trống");
            return;
        }

        if (birthYear < 1900) {
            exception.addFieldError("birthYear", "Năm sinh không được nhỏ hơn 1900");
            return;
        }

        if (birthYear >= currentYear) {
            exception.addFieldError("birthYear", "Năm sinh phải nhỏ hơn năm hiện tại");
            return;
        }

        if (birthYear > currentYear - 18) {
            exception.addFieldError("birthYear", "Khách hàng phải từ 18 tuổi trở lên");
        }
    }

    private void validateCountryPhoneAndIdentity(ProfileUpdateRequest form,
                                                 Long currentUserId,
                                                 ProfileValidationException exception) {

        if (form.getCountryId() == null) {
            exception.addFieldError("countryId", "Hãy chọn quốc gia của bạn");
            return;
        }

        Country country = countryRepository.findById(form.getCountryId()).orElse(null);

        if (country == null) {
            exception.addFieldError("countryId", "Quốc gia được chọn không hợp lệ");
            return;
        }

        validatePhone(form, country, currentUserId, exception);
        validateIdentity(form, country, currentUserId, exception);
    }

    private void validatePhone(ProfileUpdateRequest form,
                               Country country,
                               Long currentUserId,
                               ProfileValidationException exception) {

        String phone = form.getPhone();

        if (phone == null || phone.isBlank()) {
            exception.addFieldError("phone", "Số điện thoại không được để trống");
            return;
        }

        String normalizedPhone = normalizePhone(phone);
        String phoneForChecking;

        if (isVietnamCountry(country)) {
            if (!normalizedPhone.matches("^(0\\d{9,11}|\\+84\\d{9,11})$")) {
                exception.addFieldError(
                        "phone",
                        "Số điện thoại Việt Nam phải bắt đầu bằng 0 hoặc +84 và có từ 10 đến 12 chữ số"
                );
                return;
            }

            phoneForChecking = normalizedPhone;

        } else {
            String phoneCode = resolvePhoneCode(form, country);

            if (phoneCode == null || phoneCode.isBlank()) {
                exception.addFieldError("phone", "Vui lòng chọn quốc gia để xác định mã vùng điện thoại");
                return;
            }

            String normalizedPhoneCode = normalizePhone(phoneCode);

            if (!normalizedPhoneCode.matches("^\\+[1-9]\\d{0,4}$")) {
                exception.addFieldError("phone", "Mã vùng điện thoại không hợp lệ");
                return;
            }

            String localPhone = normalizedPhone;

            // Nếu user paste cả số đầy đủ, ví dụ +58395824473,
            // thì tự tách +58 ra, chỉ giữ 395824473.
            if (localPhone.startsWith(normalizedPhoneCode)) {
                localPhone = localPhone.substring(normalizedPhoneCode.length());
            }

            // Nếu còn bắt đầu bằng + nghĩa là user nhập sai mã vùng khác quốc gia đã chọn.
            if (localPhone.startsWith("+")) {
                exception.addFieldError(
                        "phone",
                        "Số điện thoại không khớp với mã vùng quốc gia đã chọn"
                );
                return;
            }

            localPhone = localPhone.replaceFirst("^0+", "");

            phoneForChecking = normalizedPhoneCode + localPhone;

            if (!phoneForChecking.matches("^\\+[1-9]\\d{7,14}$")) {
                exception.addFieldError(
                        "phone",
                        "Số điện thoại quốc tế không hợp lệ. Vui lòng chỉ nhập số điện thoại sau mã vùng"
                );
                return;
            }
        }

        if (userRepository.existsByPhoneAndIdNot(phoneForChecking, currentUserId)) {
            exception.addFieldError("phone", "Số điện thoại đã được sử dụng");
        }
    }

    private void validateIdentity(ProfileUpdateRequest form,
                                  Country country,
                                  Long currentUserId,
                                  ProfileValidationException exception) {

        String identityNumber = form.getIdentityNumber();

        if (identityNumber == null || identityNumber.isBlank()) {
            if (isVietnamCountry(country)) {
                exception.addFieldError("identityNumber", "Yêu cầu căn cước công dân");
            } else {
                exception.addFieldError("identityNumber", "Passport number is required");
            }
            return;
        }

        identityNumber = identityNumber.trim().toUpperCase();

        if (isVietnamCountry(country)) {
            if (!identityNumber.matches("^\\d{12}$")) {
                exception.addFieldError("identityNumber", "Số căn cước công dân phải đúng 12 chữ số");
                return;
            }
        } else {
            if (!identityNumber.matches("^[A-Z0-9]{6,20}$")) {
                exception.addFieldError(
                        "identityNumber",
                        "Passport number must contain 6 to 20 letters or digits"
                );
                return;
            }
        }

        if (userRepository.existsByIdentityNumberAndIdNot(identityNumber, currentUserId)) {
            exception.addFieldError("identityNumber", "Số CCCD/Hộ chiếu đã được sử dụng");
        }
    }

    private void validateAvatar(MultipartFile avatarFile,
                                ProfileValidationException exception) {

        if (avatarFile == null || avatarFile.isEmpty()) {
            return;
        }

        String originalFilename = avatarFile.getOriginalFilename();
        String contentType = avatarFile.getContentType();

        boolean validContentType = "image/jpeg".equalsIgnoreCase(contentType)
                || "image/jpg".equalsIgnoreCase(contentType)
                || "image/png".equalsIgnoreCase(contentType);

        boolean validExtension = originalFilename != null
                && (originalFilename.toLowerCase().endsWith(".jpg")
                || originalFilename.toLowerCase().endsWith(".jpeg")
                || originalFilename.toLowerCase().endsWith(".png"));

        if (!validContentType || !validExtension) {
            exception.addFieldError("avatarFile", "Ảnh đại diện phải là JPG hoặc PNG");
            return;
        }

        if (avatarFile.getSize() > MAX_AVATAR_SIZE) {
            exception.addFieldError("avatarFile", "Ảnh đại diện phải bé hơn 2MB");
        }
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

        if (form.getCurrentPassword() == null || form.getCurrentPassword().isBlank()) {
            exception.addFieldError("currentPassword", "Mật khẩu hiện tại không được để trống");
        } else if (!passwordEncoder.matches(form.getCurrentPassword(), currentUser.getPasswordHash())) {
            exception.addFieldError("currentPassword", "Mật khẩu hiện tại không đúng");
        }

        validatePasswordStrength(form.getNewPassword(), exception);

        if (form.getConfirmPassword() == null || form.getConfirmPassword().isBlank()) {
            exception.addFieldError("confirmPassword", "Xác nhận mật khẩu không được để trống");
        } else if (form.getNewPassword() != null
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            exception.addFieldError("confirmPassword", "Mật khẩu xác nhận không khớp");
        }

        if (form.getNewPassword() != null
                && currentUser.getPasswordHash() != null
                && passwordEncoder.matches(form.getNewPassword(), currentUser.getPasswordHash())) {
            exception.addFieldError("newPassword", "Mật khẩu mới phải khác mật khẩu cũ");
        }

        if (exception.hasErrors()) {
            throw exception;
        }
    }

    private void validatePasswordStrength(String newPassword,
                                          ProfileValidationException exception) {

        if (newPassword == null || newPassword.isBlank()) {
            exception.addFieldError("newPassword", "Mật khẩu mới không được để trống");
            return;
        }

        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,50}$";

        if (!newPassword.matches(passwordPattern)) {
            exception.addFieldError(
                    "newPassword",
                    "Mật khẩu mới phải từ 8 đến 50 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt"
            );
        }
    }

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest form) {
        User user = getUserOrThrow(userId);

        Country country = updateCountryAndIdentity(user, form);

        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setPhone(buildPhoneNumberForSaving(form, country));
        user.setGender(form.getGender());
        user.setBirthYear(form.getBirthYear());
        updateAvatarIfPresent(user, form.getAvatarFile());

        userRepository.save(user);
    }

    private Country updateCountryAndIdentity(User user, ProfileUpdateRequest form) {
        Country country = countryRepository.findById(form.getCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Quốc gia không tồn tại"));

        user.setCountry(country);

        if (isVietnamCountry(country)) {
            user.setIdentityType(IdentityType.CCCD);
        } else {
            user.setIdentityType(IdentityType.PASSPORT);
        }

        user.setIdentityNumber(form.getIdentityNumber().trim().toUpperCase());

        return country;
    }

    private void updateAvatarIfPresent(User user, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return;
        }

        String avatarUrl = cloudinaryService.uploadAvatar(avatarFile);
        user.setAvatarUrl(avatarUrl);
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

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        return phone.trim().replaceAll("[\\s\\-]", "");
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

    private String buildPhoneNumberForSaving(ProfileUpdateRequest form, Country country) {
        String normalizedPhone = normalizePhone(form.getPhone());

        if (isVietnamCountry(country)) {
            return normalizedPhone;
        }

        String phoneCode = form.getPhoneCode();

        if (phoneCode == null || phoneCode.isBlank()) {
            return normalizedPhone;
        }

        normalizedPhone = normalizedPhone.replaceFirst("^0+", "");

        return phoneCode.trim() + normalizedPhone;
    }

    private String toDisplayPhone(String storedPhone, Country country) {
        if (storedPhone == null || storedPhone.isBlank()) {
            return storedPhone;
        }

        String normalizedPhone = normalizePhone(storedPhone);

        if (country == null || isVietnamCountry(country)) {
            return normalizedPhone;
        }

        String phoneCode = country.getPhoneCode();

        if (phoneCode == null || phoneCode.isBlank()) {
            return normalizedPhone;
        }

        String normalizedPhoneCode = normalizePhone(phoneCode);

        if (normalizedPhone.startsWith(normalizedPhoneCode)) {
            return normalizedPhone.substring(normalizedPhoneCode.length());
        }

        return normalizedPhone;
    }

    private String resolvePhoneCode(ProfileUpdateRequest form, Country country) {
        if (form.getPhoneCode() != null && !form.getPhoneCode().isBlank()) {
            return form.getPhoneCode().trim();
        }

        if (country != null && country.getPhoneCode() != null) {
            return country.getPhoneCode().trim();
        }

        return null;
    }


}