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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_APPROVAL_NOTE_LENGTH = 150;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

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

            predicates.add(cb.isFalse(root.get("isDeleted")));

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
        return userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
    }

    @Override
    @Transactional
    public void updateUser(Long id, AccountUpdateRequest request) {
        User existingUser = getUserById(id);
        validateSelfAccountUpdate(existingUser, request);

        if (Boolean.TRUE.equals(request.getIsActive())
                && existingUser.getApprovalStatus() == ApprovalStatus.REJECTED
                && request.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Không thể kích hoạt tài khoản đã bị từ chối. Hãy duyệt tài khoản trước.");
        }

        if (request.getIsActive() != null) {
            if (Boolean.FALSE.equals(request.getIsActive())) {
                validateLastActiveSystemAdmin(existingUser, null);
            }
            existingUser.setIsActive(request.getIsActive());
        }

        if (request.getApprovalStatus() != null) {
            validateApprovalStatus(request.getApprovalStatus());
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
            Role newRole = validateSingleRole(request.getRoleId());
            if (existingUser.getApprovalStatus() == ApprovalStatus.REJECTED) {
                throw new IllegalArgumentException("Không thể thay đổi vai trò của tài khoản đã bị từ chối.");
            }
            validateLastActiveSystemAdmin(existingUser, newRole.getRoleName());

            existingUser.setUserType(resolveUserType(newRole.getRoleName()));
            existingUser.setUpdatedAt(Instant.now());
            userRepository.saveAndFlush(existingUser);

            userRoleRepository.deleteByUserId(existingUser.getId());
            userRoleRepository.flush();
            entityManager.clear();

            User assignedBy = request.getCurrentAdminId() != null
                    ? entityManager.getReference(User.class, request.getCurrentAdminId())
                    : null;

            UserRole newUserRole = new UserRole();
            newUserRole.setId(new UserRoleId(id, newRole.getId()));
            newUserRole.setUser(entityManager.getReference(User.class, id));
            newUserRole.setRole(entityManager.getReference(Role.class, newRole.getId()));
            newUserRole.setAssignedAt(Instant.now());
            newUserRole.setAssignedBy(assignedBy);
            userRoleRepository.save(newUserRole);
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
            if (approvalNote.length() > MAX_APPROVAL_NOTE_LENGTH) {
                throw new IllegalArgumentException("Lý do từ chối không được vượt quá "
                        + MAX_APPROVAL_NOTE_LENGTH + " ký tự.");
            }
            validateLastActiveSystemAdmin(existingUser, null);
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

    private void validateApprovalStatus(ApprovalStatus approvalStatus) {
        if (approvalStatus != ApprovalStatus.APPROVED && approvalStatus != ApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Thao tác duyệt tài khoản không hợp lệ.");
        }
    }

    private Role validateSingleRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Vui lòng chọn một vai trò.");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không hợp lệ."));

        if (role.getRoleName() == null) {
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }

        return role;
    }

    private UserType resolveUserType(RoleName roleName) {
        return roleName == RoleName.GUEST ? UserType.GUEST : UserType.STAFF;
    }

    private void validateLastActiveSystemAdmin(User existingUser, RoleName targetRoleName) {
        if (!Boolean.TRUE.equals(existingUser.getIsActive()) || !hasRole(existingUser, RoleName.SYSTEM_ADMIN)) {
            return;
        }

        if (targetRoleName == RoleName.SYSTEM_ADMIN) {
            return;
        }

        if (userRepository.countActiveUsersByRoleName(RoleName.SYSTEM_ADMIN) <= 1) {
            throw new IllegalArgumentException("Phải giữ lại ít nhất một tài khoản Quản trị Hệ thống đang hoạt động.");
        }
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getUserRoles() != null
                && user.getUserRoles().stream()
                .map(UserRole::getRole)
                .anyMatch(role -> role != null && role.getRoleName() == roleName);
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
