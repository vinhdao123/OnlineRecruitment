-- =========================================================================
-- SCRIPT TẠO BẢNG & INSERT DỮ LIỆU MẪU CHO TÍNH NĂNG
-- 1. BỘ CÂU HỎI PHỎNG VẤN (INTERVIEW QUESTIONS)
-- 2. ĐÁNH GIÁ KỸ NĂNG NGHỀ NGHIỆP (SKILL TESTS & QUESTION POOL)
-- Database: OnlineRecruitment (Microsoft SQL Server)
-- =========================================================================

USE [OnlineRecruitment];
GO

-- =========================================================================
-- BƯỚC 1: TẠO 4 BẢNG MỚI ĐỘC LẬP (KHÔNG ẢNH HƯỞNG BẢNG HIỆN CÓ)
-- =========================================================================

-- 1. Bảng lưu trữ Bộ câu hỏi phỏng vấn
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[interview_questions]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[interview_questions] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [question] NVARCHAR(500) NOT NULL,
        [category] NVARCHAR(100) NOT NULL,
        [target_level] NVARCHAR(50) NULL,
        [purpose] NVARCHAR(MAX) NULL,
        [tips] NVARCHAR(MAX) NULL,
        [sample_answer] NVARCHAR(MAX) NULL,
        [pitfalls] NVARCHAR(MAX) NULL,
        [view_count] INT DEFAULT 0,
        [display_order] INT DEFAULT 0,
        [status] NVARCHAR(20) DEFAULT 'ACTIVE',
        [created_at] DATETIME2 DEFAULT SYSDATETIME(),
        [updated_at] DATETIME2 DEFAULT SYSDATETIME()
    );
END
GO

-- 2. Bảng lưu danh sách Đề thi đánh giá kỹ năng
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[skill_tests]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[skill_tests] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [title] NVARCHAR(255) NOT NULL,
        [skill_name] NVARCHAR(100) NOT NULL,
        [category] NVARCHAR(100) NOT NULL,
        [difficulty] NVARCHAR(50) NULL,
        [description] NVARCHAR(MAX) NULL,
        [duration_minutes] INT NOT NULL,
        [total_questions_per_test] INT NOT NULL,
        [passing_score] INT NOT NULL,
        [participant_count] INT DEFAULT 0,
        [badge_color] NVARCHAR(30) NULL,
        [status] NVARCHAR(20) DEFAULT 'ACTIVE',
        [created_at] DATETIME2 DEFAULT SYSDATETIME()
    );
END
GO

-- 3. Bảng lưu Ngân hàng câu hỏi (Question Pool) cho từng đề thi
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[skill_test_questions]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[skill_test_questions] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [skill_test_id] BIGINT NOT NULL,
        [question_text] NVARCHAR(MAX) NOT NULL,
        [option_a] NVARCHAR(MAX) NOT NULL,
        [option_b] NVARCHAR(MAX) NOT NULL,
        [option_c] NVARCHAR(MAX) NOT NULL,
        [option_d] NVARCHAR(MAX) NOT NULL,
        [correct_option] NVARCHAR(10) NOT NULL,
        [explanation] NVARCHAR(MAX) NULL,
        [status] NVARCHAR(20) DEFAULT 'ACTIVE',
        [created_at] DATETIME2 DEFAULT SYSDATETIME(),
        CONSTRAINT [FK_skill_test_questions_skill_tests] FOREIGN KEY ([skill_test_id]) 
            REFERENCES [dbo].[skill_tests]([id]) ON DELETE CASCADE
    );
END
GO

