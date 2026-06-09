package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRole;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.UserRoleId;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request.AccountUpdateRequest;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.UserRoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<User> getAccountPage(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }

        String normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        Specification<User> specification = (root, query, cb) -> {
            query.distinct(true);

            Join<User, UserRole> userRoleJoin = root.join("userRoles", JoinType.LEFT);
            Join<UserRole, Role> roleJoin = userRoleJoin.join("role", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), normalizedKeyword),
                    cb.like(cb.lower(root.get("lastName")), normalizedKeyword),
                    cb.like(cb.lower(root.get("email")), normalizedKeyword),
                    cb.like(cb.lower(root.get("phone")), normalizedKeyword),
                    cb.like(cb.lower(root.get("identityNumber")), normalizedKeyword),
                    cb.like(cb.lower(root.get("userType").as(String.class)), normalizedKeyword),
                    cb.like(cb.lower(roleJoin.get("roleName").as(String.class)), normalizedKeyword)
            );
        };

        return userRepository.findAll(specification, pageable);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    @Transactional
    public void updateUser(Long id, AccountUpdateRequest request) {
        User existingUser = getUserById(id);

        if (request.getFirstName() != null) {
            existingUser.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            existingUser.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            existingUser.setPhone(request.getPhone());
        }
        existingUser.setUpdatedAt(Instant.now());

        if (request.getIsActive() != null) {
            existingUser.setIsActive(request.getIsActive());
        }

        if (request.getApprovalStatus() != null && request.getApprovalStatus() != existingUser.getApprovalStatus()) {
            if (request.getApprovalStatus() == ApprovalStatus.REJECTED) {
                if (request.getApprovalNote() == null || request.getApprovalNote().trim().isEmpty()) {
                    throw new IllegalArgumentException("approval_note is mandatory when rejecting an account.");
                }
                existingUser.setApprovalNote(request.getApprovalNote());
            } else if (request.getApprovalStatus() == ApprovalStatus.APPROVED) {
                existingUser.setApprovalNote(null);
            }

            existingUser.setApprovalStatus(request.getApprovalStatus());
            existingUser.setReviewedAt(Instant.now());

            if (request.getCurrentAdminId() != null) {
                User adminUser = getUserById(request.getCurrentAdminId());
                existingUser.setReviewedBy(adminUser);
            }
        }

        userRepository.save(existingUser);

        if (request.getRoleName() != null && !request.getRoleName().trim().isEmpty()) {
            RoleName roleEnum;
            try {
                roleEnum = RoleName.valueOf(request.getRoleName().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role name provided: " + request.getRoleName());
            }

            Role newRole = roleRepository.findByRoleName(roleEnum);
            if (newRole == null) {
                throw new RuntimeException("Role not found in database: " + roleEnum);
            }

            List<UserRole> currentUserRoles = userRoleRepository.findAllByUserId(existingUser.getId());
            boolean alreadyHasOnlyRequestedRole = currentUserRoles.size() == 1
                    && currentUserRoles.getFirst().getRole() != null
                    && currentUserRoles.getFirst().getRole().getId().equals(newRole.getId());

            existingUser.setUserType(resolveUserType(roleEnum));
            existingUser.setUpdatedAt(Instant.now());
            userRepository.saveAndFlush(existingUser);

            if (alreadyHasOnlyRequestedRole) {
                return;
            }

            userRoleRepository.deleteByUserId(existingUser.getId());
            userRoleRepository.flush();
            entityManager.clear();

            UserRole newUserRole = new UserRole();
            newUserRole.setId(new UserRoleId(id, newRole.getId()));
            newUserRole.setUser(entityManager.getReference(User.class, id));
            newUserRole.setRole(entityManager.getReference(Role.class, newRole.getId()));
            newUserRole.setAssignedAt(Instant.now());

            if (request.getCurrentAdminId() != null) {
                newUserRole.setAssignedBy(entityManager.getReference(User.class, request.getCurrentAdminId()));
            }

            userRoleRepository.save(newUserRole);
        }
    }

    private UserType resolveUserType(RoleName roleName) {
        return roleName == RoleName.GUEST ? UserType.GUEST : UserType.STAFF;
    }

    @Override
    @Transactional
    public String resetPassword(Long id) {
        User existingUser = getUserById(id);
        String temporaryPassword = generateTemporaryPassword();

        existingUser.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        existingUser.setUpdatedAt(Instant.now());
        userRepository.save(existingUser);

        return temporaryPassword;
    }

    private String generateTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder password = new StringBuilder("Vh@");

        for (int i = 0; i < 9; i++) {
            password.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }

        return password.toString();
    }
}
