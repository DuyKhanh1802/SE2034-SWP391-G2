package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.exception.EmailVerificationResentException;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Country;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRoleId;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RegisterRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UserRepository userRepository,
                       CountryRepository countryRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder,
                       EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.countryRepository = countryRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    @Transactional(dontRollbackOn = EmailVerificationResentException.class)
    public void register(RegisterRequest request) {
        validateRegisterRequest(request);

        String email = normalizeEmail(request.getEmail());

        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null) {
            handleExistingUser(existingUser);
            return;
        }

        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Quốc gia không hợp lệ."));

        Role guestRole = findGuestRole();

        User user = new User();

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPasswordHash()));
        user.setCountry(country);

        user.setUserType(UserType.GUEST);
        user.setApprovalStatus(ApprovalStatus.APPROVED);

        user.setTotalStays(0);
        user.setTotalSpent(BigDecimal.ZERO);

        /*
         * Quan trọng:
         * Tài khoản mới đăng ký chưa được đăng nhập ngay.
         * Sau khi nhập đúng OTP thì EmailVerificationService mới set isActive = true.
         */
        user.setIsActive(false);
        user.setIsDeleted(false);

        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);

        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(savedUser.getId());
        userRoleId.setRoleId(guestRole.getId());

        UserRole userRole = new UserRole();
        userRole.setId(userRoleId);
        userRole.setUser(savedUser);
        userRole.setRole(guestRole);
        userRole.setAssignedAt(Instant.now());
        userRole.setAssignedBy(null);

        userRoleRepository.save(userRole);

        /*
         * Gửi OTP xác thực tài khoản đăng ký.
         * Không dùng createAndSendVerificationToken nữa.
         */
        emailVerificationService.createAndSendVerificationOtp(savedUser);
    }

    private void handleExistingUser(User existingUser) {
        if (Boolean.TRUE.equals(existingUser.getIsDeleted())) {
            throw new IllegalArgumentException("Email này thuộc tài khoản đã bị vô hiệu hóa.");
        }

        if (Boolean.TRUE.equals(existingUser.getIsActive())) {
            throw new IllegalArgumentException("Email đã tồn tại trong hệ thống.");
        }

        /*
         * Email đã có user nhưng chưa xác thực.
         * Gửi lại OTP mới thay vì tạo user mới.
         */
        emailVerificationService.createAndSendVerificationOtp(existingUser);

        throw new EmailVerificationResentException(
                "Email này đã được đăng ký nhưng chưa xác thực. Hệ thống đã gửi lại mã OTP xác thực."
        );
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thông tin đăng ký không hợp lệ.");
        }

        if (isBlank(request.getFirstName())) {
            throw new IllegalArgumentException("Tên không được để trống.");
        }

        if (isBlank(request.getLastName())) {
            throw new IllegalArgumentException("Họ không được để trống.");
        }

        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException("Email không được để trống.");
        }

        String email = normalizeEmail(request.getEmail());

        if (!email.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            throw new IllegalArgumentException("Email phải đúng định dạng @gmail.com.");
        }

        if (request.getCountryId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn quốc gia.");
        }

        if (isBlank(request.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }

        if (isBlank(request.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }

        if (request.getPasswordHash().length() < 8) {
            throw new IllegalArgumentException("Mật khẩu phải có tối thiểu 8 ký tự.");
        }

        if (!request.getPasswordHash().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$")) {
            throw new IllegalArgumentException("Mật khẩu phải có chữ hoa, chữ thường, số và ký tự đặc biệt.");
        }

        if (isBlank(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu không được để trống.");
        }

        if (!request.getPasswordHash().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Confirm password không khớp với password.");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Role findGuestRole() {
        return roleRepository.findByRoleName(RoleName.GUEST)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền GUEST."));
    }
}