package OnlineRecruitment.OnlineRecruitment.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/** Database integration: JDBC -> REAL isolated SQL Server; verifies Job/Category/Skill SQL constraints and flows.
 * This is NOT an end-to-end HTTP->service->JPA integration suite. */
@Testcontainers
@EnabledIfEnvironmentVariable(named="RUN_SQLSERVER_IT", matches="true")
class JobCategorySkillSqlIntegrationTest {
    @Container static final MSSQLServerContainer<?> db = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
    static JdbcTemplate jdbc;

    @BeforeAll static void schema() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        ds.setUrl(db.getJdbcUrl()); ds.setUsername(db.getUsername()); ds.setPassword(db.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE dbo.job_categories (id BIGINT IDENTITY PRIMARY KEY, name NVARCHAR(150) NOT NULL UNIQUE, description NVARCHAR(MAX), status NVARCHAR(20) NOT NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME())");
        jdbc.execute("CREATE TABLE dbo.skills (id BIGINT IDENTITY PRIMARY KEY, name NVARCHAR(150) NOT NULL, status NVARCHAR(20) NOT NULL, category_id BIGINT NULL REFERENCES dbo.job_categories(id))");
        jdbc.execute("CREATE TABLE dbo.jobs (id BIGINT IDENTITY PRIMARY KEY, title NVARCHAR(255) NOT NULL, category_id BIGINT NOT NULL REFERENCES dbo.job_categories(id), status NVARCHAR(20) NOT NULL, rejection_reason NVARCHAR(500) NULL)");
        jdbc.execute("CREATE TABLE dbo.job_skills (job_id BIGINT NOT NULL REFERENCES dbo.jobs(id), skill_id BIGINT NOT NULL REFERENCES dbo.skills(id), is_required BIT NOT NULL, PRIMARY KEY(job_id,skill_id))");
    }
    @BeforeEach void clean() {
        jdbc.update("DELETE FROM dbo.job_skills"); jdbc.update("DELETE FROM dbo.jobs"); jdbc.update("DELETE FROM dbo.skills"); jdbc.update("DELETE FROM dbo.job_categories");
    }
    private long category(String name) {
        jdbc.update("INSERT dbo.job_categories(name,status) VALUES (?,?)", name,"ACTIVE");
        return jdbc.queryForObject("SELECT id FROM dbo.job_categories WHERE name=?",Long.class,name);
    }
    private long skill(long categoryId,String name) {
        jdbc.update("INSERT dbo.skills(name,status,category_id) VALUES (?,'ACTIVE',?)",name,categoryId);
        return jdbc.queryForObject("SELECT id FROM dbo.skills WHERE category_id=? AND name=?",Long.class,categoryId,name);
    }
    private long job(long categoryId,String name) {
        jdbc.update("INSERT dbo.jobs(title,category_id,status) VALUES (?,?,'PENDING')",name,categoryId);
        return jdbc.queryForObject("SELECT id FROM dbo.jobs WHERE title=?",Long.class,name);
    }
    @Test void categoryCreateAndRead() {
        long id=category("Backend");
        assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM dbo.job_categories WHERE id=?",String.class,id));
    }
    @Test void categoryUpdatePreservesId() {
        long id=category("Backend");
        jdbc.update("UPDATE dbo.job_categories SET name=?, status=? WHERE id=?","Backend Dev","INACTIVE",id);
        assertEquals("Backend Dev",jdbc.queryForObject("SELECT name FROM dbo.job_categories WHERE id=?",String.class,id));
        assertEquals("INACTIVE",jdbc.queryForObject("SELECT status FROM dbo.job_categories WHERE id=?",String.class,id));
    }
    @Test void categoryNameUniqueConstraint() {
        category("IT");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->category("IT"));
    }
    @Test void skillCreateAndChangeCategory() {
        long a=category("IT"), b=category("Design"); long id=skill(a,"Java");
        jdbc.update("UPDATE dbo.skills SET category_id=?, status='INACTIVE' WHERE id=?",b,id);
        assertEquals(b,jdbc.queryForObject("SELECT category_id FROM dbo.skills WHERE id=?",Long.class,id));
        assertEquals("INACTIVE",jdbc.queryForObject("SELECT status FROM dbo.skills WHERE id=?",String.class,id));
    }
    @Test void skillCannotReferToUnknownCategory() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->skill(999999,"Java"));
    }
    @Test void jobCreateApproveRejectAndClose() {
        long id=job(category("IT"),"Backend engineer");
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM dbo.jobs WHERE id=?",String.class,id));
        jdbc.update("UPDATE dbo.jobs SET status='ACTIVE' WHERE id=?",id);
        assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM dbo.jobs WHERE id=?",String.class,id));
        jdbc.update("UPDATE dbo.jobs SET status='CLOSED' WHERE id=?",id);
        assertEquals("CLOSED",jdbc.queryForObject("SELECT status FROM dbo.jobs WHERE id=?",String.class,id));
    }
    @Test void jobRejectedReasonIsPersisted() {
        long id=job(category("IT"),"Backend engineer");
        jdbc.update("UPDATE dbo.jobs SET status='REJECTED',rejection_reason=? WHERE id=?","Missing salary",id);
        assertEquals("Missing salary",jdbc.queryForObject("SELECT rejection_reason FROM dbo.jobs WHERE id=?",String.class,id));
    }
    @Test void jobRetainsCategoryReferenceAfterCategoryRename() {
        long cid=category("Java"), jid=job(cid,"Java dev");
        jdbc.update("UPDATE dbo.job_categories SET name='Backend' WHERE id=?",cid);
        assertEquals("Backend",jdbc.queryForObject("SELECT c.name FROM dbo.jobs j JOIN dbo.job_categories c ON c.id=j.category_id WHERE j.id=?",String.class,jid));
    }
    @Test void jobSkillRelationPersists() {
        long cat=category("IT"), jid=job(cat,"API developer"), sid=skill(cat,"REST API");
        jdbc.update("INSERT dbo.job_skills VALUES (?,?,1)",jid,sid);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.job_skills WHERE job_id=? AND skill_id=?",Integer.class,jid,sid));
    }
    @Test void duplicateJobSkillRelationRejected() {
        long cat=category("IT"), jid=job(cat,"Java developer"), sid=skill(cat,"Java");
        jdbc.update("INSERT dbo.job_skills VALUES (?,?,1)",jid,sid);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT dbo.job_skills VALUES (?,?,1)",jid,sid));
    }
    @Test void jobCannotReferToUnknownCategory() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->job(999999,"Ghost"));
    }
}
