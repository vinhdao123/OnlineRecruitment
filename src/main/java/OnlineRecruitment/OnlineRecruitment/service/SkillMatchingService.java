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
