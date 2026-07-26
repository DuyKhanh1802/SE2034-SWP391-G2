package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRoleId;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BookingGuestAccountService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public User findOrCreateGuest(String firstName,
                                  String lastName,
                                  String email,
                                  String phone,
                                  String identityNumber) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalize(phone);
        String normalizedIdentityNumber = normalize(identityNumber);

        User existingUser = findExistingUser(
                normalizedEmail,
                normalizedPhone,
                normalizedIdentityNumber
        );

        if (existingUser != null) {
            if (existingUser.getUserType() == UserType.GUEST) {
                existingUser.setApprovalStatus(ApprovalStatus.APPROVED);
                existingUser.setApprovalNote(null);
                existingUser.setIsActive(true);
                existingUser = userRepository.save(existingUser);
                ensureGuestRole(existingUser);
            }
            return existingUser;
        }

        User guest = new User();
        guest.setFirstName(normalizeRequired(firstName, "Tên khách không hợp lệ."));
        guest.setLastName(normalizeRequired(lastName, "Họ khách không hợp lệ."));
        guest.setEmail(normalizedEmail);
        guest.setPhone(normalizedPhone);
        guest.setPasswordHash(null);
        guest.setUserType(UserType.GUEST);
        guest.setApprovalStatus(ApprovalStatus.APPROVED);
        guest.setApprovalNote(null);
        guest.setIsActive(true);
        guest.setIsDeleted(false);
        guest.setCreatedAt(Instant.now());
        guest.setUpdatedAt(Instant.now());

        User savedGuest = userRepository.save(guest);
        ensureGuestRole(savedGuest);
        return savedGuest;
    }

    private User findExistingUser(String email, String phone, String identityNumber) {
        if (!identityNumber.isEmpty()) {
            User user = userRepository.findByIdentityNumberAndIsDeletedFalse(identityNumber).orElse(null);
            if (user != null) {
                return user;
            }
        }

        if (!email.isEmpty()) {
            User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
            if (user != null) {
                return user;
            }
        }

        if (!phone.isEmpty()) {
            return userRepository.findByPhoneAndIsDeletedFalse(phone).orElse(null);
        }

        return null;
    }

    private void ensureGuestRole(User user) {
        boolean hasGuestRole = user.getUserRoles() != null
                && user.getUserRoles().stream()
                .map(UserRole::getRole)
                .anyMatch(role -> role != null && role.getRoleName() == RoleName.GUEST);

        if (hasGuestRole) {
            return;
        }

        Role guestRole = roleRepository.findByRoleName(RoleName.GUEST)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò GUEST."));

        userRoleRepository.deleteByUserId(user.getId());
        userRoleRepository.flush();

        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getId(), guestRole.getId()));
        userRole.setUser(user);
        userRole.setRole(guestRole);
        userRole.setAssignedAt(Instant.now());
        userRole.setAssignedBy(null);
        userRoleRepository.save(userRole);
    }

    private String normalizeEmail(String value) {
        return normalize(value).toLowerCase();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeRequired(String value, String message) {
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalizedValue;
    }
}
