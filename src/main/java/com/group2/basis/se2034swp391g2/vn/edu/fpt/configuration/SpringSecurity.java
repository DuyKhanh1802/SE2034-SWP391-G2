package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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
                                "/fragment/**")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/receptionist/**").hasRole("RECEPTIONIST")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .successHandler(cusAuthenticationSuccessHandler())
                        .failureUrl("/auth/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/auth/forgot-password",
                        "/auth/verify-reset-otp",
                        "/auth/reset-password",
                        "/auth/register"
                ));

        return http.build();
    }

   @Bean
   public AuthenticationSuccessHandler cusAuthenticationSuccessHandler(){
        return(request, response, authentication) -> {

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