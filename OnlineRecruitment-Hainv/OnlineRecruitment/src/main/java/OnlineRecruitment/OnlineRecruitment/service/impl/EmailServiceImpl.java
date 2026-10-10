package OnlineRecruitment.OnlineRecruitment.service.impl;

import OnlineRecruitment.OnlineRecruitment.service.EmailService;

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

    // Lấy email gửi từ application.properties.
    // Giá trị mặc định dùng khi chưa cấu hình thuộc tính này.
    @Value("${spring.mail.username:justtown1012@gmail.com}")
    private String fromEmail;

    /**
     * Gửi email chứa mã OTP.
     */
    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {

        String subject = "Mã xác thực OTP đăng ký tài khoản";

        String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2>Xác thực tài khoản</h2>
                    <p>Mã OTP xác nhận tài khoản của bạn là:</p>
                    <div style="
                        display: inline-block;
                        padding: 12px 24px;
                        background-color: #f1f5f9;
                        color: #2563eb;
                        font-size: 28px;
                        font-weight: bold;
                        letter-spacing: 6px;
                        border-radius: 8px;">
                        %s
                    </div>
                    <p>Vui lòng không chia sẻ mã này với người khác.</p>
                    <p>Nếu bạn không thực hiện đăng ký, hãy bỏ qua email này.</p>
                    <p>Trân trọng,<br>Online Recruitment</p>
                </body>
                </html>
                """.formatted(escapeHtml(otpCode));

        sendHtmlEmail(toEmail, subject, html);
    }

    /**
     * Gửi email xác minh có OTP và đường dẫn xác minh.
     */
    @Override
    public void sendVerificationEmail(
            String toEmail,
            String fullName,
            String otpCode,
            String verificationLink
    ) {

        String safeName = escapeHtml(
                fullName == null || fullName.isBlank()
                        ? "bạn"
                        : fullName
        );

        String safeOtp = escapeHtml(otpCode);
        String safeLink = escapeHtml(verificationLink);

        String subject = "Xác minh email - Online Recruitment";

        String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="
                    margin: 0;
                    padding: 24px;
                    font-family: Arial, sans-serif;
                    color: #333;
                    line-height: 1.6;">

                    <div style="
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 28px;
                        border: 1px solid #e5e7eb;
                        border-radius: 12px;">

                        <h2 style="color: #2563eb;">
                            Xác minh tài khoản
                        </h2>

                        <p>Xin chào %s,</p>

                        <p>
                            Cảm ơn bạn đã đăng ký tài khoản
                            tại <strong>Online Recruitment</strong>.
                        </p>

                        <p>Mã OTP xác minh email của bạn là:</p>

                        <div style="
                            margin: 20px 0;
                            padding: 16px;
                            background: #eff6ff;
                            border-radius: 8px;
                            text-align: center;
                            font-size: 30px;
                            font-weight: bold;
                            letter-spacing: 8px;
                            color: #1d4ed8;">
                            %s
                        </div>

                        <p>
                            Mã OTP có hiệu lực trong 15 phút.
                            Vui lòng không chia sẻ mã này với người khác.
                        </p>

                        <p>Bạn cũng có thể xác minh email bằng liên kết sau:</p>

                        <p style="margin: 24px 0;">
                            <a href="%s"
                               style="
                                   display: inline-block;
                                   padding: 12px 22px;
                                   color: white;
                                   background: #2563eb;
                                   text-decoration: none;
                                   border-radius: 6px;">
                                Xác minh email
                            </a>
                        </p>

                        <p>
                            Nếu bạn không thực hiện đăng ký,
                            hãy bỏ qua email này.
                        </p>

                        <hr style="border: 0; border-top: 1px solid #e5e7eb;">

                        <p style="font-size: 13px; color: #6b7280;">
                            Email tự động từ Online Recruitment.
                            Vui lòng không trả lời email này.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(safeName, safeOtp, safeLink);

        sendHtmlEmail(toEmail, subject, html);
    }

    /**
     * Hàm dùng chung để gửi email HTML.
     */
    private void sendHtmlEmail(
            String toEmail,
            String subject,
            String htmlContent
    ) {

        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Địa chỉ email người nhận không được để trống."
            );
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Đã gửi email thành công tới {}", toEmail);

        } catch (Exception e) {
            log.error(
                    "Không thể gửi email tới {}",
                    toEmail,
                    e
            );

            throw new RuntimeException(
                    "Không thể gửi email. Vui lòng kiểm tra cấu hình SMTP.",
                    e
            );
        }
    }

    /**
     * Mã hóa ký tự HTML để tránh chèn HTML vào nội dung email.
     */
    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}