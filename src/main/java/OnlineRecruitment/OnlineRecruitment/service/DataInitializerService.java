package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.InterviewQuestion;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTest;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestQuestion;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestRatingThreshold;
import OnlineRecruitment.OnlineRecruitment.repository.InterviewQuestionRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestQuestionRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestRatingThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializerService implements CommandLineRunner {

    private final DataSource dataSource;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final SkillTestRepository skillTestRepository;
    private final SkillTestQuestionRepository skillTestQuestionRepository;
    private final SkillTestRatingThresholdRepository ratingThresholdRepository;

    @Override
    public void run(String... args) {
        migrateSkillTestsTable();
        try {
            seedInterviewQuestions();
        } catch (Exception e) {
            log.warn("Auto-seeding interview questions encountered notice: {}", e.getMessage());
        }
        try {
            seedSkillTests();
        } catch (Exception e) {
            log.warn("Auto-seeding skill tests encountered notice: {}", e.getMessage());
        }
        try {
            seedRatingThresholds();
        } catch (Exception e) {
            log.warn("Auto-seeding rating thresholds encountered notice: {}", e.getMessage());
        }
    }

    private void migrateSkillTestsTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Add updated_at column if missing (schema compatibility fix)
            stmt.execute(
                "IF NOT EXISTS (SELECT 1 FROM sys.columns " +
                "WHERE object_id = OBJECT_ID('dbo.skill_tests') AND name = 'updated_at') " +
                "ALTER TABLE dbo.skill_tests ADD updated_at DATETIME2 NULL");

            // Auto-create skill_test_rating_thresholds table if not exists
            stmt.execute(
                "IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[skill_test_rating_thresholds]') AND type in (N'U')) " +
                "BEGIN " +
                "    CREATE TABLE [dbo].[skill_test_rating_thresholds] ( " +
                "        [id] BIGINT IDENTITY(1,1) PRIMARY KEY, " +
                "        [min_score] INT NOT NULL, " +
                "        [label] NVARCHAR(100) NOT NULL, " +
                "        [display_order] INT NOT NULL DEFAULT 0, " +
                "        [created_at] DATETIME2 NOT NULL DEFAULT SYSDATETIME(), " +
                "        [updated_at] DATETIME2 NOT NULL DEFAULT SYSDATETIME() " +
                "    ); " +
                "    INSERT INTO [dbo].[skill_test_rating_thresholds] ([min_score], [label], [display_order]) VALUES " +
                "    (90, N'Xuất sắc', 1), " +
                "    (80, N'Giỏi', 2), " +
                "    (65, N'Khá', 3), " +
                "    (50, N'Đạt', 4), " +
                "    (0, N'Chưa đạt', 5); " +
                "END");

            log.info("skill_tests and skill_test_rating_thresholds schema migration checked OK.");
        } catch (Exception e) {
            log.warn("skill_tests schema migration warning: {}", e.getMessage());
        }
    }

    private void seedInterviewQuestions() {
        if (interviewQuestionRepository.count() > 0) return;

        List<InterviewQuestion> list = List.of(
            createInterviewQuestion(
                "Hãy giới thiệu ngắn gọn về bản thân bạn?",
                "Phỏng vấn chung",
                "Mọi cấp bậc",
                "Nhà tuyển dụng muốn đánh giá Kháả năng giao tiếp, sự tự tin và mức độ phù hợp của bạn với văn hóa và vị trí ứng tuyển.",
                "Áp dụng cấu trúc Hiện tại - Quá Kháứ - Tương lai: Tóm tắt vị trí hiện tại, kinh nghiệm nổi bật liên quan trực tiếp và lý do muốn gia nhập công ty.",
                "Em là Nguyễn Văn A, tốt nghiệp chuyên ngành CNTT. Hiện em có 2 năm kinh nghiệm phát triển backend với Java/Spring Boot. Ở dự án gần nhất, em phụ trách tối ưu hóa API và giảm 30% thời gian phản hồi hệ thống. Em tìm kiếm cơ hội tại quý công ty vì ấn tượng với môi trường chuyên nghiệp và quy mô sản phẩm.",
                "Tránh kể lại toàn bộ sơ yếu lý lịch từ thời phổ thông hoặc nói lan man về sở thích cá nhân Kháông liên quan đến công việc.",
                1
            ),
            createInterviewQuestion(
                "Điểm mạnh lớn nhất của bạn là gì?",
                "Phỏng vấn chung",
                "Mọi cấp bậc",
                "Đánh giá bạn có tự nhận thức được giá trị bản thân và kỹ năng đó có giải quyết được bài toán của công ty hay Kháông.",
                "Chọn 1-2 điểm mạnh liên quan mật thiết nhất đến mô tả công việc (JD), kèm theo ví dụ thực tế chứng minh (kết quả có số liệu nếu có).",
                "Điểm mạnh lớn nhất của em là Kháả năng tự học công nghệ mới và tư duy giải quyết vấn đề. Khái dự án trước yêu cầu tích hợp giải pháp thanh toán chưa từng làm, em đã chủ động nghiên cứu tài liệu kỹ thuật và hoàn thành module đúng hạn trong 1 tuần.",
                "Tránh nói những câu chung chung sáo rỗng như 'Em rất chăm chỉ, cầu tiến' mà Kháông đưa ra được minh chứng thực tế.",
                2
            ),
            createInterviewQuestion(
                "Điểm yếu lớn nhất của bạn là gì và bạn Kháắc phục nó như thế nào?",
                "Phỏng vấn chung",
                "Mọi cấp bậc",
                "Kiểm tra tính trung thực, Kháả năng tự nhận thức và nỗ lực hoàn thiện bản thân của ứng viên.",
                "Chọn một điểm yếu thực sự nhưng Kháông gây chí mạng cho công việc, tập trung 70% thời gian nói về các hành động cụ thể bạn đang thực hiện để cải thiện.",
                "Trước đây em thường ôm đồm nhiều việc vì sợ làm phiền đồng nghiệp. Sau đó em nhận ra điều này ảnh hưởng tới tiến độ chung, nên em đã học cách sử dụng công cụ quản lý Trello và chủ động trao đổi chia sẻ đầu việc trong các buổi Daily Scrum.",
                "Tuyệt đối Kháông trả lời 'Em Kháông có điểm yếu nào' hoặc trá hình 'Điểm yếu của em là quá cầu toàn, ham mê công việc'.",
                3
            ),
            createInterviewQuestion(
                "Hãy kể về một lần bạn gặp thất bại hoặc bất đồng ý kiến trong công việc và cách bạn vượt qua?",
                "Tình huống & Hành vi",
                "Mọi cấp bậc",
                "Đánh giá trí tuệ cảm xúc (EQ), kỹ năng làm việc nhóm, kỹ năng giải quyết mâu thuẫn và thái độ Khái đối mặt Kháó Kháăn.",
                "Sử dụng mô hình STAR (Situation - Tình huống, Task - Nhiệm vụ, Action - Hành động, Result - Kết quả & Bài học rút ra).",
                "Trong dự án xây dựng cổng thanh toán, em và bạn Tech Lead có bất đồng về việc lựa chọn kiến trúc Microservices hay Monolith. Thay vì tranh cãi cá nhân, em đã lập bảng so sánh chi phí server, thời gian phát triển và Kháả năng mở rộng. Cuối cùng cả đội thống nhất giải pháp phù hợp nhất và dự án ra mắt đúng hạn.",
                "Tránh đổ lỗi cho đồng nghiệp, sếp cũ hoặc tỏ thái độ tiêu cực, bực bội Khái nhắc lại sự việc.",
                4
            ),
            createInterviewQuestion(
                "Mục tiêu nghề nghiệp trong 3 - 5 năm tới của bạn là gì?",
                "Định hướng nghề nghiệp",
                "Mọi cấp bậc",
                "Nhà tuyển dụng muốn biết bạn có kế hoạch gắn bó lâu dài hay chỉ coi đây là trạm dừng chân ngắn hạn.",
                "Chia mục tiêu thành 2 phần: Trong 1-2 năm đầu thành thạo quy trình và tạo giá trị cho vị trí này; 3-5 năm tới nâng cao năng lực chuyên sâu hoặc hướng tới vai trò quản lý / chuyên gia.",
                "Trong 1-2 năm tới, em muốn làm chủ hoàn toàn hệ thống của công ty và nâng cao kỹ năng tối ưu kiến trúc. Trong 3-5 năm, em hướng tới vai trò Senior Developer hoặc Team Leader, có thể dẫn dắt các bạn mới và đóng góp vào định hướng kỹ thuật.",
                "Tránh trả lời mơ hồ kiểu 'Em muốn kiếm thật nhiều tiền' hoặc lộ rõ kế hoạch tách ra mở công ty riêng trong tương lai gần.",
                5
            ),
            createInterviewQuestion(
                "Tại sao bạn lại nghỉ việc ở công ty cũ?",
                "Phỏng vấn chung",
                "Junior / Middle",
                "Đánh giá thái độ của bạn với công ty cũ và động lực thực sự Khái tìm kiếm công việc mới.",
                "Tập trung vào Kháía cạnh tích cực: mong muốn tìm kiếm thử thách mới, mở rộng phạm vi chuyên môn hoặc môi trường phù hợp với định hướng phát triển dài hạn.",
                "Em rất biết ơn thời gian làm việc tại công ty cũ vì đã giúp em xây dựng nền tảng vững chắc. Tuy nhiên, em nhận thấy mô hình công ty hiện tại đã bão hòa về cơ hội phát triển chuyên môn, nên em muốn tìm kiếm môi trường mới có quy mô sản phẩm lớn hơn để tiếp tục nâng cao kỹ năng.",
                "Kháông bao giờ nói xấu sếp cũ, đồng nghiệp cũ hoặc than phiền về chế độ đãi ngộ.",
                6
            ),
            createInterviewQuestion(
                "Mức lương kỳ vọng của bạn là bao nhiêu?",
                "Đàm phán lương",
                "Mọi cấp bậc",
                "Kiểm tra xem mức lương bạn kỳ vọng có nằm trong ngân sách của công ty và bạn định giá bản thân ra sao.",
                "Tìm hiểu trước dải lương thị trường cho vị trí tương đương. Đưa ra một Kháoảng lương (range) linh hoạt thay vì một con số cố định cứng nhắc.",
                "Qua tìm hiểu về yêu cầu công việc và mặt bằng thị trường đối với vị trí Backend Java 2 năm kinh nghiệm, em kỳ vọng mức lương trong Kháoảng từ 18 - 22 triệu đồng. Tuy nhiên em hoàn toàn cởi mở để thảo luận thêm dựa trên tổng gói đãi ngộ và lộ trình đánh giá của công ty.",
                "Tránh đưa ra con số quá cao phi thực tế hoặc quá thấp tự hạ thấp giá trị bản thân, hoặc nói 'Công ty trả bao nhiêu cũng được'.",
                7
            ),
            createInterviewQuestion(
                "Bạn có câu hỏi nào dành cho chúng tôi Kháông?",
                "Phỏng vấn chung",
                "Mọi cấp bậc",
                "Đánh giá mức độ quan tâm, sự chuẩn bị chu đáo và tư duy chủ động của ứng viên đối với công ty.",
                "Chuẩn bị trước 2-3 câu hỏi chất lượng về văn hóa nhóm, định hướng sản phẩm hoặc tiêu chí đánh giá thành công của vị trí.",
                "Em muốn hỏi thêm về: Trong 3 tháng thử việc đầu tiên, tiêu chí quan trọng nhất để đánh giá một nhân viên thành công ở vị trí này là gì? Và đội ngũ phát triển hiện đang áp dụng quy trình kiểm thử và deploy ra sao?",
                "Tránh trả lời 'Em Kháông có câu hỏi nào' hoặc chỉ chăm chăm hỏi về giờ giải lao, ngày nghỉ phép ngay từ vòng đầu.",
                8
            ),
            createInterviewQuestion(
                "Bạn sử dụng AI (ChatGPT, Copilot,...) trong công việc như thế nào?",
                "Kỹ năng công nghệ",
                "Mọi cấp bậc",
                "Xem xét bạn có cập nhật xu hướng công nghệ để tối ưu năng suất làm việc hay bị phụ thuộc hoàn toàn vào công cụ.",
                "Nhấn mạnh AI là công cụ hỗ trợ tăng tốc (viết test case, gợi ý cú pháp, review tài liệu), nhưng bạn luôn kiểm tra lại và hiểu sâu bản chất logic.",
                "Em thường xuyên sử dụng AI để hỗ trợ tra cứu cú pháp, viết các đoạn unit test mẫu hoặc tối ưu câu lệnh SQL. Tuy nhiên, em luôn tự review cẩn thận để đảm bảo bảo mật mã nguồn công ty và Kháông sao chép mù quáng những đoạn mã chưa nắm rõ nguyên lý.",
                "Tránh trả lời bài xích công nghệ hoặc thừa nhận phó mặc toàn bộ code cho AI viết.",
                9
            )
        );

        interviewQuestionRepository.saveAll(list);
        log.info("Seeded {} interview questions successfully.", list.size());
    }

    private InterviewQuestion createInterviewQuestion(String question, String category, String level,
                                                      String purpose, String tips, String sample, String pitfalls, int order) {
        InterviewQuestion q = new InterviewQuestion();
        q.setQuestion(question);
        q.setCategory(category);
        q.setTargetLevel(level);
        q.setPurpose(purpose);
        q.setTips(tips);
        q.setSampleAnswer(sample);
        q.setPitfalls(pitfalls);
        q.setDisplayOrder(order);
        q.setViewCount(100 + order * 35);
        q.setStatus("ACTIVE");
        return q;
    }

    /**
     * Dữ liệu đánh giá năng lực ứng viên (đề thi, ngân hàng câu hỏi)
     * được lưu trữ và quản lý trực tiếp trên DATABASE thông qua file script SQL,
     * KHÔNG viết cứng trong code Java backend.
     * Nếu cơ sở dữ liệu chưa có dữ liệu, phương thức này sẽ tự động nạp từ
     * file script schema_skilltest_and_interview.sql vào Database.
     */
    private void seedSkillTests() {
        if (skillTestRepository.count() > 0) {
            log.info("Skill tests already exist in database (total: {}). No initialization needed.", skillTestRepository.count());
            return;
        }

        log.info("Skill tests table is empty. Loading skill test data into database from schema_skilltest_and_interview.sql...");
        try (Connection conn = dataSource.getConnection();
             InputStream is = getClass().getClassLoader().getResourceAsStream("schema_skilltest_and_interview.sql")) {

            if (is == null) {
                log.warn("SQL script schema_skilltest_and_interview.sql not found in resources!");
                return;
            }

            String fullSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // Tách các lệnh SQL theo batch separator GO của SQL Server
            String[] batches = fullSql.split("(?im)^\\s*GO\\s*$");
            try (Statement stmt = conn.createStatement()) {
                for (String batch : batches) {
                    String clean = batch.trim();
                    // Bỏ qua lệnh USE database để an toàn với cấu hình DataSource hiện tại
                    if (clean.toUpperCase().startsWith("USE ")) {
                        continue;
                    }
                    if (!clean.isEmpty()) {
                        try {
                            stmt.execute(clean);
                        } catch (Exception ex) {
                            log.debug("Batch execution notice: {}", ex.getMessage());
                        }
                    }
                }
            }
            log.info("Loaded skill test data from database script successfully. Current count: {}", skillTestRepository.count());
        } catch (Exception e) {
            log.warn("Notice while loading skill tests from database script: {}", e.getMessage());
        }
    }

    private void seedRatingThresholds() {
        if (ratingThresholdRepository.count() > 0) return;

        List<SkillTestRatingThreshold> thresholds = List.of(
            createThreshold(90, "Xuất sắc", 1),
            createThreshold(80, "Giỏi",      2),
            createThreshold(65, "Khá",       3),
            createThreshold(50, "Đạt",       4),
            createThreshold(0,  "Chưa đạt",  5)
        );

        ratingThresholdRepository.saveAll(thresholds);
        log.info("Seeded {} skill test rating thresholds successfully.", thresholds.size());
    }

    private SkillTestRatingThreshold createThreshold(int minScore, String label, int displayOrder) {
        SkillTestRatingThreshold t = new SkillTestRatingThreshold();
        t.setMinScore(minScore);
        t.setLabel(label);
        t.setDisplayOrder(displayOrder);
        return t;
    }
}
