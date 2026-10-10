package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String input)
            throws UsernameNotFoundException {

        System.out.println(">>> loadUserByUsername called: " + input);

        if (input == null || input.isBlank()) {
            throw new UsernameNotFoundException(
                    "Vui lòng nhập email hoặc tên đăng nhập"
            );
        }

        String login = input.trim();

        User user = userRepository.findByEmail(login)
                .or(() -> userRepository.findByUsername(login))
                .orElseThrow(() -> {
                    System.out.println(
                            ">>> ACCOUNT NOT FOUND: " + login
                    );

                    return new UsernameNotFoundException(
                            "Không tìm thấy tài khoản: " + login
                    );
                });

        System.out.println(">>> ACCOUNT FOUND");
        System.out.println(">>> Email: " + user.getEmail());
        System.out.println(">>> Username: " + user.getUsername());
        System.out.println(">>> Role: " + user.getRole());
        System.out.println(">>> Status: " + user.getStatus());
        System.out.println(">>> Password hash exists: "
                + (user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank()));

        String authority = "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(
                        new SimpleGrantedAuthority(authority)
                ))
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }
}