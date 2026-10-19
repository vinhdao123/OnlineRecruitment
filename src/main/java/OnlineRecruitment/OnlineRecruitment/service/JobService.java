package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.RecruiterJobForm;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.EmploymentType;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    public JobService(
            JobRepository jobRepository,
            JobCategoryRepository jobCategoryRepository,
            CompanyRepository companyRepository
    ) {
        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.companyRepository = companyRepository;
    }

    // =====================================================
    // GUEST
    // =====================================================

    @Transactional(readOnly = true)
    public List<Job> getNewestJobs() {
        return jobRepository.findActiveJobs();
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
                .filter(category ->
                        category.getStatus() != null
                                && "ACTIVE".equalsIgnoreCase(
                                category.getStatus().trim()
                        )
                )
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
        // Chỉ lấy tin ACTIVE của công ty ACTIVE và chưa hết hạn.
        List<Job> jobs = jobRepository.findActiveJobs();

        if (hasText(keyword)) {
            String keywordLower = keyword.trim().toLowerCase(Locale.ROOT);

            jobs = jobs.stream()
                    .filter(job ->
                            job.getTitle() != null
                                    && job.getTitle()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(keywordLower)
                    )
                    .collect(Collectors.toList());
        }

        if (hasText(locationKeyword)) {
            String locationLower =
                    locationKeyword.trim().toLowerCase(Locale.ROOT);

            jobs = jobs.stream()
                    .filter(job ->
                            job.getLocation() != null
                                    && job.getLocation()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(locationLower)
                    )
                    .collect(Collectors.toList());
        }

        if (locations != null && !locations.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job ->
                            job.getLocation() != null
                                    && locations.stream().anyMatch(location ->
                                    job.getLocation()
                                            .equalsIgnoreCase(location)
                            )
                    )
                    .collect(Collectors.toList());
        }

        if (experiences != null && !experiences.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job ->
                            matchesExperience(
                                    job.getExperience(),
                                    experiences
                            )
                    )
                    .collect(Collectors.toList());
        }

        if (salaryRanges != null && !salaryRanges.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job ->
                            matchesSalary(
                                    job.getSalaryMin(),
                                    job.getSalaryMax(),
                                    job.getSalaryCurrency(),
                                    salaryRanges
                            )
                    )
                    .collect(Collectors.toList());
        }

        if (levels != null && !levels.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job ->
                            job.getLevel() != null
                                    && levels.stream().anyMatch(level ->
                                    job.getLevel()
                                            .equalsIgnoreCase(level)
                            )
                    )
                    .collect(Collectors.toList());
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job ->
                            job.getCategory() != null
                                    && categoryIds.contains(
                                    job.getCategory().getId()
                            )
                    )
                    .collect(Collectors.toList());
        }

        jobs.sort(
                Comparator.comparing(
                        Job::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        return jobs;
    }

    // =====================================================
    // HỖ TRỢ LỌC KINH NGHIỆM
    // =====================================================

    private boolean matchesExperience(
            String experience,
            List<String> selectedRanges
    ) {
        if (!hasText(experience)) {
            return selectedRanges.contains("none");
        }

        String value = experience.trim().toLowerCase(Locale.ROOT);

        if (value.contains("không")
                || NO_EXP_PATTERN.matcher(value).find()
                || value.contains("not required")
                || value.contains("no experience")) {
            return selectedRanges.contains("none");
        }

        List<Integer> numbers = extractNumbers(value);

        if (numbers.isEmpty()) {
            return false;
        }

        double jobMin = numbers.get(0);
        double jobMax = numbers.size() >= 2
                ? numbers.get(1)
                : jobMin;

        if (jobMin > 5) {
            jobMax = Double.MAX_VALUE;
        }

        for (String range : selectedRanges) {
            double filterMin;
            double filterMax;

            switch (range) {
                case "0-1":
                    filterMin = 0;
                    filterMax = 1;
                    break;

                case "1-3":
                    filterMin = 1;
                    filterMax = 3;
                    break;

                case "3-5":
                    filterMin = 3;
                    filterMax = 5;
                    break;

                case "5+":
                    filterMin = 5;
                    filterMax = Double.MAX_VALUE;
                    break;

                default:
                    continue;
            }

            if (jobMin <= filterMax && jobMax >= filterMin) {
                return true;
            }
        }

        return false;
    }

    // =====================================================
    // HỖ TRỢ LỌC MỨC LƯƠNG
    // =====================================================

    private boolean matchesSalary(
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String currency,
            List<String> selectedRanges
    ) {
        if (salaryMin == null && salaryMax == null) {
            return false;
        }

        double jobMin = salaryMin != null
                ? salaryMin.doubleValue()
                : salaryMax.doubleValue();

        double jobMax = salaryMax != null
                ? salaryMax.doubleValue()
                : salaryMin.doubleValue();

        if ("VND".equalsIgnoreCase(currency)
                || jobMin > 100000
                || jobMax > 100000) {
            jobMin /= VND_TO_USD_RATE;
            jobMax /= VND_TO_USD_RATE;
        }

        for (String range : selectedRanges) {
            double filterMin;
            double filterMax;

            switch (range) {
                case "under-1000":
                    filterMin = 0;
                    filterMax = 1000;
                    break;

                case "1000-2000":
                    filterMin = 1000;
                    filterMax = 2000;
                    break;

                case "2000-3000":
                    filterMin = 2000;
                    filterMax = 3000;
                    break;

                case "3000+":
                    filterMin = 3000;
                    filterMax = Double.MAX_VALUE;
                    break;

                default:
                    continue;
            }

            if (jobMin <= filterMax && jobMax >= filterMin) {
                return true;
            }
        }

        return false;
    }

    // =====================================================
    // TÁCH SỐ TỪ CHUỖI KINH NGHIỆM
    // =====================================================

    private List<Integer> extractNumbers(String text) {
        List<Integer> numbers = new ArrayList<>();

        for (String part : text.split("[^0-9]+")) {
            if (!part.isEmpty()) {
                try {
                    numbers.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                    // Bỏ qua phần không thể chuyển thành số.
                }
            }
        }

        return numbers;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // =====================================================
    // RECRUITER: LẤY TẤT CẢ TIN TRONG DATABASE
    // =====================================================

    // Giữ phương thức hai tham số để tương thích với code cũ.
    @Transactional(readOnly = true)
    public List<Job> findAllJobsForManagement(
            String keyword,
            String status
    ) {
        return findAllJobsForManagement(keyword, null, status);
    }

    // Phương thức chính: lọc tiêu đề, địa điểm và trạng thái.
    @Transactional(readOnly = true)
    public List<Job> findAllJobsForManagement(
            String keyword,
            String location,
            String status
    ) {
        String normalizedKeyword =
                hasText(keyword) ? keyword.trim() : null;

        String normalizedLocation =
                hasText(location) ? location.trim() : null;

        JobStatus jobStatus = null;

        if (hasText(status)) {
            try {
                jobStatus = JobStatus.valueOf(
                        status.trim().toUpperCase(Locale.ROOT)
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Trạng thái tin tuyển dụng không hợp lệ: " + status
                );
            }
        }

        return jobRepository.findAllForManagement(
                normalizedKeyword,
                normalizedLocation,
                jobStatus,
                Pageable.unpaged()
        ).getContent();
    }

    // =====================================================
    // RECRUITER: PHÂN TRANG DANH SÁCH TIN QUẢN LÝ
    // Giữ nguyên phương thức List cũ để không ảnh hưởng các trang đang dùng.
    // Trang bắt đầu từ 0, mỗi trang hiển thị 6 tin.
    // =====================================================

    @Transactional(readOnly = true)
    public Page<Job> findAllJobsForManagementPage(
            String keyword,
            String location,
            String status,
            int page
    ) {
        int pageSize = 6;
        int safePage = Math.max(page, 0);
        List<Job> allJobs = findAllJobsForManagement(keyword, location, status);
        Pageable pageable = PageRequest.of(safePage, pageSize);
        int start = (int) pageable.getOffset();

        if (start >= allJobs.size()) {
            return new PageImpl<>(List.of(), pageable, allJobs.size());
        }

        int end = Math.min(start + pageable.getPageSize(), allJobs.size());
        return new PageImpl<>(allJobs.subList(start, end), pageable, allJobs.size());
    }

    // =====================================================
    // THỐNG KÊ TOÀN BỘ DATABASE
    // =====================================================

    @Transactional(readOnly = true)
    public long countAllJobs() {
        return jobRepository.count();
    }

    @Transactional(readOnly = true)
    public long countAllJobsByStatus(JobStatus status) {
        if (status == null) {
            return 0;
        }

        return jobRepository.countByStatus(status);
    }

    // =====================================================
    // RECRUITER: DANH SÁCH VÀ THỐNG KÊ RIÊNG
    // Giữ lại phục vụ các trang cũ.
    // =====================================================

    @Transactional(readOnly = true)
    public List<Job> findMyJobs(
            Long recruiterId,
            String keyword,
            String status
    ) {
        if (recruiterId == null) {
            throw new IllegalArgumentException(
                    "Recruiter ID không hợp lệ."
            );
        }

        boolean hasKeyword = hasText(keyword);
        boolean hasStatus = hasText(status);
        JobStatus jobStatus = null;

        if (hasStatus) {
            try {
                jobStatus = JobStatus.valueOf(
                        status.trim().toUpperCase(Locale.ROOT)
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Trạng thái công việc không hợp lệ: " + status
                );
            }
        }

        if (hasKeyword && hasStatus) {
            return jobRepository
                    .findByRecruiter_IdAndTitleContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
                            recruiterId,
                            keyword.trim(),
                            jobStatus
                    );
        }

        if (hasKeyword) {
            return jobRepository
                    .findByRecruiter_IdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                            recruiterId,
                            keyword.trim()
                    );
        }

        if (hasStatus) {
            return jobRepository
                    .findByRecruiter_IdAndStatusOrderByCreatedAtDesc(
                            recruiterId,
                            jobStatus
                    );
        }

        return jobRepository.findByRecruiter_IdOrderByCreatedAtDesc(
                recruiterId
        );
    }

    @Transactional(readOnly = true)
    public long countMyJobs(Long recruiterId) {
        return recruiterId == null
                ? 0
                : jobRepository.countByRecruiter_Id(recruiterId);
    }

    @Transactional(readOnly = true)
    public long countMyJobsByStatus(
            Long recruiterId,
            JobStatus status
    ) {
        if (recruiterId == null || status == null) {
            return 0;
        }

        return jobRepository.countByRecruiter_IdAndStatus(
                recruiterId,
                status
        );
    }

    // =====================================================
    // LẤY TIN CỦA RECRUITER VÀ KIỂM TRA CHỦ SỞ HỮU
    // =====================================================

    @Transactional(readOnly = true)
    public Job getJobForRecruiter(
            Long jobId,
            Long recruiterId
    ) {
        return jobRepository.findByIdAndRecruiter_Id(jobId, recruiterId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tin tuyển dụng hoặc bạn không có quyền truy cập."
                ));
    }

    // =====================================================
    // RECRUITER: TẠO / SỬA / LƯU NHÁP / GỬI DUYỆT
    // =====================================================

    public Job saveRecruiterJob(
            User recruiter,
            RecruiterJobForm form,
            boolean submitForReview
    ) {
        if (recruiter == null || recruiter.getId() == null) {
            throw new IllegalArgumentException(
                    "Không xác định được tài khoản Recruiter."
            );
        }

        if (form == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu tin tuyển dụng không hợp lệ."
            );
        }

        validateJobForm(form, submitForReview);

        Company company = companyRepository.findByRecruiterId(
                        recruiter.getId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tài khoản chưa có thông tin công ty."
                ));

        Job job;

        // true nếu đang sửa tin ACTIVE/PENDING
        boolean wasLiveOrPending = false;

        if (form.getJobId() != null) {
            // Chỉ sửa tin thuộc recruiter đang đăng nhập.
            job = getJobForRecruiter(
                    form.getJobId(),
                    recruiter.getId()
            );

            // Tin đã đóng (CLOSED) phải mở lại rồi mới sửa; các trạng thái khác được sửa
            if (job.getStatus() == JobStatus.CLOSED) {
                throw new IllegalArgumentException(
                        "Tin đã đóng. Hãy mở lại tin trước khi sửa."
                );
            }

            // Nhớ trạng thái cũ để biết tin đang chạy/chờ duyệt hay không
            wasLiveOrPending = job.getStatus() == JobStatus.ACTIVE
                    || job.getStatus() == JobStatus.PENDING;
        } else {
            job = new Job();
            job.setRecruiter(recruiter);
            job.setCompany(company);
        }

        // Cập nhật logo công ty từ URL trong form (để trống = giữ logo cũ)
        if (hasText(form.getLogoUrl())) {
            String logoUrl = form.getLogoUrl().trim();

            // Chỉ nhận đường dẫn http/https
            if (!logoUrl.matches("(?i)^https?://\\S+$")) {
                throw new IllegalArgumentException(
                        "URL logo phải bắt đầu bằng http:// hoặc https://"
                );
            }

            if (logoUrl.length() > 500) {
                throw new IllegalArgumentException(
                        "URL logo không được vượt quá 500 ký tự."
                );
            }

            company.setLogoUrl(logoUrl);
            companyRepository.save(company);
        }

        JobCategory category = jobCategoryRepository
                .findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Danh mục nghề nghiệp không tồn tại."
                ));

        if (!"ACTIVE".equalsIgnoreCase(category.getStatus())) {
            throw new IllegalArgumentException(
                    "Danh mục nghề nghiệp này hiện không hoạt động."
            );
        }

        job.setRecruiter(recruiter);
        job.setCompany(company);
        job.setCategory(category);
        job.setTitle(form.getTitle().trim());
        job.setDescription(form.getDescription().trim());
        job.setRequirements(trimToNull(form.getRequirements()));
        job.setBenefits(trimToNull(form.getBenefits()));
        job.setLocation(trimToNull(form.getLocation()));
        job.setSalaryMin(form.getSalaryMin());
        job.setSalaryMax(form.getSalaryMax());
        job.setSalaryCurrency(
                hasText(form.getSalaryCurrency())
                        ? form.getSalaryCurrency()
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        : "VND"
        );
        job.setExperience(trimToNull(form.getExperience()));
        job.setLevel(trimToNull(form.getLevel()));
        job.setEmploymentType(
                form.getEmploymentType() != null
                        ? form.getEmploymentType()
                        : EmploymentType.FULL_TIME
        );
        job.setQuantity(
                form.getQuantity() != null ? form.getQuantity() : 1
        );
        job.setDeadline(form.getDeadline());

        // Sửa tin đang hiển thị/chờ duyệt: gửi lại Admin duyệt (PENDING), dù bấm nút nào
        if (submitForReview || wasLiveOrPending) {
            ensureCompanyCanPublish(company);
            job.setStatus(JobStatus.PENDING);
            job.setRejectionReason(null);
            job.setPublishedAt(null);
        } else {
            job.setStatus(JobStatus.DRAFT);
            job.setRejectionReason(null);
            job.setPublishedAt(null);
        }

        return jobRepository.save(job);
    }

    // =====================================================
    // GỬI TIN ĐÃ LƯU NHÁP HOẶC TIN BỊ TỪ CHỐI
    // =====================================================

    public Job submitExistingJob(
            Long jobId,
            User recruiter
    ) {
        if (recruiter == null || recruiter.getId() == null) {
            throw new IllegalArgumentException(
                    "Không xác định được tài khoản Recruiter."
            );
        }

        Job job = getJobForRecruiter(jobId, recruiter.getId());

        if (job.getStatus() != JobStatus.DRAFT
                && job.getStatus() != JobStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Chỉ được gửi duyệt tin DRAFT hoặc REJECTED."
            );
        }

        if (!hasText(job.getTitle())
                || !hasText(job.getDescription())
                || job.getCategory() == null) {
            throw new IllegalArgumentException(
                    "Tin cần có tiêu đề, mô tả và danh mục trước khi gửi duyệt."
            );
        }

        if (job.getDeadline() != null
                && job.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Hạn ứng tuyển đã qua. Hãy cập nhật hạn ứng tuyển trước."
            );
        }

        ensureCompanyCanPublish(job.getCompany());

        job.setStatus(JobStatus.PENDING);
        job.setRejectionReason(null);
        job.setPublishedAt(null);

        return jobRepository.save(job);
    }

    // =====================================================
    // ĐÓNG TIN TUYỂN DỤNG
    // =====================================================

    public Job closeRecruiterJob(
            Long jobId,
            User recruiter
    ) {
        if (recruiter == null || recruiter.getId() == null) {
            throw new IllegalArgumentException(
                    "Không xác định được tài khoản Recruiter."
            );
        }

        Job job = getJobForRecruiter(jobId, recruiter.getId());

        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Chỉ có thể đóng tin đang ACTIVE."
            );
        }

        job.setStatus(JobStatus.CLOSED);

        return jobRepository.save(job);
    }

    // =====================================================
    // MỞ LẠI TIN ĐÃ ĐÓNG
    // =====================================================

    public Job reopenRecruiterJob(
            Long jobId,
            User recruiter
    ) {
        if (recruiter == null || recruiter.getId() == null) {
            throw new IllegalArgumentException(
                    "Không xác định được tài khoản Recruiter."
            );
        }

        // Chỉ lấy được tin của chính recruiter này
        Job job = getJobForRecruiter(jobId, recruiter.getId());

        // Chỉ tin đã đóng mới mở lại được
        if (job.getStatus() != JobStatus.CLOSED) {
            throw new IllegalArgumentException(
                    "Chỉ có thể mở lại tin đã đóng."
            );
        }

        // Hạn ứng tuyển đã qua thì phải sửa hạn trước
        if (job.getDeadline() != null
                && job.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Hạn ứng tuyển đã qua. Hãy cập nhật hạn ứng tuyển trước khi mở lại."
            );
        }

        // Công ty phải còn ACTIVE
        ensureCompanyCanPublish(job.getCompany());

        // Tin đã từng được duyệt nên mở lại luôn
        job.setStatus(JobStatus.ACTIVE);

        return jobRepository.save(job);
    }

    // =====================================================
    // VALIDATE FORM TUYỂN DỤNG
    // =====================================================

    private void validateJobForm(
            RecruiterJobForm form,
            boolean submitForReview
    ) {
        if (!hasText(form.getTitle())) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tiêu đề tin tuyển dụng."
            );
        }

        if (form.getTitle().trim().length() > 255) {
            throw new IllegalArgumentException(
                    "Tiêu đề không được vượt quá 255 ký tự."
            );
        }

        if (!hasText(form.getDescription())) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập mô tả công việc."
            );
        }

        if (form.getCategoryId() == null) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn danh mục nghề nghiệp."
            );
        }

        if (form.getQuantity() == null || form.getQuantity() < 1) {
            throw new IllegalArgumentException(
                    "Số lượng tuyển phải từ 1 trở lên."
            );
        }

        if (form.getSalaryMin() != null
                && form.getSalaryMin().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Lương tối thiểu không được âm."
            );
        }

        if (form.getSalaryMax() != null
                && form.getSalaryMax().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Lương tối đa không được âm."
            );
        }

        if (form.getSalaryMin() != null
                && form.getSalaryMax() != null
                && form.getSalaryMin().compareTo(form.getSalaryMax()) > 0) {
            throw new IllegalArgumentException(
                    "Lương tối thiểu không được lớn hơn lương tối đa."
            );
        }

        if (form.getDeadline() != null
                && form.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Hạn ứng tuyển không được là ngày trong quá khứ."
            );
        }

        if (submitForReview && !hasText(form.getDescription())) {
            throw new IllegalArgumentException(
                    "Cần có mô tả công việc trước khi gửi duyệt."
            );
        }
    }

    // =====================================================
    // KIỂM TRA CÔNG TY ĐƯỢC PHÉP ĐĂNG TIN
    // =====================================================

    private void ensureCompanyCanPublish(Company company) {
        if (company == null
                || company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Công ty chưa được Admin phê duyệt. Bạn chỉ có thể lưu nháp."
            );
        }
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    // =====================================================
    // ADMIN: LẤY TIN ĐANG CHỜ DUYỆT
    // =====================================================

    @Transactional(readOnly = true)
    public List<Job> getPendingJobs() {
        return jobRepository.findPendingJobsWithDetails();
    }

    // =====================================================
    // ADMIN: DUYỆT TIN
    // =====================================================

    public Job approveJob(Long jobId) {
        Job job = jobRepository.findByIdWithDetails(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tin tuyển dụng."
                ));

        if (job.getStatus() != JobStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Chỉ có thể duyệt tin đang ở trạng thái PENDING."
            );
        }

        if (job.getCompany().getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Không thể duyệt tin vì công ty chưa ở trạng thái ACTIVE."
            );
        }

        if (job.getDeadline() != null
                && job.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Không thể duyệt tin vì hạn ứng tuyển đã qua."
            );
        }

        job.setStatus(JobStatus.ACTIVE);
        job.setPublishedAt(LocalDateTime.now());
        job.setRejectionReason(null);

        return jobRepository.save(job);
    }

    // =====================================================
    // ADMIN: TỪ CHỐI TIN
    // =====================================================

    public Job rejectJob(
            Long jobId,
            String reason
    ) {
        if (!hasText(reason)) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập lý do từ chối."
            );
        }

        String normalizedReason = reason.trim();

        if (normalizedReason.length() > 500) {
            throw new IllegalArgumentException(
                    "Lý do từ chối không được vượt quá 500 ký tự."
            );
        }

        Job job = jobRepository.findByIdWithDetails(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tin tuyển dụng."
                ));

        if (job.getStatus() != JobStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Chỉ có thể từ chối tin đang ở trạng thái PENDING."
            );
        }

        job.setStatus(JobStatus.REJECTED);
        job.setRejectionReason(normalizedReason);
        job.setPublishedAt(null);

        return jobRepository.save(job);
    }
}
