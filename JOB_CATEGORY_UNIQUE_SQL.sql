-- SQL Server: chạy một lần sau khi kiểm tra dữ liệu trùng.
-- Database: OnlineRecruitment. KHÔNG chạy trên DB khác.
USE [OnlineRecruitment];
GO
-- Rà soát trùng tên, không phân biệt hoa/thường, khoảng trắng đầu/cuối.
SELECT LOWER(LTRIM(RTRIM([name])) COLLATE Latin1_General_100_CI_AS) AS normalized_name,
       COUNT(*) AS duplicate_count
FROM dbo.job_categories
GROUP BY LOWER(LTRIM(RTRIM([name])) COLLATE Latin1_General_100_CI_AS)
HAVING COUNT(*) > 1;
GO
-- Nếu query trên trả về dòng, giải quyết bản ghi trùng trước khi tiếp tục.
-- Ràng buộc unique theo tên chuẩn hóa (case-insensitive):
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.job_categories') AND name = N'name_normalized'
)
BEGIN
    ALTER TABLE dbo.job_categories ADD name_normalized AS
        LOWER(LTRIM(RTRIM([name])) COLLATE Latin1_General_100_CI_AS) PERSISTED;
END;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'dbo.job_categories')
                 AND name = N'UX_job_categories_name_normalized')
BEGIN
    CREATE UNIQUE INDEX UX_job_categories_name_normalized
        ON dbo.job_categories(name_normalized);
END;
GO
-- Lưu ý: app chuẩn hóa cả khoảng trắng liên tiếp khi tạo/sửa.
-- Dữ liệu cũ có khoảng trắng giữa các từ khác nhau cần được làm sạch thủ công nếu cần.
