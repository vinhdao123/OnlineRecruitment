
package OnlineRecruitment.OnlineRecruitment.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // =====================================================
    // SẮP XẾP
    // =====================================================

    List<Job> findAllByOrderByCreatedAtDesc();

    List<Job> findByStatusAndDeadlineBefore(
            JobStatus status,
            LocalDate date
    );

    List<Job> findByStatusAndUpdatedAtBefore(
            JobStatus status,
            LocalDateTime time
    );

    List<Job> findAllByOrderBySalaryMaxDesc();

    List<Job> findAllByOrderBySalaryMaxAsc();


    // =====================================================
    // TÌM KIẾM CƠ BẢN
    // =====================================================

    List<Job> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            String keyword
    );

    List<Job> findByLocationContainingIgnoreCaseOrderByCreatedAtDesc(
            String location
    );

    List<Job> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseOrderByCreatedAtDesc(
            String keyword,
            String location
    );


    // =====================================================
    // ĐỊA ĐIỂM
    // =====================================================

    List<Job> findDistinctByLocationIsNotNullOrderByLocationAsc();


    // =====================================================
    // THỐNG KÊ TOÀN BỘ TIN
    // =====================================================

    long countByStatus(JobStatus status);


    // =====================================================
    // RECRUITER: LẤY TẤT CẢ TIN TRONG DATABASE
    // Lọc theo tiêu đề, địa điểm và trạng thái.
    // Hỗ trợ phân trang.
    // =====================================================

    @Query(
            value = """
            SELECT DISTINCT j
            FROM Job j
            LEFT JOIN FETCH j.company
            LEFT JOIN FETCH j.recruiter
            LEFT JOIN FETCH j.category
            WHERE (
                :keyword IS NULL
                OR LOWER(j.title) LIKE
                   LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (
                :location IS NULL
                OR LOWER(j.location) LIKE
                   LOWER(CONCAT('%', :location, '%'))
            )
            AND (
                :status IS NULL
                OR j.status = :status
            )
            ORDER BY j.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(j)
            FROM Job j
            WHERE (
                :keyword IS NULL
                OR LOWER(j.title) LIKE
                   LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (
                :location IS NULL
                OR LOWER(j.location) LIKE
                   LOWER(CONCAT('%', :location, '%'))
            )
            AND (
                :status IS NULL
                OR j.status = :status
            )
            """
    )
    Page<Job> findAllForManagement(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("status") JobStatus status,
            Pageable pageable
    );


    // =====================================================
    // GUEST: CHỈ LẤY TIN ACTIVE, CÔNG TY ACTIVE
    // VÀ CHƯA HẾT HẠN
    // =====================================================

    @Query("""
        SELECT j
        FROM Job j
        JOIN FETCH j.company c
        JOIN FETCH j.category cat
        WHERE j.status =
              OnlineRecruitment.OnlineRecruitment.entity.JobStatus.ACTIVE
          AND c.status =
              OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus.ACTIVE
          AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE)
        ORDER BY j.createdAt DESC
        """)
    List<Job> findActiveJobs();


    // =====================================================
    // ADMIN: LẤY TIN ĐANG CHỜ DUYỆT
    // =====================================================

    @Query("""
        SELECT j
        FROM Job j
        JOIN FETCH j.company c
        JOIN FETCH j.recruiter r
        JOIN FETCH j.category cat
        WHERE j.status =
              OnlineRecruitment.OnlineRecruitment.entity.JobStatus.PENDING
        ORDER BY j.createdAt ASC
        """)
    List<Job> findPendingJobsWithDetails();


    // =====================================================
    // RECRUITER: TIN CỦA CHÍNH MÌNH
    // Giữ nguyên các phương thức phục vụ nghiệp vụ cũ.
    // =====================================================

    List<Job> findByRecruiter_IdOrderByCreatedAtDesc(
            Long recruiterId
    );

    List<Job> findByRecruiter_IdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            Long recruiterId,
            String keyword
    );

    List<Job> findByRecruiter_IdAndStatusOrderByCreatedAtDesc(
            Long recruiterId,
            JobStatus status
    );

    List<Job> findByRecruiter_IdAndTitleContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            Long recruiterId,
            String keyword,
            JobStatus status
    );


    // =====================================================
    // THỐNG KÊ RIÊNG CỦA RECRUITER
    // =====================================================

    long countByRecruiter_Id(Long recruiterId);

    long countByRecruiter_IdAndStatus(
            Long recruiterId,
            JobStatus status
    );


    // =====================================================
    // KIỂM TRA CHỦ SỞ HỮU CỦA TIN
    // =====================================================

    Optional<Job> findByIdAndRecruiter_Id(
            Long id,
            Long recruiterId
    );


    // =====================================================
    // ADMIN: LẤY CHI TIẾT TIN KÈM CÁC QUAN HỆ
    // =====================================================

    @Query("""
        SELECT j
        FROM Job j
        JOIN FETCH j.company c
        JOIN FETCH j.recruiter r
        JOIN FETCH j.category cat
        WHERE j.id = :id
        """)
    Optional<Job> findByIdWithDetails(
            @Param("id") Long id
    );
}
