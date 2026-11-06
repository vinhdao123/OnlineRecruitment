package OnlineRecruitment.OnlineRecruitment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JobSkillRepository {
    private final JdbcTemplate jdbc;

    public boolean isUsed(Long skillId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT_BIG(*) FROM dbo.job_skills WHERE skill_id = ?", Long.class, skillId);
        return count != null && count > 0;
    }
}
