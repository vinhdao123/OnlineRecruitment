package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // ================= SẮP XẾP =================

    List<Job> findAllByOrderByCreatedAtDesc();

    // Tìm all by order by salary max desc trong luồng tuyển dụng.
    List<Job> findAllByOrderBySalaryMaxDesc();

    // Tìm all by order by salary max asc trong luồng tuyển dụng.
    List<Job> findAllByOrderBySalaryMaxAsc();

    // ================= TÌM KIẾM =================

    List<Job> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            String keyword
    );

    // Tìm by location containing ignore case order by created at desc trong luồng tuyển dụng.
    List<Job> findByLocationContainingIgnoreCaseOrderByCreatedAtDesc(
            String location
    );

    // Tìm by title containing ignore case and location containing ignore case order by created at desc trong luồng tuyển dụng.
    List<Job> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseOrderByCreatedAtDesc(
            String keyword,
            String location
    );

    // ================= ĐỊA ĐIỂM =================

    List<Job> findDistinctByLocationIsNotNullOrderByLocationAsc();

    // ================= ĐẾM THEO STATUS =================

    long countByStatus(JobStatus status);

    // ================= ACTIVE JOBS =================
    @Query("""
        SELECT j FROM Job j
        JOIN FETCH j.company c
        WHERE j.status = OnlineRecruitment.OnlineRecruitment.entity.JobStatus.ACTIVE
          AND c.status = OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus.ACTIVE
          AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE)
        ORDER BY j.createdAt DESC
    """)
    // Tìm active jobs trong luồng tuyển dụng.
    List<Job> findActiveJobs();

    // ================= RECRUITER: QUẢN LÝ TẤT CẢ TIN =================
    // Lọc theo tiêu đề, địa điểm, trạng thái (tham số null = không lọc)
    @Query(
            value = """
            SELECT j FROM Job j
            JOIN FETCH j.company c
            WHERE (:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:status IS NULL OR j.status = :status)
            ORDER BY j.createdAt DESC
        """,
            countQuery = """
            SELECT COUNT(j) FROM Job j
            WHERE (:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:status IS NULL OR j.status = :status)
        """
    )
    // Tìm all for management trong luồng tuyển dụng.
    Page<Job> findAllForManagement(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("status") JobStatus status,
            Pageable pageable
    );

    // ================= RECRUITER: TIN CỦA RIÊNG MÌNH =================

    List<Job> findByRecruiter_IdOrderByCreatedAtDesc(Long recruiterId);

    // Tìm by recruiter id and status order by created at desc trong luồng tuyển dụng.
    List<Job> findByRecruiter_IdAndStatusOrderByCreatedAtDesc(
            Long recruiterId,
            JobStatus status
    );

    // Tìm by recruiter id and title containing ignore case order by created at desc trong luồng tuyển dụng.
    List<Job> findByRecruiter_IdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            Long recruiterId,
            String keyword
    );

    // Tìm by recruiter id and title containing ignore case and status order by created at desc trong luồng tuyển dụng.
    List<Job> findByRecruiter_IdAndTitleContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            Long recruiterId,
            String keyword,
            JobStatus status
    );

    // Đếm by recruiter id trong luồng tuyển dụng.
    long countByRecruiter_Id(Long recruiterId);

    // Đếm by recruiter id and status trong luồng tuyển dụng.
    long countByRecruiter_IdAndStatus(Long recruiterId, JobStatus status);

    // Lấy tin theo id nhưng chỉ khi thuộc về recruiter này (kiểm tra quyền sở hữu)
    Optional<Job> findByIdAndRecruiter_Id(Long id, Long recruiterId);

    // ================= ADMIN: DUYỆT TIN =================

    // Tin chờ duyệt, kèm công ty, danh mục, recruiter để trang admin không lỗi lazy
    @Query("""
        SELECT j FROM Job j
        JOIN FETCH j.company
        JOIN FETCH j.category
        JOIN FETCH j.recruiter
        WHERE j.status = OnlineRecruitment.OnlineRecruitment.entity.JobStatus.PENDING
        ORDER BY j.createdAt ASC
    """)
    // Tìm pending jobs with details trong luồng tuyển dụng.
    List<Job> findPendingJobsWithDetails();

    // Một tin theo id, kèm đầy đủ quan hệ (dùng khi duyệt / từ chối)
    @Query("""
        SELECT j FROM Job j
        JOIN FETCH j.company
        JOIN FETCH j.category
        JOIN FETCH j.recruiter
        WHERE j.id = :id
    """)
    // Tìm by id with details trong luồng tuyển dụng.
    Optional<Job> findByIdWithDetails(@Param("id") Long id);

    // ================= TỰ ĐỘNG ĐÓNG TIN =================

    // Tin quá hạn nộp hồ sơ
    List<Job> findByStatusAndDeadlineBefore(JobStatus status, LocalDate date);

    // Tin đã đăng (tính từ ngày duyệt published_at) trước một thời điểm
    List<Job> findByStatusAndPublishedAtBefore(JobStatus status, LocalDateTime time);

    // Tin cũ chưa có ngày duyệt: tạm tính theo ngày cập nhật cuối
    List<Job> findByStatusAndPublishedAtIsNullAndUpdatedAtBefore(JobStatus status, LocalDateTime time);
}