package OnlineRecruitment.OnlineRecruitment.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;


    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);

            log.info("Gửi email thành công đến: {}", to);

        } catch (MailException e) {
            log.error("Gửi email thất bại đến {}: {}", to, e.getMessage());

            throw new IllegalStateException(
                    "Không thể gửi email đến " + to, e
            );
        }
    }


    public void sendOtpEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
            throw new IllegalStateException("Không thể gửi email chứa mã OTP. Vui lòng kiểm tra địa chỉ email hoặc thử lại sau!");
        }
    }
    public void sendEmailWithAttachment(
            String to,
            String subject,
            String content,
            File attachment,
            String attachmentName
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);

            if (attachment != null && attachment.exists()) {
                helper.addAttachment(
                        attachmentName,
                        new FileSystemResource(attachment)
                );
            }

            mailSender.send(message);

        } catch (Exception e) {
            log.error("Không thể gửi email đến {}: {}", to, e.getMessage());
        }
    }
}