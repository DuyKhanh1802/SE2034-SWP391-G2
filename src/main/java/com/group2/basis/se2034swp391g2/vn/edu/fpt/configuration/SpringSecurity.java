package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
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

                        .requestMatchers(HttpMethod.GET, "/profile/*.css").permitAll()
                        .requestMatchers(HttpMethod.GET, "/profile/edit").permitAll()
                        .requestMatchers(HttpMethod.POST, "/profile/update").permitAll()
                        .requestMatchers(HttpMethod.GET, "/profile/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/profile/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/system-admin/list-user/*/review")
                        .hasRole(RoleName.SYSTEM_ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/system-admin/list-user/*/edit")
                        .hasRole(RoleName.SYSTEM_ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/system-admin/list-user/**")
                        .hasRole(RoleName.SYSTEM_ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/system-admin/**")
                        .hasRole(RoleName.SYSTEM_ADMIN.name())

                        .requestMatchers("/hotel-admin/dashboard")
                        .hasRole(RoleName.HOTEL_ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/hotel-admin/promotions/**")
                        .hasRole(RoleName.HOTEL_ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/hotel-admin/promotions/**",
                                "/hotel-admin/promotion-images/**")
                        .hasRole(RoleName.HOTEL_ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/hotel-admin/**")
                        .hasRole(RoleName.HOTEL_ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/hotel-admin/**")
                        .hasRole(RoleName.HOTEL_ADMIN.name())

                        .requestMatchers("/manager/dashboard")
                        .hasRole(RoleName.MANAGER.name())
                        .requestMatchers(HttpMethod.GET, "/manager/reports", "/manager/reports/**")
                        .hasRole(RoleName.MANAGER.name())
                        .requestMatchers(HttpMethod.GET, "/manager/**")
                        .hasRole(RoleName.MANAGER.name())
                        .requestMatchers(HttpMethod.POST, "/manager/**")
                        .hasRole(RoleName.MANAGER.name())

                        .requestMatchers(HttpMethod.GET, "/storekeeper/**")
                        .hasRole(RoleName.STOREKEEPER.name())
                        .requestMatchers(HttpMethod.POST, "/storekeeper/**")
                        .hasRole(RoleName.STOREKEEPER.name())

                        .requestMatchers("/receptionist/dashboard")
                        .hasRole(RoleName.RECEPTIONIST.name())
                        .requestMatchers(HttpMethod.POST, "/receptionist/check-in/**",
                                "/receptionist/bookings/*/confirm-check-in")
                        .hasRole(RoleName.RECEPTIONIST.name())
                        .requestMatchers(HttpMethod.GET, "/receptionist/**")
                        .hasRole(RoleName.RECEPTIONIST.name())
                        .requestMatchers(HttpMethod.POST, "/receptionist/**")
                        .hasRole(RoleName.RECEPTIONIST.name())

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
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember-me")
                .rememberMeCookieName("VHOTEL_REMEMBER_ME")
                                .key("vhotel-remember-me-secret-key")
                                .tokenValiditySeconds(7 * 24 * 60 * 60)
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

            response.sendRedirect(getDashboardPath(resolveDefaultRole(roles)));
        };
    }

    private RoleName resolveDefaultRole(Set<String> authorities) {
        if (authorities.contains("ROLE_SYSTEM_ADMIN")) {
            return RoleName.SYSTEM_ADMIN;
        }
        if (authorities.contains("ROLE_HOTEL_ADMIN")) {
            return RoleName.HOTEL_ADMIN;
        }
        if (authorities.contains("ROLE_MANAGER")) {
            return RoleName.MANAGER;
        }
        if (authorities.contains("ROLE_STOREKEEPER")) {
            return RoleName.STOREKEEPER;
        }
        if (authorities.contains("ROLE_RECEPTIONIST")) {
            return RoleName.RECEPTIONIST;
        }
        return RoleName.GUEST;
    }

    private String getDashboardPath(RoleName role) {
        return switch (role) {
            case SYSTEM_ADMIN -> "/system-admin/list-user";
            case HOTEL_ADMIN -> "/hotel-admin/dashboard";
            case MANAGER -> "/manager/dashboard";
            case STOREKEEPER -> "/storekeeper/inventory";
            case RECEPTIONIST -> "/receptionist/dashboard";
            case GUEST -> "/page/home";
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
