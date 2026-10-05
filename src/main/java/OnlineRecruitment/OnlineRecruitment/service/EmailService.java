package OnlineRecruitment.OnlineRecruitment.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String recipientName, String otpCode, String verificationLink);
}
