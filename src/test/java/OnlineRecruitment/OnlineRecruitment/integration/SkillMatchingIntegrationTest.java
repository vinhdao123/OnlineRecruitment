package OnlineRecruitment.OnlineRecruitment.integration;

import OnlineRecruitment.OnlineRecruitment.service.SkillMatchingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION TEST THẬT: Service -> JdbcTemplate -> Microsoft SQL Server trong Docker.
 * Không dùng Mockito, không kết nối database OnlineRecruitment của nhóm.
 * Bật bằng RUN_SQLSERVER_IT=true. Cần Docker Desktop chạy và kéo image SQL Server.
 */
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_SQLSERVER_IT", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SkillMatchingIntegrationTest {

    @Container
    private static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();

    private static JdbcTemplate jdbc;
    private static SkillMatchingService matching;

    @BeforeAll
    static void init() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        source.setUrl(SQL_SERVER.getJdbcUrl());
        source.setUsername(SQL_SERVER.getUsername());
        source.setPassword(SQL_SERVER.getPassword());
        jdbc = new JdbcTemplate(source);
        matching = new SkillMatchingService(jdbc);
        jdbc.execute("CREATE TABLE dbo.applications (id BIGINT PRIMARY KEY, job_id BIGINT NOT NULL, cv_id BIGINT NULL)");
        jdbc.execute("CREATE TABLE dbo.job_skills (job_id BIGINT NOT NULL, skill_id BIGINT NOT NULL, is_required BIT NOT NULL)");
        jdbc.execute("CREATE TABLE dbo.cv_skills (cv_id BIGINT NOT NULL, skill_id BIGINT NOT NULL)");
    }

    @BeforeEach
    void resetData() {
        jdbc.update("DELETE FROM dbo.cv_skills");
        jdbc.update("DELETE FROM dbo.job_skills");
        jdbc.update("DELETE FROM dbo.applications");
    }

    private void application(long applicationId, long jobId, Long cvId) {
        jdbc.update("INSERT INTO dbo.applications (id,job_id,cv_id) VALUES (?,?,?)", applicationId, jobId, cvId);
    }

    private void jobSkills(long jobId, int... skills) {
        for (int skill : skills) jdbc.update("INSERT INTO dbo.job_skills VALUES (?,?,1)", jobId, skill);
    }

    private void cvSkills(long cvId, int... skills) {
        for (int skill : skills) jdbc.update("INSERT INTO dbo.cv_skills VALUES (?,?)", cvId, skill);
    }

    private Map<String,Object> row(long id) {
        Map<String,Object> row = new HashMap<>();
        row.put("id",id);
        return row;
    }

    @Test @Order(1)
    @DisplayName("INT_SM_01 - Job 5 Skills / CV 4 Skills: 80% qua SQL Server thật")
    void fiveRequiredFourMatched() {
        application(2,1,1L); jobSkills(1,2,3,4,5,6); cvSkills(1,2,3,4,5);
        Map<String,Object> app=row(2);
        matching.attachScores(List.of(app));
        assertAll(() -> assertEquals(5,app.get("requiredSkills")),
                  () -> assertEquals(4,app.get("matchedSkills")),
                  () -> assertEquals(80,app.get("matchPercentage")));
    }

    @Test @Order(2)
    @DisplayName("INT_SM_02 - Job chưa gắn kỹ năng: NULL thay vì 0%")
    void noSkillsOnJob() {
        application(1,7,1L); cvSkills(1,2,3);
        Map<String,Object> app=row(1); matching.attachScores(List.of(app));
        assertEquals(0,app.get("requiredSkills"));
        assertNull(app.get("matchPercentage"));
    }

    @Test @Order(3)
    @DisplayName("INT_SM_03 - CV không có kỹ năng: 0%")
    void noSkillsOnCv() {
        application(3,1,1L); jobSkills(1,2,3);
        Map<String,Object> app=row(3); matching.attachScores(List.of(app));
        assertEquals(0,app.get("matchPercentage"));
    }

    @Test @Order(4)
    @DisplayName("INT_SM_04 - CV đủ kỹ năng: 100%")
    void cvMatchesAll() {
        application(4,1,1L); jobSkills(1,2,3); cvSkills(1,2,3);
        Map<String,Object> app=row(4); matching.attachScores(List.of(app));
        assertEquals(100,app.get("matchPercentage"));
    }

    @Test @Order(5)
    @DisplayName("INT_SM_05 - Batch nhiều Application không bị lẫn điểm")
    void severalApplicationsIndependent() {
        application(1,7,1L); application(2,1,2L); application(3,3,3L);
        jobSkills(1,2,3,4,5,6); cvSkills(2,2,3,4,5);
        jobSkills(3,7,8); cvSkills(3,7);
        Map<String,Object> a=row(1),b=row(2),c=row(3);
        matching.attachScores(List.of(a,b,c));
        assertAll(() -> assertNull(a.get("matchPercentage")),
                  () -> assertEquals(80,b.get("matchPercentage")),
                  () -> assertEquals(50,c.get("matchPercentage")));
    }

    @Test @Order(6)
    @DisplayName("INT_SM_06 - Một skill lặp trong CV không được đếm đôi")
    void duplicateSkillsDoNotInflateMatch() {
        application(6,1,1L); jobSkills(1,2,3); cvSkills(1,2,2);
        Map<String,Object> app=row(6); matching.attachScores(List.of(app));
        assertEquals(1,app.get("matchedSkills"));
        assertEquals(50,app.get("matchPercentage"));
    }

    @Test @Order(7)
    @DisplayName("INT_SM_07 - Skill không bắt buộc không được đưa vào mẫu số")
    void optionalSkillExcluded() {
        application(7,1,1L); jobSkills(1,2,3); cvSkills(1,2);
        jdbc.update("INSERT INTO dbo.job_skills VALUES (?,?,0)",1,4);
        Map<String,Object> app=row(7); matching.attachScores(List.of(app));
        assertEquals(2,app.get("requiredSkills"));
        assertEquals(1,app.get("matchedSkills"));
        assertEquals(50,app.get("matchPercentage"));
    }

    @Test @Order(8)
    @DisplayName("INT_SM_08 - CV NULL và Job có Skills => 0%")
    void applicationWithoutCv() {
        application(8,1,null); jobSkills(1,2,3);
        Map<String,Object> app=row(8); matching.attachScores(List.of(app));
        assertEquals(0,app.get("matchPercentage"));
    }
}
