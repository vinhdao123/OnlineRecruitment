package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.RecruiterJobForm;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository; // Thêm import này

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobService {

    private static final double VND_TO_USD_RATE = 25000.0;

    private static final Pattern NO_EXP_PATTERN =
            Pattern.compile("\\bno\\b", Pattern.CASE_INSENSITIVE);

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;
    private final JobSkillService jobSkillService;
    private final ServicePackageService servicePackageService;
    private final UserPackageRepository userPackageRepository; // Thêm repo này để cập nhật usedJobs

    // Cập nhật Constructor inject thêm UserPackageRepository
    public JobService(
            JobRepository jobRepository,
            JobCategoryRepository jobCategoryRepository,
            CompanyRepository companyRepository,
            FileStorageService fileStorageService,
            JobSkillService jobSkillService,
            ServicePackageService servicePackageService,
            UserPackageRepository userPackageRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.companyRepository = companyRepository;
        this.fileStorageService = fileStorageService;
        this.jobSkillService = jobSkillService;
        this.servicePackageService = servicePackageService;
        this.userPackageRepository = userPackageRepository;
    }

    // =====================================================
    // GUEST
    // =====================================================

    @Transactional(readOnly = true)
    public List<Job> getNewestJobs() {
        return jobRepository.findActiveJobs();
    }

    @Transactional
    public void refreshExpiredJobs() {
        jobRepository.expireActiveJobs(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<String> getAllLocations() {
        return jobRepository.findDistinctByLocationIsNotNullOrderByLocationAsc()
                .stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(location -> !location.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobCategory> getAllCategories() {
        return jobCategoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<JobCategory> getActiveCategories() {
        return jobCategoryRepository.findAll()
                .stream()
                .filter(category -> category.getStatus() != null && "ACTIVE".equalsIgnoreCase(category.getStatus().trim()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Job> filterJobs(
            String keyword,
            String locationKeyword,
            List<String> locations,
            List<String> experiences,
            List<String> salaryRanges,
            List<String> levels,
            List<Long> categoryIds
    ) {
        List<Job> jobs = jobRepository.findActiveJobs();

        if (hasText(keyword)) {
            String keywordLower = keyword.trim().toLowerCase(Locale.ROOT);
            jobs = jobs.stream().filter(job -> job.getTitle() != null && job.getTitle().toLowerCase(Locale.ROOT).contains(keywordLower)).collect(Collectors.toList());
        }
        if (hasText(locationKeyword)) {
            String locationLower = locationKeyword.trim().toLowerCase(Locale.ROOT);
            jobs = jobs.stream().filter(job -> job.getLocation() != null && job.getLocation().toLowerCase(Locale.ROOT).contains(locationLower)).collect(Collectors.toList());
        }
        if (locations != null && !locations.isEmpty()) {
            jobs = jobs.stream().filter(job -> job.getLocation() != null && locations.stream().anyMatch(location -> job.getLocation().equalsIgnoreCase(location))).collect(Collectors.toList());
        }
        if (experiences != null && !experiences.isEmpty()) {
            jobs = jobs.stream().filter(job -> matchesExperience(job.getExperience(), experiences)).collect(Collectors.toList());
        }
        if (salaryRanges != null && !salaryRanges.isEmpty()) {
            jobs = jobs.stream().filter(job -> matchesSalary(job.getSalaryMin(), job.getSalaryMax(), job.getSalaryCurrency(), salaryRanges)).collect(Collectors.toList());
        }
        if (levels != null && !levels.isEmpty()) {
            jobs = jobs.stream().filter(job -> job.getLevel() != null && levels.stream().anyMatch(level -> job.getLevel().equalsIgnoreCase(level))).collect(Collectors.toList());
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            jobs = jobs.stream().filter(job -> job.getCategory() != null && categoryIds.contains(job.getCategory().getId())).collect(Collectors.toList());
        }

        jobs.sort(Comparator.comparing(Job::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return jobs;
    }

    private boolean matchesExperience(String experience, List<String> selectedRanges) {
        if (!hasText(experience)) return selectedRanges.contains("none");
        String value = experience.trim().toLowerCase(Locale.ROOT);
        if (value.contains("không") || NO_EXP_PATTERN.matcher(value).find() || value.contains("not required") || value.contains("no experience")) return selectedRanges.contains("none");
        List<Integer> numbers = extractNumbers(value);
        if (numbers.isEmpty()) return false;
        double jobMin = numbers.get(0);
        double jobMax = numbers.size() >= 2 ? numbers.get(1) : jobMin;
        if (jobMin > 5) jobMax = Double.MAX_VALUE;

        for (String range : selectedRanges) {
            double filterMin, filterMax;
            switch (range) {
                case "0-1": filterMin = 0; filterMax = 1; break;
                case "1-3": filterMin = 1; filterMax = 3; break;
                case "3-5": filterMin = 3; filterMax = 5; break;
                case "5+": filterMin = 5; filterMax = Double.MAX_VALUE; break;
                default: continue;
            }
            if (jobMin <= filterMax && jobMax >= filterMin) return true;
        }
        return false;
    }

    private boolean matchesSalary(BigDecimal salaryMin, BigDecimal salaryMax, String currency, List<String> selectedRanges) {
        if (salaryMin == null && salaryMax == null) return false;
        double jobMin = salaryMin != null ? salaryMin.doubleValue() : salaryMax.doubleValue();
        double jobMax = salaryMax != null ? salaryMax.doubleValue() : salaryMin.doubleValue();
        if ("VND".equalsIgnoreCase(currency) || jobMin > 100000 || jobMax > 100000) {
            jobMin /= VND_TO_USD_RATE;
            jobMax /= VND_TO_USD_RATE;
        }
        for (String range : selectedRanges) {
            double filterMin, filterMax;
            switch (range) {
                case "under-1000": filterMin = 0; filterMax = 1000; break;
                case "1000-2000": filterMin = 1000; filterMax = 2000; break;
                case "2000-3000": filterMin = 2000; filterMax = 3000; break;
                case "3000+": filterMin = 3000; filterMax = Double.MAX_VALUE; break;
                default: continue;
            }
            if (jobMin <= filterMax && jobMax >= filterMin) return true;
        }
        return false;
    }

    private List<Integer> extractNumbers(String text) {
        List<Integer> numbers = new ArrayList<>();
        for (String part : text.split("[^0-9]+")) {
            if (!part.isEmpty()) {
                try { numbers.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) {}
            }
        }
        return numbers;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // =====================================================
    // RECRUITER: LẤY TẤT CẢ TIN
    // =====================================================

    @Transactional(readOnly = true)
    public List<Job> findAllJobsForManagement(String keyword, String status) {
        return findAllJobsForManagement(keyword, null, status);
    }

    @Transactional(readOnly = true)
    public List<Job> findAllJobsForManagement(String keyword, String location, String status) {
        String normalizedKeyword = hasText(keyword) ? keyword.trim() : null;
        String normalizedLocation = hasText(location) ? location.trim() : null;
        JobStatus jobStatus = null;
        if (hasText(status)) {
            try { jobStatus = JobStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Trạng thái tin tuyển dụng không hợp lệ: " + status); }
        }
        return jobRepository.findAllForManagement(normalizedKeyword, normalizedLocation, jobStatus, Pageable.unpaged()).getContent();
    }

    @Transactional(readOnly = true)
    public Page<Job> findAllJobsForManagementPage(String keyword, String location, String status, int page) {
        int pageSize = 6;
        int safePage = Math.max(page, 0);
        List<Job> allJobs = findAllJobsForManagement(keyword, location, status);
        Pageable pageable = PageRequest.of(safePage, pageSize);
        int start = (int) pageable.getOffset();
        if (start >= allJobs.size()) return new PageImpl<>(List.of(), pageable, allJobs.size());
        int end = Math.min(start + pageable.getPageSize(), allJobs.size());
        return new PageImpl<>(allJobs.subList(start, end), pageable, allJobs.size());
    }

    @Transactional(readOnly = true)
    public long countAllJobs() { return jobRepository.count(); }

    @Transactional(readOnly = true)
    public long countAllJobsByStatus(JobStatus status) {
        if (status == null) return 0;
        return jobRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Job> findMyJobs(Long recruiterId, String keyword, String status) {
        if (recruiterId == null) throw new IllegalArgumentException("Recruiter ID không hợp lệ.");
        boolean hasKeyword = hasText(keyword);
        boolean hasStatus = hasText(status);
        JobStatus jobStatus = null;
        if (hasStatus) {
            try { jobStatus = JobStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Trạng thái công việc không hợp lệ: " + status); }
        }

        if (hasKeyword && hasStatus) return jobRepository.findByRecruiter_IdAndTitleContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(recruiterId, keyword.trim(), jobStatus);
        if (hasKeyword) return jobRepository.findByRecruiter_IdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(recruiterId, keyword.trim());
        if (hasStatus) return jobRepository.findByRecruiter_IdAndStatusOrderByCreatedAtDesc(recruiterId, jobStatus);
        return jobRepository.findByRecruiter_IdOrderByCreatedAtDesc(recruiterId);
    }

    @Transactional(readOnly = true)
    public long countMyJobs(Long recruiterId) {
        return recruiterId == null ? 0 : jobRepository.countByRecruiter_Id(recruiterId);
    }

    @Transactional(readOnly = true)
    public long countMyJobsByStatus(Long recruiterId, JobStatus status) {
        if (recruiterId == null || status == null) return 0;
        return jobRepository.countByRecruiter_IdAndStatus(recruiterId, status);
    }

    @Transactional(readOnly = true)
    public Job getJobForRecruiter(Long jobId, Long recruiterId) {
        return jobRepository.findByIdAndRecruiter_Id(jobId, recruiterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin tuyển dụng hoặc bạn không có quyền truy cập."));
    }

    // =====================================================
    // RECRUITER: TẠO / SỬA / LƯU NHÁP / GỬI DUYỆT (QUAN TRỌNG)
    // =====================================================

    public Job saveRecruiterJob(User recruiter, RecruiterJobForm form, boolean submitForReview) {
        if (recruiter == null || recruiter.getId() == null) throw new IllegalArgumentException("Không xác định được tài khoản Recruiter.");
        if (form == null) throw new IllegalArgumentException("Dữ liệu tin tuyển dụng không hợp lệ.");

        validateJobForm(form, submitForReview);

        Company company = companyRepository.findByRecruiterId(recruiter.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có thông tin công ty."));

        Job job;
        if (form.getJobId() != null) {
            job = getJobForRecruiter(form.getJobId(), recruiter.getId());
            if (job.getStatus() == JobStatus.CLOSED || job.getStatus() == JobStatus.EXPIRED) throw new IllegalArgumentException("Tin đã đóng hoặc quá hạn. Hãy chuyển về nháp trước khi sửa.");
        } else {
            job = new Job();
            job.setRecruiter(recruiter);
            job.setCompany(company);
        }

        if (hasText(form.getLogoUrl())) {
            String logoUrl = form.getLogoUrl().trim();
            if (!logoUrl.matches("(?i)^https?://\\S+$")) throw new IllegalArgumentException("URL logo phải bắt đầu bằng http:// hoặc https://");
            if (logoUrl.length() > 500) throw new IllegalArgumentException("URL logo không được vượt quá 500 ký tự.");
            company.setLogoUrl(logoUrl);
            companyRepository.save(company);
        }

        JobCategory category = jobCategoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Danh mục nghề nghiệp không tồn tại."));
        if (!"ACTIVE".equalsIgnoreCase(category.getStatus())) throw new IllegalArgumentException("Danh mục nghề nghiệp này hiện không hoạt động.");

        List<Long> selectedSkills = jobSkillService.validate(category.getId(), form.getSkillIds());

        job.setRecruiter(recruiter);
        job.setCompany(company);
        job.setCategory(category);
        job.setTitle(form.getTitle().trim());
        job.setDescription(hasText(form.getDescription()) ? form.getDescription().trim() : "");
        job.setRequirements(trimToNull(form.getRequirements()));
        job.setBenefits(trimToNull(form.getBenefits()));
        job.setLocation(trimToNull(form.getLocation()));
        job.setSalaryMin(form.getSalaryMin());
        job.setSalaryMax(form.getSalaryMax());
        job.setSalaryCurrency(hasText(form.getSalaryCurrency()) ? form.getSalaryCurrency().trim().toUpperCase(Locale.ROOT) : "VND");
        job.setExperience(trimToNull(form.getExperience()));
        job.setLevel(trimToNull(form.getLevel()));
        job.setEmploymentType(form.getEmploymentType() != null ? form.getEmploymentType() : EmploymentType.FULL_TIME);
        job.setQuantity(form.getQuantity() != null ? form.getQuantity() : 1);
        job.setDeadline(form.getDeadline());

        // ==========================================
        // XỬ LÝ GHIM TIN VÀ GIỚI HẠN ĐĂNG BÀI (CHUẨN)
        // ==========================================
        UserPackage activePkg = servicePackageService.getActivePackage(recruiter);

        // 1. Kiểm tra quyền ghim tin
        if (form.getIsPinned() != null && form.getIsPinned()) {
            boolean canPin = activePkg != null && Boolean.TRUE.equals(activePkg.getAllowPinJob());
            if (!canPin) {
                throw new IllegalArgumentException("Gói hiện tại không hỗ trợ ghim tin. Vui lòng nâng cấp gói Pro.");
            }
            job.setIsPinned(true);
        } else {
            job.setIsPinned(false);
        }

        // 2. Kiểm tra giới hạn đăng bài (Chỉ check và trừ quota khi TẠO MỚI)
        if (form.getJobId() == null) {
            if (activePkg == null) {
                // Chưa có gói -> 1 bài free
                long currentCount = jobRepository.countByRecruiter_Id(recruiter.getId());
                if (currentCount >= 1) {
                    throw new IllegalArgumentException("Bạn đã dùng hết 1 bài đăng miễn phí. Vui lòng mua gói.");
                }
            } else {
                // Đã có gói -> check snapshot quota
                Integer maxJobs = activePkg.getSnapshotMaxJobs();
                if (maxJobs != null) {
                    int used = activePkg.getUsedJobs() == null ? 0 : activePkg.getUsedJobs();
                    if (used >= maxJobs) {
                        throw new IllegalArgumentException("Bạn đã đăng " + used + "/" + maxJobs + " bài của gói. Vui lòng mua thêm tin.");
                    }
                    // Trừ 1 lượt và lưu lại
                    activePkg.setUsedJobs(used + 1);
                    userPackageRepository.save(activePkg);
                }
                // Nếu maxJobs == null (vô hạn) thì không cần trừ
            }
        }
        // ==========================================

        if (form.getJobImageFile() != null && !form.getJobImageFile().isEmpty()) {
            try {
                String jobImageUrl = fileStorageService.saveJobImage(form.getJobImageFile());
                job.setLogoUrl(jobImageUrl);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Không thể lưu ảnh Job. Vui lòng thử lại.", ex);
            }
        }

        if (submitForReview) {
            ensureCompanyCanPublish(company);
            job.setStatus(JobStatus.PENDING);
            job.setRejectionReason(null);
            job.setPublishedAt(null);
        } else {
            job.setStatus(JobStatus.DRAFT);
            job.setRejectionReason(null);
            job.setPublishedAt(null);
        }

        Job saved = jobRepository.save(job);
        jobSkillService.replaceRequired(saved.getId(), selectedSkills);
        return saved;
    }

    public Job submitExistingJob(Long jobId, User recruiter) {
        if (recruiter == null || recruiter.getId() == null) throw new IllegalArgumentException("Không xác định được tài khoản Recruiter.");
        Job job = getJobForRecruiter(jobId, recruiter.getId());
        validatePublishCategoryAndSkills(job);
        if (job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.REJECTED) throw new IllegalArgumentException("Chỉ được gửi duyệt tin DRAFT hoặc REJECTED.");
        if (!hasText(job.getTitle()) || !hasText(job.getDescription()) || job.getCategory() == null) throw new IllegalArgumentException("Tin cần có tiêu đề, mô tả và danh mục trước khi gửi duyệt.");
        if (job.getDeadline() == null || job.getDeadline().isBefore(LocalDate.now())) throw new IllegalArgumentException("Hãy cập nhật hạn ứng tuyển từ hôm nay trở đi trước khi gửi duyệt.");
        ensureCompanyCanPublish(job.getCompany());
        job.setStatus(JobStatus.PENDING);
        job.setRejectionReason(null);
        job.setPublishedAt(null);
        return jobRepository.save(job);
    }

    public Job closeRecruiterJob(Long jobId, User recruiter) {
        if (recruiter == null || recruiter.getId() == null) throw new IllegalArgumentException("Không xác định được tài khoản Recruiter.");
        Job job = getJobForRecruiter(jobId, recruiter.getId());
        if (job.getStatus() != JobStatus.ACTIVE) throw new IllegalArgumentException("Chỉ có thể đóng tin đang ACTIVE.");
        job.setStatus(JobStatus.CLOSED);
        return jobRepository.save(job);
    }

    public Job reopenRecruiterJob(Long jobId, User recruiter) {
        return reopenRecruiterJob(jobId, recruiter, null);
    }

    public Job reopenRecruiterJob(Long jobId, User recruiter, LocalDate newDeadline) {
        if (recruiter == null || recruiter.getId() == null)
            throw new IllegalArgumentException("Không xác định được tài khoản Recruiter.");
        Job job = getJobForRecruiter(jobId, recruiter.getId());
        if (job.getStatus() != JobStatus.CLOSED && job.getStatus() != JobStatus.EXPIRED)
            throw new IllegalArgumentException("Chỉ có thể mở lại tin đã đóng hoặc quá hạn.");
        if (newDeadline != null && newDeadline.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Hạn ứng tuyển mới không được ở quá khứ.");
        if (newDeadline != null) job.setDeadline(newDeadline);
        job.setStatus(JobStatus.DRAFT);
        job.setPublishedAt(null);
        job.setRejectionReason(null);
        return jobRepository.save(job);
    }

    private void validateJobForm(RecruiterJobForm form, boolean submitForReview) {
        if (!hasText(form.getTitle())) throw new IllegalArgumentException("Vui lòng nhập tiêu đề tin tuyển dụng.");
        if (form.getTitle().trim().length() > 255) throw new IllegalArgumentException("Tiêu đề không được vượt quá 255 ký tự.");
        if (submitForReview && !hasText(form.getDescription())) throw new IllegalArgumentException("Cần có mô tả công việc trước khi gửi duyệt.");
        if (form.getCategoryId() == null) throw new IllegalArgumentException("Vui lòng chọn danh mục nghề nghiệp.");
        if (form.getQuantity() == null || form.getQuantity() < 1) throw new IllegalArgumentException("Số lượng tuyển phải từ 1 trở lên.");
        if (form.getSalaryMin() != null && form.getSalaryMin().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Lương tối thiểu không được âm.");
        if (form.getSalaryMax() != null && form.getSalaryMax().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Lương tối đa không được âm.");
        if (form.getSalaryMin() != null && form.getSalaryMax() != null && form.getSalaryMin().compareTo(form.getSalaryMax()) > 0) throw new IllegalArgumentException("Lương tối thiểu không được lớn hơn lương tối đa.");
        if (submitForReview && (form.getDeadline() == null || form.getDeadline().isBefore(LocalDate.now()))) throw new IllegalArgumentException("Hạn ứng tuyển phải từ hôm nay trở đi trước khi gửi duyệt.");
    }

    private void ensureCompanyCanPublish(Company company) {
        if (company == null || company.getStatus() != CompanyStatus.ACTIVE) throw new IllegalArgumentException("Công ty chưa được Admin phê duyệt. Bạn chỉ có thể lưu nháp.");
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    @Transactional(readOnly = true)
    public List<Job> getPendingJobs() {
        return jobRepository.findPendingJobsWithDetails();
    }

    public Job approveJob(Long jobId) {
        Job job = jobRepository.findByIdWithDetails(jobId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin tuyển dụng."));
        validatePublishCategoryAndSkills(job);
        if (job.getStatus() != JobStatus.PENDING) throw new IllegalArgumentException("Chỉ có thể duyệt tin đang ở trạng thái PENDING.");
        if (job.getCompany().getStatus() != CompanyStatus.ACTIVE) throw new IllegalArgumentException("Không thể duyệt tin vì công ty chưa ở trạng thái ACTIVE.");
        if (job.getDeadline() == null || job.getDeadline().isBefore(LocalDate.now())) throw new IllegalArgumentException("Không thể duyệt tin nếu thiếu hạn ứng tuyển hợp lệ.");
        job.setStatus(JobStatus.ACTIVE);
        job.setPublishedAt(LocalDateTime.now());
        job.setRejectionReason(null);
        return jobRepository.save(job);
    }

    private void validatePublishCategoryAndSkills(Job job) {
        if (job.getCategory() == null || !"ACTIVE".equalsIgnoreCase(job.getCategory().getStatus()))
            throw new IllegalArgumentException("Danh mục đã ngừng hoạt động. Hãy chọn danh mục và kỹ năng hợp lệ trước khi gửi hoặc duyệt tin.");
        jobSkillService.validate(job.getCategory().getId(), jobSkillService.requiredIds(job.getId()));
    }

    public Job rejectJob(Long jobId, String reason) {
        if (!hasText(reason)) throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 500) throw new IllegalArgumentException("Lý do từ chối không được vượt quá 500 ký tự.");
        Job job = jobRepository.findByIdWithDetails(jobId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin tuyển dụng."));
        if (job.getStatus() != JobStatus.PENDING) throw new IllegalArgumentException("Chỉ có thể từ chối tin đang ở trạng thái PENDING.");
        job.setStatus(JobStatus.REJECTED);
        job.setRejectionReason(normalizedReason);
        job.setPublishedAt(null);
        return jobRepository.save(job);
    }

    public Job reopenAdminJob(Long jobId) {
        Job job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin tuyển dụng."));
        if (job.getStatus() != JobStatus.CLOSED && job.getStatus() != JobStatus.EXPIRED)
            throw new IllegalArgumentException("Chỉ mở lại được tin đã đóng hoặc quá hạn.");
        job.setStatus(JobStatus.DRAFT);
        job.setPublishedAt(null);
        job.setRejectionReason(null);
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Optional<Job> findJobById(Long id) {
        return jobRepository.findById(id);
    }
}