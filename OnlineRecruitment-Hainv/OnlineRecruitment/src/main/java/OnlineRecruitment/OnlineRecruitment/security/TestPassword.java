package OnlineRecruitment.OnlineRecruitment.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = encoder.encode("123456");

        System.out.println(hash);
        System.out.println(hash.length());
        System.out.println(encoder.matches("123456", hash));
    }
}