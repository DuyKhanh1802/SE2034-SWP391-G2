package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Country;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.RegisterRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CountryRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService  {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Thêm cái nà
    private final CountryRepository countryRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAll(Sort.by(Sort.Direction.ASC, "countryName"));
    }

    public void register(RegisterRequest request) {
        // Lỗi 1 đã sửa: dùng .isPresent()

        if (!request.getPasswordHash().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu không khớp");
        }

        if(userRepository.findByPhone(request.getPhone()).isPresent()){
            throw new IllegalArgumentException("Phone đã được đăng ký");
        }
        if(userRepository.findByIdentityCard(request.getIdentityCard()).isPresent()){
            throw new IllegalArgumentException("CCCD/Passport đã được đăng ký");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {

            throw new IllegalArgumentException("Email đã được đăng ký");
        }
        Country country = countryRepository.findById(Long.valueOf(request.getNationalityId()))
                .orElseThrow(() ->
                        new IllegalArgumentException("Quốc tịch không hợp lệ"));
        // Lỗi 2 đã sửa: build đầy đủ
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .identityCard(request.getIdentityCard())
                .nationality(country)
                .passwordHash(passwordEncoder.encode(request.getPasswordHash()))
                .build();

        User saved = userRepository.save(user);
    }
}