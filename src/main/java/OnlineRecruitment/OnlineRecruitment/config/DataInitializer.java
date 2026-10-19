package OnlineRecruitment.OnlineRecruitment.config;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        int hashed = 0;

        for (User u : userRepository.findAll()) {
            String hash = u.getPasswordHash();
            boolean isBcrypt = hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));

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

    private String plainPasswordFor(String username) {
        String adminPass = System.getenv("ADMIN_PASSWORD");
        if (adminPass != null && !adminPass.isBlank() && "admin01".equals(username)) {
            return adminPass;
        }

        return switch (username) {
            case "admin01"     -> "admin123";
            case "recruiter01" -> "recruiter123";
            case "candidate01" -> "candidate123";
            default            -> "password123";
        };
    }
}