package OnlineRecruitment.OnlineRecruitment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JobSkillRepository {
    private final JdbcTemplate jdbc;
    public List<Long> findRequiredSkillIds(Long jobId) {
        return jdbc.queryForList("SELECT skill_id FROM dbo.job_skills WHERE job_id = ? AND is_required = 1 ORDER BY skill_id", Long.class, jobId);
    }
    public boolean isUsed(Long skillId) {
        Long count = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.job_skills WHERE skill_id = ?", Long.class, skillId);
        return count != null && count > 0;
    }
    /** Called inside the job's transaction; optional links are retained. */
    public void replaceRequired(Long jobId, List<Long> ids) {
        jdbc.update("DELETE FROM dbo.job_skills WHERE job_id = ? AND is_required = 1", jobId);
        for (Long skillId : ids) {
            int changed = jdbc.update("UPDATE dbo.job_skills SET is_required = 1 WHERE job_id = ? AND skill_id = ?", jobId, skillId);
            if (changed == 0) jdbc.update("INSERT INTO dbo.job_skills (job_id, skill_id, is_required) VALUES (?, ?, 1)", jobId, skillId);
        }
    }
}
