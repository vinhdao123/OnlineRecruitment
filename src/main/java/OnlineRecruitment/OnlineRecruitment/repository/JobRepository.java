package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // ================= SẮP XẾP =================

    List<Job> findAllByOrderByCreatedAtDesc();

    List<Job> findAllByOrderBySalaryMaxDesc();

    List<Job> findAllByOrderBySalaryMaxAsc();

    // ================= TÌM KIẾM =================

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
    List<Job> findActiveJobs();
}