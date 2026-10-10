package OnlineRecruitment.OnlineRecruitment.config;

import OnlineRecruitment.OnlineRecruitment.service.CustomUserDetailsService;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider
    ) throws Exception {

        http
                .authenticationProvider(authenticationProvider)

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        // Các trang công khai
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/register/**",

                                // Xác thực email bằng OTP
                                "/auth/verify-otp",
                                "/auth/resend-otp",
                                "/auth/verify",

                                "/403",
                                "/error",
                                "/about",
                                "/jobs/**",
                                "/companies/**",

                                // Tài nguyên tĩnh
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/image/**",
                                "/static/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Phân quyền theo vai trò
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/recruiter/**")
                        .hasRole("RECRUITER")

                        .requestMatchers("/candidate/**")
                        .hasRole("CANDIDATE")

                        // Các trang còn lại cần đăng nhập
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")

                        // Khớp với name của input trong form đăng nhập
                        .usernameParameter("email")
                        .passwordParameter("password")

                        // Chuyển hướng theo vai trò sau đăng nhập
                        .successHandler((request, response, authentication) -> {

                            String target = authentication.getAuthorities()
                                    .stream()
                                    .map(authority ->
                                            authority.getAuthority())
                                    .filter(role ->
                                            role.equals("ROLE_ADMIN")
                                                    || role.equals("ROLE_RECRUITER")
                                                    || role.equals("ROLE_CANDIDATE"))
                                    .findFirst()
                                    .map(role -> switch (role) {
                                        case "ROLE_ADMIN" ->
                                                "/admin/dashboard";

                                        case "ROLE_RECRUITER" ->
                                                "/recruiter/jobs";

                                        // Candidate vào thẳng trang việc làm
                                        case "ROLE_CANDIDATE" ->
                                                "/jobs";

                                        default -> "/";
                                    })
                                    .orElse("/");

                            response.sendRedirect(
                                    request.getContextPath() + target
                            );
                        })

                        .failureUrl("/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .exceptionHandling(ex ->
                        ex.accessDeniedPage("/403")
                );

        return http.build();
    }
}
