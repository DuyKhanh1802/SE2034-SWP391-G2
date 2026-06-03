package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Country;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRoleId;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RegisterRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CountryRepository countryRepository;
    private final RoleRepository roleRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAllByOrderByCountryNameAsc();
    }

    public void register(RegisterRequest request) {
        if (!request.getPasswordHash().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu không hợp lệ");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký");
        }

        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Quốc gia không hợp lệ"));

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCountry(country);
        user.setPasswordHash(passwordEncoder.encode(request.getPasswordHash()));

        // Save user trước để có id
        User savedUser = userRepository.save(user);

        // Tạo UserRole với id đầy đủ
        Role guestRole = roleRepository.findByRoleName(RoleName.GUEST);
        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(savedUser.getId(), guestRole.getId()));
        userRole.setUser(savedUser);
        userRole.setRole(guestRole);

        savedUser.getUserRoles().add(userRole);
        userRepository.save(savedUser);
    }
}