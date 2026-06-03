package com.group2.basis.se2034swp391g2.vn.edu.fpt.service.impl;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRoleId;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountCreateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRoleRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAllStaffUsers() {
        return userRepository.findByUserTypeAndIsDeletedFalse(UserType.STAFF);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    @Transactional
    public void createUser(AccountCreateRequest request) {
        // BR-24: Tài khoản STAFF bắt buộc phải có email và password_hash
        if (request.getEmail() == null || request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Staff accounts must have a non-null email and password.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPhone(request.getPhone());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setUserType(UserType.STAFF);

        // BR-23: New staff accounts must be created with approval_status = PENDING
        newUser.setApprovalStatus(ApprovalStatus.PENDING);

        newUser.setIsActive(true);
        newUser.setIsDeleted(false);
        newUser.setCreatedAt(Instant.now());
        newUser.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(newUser);

        // Gắn quyền (Role)
        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(savedUser.getId(), role.getId()));
        userRole.setUser(savedUser);
        userRole.setRole(role);

        // BR-27: Each role assignment records assigned_at and optionally assigned_by
        userRole.setAssignedAt(Instant.now());
        if (request.getCurrentAdminId() != null) {
            User adminUser = getUserById(request.getCurrentAdminId());
            userRole.setAssignedBy(adminUser);
        }

        userRoleRepository.save(userRole);
    }

    @Override
    @Transactional
    public void updateUser(Long id, AccountUpdateRequest request) {
        User existingUser = getUserById(id);

        existingUser.setFirstName(request.getFirstName());
        existingUser.setLastName(request.getLastName());
        existingUser.setPhone(request.getPhone());
        existingUser.setUpdatedAt(Instant.now());

        // BR-25: Staff accounts must not be permanently deleted. Instead, is_active is set to 0 or is_deleted is set
        if (request.getIsActive() != null) {
            existingUser.setIsActive(request.getIsActive());
            if (!request.getIsActive()) {
                existingUser.setIsDeleted(true);
            } else {
                existingUser.setIsDeleted(false);
            }
        }

        // BR-23: Xử lý Review (Approve/Reject)
        if (request.getApprovalStatus() != null && request.getApprovalStatus() != existingUser.getApprovalStatus()) {

            // Nếu REJECTED, bắt buộc phải có approval_note
            if (request.getApprovalStatus() == ApprovalStatus.REJECTED) {
                if (request.getApprovalNote() == null || request.getApprovalNote().trim().isEmpty()) {
                    throw new IllegalArgumentException("approval_note is mandatory when rejecting an account.");
                }
                existingUser.setApprovalNote(request.getApprovalNote());
            } else if (request.getApprovalStatus() == ApprovalStatus.APPROVED) {
                existingUser.setApprovalNote(null); // Clear note nếu được approve
            }

            existingUser.setApprovalStatus(request.getApprovalStatus());
            existingUser.setReviewedAt(Instant.now());

            if (request.getCurrentAdminId() != null) {
                User adminUser = getUserById(request.getCurrentAdminId());
                existingUser.setReviewedBy(adminUser.getId().intValue()); // Lưu ID người review
            }
        }

        userRepository.save(existingUser);

        // Xử lý đổi Role (BR-26, BR-27)
        if (request.getRoleName() != null && !request.getRoleName().isEmpty()) {
            Role newRole = roleRepository.findByRoleName(request.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            UserRole existingUserRole = userRoleRepository.findByUserId(existingUser.getId()).orElse(null);

            if (existingUserRole == null || !existingUserRole.getRole().getId().equals(newRole.getId())) {
                userRoleRepository.deleteByUserId(existingUser.getId());

                UserRole newUserRole = new UserRole();
                newUserRole.setId(new UserRoleId(existingUser.getId(), newRole.getId()));
                newUserRole.setUser(existingUser);
                newUserRole.setRole(newRole);

                // BR-27: Lưu vết người gán quyền mới
                newUserRole.setAssignedAt(Instant.now());
                if (request.getCurrentAdminId() != null) {
                    User adminUser = getUserById(request.getCurrentAdminId());
                    newUserRole.setAssignedBy(adminUser);
                }

                userRoleRepository.save(newUserRole);
            }
        }
    }
}