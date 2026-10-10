package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Job2;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.JobRepository;
import OnlineRecruitment.OnlineRecruitment.service.CandidateApplicationService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/candidate-job")
@RequiredArgsConstructor
public class CandidateJobController {

    private final CandidateApplicationService candidateApplicationService;
    private final JobRepository jobRepository;

    /**
     * Hiển thị trang ứng tuyển.
     * URL: /candidate-job?jobId=1
     */
    @GetMapping
    public String showApplicationPage(
            @RequestParam("jobId") Long jobId,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Job2 job = jobRepository.findById(jobId).orElse(null);

        if (job == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không tìm thấy tin tuyển dụng."
            );
            return "redirect:/jobs";
        }

        model.addAttribute("jobId", job.getId());
        model.addAttribute("jobTitle", job.getTitle());
        model.addAttribute("job", job);

        return "candidate-job";
    }

    /**
     * Xử lý nộp hồ sơ bằng một trong hai cách:
     * 1. Nhập thông tin CV thủ công.
     * 2. Tải file CV lên.
     *
     * URL: POST /candidate-job/apply
     */
    @PostMapping("/apply")
    public String apply(
            @RequestParam("jobId") Long jobId,

            @RequestParam(value = "fullName", required = false)
            String fullName,

            @RequestParam(value = "formEmail", required = false)
            String formEmail,

            @RequestParam(value = "phone", required = false)
            String phone,

            @RequestParam(value = "position", required = false)
            String position,

            @RequestParam(value = "address", required = false)
            String address,

            @RequestParam(value = "summary", required = false)
            String summary,

            @RequestParam(value = "skills", required = false)
            String skills,

            @RequestParam(value = "template", required = false)
            String template,

            @RequestParam(value = "experienceTitles", required = false)
            String[] experienceTitles,

            @RequestParam(value = "experiencePeriods", required = false)
            String[] experiencePeriods,

            @RequestParam(value = "experienceDescriptions", required = false)
            String[] experienceDescriptions,

            @RequestParam(value = "educationSchools", required = false)
            String[] educationSchools,

            @RequestParam(value = "educationPeriods", required = false)
            String[] educationPeriods,

            @RequestParam(value = "educationDescriptions", required = false)
            String[] educationDescriptions,

            @RequestParam(value = "cvFile", required = false)
            MultipartFile cvFile,

            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            candidateApplicationService.apply(
                    authentication.getName(),
                    jobId,
                    fullName,
                    formEmail,
                    phone,
                    position,
                    address,
                    summary,
                    skills,
                    template,
                    experienceTitles,
                    experiencePeriods,
                    experienceDescriptions,
                    educationSchools,
                    educationPeriods,
                    educationDescriptions,
                    cvFile
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Nộp hồ sơ thành công!"
            );

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể nộp hồ sơ. Vui lòng kiểm tra dữ liệu và thử lại."
            );
        }

        return "redirect:/candidate-job?jobId=" + jobId;
    }
}
