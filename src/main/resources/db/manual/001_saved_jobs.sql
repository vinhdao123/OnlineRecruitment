-- Chạy trên database của ứng dụng. Không xóa hoặc sửa dữ liệu hiện có.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.saved_jobs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.saved_jobs (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_saved_jobs PRIMARY KEY,
        user_id BIGINT NOT NULL,
        job_id BIGINT NOT NULL,
        saved_at DATETIME2 NOT NULL CONSTRAINT DF_saved_jobs_saved_at DEFAULT SYSDATETIME(),
        CONSTRAINT FK_saved_jobs_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_saved_jobs_job FOREIGN KEY (job_id) REFERENCES dbo.jobs(id),
        CONSTRAINT UQ_saved_jobs_user_job UNIQUE (user_id, job_id)
    );
END;

COMMIT TRANSACTION;
