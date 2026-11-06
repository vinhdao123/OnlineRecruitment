package OnlineRecruitment.OnlineRecruitment.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tính tỷ lệ trùng kỹ năng theo từng hồ sơ ứng tuyển.
 * Chỉ đọc job_skills và cv_skills; không thay đổi CV hay trạng thái ứng tuyển.
 */
@Service
public class SkillMatchingService {
    private final JdbcTemplate jdbcTemplate;

    public SkillMatchingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> detail(Long applicationId, Long recruiterId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT a.id AS applicationId, a.job_id AS jobId, a.cv_id AS cvId,
                       j.title AS jobTitle, u.full_name AS candidateName
                FROM dbo.applications a
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.candidates c ON c.id = a.candidate_id
                JOIN dbo.users u ON u.id = c.user_id
                WHERE a.id = ? AND j.recruiter_id = ?
                """, applicationId, recruiterId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Không tìm thấy hồ sơ hoặc bạn không có quyền xem.");
        Map<String, Object> application = rows.get(0);
        List<Map<String, Object>> skills = jdbcTemplate.queryForList("""
                SELECT s.id AS skillId, s.name AS skillName,
                       CASE WHEN EXISTS (SELECT 1 FROM dbo.cv_skills cs WHERE cs.cv_id = ? AND cs.skill_id = s.id)
                       THEN 1 ELSE 0 END AS matched
                FROM dbo.job_skills js
                JOIN dbo.skills s ON s.id = js.skill_id
                WHERE js.job_id = ? AND js.is_required = 1
                ORDER BY s.name
                """, application.get("cvId"), application.get("jobId"));
        int count = (int) skills.stream().filter(s -> ((Number) s.get("matched")).intValue() == 1).count();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("applicationData", application);
        result.put("requiredSkills", skills);
        result.put("requiredCount", skills.size());
        result.put("matchedCount", count);
        result.put("matchPercentage", skills.isEmpty() ? null : count * 100 / skills.size());
        return result;
    }

    /**
     * Dữ liệu đã được Controller giới hạn theo Recruiter đăng nhập.
     * Batch query tránh vấn đề N+1 khi bảng ứng tuyển có nhiều dòng.
     * Nếu Job không yêu cầu kỹ năng thì score = null (không có cơ sở đánh giá).
     */
    public void attachScores(List<Map<String, Object>> applications) {
        if (applications.isEmpty()) return;

        String placeholders = applications.stream().map(a -> "?").collect(Collectors.joining(","));
        Object[] ids = applications.stream().map(a -> a.get("id")).toArray();

        String sql = """
                SELECT a.id AS application_id,
                       COUNT(DISTINCT js.skill_id) AS required_count,
                       COUNT(DISTINCT cs.skill_id) AS matched_count
                FROM dbo.applications a
                LEFT JOIN dbo.job_skills js
                  ON js.job_id = a.job_id AND js.is_required = 1
                LEFT JOIN dbo.cv_skills cs
                  ON cs.cv_id = a.cv_id AND cs.skill_id = js.skill_id
                WHERE a.id IN (%s)
                GROUP BY a.id
                """.formatted(placeholders);

        Map<Long, Map<String, Object>> counts = jdbcTemplate.queryForList(sql, ids)
                .stream().collect(Collectors.toMap(
                        row -> ((Number) row.get("application_id")).longValue(),
                        row -> row));

        for (Map<String, Object> application : applications) {
            Map<String, Object> result = counts.get(((Number) application.get("id")).longValue());
            int required = result == null ? 0 : ((Number) result.get("required_count")).intValue();
            int matched = result == null ? 0 : ((Number) result.get("matched_count")).intValue();
            application.put("requiredSkills", required);
            application.put("matchedSkills", matched);
            application.put("matchPercentage", required == 0 ? null : matched * 100 / required);
        }
    }
}