-- 4. Bảng lưu Lịch sử làm bài thi của Ứng viên
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[skill_test_attempts]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[skill_test_attempts] (
        [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
        [user_id] BIGINT NOT NULL,
        [skill_test_id] BIGINT NOT NULL,
        [score] FLOAT NOT NULL,
        [correct_answers] INT NOT NULL,
        [total_questions] INT NOT NULL,
        [is_passed] BIT NOT NULL,
        [performance_rating] NVARCHAR(50) NULL,
        [time_spent_seconds] INT NULL,
        [answers_json] NVARCHAR(MAX) NULL,
        [completed_at] DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT [FK_skill_test_attempts_users] FOREIGN KEY ([user_id]) 
            REFERENCES [dbo].[users]([id]) ON DELETE CASCADE,
        CONSTRAINT [FK_skill_test_attempts_skill_tests] FOREIGN KEY ([skill_test_id]) 
            REFERENCES [dbo].[skill_tests]([id]) ON DELETE CASCADE
    );
END
GO

-- =========================================================================
-- BƯỚC 2: CHÈN DỮ LIỆU MẪU CÂU HỎI PHỎNG VẤN (NẾU CHƯA CÓ)
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM [dbo].[interview_questions])
BEGIN
    INSERT INTO [dbo].[interview_questions] 
        ([question], [category], [target_level], [purpose], [tips], [sample_answer], [pitfalls], [display_order], [view_count], [status])
    VALUES
    (
        N'Hãy giới thiệu ngắn gọn về bản thân bạn?',
        N'Phỏng vấn chung',
        N'Mọi cấp bậc',
        N'Nhà tuyển dụng muốn đánh giá khả năng giao tiếp, sự tự tin và mức độ phù hợp của bạn với văn hóa và vị trí ứng tuyển.',
        N'Áp dụng cấu trúc Hiện tại - Quá khứ - Tương lai: Tóm tắt vị trí hiện tại, kinh nghiệm nổi bật liên quan trực tiếp và lý do muốn gia nhập công ty.',
        N'Em là Nguyễn Văn A, tốt nghiệp chuyên ngành CNTT. Hiện em có 2 năm kinh nghiệm phát triển backend với Java/Spring Boot. Ở dự án gần nhất, em phụ trách tối ưu hóa API và giảm 30% thời gian phản hồi hệ thống. Em tìm kiếm cơ hội tại quý công ty vì ấn tượng với môi trường chuyên nghiệp và quy mô sản phẩm.',
        N'Tránh kể lại toàn bộ sơ yếu lý lịch từ thời phổ thông hoặc nói lan man về sở thích cá nhân không liên quan đến công việc.',
        1, 135, 'ACTIVE'
    ),
    (
        N'Điểm mạnh lớn nhất của bạn là gì?',
        N'Phỏng vấn chung',
        N'Mọi cấp bậc',
        N'Đánh giá bạn có tự nhận thức được giá trị bản thân và kỹ năng đó có giải quyết được bài toán của công ty hay không.',
        N'Chọn 1-2 điểm mạnh liên quan mật thiết nhất đến mô tả công việc (JD), kèm theo ví dụ thực tế chứng minh (kết quả có số liệu nếu có).',
        N'Điểm mạnh lớn nhất của em là khả năng tự học công nghệ mới và tư duy giải quyết vấn đề. Khi dự án trước yêu cầu tích hợp giải pháp thanh toán chưa từng làm, em đã chủ động nghiên cứu tài liệu kỹ thuật và hoàn thành module đúng hạn trong 1 tuần.',
        N'Tránh nói những câu chung chung sáo rỗng như ''Em rất chăm chỉ, cầu tiến'' mà không đưa ra được minh chứng thực tế.',
        2, 170, 'ACTIVE'
    ),
    (
        N'Điểm yếu lớn nhất của bạn là gì và bạn khắc phục nó như thế nào?',
        N'Phỏng vấn chung',
        N'Mọi cấp bậc',
        N'Kiểm tra tính trung thực, khả năng tự nhận thức và nỗ lực hoàn thiện bản thân của ứng viên.',
        N'Chọn một điểm yếu thực sự nhưng không gây chí mạng cho công việc, tập trung 70% thời gian nói về các hành động cụ thể bạn đang thực hiện để cải thiện.',
        N'Trước đây em thường ôm đồm nhiều việc vì sợ làm phiền đồng nghiệp. Sau đó em nhận ra điều này ảnh hưởng tới tiến độ chung, nên em đã học cách sử dụng công cụ quản lý Trello và chủ động trao đổi chia sẻ đầu việc trong các buổi Daily Scrum.',
        N'Tuyệt đối không trả lời ''Em không có điểm yếu nào'' hoặc trá hình ''Điểm yếu của em là quá cầu toàn, ham mê công việc''.',
        3, 205, 'ACTIVE'
    ),
    (
        N'Hãy kể về một lần bạn gặp thất bại hoặc bất đồng ý kiến trong công việc và cách bạn vượt qua?',
        N'Tình huống & Hành vi',
        N'Mọi cấp bậc',
        N'Đánh giá trí tuệ cảm xúc (EQ), kỹ năng làm việc nhóm, kỹ năng giải quyết mâu thuẫn và thái độ khi đối mặt khó khăn.',
        N'Sử dụng mô hình STAR (Situation - Tình huống, Task - Nhiệm vụ, Action - Hành động, Result - Kết quả & Bài học rút ra).',
        N'Trong dự án xây dựng cổng thanh toán, em và bạn Tech Lead có bất đồng về việc lựa chọn kiến trúc Microservices hay Monolith. Thay vì tranh cãi cá nhân, em đã lập bảng so sánh chi phí server, thời gian phát triển và khả năng mở rộng. Cuối cùng cả đội thống nhất giải pháp phù hợp nhất và dự án ra mắt đúng hạn.',
        N'Tránh đổ lỗi cho đồng nghiệp, sếp cũ hoặc tỏ thái độ tiêu cực, bực bội khi nhắc lại sự việc.',
        4, 240, 'ACTIVE'
    ),
    (
        N'Mục tiêu nghề nghiệp trong 3 - 5 năm tới của bạn là gì?',
        N'Định hướng nghề nghiệp',
        N'Mọi cấp bậc',
        N'Nhà tuyển dụng muốn biết bạn có kế hoạch gắn bó lâu dài hay chỉ coi đây là trạm dừng chân ngắn hạn.',
        N'Chia mục tiêu thành 2 phần: Trong 1-2 năm đầu thành thạo quy trình và tạo giá trị cho vị trí này; 3-5 năm tới nâng cao năng lực chuyên sâu hoặc hướng tới vai trò quản lý / chuyên gia.',
        N'Trong 1-2 năm tới, em muốn làm chủ hoàn toàn hệ thống của công ty và nâng cao kỹ năng tối ưu kiến trúc. Trong 3-5 năm, em hướng tới vai trò Senior Developer hoặc Team Leader, có thể dẫn dắt các bạn mới và đóng góp vào định hướng kỹ thuật.',
        N'Tránh trả lời mơ hồ kiểu ''Em muốn kiếm thật nhiều tiền'' hoặc lộ rõ kế hoạch tách ra mở công ty riêng trong tương lai gần.',
        5, 275, 'ACTIVE'
    ),
    (
        N'Tại sao bạn lại nghỉ việc ở công ty cũ?',
        N'Phỏng vấn chung',
        N'Junior / Middle',
        N'Đánh giá thái độ của bạn với công ty cũ và động lực thực sự khi tìm kiếm công việc mới.',
        N'Tập trung vào khía cạnh tích cực: mong muốn tìm kiếm thử thách mới, mở rộng phạm vi chuyên môn hoặc môi trường phù hợp với định hướng phát triển dài hạn.',
        N'Em rất biết ơn thời gian làm việc tại công ty cũ vì đã giúp em xây dựng nền tảng vững chắc. Tuy nhiên, em nhận thấy mô hình công ty hiện tại đã bão hòa về cơ hội phát triển chuyên môn, nên em muốn tìm kiếm môi trường mới có quy mô sản phẩm lớn hơn để tiếp tục nâng cao kỹ năng.',
        N'Không bao giờ nói xấu sếp cũ, đồng nghiệp cũ hoặc than phiền về chế độ đãi ngộ.',
        6, 310, 'ACTIVE'
    ),
    (
        N'Mức lương kỳ vọng của bạn là bao nhiêu?',
        N'Đàm phán lương',
        N'Mọi cấp bậc',
        N'Kiểm tra xem mức lương bạn kỳ vọng có nằm trong ngân sách của công ty và bạn định giá bản thân ra sao.',
        N'Tìm hiểu trước dải lương thị trường cho vị trí tương đương. Đưa ra một khoảng lương (range) linh hoạt thay vì một con số cố định cứng nhắc.',
        N'Qua tìm hiểu về yêu cầu công việc và mặt bằng thị trường đối với vị trí Backend Java 2 năm kinh nghiệm, em kỳ vọng mức lương trong khoảng từ 18 - 22 triệu đồng. Tuy nhiên em hoàn toàn cởi mở để thảo luận thêm dựa trên tổng gói đãi ngộ và lộ trình đánh giá của công ty.',
        N'Tránh đưa ra con số quá cao phi thực tế hoặc quá thấp tự hạ thấp giá trị bản thân, hoặc nói ''Công ty trả bao nhiêu cũng được''.',
        7, 345, 'ACTIVE'
    ),
    (
        N'Bạn có câu hỏi nào dành cho chúng tôi không?',
        N'Phỏng vấn chung',
        N'Mọi cấp bậc',
        N'Đánh giá mức độ quan tâm, sự chuẩn bị chu đáo và tư duy chủ động của ứng viên đối với công ty.',
        N'Chuẩn bị trước 2-3 câu hỏi chất lượng về văn hóa nhóm, định hướng sản phẩm hoặc tiêu chí đánh giá thành công của vị trí.',
        N'Em muốn hỏi thêm về: Trong 3 tháng thử việc đầu tiên, tiêu chí quan trọng nhất để đánh giá một nhân viên thành công ở vị trí này là gì? Và đội ngũ phát triển hiện đang áp dụng quy trình kiểm thử và deploy ra sao?',
        N'Tránh trả lời ''Em không có câu hỏi nào'' hoặc chỉ chăm chăm hỏi về giờ giải lao, ngày nghỉ phép ngay từ vòng đầu.',
        8, 380, 'ACTIVE'
    ),
    (
        N'Bạn sử dụng AI (ChatGPT, Copilot,...) trong công việc như thế nào?',
        N'Kỹ năng công nghệ',
        N'Mọi cấp bậc',
        N'Xem xét bạn có cập nhật xu hướng công nghệ để tối ưu năng suất làm việc hay bị phụ thuộc hoàn toàn vào công cụ.',
        N'Nhấn mạnh AI là công cụ hỗ trợ tăng tốc (viết test case, gợi ý cú pháp, review tài liệu), nhưng bạn luôn kiểm tra lại và hiểu sâu bản chất logic.',
        N'Em thường xuyên sử dụng AI để hỗ trợ tra cứu cú pháp, viết các đoạn unit test mẫu hoặc tối ưu câu lệnh SQL. Tuy nhiên, em luôn tự review cẩn thận để đảm bảo bảo mật mã nguồn công ty và không sao chép mù quáng những đoạn mã chưa nắm rõ nguyên lý.',
        N'Tránh trả lời bài xích công nghệ hoặc thừa nhận phó mặc toàn bộ code cho AI viết.',
        9, 415, 'ACTIVE'
    );
