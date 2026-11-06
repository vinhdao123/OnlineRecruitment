-- =========================================================================
-- SCRIPT CẬP NHẬT DATABASE (DATABASE UPDATE SCRIPT)
-- Hệ thống: Online Recruitment
-- Hệ quản trị CSDL: Microsoft SQL Server (SSMS)
-- =========================================================================

USE [OnlineRecruitment];
GO

-- =========================================================================
-- 1. TẠO BẢNG NGƯỠNG XẾP LOẠI BÀI THI KỸ NĂNG (SKILL TEST RATING THRESHOLDS)
--    (Chuyển logic xếp loại từ hardcode backend lên Database)
-- =========================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[skill_test_rating_thresholds]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[skill_test_rating_thresholds] (
        [id]            BIGINT IDENTITY(1,1) PRIMARY KEY,
        [min_score]     INT            NOT NULL,  -- Điểm tối thiểu (0 - 100)
        [label]         NVARCHAR(100)  NOT NULL,  -- Xuất sắc, Giỏi, Khá, Đạt, Chưa đạt
        [display_order] INT            NOT NULL DEFAULT 0,
        [created_at]    DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
        [updated_at]    DATETIME2      NOT NULL DEFAULT SYSDATETIME()
    );
    PRINT N'-> Đã tạo bảng [skill_test_rating_thresholds] thành công.';
END
ELSE
BEGIN
    PRINT N'-> Bảng [skill_test_rating_thresholds] đã tồn tại.';
END
GO

-- =========================================================================
-- 2. INSERT DỮ LIỆU CÁC MỐC ĐIỂM XẾP LOẠI MẶC ĐỊNH
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM [dbo].[skill_test_rating_thresholds])
BEGIN
    INSERT INTO [dbo].[skill_test_rating_thresholds] ([min_score], [label], [display_order])
    VALUES
        (90, N'Xuất sắc', 1),
        (80, N'Giỏi',     2),
        (65, N'Khá',      3),
        (50, N'Đạt',      4),
        (0,  N'Chưa đạt', 5);
    PRINT N'-> Đã nạp dữ liệu ngưỡng xếp loại mặc định thành công.';
END
ELSE
BEGIN
    PRINT N'-> Dữ liệu [skill_test_rating_thresholds] đã có sẵn.';
END
GO

-- =========================================================================
-- 3. BỔ SUNG CỘT [updated_at] VÀO BẢNG [skill_tests] (NẾU THIẾU)
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.skill_tests') AND name = 'updated_at')
BEGIN
    ALTER TABLE dbo.skill_tests ADD updated_at DATETIME2 NULL;
    PRINT N'-> Đã thêm cột [updated_at] vào bảng [skill_tests].';
END
ELSE
BEGIN
    PRINT N'-> Cột [updated_at] trong [skill_tests] đã tồn tại.';
END
GO

-- =========================================================================
-- 4. FIX LỖI USER MỒ CÔI (USER ID 38 KHÔNG TỒN TẠI TRONG BẢNG USERS)
--    Tự động chèn tài khoản Recruiter bù cho các recruiter_id bị thiếu
-- =========================================================================
IF EXISTS (SELECT 1 FROM companies WHERE recruiter_id IS NOT NULL AND recruiter_id NOT IN (SELECT id FROM users))
BEGIN
    DECLARE @orphan_id BIGINT;
    DECLARE cur CURSOR FOR 
        SELECT DISTINCT recruiter_id FROM companies 
        WHERE recruiter_id IS NOT NULL AND recruiter_id NOT IN (SELECT id FROM users);
    
    OPEN cur;
    FETCH NEXT FROM cur INTO @orphan_id;
    
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET IDENTITY_INSERT users ON;
        INSERT INTO users (id, username, email, password_hash, full_name, role, status, created_at, updated_at)
        VALUES (
            @orphan_id, 
            'recruiter_' + CAST(@orphan_id AS NVARCHAR(20)), 
            'recruiter_' + CAST(@orphan_id AS NVARCHAR(20)) + '@system.local', 
            '$2a$10$76gXQeZq4sAcm1z7Jq9kMe6YJq4sAcm1z7Jq9kMe6YJq4sAcm1z7J', 
            N'Nhà tuyển dụng #' + CAST(@orphan_id AS NVARCHAR(20)), 
            'RECRUITER', 
            'ACTIVE', 
            SYSDATETIME(), 
            SYSDATETIME()
        );
        SET IDENTITY_INSERT users OFF;
        PRINT N'-> Đã tự động tạo bù User ID ' + CAST(@orphan_id AS NVARCHAR(20)) + N' cho bảng companies.';
        FETCH NEXT FROM cur INTO @orphan_id;
    END;
    
    CLOSE cur;
    DEALLOCATE cur;
END
ELSE
BEGIN
    PRINT N'-> Dữ liệu liên kết giữa companies và users đã toàn vẹn, không có User mồ côi.';
END
GO

PRINT N'======================================================';
PRINT N'HOÀN TẤT CẬP NHẬT CƠ SỞ DỮ LIỆU THÀNH CÔNG!';
PRINT N'======================================================';
GO
