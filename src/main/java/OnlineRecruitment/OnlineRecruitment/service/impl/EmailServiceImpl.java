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
    public void sendVerificationEmail(
            String toEmail,
            String recipientName,
            String otpCode,
            String verificationLink) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("justtown1012@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực OTP đăng ký tài khoản");

            String htmlContent =
                    "<html>"
                            + "<body>"
                            + "<h2>Xin chào " + recipientName + "!</h2>"
                            + "<p>Cảm ơn bạn đã đăng ký tài khoản tại Online Recruitment.</p>"
                            + "<p>Mã OTP xác thực của bạn là:</p>"
                            + "<h1 style='color: #007bff;'>" + otpCode + "</h1>"
                            + "<p>Mã OTP có hiệu lực trong <b>15 phút</b>.</p>"
                            + "<p>Bạn cũng có thể xác thực tài khoản bằng đường dẫn sau:</p>"
                            + "<p><a href='" + verificationLink + "'>"
                            + "Xác thực tài khoản"
                            + "</a></p>"
                            + "<br>"
                            + "<p>Nếu bạn không thực hiện đăng ký tài khoản này, "
                            + "vui lòng bỏ qua email.</p>"
                            + "</body>"
                            + "</html>";

            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println(
                    ">>> [SUCCESS] Da gui email xac thuc thanh cong toi: "
                            + toEmail
            );

        } catch (Exception e) {

            System.err.println(
                    ">>> [ERROR] Loi gui email toi "
                            + toEmail + ": " + e.getMessage()
            );

            e.printStackTrace();

            throw new RuntimeException(
                    "Không thể gửi email xác thực. Vui lòng kiểm tra cấu hình email!"
            );
        }
    }
}