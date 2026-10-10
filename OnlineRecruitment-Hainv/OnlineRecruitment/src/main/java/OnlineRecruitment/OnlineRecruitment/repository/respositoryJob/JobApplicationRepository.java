package OnlineRecruitment.OnlineRecruitment.repository.respositoryJob;

import OnlineRecruitment.OnlineRecruitment.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long> {

    // Kiểm tra ứng viên đã nộp hồ sơ cho công việc hay chưa
    boolean existsByCandidate_IdAndJob_Id(
            Long candidateId,
            Long jobId
    );

    // Lấy hồ sơ theo ID ứng viên và ID công việc
    List<JobApplication> findByCandidate_IdAndJob_Id(
            Long candidateId,
            Long jobId
    );

    // Lấy hồ sơ mới nhất theo ID tài khoản User và ID công việc
    @Query("""
        SELECT a
        FROM JobApplication a
        JOIN a.candidate c
        WHERE c.user.id = :userId
          AND a.job.id = :jobId
        ORDER BY a.appliedAt DESC
        """)
    List<JobApplication> findApplicationsByUserIdAndJobId(
            @Param("userId") Long userId,
            @Param("jobId") Long jobId
    );

    // Lấy danh sách hồ sơ ứng tuyển thuộc các tin của recruiter
    List<JobApplication> findByJob_RecruiterIdOrderByAppliedAtDesc(
            Long recruiterId
    );

    // Tìm hồ sơ thuộc tin tuyển dụng của recruiter
    Optional<JobApplication> findByIdAndJob_RecruiterId(
            Long applicationId,
            Long recruiterId
    );

    // Lấy chi tiết hồ sơ, bao gồm ứng viên, tài khoản, công việc và CV
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
