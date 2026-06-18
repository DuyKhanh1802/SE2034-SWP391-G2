package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class CustomerUserDetails implements UserDetails {
    private final User user;

    public CustomerUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        user.getUserRoles().stream()
                .map(userRole -> userRole.getRole())
                .filter(role -> role != null && role.getRoleName() != null)
                .forEach(role -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName().name()));
                    role.getRolePermissions().stream()
                            .map(rolePermission -> rolePermission.getPermission())
                            .filter(permission -> permission != null
                                    && permission.getCode() != null
                                    && !permission.getCode().isBlank())
                            .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                            .forEach(authorities::add);
                });

        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(user.getIsActive());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getApprovalStatus() == ApprovalStatus.APPROVED;
    }

    public User getUser() {
        return user;
    }
}
