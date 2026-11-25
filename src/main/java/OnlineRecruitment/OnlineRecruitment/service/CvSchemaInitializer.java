package OnlineRecruitment.OnlineRecruitment.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Khởi tạo các bảng phụ của module CV (nếu chưa có) và seed 5 mẫu CV chuẩn.
 * Tách ra từ CvService để service chỉ còn nghiệp vụ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CvSchemaInitializer {

    private final DataSource dataSource;

    @PostConstruct
    public void initCvTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. cv_projects
            stmt.execute("IF OBJECT_ID('dbo.cv_projects', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_projects (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  name NVARCHAR(255) NOT NULL," +
                    "  role NVARCHAR(255) NULL," +
                    "  start_date DATE NULL," +
                    "  end_date DATE NULL," +
                    "  project_url NVARCHAR(1000) NULL," +
                    "  description NVARCHAR(MAX) NULL," +
                    "  technologies NVARCHAR(MAX) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_projects_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");

            // 2. cv_certificates
            stmt.execute("IF OBJECT_ID('dbo.cv_certificates', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_certificates (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  name NVARCHAR(255) NOT NULL," +
                    "  issuing_org NVARCHAR(255) NULL," +
                    "  issue_date DATE NULL," +
                    "  expiry_date DATE NULL," +
                    "  credential_id NVARCHAR(255) NULL," +
                    "  credential_url NVARCHAR(1000) NULL," +
                    "  description NVARCHAR(MAX) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_certificates_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");
            stmt.execute("IF COL_LENGTH('dbo.cv_certificates', 'description') IS NULL " +
                    "ALTER TABLE dbo.cv_certificates ADD description NVARCHAR(MAX) NULL");

            // 3. cv_awards
            stmt.execute("IF OBJECT_ID('dbo.cv_awards', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_awards (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  name NVARCHAR(255) NOT NULL," +
                    "  issuer NVARCHAR(255) NULL," +
                    "  award_date DATE NULL," +
                    "  description NVARCHAR(MAX) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_awards_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");

            // 4. cv_activities
            stmt.execute("IF OBJECT_ID('dbo.cv_activities', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_activities (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  organization NVARCHAR(255) NOT NULL," +
                    "  role NVARCHAR(255) NULL," +
                    "  start_date DATE NULL," +
                    "  end_date DATE NULL," +
                    "  description NVARCHAR(MAX) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_activities_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");

            // 5. cv_references
            stmt.execute("IF OBJECT_ID('dbo.cv_references', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_references (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  full_name NVARCHAR(255) NOT NULL," +
                    "  position NVARCHAR(255) NULL," +
                    "  organization NVARCHAR(255) NULL," +
                    "  email NVARCHAR(255) NULL," +
                    "  phone NVARCHAR(30) NULL," +
                    "  relationship NVARCHAR(255) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_references_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");
            stmt.execute("IF COL_LENGTH('dbo.cv_references', 'relationship') IS NULL " +
                    "ALTER TABLE dbo.cv_references ADD relationship NVARCHAR(255) NULL");

            // 6. cv_templates & cv_template_versions
            stmt.execute("IF OBJECT_ID('dbo.cv_templates', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_templates (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  category_id BIGINT NOT NULL DEFAULT 1," +
                    "  name NVARCHAR(150) NOT NULL," +
                    "  code NVARCHAR(100) NOT NULL UNIQUE," +
                    "  status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE'" +
                    ")");

            stmt.execute("IF OBJECT_ID('dbo.cv_template_versions', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_template_versions (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  template_id BIGINT NOT NULL," +
                    "  version_number INT NOT NULL DEFAULT 1," +
                    "  layout_definition NVARCHAR(MAX) NOT NULL DEFAULT ''," +
                    "  is_published BIT NOT NULL DEFAULT 1," +
                    "  CONSTRAINT FK_cv_template_versions_template FOREIGN KEY (template_id) REFERENCES dbo.cv_templates(id) ON DELETE CASCADE" +
                    ")");

            // Seed 5 templates if not exists
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'STANDARD') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Tiêu chuẩn', 'STANDARD', 'ACTIVE')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'MINIMALIST') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Tiêu chuẩn (Ít kinh nghiệm)', 'MINIMALIST', 'ACTIVE')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'CREATIVE') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Thanh lịch', 'CREATIVE', 'ACTIVE')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'MODERN') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Hiện đại (2 cột)', 'MODERN', 'ACTIVE')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'ATS_FRIENDLY') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Chuẩn ATS', 'ATS_FRIENDLY', 'ACTIVE')");

            // Seed versions
            stmt.execute("INSERT INTO dbo.cv_template_versions (template_id, version_number, layout_definition, is_published) " +
                    "SELECT t.id, 1, t.code, 1 FROM dbo.cv_templates t " +
                    "WHERE NOT EXISTS (SELECT 1 FROM dbo.cv_template_versions v WHERE v.template_id = t.id AND v.version_number = 1)");

            log.info("Initialized CV extra tables and 5 templates successfully.");
        } catch (Exception e) {
            log.warn("Notice while initializing CV tables: {}", e.getMessage());
        }
    }
}
