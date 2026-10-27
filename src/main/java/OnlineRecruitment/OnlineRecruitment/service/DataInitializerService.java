package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.InterviewQuestion;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTest;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestQuestion;
import OnlineRecruitment.OnlineRecruitment.repository.InterviewQuestionRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestQuestionRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializerService implements CommandLineRunner {

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final SkillTestRepository skillTestRepository;
    private final SkillTestQuestionRepository skillTestQuestionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedInterviewQuestions();
            seedSkillTests();
        } catch (Exception e) {
            log.warn("Auto-seeding skill tests / interview questions encountered notice: {}", e.getMessage());
        }
    }

    private void seedInterviewQuestions() {
        if (interviewQuestionRepository.count() > 0) return;

        List<InterviewQuestion> list = List.of(
            createInterviewQuestion(
                "Hãy giới thiệu ngắn gọn về bản thân bạn?",
                "Phỏng vấn chung",
                "Mọi cấp bậc",
                "Nhà tuyển dụng muốn đánh giá khả năng giao tiếp, sự tự tin và mức độ phù hợp của bạn với văn hóa và vị trí ứng tuyển.",
                "Áp dụng cấu trúc Hiện tại - Quá khứ - Tương lai: Tóm tắt vị trí hiện tại, kinh nghiệm nổi bật liên quan trực tiếp và lý do muốn gia nhập công ty.",
                "Em là Nguyễn Văn A, tốt nghiệp chuyên ngành CNTT. Hiện em có 2 năm kinh nghiệm phát triển backend với Java/Spring Boot. Ở dự án gần nhất, em phụ trách tối ưu hóa API và giảm 30% thời gian phản hồi hệ thống. Em tìm kiếm cơ hội tại quý công ty vì ấn tượng với môi trường chuyên nghiệp và quy mô sản phẩm.",
                "Tránh kể lại toàn bộ sơ yếu lý lịch từ thời phổ thông hoặc nói lan man về sở thích cá nhân không liên quan đến công việc.",
                1
            ),
            createInterviewQuestion(
                "Điểm mạnh lớn nhất của bạn là gì?",
                "Phỏng vấn chung",
                "Mọi cấp bậc",
                "Đánh giá bạn có tự nhận thức được giá trị bản thân và kỹ năng đó có giải quyết được bài toán của công ty hay không.",
                "Chọn 1-2 điểm mạnh liên quan mật thiết nhất đến mô tả công việc (JD), kèm theo ví dụ thực tế chứng minh (kết quả có số liệu nếu có).",
                "Điểm mạnh lớn nhất của em là khả năng tự học công nghệ mới và tư duy giải quyết vấn đề. Khi dự án trước yêu cầu tích hợp giải pháp thanh toán chưa từng làm, em đã chủ động nghiên cứu tài liệu kỹ thuật và hoàn thành module đúng hạn trong 1 tuần.",
                "Tránh nói những câu chung chung sáo rỗng như 'Em rất chăm chỉ, cầu tiến' mà không đưa ra được minh chứng thực tế.",
                2
            ),
            createInterviewQuestion(
                "Điểm yếu lớn nhất của bạn là gì và bạn khắc phục nó như thế nào?",
                "Phỏng vấn chung",
                "Mọi cấp bậc",
                "Kiểm tra tính trung thực, khả năng tự nhận thức và nỗ lực hoàn thiện bản thân của ứng viên.",
                "Chọn một điểm yếu thực sự nhưng không gây chí mạng cho công việc, tập trung 70% thời gian nói về các hành động cụ thể bạn đang thực hiện để cải thiện.",
                "Trước đây em thường ôm đồm nhiều việc vì sợ làm phiền đồng nghiệp. Sau đó em nhận ra điều này ảnh hưởng tới tiến độ chung, nên em đã học cách sử dụng công cụ quản lý Trello và chủ động trao đổi chia sẻ đầu việc trong các buổi Daily Scrum.",
                "Tuyệt đối không trả lời 'Em không có điểm yếu nào' hoặc trá hình 'Điểm yếu của em là quá cầu toàn, ham mê công việc'.",
                3
            ),
            createInterviewQuestion(
                "Hãy kể về một lần bạn gặp thất bại hoặc bất đồng ý kiến trong công việc và cách bạn vượt qua?",
                "Tình huống & Hành vi",
                "Mọi cấp bậc",
                "Đánh giá trí tuệ cảm xúc (EQ), kỹ năng làm việc nhóm, kỹ năng giải quyết mâu thuẫn và thái độ khi đối mặt khó khăn.",
                "Sử dụng mô hình STAR (Situation - Tình huống, Task - Nhiệm vụ, Action - Hành động, Result - Kết quả & Bài học rút ra).",
                "Trong dự án xây dựng cổng thanh toán, em và bạn Tech Lead có bất đồng về việc lựa chọn kiến trúc Microservices hay Monolith. Thay vì tranh cãi cá nhân, em đã lập bảng so sánh chi phí server, thời gian phát triển và khả năng mở rộng. Cuối cùng cả đội thống nhất giải pháp phù hợp nhất và dự án ra mắt đúng hạn.",
                "Tránh đổ lỗi cho đồng nghiệp, sếp cũ hoặc tỏ thái độ tiêu cực, bực bội khi nhắc lại sự việc.",
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
                "Đánh giá thái độ của bạn với công ty cũ và động lực thực sự khi tìm kiếm công việc mới.",
                "Tập trung vào khía cạnh tích cực: mong muốn tìm kiếm thử thách mới, mở rộng phạm vi chuyên môn hoặc môi trường phù hợp với định hướng phát triển dài hạn.",
                "Em rất biết ơn thời gian làm việc tại công ty cũ vì đã giúp em xây dựng nền tảng vững chắc. Tuy nhiên, em nhận thấy mô hình công ty hiện tại đã bão hòa về cơ hội phát triển chuyên môn, nên em muốn tìm kiếm môi trường mới có quy mô sản phẩm lớn hơn để tiếp tục nâng cao kỹ năng.",
                "Không bao giờ nói xấu sếp cũ, đồng nghiệp cũ hoặc than phiền về chế độ đãi ngộ.",
                6
            ),
            createInterviewQuestion(
                "Mức lương kỳ vọng của bạn là bao nhiêu?",
                "Đàm phán lương",
                "Mọi cấp bậc",
                "Kiểm tra xem mức lương bạn kỳ vọng có nằm trong ngân sách của công ty và bạn định giá bản thân ra sao.",
                "Tìm hiểu trước dải lương thị trường cho vị trí tương đương. Đưa ra một khoảng lương (range) linh hoạt thay vì một con số cố định cứng nhắc.",
                "Qua tìm hiểu về yêu cầu công việc và mặt bằng thị trường đối với vị trí Backend Java 2 năm kinh nghiệm, em kỳ vọng mức lương trong khoảng từ 18 - 22 triệu đồng. Tuy nhiên em hoàn toàn cởi mở để thảo luận thêm dựa trên tổng gói đãi ngộ và lộ trình đánh giá của công ty.",
                "Tránh đưa ra con số quá cao phi thực tế hoặc quá thấp tự hạ thấp giá trị bản thân, hoặc nói 'Công ty trả bao nhiêu cũng được'.",
                7
            ),
            createInterviewQuestion(
                "Bạn có câu hỏi nào dành cho chúng tôi không?",
                "Phỏng vấn chung",
                "Mọi cấp bậc",
                "Đánh giá mức độ quan tâm, sự chuẩn bị chu đáo và tư duy chủ động của ứng viên đối với công ty.",
                "Chuẩn bị trước 2-3 câu hỏi chất lượng về văn hóa nhóm, định hướng sản phẩm hoặc tiêu chí đánh giá thành công của vị trí.",
                "Em muốn hỏi thêm về: Trong 3 tháng thử việc đầu tiên, tiêu chí quan trọng nhất để đánh giá một nhân viên thành công ở vị trí này là gì? Và đội ngũ phát triển hiện đang áp dụng quy trình kiểm thử và deploy ra sao?",
                "Tránh trả lời 'Em không có câu hỏi nào' hoặc chỉ chăm chăm hỏi về giờ giải lao, ngày nghỉ phép ngay từ vòng đầu.",
                8
            ),
            createInterviewQuestion(
                "Bạn sử dụng AI (ChatGPT, Copilot,...) trong công việc như thế nào?",
                "Kỹ năng công nghệ",
                "Mọi cấp bậc",
                "Xem xét bạn có cập nhật xu hướng công nghệ để tối ưu năng suất làm việc hay bị phụ thuộc hoàn toàn vào công cụ.",
                "Nhấn mạnh AI là công cụ hỗ trợ tăng tốc (viết test case, gợi ý cú pháp, review tài liệu), nhưng bạn luôn kiểm tra lại và hiểu sâu bản chất logic.",
                "Em thường xuyên sử dụng AI để hỗ trợ tra cứu cú pháp, viết các đoạn unit test mẫu hoặc tối ưu câu lệnh SQL. Tuy nhiên, em luôn tự review cẩn thận để đảm bảo bảo mật mã nguồn công ty và không sao chép mù quáng những đoạn mã chưa nắm rõ nguyên lý.",
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

    private void seedSkillTests() {
        if (skillTestRepository.count() > 0) return;

        // 1. JAVA TEST
        SkillTest javaTest = new SkillTest();
        javaTest.setTitle("Đề thi đánh giá kỹ năng Lập trình Java");
        javaTest.setSkillName("Java");
        javaTest.setCategory("Kỹ năng công nghệ");
        javaTest.setDifficulty("Trung bình");
        javaTest.setDescription("Kiểm tra kiến thức cốt lõi về Java Core, OOP, Collections, Multi-threading, Exception handling và Java 8 Features.");
        javaTest.setDurationMinutes(15);
        javaTest.setTotalQuestionsPerTest(10);
        javaTest.setPassingScore(60);
        javaTest.setParticipantCount(1420);
        javaTest.setBadgeColor("#f97316");
        javaTest.setStatus("ACTIVE");
        javaTest = skillTestRepository.save(javaTest);

        seedJavaQuestions(javaTest);

        // 2. JAVASCRIPT TEST
        SkillTest jsTest = new SkillTest();
        jsTest.setTitle("Đề thi đánh giá kỹ năng Lập trình Javascript");
        jsTest.setSkillName("Javascript");
        jsTest.setCategory("Kỹ năng công nghệ");
        jsTest.setDifficulty("Trung bình");
        jsTest.setDescription("Đánh giá kiến thức về ES6+, Scope, Closure, Event Loop, Promise/Async Await và DOM manipulation.");
        jsTest.setDurationMinutes(15);
        jsTest.setTotalQuestionsPerTest(10);
        jsTest.setPassingScore(60);
        jsTest.setParticipantCount(1650);
        jsTest.setBadgeColor("#eab308");
        jsTest.setStatus("ACTIVE");
        jsTest = skillTestRepository.save(jsTest);

        seedJavascriptQuestions(jsTest);

        // 3. GIT TEST
        SkillTest gitTest = new SkillTest();
        gitTest.setTitle("Đề thi đánh giá kỹ năng Quản lý mã nguồn Git");
        gitTest.setSkillName("Git");
        gitTest.setCategory("Kỹ năng công nghệ");
        gitTest.setDifficulty("Trung bình");
        gitTest.setDescription("Kiểm tra khả năng thao tác Git Branching, Merge, Rebase, Conflict Resolution, Git Flow và quản lý commit.");
        gitTest.setDurationMinutes(15);
        gitTest.setTotalQuestionsPerTest(10);
        gitTest.setPassingScore(60);
        gitTest.setParticipantCount(1820);
        gitTest.setBadgeColor("#ef4444");
        gitTest.setStatus("ACTIVE");
        gitTest = skillTestRepository.save(gitTest);

        seedGitQuestions(gitTest);

        // 4. MYSQL TEST
        SkillTest sqlTest = new SkillTest();
        sqlTest.setTitle("Đề thi đánh giá kỹ năng Cơ sở dữ liệu MySQL");
        sqlTest.setSkillName("MySQL");
        sqlTest.setCategory("Kỹ năng công nghệ");
        sqlTest.setDifficulty("Trung bình");
        sqlTest.setDescription("Kiểm tra cú pháp SQL, JOINs, Indexing, Transaction ACID, Subqueries và thiết kế quan hệ Database.");
        sqlTest.setDurationMinutes(15);
        sqlTest.setTotalQuestionsPerTest(10);
        sqlTest.setPassingScore(60);
        sqlTest.setParticipantCount(1490);
        sqlTest.setBadgeColor("#0284c7");
        sqlTest.setStatus("ACTIVE");
        sqlTest = skillTestRepository.save(sqlTest);

        seedMySQLQuestions(sqlTest);

        // 5. AGILE TEST
        SkillTest agileTest = new SkillTest();
        agileTest.setTitle("Đề thi đánh giá kỹ năng Phương pháp Agile / Scrum");
        agileTest.setSkillName("Agile");
        agileTest.setCategory("Kỹ năng công nghệ");
        agileTest.setDifficulty("Trung bình");
        agileTest.setDescription("Đánh giá hiểu biết về Scrum Framework, Sprint Lifecycle, User Stories, Product Backlog, Retrospective và các vai trò trong Scrum.");
        agileTest.setDurationMinutes(15);
        agileTest.setTotalQuestionsPerTest(10);
        agileTest.setPassingScore(60);
        agileTest.setParticipantCount(2140);
        agileTest.setBadgeColor("#6366f1");
        agileTest.setStatus("ACTIVE");
        agileTest = skillTestRepository.save(agileTest);

        seedAgileQuestions(agileTest);

        // 6. HTML/CSS TEST
        SkillTest htmlTest = new SkillTest();
        htmlTest.setTitle("Đề thi đánh giá kỹ năng HTML5 & CSS3 Cơ bản");
        htmlTest.setSkillName("HTML");
        htmlTest.setCategory("Kỹ năng công nghệ");
        htmlTest.setDifficulty("Trung bình");
        htmlTest.setDescription("Đánh giá kiến thức về Semantic HTML, Box Model, Flexbox, Grid, CSS Selectors và Responsive Web Design.");
        htmlTest.setDurationMinutes(15);
        htmlTest.setTotalQuestionsPerTest(10);
        htmlTest.setPassingScore(60);
        htmlTest.setParticipantCount(3510);
        htmlTest.setBadgeColor("#8b5cf6");
        htmlTest.setStatus("ACTIVE");
        htmlTest = skillTestRepository.save(htmlTest);

        seedHtmlQuestions(htmlTest);

        log.info("Seeded 6 Skill Tests and Question Pools successfully.");
    }

    private void seedJavaQuestions(SkillTest test) {
        List<SkillTestQuestion> list = List.of(
            q(test, "Trong Java, tính chất nào của OOP cho phép một class có nhiều phương thức cùng tên nhưng khác tham số?",
              "Tính đa hình (Polymorphism / Method Overloading)", "Tính đóng gói (Encapsulation)", "Tính kế thừa (Inheritance)", "Tính trừu tượng (Abstraction)",
              "A", "Method Overloading cho phép định nghĩa nhiều method cùng tên nhưng khác nhau về số lượng hoặc kiểu tham số trong cùng một class."),
            q(test, "Collection nào trong Java không cho phép phần tử trùng lặp và không đảm bảo thứ tự thêm vào?",
              "ArrayList", "HashSet", "LinkedList", "TreeSet",
              "B", "HashSet cài đặt giao diện Set, không cho phép phần tử trùng lặp và sử dụng HashTable nên không đảm bảo thứ tự thêm vào."),
            q(test, "Từ khóa 'final' khi được khai báo trước một phương thức (method) có ý nghĩa gì?",
              "Phương thức không thể bị ghi đè (override) ở lớp con", "Phương thức không thể truy cập từ bên ngoài", "Phương thức là phương thức trừu tượng", "Phương thức chạy nhanh hơn",
              "A", "Phương thức 'final' ngăn cản các lớp con ghi đè lại nội dung của phương thức đó."),
            q(test, "Phát biểu nào sau đây đúng về String trong Java?",
              "String là kiểu dữ liệu nguyên thủy (primitive)", "Đối tượng String là bất biến (immutable)", "String có thể thay đổi kích thước trực tiếp trong bộ nhớ", "String kế thừa từ List",
              "B", "Đối tượng String trong Java là bất biến (Immutable), mỗi thao tác thay đổi chuỗi sẽ sinh ra một đối tượng String mới trong String Pool."),
            q(test, "Điểm khác biệt chính giữa interface và abstract class từ Java 8 trở đi là gì?",
              "Interface không thể có method nào có phần thân", "Một class có thể implement nhiều interface nhưng chỉ có thể kế thừa một abstract class", "Interface có thể có constructor", "Abstract class không thể chứa biến",
              "B", "Java hỗ trợ đơn kế thừa với class/abstract class, nhưng cho phép một class triển khai (implement) nhiều interface cùng lúc."),
            q(test, "Từ khóa 'static' trong Java gắn biến hoặc phương thức với đối tượng nào?",
              "Gắn với từng instance cụ thể", "Gắn với chính Class (lớp) chứ không phụ thuộc vào instance", "Gắn với Thread đang thực thi", "Gắn với Garbage Collector",
              "B", "Thành phần static thuộc về Class, được nạp vào bộ nhớ một lần duy nhất khi nạp Class và có thể gọi trực tiếp qua ClassName."),
            q(test, "Trong Java, khối mã 'finally' trong cấu trúc try-catch-finally có đặc điểm gì?",
              "Chỉ được thực thi nếu có exception xảy ra", "Chỉ được thực thi nếu không có exception xảy ra", "Luôn được thực thi kể cả khi có exception hay không", "Chỉ được thực thi khi có return",
              "C", "Khối finally luôn được thực thi (ngoại trừ trường hợp System.exit() được gọi), thường dùng để giải phóng tài nguyên."),
            q(test, "Giao diện (Functional Interface) nào trong Java 8 nhận vào một tham số và trả về kiểu boolean?",
              "Consumer<T>", "Supplier<T>", "Predicate<T>", "Function<T, R>",
              "C", "Predicate<T> có phương thức boolean test(T t), chuyên dùng để kiểm tra điều kiện lọc dữ liệu."),
            q(test, "Điểm khác biệt chính giữa HashMap và ConcurrentHashMap là gì?",
              "HashMap luân phiên khóa toàn bộ bảng", "ConcurrentHashMap an toàn trong môi trường đa luồng (thread-safe) với hiệu năng cao hơn Hashtable", "HashMap không cho phép key null", "ConcurrentHashMap chậm hơn Vector",
              "B", "ConcurrentHashMap là thread-safe và sử dụng cơ chế chia nhỏ khóa (bucket-level locking) giúp tối ưu hiệu năng trong môi trường đa luồng."),
            q(test, "Để so sánh nội dung giá trị giữa 2 đối tượng trong Java, ta nên sử dụng phương thức nào?",
              "Toán tử ==", "Phương thức equals()", "Toán tử ===", "Phương thức compare()",
              "B", "Phương thức equals() dùng để so sánh giá trị logic giữa 2 object, trong khi '==' so sánh địa chỉ vùng nhớ tham chiếu."),
            q(test, "Garbage Collection (GC) trong Java có vai trò gì?",
              "Tự động giải phóng bộ nhớ heap của các đối tượng không còn được tham chiếu", "Dọn dẹp các biến local trong stack", "Tối ưu hóa mã bytecode", "Kiểm tra lỗi runtime",
              "A", "Garbage Collection tự động thu hồi vùng nhớ heap của các đối tượng không còn được trỏ tới bởi bất kỳ tham chiếu nào."),
            q(test, "Trong Java Stream API, phương thức nào sau đây là Terminal Operation?",
              "filter()", "map()", "collect()", "distinct()",
              "C", "collect() là Terminal Operation kết thúc luồng Stream và chuyển đổi kết quả sang Collection hoặc kiểu dữ liệu mong muốn.")
        );
        skillTestQuestionRepository.saveAll(list);
    }

    private void seedJavascriptQuestions(SkillTest test) {
        List<SkillTestQuestion> list = List.of(
            q(test, "Kết quả của biểu thức: typeof null trong JavaScript là gì?",
              "'null'", "'undefined'", "'object'", "'boolean'",
              "C", "Trong JavaScript, do lỗi thiết kế lịch sử ban đầu, typeof null trả về 'object'."),
            q(test, "Sự khác biệt chính giữa '==' và '===' trong JavaScript là gì?",
              "'==' so sánh cả kiểu dữ liệu, '===' ép kiểu tự động", "'===' so sánh nghiêm ngặt cả giá trị và kiểu dữ liệu (không ép kiểu)", "Không có sự khác biệt", "'===' chỉ dùng cho số",
              "B", "Toán tử '===' (Strict Equality) so sánh cả giá trị và kiểu dữ liệu mà không thực hiện ép kiểu tự động."),
            q(test, "Từ khóa nào trong ES6 dùng để khai báo biến có phạm vi khối (block-scoped) và không thể gán lại giá trị?",
              "var", "let", "const", "def",
              "C", "const khai báo hằng số có phạm vi block-scope và không cho phép gán lại (re-assign) tham chiếu."),
            q(test, "Closure trong JavaScript là gì?",
              "Một hàm tự động đóng tab trình duyệt", "Một hàm có khả năng ghi nhớ và truy cập các biến trong phạm vi bao ngoài (outer scope) ngay cả khi hàm ngoài đã thực thi xong", "Một phương thức bất đồng bộ", "Một đối tượng JSON",
              "B", "Closure là sự kết hợp giữa một function và môi trường lexical nơi function đó được khai báo, cho phép truy cập biến của outer scope."),
            q(test, "Event Loop trong JavaScript có nhiệm vụ gì?",
              "Thực thi các hàm toán học nhanh hơn", "Liên tục kiểm tra Call Stack và chuyển các callback từ Task Queue vào Call Stack khi stack trống", "Tự động refresh trang web", "Mã hóa dữ liệu cookie",
              "B", "Event Loop giám sát Call Stack và Callback Queue; khi Call Stack rỗng, nó đẩy tác vụ đầu tiên từ Queue vào Stack để xử lý."),
            q(test, "Phương thức Array nào dùng để tạo ra một mảng mới với các phần tử thỏa mãn điều kiện lọc?",
              "map()", "filter()", "reduce()", "forEach()",
              "B", "filter() duyệt qua từng phần tử và trả về mảng mới chỉ chứa các phần tử trả về true từ hàm callback."),
            q(test, "Kết quả của: console.log(1 + '2' + 3) trong JavaScript là gì?",
              "'6'", "'123'", "'33'", "NaN",
              "B", "Toán tử '+' khi gặp chuỗi sẽ thực hiện phép nối chuỗi: 1 + '2' = '12', sau đó '12' + 3 = '123'."),
            q(test, "Hàm nào dùng để chuyển đổi một chuỗi JSON thành đối tượng JavaScript?",
              "JSON.stringify()", "JSON.parse()", "JSON.object()", "JSON.convert()",
              "B", "JSON.parse() phân tích chuỗi JSON và chuyển đổi thành Object JavaScript tương ứng."),
            q(test, "Promise trong JavaScript có bao nhiêu trạng thái chính?",
              "2 trạng thái (Running, Stopped)", "3 trạng thái (Pending, Fulfilled, Rejected)", "4 trạng thái", "1 trạng thái",
              "B", "Một Promise có 3 trạng thái: Pending (đang chờ), Fulfilled (hoàn thành thành công) và Rejected (thất bại)."),
            q(test, "Khi gọi hàm qua setTimeout(fn, 0), hàm fn sẽ được thực thi khi nào?",
              "Ngay lập tức trước dòng lệnh tiếp theo", "Sau khi Call Stack hiện tại đã hoàn tất việc thực thi", "Sau đúng 1 giây", "Không bao giờ thực thi",
              "B", "setTimeout đưa callback vào Task Queue, callback chỉ được thực thi sau khi Call Stack chính đã hoàn toàn rỗng.")
        );
        skillTestQuestionRepository.saveAll(list);
    }

    private void seedGitQuestions(SkillTest test) {
        List<SkillTestQuestion> list = List.of(
            q(test, "Lệnh nào trong Git dùng để tạo và chuyển ngay sang một nhánh mới?",
              "git branch new-branch", "git checkout -b new-branch", "git switch new-branch", "git merge -b new-branch",
              "B", "Lệnh 'git checkout -b <tên_nhánh>' (hoặc 'git switch -c') sẽ tạo mới nhánh và checkout trực tiếp sang nhánh đó."),
            q(test, "Lệnh 'git pull' thực chất là sự kết hợp của 2 lệnh nào sau đây?",
              "git fetch và git merge", "git push và git commit", "git clone và git checkout", "git add và git commit",
              "A", "'git pull' tương đương với việc chạy 'git fetch' để lấy dữ liệu từ remote về và 'git merge' vào nhánh hiện tại."),
            q(test, "Lệnh nào giúp lưu tạm thời các thay đổi chưa commit vào vùng lưu trữ tạm để làm việc khác?",
              "git stash", "git save", "git pause", "git reset",
              "A", "'git stash' tạm thời cất các thay đổi uncommitted vào ngăn xếp stash để bạn có một working directory sạch sẽ."),
            q(test, "Điểm khác biệt chính giữa 'git merge' và 'git rebase' là gì?",
              "Merge xóa lịch sử commit, Rebase giữ nguyên", "Merge tạo ra commit gộp (merge commit), còn Rebase di chuyển gốc nhánh tạo lịch sử thẳng hàng", "Rebase chỉ dùng trên GitHub", "Không có khác biệt",
              "B", "Rebase viết lại lịch sử commit thành một đường thẳng liên tục, trong khi Merge tạo ra một merge commit nối hai nhánh lại."),
            q(test, "Lệnh nào dùng để hủy bỏ hoàn toàn các thay đổi chưa stage ở Working Directory?",
              "git restore . hoặc git checkout -- .", "git commit --amend", "git clean -f", "git push --force",
              "A", "git restore . sẽ hoàn nguyên các file đã chỉnh sửa về trạng thái của commit gần nhất."),
            q(test, "Để sửa lại message của commit gần nhất, ta sử dụng lệnh nào?",
              "git commit --retry", "git commit --amend", "git commit --change", "git commit --fix",
              "B", "'git commit --amend' cho phép bổ sung thay đổi hoặc sửa lại thông điệp của commit gần nhất."),
            q(test, "File nào trong thư mục dự án dùng để chỉ định các file hoặc folder mà Git không được theo dõi?",
              ".gitconfig", ".gitignore", ".gitmodules", ".gitkeep",
              "B", "File .gitignore khai báo danh sách các pattern file/thư mục cần bỏ qua không đẩy lên repository."),
            q(test, "Lệnh nào dùng để xem lịch sử commit dưới dạng tóm tắt một dòng cho mỗi commit?",
              "git log --oneline", "git show --short", "git history", "git status -s",
              "A", "Lệnh 'git log --oneline' hiển thị ngắn gọn mã SHA rút gọn và tiêu đề của từng commit trên một dòng."),
            q(test, "Khi xảy ra xung đột (Conflict) trong quá trình merge, ta cần làm gì?",
              "Xóa repository và clone lại", "Mở các file bị conflict, giải quyết các đoạn mã xung đột, chạy 'git add' rồi hoàn tất commit", "Chạy ngay 'git push --force'", "Khởi động lại máy tính",
              "B", "Cần mở các file có cờ conflict, chọn đoạn code chính xác, sau đó 'git add' các file đã sửa và hoàn tất merge commit."),
            q(test, "Lệnh nào dùng để sao chép một commit cụ thể từ nhánh khác về nhánh hiện tại?",
              "git cherry-pick <commit-hash>", "git copy <commit-hash>", "git clone <commit-hash>", "git fetch <commit-hash>",
              "A", "'git cherry-pick <commit-hash>' áp dụng các thay đổi của một commit chỉ định vào nhánh làm việc hiện tại.")
        );
        skillTestQuestionRepository.saveAll(list);
    }

    private void seedMySQLQuestions(SkillTest test) {
        List<SkillTestQuestion> list = List.of(
            q(test, "Mệnh đề nào trong SQL được dùng để lọc dữ liệu dựa trên kết quả của các hàm tổng hợp (Aggregate Functions như COUNT, SUM)?",
              "WHERE", "HAVING", "GROUP BY", "ORDER BY",
              "B", "Mệnh đề HAVING được dùng để lọc các nhóm sau khi GROUP BY và tính toán hàm tổng hợp (WHERE không lọc được aggregate)."),
            q(test, "Loại JOIN nào trả về tất cả các dòng từ bảng bên trái và các dòng khớp từ bảng bên phải?",
              "INNER JOIN", "LEFT JOIN (hoặc LEFT OUTER JOIN)", "RIGHT JOIN", "FULL JOIN",
              "B", "LEFT JOIN giữ lại toàn bộ bản ghi của bảng bên trái, nếu bảng bên phải không có bản ghi khớp thì điền NULL."),
            q(test, "Thuộc tính ACID trong hệ quản trị cơ sở dữ liệu đại diện cho 4 từ khóa nào?",
              "Atomicity, Consistency, Isolation, Durability", "Accuracy, Control, Integration, Database", "Access, Connection, Index, Data", "Auto, Cache, Insert, Delete",
              "A", "ACID đại diện cho Tính nguyên tố (Atomicity), Tính nhất quán (Consistency), Tính cô lập (Isolation) và Tính bền vững (Durability)."),
            q(test, "Mục đích chính của việc tạo Index (chỉ mục) trên một cột trong MySQL là gì?",
              "Tăng tốc độ truy vấn tìm kiếm dữ liệu (SELECT)", "Tăng tốc độ ghi dữ liệu (INSERT, UPDATE)", "Tiết kiệm dung lượng ổ cứng", "Tự động mã hóa mật khẩu",
              "A", "Index giúp cơ sở dữ liệu tra cứu bản ghi nhanh hơn tương tự mục lục sách, tuy nhiên sẽ làm chậm nhẹ thao tác INSERT/UPDATE."),
            q(test, "Câu lệnh nào dùng để xóa toàn bộ dữ liệu trong bảng nhanh nhất và reset lại cột Auto Increment?",
              "DELETE FROM table_name;", "TRUNCATE TABLE table_name;", "DROP TABLE table_name;", "REMOVE FROM table_name;",
              "B", "TRUNCATE TABLE là lệnh DDL giải phóng toàn bộ trang dữ liệu và reset lại ID tự tăng, nhanh hơn DELETE từng dòng."),
            q(test, "Khóa chính (Primary Key) trong MySQL có đặc điểm gì?",
              "Có thể chứa giá trị NULL", "Duy nhất (UNIQUE) và không bao giờ chứa giá trị NULL", "Một bảng có thể có nhiều Primary Key riêng biệt", "Chỉ áp dụng cho kiểu số nguyên",
              "B", "Primary Key xác định duy nhất mỗi bản ghi trong bảng và không được phép chứa giá trị NULL."),
            q(test, "Hàm nào trong SQL dùng để đếm số lượng dòng không có giá trị NULL trong một cột?",
              "SUM()", "COUNT(column_name)", "TOTAL()", "AVG()",
              "B", "COUNT(column_name) đếm số lượng dòng có giá trị khác NULL của cột đó."),
            q(test, "Để giới hạn số lượng bản ghi trả về là 10 dòng bắt đầu từ dòng thứ 21, ta dùng cú pháp nào trong MySQL?",
              "LIMIT 10 OFFSET 20", "LIMIT 20, 10", "Cả A và B đều đúng", "TOP 10 START 20",
              "C", "Trong MySQL, cả cú pháp 'LIMIT 10 OFFSET 20' và 'LIMIT 20, 10' đều lấy 10 bản ghi bỏ qua 20 bản ghi đầu."),
            q(test, "Khóa ngoại (Foreign Key) được dùng để làm gì trong cơ sở dữ liệu quan hệ?",
              "Tự động tăng giá trị bản ghi", "Đảm bảo tính toàn vẹn tham chiếu giữa 2 bảng", "Mã hóa dữ liệu nhạy cảm", "Tăng tốc độ kết nối mạng",
              "B", "Khóa ngoại liên kết với khóa chính của bảng khác để duy trì tính toàn vẹn dữ liệu tham chiếu giữa các quan hệ."),
            q(test, "Lệnh TRANSACTION nào dùng để hủy bỏ tất cả các thao tác dữ liệu vừa thực hiện trong transaction?",
              "COMMIT", "ROLLBACK", "REVOKE", "ABORT",
              "B", "ROLLBACK khôi phục dữ liệu về trạng thái trước khi bắt đầu transaction, hủy bỏ các thay đổi chưa được COMMIT.")
        );
        skillTestQuestionRepository.saveAll(list);
    }

    private void seedAgileQuestions(SkillTest test) {
        List<SkillTestQuestion> list = List.of(
            q(test, "Trong mô hình Scrum, ai là người chịu trách nhiệm tối ưu hóa giá trị của sản phẩm và quản lý Product Backlog?",
              "Scrum Master", "Product Owner (PO)", "Development Team", "Project Manager",
              "B", "Product Owner chịu trách nhiệm định hướng sản phẩm, quản lý và sắp xếp mức độ ưu tiên của Product Backlog."),
            q(test, "Cuộc họp Daily Scrum thường diễn ra trong thời gian tối đa bao lâu?",
              "15 phút", "30 phút", "45 phút", "60 phút",
              "A", "Daily Scrum là cuộc họp ngắn gọn diễn ra hàng ngày với timebox tối đa 15 phút để đồng bộ công việc trong team."),
            q(test, "Ba câu hỏi kinh điển trong buổi họp Daily Scrum là gì?",
              "Hôm qua đã làm gì? Hôm nay sẽ làm gì? Có gặp khó khăn/trở ngại gì không?", "Dự án lời bao nhiêu? Khách hàng khen gì? Khi nào được thưởng?", "Bug này do ai gây ra? Ai chịu phạt? Khi nào fix xong?", "Hôm nay ăn gì? Mấy giờ về? Đi đâu chơi?",
              "A", "3 câu hỏi Daily Scrum: Hôm qua tôi đã làm gì, hôm nay tôi sẽ làm gì, và có trở ngại (impediment) nào cản trở công việc không."),
            q(test, "Sự kiện nào trong Scrum diễn ra ở cuối Sprint để team cùng nhìn lại quy trình và cải tiến cách làm việc?",
              "Sprint Planning", "Sprint Review", "Sprint Retrospective", "Backlog Refinement",
              "C", "Sprint Retrospective là cơ hội để Scrum Team tự thanh tra và lập kế hoạch cải tiến quy trình làm việc cho các Sprint tiếp theo."),
            q(test, "Thời lượng lý tưởng của một Sprint trong Scrum thường kéo dài từ bao lâu?",
              "1 đến 4 tuần", "6 tháng đến 1 năm", "1 ngày duy nhất", "Không giới hạn thời gian",
              "A", "Một Sprint thường có độ dài cố định từ 1 đến 4 tuần (phổ biến nhất là 2 tuần) để tạo nhịp độ đều đặn."),
            q(test, "Định nghĩa Hoàn thành (Definition of Done - DoD) trong Scrum có vai trò gì?",
              "Danh sách tính năng cần phát triển", "Tiêu chuẩn chung để xác định một phần việc (Increment) đã đạt chất lượng bàn giao", "Kế hoạch phân bổ ngân sách", "Chữ ký nghiệm thu của sếp",
              "B", "DoD là sự hiểu biết chung của cả đội về những tiêu chí kỹ thuật/chất lượng cần đạt để một hạng mục được coi là hoàn tất."),
            q(test, "Ai là người có trách nhiệm loại bỏ các trở ngại (impediments) cản trở tiến độ của Development Team trong Scrum?",
              "Product Owner", "Scrum Master", "Khách hàng", "Bộ phận HR",
              "B", "Scrum Master đóng vai trò Servant Leader, bảo vệ nhóm và hỗ trợ gỡ bỏ các rào cản kỹ thuật/quy trình cản trở nhóm."),
            q(test, "User Story thường được viết theo cấu trúc tiêu chuẩn nào?",
              "Là một [Người dùng], tôi muốn [Tính năng], để [Lợi ích mang lại]", "Làm xong [Công việc] trước ngày [Hạn chót]", "Hệ thống phải tự động [Thao tác]", "Nếu [Điều kiện] thì thực hiện [Hành động]",
              "A", "Cấu trúc User Story phổ biến: As a [User], I want [Feature], So that [Value/Benefit]."),
            q(test, "Story Point trong Scrum dùng để ước lượng điều gì?",
              "Số giờ chính xác cần làm", "Độ phức tạp, khối lượng công việc và rủi ro tương đối của một User Story", "Mức lương trả cho lập trình viên", "Số dòng code cần viết",
              "B", "Story Point là đơn vị ước lượng tương đối dựa trên độ phức tạp, lượng công việc và sự không chắc chắn của tác vụ."),
            q(test, "Biểu đồ nào thường dùng trong Scrum để theo dõi khối lượng công việc còn lại so với thời gian trong Sprint?",
              "Burndown Chart", "Gantt Chart", "Pie Chart", "Flowchart",
              "A", "Burndown Chart trực quan hóa lượng công việc còn lại giảm dần theo từng ngày của Sprint hướng tới mục tiêu 0.")
        );
        skillTestQuestionRepository.saveAll(list);
    }

    private void seedHtmlQuestions(SkillTest test) {
        List<SkillTestQuestion> list = List.of(
            q(test, "Thẻ HTML5 nào mang ý nghĩa ngữ nghĩa (Semantic HTML) dùng để chứa phần điều hướng trang web?",
              "<div>", "<nav>", "<section>", "<aside>",
              "B", "<nav> là thẻ ngữ nghĩa đại diện cho phần chứa các liên kết điều hướng chính của trang web."),
            q(test, "CSS Box Model bao gồm 4 thành phần nào theo thứ tự từ trong ra ngoài?",
              "Content -> Padding -> Border -> Margin", "Content -> Margin -> Border -> Padding", "Border -> Margin -> Padding -> Content", "Padding -> Border -> Margin -> Content",
              "A", "CSS Box Model gồm: Nội dung (Content), Khoảng đệm (Padding), Đường viền (Border) và Khoảng cách ngoài (Margin)."),
            q(test, "Thuộc tính nào trong CSS Flexbox dùng để căn chỉnh các phần tử con theo trục chính (Main Axis)?",
              "align-items", "justify-content", "flex-direction", "align-content",
              "B", "justify-content dùng để căn chỉnh và phân bổ khoảng trống cho các flex items dọc theo trục chính (Main Axis)."),
            q(test, "Giá trị nào của thuộc tính 'position' trong CSS khiến phần tử cố định theo khung nhìn trình duyệt ngay cả khi cuộn trang?",
              "relative", "absolute", "fixed", "static",
              "C", "'position: fixed' ghim phần tử cố định so với viewport của trình duyệt, không di chuyển khi cuộn trang."),
            q(test, "Đơn vị 'rem' trong CSS có kích thước phụ thuộc vào giá trị nào?",
              "Font-size của phần tử cha trực tiếp", "Font-size của phần tử gốc (thẻ <html>)", "Chiều rộng màn hình", "Chiều cao của trình duyệt",
              "B", "1rem (root em) bằng với kích thước font-size của phần tử root (mặc định thẻ <html> là 16px)."),
            q(test, "Thuộc tính nào dùng để đảm bảo kích thước chiều rộng của phần tử bao gồm cả Padding và Border?",
              "box-sizing: border-box;", "box-sizing: content-box;", "display: block;", "overflow: hidden;",
              "A", "'box-sizing: border-box' tính padding và border vào tổng width/height của element, giúp kiểm soát layout dễ dàng."),
            q(test, "Để tạo một bố cục lưới 3 cột có kích thước bằng nhau bằng CSS Grid, ta dùng cú pháp nào?",
              "grid-template-columns: repeat(3, 1fr);", "grid-columns: 3;", "grid-template-rows: 3fr;", "display: 3-grid;",
              "A", "'grid-template-columns: repeat(3, 1fr);' tạo ra 3 cột có tỉ lệ độ rộng bằng nhau phân chia đều diện tích lưới."),
            q(test, "Thẻ meta nào bắt buộc phải có trong thẻ <head> để website hiển thị responsive tốt trên thiết bị di động?",
              "<meta name='viewport' content='width=device-width, initial-scale=1.0'>", "<meta name='mobile' content='true'>", "<meta charset='UTF-8'>", "<meta http-equiv='X-UA-Compatible'>",
              "A", "Thẻ viewport giúp trình duyệt di động đặt chiều rộng viewport bằng chiều rộng vật lý của thiết bị và không zoom mặc định."),
            q(test, "Selector nào trong CSS có độ ưu tiên (Specificity) cao nhất?",
              "Tag selector (ví dụ: div)", "Class selector (ví dụ: .btn)", "ID selector (ví dụ: #header)", "Universal selector (ví dụ: *)",
              "C", "ID selector (#id) có trọng số Specificity cao hơn Class selector (.class) và Tag selector."),
            q(test, "Thuộc tính 'alt' trong thẻ <img> có tác dụng gì quan trọng nhất?",
              "Tạo hiệu ứng phóng to ảnh khi hover", "Cung cấp văn bản thay thế khi ảnh lỗi và hỗ trợ SEO / Trình đọc màn hình (Screen Readers)", "Đổi màu nền của ảnh", "Tăng độ phân giải cho bức ảnh",
              "B", "Thuộc tính alt cung cấp mô tả văn bản thay thế khi hình ảnh không tải được và hỗ trợ tiếp cận (Accessibility) cũng như SEO.")
        );
        skillTestQuestionRepository.saveAll(list);
    }

    private SkillTestQuestion q(SkillTest test, String text, String a, String b, String c, String d, String correct, String exp) {
        SkillTestQuestion q = new SkillTestQuestion();
        q.setSkillTest(test);
        q.setQuestionText(text);
        q.setOptionA(a);
        q.setOptionB(b);
        q.setOptionC(c);
        q.setOptionD(d);
        q.setCorrectOption(correct);
        q.setExplanation(exp);
        q.setStatus("ACTIVE");
        return q;
    }
}
