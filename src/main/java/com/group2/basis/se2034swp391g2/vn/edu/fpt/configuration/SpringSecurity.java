package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.RoleSwitchController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Set;

@Configuration
public class SpringSecurity {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        // 1. Nhóm các đường dẫn Public (Ai cũng vào được)
                        .requestMatchers(
                                "/",
                                "/home",
                                "/room-types",
                                "/services",
                                "/error",
                                "/common/**",
                                "/auth/**",
                                "/page/**",
                                "/fragment/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/system_admin/**",
                                "/hotel_admin/**",
                                "/Admin/**",
                                "/guest/**"
                        ).permitAll()

                        // 2. Nhóm quyền SYSTEM_ADMIN
                        .requestMatchers("/profile/**", "/api/user/switch-role").authenticated()
                        .requestMatchers("/system-admin/**").hasRole("SYSTEM_ADMIN")

                        // 3. Nhóm quyền HOTEL_ADMIN
                        .requestMatchers(
                                "/hotel-admin/dashboard",
                                "/hotel-admin/list-room/**",
                                "/hotel-admin/rooms/**",
                                "/hotel-admin/room-images/**",
                                "/hotel-admin/services/**",
                                "/hotel-admin/promotions/**",
                                "/hotel-admin/promotion-images/**"
                        ).hasRole("HOTEL_ADMIN")

                        // 4. Các quyền khác
                        .requestMatchers("/hotel-admin/**").hasRole("HOTEL_ADMIN")
                        .requestMatchers("/receptionist/**").hasRole("RECEPTIONIST")
                        .requestMatchers("/storekeeper/**").hasRole("STOREKEEPER")
                        .requestMatchers("/manager/**").hasRole("MANAGER")

                        // 5. Mọi request khác đều phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .successHandler(cusAuthenticationSuccessHandler())
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                /*
                 * Bỏ CSRF cho API upload ảnh khuyến mãi.
                 */
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/hotel-admin/promotion-images/upload")

                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler cusAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            RoleName activeRole = RoleSwitchController.resolveDefaultActiveRole(roles);
            request.getSession(true).setAttribute(RoleSwitchController.CURRENT_ACTIVE_ROLE, activeRole.name());
            request.getSession(true).setAttribute(RoleSwitchController.CURRENT_ACTIVE_ROLE_LABEL, RoleSwitchController.getRoleLabel(activeRole));
            request.getSession(true).setAttribute(RoleSwitchController.AVAILABLE_ACTIVE_ROLE_OPTIONS, RoleSwitchController.getAvailableActiveRoleOptions(roles));
            response.sendRedirect(RoleSwitchController.getDashboardPath(activeRole));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
