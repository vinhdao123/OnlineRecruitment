package OnlineRecruitment.OnlineRecruitment.config;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.admin.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Hash lại password cho mọi user có password_hash không phải BCrypt hợp lệ.
     * Password mặc định:
     *   admin01 -> admin123
     *   recruiter01 -> recruiter123
     *   candidate01 -> candidate123
     *   tất cả user khác -> password123
     */
    @Override
    @Transactional
    public void run(String... args) {
        int hashed = 0;

        for (User u : userRepository.findAll()) {
            String plain = plainPasswordFor(u.getUsername());

            boolean needsHash;
            try {
                needsHash = !passwordEncoder.matches(plain, u.getPasswordHash());
            } catch (Exception e) {
                needsHash = true;   // hash cũ không phải BCrypt
            }

            if (needsHash) {
                u.setPasswordHash(passwordEncoder.encode(plain));
                userRepository.save(u);
                hashed++;
                System.out.println(">>> Hash password: " + u.getUsername() + " / " + plain);
            }
        }

        if (hashed > 0) {
            System.out.println(">>> Đã hash " + hashed + " password.");
        } else {
            System.out.println(">>> Tất cả password đã là BCrypt, không cần hash lại.");
        }
    }

    private String plainPasswordFor(String username) {
        return switch (username) {
            case "admin01"     -> "admin123";
            case "recruiter01" -> "recruiter123";
            case "candidate01" -> "candidate123";
            default            -> "password123";
        };
    }
}