END
GO

-- =========================================================================
-- BƯỚC 3: CHÈN DỮ LIỆU ĐỀ THI & QUESTION POOL (NẾU CHƯA CÓ)
-- =========================================================================
IF NOT EXISTS (SELECT 1 FROM [dbo].[skill_tests])
BEGIN
    -- 1. Java Test
    INSERT INTO [dbo].[skill_tests] 
        ([title], [skill_name], [category], [difficulty], [description], [duration_minutes], [total_questions_per_test], [passing_score], [participant_count], [badge_color], [status])
    VALUES 
        (N'Đề thi đánh giá kỹ năng Lập trình Java', N'Java', N'Kỹ năng công nghệ', N'Trung bình', 
         N'Kiểm tra kiến thức cốt lõi về Java Core, OOP, Collections, Multi-threading, Exception handling và Java 8 Features.', 
         15, 10, 60, 1420, N'#f97316', N'ACTIVE');
    DECLARE @javaId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [dbo].[skill_test_questions] ([skill_test_id], [question_text], [option_a], [option_b], [option_c], [option_d], [correct_option], [explanation], [status])
    VALUES
    (@javaId, N'Trong Java, tính chất nào của OOP cho phép một class có nhiều phương thức cùng tên nhưng khác tham số?',
     N'Tính đa hình (Polymorphism / Method Overloading)', N'Tính đóng gói (Encapsulation)', N'Tính kế thừa (Inheritance)', N'Tính trừu tượng (Abstraction)', 'A',
     N'Method Overloading cho phép định nghĩa nhiều method cùng tên nhưng khác nhau về số lượng hoặc kiểu tham số trong cùng một class.', 'ACTIVE'),
    (@javaId, N'Collection nào trong Java không cho phép phần tử trùng lặp và không đảm bảo thứ tự thêm vào?',
     N'ArrayList', N'HashSet', N'LinkedList', N'TreeSet', 'B',
     N'HashSet cài đặt giao diện Set, không cho phép phần tử trùng lặp và sử dụng HashTable nên không đảm bảo thứ tự thêm vào.', 'ACTIVE'),
    (@javaId, N'Từ khóa ''final'' khi được khai báo trước một phương thức (method) có ý nghĩa gì?',
     N'Phương thức không thể bị ghi đè (override) ở lớp con', N'Phương thức không thể truy cập từ bên ngoài', N'Phương thức là phương thức trừu tượng', N'Phương thức chạy nhanh hơn', 'A',
     N'Phương thức ''final'' ngăn cản các lớp con ghi đè lại nội dung của phương thức đó.', 'ACTIVE'),
    (@javaId, N'Phát biểu nào sau đây đúng về String trong Java?',
     N'String là kiểu dữ liệu nguyên thủy (primitive)', N'Đối tượng String là bất biến (immutable)', N'String có thể thay đổi kích thước trực tiếp trong bộ nhớ', N'String kế thừa từ List', 'B',
     N'Đối tượng String trong Java là bất biến (Immutable), mỗi thao tác thay đổi chuỗi sẽ sinh ra một đối tượng String mới trong String Pool.', 'ACTIVE'),
    (@javaId, N'Điểm khác biệt chính giữa interface và abstract class từ Java 8 trở đi là gì?',
     N'Interface không thể có method nào có phần thân', N'Một class có thể implement nhiều interface nhưng chỉ có thể kế thừa một abstract class', N'Interface có thể có constructor', N'Abstract class không thể chứa biến', 'B',
     N'Java hỗ trợ đơn kế thừa với class/abstract class, nhưng cho phép một class triển khai (implement) nhiều interface cùng lúc.', 'ACTIVE'),
    (@javaId, N'Từ khóa ''static'' trong Java gắn biến hoặc phương thức với đối tượng nào?',
     N'Gắn với từng instance cụ thể', N'Gắn với chính Class (lớp) chứ không phụ thuộc vào instance', N'Gắn với Thread đang thực thi', N'Gắn với Garbage Collector', 'B',
     N'Thành phần static thuộc về Class, được nạp vào bộ nhớ một lần duy nhất khi nạp Class và có thể gọi trực tiếp qua ClassName.', 'ACTIVE'),
    (@javaId, N'Trong Java, khối mã ''finally'' trong cấu trúc try-catch-finally có đặc điểm gì?',
     N'Chỉ được thực thi nếu có exception xảy ra', N'Chỉ được thực thi nếu không có exception xảy ra', N'Luôn được thực thi kể cả khi có exception hay không', N'Chỉ được thực thi khi có return', 'C',
     N'Khối finally luôn được thực thi (ngoại trừ trường hợp System.exit() được gọi), thường dùng để giải phóng tài nguyên.', 'ACTIVE'),
    (@javaId, N'Giao diện (Functional Interface) nào trong Java 8 nhận vào một tham số và trả về kiểu boolean?',
     N'Consumer<T>', N'Supplier<T>', N'Predicate<T>', N'Function<T, R>', 'C',
     N'Predicate<T> có phương thức boolean test(T t), chuyên dùng để kiểm tra điều kiện lọc dữ liệu.', 'ACTIVE'),
    (@javaId, N'Điểm khác biệt chính giữa HashMap và ConcurrentHashMap là gì?',
     N'HashMap luân phiên khóa toàn bộ bảng', N'ConcurrentHashMap an toàn trong môi trường đa luồng (thread-safe) với hiệu năng cao hơn Hashtable', N'HashMap không cho phép key null', N'ConcurrentHashMap chậm hơn Vector', 'B',
     N'ConcurrentHashMap là thread-safe và sử dụng cơ chế chia nhỏ khóa (bucket-level locking) giúp tối ưu hiệu năng trong môi trường đa luồng.', 'ACTIVE'),
    (@javaId, N'Để so sánh nội dung giá trị giữa 2 đối tượng trong Java, ta nên sử dụng phương thức nào?',
     N'Toán tử ==', N'Phương thức equals()', N'Toán tử ===', N'Phương thức compare()', 'B',
     N'Phương thức equals() dùng để so sánh giá trị logic giữa 2 object, trong khi ''=='' so sánh địa chỉ vùng nhớ tham chiếu.', 'ACTIVE'),
    (@javaId, N'Garbage Collection (GC) trong Java có vai trò gì?',
     N'Tự động giải phóng bộ nhớ heap của các đối tượng không còn được tham chiếu', N'Dọn dẹp các biến local trong stack', N'Tối ưu hóa mã bytecode', N'Kiểm tra lỗi runtime', 'A',
     N'Garbage Collection tự động thu hồi vùng nhớ heap của các đối tượng không còn được trỏ tới bởi bất kỳ tham chiếu nào.', 'ACTIVE'),
    (@javaId, N'Trong Java Stream API, phương thức nào sau đây là Terminal Operation?',
     N'filter()', N'map()', N'collect()', N'distinct()', 'C',
     N'collect() là Terminal Operation kết thúc luồng Stream và chuyển đổi kết quả sang Collection hoặc kiểu dữ liệu mong muốn.', 'ACTIVE');

    -- 2. JavaScript Test
    INSERT INTO [dbo].[skill_tests] 
        ([title], [skill_name], [category], [difficulty], [description], [duration_minutes], [total_questions_per_test], [passing_score], [participant_count], [badge_color], [status])
    VALUES 
        (N'Đề thi đánh giá kỹ năng Lập trình Javascript', N'Javascript', N'Kỹ năng công nghệ', N'Trung bình', 
         N'Đánh giá kiến thức về ES6+, Scope, Closure, Event Loop, Promise/Async Await và DOM manipulation.', 
         15, 10, 60, 1650, N'#eab308', N'ACTIVE');
    DECLARE @jsId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [dbo].[skill_test_questions] ([skill_test_id], [question_text], [option_a], [option_b], [option_c], [option_d], [correct_option], [explanation], [status])
    VALUES
    (@jsId, N'Kết quả của biểu thức: typeof null trong JavaScript là gì?',
     N'''null''', N'''undefined''', N'''object''', N'''boolean''', 'C',
     N'Trong JavaScript, do lỗi thiết kế lịch sử ban đầu, typeof null trả về ''object''.', 'ACTIVE'),
    (@jsId, N'Sự khác biệt chính giữa ''=='' và ''==='' trong JavaScript là gì?',
     N'''=='' so sánh cả kiểu dữ liệu, ''==='' ép kiểu tự động', N'''==='' so sánh nghiêm ngặt cả giá trị và kiểu dữ liệu (không ép kiểu)', N'Không có sự khác biệt', N'''==='' chỉ dùng cho số', 'B',
     N'Toán tử ''==='' (Strict Equality) so sánh cả giá trị và kiểu dữ liệu mà không thực hiện ép kiểu tự động.', 'ACTIVE'),
    (@jsId, N'Từ khóa nào trong ES6 dùng để khai báo biến có phạm vi khối (block-scoped) và không thể gán lại giá trị?',
     N'var', N'let', N'const', N'def', 'C',
     N'const khai báo hằng số có phạm vi block-scope và không cho phép gán lại (re-assign) tham chiếu.', 'ACTIVE'),
    (@jsId, N'Closure trong JavaScript là gì?',
     N'Một hàm tự động đóng tab trình duyệt', N'Một hàm có khả năng ghi nhớ và truy cập các biến trong phạm vi bao ngoài (outer scope) ngay cả khi hàm ngoài đã thực thi xong', N'Một phương thức bất đồng bộ', N'Một đối tượng JSON', 'B',
     N'Closure là sự kết hợp giữa một function và môi trường lexical nơi function đó được khai báo, cho phép truy cập biến của outer scope.', 'ACTIVE'),
    (@jsId, N'Event Loop trong JavaScript có nhiệm vụ gì?',
     N'Thực thi các hàm toán học nhanh hơn', N'Liên tục kiểm tra Call Stack và chuyển các callback từ Task Queue vào Call Stack khi stack trống', N'Tự động refresh trang web', N'Mã hóa dữ liệu cookie', 'B',
     N'Event Loop giám sát Call Stack và Callback Queue; khi Call Stack rỗng, nó đẩy tác vụ đầu tiên từ Queue vào Stack để xử lý.', 'ACTIVE'),
    (@jsId, N'Phương thức Array nào dùng để tạo ra một mảng mới với các phần tử thỏa mãn điều kiện lọc?',
     N'map()', N'filter()', N'reduce()', N'forEach()', 'B',
     N'filter() duyệt qua từng phần tử và trả về mảng mới chỉ chứa các phần tử trả về true từ hàm callback.', 'ACTIVE'),
    (@jsId, N'Kết quả của: console.log(1 + ''2'' + 3) trong JavaScript là gì?',
     N'''6''', N'''123''', N'''33''', N'NaN', 'B',
     N'Toán tử ''+'' khi gặp chuỗi sẽ thực hiện phép nối chuỗi: 1 + ''2'' = ''12'', sau đó ''12'' + 3 = ''123''.', 'ACTIVE'),
    (@jsId, N'Hàm nào dùng để chuyển đổi một chuỗi JSON thành đối tượng JavaScript?',
     N'JSON.stringify()', N'JSON.parse()', N'JSON.object()', N'JSON.convert()', 'B',
     N'JSON.parse() phân tích chuỗi JSON và chuyển đổi thành Object JavaScript tương ứng.', 'ACTIVE'),
    (@jsId, N'Promise trong JavaScript có bao nhiêu trạng thái chính?',
     N'2 trạng thái (Running, Stopped)', N'3 trạng thái (Pending, Fulfilled, Rejected)', N'4 trạng thái', N'1 trạng thái', 'B',
     N'Một Promise có 3 trạng thái: Pending (đang chờ), Fulfilled (hoàn thành thành công) và Rejected (thất bại).', 'ACTIVE'),
    (@jsId, N'Khi gọi hàm qua setTimeout(fn, 0), hàm fn sẽ được thực thi khi nào?',
     N'Ngay lập tức trước dòng lệnh tiếp theo', N'Sau khi Call Stack hiện tại đã hoàn tất việc thực thi', N'Sau đúng 1 giây', N'Không bao giờ thực thi', 'B',
     N'setTimeout đưa callback vào Task Queue, callback chỉ được thực thi sau khi Call Stack chính đã hoàn toàn rỗng.', 'ACTIVE');

    -- 3. Git Test
    INSERT INTO [dbo].[skill_tests] 
        ([title], [skill_name], [category], [difficulty], [description], [duration_minutes], [total_questions_per_test], [passing_score], [participant_count], [badge_color], [status])
    VALUES 
        (N'Đề thi đánh giá kỹ năng Quản lý mã nguồn Git', N'Git', N'Kỹ năng công nghệ', N'Trung bình', 
         N'Kiểm tra khả năng thao tác Git Branching, Merge, Rebase, Conflict Resolution, Git Flow và quản lý commit.', 
         15, 10, 60, 1820, N'#ef4444', N'ACTIVE');
    DECLARE @gitId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [dbo].[skill_test_questions] ([skill_test_id], [question_text], [option_a], [option_b], [option_c], [option_d], [correct_option], [explanation], [status])
    VALUES
    (@gitId, N'Lệnh nào trong Git dùng để tạo và chuyển ngay sang một nhánh mới?',
     N'git branch new-branch', N'git checkout -b new-branch', N'git switch new-branch', N'git merge -b new-branch', 'B',
     N'Lệnh ''git checkout -b <tên_nhánh>'' (hoặc ''git switch -c'') sẽ tạo mới nhánh và checkout trực tiếp sang nhánh đó.', 'ACTIVE'),
    (@gitId, N'Lệnh ''git pull'' thực chất là sự kết hợp của 2 lệnh nào sau đây?',
     N'git fetch và git merge', N'git push và git commit', N'git clone và git checkout', N'git add và git commit', 'A',
     N'''git pull'' tương đương với việc chạy ''git fetch'' để lấy dữ liệu từ remote về và ''git merge'' vào nhánh hiện tại.', 'ACTIVE'),
    (@gitId, N'Lệnh nào giúp lưu tạm thời các thay đổi chưa commit vào vùng lưu trữ tạm để làm việc khác?',
     N'git stash', N'git save', N'git pause', N'git reset', 'A',
     N'''git stash'' tạm thời cất các thay đổi uncommitted vào ngăn xếp stash để bạn có một working directory sạch sẽ.', 'ACTIVE'),
    (@gitId, N'Điểm khác biệt chính giữa ''git merge'' và ''git rebase'' là gì?',
     N'Merge xóa lịch sử commit, Rebase giữ nguyên', N'Merge tạo ra commit gộp (merge commit), còn Rebase di chuyển gốc nhánh tạo lịch sử thẳng hàng', N'Rebase chỉ dùng trên GitHub', N'Không có khác biệt', 'B',
     N'Rebase viết lại lịch sử commit thành một đường thẳng liên tục, trong khi Merge tạo ra một merge commit nối hai nhánh lại.', 'ACTIVE'),
    (@gitId, N'Lệnh nào dùng để hủy bỏ hoàn toàn các thay đổi chưa stage ở Working Directory?',
     N'git restore . hoặc git checkout -- .', N'git commit --amend', N'git clean -f', N'git push --force', 'A',
     N'git restore . sẽ hoàn nguyên các file đã chỉnh sửa về trạng thái của commit gần nhất.', 'ACTIVE'),
    (@gitId, N'Để sửa lại message của commit gần nhất, ta sử dụng lệnh nào?',
     N'git commit --retry', N'git commit --amend', N'git commit --change', N'git commit --fix', 'B',
     N'''git commit --amend'' cho phép bổ sung thay đổi hoặc sửa lại thông điệp của commit gần nhất.', 'ACTIVE'),
    (@gitId, N'File nào trong thư mục dự án dùng để chỉ định các file hoặc folder mà Git không được theo dõi?',
     N'.gitconfig', N'.gitignore', N'.gitmodules', N'.gitkeep', 'B',
     N'File .gitignore khai báo danh sách các pattern file/thư mục cần bỏ qua không đẩy lên repository.', 'ACTIVE'),
    (@gitId, N'Lệnh nào dùng để xem lịch sử commit dưới dạng tóm tắt một dòng cho mỗi commit?',
     N'git log --oneline', N'git show --short', N'git history', N'git status -s', 'A',
     N'Lệnh ''git log --oneline'' hiển thị ngắn gọn mã SHA rút gọn và tiêu đề của từng commit trên một dòng.', 'ACTIVE'),
    (@gitId, N'Khi xảy ra xung đột (Conflict) trong quá trình merge, ta cần làm gì?',
     N'Xóa repository và clone lại', N'Mở các file bị conflict, giải quyết các đoạn mã xung đột, chạy ''git add'' rồi hoàn tất commit', N'Chạy ngay ''git push --force''', N'Khởi động lại máy tính', 'B',
     N'Cần mở các file có cờ conflict, chọn đoạn code chính xác, sau đó ''git add'' các file đã sửa và hoàn tất merge commit.', 'ACTIVE'),
    (@gitId, N'Lệnh nào dùng để sao chép một commit cụ thể từ nhánh khác về nhánh hiện tại?',
     N'git cherry-pick <commit-hash>', N'git copy <commit-hash>', N'git clone <commit-hash>', N'git fetch <commit-hash>', 'A',
     N'''git cherry-pick <commit-hash>'' áp dụng các thay đổi của một commit chỉ định vào nhánh làm việc hiện tại.', 'ACTIVE');

    -- 4. MySQL Test
    INSERT INTO [dbo].[skill_tests] 
        ([title], [skill_name], [category], [difficulty], [description], [duration_minutes], [total_questions_per_test], [passing_score], [participant_count], [badge_color], [status])
    VALUES 
        (N'Đề thi đánh giá kỹ năng Cơ sở dữ liệu MySQL', N'MySQL', N'Kỹ năng công nghệ', N'Trung bình', 
         N'Kiểm tra cú pháp SQL, JOINs, Indexing, Transaction ACID, Subqueries và thiết kế quan hệ Database.', 
         15, 10, 60, 1490, N'#0284c7', N'ACTIVE');
    DECLARE @sqlId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [dbo].[skill_test_questions] ([skill_test_id], [question_text], [option_a], [option_b], [option_c], [option_d], [correct_option], [explanation], [status])
    VALUES
    (@sqlId, N'Mệnh đề nào trong SQL được dùng để lọc dữ liệu dựa trên kết quả của các hàm tổng hợp (Aggregate Functions như COUNT, SUM)?',
     N'WHERE', N'HAVING', N'GROUP BY', N'ORDER BY', 'B',
     N'Mệnh đề HAVING được dùng để lọc các nhóm sau khi GROUP BY và tính toán hàm tổng hợp (WHERE không lọc được aggregate).', 'ACTIVE'),
    (@sqlId, N'Loại JOIN nào trả về tất cả các dòng từ bảng bên trái và các dòng khớp từ bảng bên phải?',
     N'INNER JOIN', N'LEFT JOIN (hoặc LEFT OUTER JOIN)', N'RIGHT JOIN', N'FULL JOIN', 'B',
     N'LEFT JOIN giữ lại toàn bộ bản ghi của bảng bên trái, nếu bảng bên phải không có bản ghi khớp thì điền NULL.', 'ACTIVE'),
    (@sqlId, N'Thuộc tính ACID trong hệ quản trị cơ sở dữ liệu đại diện cho 4 từ khóa nào?',
     N'Atomicity, Consistency, Isolation, Durability', N'Accuracy, Control, Integration, Database', N'Access, Connection, Index, Data', N'Auto, Cache, Insert, Delete', 'A',
     N'ACID đại diện cho Tính nguyên tố (Atomicity), Tính nhất quán (Consistency), Tính cô lập (Isolation) và Tính bền vững (Durability).', 'ACTIVE'),
    (@sqlId, N'Mục đích chính của việc tạo Index (chỉ mục) trên một cột trong MySQL là gì?',
     N'Tăng tốc độ truy vấn tìm kiếm dữ liệu (SELECT)', N'Tăng tốc độ ghi dữ liệu (INSERT, UPDATE)', N'Tiết kiệm dung lượng ổ cứng', N'Tự động mã hóa mật khẩu', 'A',
     N'Index giúp cơ sở dữ liệu tra cứu bản ghi nhanh hơn tương tự mục lục sách, tuy nhiên sẽ làm chậm nhẹ thao tác INSERT/UPDATE.', 'ACTIVE'),
    (@sqlId, N'Câu lệnh nào dùng để xóa toàn bộ dữ liệu trong bảng nhanh nhất và reset lại cột Auto Increment?',
     N'DELETE FROM table_name;', N'TRUNCATE TABLE table_name;', N'DROP TABLE table_name;', N'REMOVE FROM table_name;', 'B',
     N'TRUNCATE TABLE là lệnh DDL giải phóng toàn bộ trang dữ liệu và reset lại ID tự tăng, nhanh hơn DELETE từng dòng.', 'ACTIVE'),
    (@sqlId, N'Khóa chính (Primary Key) trong MySQL có đặc điểm gì?',
     N'Có thể chứa giá trị NULL', N'Duy nhất (UNIQUE) và không bao giờ chứa giá trị NULL', N'Một bảng có thể có nhiều Primary Key riêng biệt', N'Chỉ áp dụng cho kiểu số nguyên', 'B',
     N'Primary Key xác định duy nhất mỗi bản ghi trong bảng và không được phép chứa giá trị NULL.', 'ACTIVE'),
    (@sqlId, N'Hàm nào trong SQL dùng để đếm số lượng dòng không có giá trị NULL trong một cột?',
     N'SUM()', N'COUNT(column_name)', N'TOTAL()', N'AVG()', 'B',
     N'COUNT(column_name) đếm số lượng dòng có giá trị khác NULL của cột đó.', 'ACTIVE'),
    (@sqlId, N'Để giới hạn số lượng bản ghi trả về là 10 dòng bắt đầu từ dòng thứ 21, ta dùng cú pháp nào trong MySQL?',
     N'LIMIT 10 OFFSET 20', N'LIMIT 20, 10', N'Cả A và B đều đúng', N'TOP 10 START 20', 'C',
     N'Trong MySQL, cả cú pháp ''LIMIT 10 OFFSET 20'' và ''LIMIT 20, 10'' đều lấy 10 bản ghi bỏ qua 20 bản ghi đầu.', 'ACTIVE'),
    (@sqlId, N'Khóa ngoại (Foreign Key) được dùng để làm gì trong cơ sở dữ liệu quan hệ?',
     N'Tự động tăng giá trị bản ghi', N'Đảm bảo tính toàn vẹn tham chiếu giữa 2 bảng', N'Mã hóa dữ liệu nhạy cảm', N'Tăng tốc độ kết nối mạng', 'B',
     N'Khóa ngoại liên kết với khóa chính của bảng khác để duy trì tính toàn vẹn dữ liệu tham chiếu giữa các quan hệ.', 'ACTIVE'),
    (@sqlId, N'Lệnh TRANSACTION nào dùng để hủy bỏ tất cả các thao tác dữ liệu vừa thực hiện trong transaction?',
     N'COMMIT', N'ROLLBACK', N'REVOKE', N'ABORT', 'B',
     N'ROLLBACK khôi phục dữ liệu về trạng thái trước khi bắt đầu transaction, hủy bỏ các thay đổi chưa được COMMIT.', 'ACTIVE');

    -- 5. Agile Test
    INSERT INTO [dbo].[skill_tests] 
        ([title], [skill_name], [category], [difficulty], [description], [duration_minutes], [total_questions_per_test], [passing_score], [participant_count], [badge_color], [status])
    VALUES 
        (N'Đề thi đánh giá kỹ năng Phương pháp Agile / Scrum', N'Agile', N'Kỹ năng công nghệ', N'Trung bình', 
         N'Đánh giá hiểu biết về Scrum Framework, Sprint Lifecycle, User Stories, Product Backlog, Retrospective và các vai trò trong Scrum.', 
         15, 10, 60, 2140, N'#6366f1', N'ACTIVE');
    DECLARE @agileId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [dbo].[skill_test_questions] ([skill_test_id], [question_text], [option_a], [option_b], [option_c], [option_d], [correct_option], [explanation], [status])
    VALUES
    (@agileId, N'Trong mô hình Scrum, ai là người chịu trách nhiệm tối ưu hóa giá trị của sản phẩm và quản lý Product Backlog?',
     N'Scrum Master', N'Product Owner (PO)', N'Development Team', N'Project Manager', 'B',
     N'Product Owner chịu trách nhiệm định hướng sản phẩm, quản lý và sắp xếp mức độ ưu tiên của Product Backlog.', 'ACTIVE'),
    (@agileId, N'Cuộc họp Daily Scrum thường diễn ra trong thời gian tối đa bao lâu?',
     N'15 phút', N'30 phút', N'45 phút', N'60 phút', 'A',
     N'Daily Scrum là cuộc họp ngắn gọn diễn ra hàng ngày với timebox tối đa 15 phút để đồng bộ công việc trong team.', 'ACTIVE'),
    (@agileId, N'Ba câu hỏi kinh điển trong buổi họp Daily Scrum là gì?',
     N'Hôm qua đã làm gì? Hôm nay sẽ làm gì? Có gặp khó khăn/trở ngại gì không?', N'Dự án lời bao nhiêu? Khách hàng khen gì? Khi nào được thưởng?', N'Bug này do ai gây ra? Ai chịu phạt? Khi nào fix xong?', N'Hôm nay ăn gì? Mấy giờ về? Đi đâu chơi?', 'A',
     N'3 câu hỏi Daily Scrum: Hôm qua tôi đã làm gì, hôm nay tôi sẽ làm gì, và có trở ngại (impediment) nào cản trở công việc không.', 'ACTIVE'),
    (@agileId, N'Sự kiện nào trong Scrum diễn ra ở cuối Sprint để team cùng nhìn lại quy trình và cải tiến cách làm việc?',
     N'Sprint Planning', N'Sprint Review', N'Sprint Retrospective', N'Backlog Refinement', 'C',
     N'Sprint Retrospective là cơ hội để Scrum Team tự thanh tra và lập kế hoạch cải tiến quy trình làm việc cho các Sprint tiếp theo.', 'ACTIVE'),
    (@agileId, N'Thời lượng lý tưởng của một Sprint trong Scrum thường kéo dài từ bao lâu?',
     N'1 đến 4 tuần', N'6 tháng đến 1 năm', N'1 ngày duy nhất', N'Không giới hạn thời gian', 'A',
     N'Một Sprint thường có độ dài cố định từ 1 đến 4 tuần (phổ biến nhất là 2 tuần) để tạo nhịp độ đều đặn.', 'ACTIVE'),
    (@agileId, N'Định nghĩa Hoàn thành (Definition of Done - DoD) trong Scrum có vai trò gì?',
     N'Danh sách tính năng cần phát triển', N'Tiêu chuẩn chung để xác định một phần việc (Increment) đã đạt chất lượng bàn giao', N'Kế hoạch phân bổ ngân sách', N'Chữ ký nghiệm thu của sếp', 'B',
     N'DoD là sự hiểu biết chung của cả đội về những tiêu chí kỹ thuật/chất lượng cần đạt để một hạng mục được coi là hoàn tất.', 'ACTIVE'),
    (@agileId, N'Ai là người có trách nhiệm loại bỏ các trở ngại (impediments) cản trở tiến độ của Development Team trong Scrum?',
     N'Product Owner', N'Scrum Master', N'Khách hàng', N'Bộ phận HR', 'B',
     N'Scrum Master đóng vai trò Servant Leader, bảo vệ nhóm và hỗ trợ gỡ bỏ các rào cản kỹ thuật/quy trình cản trở nhóm.', 'ACTIVE'),
    (@agileId, N'User Story thường được viết theo cấu trúc tiêu chuẩn nào?',
     N'Là một [Người dùng], tôi muốn [Tính năng], để [Lợi ích mang lại]', N'Làm xong [Công việc] trước ngày [Hạn chót]', N'Hệ thống phải tự động [Thao tác]', N'Nếu [Điều kiện] thì thực hiện [Hành động]', 'A',
     N'Cấu trúc User Story phổ biến: As a [User], I want [Feature], So that [Value/Benefit].', 'ACTIVE'),
    (@agileId, N'Story Point trong Scrum dùng để ước lượng điều gì?',
     N'Số giờ chính xác cần làm', N'Độ phức tạp, khối lượng công việc và rủi ro tương đối của một User Story', N'Mức lương trả cho lập trình viên', N'Số dòng code cần viết', 'B',
     N'Story Point là đơn vị ước lượng tương đối dựa trên độ phức tạp, lượng công việc và sự không chắc chắn của tác vụ.', 'ACTIVE'),
    (@agileId, N'Biểu đồ nào thường dùng trong Scrum để theo dõi khối lượng công việc còn lại so với thời gian trong Sprint?',
     N'Burndown Chart', N'Gantt Chart', N'Pie Chart', N'Flowchart', 'A',
     N'Burndown Chart trực quan hóa lượng công việc còn lại giảm dần theo từng ngày của Sprint hướng tới mục tiêu 0.', 'ACTIVE');

    -- 6. HTML/CSS Test
    INSERT INTO [dbo].[skill_tests] 
        ([title], [skill_name], [category], [difficulty], [description], [duration_minutes], [total_questions_per_test], [passing_score], [participant_count], [badge_color], [status])
    VALUES 
        (N'Đề thi đánh giá kỹ năng HTML5 & CSS3 Cơ bản', N'HTML', N'Kỹ năng công nghệ', N'Trung bình', 
         N'Đánh giá kiến thức về Semantic HTML, Box Model, Flexbox, Grid, CSS Selectors và Responsive Web Design.', 
         15, 10, 60, 3510, N'#8b5cf6', N'ACTIVE');
    DECLARE @htmlId BIGINT = SCOPE_IDENTITY();

    INSERT INTO [dbo].[skill_test_questions] ([skill_test_id], [question_text], [option_a], [option_b], [option_c], [option_d], [correct_option], [explanation], [status])
    VALUES
    (@htmlId, N'Thẻ HTML5 nào mang ý nghĩa ngữ nghĩa (Semantic HTML) dùng để chứa phần điều hướng trang web?',
     N'<div>', N'<nav>', N'<section>', N'<aside>', 'B',
     N'<nav> là thẻ ngữ nghĩa đại diện cho phần chứa các liên kết điều hướng chính của trang web.', 'ACTIVE'),
    (@htmlId, N'CSS Box Model bao gồm 4 thành phần nào theo thứ tự từ trong ra ngoài?',
     N'Content -> Padding -> Border -> Margin', N'Content -> Margin -> Border -> Padding', N'Border -> Margin -> Padding -> Content', N'Padding -> Border -> Margin -> Content', 'A',
     N'CSS Box Model gồm: Nội dung (Content), Khoảng đệm (Padding), Đường viền (Border) và Khoảng cách ngoài (Margin).', 'ACTIVE'),
    (@htmlId, N'Thuộc tính nào trong CSS Flexbox dùng để căn chỉnh các phần tử con theo trục chính (Main Axis)?',
     N'align-items', N'justify-content', N'flex-direction', N'align-content', 'B',
     N'justify-content dùng để căn chỉnh và phân bổ khoảng trống cho các flex items dọc theo trục chính (Main Axis).', 'ACTIVE'),
    (@htmlId, N'Giá trị nào của thuộc tính ''position'' trong CSS khiến phần tử cố định theo khung nhìn trình duyệt ngay cả khi cuộn trang?',
     N'relative', N'absolute', N'fixed', N'static', 'C',
     N'''position: fixed'' ghim phần tử cố định so với viewport của trình duyệt, không di chuyển khi cuộn trang.', 'ACTIVE'),
    (@htmlId, N'Đơn vị ''rem'' trong CSS có kích thước phụ thuộc vào giá trị nào?',
     N'Font-size của phần tử cha trực tiếp', N'Font-size của phần tử gốc (thẻ <html>)', N'Chiều rộng màn hình', N'Chiều cao của trình duyệt', 'B',
     N'1rem (root em) bằng với kích thước font-size của phần tử root (mặc định thẻ <html> là 16px).', 'ACTIVE'),
    (@htmlId, N'Thuộc tính nào dùng để đảm bảo kích thước chiều rộng của phần tử bao gồm cả Padding và Border?',
     N'box-sizing: border-box;', N'box-sizing: content-box;', N'display: block;', N'overflow: hidden;', 'A',
     N'''box-sizing: border-box'' tính padding và border vào tổng width/height của element, giúp kiểm soát layout dễ dàng.', 'ACTIVE'),
    (@htmlId, N'Để tạo một bố cục lưới 3 cột có kích thước bằng nhau bằng CSS Grid, ta dùng cú pháp nào?',
     N'grid-template-columns: repeat(3, 1fr);', N'grid-columns: 3;', N'grid-template-rows: 3fr;', N'display: 3-grid;', 'A',
     N'''grid-template-columns: repeat(3, 1fr);'' tạo ra 3 cột có tỉ lệ độ rộng bằng nhau phân chia đều diện tích lưới.', 'ACTIVE'),
    (@htmlId, N'Thẻ meta nào bắt buộc phải có trong thẻ <head> để website hiển thị responsive tốt trên thiết bị di động?',
     N'<meta name=''viewport'' content=''width=device-width, initial-scale=1.0''>', N'<meta name=''mobile'' content=''true''>', N'<meta charset=''UTF-8''>', N'<meta http-equiv=''X-UA-Compatible''>', 'A',
     N'Thẻ viewport giúp trình duyệt di động đặt chiều rộng viewport bằng chiều rộng vật lý của thiết bị và không zoom mặc định.', 'ACTIVE'),
    (@htmlId, N'Selector nào trong CSS có độ ưu tiên (Specificity) cao nhất?',
     N'Tag selector (ví dụ: div)', N'Class selector (ví dụ: .btn)', N'ID selector (ví dụ: #header)', N'Universal selector (ví dụ: *)', 'C',
     N'ID selector (#id) có trọng số Specificity cao hơn Class selector (.class) và Tag selector.', 'ACTIVE'),
    (@htmlId, N'Thuộc tính ''alt'' trong thẻ <img> có tác dụng gì quan trọng nhất?',
     N'Tạo hiệu ứng phóng to ảnh khi hover', N'Cung cấp văn bản thay thế khi ảnh lỗi và hỗ trợ SEO / Trình đọc màn hình (Screen Readers)', N'Đổi màu nền của ảnh', N'Tăng độ phân giải cho bức ảnh', 'B',
     N'Thuộc tính alt cung cấp mô tả văn bản thay thế khi hình ảnh không tải được và hỗ trợ tiếp cận (Accessibility) cũng như SEO.', 'ACTIVE');
END
GO
