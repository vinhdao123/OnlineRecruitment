package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobCategoryRepository
        extends JpaRepository<JobCategory, Long> {
}