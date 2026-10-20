-- Chay mot lan tren SQL Server neu bang jobs chua co cot logo_url.
USE OnlineRecruitment;
GO
IF COL_LENGTH('dbo.jobs', 'logo_url') IS NULL
    ALTER TABLE dbo.jobs ADD logo_url VARCHAR(500) NULL;
GO
-- Kiem tra logo tung Job:
SELECT id, title, logo_url FROM dbo.jobs ORDER BY id;
