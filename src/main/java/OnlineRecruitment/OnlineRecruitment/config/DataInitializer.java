package OnlineRecruitment.OnlineRecruitment.config;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. TẠO TÀI KHOẢN NẾU CHƯA CÓ TRONG DATABASE
        createUserIfMissing("admin01",     "admin123",     "System Admin",          UserRole.ADMIN,     UserStatus.ACTIVE);
        createUserIfMissing("recruiter01", "recruiter123", "Nguyen Van Recruiter",  UserRole.RECRUITER, UserStatus.ACTIVE);
        createUserIfMissing("candidate01", "candidate123", "Tran Thi Candidate",    UserRole.CANDIDATE, UserStatus.ACTIVE);

        // 2. MÃ HÓA LẠI PASSWORD THEO ĐÚNG HÀM plainPasswordFor DÀNH CHO TẤT CẢ USER
        int hashed = 0;
        for (User u : userRepository.findAll()) {
            String hash = u.getPasswordHash();
            boolean isBcrypt = hash != null &&
                    (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));

            if (!isBcrypt) {
                String plain = plainPasswordFor(u.getUsername());
                u.setPasswordHash(passwordEncoder.encode(plain));
                userRepository.save(u);
                hashed++;
                log.info("Initialized BCrypt password hash for user: {}", u.getUsername());
            }
        }

        if (hashed > 0) {
            log.info("Finished initializing {} user passwords to BCrypt.", hashed);
        }
    }

    private void createUserIfMissing(String username, String rawPassword, String fullName,
                                     UserRole role, UserStatus status) {
        // Đã tồn tại → bỏ qua
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@recruitment.local");
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName);
        u.setRole(role);
        u.setStatus(status);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());

        userRepository.save(u);
        log.info("Created new user: {} / {} ({})", username, rawPassword, role);
    }

    private String plainPasswordFor(String username) {
        if (username == null) {
            return "password123";
        }

        return switch (username) {
            case "admin01"     -> "admin123";
            case "recruiter01" -> "recruiter123";
            case "candidate01" -> "candidate123";
            default            -> "password123";
        };
    }
}