package OnlineRecruitment.OnlineRecruitment;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePassword {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String password = encoder.encode("Recruiter@123");

        System.out.println("BCrypt password:");
        System.out.println(password);
    }
}