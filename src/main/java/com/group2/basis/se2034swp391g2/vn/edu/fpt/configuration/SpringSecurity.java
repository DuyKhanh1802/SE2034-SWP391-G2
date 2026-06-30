package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.constants.PermissionCode;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.RoleSwitchController;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SpringSecurity {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        // 1. Nhóm các đường dẫn Public (Ai cũng vào được)
                        .requestMatchers(
                                "/",
                                "/home",
                                "/room-types",
                                "/overview",
                                "/offers",
                                "/accommodation",
                                "/services",
                                "/services/**",
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
                                "/guest/**"
                        ).permitAll()

                        .requestMatchers("/api/user/switch-role").authenticated()
                        .requestMatchers(HttpMethod.GET, "/profile/*.css").permitAll()
                        .requestMatchers(HttpMethod.GET, "/profile/edit").permitAll()
                        .requestMatchers(HttpMethod.POST, "/profile/update").permitAll()
                        .requestMatchers(HttpMethod.GET, "/profile/**").hasAuthority(PermissionCode.PROFILE_VIEW)
                        .requestMatchers(HttpMethod.POST, "/profile/**").hasAuthority(PermissionCode.PROFILE_EDIT)

                        .requestMatchers("/system-admin/role-permissions/**")
                        .hasAuthority(PermissionCode.ROLE_PERMISSION_MANAGE)
                        .requestMatchers(HttpMethod.POST, "/system-admin/list-user/*/review")
                        .hasAuthority(PermissionCode.USER_APPROVE)
                        .requestMatchers(HttpMethod.GET, "/system-admin/list-user/*/edit")
                        .hasAnyAuthority(
                                PermissionCode.USER_MANAGE,
                                PermissionCode.USER_APPROVE
                        )
                        .requestMatchers(HttpMethod.POST, "/system-admin/list-user/**")
                        .hasAuthority(PermissionCode.USER_MANAGE)
                        .requestMatchers(HttpMethod.GET, "/system-admin/**")
                        .hasAuthority(PermissionCode.USER_VIEW)

                        .requestMatchers("/hotel-admin/dashboard")
                        .hasAuthority(PermissionCode.HOTEL_DASHBOARD_VIEW)
                        .requestMatchers(HttpMethod.GET, "/hotel-admin/promotions/**")
                        .hasAuthority(PermissionCode.PROMOTION_VIEW)
                        .requestMatchers(HttpMethod.POST, "/hotel-admin/promotions/**",
                                "/hotel-admin/promotion-images/**")
                        .hasAuthority(PermissionCode.PROMOTION_MANAGE)
                        .requestMatchers(HttpMethod.GET, "/hotel-admin/**")
                        .hasAuthority(PermissionCode.ROOM_VIEW)
                        .requestMatchers(HttpMethod.POST, "/hotel-admin/**")
                        .hasAuthority(PermissionCode.ROOM_MANAGE)

                        .requestMatchers("/manager/dashboard")
                        .hasAuthority(PermissionCode.MANAGER_DASHBOARD_VIEW)
                        .requestMatchers(HttpMethod.GET, "/manager/reports/**")
                        .hasAuthority(PermissionCode.REPORT_VIEW)
                        .requestMatchers(HttpMethod.GET, "/manager/**")
                        .hasAuthority(PermissionCode.FINANCE_VIEW)
                        .requestMatchers(HttpMethod.POST, "/manager/**")
                        .hasAuthority(PermissionCode.FINANCE_MANAGE)

                        .requestMatchers(HttpMethod.GET, "/storekeeper/**")
                        .hasAuthority(PermissionCode.INVENTORY_VIEW)
                        .requestMatchers(HttpMethod.POST, "/storekeeper/**")
                        .hasAuthority(PermissionCode.INVENTORY_MANAGE)

                        .requestMatchers("/receptionist/dashboard")
                        .hasAuthority(PermissionCode.RECEPTION_DASHBOARD_VIEW)
                        .requestMatchers(HttpMethod.POST, "/receptionist/check-in/**",
                                "/receptionist/bookings/*/confirm-check-in")
                        .hasAuthority(PermissionCode.CHECK_IN)
                        .requestMatchers(HttpMethod.GET, "/receptionist/**")
                        .hasAuthority(PermissionCode.BOOKING_VIEW)
                        .requestMatchers(HttpMethod.POST, "/receptionist/**")
                        .hasAuthority(PermissionCode.BOOKING_MANAGE)

                        // 5. Mọi request khác đều phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .successHandler(cusAuthenticationSuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof LockedException) {
                                response.sendRedirect("/auth/login?error=inactive");
                            } else if (exception instanceof DisabledException) {
                                response.sendRedirect("/auth/login?error=pending");
                            } else {
                                response.sendRedirect("/auth/login?error=credentials");
                            }
                        })
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
