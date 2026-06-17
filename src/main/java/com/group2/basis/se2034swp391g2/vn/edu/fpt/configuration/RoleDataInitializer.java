package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Role;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoleDataInitializer {

    // Tạo sẵn các quyền mặc định nếu bảng roles đang trống hoặc thiếu role.
    @Bean
    public CommandLineRunner seedDefaultRoles(RoleRepository roleRepository) {
        return args -> {
            for (RoleName roleName : RoleName.values()) {
                if (roleRepository.findByRoleName(roleName) == null) {
                    Role role = new Role();
                    role.setRoleName(roleName);
                    roleRepository.save(role);
                }
            }
        };
    }
}
