package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.service.CvApplicationPreviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import OnlineRecruitment.OnlineRecruitment.service.EmailService;
import OnlineRecruitment.OnlineRecruitment.service.CvFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/recruiter/applications")
public class RecruiterApplicationController {

    private final JdbcTemplate jdbcTemplate;
    private final OnlineRecruitment.OnlineRecruitment.service.SkillMatchingService skillMatchingService;
    private final CvApplicationPreviewService cvApplicationPreviewService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private EmailService emailService;

    @Autowired
    private CvFileStorageService cvFileStorageService;
    public RecruiterApplicationController(JdbcTemplate jdbcTemplate, OnlineRecruitment.OnlineRecruitment.service.SkillMatchingService skillMatchingService) {
        this(jdbcTemplate, skillMatchingService, null);
    }

    @Autowired
    public RecruiterApplicationController(
            JdbcTemplate jdbcTemplate,
            OnlineRecruitment.OnlineRecruitment.service.SkillMatchingService skillMatchingService,
            CvApplicationPreviewService cvApplicationPreviewService) {
        this.jdbcTemplate = jdbcTemplate;
        this.skillMatchingService = skillMatchingService;
        this.cvApplicationPreviewService = cvApplicationPreviewService;
    }

    // =========================================
    // DANH SÁCH ĐƠN ỨNG TUYỂN
    // =========================================sql.append(" ORDER BY a.applied_at DESC");
    @GetMapping
    public String applications(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "SUBMITTED") String status,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            Model model) {


        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();

        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT id
                FROM dbo.users
                WHERE username = ? OR email = ?
                """, username, username);

        if (users.isEmpty()) {
            model.addAttribute("applications", List.of());
            model.addAttribute("jobs", List.of());
            addFilters(model, keyword, status, jobId, sort);
            return "recruiter/applications";
        }

        Object recruiterId = users.get(0).get("id");

        // Chỉ lấy công việc thuộc công ty của recruiter đang đăng nhập.
        List<Map<String, Object>> jobs = jdbcTemplate.queryForList("""
                SELECT j.id, j.title
                FROM dbo.jobs j
                JOIN dbo.companies c ON c.id = j.company_id
                WHERE c.recruiter_id = ?
                ORDER BY j.title
                """, recruiterId);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    a.id AS id,
                    a.candidate_id AS candidateId,
                    a.job_id AS jobId,
                    a.cv_id AS cvId,
                    a.cv_template_version_id AS cvTemplateVersionId,
                    a.cv_snapshot_json AS cvSnapshotJson,
                    a.cv_file_path_snapshot AS cvFilePathSnapshot,
                    u.full_name AS candidateName,
                    u.email AS email,
                    u.phone AS phone,
                    j.title AS jobTitle,
                    c.company_name AS companyName,
                    j.location AS location,
                    a.applied_at AS appliedAt,
                    a.status AS status,
                    cv.title AS cvTitle,
                    cv.professional_title AS cvProfessionalTitle,
                    cv.summary AS cvSummary
                FROM dbo.applications a
                JOIN dbo.candidates ca ON ca.id = a.candidate_id
                JOIN dbo.users u ON u.id = ca.user_id
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.companies c ON c.id = j.company_id
                LEFT JOIN dbo.cv cv ON cv.id = a.cv_id
                WHERE c.recruiter_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(recruiterId);

        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }

        if (jobId != null) {
            sql.append(" AND j.id = ?");
            params.add(jobId);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                         u.full_name LIKE ?
                         OR u.email LIKE ?
                         OR j.title LIKE ?
                     )
                    """);

            String search = "%" + keyword.trim() + "%";
            params.add(search);
            params.add(search);
            params.add(search);
        }


        if ("oldest".equals(sort)) {
            sql.append(" ORDER BY a.applied_at ASC");
        } else {
            sql.append(" ORDER BY a.applied_at DESC");
        }


        List<Map<String, Object>> applications =
                jdbcTemplate.queryForList(sql.toString(), params.toArray());
        skillMatchingService.attachScores(applications);

        model.addAttribute("applications", applications);
        model.addAttribute("jobs", jobs);
        addFilters(model, keyword, status, jobId, sort);

        return "recruiter/applications";
    }

    // =========================================
    // XEM CV THEO ĐƠN ỨNG TUYỂN
    // =========================================
    @GetMapping("/{applicationId}/cv")
    public String viewApplicationCv(
            @PathVariable Long applicationId,
            Model model) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();

        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT id
                FROM dbo.users
                WHERE username = ? OR email = ?
                """, username, username);

        if (users.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy tài khoản nhà tuyển dụng."
            );
        }

        Object recruiterId = users.get(0).get("id");

        // Chỉ xem đơn thuộc công ty của recruiter đang đăng nhập.
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT
                    a.id AS applicationId,
                    a.candidate_id AS candidateId,
                    a.job_id AS jobId,
                    a.cv_id AS cvId,
                    a.cv_template_version_id AS cvTemplateVersionId,
                    a.cv_snapshot_json AS cvSnapshotJson,
                    u.full_name AS candidateName,
                    a.cv_file_path_snapshot AS cvFilePathSnapshot,
                    u.email AS email,
                    u.phone AS phone,
                    j.title AS jobTitle,
                    c.company_name AS companyName,
                    cv.title AS cvTitle,
                    cv.professional_title AS professionalTitle,
                    cv.summary AS summary
                FROM dbo.applications a
                JOIN dbo.candidates ca ON ca.id = a.candidate_id
                JOIN dbo.users u ON u.id = ca.user_id
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.companies c ON c.id = j.company_id
                LEFT JOIN dbo.cv cv ON cv.id = a.cv_id
                WHERE a.id = ?
                  AND c.recruiter_id = ?
                """, applicationId, recruiterId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy đơn ứng tuyển hoặc bạn không có quyền xem."
            );
        }

        // Sao chép dữ liệu từ kết quả SQL sang Map riêng.
        Map<String, Object> applicationData =
                new HashMap<>(rows.get(0));

        // Giải mã snapshot CV tại thời điểm ứng tuyển để hiển thị chính xác ngay cả khi ứng viên sửa CV sau đó
        Map<String, Object> snapshotMap = Map.of();
        Object rawSnapshot = applicationData.get("cvSnapshotJson");
        if (rawSnapshot instanceof String json && !json.isBlank()) {
            try {
                snapshotMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                if (snapshotMap.get("cv") instanceof Map<?, ?> snapCv) {
                    if (snapCv.get("title") != null) applicationData.put("cvTitle", snapCv.get("title"));
                    if (snapCv.get("professionalTitle") != null) applicationData.put("professionalTitle", snapCv.get("professionalTitle"));
                    if (snapCv.get("summary") != null) applicationData.put("summary", snapCv.get("summary"));
                }
            } catch (Exception ignored) {
            }
        }
        model.addAttribute("snapshotData", snapshotMap);

        if (cvApplicationPreviewService != null) {
            Long candId = applicationData.get("candidateId") instanceof Number n ? n.longValue() : null;
            Long cvId = applicationData.get("cvId") instanceof Number n ? n.longValue() : null;
            Long tvId = applicationData.get("cvTemplateVersionId") instanceof Number n ? n.longValue() : null;
            String filePathSnap = applicationData.get("cvFilePathSnapshot") instanceof String s ? s : null;
            String fbTitle = applicationData.get("cvTitle") instanceof String s ? s : null;
            String fbProf = applicationData.get("professionalTitle") instanceof String s ? s : null;
            String fbSum = applicationData.get("summary") instanceof String s ? s : null;

            model.addAllAttributes(cvApplicationPreviewService.buildPreviewModel(
                    candId, cvId, tvId, filePathSnap, snapshotMap, fbTitle, fbProf, fbSum));
            model.addAttribute("avatarEndpoint", "/recruiter/applications/" + applicationId + "/cv-avatar");
            model.addAttribute("fileDownloadUrl", "/recruiter/applications/" + applicationId + "/cv-file");
            model.addAttribute("fileViewUrl", "/recruiter/applications/" + applicationId + "/cv-file?inline=true");
        }

        // Dùng tên applicationData để tránh nhầm với các biến
        // ngầm định của môi trường Thymeleaf.
        model.addAttribute("applicationData", applicationData);

        return "recruiter/cv-preview";
    }

    // =========================================
    // DỮ LIỆU BỘ LỌC
    // =========================================

    private void addFilters(
            Model model,
            String keyword,
            String status,
            Long jobId,
            String sort) {

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("jobId", jobId);
        model.addAttribute("sort", sort);
    }




    // Duyệt đơn ứng tuyển

    @PostMapping("/{applicationId}/approve")
    public String approveApplication(
            @PathVariable Long applicationId,
            RedirectAttributes redirectAttributes) {

        String username = getCurrentUsername();

        if (username == null) {
            return "redirect:/login";
        }

        // 1. Lấy ID Recruiter
        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
            SELECT id
            FROM dbo.users
            WHERE username = ? OR email = ?
            """, username, username);

        if (users.isEmpty()) {
            return "redirect:/login";
        }

        Object recruiterId = users.get(0).get("id");

        // 2. Cập nhật trạng thái đơn thành ACCEPTED
        int updated = jdbcTemplate.update("""
            UPDATE a
            SET a.status = 'ACCEPTED'
            FROM dbo.applications a
            JOIN dbo.jobs j ON j.id = a.job_id
            JOIN dbo.companies c ON c.id = j.company_id
            WHERE a.id = ?
              AND c.recruiter_id = ?
              AND a.status = 'SUBMITTED'
            """, applicationId, recruiterId);

        if (updated == 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không tìm thấy đơn chờ duyệt hoặc bạn không có quyền thao tác."
            );
            return "redirect:/recruiter/applications?status=SUBMITTED";
        }

        try {
            // 3. Lấy thông tin Candidate, Recruiter, công ty và CV
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT
                    candidate.full_name AS candidateName,
                    candidate.email AS candidateEmail,
                    recruiter.email AS recruiterEmail,
                    company.company_name AS companyName,
                    j.title AS jobTitle,
                    a.cv_file_path_snapshot AS cvFilePath
                FROM dbo.applications a
                JOIN dbo.candidates ca ON ca.id = a.candidate_id
                JOIN dbo.users candidate ON candidate.id = ca.user_id
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.companies company ON company.id = j.company_id
                JOIN dbo.users recruiter ON recruiter.id = company.recruiter_id
                WHERE a.id = ?
                  AND company.recruiter_id = ?
                """, applicationId, recruiterId);

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

            // 4. Gửi Gmail cho Candidate
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

            // 5. Gửi Gmail cho Recruiter kèm CV
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

            java.io.File cvFile =
                    cvFileStorageService.resolveCvFile(cvPath);

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
                        recruiterContent + "\nLưu ý: Đơn chưa có file CV vật lý để đính kèm."
                );
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã duyệt đơn và gửi email thông báo."
            );

        } catch (Exception e) {
            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Đơn đã được duyệt nhưng có lỗi khi gửi email: "
                            + e.getMessage()
            );
        }

        return "redirect:/recruiter/applications?status=ACCEPTED";
    }


    // Từ chối đơn ứng tuyển

    // Từ chối đơn ứng tuyển và gửi Gmail cho Candidate
    @PostMapping("/{applicationId}/reject")
    public String rejectApplication(
            @PathVariable Long applicationId,
            RedirectAttributes redirectAttributes) {

        String username = getCurrentUsername();

        if (username == null) {
            return "redirect:/login";
        }

        // 1. Lấy ID Recruiter
        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
            SELECT id
            FROM dbo.users
            WHERE username = ? OR email = ?
            """, username, username);

        if (users.isEmpty()) {
            return "redirect:/login";
        }

        Object recruiterId = users.get(0).get("id");

        // 2. Cập nhật trạng thái đơn thành REJECTED
        int updated = jdbcTemplate.update("""
            UPDATE a
            SET a.status = 'REJECTED'
            FROM dbo.applications a
            JOIN dbo.jobs j ON j.id = a.job_id
            JOIN dbo.companies c ON c.id = j.company_id
            WHERE a.id = ?
              AND c.recruiter_id = ?
              AND a.status = 'SUBMITTED'
            """, applicationId, recruiterId);

        if (updated == 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không tìm thấy đơn chờ duyệt hoặc bạn không có quyền thao tác."
            );

            return "redirect:/recruiter/applications?status=SUBMITTED";
        }

        try {
            // 3. Lấy thông tin Candidate và công ty
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT
                    candidate.full_name AS candidateName,
                    candidate.email AS candidateEmail,
                    company.company_name AS companyName,
                    j.title AS jobTitle
                FROM dbo.applications a
                JOIN dbo.candidates ca ON ca.id = a.candidate_id
                JOIN dbo.users candidate ON candidate.id = ca.user_id
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.companies company ON company.id = j.company_id
                WHERE a.id = ?
                  AND company.recruiter_id = ?
                """, applicationId, recruiterId);

            if (rows.isEmpty()) {
                throw new IllegalStateException(
                        "Không tìm thấy thông tin ứng viên."
                );
            }

            Map<String, Object> data = rows.get(0);

            String candidateName =
                    String.valueOf(data.get("candidateName"));

            String candidateEmail =
                    String.valueOf(data.get("candidateEmail"));

            String companyName =
                    String.valueOf(data.get("companyName"));

            String jobTitle =
                    String.valueOf(data.get("jobTitle"));

            // 4. Nội dung Gmail thông báo từ chối
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

            // 5. Gửi Gmail cho Candidate
            emailService.sendEmail(
                    candidateEmail,
                    subject,
                    content
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã từ chối đơn và gửi email thông báo cho ứng viên."
            );

        } catch (Exception e) {
            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Đơn đã bị từ chối nhưng gửi email thất bại: "
                            + e.getMessage()
            );
        }

        return "redirect:/recruiter/applications?status=REJECTED";
    }


    // Lấy email hoặc username của tài khoản đang đăng nhập
    private String getCurrentUsername() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        return auth.getName();
    }

}
