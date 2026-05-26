import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll() // Cho phép truy cập TẤT CẢ các URL không cần đăng nhập
                        // Hoặc cấu hình cụ thể: .requestMatchers("/", "/home", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable()) // Tắt form login mặc định nếu không dùng
                .csrf(csrf -> csrf.disable());     // Tắt CSRF nếu đang test API (Postman)

        return http.build();
    }
}