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
                        .requestMatchers(
                                "/",           // Cho phép vào trang gốc
                                "/home",
                                "/error",      // QUAN TRỌNG: Mở khóa trang báo lỗi để tránh 403 ảo
                                "/common/**",
                                "/auth/**",
                                "/page/**",
                                "/fragment/**",
                                "/css/**",     // MỞ KHÓA CSS
                                "/js/**",      // MỞ KHÓA JS
                                "/images/**"   // MỞ KHÓA HÌNH ẢNH
                        ).permitAll()

                        // GIỮ NGUYÊN 100% LOGIC CỦA LEADER
                        .requestMatchers("/admin/**").hasRole("ADMIN")
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
    public AuthenticationSuccessHandler cusAuthenticationSuccessHandler(){
        return(request, response, authentication) -> {

            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            // GIỮ NGUYÊN 100% LOGIC CỦA LEADER
            if(roles.contains("ROLE_ADMIN")){
                response.sendRedirect("/admin/dashboard");
            } else if(roles.contains("ROLE_RECEPTIONIST")){
                response.sendRedirect("/receptionist/dashboard");
            } else if (roles.contains("ROLE_MANAGER")) {
                response.sendRedirect("/manager/dashboard");
            } else {
                response.sendRedirect("/home");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}