package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByIdentityNumber(String identityNumber);
    Optional<User> findByPhone(String phone);

    // Lấy danh sách tài khoản nhân viên (Không lấy GUEST) và chưa bị xóa
    List<User> findByUserTypeAndIsDeletedFalse(UserType userType);

    // Kiểm tra trùng lặp khi tạo mới
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}