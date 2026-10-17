package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy email: " + email));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new LockedException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ admin.");
        }

        if (user.getStatus() == UserStatus.PENDING || user.getStatus() == UserStatus.REJECTED) {
            throw new DisabledException("Tài khoản chưa được kích hoạt hoặc đang chờ duyệt.");
        }

        String authority = "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(authority)))
                .build();
    }
}