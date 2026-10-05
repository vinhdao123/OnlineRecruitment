package OnlineRecruitment.OnlineRecruitment.service.impl;

import OnlineRecruitment.OnlineRecruitment.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("justtown1012@gmail.com"); // Email gốc
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực OTP đăng ký tài khoản");
            helper.setText("<p>Mã OTP xác nhận của bạn là: <b>" + otpCode + "</b></p>", true);

            mailSender.send(message);
            System.out.println(">>> [SUCCESS] Da gui OTP thanh cong toi email: " + toEmail);
        } catch (Exception e) {
            System.err.println(">>> [ERROR] Loi gui OTP toi email " + toEmail + ": " + e.getMessage());
            e.printStackTrace(); // In loi chi tiet ra Console IDE
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng kiểm tra lại địa chỉ email hoặc kết nối!");
        }
    }
}