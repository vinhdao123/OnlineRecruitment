package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.dto.RecruiterJobForm;
import OnlineRecruitment.OnlineRecruitment.entity.EmploymentType;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.JobService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter/jobs")
@RequiredArgsConstructor
public class RecruiterJobController {

    private final JobService jobService;
    private final UserRepository userRepository;

    // Số tin hiển thị trên mỗi trang
    private static final int PAGE_SIZE = 9;

    // =====================================================
    // HIỂN THỊ TẤT CẢ TIN TUYỂN DỤNG TỪ DATABASE
    // =====================================================

    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        User recruiter = getLoggedInRecruiter(authentication);

        // Lấy tất cả tin khớp bộ lọc (tiêu đề, địa điểm, trạng thái)
        List<Job> allJobs = jobService.findAllJobsForManagement(
                keyword,
                location,
                status
        );

        // Tính số trang (luôn có ít nhất 1 trang)
        int totalFound = allJobs.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalFound / (double) PAGE_SIZE));

        // Giữ số trang hiện tại trong khoảng hợp lệ
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);

        // Cắt đúng các tin của trang hiện tại
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, totalFound);
        List<Job> pageContent = allJobs.subList(from, to);

        // Đóng gói thành Page để giao diện dùng
        Page<Job> jobPage = new PageImpl<>(
                pageContent,
                PageRequest.of(currentPage, PAGE_SIZE),
                totalFound
        );

        // Chỉ hiện tối đa 5 nút số trang quanh trang hiện tại
        int pageStart = Math.max(0, currentPage - 2);
        int pageEnd = Math.min(totalPages - 1, currentPage + 2);

        model.addAttribute("jobs", pageContent);
        model.addAttribute("jobPage", jobPage);
        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);

        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);
        model.addAttribute("status", status);
        model.addAttribute("currentRecruiter", recruiter);

        // Thống kê trên toàn bộ database.
        model.addAttribute("totalJobs", jobService.countAllJobs());

        model.addAttribute(
                "draftJobs",
                jobService.countAllJobsByStatus(JobStatus.DRAFT)
        );

        model.addAttribute(
                "pendingJobs",
                jobService.countAllJobsByStatus(JobStatus.PENDING)
        );

        model.addAttribute(
                "activeJobs",
                jobService.countAllJobsByStatus(JobStatus.ACTIVE)
        );

        model.addAttribute(
                "rejectedJobs",
                jobService.countAllJobsByStatus(JobStatus.REJECTED)
        );

        return "recruiter/jobs";
    }

    // =====================================================
    // HIỂN THỊ FORM TẠO TIN
    // =====================================================

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("jobForm", new RecruiterJobForm());
        model.addAttribute("categories", jobService.getActiveCategories());
        model.addAttribute("employmentTypes", EmploymentType.values());
        model.addAttribute("pageTitle", "Tạo tin tuyển dụng");

        return "recruiter/job-form";
    }

    // =====================================================
    // HIỂN THỊ FORM SỬA TIN
    // =====================================================

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User recruiter = getLoggedInRecruiter(authentication);

            // Chỉ chủ sở hữu của tin mới được sửa.
            Job job = jobService.getJobForRecruiter(
                    id,
                    recruiter.getId()
            );

            // Tin đã đóng thì phải mở lại trước; các trạng thái khác đều được sửa
            // (sửa tin chờ duyệt/đang hiển thị sẽ được gửi lại cho Admin duyệt)
            if (job.getStatus() == JobStatus.CLOSED) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Tin đã đóng. Hãy mở lại tin trước khi sửa."
                );

                return "redirect:/recruiter/jobs";
            }

            RecruiterJobForm form = new RecruiterJobForm();

            form.setJobId(job.getId());
            form.setCategoryId(job.getCategory().getId());
            form.setTitle(job.getTitle());
            form.setDescription(job.getDescription());
            form.setRequirements(job.getRequirements());
            form.setBenefits(job.getBenefits());
            form.setLocation(job.getLocation());
            form.setSalaryMin(job.getSalaryMin());
            form.setSalaryMax(job.getSalaryMax());
            form.setSalaryCurrency(job.getSalaryCurrency());
            form.setExperience(job.getExperience());
            form.setLevel(job.getLevel());
            form.setEmploymentType(job.getEmploymentType());
            form.setQuantity(job.getQuantity());
            form.setDeadline(job.getDeadline());

            // Đưa URL logo hiện tại của công ty lên form
            if (job.getCompany() != null) {
                form.setLogoUrl(job.getCompany().getLogoUrl());
            }

            model.addAttribute("jobForm", form);
            model.addAttribute(
                    "categories",
                    jobService.getActiveCategories()
            );
            model.addAttribute(
                    "employmentTypes",
                    EmploymentType.values()
            );
            model.addAttribute(
                    "pageTitle",
                    "Chỉnh sửa tin tuyển dụng"
            );

            return "recruiter/job-form";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/recruiter/jobs";
        }
    }

    // =====================================================
    // LƯU BẢN NHÁP HOẶC GỬI TIN CHO ADMIN DUYỆT
    // =====================================================

    @PostMapping("/save")
    public String saveJob(
            @ModelAttribute("jobForm") RecruiterJobForm form,
            @RequestParam(defaultValue = "draft") String action,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User recruiter = getLoggedInRecruiter(authentication);

            boolean submitForReview =
                    "submit".equalsIgnoreCase(action.trim());

            jobService.saveRecruiterJob(
                    recruiter,
                    form,
                    submitForReview
            );

            if (submitForReview) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Nộp tin tuyển dụng thành công! "
                                + "Tin đang chờ quản trị viên phê duyệt."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Lưu bản nháp thành công!"
                );
            }

            return "redirect:/recruiter/jobs";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            if (form.getJobId() != null) {
                return "redirect:/recruiter/jobs/"
                        + form.getJobId() + "/edit";
            }

            return "redirect:/recruiter/jobs/new";
        }
    }

    // =====================================================
    // GỬI TIN NHÁP HOẶC TIN BỊ TỪ CHỐI ĐỂ ADMIN DUYỆT
    // =====================================================

    @PostMapping("/{id}/submit")
    public String submitJob(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User recruiter = getLoggedInRecruiter(authentication);

            jobService.submitExistingJob(id, recruiter);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Nộp tin tuyển dụng thành công! "
                            + "Tin đang chờ quản trị viên phê duyệt."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/recruiter/jobs";
    }

    // =====================================================
    // ĐÓNG TIN TUYỂN DỤNG
    // =====================================================

    // POST /recruiter/jobs/{id}/reopen : mở lại tin đã đóng
    @PostMapping("/{id}/reopen")
    public String reopenJob(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Lấy nhà tuyển dụng đang đăng nhập
            User recruiter = getLoggedInRecruiter(authentication);

            // Gọi service mở lại tin
            jobService.reopenRecruiterJob(id, recruiter);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Mở lại tin tuyển dụng thành công!"
            );
        } catch (IllegalArgumentException e) {
            // Có lỗi nghiệp vụ thì báo ra màn hình
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/recruiter/jobs";
    }

    @PostMapping("/{id}/close")
    public String closeJob(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User recruiter = getLoggedInRecruiter(authentication);

            jobService.closeRecruiterJob(id, recruiter);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đóng tin tuyển dụng thành công!"
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/recruiter/jobs";
    }

    // =====================================================
    // LẤY TÀI KHOẢN ĐANG ĐĂNG NHẬP
    // =====================================================

    private User getLoggedInRecruiter(
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new IllegalArgumentException(
                    "Bạn cần đăng nhập để thực hiện thao tác này."
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài khoản đang đăng nhập."
                ));
    }
}