package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.controller.RoleSwitchController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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

                        // 1. Public URLs
                        .requestMatchers(
                                "/",
                                "/home",
                                "/error",
                                "/common/**",
                                "/auth/**",
                                "/page/**",
                                "/fragment/**",
                                "/images/**",
                                "/guest/**"
                        ).permitAll()
                        .requestMatchers("/profile/**", "/api/user/switch-role").authenticated()
                        .requestMatchers("/admin/account/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers("/admin/list-user/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(
                                "/admin/dashboard",
                                "/admin/list_room/**",
                                "/admin/rooms/**",
                                "/admin/room-images/**",
                                "/admin/services/**",
                                "/admin/promotions/**"
                        ).hasRole("HOTEL_ADMIN")
                        .requestMatchers("/receptionist/**").hasRole("RECEPTIONIST")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
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
                .csrf(Customizer.withDefaults());

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
