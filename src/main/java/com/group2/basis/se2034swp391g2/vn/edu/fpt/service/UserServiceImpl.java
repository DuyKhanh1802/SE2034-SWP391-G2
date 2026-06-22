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
import jakarta.persistence.criteria.Predicate;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
    public Page<User> getAccountPage(String keyword,
                                     RoleName role,
                                     String activeStatus,
                                     ApprovalStatus approvalStatus,
                                     Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.trim().isEmpty()
                ? null
                : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        Specification<User> specification = (root, query, cb) -> {
            query.distinct(true);

            Join<User, UserRole> userRoleJoin = root.join("userRoles", JoinType.LEFT);
            Join<UserRole, Role> roleJoin = userRoleJoin.join("role", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();

            if (normalizedKeyword != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), normalizedKeyword),
                        cb.like(cb.lower(root.get("lastName")), normalizedKeyword),
                        cb.like(
                                cb.lower(
                                        cb.concat(
                                                cb.concat(root.get("lastName"), " "),
                                                root.get("firstName")
                                        )
                                ),
                                normalizedKeyword
                        )
                ));
            }

            if (role != null) {
                predicates.add(cb.equal(roleJoin.get("roleName"), role));
            }

            if ("ACTIVE".equalsIgnoreCase(activeStatus)) {
                predicates.add(cb.isTrue(root.get("isActive")));
            } else if ("INACTIVE".equalsIgnoreCase(activeStatus)) {
                predicates.add(cb.isFalse(root.get("isActive")));
            }

            if (approvalStatus != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
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
        validateSelfAccountUpdate(existingUser, request);

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

        if (Boolean.TRUE.equals(request.getIsActive())
                && existingUser.getApprovalStatus() == ApprovalStatus.REJECTED
                && request.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Không thể kích hoạt tài khoản đã bị từ chối. Hãy duyệt tài khoản trước.");
        }

        if (request.getIsActive() != null) {
            existingUser.setIsActive(request.getIsActive());
        }

        if (request.getApprovalStatus() != null) {
            if (existingUser.getApprovalStatus() != ApprovalStatus.PENDING) {
                throw new IllegalArgumentException("Tài khoản này đã được xử lý phê duyệt trước đó.");
            }
            applyApprovalDecision(existingUser, request);
            existingUser.setReviewedAt(Instant.now());

            if (request.getCurrentAdminId() != null) {
                User adminUser = getUserById(request.getCurrentAdminId());
                existingUser.setReviewedBy(adminUser);
            }
        }

        userRepository.save(existingUser);

        if (Boolean.TRUE.equals(request.getRoleUpdateRequested())) {
            List<Long> submittedRoleIds = request.getRoleIds() == null ? List.of() : request.getRoleIds();
            List<Long> requestedRoleIds = submittedRoleIds.stream()
                    .filter(roleId -> roleId != null)
                    .toList();
            List<Long> roleIds = requestedRoleIds.stream()
                    .distinct()
                    .toList();

            List<Role> newRoles = roleRepository.findAllById(roleIds);
            validateRoleCombination(requestedRoleIds, newRoles);

            Set<RoleName> roleNames = newRoles.stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toSet());

            existingUser.setUserType(resolveUserType(roleNames));
            existingUser.setUpdatedAt(Instant.now());
            userRepository.saveAndFlush(existingUser);

            userRoleRepository.deleteByUserId(existingUser.getId());
            userRoleRepository.flush();
            entityManager.clear();

            User assignedBy = request.getCurrentAdminId() != null
                    ? entityManager.getReference(User.class, request.getCurrentAdminId())
                    : null;

            for (Role role : newRoles) {
                UserRole newUserRole = new UserRole();
                newUserRole.setId(new UserRoleId(id, role.getId()));
                newUserRole.setUser(entityManager.getReference(User.class, id));
                newUserRole.setRole(entityManager.getReference(Role.class, role.getId()));
                newUserRole.setAssignedAt(Instant.now());
                newUserRole.setAssignedBy(assignedBy);
                userRoleRepository.save(newUserRole);
            }
        }
    }

    private void applyApprovalDecision(User existingUser, AccountUpdateRequest request) {
        ApprovalStatus approvalStatus = request.getApprovalStatus();

        if (approvalStatus == ApprovalStatus.REJECTED) {
            String approvalNote = request.getApprovalNote() == null
                    ? ""
                    : request.getApprovalNote().trim();
            if (approvalNote.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối tài khoản.");
            }
            existingUser.setApprovalNote(approvalNote);
            existingUser.setIsActive(false);
        } else if (approvalStatus == ApprovalStatus.APPROVED) {
            existingUser.setApprovalNote(null);
            existingUser.setIsActive(true);
        } else {
            existingUser.setApprovalNote(null);
        }

        existingUser.setApprovalStatus(approvalStatus);
    }

    private void validateRoleCombination(List<Long> requestedRoleIds, List<Role> roles) {
        if (requestedRoleIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một vai trò.");
        }

        int distinctRoleCount = new HashSet<>(requestedRoleIds).size();
        if (distinctRoleCount != requestedRoleIds.size()) {
            throw new IllegalArgumentException("Không được chọn trùng vai trò.");
        }

        if (roles.size() != distinctRoleCount) {
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }

        Set<RoleName> roleNames = roles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toCollection(HashSet::new));

        if (roleNames.contains(RoleName.GUEST) && roleNames.size() > 1) {
            throw new IllegalArgumentException("Khách hàng không thể có thêm vai trò nhân viên.");
        }

        if (roleNames.contains(RoleName.SYSTEM_ADMIN) && roleNames.size() > 1) {
            throw new IllegalArgumentException("Quản trị hệ thống không thể kiêm nhiệm vai trò khác.");
        }

        boolean hasHotelAdmin = roleNames.contains(RoleName.HOTEL_ADMIN);
        boolean hasManager = roleNames.contains(RoleName.MANAGER);
        boolean hasStorekeeper = roleNames.contains(RoleName.STOREKEEPER);
        boolean hasReceptionist = roleNames.contains(RoleName.RECEPTIONIST);

        if (!hasHotelAdmin
                && !hasManager
                && !hasStorekeeper
                && !hasReceptionist
                && !roleNames.contains(RoleName.GUEST)
                && !roleNames.contains(RoleName.SYSTEM_ADMIN)) {
            throw new IllegalArgumentException("Tổ hợp vai trò không hợp lệ.");
        }
    }

    private UserType resolveUserType(Set<RoleName> roleNames) {
        return roleNames.size() == 1 && roleNames.contains(RoleName.GUEST) ? UserType.GUEST : UserType.STAFF;
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

    private void validateSelfAccountUpdate(User existingUser, AccountUpdateRequest request) {
        if (request.getCurrentAdminId() == null || existingUser.getId() == null) {
            return;
        }

        if (request.getCurrentAdminId().equals(existingUser.getId())) {
            throw new IllegalArgumentException("Không thể chỉnh sửa tài khoản đang đăng nhập.");
        }
    }
}
