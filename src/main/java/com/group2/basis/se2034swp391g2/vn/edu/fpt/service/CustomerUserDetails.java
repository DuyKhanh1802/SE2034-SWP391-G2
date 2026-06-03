package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CustomerUserDetails implements UserDetails {
    private final User user;

    public CustomerUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getUserRoles().stream()
                .map(userRole -> {
                    // 1. Lấy đối tượng Role thật từ bảng trung gian
                    Role role = userRole.getRole();

                    // 2. Lấy giá trị chuỗi của Enum RoleName (Ví dụ: "ADMIN", "MANAGER", "GUEST")
                    String roleString = role.getRoleName().name();

                    // 3. Ghép chuỗi tạo thành quyền hoàn chỉnh cho Spring Security (Ví dụ: "ROLE_ADMIN")
                    return new SimpleGrantedAuthority("ROLE_" + roleString);
                })
                .collect(Collectors.toList());
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
