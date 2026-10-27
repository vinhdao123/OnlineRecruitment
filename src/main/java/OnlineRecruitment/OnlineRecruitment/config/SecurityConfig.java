package OnlineRecruitment.OnlineRecruitment.config;

import OnlineRecruitment.OnlineRecruitment.exception.AccountRejectedException;
import OnlineRecruitment.OnlineRecruitment.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // Thêm import

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private AuthenticationFailureHandler failureHandler() {
        return (request, response, exception) -> {
            String error = "badCredentials";
            Throwable cause = exception;
            while (cause.getCause() != null && cause != cause.getCause()) {
                cause = cause.getCause();
            }
            if (cause instanceof AccountRejectedException || exception instanceof AccountRejectedException) {
                error = "rejected";
            } else if (cause instanceof LockedException || exception instanceof LockedException) {
                error = "locked";
            } else if (cause instanceof DisabledException || exception instanceof DisabledException) {
                error = "inactive";
            }
            response.sendRedirect(request.getContextPath() + "/login?" + error);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .userDetailsService(userDetailsService)
                // 1. TẮT CSRF CHO API WEBHOOK
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/sepay-webhook")
                )

                .authorizeHttpRequests(auth -> auth
                        // 2. CHO PHÉP TRUY CẬP TỰ DO VÀO WEBHOOK VÀ API PAYMENT STATUS
                        .requestMatchers("/api/sepay-webhook", "/pricing/api/payment-status/**").permitAll()

                        .requestMatchers(
                                "/", "/login", "/register", "/register/**",
                                "/verify-otp", "/resend-otp", "/auth/**",
                                "/forgot-password", "/forgot-password/**",
                                "/403", "/error", "/companies", "/about",
                                "/company-profile",
                                "/css/**", "/js/**", "/images/**", "/static/**",
                                "/uploads/**",
                                "/favicon.ico"
                        ).permitAll()

                        .requestMatchers("/jobs/*/apply").hasRole("CANDIDATE")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/jobs", "/jobs/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/recruiter/**").hasRole("RECRUITER")
                        .requestMatchers("/candidate/**").hasRole("CANDIDATE")
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((req, res, auth) -> {
                            String role = auth.getAuthorities().iterator().next().getAuthority();
                            String target = switch (role) {
                                case "ROLE_ADMIN"     -> "/admin/dashboard";
                                case "ROLE_RECRUITER" -> "/recruiter/dashboard";
                                case "ROLE_CANDIDATE" -> "/candidate/dashboard";
                                default               -> "/";
                            };
                            res.sendRedirect(target);
                        })
                        .failureHandler(failureHandler())
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .exceptionHandling(ex -> ex.accessDeniedPage("/403"));

        return http.build();
    }
}