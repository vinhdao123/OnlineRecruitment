package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}