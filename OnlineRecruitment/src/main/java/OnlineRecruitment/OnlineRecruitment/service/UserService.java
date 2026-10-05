package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> search(String keyword, UserRole role, UserStatus status,
                             int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return userRepository.search(
                kw, role, status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
    }

    @Transactional(readOnly = true)
    public List<User> searchAll(String keyword, UserRole role, UserStatus status) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return userRepository.searchAll(kw, role, status);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + id));
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + username));
    }

    @Transactional
    public User create(String username, String email, String fullName,
                       String phone, String location,
                       String rawPassword, UserRole role, UserStatus status,
                       User actor) {

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username đã tồn tại: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại: " + email);
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setLocation(location);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setStatus(status);

        userRepository.save(u);

        activityLogService.log(actor, "CREATE_USER",
                "Đã tạo tài khoản mới: " + username + " (" + role + ")",
                "USER", u.getId(), "System Update");

        return u;
    }

    @Transactional
    public void lock(Long id, User actor) {
        User u = getById(id);
        u.setStatus(UserStatus.LOCKED);
        userRepository.save(u);

        activityLogService.log(actor, "BAN_USER",
                "Đã khóa tài khoản " + u.getUsername() + " (" + u.getFullName() + ")",
                "USER", u.getId(), "Suspicious Activity");
    }

    @Transactional
    public void unlock(Long id, User actor) {
        User u = getById(id);
        u.setStatus(UserStatus.ACTIVE);
        userRepository.save(u);

        activityLogService.log(actor, "UNBAN_USER",
                "Đã mở khóa tài khoản " + u.getUsername(),
                "USER", u.getId(), "System Update");
    }

    public long countAll()                       { return userRepository.count(); }
    public long countByRole(UserRole role)       { return userRepository.countByRole(role); }
    public long countByStatus(UserStatus status) { return userRepository.countByStatus(status); }
}