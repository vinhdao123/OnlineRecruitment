package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface EmailService {

    // Gửi email chỉ chứa mã OTP
    void sendOtpEmail(String toEmail, String otpCode);

    // Gửi email xác minh gồm mã OTP và đường dẫn xác minh
    void sendVerificationEmail(
            String toEmail,
            String fullName,
            String otpCode,
            String verificationLink
    );

    @Repository
    interface JobApplicationRepository
            extends JpaRepository<JobApplication, Long> {

        // Kiểm tra ứng viên đã ứng tuyển công việc hay chưa
        boolean existsByCandidate_IdAndJob_Id(
                Long candidateId,
                Long jobId
        );

        // Lấy hồ sơ ứng tuyển của một ứng viên cho một công việc
        List<JobApplication> findByCandidate_IdAndJob_Id(
                Long candidateId,
                Long jobId
        );

        // Lấy danh sách hồ sơ ứng tuyển vào các công việc
        // thuộc nhà tuyển dụng, sắp xếp mới nhất trước
        List<JobApplication> findByJob_RecruiterIdOrderByAppliedAtDesc(
                Long recruiterId
        );

        // Tìm một hồ sơ ứng tuyển thuộc nhà tuyển dụng
        Optional<JobApplication> findByIdAndJob_RecruiterId(
                Long applicationId,
                Long recruiterId
        );

        // Lấy chi tiết hồ sơ cùng các quan hệ cần hiển thị.
        // Đồng thời đảm bảo hồ sơ thuộc công việc của recruiter hiện tại.
        @Query("""
            SELECT a
            FROM JobApplication a
            JOIN FETCH a.candidate c
            JOIN FETCH c.user u
            JOIN FETCH a.job j
            LEFT JOIN FETCH a.cv cv
            WHERE a.id = :applicationId
              AND j.recruiterId = :recruiterId
            """)
        Optional<JobApplication> findDetailByIdAndRecruiterId(
                @Param("applicationId") Long applicationId,
                @Param("recruiterId") Long recruiterId
        );
    }
}