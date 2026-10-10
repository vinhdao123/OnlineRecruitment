package OnlineRecruitment.OnlineRecruitment.repository.respositoryJob;

import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByStatusOrderByName(String status);
}