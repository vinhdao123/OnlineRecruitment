package OnlineRecruitment.OnlineRecruitment.service.impl;

import OnlineRecruitment.OnlineRecruitment.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@recruitment.com}")
    private String senderEmail;

    @Override
    public void sendVerificationEmail(String toEmail, String recipientName, String otpCode, String verificationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "Online Recruitment System");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực tài khoản Online Recruitment: " + otpCode);

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;\">"
                    + "<div style=\"text-align: center; margin-bottom: 20px;\">"
                    + "<h2 style=\"color: #0d6efd; margin: 0;\">Online Recruitment</h2>"
                    + "<p style=\"color: #6c757d; margin-top: 5px;\">Hệ thống tuyển dụng và tìm việc làm trực tuyến</p>"
                    + "</div>"
                    + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">"
                    + "<p>Xin chào <strong>" + (recipientName != null ? recipientName : "Bạn") + "</strong>,</p>"
                    + "<p>Cảm ơn bạn đã đăng ký tài khoản tại hệ thống Online Recruitment. Dưới đây là mã xác thực tài khoản của bạn:</p>"
                    + "<div style=\"background-color: #f8f9fa; border: 2px dashed #0d6efd; border-radius: 8px; padding: 15px; text-align: center; margin: 25px 0;\">"
                    + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #0d6efd;\">" + otpCode + "</span>"
                    + "</div>"
                    + "<p style=\"text-align: center;\">Hoặc bạn có thể bấm vào liên kết dưới đây để kích hoạt tài khoản ngay:</p>"
                    + "<div style=\"text-align: center; margin: 20px 0;\">"
                    + "<a href=\"" + verificationLink + "\" style=\"background-color: #0d6efd; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: bold; display: inline-block;\">Kích hoạt tài khoản ngay</a>"
                    + "</div>"
                    + "<p style=\"color: #dc3545; font-size: 13px;\">⚠️ Lưu ý: Mã xác thực có hiệu lực trong vòng <strong>15 phút</strong>. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">"
                    + "<p style=\"font-size: 12px; color: #6c757d; text-align: center;\">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.<br>&copy; 2026 Online Recruitment System.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email xác thực đã được gửi thành công đến: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email xác thực tới {}: {}", toEmail, e.getMessage());
            // In mã OTP ra log để nhà phát triển có thể test ngay cả khi chưa thiết lập App Password Gmail
            log.warn(">>> [DEV TEST] Mã OTP xác thực cho {}: {} | Link: {}", toEmail, otpCode, verificationLink);
        } catch (Exception e) {
            log.error("Lỗi gửi mail: {}", e.getMessage());
            log.warn(">>> [DEV TEST] Mã OTP xác thực cho {}: {} | Link: {}", toEmail, otpCode, verificationLink);
        }
    }
}
