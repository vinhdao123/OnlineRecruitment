package OnlineRecruitment.OnlineRecruitment.controller.JobController;

import OnlineRecruitment.OnlineRecruitment.dto.JobForm;
import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Skill;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.serviceJob.JobService;
import OnlineRecruitment.OnlineRecruitment.util.SalaryRanges;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter/jobs")
public class RecruiterJobController {

    private final JobService jobService;
    private final UserRepository userRepository;

    public RecruiterJobController(
            JobService jobService,
            UserRepository userRepository) {
        this.jobService = jobService;
        this.userRepository = userRepository;
    }

    // ===== DỮ LIỆU CHO FORM =====

    @ModelAttribute("levels")
    public List<String> levels() {
        return List.of(
                "Intern",
                "Fresher",
                "Junior",
                "Middle",
                "Senior",
                "Team Leader",
                "Manager"
        );
    }

    @ModelAttribute("provinces")
    public List<String> provinces() {
        return List.of(
                "Hanoi",
                "Ho Chi Minh",
                "Da Nang",
                "Hai Phong",
                "Can Tho",
                "Bac Ninh",
                "Bac Giang",
                "Hue",
                "Nha Trang",
                "Remote"
        );
    }

    @ModelAttribute("categories")
    public List<JobCategory> categories() {
        return jobService.getCategories();
    }

    @ModelAttribute("skillOptions")
    public List<Skill> skillOptions() {
        return jobService.getSkills();
    }

    @ModelAttribute("salaryOptions")
    public List<String> salaryOptions() {
        return SalaryRanges.labels();
    }

    @ModelAttribute("experienceOptions")
    public List<String> experienceOptions() {
        return List.of(
                "No experience",
                "Less than 1 year",
                "1 year",
                "2 years",
                "3 years",
                "4 years",
                "5+ years"
        );
    }

    // ===== LẤY ID TÀI KHOẢN ĐANG ĐĂNG NHẬP =====

    private Long getRecruiterId(UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn cần đăng nhập để tiếp tục."
            );
        }

        // CustomUserDetailsService sử dụng email làm username.
        String email = principal.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Không tìm thấy tài khoản đang đăng nhập."
                        )
                );

        return user.getId();
    }

    // ===== READ: DANH SÁCH TIN CỦA TÔI =====

    @GetMapping
    public String myJobs(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            @AuthenticationPrincipal UserDetails principal) {

        Long recruiterId = getRecruiterId(principal);

        model.addAttribute(
                "jobPage",
                jobService.findMine(
                        recruiterId,
                        keyword,
                        status,
                        page
                )
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "Job/recruiter-jobs";
    }

    // ===== CREATE: HIỂN THỊ FORM =====

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("jobForm", new JobForm());

        prepare(
                model,
                "Tạo tin tuyển dụng mới",
                "/recruiter/jobs/new",
                false
        );

        return "Job/job-form";
    }

    // ===== CREATE: LƯU TIN =====

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("jobForm") JobForm form,
            BindingResult result,
            Model model,
            RedirectAttributes ra,
            @AuthenticationPrincipal UserDetails principal) {

        Long recruiterId = getRecruiterId(principal);

        if (result.hasErrors()) {
            prepare(
                    model,
                    "Tạo tin tuyển dụng mới",
                    "/recruiter/jobs/new",
                    false
            );

            return "Job/job-form";
        }

        jobService.create(form, recruiterId);

        ra.addFlashAttribute(
                "success",
                "Tin đã được gửi cho Admin duyệt. "
                        + "Tin sẽ hiển thị công khai sau khi được phê duyệt."
        );

        return "redirect:/recruiter/jobs";
    }

    // ===== UPDATE: HIỂN THỊ FORM SỬA =====

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model,
            @AuthenticationPrincipal UserDetails principal) {

        Long recruiterId = getRecruiterId(principal);

        model.addAttribute(
                "jobForm",
                jobService.getForm(id, recruiterId)
        );

        prepare(
                model,
                "Cập nhật tin tuyển dụng",
                "/recruiter/jobs/" + id + "/edit",
                true
        );

        return "Job/job-form";
    }

    // ===== UPDATE: LƯU THAY ĐỔI =====

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("jobForm") JobForm form,
            BindingResult result,
            Model model,
            RedirectAttributes ra,
            @AuthenticationPrincipal UserDetails principal) {

        Long recruiterId = getRecruiterId(principal);

        if (result.hasErrors()) {
            prepare(
                    model,
                    "Cập nhật tin tuyển dụng",
                    "/recruiter/jobs/" + id + "/edit",
                    true
            );

            return "Job/job-form";
        }

        jobService.update(id, form, recruiterId);

        ra.addFlashAttribute(
                "success",
                "Đã cập nhật tin. Tin được gửi Admin duyệt lại "
                        + "trước khi hiển thị công khai."
        );

        return "redirect:/recruiter/jobs";
    }

    // ===== CLOSE: ĐÓNG TIN =====

    @PostMapping("/{id}/close")
    public String close(
            @PathVariable Long id,
            RedirectAttributes ra,
            @AuthenticationPrincipal UserDetails principal) {

        Long recruiterId = getRecruiterId(principal);

        jobService.close(id, recruiterId);

        ra.addFlashAttribute(
                "success",
                "Đã đóng tin tuyển dụng."
        );

        return "redirect:/recruiter/jobs";
    }

    // ===== DELETE: XÓA TIN =====

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes ra,
            @AuthenticationPrincipal UserDetails principal) {

        Long recruiterId = getRecruiterId(principal);

        try {
            jobService.delete(id, recruiterId);

            ra.addFlashAttribute(
                    "success",
                    "Đã xóa tin tuyển dụng."
            );

        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute(
                    "error",
                    "Không thể xóa vì tin đã có dữ liệu liên quan "
                            + "(ví dụ hồ sơ ứng tuyển). "
                            + "Hãy dùng \"Đóng tin\" thay thế."
            );
        }

        return "redirect:/recruiter/jobs";
    }

    // ===== CHUẨN BỊ DỮ LIỆU CHO FORM =====

    private void prepare(
            Model model,
            String title,
            String action,
            boolean editMode) {

        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
        model.addAttribute("editMode", editMode);
    }
}