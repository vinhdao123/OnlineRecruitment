package OnlineRecruitment.OnlineRecruitment.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Nếu giá trị trong DB là hash BCrypt ($2...) thì kiểm tra bằng BCrypt.
 * Ngược lại so sánh trực tiếp, chỉ để chạy với dữ liệu demo "demo_hash_*".
 */
public class FlexiblePasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String stored) {
        if (stored == null) return false;
        if (stored.startsWith("$2")) {
            return bcrypt.matches(rawPassword, stored);
        }
        return MessageDigest.isEqual(
                rawPassword.toString().getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }
}