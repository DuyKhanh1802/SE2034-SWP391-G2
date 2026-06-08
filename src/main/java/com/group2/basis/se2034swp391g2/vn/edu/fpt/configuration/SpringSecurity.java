package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
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
                        .requestMatchers(
                                "/home",
                                "/common/**",
                                "/auth/**",
                                "/page/**",
                                "/fragment/**",
                                "/error")
                        .permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/receptionist/**").hasRole("RECEPTIONIST")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .successHandler(cusAuthenticationSuccessHandler())
                        .failureUrl("/auth/login")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                /*
                 * Tạm thời bỏ CSRF cho API upload ảnh khuyến mãi.
                 * API này vẫn bị kiểm tra quyền ROLE_MANAGER ở dòng /manager/** bên trên.
                 */
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/manager/promotion-images/upload")
                )

                /*
                 * In lỗi khi bị 403 để biết chính xác request nào bị chặn.
                 */
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            Authentication authentication =
                                    (Authentication) request.getUserPrincipal();

                            System.out.println("========== ACCESS DENIED ==========");
                            System.out.println("Request URI: " + request.getRequestURI());
                            System.out.println("Method: " + request.getMethod());

                            if (authentication == null) {
                                System.out.println("Authentication: null");
                            } else {
                                System.out.println("Username: " + authentication.getName());
                                System.out.println("Authorities: " + authentication.getAuthorities());
                                System.out.println("Authenticated: " + authentication.isAuthenticated());
                            }

                            System.out.println("Exception: " + accessDeniedException.getMessage());
                            System.out.println("===================================");

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        })
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler cusAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {

            Set<String> roles =
                    AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            if (roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin/dashboard");
            } else if (roles.contains("ROLE_RECEPTIONIST")) {
                response.sendRedirect("/receptionist/dashboard");
            } else if (roles.contains("ROLE_MANAGER")) {
                response.sendRedirect("/manager/dashboard");
            } else {
                response.sendRedirect("/home");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}