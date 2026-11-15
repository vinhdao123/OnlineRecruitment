package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// =====================================================
// SERVICE QUẢN LÝ ĐƠN ỨNG TUYỂN CỦA ỨNG VIÊN
//
// Luồng tổng quát:
//   Controller lấy loginName từ Spring Security
//     -> Service kiểm tra nghiệp vụ
//     -> CandidateApplicationRepository đọc / ghi dbo.applications
//     -> Lỗi nghiệp vụ được ném ra dạng IllegalArgumentException
//        với thông báo tiếng Việt để Controller hiển thị
// =====================================================

@Service
public class CandidateApplicationService {

    private final CandidateApplicationRepository candidateApplicationRepository;

    public CandidateApplicationService(
            CandidateApplicationRepository candidateApplicationRepository) {
        this.candidateApplicationRepository = candidateApplicationRepository;
    }

    // ==========================================
    // LẤY DANH SÁCH ĐƠN ỨNG TUYỂN
    //
    // Trả về các đơn của ứng viên (mới nhất trước), mỗi đơn kèm
    // thông tin công việc, công ty, CV đã dùng và lịch phỏng vấn
    // SCHEDULED gần nhất (nếu đã đặt lịch).
    // ==========================================

    public List<Map<String, Object>> getMyApplications(String loginName) {
        return candidateApplicationRepository.findApplicationsByLoginName(loginName);
    }

    // ==========================================
    // NỘP LẠI ĐƠN ỨNG TUYỂN
    //
    // 1. Xác định đúng ứng viên đang đăng nhập
    //    -> không có hồ sơ ứng viên: báo lỗi
    // 2. Nếu đã có đơn SUBMITTED / ACCEPTED / APPROVED cho job này
    //    -> không cho nộp lại
    // 3. Chỉ cho nộp lại khi tồn tại đơn REJECTED cho job này
    // 4. Tạo đơn mới SUBMITTED, sao chép CV + snapshot
    //    từ đơn bị từ chối gần nhất
    //    -> không thêm được dòng nào: báo lỗi
    //
    // Lỗi được ném ra dạng IllegalArgumentException,
    // Controller sẽ bắt và hiển thị message.
    // ==========================================

    @Transactional
    public void applyAgain(String loginName, int jobId) {

        // 1. Xác định đúng ứng viên đang đăng nhập
        List<Integer> candidateIds =
                candidateApplicationRepository.findCandidateIdsByLoginName(loginName);

        if (candidateIds.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy hồ sơ ứng viên.");
        }

        int candidateId = candidateIds.get(0);

        // 2. Kiểm tra đã có đơn đang chờ hoặc đã được chấp nhận chưa
        if (candidateApplicationRepository.countActiveApplications(candidateId, jobId) > 0) {
            throw new IllegalArgumentException(
                    "Bạn đã có đơn đang chờ xử lý hoặc đã được chấp nhận.");
        }

        // 3. Chỉ cho phép nộp lại nếu có đơn cũ bị từ chối
        if (candidateApplicationRepository.countRejectedApplications(candidateId, jobId) == 0) {
            throw new IllegalArgumentException(
                    "Bạn không có đơn bị từ chối cho công việc này.");
        }

        // 4. Tạo đơn mới, giữ nguyên công việc, công ty và CV
        // từ đơn bị từ chối gần nhất.
        int rows = candidateApplicationRepository.insertFromLatestRejected(candidateId, jobId);

        if (rows == 0) {
            throw new IllegalArgumentException(
                    "Không thể nộp lại đơn. Vui lòng thử lại.");
        }
    }
}
