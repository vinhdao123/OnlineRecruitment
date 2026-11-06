-- =====================================================================
-- BẢNG DỮ LIỆU ĐẦU MỤC CV TRONG CƠ SỞ DỮ LIỆU [OnlineRecruitment]
-- Lưu ý: Database của bạn đã có sẵn các bảng này với cấu trúc chuẩn dưới đây:
-- =====================================================================

-- 1. Dự án tiêu biểu
IF OBJECT_ID('dbo.cv_projects', 'U') IS NULL
CREATE TABLE dbo.cv_projects (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    cv_id         BIGINT         NOT NULL,
    name          NVARCHAR(255)  NOT NULL,
    role          NVARCHAR(255)  NULL,
    start_date    DATE           NULL,
    end_date      DATE           NULL,
    project_url   NVARCHAR(1000) NULL,
    description   NVARCHAR(MAX)  NULL,
    technologies  NVARCHAR(MAX)  NULL,
    display_order INT            NOT NULL DEFAULT 0,
    CONSTRAINT FK_cv_projects_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE
);
GO

-- 2. Chứng chỉ
IF OBJECT_ID('dbo.cv_certificates', 'U') IS NULL
CREATE TABLE dbo.cv_certificates (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    cv_id          BIGINT         NOT NULL,
    name           NVARCHAR(255)  NOT NULL,
    issuing_org    NVARCHAR(255)  NULL,
    issue_date     DATE           NULL,
    expiry_date    DATE           NULL,
    credential_id  NVARCHAR(255)  NULL,
    credential_url NVARCHAR(1000) NULL,
    display_order  INT            NOT NULL DEFAULT 0,
    CONSTRAINT FK_cv_certificates_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE
);
GO

-- 3. Giải thưởng
IF OBJECT_ID('dbo.cv_awards', 'U') IS NULL
CREATE TABLE dbo.cv_awards (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    cv_id         BIGINT        NOT NULL,
    name          NVARCHAR(255) NOT NULL,
    issuer        NVARCHAR(255) NULL,
    award_date    DATE          NULL,
    description   NVARCHAR(MAX) NULL,
    display_order INT           NOT NULL DEFAULT 0,
    CONSTRAINT FK_cv_awards_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE
);
GO

-- 4. Hoạt động ngoại khóa
IF OBJECT_ID('dbo.cv_activities', 'U') IS NULL
CREATE TABLE dbo.cv_activities (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    cv_id         BIGINT        NOT NULL,
    organization  NVARCHAR(255) NOT NULL,
    role          NVARCHAR(255) NULL,
    start_date    DATE          NULL,
    end_date      DATE          NULL,
    description   NVARCHAR(MAX) NULL,
    display_order INT           NOT NULL DEFAULT 0,
    CONSTRAINT FK_cv_activities_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE
);
GO

-- 5. Người tham chiếu
IF OBJECT_ID('dbo.cv_references', 'U') IS NULL
CREATE TABLE dbo.cv_references (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    cv_id         BIGINT        NOT NULL,
    full_name     NVARCHAR(255) NOT NULL,
    position      NVARCHAR(255) NULL,
    organization  NVARCHAR(255) NULL,
    email         NVARCHAR(255) NULL,
    phone         NVARCHAR(30)  NULL,
    display_order INT           NOT NULL DEFAULT 0,
    CONSTRAINT FK_cv_references_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE
);
GO
