package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.repository.candidate.ApplicationSubmitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// =====================================================
// SERVICE NỘP HỒ SƠ ỨNG TUYỂN
//
// Luồng tổng quát khi ứng viên bấm "Gửi hồ sơ":
//   1. Tìm user -> candidate từ tài khoản đăng nhập
//   2. Kiểm tra công việc còn mở tuyển
//   3. Kiểm tra CV ACTIVE thuộc đúng ứng viên
//   4. Kiểm tra đơn cũ (đang chờ / đã nhận -> chặn, bị từ chối -> mở lại)
//   5. Chụp snapshot toàn bộ CV thành JSON
//   6. Xác định file CV đính kèm (nếu có)
//   7. Mở lại đơn bị từ chối hoặc tạo đơn mới
//
// Lỗi nghiệp vụ ném ra IllegalArgumentException / IllegalStateException,
// lỗi tạo JSON ném ra JacksonException; Controller bắt và hiển thị.
// Mọi thao tác chạy trong một transaction.
// =====================================================

@Service
public class ApplicationSubmitService {

    // Kết quả nộp đơn để Controller chọn thông báo
    public enum SubmitResult {
        SUBMITTED,          // Nộp thành công (tạo mới hoặc mở lại đơn bị từ chối)
        ALREADY_APPLIED     // Đã có đơn SUBMITTED / ACCEPTED cho công việc này
    }

    private final ApplicationSubmitRepository applicationSubmitRepository;
    private final ObjectMapper objectMapper;

    public ApplicationSubmitService(
            ApplicationSubmitRepository applicationSubmitRepository,
            ObjectMapper objectMapper) {
        this.applicationSubmitRepository = applicationSubmitRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmitResult submitApplication(String loginName, Long jobId, Long cvId) {

        // 1. Tìm user đang đăng nhập (username hoặc email)
        List<Map<String, Object>> users =
                applicationSubmitRepository.findUserByLoginName(loginName);

        if (users.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không tìm thấy tài khoản đang đăng nhập.");
        }

        Long userId = ((Number) users.get(0).get("id")).longValue();

        // 2. Lấy candidate ID từ tài khoản hiện tại
        List<Map<String, Object>> candidates =
                applicationSubmitRepository.findCandidateByUserId(userId);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Tài khoản chưa có hồ sơ ứng viên.");
        }

        Long candidateId =
                ((Number) candidates.get(0).get("id")).longValue();

        // 3. Kiểm tra công việc và công ty sở hữu công việc
        //    (job ACTIVE, công ty ACTIVE, chưa quá hạn nộp)
        if (applicationSubmitRepository.findOpenJob(jobId).isEmpty()) {
            throw new IllegalArgumentException(
                    "Không tìm thấy công việc hoặc công ty tuyển dụng.");
        }

        // 4. Chỉ chấp nhận CV ACTIVE thuộc đúng ứng viên
        List<Map<String, Object>> cvs =
                applicationSubmitRepository.findActiveCv(cvId, candidateId);

        if (cvs.isEmpty()) {
            throw new IllegalArgumentException(
                    "CV không tồn tại hoặc không thuộc tài khoản của bạn.");
        }

        Map<String, Object> cv = cvs.get(0);

        Long templateVersionId =
                ((Number) cv.get("templateVersionId")).longValue();

        // 5. Kiểm tra đơn ứng tuyển đã tồn tại
        //    - SUBMITTED / ACCEPTED: không cho nộp thêm
        //    - REJECTED: ghi nhớ ID để mở lại đơn ở bước 8
        Long rejectedApplicationId = null;

        List<Map<String, Object>> existingApplications =
                applicationSubmitRepository.findExistingApplications(candidateId, jobId);

        if (!existingApplications.isEmpty()) {

            Map<String, Object> oldApplication = existingApplications.get(0);

            String oldStatus = (String) oldApplication.get("status");

            Long oldApplicationId =
                    ((Number) oldApplication.get("id")).longValue();

            if ("SUBMITTED".equals(oldStatus)
                    || "ACCEPTED".equals(oldStatus)) {
                return SubmitResult.ALREADY_APPLIED;
            }

            if ("REJECTED".equals(oldStatus)) {
                rejectedApplicationId = oldApplicationId;
            }
        }

        // 6. Tạo bản chụp dữ liệu CV tại thời điểm ứng tuyển
        //    -> nhà tuyển dụng luôn xem đúng CV lúc nộp,
        //       dù sau này ứng viên có sửa CV
        String snapshotJson = buildCvSnapshotJson(cv, cvId);

        // 7. Lấy đường dẫn file đính kèm nếu có
        String filePath = resolveCvFilePath(cv, cvId);

        // 8. Lưu đơn ứng tuyển
        if (rejectedApplicationId != null) {

            // Đơn cũ bị từ chối: mở lại đơn ứng tuyển
            // Giữ nguyên toàn bộ dữ liệu CV đã nộp trước đó
            int updated = applicationSubmitRepository.reopenRejectedApplication(
                    rejectedApplicationId,
                    candidateId,
                    jobId
            );

            if (updated == 0) {
                throw new IllegalStateException(
                        "Không thể ứng tuyển lại. Vui lòng tải lại trang."
                );
            }

        } else {

            // Chưa từng ứng tuyển: tạo đơn mới
            applicationSubmitRepository.insertApplication(
                    candidateId,
                    jobId,
                    cvId,
                    templateVersionId,
                    snapshotJson,
                    filePath
            );
        }

        return SubmitResult.SUBMITTED;
    }

