package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.JobApplication;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.JobApplicationRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/recruiter/applications")
public class RecruiterApplicationController {

    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "SUBMITTED",
            "REVIEWING",
            "INTERVIEW",
            "REJECTED",
            "HIRED"
    );

    public RecruiterApplicationController(
            JobApplicationRepository applicationRepository,
            UserRepository userRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lấy ID của recruiter đang đăng nhập.
     * Giả định username đăng nhập chính là email của User.
     */
    private Long getRecruiterId(UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Vui lòng đăng nhập."
            );
        }

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đang đăng nhập."
                ));

        return user.getId();
    }

    /**
     * Danh sách hồ sơ ứng tuyển vào các tin của recruiter.
     */
    @GetMapping
    public String listApplications(
            @AuthenticationPrincipal UserDetails principal,
            Model model
    ) {
        Long recruiterId = getRecruiterId(principal);

        List<JobApplication> applications =
                applicationRepository
                        .findByJob_RecruiterIdOrderByAppliedAtDesc(recruiterId);

        model.addAttribute("applications", applications);

        return "Job/recruiter-applications";
    }

    /**
     * Chi tiết hồ sơ ứng tuyển.
     */
    @GetMapping("/{id}")
    public String applicationDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails principal,
            Model model
    ) {
        Long recruiterId = getRecruiterId(principal);

        JobApplication application = applicationRepository
                .findDetailByIdAndRecruiterId(id, recruiterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ hoặc bạn không có quyền truy cập."
                ));

        model.addAttribute(
                "applicationId",
                application.getId()
        );

        model.addAttribute(
                "candidateName",
                application.getCandidate().getUser().getFullName()
        );

        model.addAttribute(
                "candidateEmail",
                application.getCandidate().getUser().getEmail()
        );

        model.addAttribute(
                "jobTitle",
                application.getJob().getTitle()
        );

        model.addAttribute(
                "appliedAt",
                application.getAppliedAt()
        );

        model.addAttribute(
                "applicationStatus",
                application.getStatus()
        );

        model.addAttribute(
                "cvFilePath",
                application.getCvFilePath()
        );

        return "Job/recruiter-application-detail";
    }

    /**
     * Cập nhật trạng thái hồ sơ.
     * Chỉ cập nhật hồ sơ thuộc tin tuyển dụng của recruiter hiện tại.
     */
    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes
    ) {
        Long recruiterId = getRecruiterId(principal);

        String newStatus = status == null
                ? ""
                : status.trim().toUpperCase();

        if (!ALLOWED_STATUSES.contains(newStatus)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Trạng thái hồ sơ không hợp lệ."
            );

            return "redirect:/recruiter/applications/" + id;
        }

        JobApplication application = applicationRepository
                .findByIdAndJob_RecruiterId(id, recruiterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ hoặc bạn không có quyền cập nhật."
                ));

        application.setStatus(newStatus);

        // Lưu trạng thái xuống database.
        applicationRepository.saveAndFlush(application);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Cập nhật trạng thái hồ sơ thành công! Trạng thái mới: "
                        + newStatus
        );

        // Chỉ chuyển về Jobs sau khi lệnh lưu hoàn thành.
        return "redirect:/recruiter/jobs";
    }

    /**
     * Tải CV của ứng viên.
     */
    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> downloadCv(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long recruiterId = getRecruiterId(principal);

        JobApplication application = applicationRepository
                .findByIdAndJob_RecruiterId(id, recruiterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy hồ sơ hoặc bạn không có quyền tải CV."
                ));

        String cvFilePath = application.getCvFilePath();

        if (cvFilePath == null || cvFilePath.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Hồ sơ này chưa có file CV."
            );
        }

        try {
            Path filePath = Paths.get(cvFilePath)
                    .toAbsolutePath()
                    .normalize();

            if (!Files.isRegularFile(filePath)
                    || !Files.isReadable(filePath)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy file CV trên máy chủ."
                );
            }

            Resource resource = new FileSystemResource(filePath);

            String contentType = Files.probeContentType(filePath);

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String extension = getSafeExtension(filePath);
            String downloadName = "CV-" + id + extension;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadName + "\""
                    )
                    .body(resource);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tải file CV.",
                    e
            );
        }
    }

    /**
     * Lấy phần mở rộng an toàn của file.
     */
    private String getSafeExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0) {
            return ".bin";
        }

        String extension = fileName.substring(dotIndex);

        if (!extension.matches("\\.[a-zA-Z0-9]{1,10}")) {
            return ".bin";
        }

        return extension;
    }
}
