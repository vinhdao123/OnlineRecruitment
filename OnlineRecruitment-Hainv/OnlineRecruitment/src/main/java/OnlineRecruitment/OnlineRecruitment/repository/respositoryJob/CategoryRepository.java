package OnlineRecruitment.OnlineRecruitment.repository.respositoryJob;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository
        extends JpaRepository<JobCategory, Long> {

    List<JobCategory> findByStatusOrderByName(String status);
}