    // ==========================================
    // CHỤP SNAPSHOT CV
    // Gom thông tin chính của CV và toàn bộ các mục
    // (kinh nghiệm, học vấn, kỹ năng, dự án, chứng chỉ,
    //  giải thưởng, hoạt động, người tham chiếu)
    // thành một JSON lưu vào applications.cv_snapshot_json.
    // ==========================================

    private String buildCvSnapshotJson(Map<String, Object> cv, Long cvId) {

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("cv", cv);
        snapshot.put("workExperiences", applicationSubmitRepository.findWorkExperiences(cvId));
        snapshot.put("educations", applicationSubmitRepository.findEducations(cvId));
        snapshot.put("skills", applicationSubmitRepository.findSkills(cvId));
        snapshot.put("projects", applicationSubmitRepository.findProjects(cvId));
        snapshot.put("certificates", applicationSubmitRepository.findCertificates(cvId));
        snapshot.put("awards", applicationSubmitRepository.findAwards(cvId));
        snapshot.put("activities", applicationSubmitRepository.findActivities(cvId));
        snapshot.put("references", applicationSubmitRepository.findReferences(cvId));

        return objectMapper.writeValueAsString(snapshot);
    }

    // ==========================================
    // XÁC ĐỊNH FILE CV ĐÍNH KÈM
    // Thứ tự ưu tiên:
    //   1. File trong bảng cv_attachments (file chính, mới nhất)
    //   2. cv.file_url nếu đúng định dạng đường dẫn lưu trữ hợp lệ
    //   3. _cvAssets.fileUrl trong JSON metadata ở cv.content
    // Không tìm thấy -> null (đơn không kèm file).
    // ==========================================

    private String resolveCvFilePath(Map<String, Object> cv, Long cvId) {

        List<Map<String, Object>> attachments =
                applicationSubmitRepository.findPrimaryAttachment(cvId);

        String filePath = attachments.isEmpty()
                ? null
                : (String) attachments.get(0).get("storage_path");
        if (filePath == null && cv.get("fileUrl") instanceof String cvFileUrl && !cvFileUrl.isBlank()) {
            String cleanFileUrl = cvFileUrl.trim().replace("\\", "/");
            if (cleanFileUrl.matches("(?i)(storage/cvs/[a-f0-9-]+|/?uploads/cv/[^/\\\\]+)\\.(pdf|doc|docx)")) {
                filePath = cleanFileUrl;
            }
        }
        if (filePath == null && cv.get("content") instanceof String metadata && !metadata.isBlank()) {
            try {
                String uploadedPath = objectMapper.readTree(metadata).path("_cvAssets").path("fileUrl").asText("");
                if (uploadedPath.matches("storage/cvs/[a-f0-9-]+\\.(pdf|doc|docx)")) filePath = uploadedPath;
            } catch (JacksonException ignored) {
                // Older CVs may store plain text in content rather than asset metadata.
            }
        }
        return filePath;
    }
}
