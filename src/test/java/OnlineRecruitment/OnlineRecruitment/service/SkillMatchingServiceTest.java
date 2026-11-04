package OnlineRecruitment.OnlineRecruitment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho SkillMatchingService - KHÔNG kết nối SQL Server.
 * Mockito mô phỏng kết quả truy vấn từ job_skills và cv_skills.
 * Chỉ kiểm thử tính toán, không sửa dữ liệu hoặc trạng thái Application.
 */
class SkillMatchingServiceTest {
    @Mock private JdbcTemplate jdbcTemplate;
    private AutoCloseable mocks;
    private SkillMatchingService service;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new SkillMatchingService(jdbcTemplate);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception { mocks.close(); }

    private Map<String, Object> application(long id) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        return row;
    }

    private Map<String, Object> counts(long id, int required, int matched) {
        Map<String, Object> row = new HashMap<>();
        row.put("application_id", id);
        row.put("required_count", required);
        row.put("matched_count", matched);
        return row;
    }

    private void mockRows(Map<String, Object>... rows) {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(rows));
    }

    @Test
    @DisplayName("SM_01: Job yêu cầu 5 Skill, CV trùng 4 = 80%")
    void fourOutOfFiveIsEightyPercent() {
        mockRows(counts(2, 5, 4));
        Map<String, Object> app = application(2);
        service.attachScores(List.of(app));
        assertAll(
                () -> assertEquals(5, app.get("requiredSkills")),
                () -> assertEquals(4, app.get("matchedSkills")),
                () -> assertEquals(80, app.get("matchPercentage")));
    }

    @Test
    @DisplayName("SM_02: Job không có Skill bắt buộc = chưa đủ dữ liệu, không phải 0%")
    void noRequiredSkillProducesNullScore() {
        mockRows(counts(1, 0, 0));
        Map<String, Object> app = application(1);
        service.attachScores(List.of(app));
        assertEquals(0, app.get("requiredSkills"));
        assertNull(app.get("matchPercentage"));
    }

    @Test
    @DisplayName("SM_03: CV không khớp kỹ năng nào = 0%")
    void noneMatchedProducesZero() {
        mockRows(counts(3, 5, 0));
        Map<String, Object> app = application(3);
        service.attachScores(List.of(app));
        assertEquals(0, app.get("matchPercentage"));
    }

    @Test
    @DisplayName("SM_04: CV khớp toàn bộ kỹ năng = 100%")
    void allMatchedProducesHundred() {
        mockRows(counts(4, 5, 5));
        Map<String, Object> app = application(4);
        service.attachScores(List.of(app));
        assertEquals(100, app.get("matchPercentage"));
    }

    @Test
    @DisplayName("SM_05: nhiều Application được tính độc lập trong một truy vấn")
    void multipleApplicationsUseOwnCounts() {
        mockRows(counts(1, 0, 0), counts(2, 5, 4), counts(3, 2, 1));
        List<Map<String, Object>> apps = new ArrayList<>(List.of(
                application(1), application(2), application(3)));
        service.attachScores(apps);
        assertNull(apps.get(0).get("matchPercentage"));
        assertEquals(80, apps.get(1).get("matchPercentage"));
        assertEquals(50, apps.get(2).get("matchPercentage"));
        verify(jdbcTemplate, times(1)).queryForList(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("SM_06: danh sách rỗng không gọi database")
    void emptyApplicationsDoesNotQueryDatabase() {
        service.attachScores(List.of());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("SM_07: thiếu dòng dữ liệu đếm Skill = chưa đủ dữ liệu")
    void missingCountRowProducesNullScore() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of());
        Map<String, Object> app = application(42);
        service.attachScores(List.of(app));
        assertEquals(0, app.get("requiredSkills"));
        assertEquals(0, app.get("matchedSkills"));
        assertNull(app.get("matchPercentage"));
    }
}
