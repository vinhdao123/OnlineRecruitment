package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.repository.Recruiter.InterviewRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateInterviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// =====================================================
// SERVICE ĐẶT LỊCH PHỎNG VẤN CỦA ỨNG VIÊN
//
// Luồng tổng quát:
//   - Xem khung giờ: kiểm tra đơn hợp lệ -> lọc khung giờ trống
//   - Đặt lịch: kiểm tra ứng viên, đơn, khung giờ -> gọi
//     InterviewRepository.bookInterview (SQL tự mở transaction
//     và kiểm tra lại toàn bộ quy tắc trùng lịch trước khi INSERT)
//
// Lưu ý: KHÔNG gắn @Transactional cho bookInterview vì câu SQL
// trong InterviewRepository đã tự BEGIN / COMMIT / ROLLBACK.
// =====================================================

@Service
public class CandidateInterviewService {

    // Dữ liệu cho trang chọn khung giờ phỏng vấn
    public record BookingPage(
            Map<String, Object> application,
            List<Map<String, Object>> slots
    ) {
    }

    private final CandidateInterviewRepository candidateInterviewRepository;
    private final InterviewRepository interviewRepository;

    public CandidateInterviewService(
            CandidateInterviewRepository candidateInterviewRepository,
            InterviewRepository interviewRepository) {
        this.candidateInterviewRepository = candidateInterviewRepository;
        this.interviewRepository = interviewRepository;
    }

    // =====================================================
    // 1. LẤY DỮ LIỆU TRANG CHỌN KHUNG GIỜ PHỎNG VẤN
    //
    // 1. Lấy đơn ứng tuyển, chỉ khi đơn thuộc tài khoản đang đăng nhập
    // 2. Chỉ đơn ACCEPTED mới được đặt lịch
    // 3. Đơn đã có lịch SCHEDULED thì không cho đặt nữa
    // 4. Lọc các khung giờ ứng viên được phép chọn
    //
    // Vi phạm bước 1-3 -> Optional rỗng
    // (Controller sẽ quay về danh sách đơn).
    // =====================================================

    public Optional<BookingPage> getBookingPage(Long applicationId, String loginName) {

        // 1. Lấy thông tin đơn ứng tuyển
        List<Map<String, Object>> applications =
                candidateInterviewRepository.findApplicationOfLoginUser(applicationId, loginName);

        // Không tìm thấy đơn hoặc không thuộc ứng viên
        if (applications.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> application = applications.get(0);

        // 2. Chỉ đơn ACCEPTED mới được đặt lịch
        if (!"ACCEPTED".equalsIgnoreCase(
                String.valueOf(application.get("applicationStatus")))) {
            return Optional.empty();
        }

        Long candidateId = ((Number)
                application.get("candidateId")).longValue();

        Long companyId = ((Number)
                application.get("companyId")).longValue();

        Long recruiterId = ((Number)
                application.get("recruiterId")).longValue();

        // 3. Kiểm tra đơn này đã có lịch chưa
        if (candidateInterviewRepository.countScheduledInterviews(applicationId, candidateId) > 0) {
            return Optional.empty();
        }

        // 4. Lọc khung giờ
        List<Map<String, Object>> slots =
                candidateInterviewRepository.findAvailableSlots(companyId, recruiterId, candidateId);

        return Optional.of(new BookingPage(application, slots));
    }

    // =====================================================
    // 2. XÁC NHẬN ĐẶT LỊCH PHỎNG VẤN
    //
    // 1. Lấy candidate ID từ tài khoản đăng nhập
    // 2. Lấy recruiter + company từ đơn (đơn phải ACCEPTED, đúng ứng viên)
    // 3. Kiểm tra khung giờ thuộc đúng công ty và recruiter
    // 4. Gọi repository đặt lịch
    //    -> lỗi SQL (trùng lịch, slot đã bị chọn...) hoặc không
    //       thêm được dòng nào: báo lỗi
    //
    // Mọi lỗi ném ra IllegalArgumentException kèm thông báo
    // tiếng Việt để Controller hiển thị.
    // =====================================================

    public void bookInterview(String loginName, Long applicationId, Long availabilityId) {

        // 1. Lấy candidate ID
        List<Long> candidateIds =
                candidateInterviewRepository.findCandidateIdsByLoginName(loginName);

        if (candidateIds.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản ứng viên.");
        }

        Long candidateId = candidateIds.get(0);

        // 2. Lấy recruiter và company từ đơn
        List<Map<String, Object>> applications =
                candidateInterviewRepository.findAcceptedApplicationJob(applicationId, candidateId);

        if (applications.isEmpty()) {
            throw new IllegalArgumentException(
                    "Đơn ứng tuyển không hợp lệ hoặc chưa được chấp nhận.");
        }

        Map<String, Object> application = applications.get(0);

        Long recruiterId = ((Number)
                application.get("recruiter_id")).longValue();

        Long companyId = ((Number)
                application.get("company_id")).longValue();

        // 3. Kiểm tra khung giờ thuộc đúng công ty và recruiter
        if (!candidateInterviewRepository.existsAvailability(availabilityId, companyId, recruiterId)) {
            throw new IllegalArgumentException("Khung giờ phỏng vấn không hợp lệ.");
        }

        // 4. Thực hiện đặt lịch
        // Repository sẽ kiểm tra lại toàn bộ quy tắc
        // trong transaction trước khi INSERT.
        int result;

        try {
            result = interviewRepository.bookInterview(
                    applicationId,
                    availabilityId,
                    candidateId,
                    recruiterId,
                    companyId
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Không thể đặt lịch. Khung giờ có thể đã bị trùng hoặc được người khác chọn.",
                    e);
        }

        if (result <= 0) {
            throw new IllegalArgumentException("Không thể đặt lịch. Vui lòng thử lại.");
        }
    }
}
