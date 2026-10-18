package OnlineRecruitment.OnlineRecruitment.config;

import OnlineRecruitment.OnlineRecruitment.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    // =====================================================
    // PASSWORD ENCODER
    // =====================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =====================================================
    // LOGIN FAILURE HANDLER
    // Phân biệt tài khoản bị khóa, chưa kích hoạt,
    // hoặc thông tin đăng nhập không chính xác.
    // =====================================================

    private AuthenticationFailureHandler failureHandler() {
        return (request, response, exception) -> {
            String error = "badCredentials";

            // Tìm nguyên nhân gốc của lỗi.
            Throwable cause = exception;

            while (cause.getCause() != null
                    && cause != cause.getCause()) {
                cause = cause.getCause();
            }

            if (exception instanceof LockedException
                    || cause instanceof LockedException) {
                error = "locked";
            } else if (exception instanceof DisabledException
                    || cause instanceof DisabledException) {
                error = "inactive";
            }

            response.sendRedirect(
                    request.getContextPath() + "/login?" + error
            );
        };
    }

    // =====================================================
    // SECURITY FILTER CHAIN
    // =====================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .userDetailsService(userDetailsService)

                // =========================================
                // PHÂN QUYỀN TRUY CẬP
                // =========================================

                .authorizeHttpRequests(auth -> auth

                        // Các trang công khai.
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/register/**",
                                "/verify-otp",
                                "/resend-otp",
                                "/auth/**",
                                "/403",
                                "/error",
                                "/companies",
                                "/about"
                        ).permitAll()

                        // Tài nguyên tĩnh.
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/image/**",
                                "/static/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Chỉ Candidate được nộp hồ sơ.
                        .requestMatchers("/jobs/*/apply")
                        .hasRole("CANDIDATE")

                        // Khách được xem danh sách và chi tiết việc làm.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/jobs",
                                "/jobs/**"
                        ).permitAll()

                        // Chỉ Admin truy cập khu vực quản trị.
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        // Chỉ Recruiter truy cập khu vực tuyển dụng.
                        .requestMatchers("/recruiter/**")
                        .hasRole("RECRUITER")

                        // Chỉ Candidate truy cập khu vực ứng viên.
                        .requestMatchers("/candidate/**")
                        .hasRole("CANDIDATE")

                        // Các đường dẫn còn lại cần đăng nhập.
                        .anyRequest().authenticated()
                )

                // =========================================
                // FORM LOGIN
                // =========================================

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")

                        // Chuyển hướng sau khi đăng nhập thành công.
                        .successHandler((request, response, authentication) -> {

                            String role = authentication
                                    .getAuthorities()
                                    .stream()
                                    .map(authority -> authority.getAuthority())
                                    .filter(authority ->
                                            authority.equals("ROLE_ADMIN")
                                                    || authority.equals("ROLE_RECRUITER")
                                                    || authority.equals("ROLE_CANDIDATE")
                                    )
                                    .findFirst()
                                    .orElse("");

                            String target;

                            switch (role) {
                                case "ROLE_ADMIN":
                                    target = "/admin/dashboard";
                                    break;

                                case "ROLE_RECRUITER":
                                    target = "/recruiter/dashboard";
                                    break;

                                case "ROLE_CANDIDATE":
                                    target = "/candidate/dashboard";
                                    break;

                                default:
                                    target = "/";
                                    break;
                            }

                            response.sendRedirect(
                                    request.getContextPath() + target
                            );
                        })

                        // Xử lý đăng nhập thất bại.
                        .failureHandler(failureHandler())

                        .permitAll()
                )

                // =========================================
                // LOGOUT
                // =========================================

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                // =========================================
                // ACCESS DENIED
                // =========================================

                .exceptionHandling(ex ->
                        ex.accessDeniedPage("/403")
                );

        return http.build();
    }
}