package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

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
                        // 1. Nhóm các đường dẫn Public (Ai cũng vào được)
                        .requestMatchers(
                                "/",
                                "/home",
                                "/error",
                                "/common/**",
                                "/auth/**",
                                "/page/**",
                                "/fragment/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/admin/Admin.css",
                                "/Admin/**",
                                "/profile/**",   // Đã gộp /profile/** vào đây
                                "/guest/**"      // Đã gộp /guest/** vào đây
                        ).permitAll()

                        // 2. Nhóm quyền SYSTEM_ADMIN
                        .requestMatchers("/admin/account/**").hasRole("SYSTEM_ADMIN")

                        // 3. Nhóm quyền HOTEL_ADMIN
                        .requestMatchers(
                                "/admin/dashboard",
                                "/admin/list_room/**",
                                "/admin/rooms/**",
                                "/admin/room-images/**",
                                "/admin/services/**",
                                "/admin/promotions/**"
                        ).hasRole("HOTEL_ADMIN")

                        // 4. Các quyền khác
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/receptionist/**").hasRole("RECEPTIONIST")
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
                .csrf(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler cusAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            if (roles.contains("ROLE_SYSTEM_ADMIN")) {
                response.sendRedirect("/admin/account");
            } else if (roles.contains("ROLE_HOTEL_ADMIN")) {
            Set<String> roles =
                    AuthorityUtils.authorityListToSet(authentication.getAuthorities());
            if(roles.contains("ROLE_ADMIN")){
                response.sendRedirect("/admin/dashboard");
            } else if(roles.contains("ROLE_RECEPTIONIST")){
                response.sendRedirect("/receptionist/dashboard");
            } else if (roles.contains("ROLE_MANAGER")) {
                response.sendRedirect("/manager/dashboard");
            }else{
                response.sendRedirect("/home");
            }
        };
   }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}