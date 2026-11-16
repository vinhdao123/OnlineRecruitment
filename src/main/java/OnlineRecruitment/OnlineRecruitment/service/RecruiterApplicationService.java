package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.repository.Recruiter.RecruiterApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecruiterApplicationService {

    private final RecruiterApplicationRepository applicationRepository;
    private final SkillMatchingService skillMatchingService;
    private final CvApplicationPreviewService cvApplicationPreviewService;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final CvFileStorageService cvFileStorageService;

    public RecruiterApplicationService(
            RecruiterApplicationRepository applicationRepository,
            SkillMatchingService skillMatchingService,
            CvApplicationPreviewService cvApplicationPreviewService,
            ObjectMapper objectMapper,
            EmailService emailService,
            CvFileStorageService cvFileStorageService) {

        this.applicationRepository = applicationRepository;
        this.skillMatchingService = skillMatchingService;
        this.cvApplicationPreviewService = cvApplicationPreviewService;
        this.objectMapper = objectMapper;
        this.emailService = emailService;
        this.cvFileStorageService = cvFileStorageService;
    }

    // =========================================
    // DANH SÁCH ĐƠN ỨNG TUYỂN
    // =========================================

    public Map<String, Object> getApplications(
            String username,
            String keyword,
            String status,
            Long jobId,
            String sort) {

        Map<String, Object> result = new HashMap<>();

        Long recruiterId =
                applicationRepository.findRecruiterIdByLoginName(username);

        if (recruiterId == null) {
            result.put("applications", List.of());
            result.put("jobs", List.of());
            return result;
        }

        List<Map<String, Object>> jobs =
                applicationRepository.findJobsByRecruiterId(recruiterId);

        List<Map<String, Object>> applications =
                applicationRepository.findApplications(
                        recruiterId, status, jobId, keyword, sort);

        skillMatchingService.attachScores(applications);

        result.put("applications", applications);
        result.put("jobs", jobs);

        return result;
    }

    // =========================================
    // XEM CV THEO ĐƠN ỨNG TUYỂN
    // =========================================

    public Map<String, Object> getApplicationCv(
            Long applicationId,
            String username) {

        Long recruiterId =
                applicationRepository.findRecruiterIdByLoginName(username);

        if (recruiterId == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy tài khoản nhà tuyển dụng."
            );
        }

        List<Map<String, Object>> rows =
                applicationRepository.findApplicationCv(
                        applicationId, recruiterId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy đơn ứng tuyển hoặc bạn không có quyền xem."
            );
        }

        Map<String, Object> applicationData =
                new HashMap<>(rows.get(0));

        // Đọc CV snapshot tại thời điểm ứng tuyển
        Map<String, Object> snapshotMap = Map.of();

        Object rawSnapshot = applicationData.get("cvSnapshotJson");

        if (rawSnapshot instanceof String json && !json.isBlank()) {
            try {
                snapshotMap = objectMapper.readValue(
                        json,
                        new TypeReference<Map<String, Object>>() {}
                );

                if (snapshotMap.get("cv") instanceof Map<?, ?> snapCv) {
                    if (snapCv.get("title") != null) {
                        applicationData.put("cvTitle", snapCv.get("title"));
                    }

                    if (snapCv.get("professionalTitle") != null) {
                        applicationData.put(
                                "professionalTitle",
                                snapCv.get("professionalTitle")
                        );
                    }

                    if (snapCv.get("summary") != null) {
                        applicationData.put("summary", snapCv.get("summary"));
                    }
                }
            } catch (Exception ignored) {
                // Giữ nguyên dữ liệu CV hiện tại nếu snapshot không đọc được
            }
        }

        Map<String, Object> result = new HashMap<>();

        result.put("snapshotData", snapshotMap);
        result.put("applicationData", applicationData);

        Long candidateId = toLong(applicationData.get("candidateId"));
        Long cvId = toLong(applicationData.get("cvId"));
        Long templateVersionId =
                toLong(applicationData.get("cvTemplateVersionId"));

        String filePathSnapshot =
                toStringOrNull(applicationData.get("cvFilePathSnapshot"));

        String cvTitle =
                toStringOrNull(applicationData.get("cvTitle"));

        String professionalTitle =
                toStringOrNull(applicationData.get("professionalTitle"));

        String summary =
                toStringOrNull(applicationData.get("summary"));

        Map<String, Object> previewModel =
                cvApplicationPreviewService.buildPreviewModel(
                        candidateId,
                        cvId,
                        templateVersionId,
                        filePathSnapshot,
                        snapshotMap,
                        cvTitle,
                        professionalTitle,
                        summary
                );

        result.putAll(previewModel);

        result.put(
                "avatarEndpoint",
                "/recruiter/applications/" + applicationId + "/cv-avatar"
        );

        result.put(
                "fileDownloadUrl",
                "/recruiter/applications/" + applicationId + "/cv-file"
        );

        result.put(
                "fileViewUrl",
                "/recruiter/applications/" + applicationId + "/cv-file?inline=true"
        );

        return result;
    }

    // =========================================
    // DUYỆT ĐƠN ỨNG TUYỂN
    // =========================================

    public Map<String, String> approveApplication(
            Long applicationId,
            String username) {

        Map<String, String> result = new HashMap<>();

        Long recruiterId =
                applicationRepository.findRecruiterIdByLoginName(username);

        if (recruiterId == null) {
            result.put("redirect", "/login");
            return result;
        }

        int updated =
                applicationRepository.approveApplication(
                        applicationId, recruiterId);

        if (updated == 0) {
            result.put(
                    "errorMessage",
                    "Không tìm thấy đơn chờ duyệt hoặc bạn không có quyền thao tác."
            );
            result.put("redirect", "/recruiter/applications?status=SUBMITTED");
            return result;
        }

        try {
            List<Map<String, Object>> rows =
                    applicationRepository.findApprovalEmailData(
                            applicationId, recruiterId);

            if (rows.isEmpty()) {
                throw new IllegalStateException(
                        "Không lấy được thông tin đơn ứng tuyển."
                );
            }

            Map<String, Object> data = rows.get(0);

            String candidateName = String.valueOf(data.get("candidateName"));
            String candidateEmail = String.valueOf(data.get("candidateEmail"));
            String recruiterEmail = String.valueOf(data.get("recruiterEmail"));
            String companyName = String.valueOf(data.get("companyName"));
            String jobTitle = String.valueOf(data.get("jobTitle"));

            String candidateContent = """
                    Xin chào %s,

                    CHÚC MỪNG BẠN!

                    Hồ sơ ứng tuyển của bạn đã được nhà tuyển dụng chấp nhận.

                    Thông tin ứng tuyển:

                    Công ty: %s
                    Vị trí: %s
                    Trạng thái: ACCEPTED

                    Cảm ơn bạn đã ứng tuyển vào công ty.

                    Trân trọng,
                    Online Recruitment
                    """.formatted(candidateName, companyName, jobTitle);

            emailService.sendEmail(
                    candidateEmail,
                    "Chúc mừng! Hồ sơ ứng tuyển của bạn đã được chấp nhận",
                    candidateContent
            );

            String recruiterContent = """
                    Xin chào nhà tuyển dụng,

                    Bạn đã duyệt thành công hồ sơ ứng tuyển.

                    Thông tin Candidate:

                    Họ tên: %s
                    Email: %s
                    Công ty: %s
                    Vị trí: %s

                    Trân trọng,
                    Online Recruitment
                    """.formatted(
                    candidateName,
                    candidateEmail,
                    companyName,
                    jobTitle
            );

            String cvPath = data.get("cvFilePath") != null
                    ? data.get("cvFilePath").toString()
                    : null;

            File cvFile = cvFileStorageService.resolveCvFile(cvPath);

            if (cvFile != null && cvFile.isFile()) {

                String fileName = "CV_Candidate_" + applicationId
                        + cvFile.getName().substring(
                        cvFile.getName().lastIndexOf(".")
                );

                emailService.sendEmailWithAttachment(
                        recruiterEmail,
                        "Hồ sơ ứng viên đã được duyệt - " + candidateName,
                        recruiterContent,
                        cvFile,
                        fileName
                );

            } else {
                emailService.sendEmail(
                        recruiterEmail,
                        "Hồ sơ ứng viên đã được duyệt - " + candidateName,
                        recruiterContent
                                + "\nLưu ý: Đơn chưa có file CV vật lý để đính kèm."
                );
            }

            result.put(
                    "successMessage",
                    "Đã duyệt đơn và gửi email thông báo."
            );

            result.put("redirect", "/recruiter/applications?status=ACCEPTED");

        } catch (Exception e) {
            e.printStackTrace();

            result.put(
                    "errorMessage",
                    "Đơn đã được duyệt nhưng có lỗi khi gửi email: "
                            + e.getMessage()
            );

            result.put("redirect", "/recruiter/applications?status=ACCEPTED");
        }

        return result;
    }

    // =========================================
    // TỪ CHỐI ĐƠN ỨNG TUYỂN
    // =========================================

    public Map<String, String> rejectApplication(
            Long applicationId,
            String username) {

        Map<String, String> result = new HashMap<>();

        Long recruiterId =
                applicationRepository.findRecruiterIdByLoginName(username);

        if (recruiterId == null) {
            result.put("redirect", "/login");
            return result;
        }

        int updated =
                applicationRepository.rejectApplication(
                        applicationId, recruiterId);

        if (updated == 0) {
            result.put(
                    "errorMessage",
                    "Không tìm thấy đơn chờ duyệt hoặc bạn không có quyền thao tác."
            );

            result.put("redirect", "/recruiter/applications?status=SUBMITTED");
            return result;
        }

        try {
            List<Map<String, Object>> rows =
                    applicationRepository.findRejectionEmailData(
                            applicationId, recruiterId);

            if (rows.isEmpty()) {
                throw new IllegalStateException(
                        "Không tìm thấy thông tin ứng viên."
                );
            }

            Map<String, Object> data = rows.get(0);

            String candidateName = String.valueOf(data.get("candidateName"));
            String candidateEmail = String.valueOf(data.get("candidateEmail"));
            String companyName = String.valueOf(data.get("companyName"));
            String jobTitle = String.valueOf(data.get("jobTitle"));

            String subject =
                    "Thông báo kết quả ứng tuyển - " + companyName;

            String content = """
                    Xin chào %s,

                    Cảm ơn bạn đã quan tâm và ứng tuyển vào công ty %s.

                    Sau khi xem xét hồ sơ, chúng tôi rất tiếc phải thông báo
                    rằng hồ sơ ứng tuyển của bạn cho vị trí %s
                    hiện chưa phù hợp với yêu cầu tuyển dụng.

                    Trạng thái hồ sơ: KHÔNG ĐƯỢC CHẤP NHẬN.

                    Chúng tôi chân thành cảm ơn sự quan tâm của bạn.
                    Mong rằng bạn sẽ tiếp tục theo dõi và ứng tuyển
                    những vị trí phù hợp khác trong tương lai.

                    Chúc bạn sớm tìm được công việc mong muốn!

                    Trân trọng,
                    Đội ngũ tuyển dụng %s
                    """.formatted(
                    candidateName,
                    companyName,
                    jobTitle,
                    companyName
            );

            emailService.sendEmail(
                    candidateEmail,
                    subject,
                    content
            );

            result.put(
                    "successMessage",
                    "Đã từ chối đơn và gửi email thông báo cho ứng viên."
            );

            result.put("redirect", "/recruiter/applications?status=REJECTED");

        } catch (Exception e) {
            e.printStackTrace();

            result.put(
                    "errorMessage",
                    "Đơn đã bị từ chối nhưng gửi email thất bại: "
                            + e.getMessage()
            );

            result.put("redirect", "/recruiter/applications?status=REJECTED");
        }

        return result;
    }

    // =========================================
    // HÀM HỖ TRỢ
    // =========================================

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String toStringOrNull(Object value) {
        return value instanceof String string ? string : null;
    }
}