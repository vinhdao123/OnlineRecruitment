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

// Luồng: Controller nhận request -> Service kiểm tra nghiệp vụ -> Repository đọc/ghi DB.
// Job là đối tượng dữ liệu ánh xạ bảng jobs; RecruiterJobForm chứa dữ liệu từ màn hình nhập.
@Service
// Gom các thay đổi DB trong một giao dịch; mặc định rollback khi có lỗi runtime thoát ra.
@Transactional
public class JobService {

    // Tỷ lệ cố định để đổi VND sang USD khi lọc khoảng lương, không phải tỷ giá cập nhật tự động.
    private static final double VND_TO_USD_RATE = 25000.0;

    // Tìm từ "no" độc lập, không phân biệt hoa thường, để nhận diện mô tả không cần kinh nghiệm.
    private static final Pattern NO_EXP_PATTERN =
            Pattern.compile("\\bno\\b", Pattern.CASE_INSENSITIVE);

    // Repository truy cập DB; các service còn lại hỗ trợ file ảnh, kỹ năng và gói dịch vụ.
    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;
    private final JobSkillService jobSkillService;
    private final ServicePackageService servicePackageService;
    private final UserPackageRepository userPackageRepository; // Thêm repo này để cập nhật usedJobs

    // Spring truyền các dependency vào constructor để các hàm bên dưới sử dụng.
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
    // DỮ LIỆU DANH SÁCH VIỆC LÀM VÀ BỘ LỌC
    // =====================================================

    // Chỉ đọc: lấy tin ACTIVE, công ty ACTIVE và còn hạn theo query trong repository.
    @Transactional(readOnly = true)
    public List<Job> getNewestJobs() {
        return jobRepository.findActiveJobs();
    }

    // Cập nhật hàng loạt: tin ACTIVE có deadline trước hôm nay -> EXPIRED.
    @Transactional
    public void refreshExpiredJobs() {
        jobRepository.expireActiveJobs(LocalDate.now());
    }

    // Chuẩn bị địa điểm cho bộ lọc: Job -> chuỗi địa điểm -> bỏ rỗng -> bỏ trùng.
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

    // Lấy mọi danh mục, bao gồm danh mục đã ngừng hoạt động.
    @Transactional(readOnly = true)
    public List<JobCategory> getAllCategories() {
        return jobCategoryRepository.findAll();
    }

    // Form tạo/sửa chỉ cung cấp danh mục đang hoạt động để người dùng lựa chọn.
    @Transactional(readOnly = true)
    public List<JobCategory> getActiveCategories() {
        return jobCategoryRepository.findAll()
                .stream()
                .filter(category -> category.getStatus() != null && "ACTIVE".equalsIgnoreCase(category.getStatus().trim()))
                .collect(Collectors.toList());
    }

    // Trang /jobs gọi hàm này để lọc tin công khai theo các lựa chọn của người tìm việc.
    // Các nhóm điều kiện kết hợp AND; nhiều lựa chọn trong cùng một nhóm thường kết hợp OR.
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
        // Đọc tập tin đủ điều kiện công khai từ DB trước, sau đó lọc thêm trong Java.
        List<Job> jobs = jobRepository.findActiveJobs();

        // Lọc tiêu đề chứa từ khóa, không phân biệt hoa thường.
        if (hasText(keyword)) {
            String keywordLower = keyword.trim().toLowerCase(Locale.ROOT);
            jobs = jobs.stream().filter(job -> job.getTitle() != null && job.getTitle().toLowerCase(Locale.ROOT).contains(keywordLower)).collect(Collectors.toList());
        }
        // Ô tìm địa điểm dùng so sánh chứa chuỗi, ví dụ "Hà Nội" khớp "Cầu Giấy, Hà Nội".
        if (hasText(locationKeyword)) {
            String locationLower = locationKeyword.trim().toLowerCase(Locale.ROOT);
            jobs = jobs.stream().filter(job -> job.getLocation() != null && job.getLocation().toLowerCase(Locale.ROOT).contains(locationLower)).collect(Collectors.toList());
        }
        // Danh sách địa điểm dùng so sánh bằng; chỉ cần khớp một địa điểm đã chọn.
        if (locations != null && !locations.isEmpty()) {
            jobs = jobs.stream().filter(job -> job.getLocation() != null && locations.stream().anyMatch(location -> job.getLocation().equalsIgnoreCase(location))).collect(Collectors.toList());
        }
        // Hai hàm matches... bên dưới kiểm tra khoảng kinh nghiệm và khoảng lương.
        if (experiences != null && !experiences.isEmpty()) {
            jobs = jobs.stream().filter(job -> matchesExperience(job.getExperience(), experiences)).collect(Collectors.toList());
        }
        if (salaryRanges != null && !salaryRanges.isEmpty()) {
            jobs = jobs.stream().filter(job -> matchesSalary(job.getSalaryMin(), job.getSalaryMax(), job.getSalaryCurrency(), salaryRanges)).collect(Collectors.toList());
        }
        // Giữ tin có cấp bậc thuộc một trong các lựa chọn.
        if (levels != null && !levels.isEmpty()) {
            jobs = jobs.stream().filter(job -> job.getLevel() != null && levels.stream().anyMatch(level -> job.getLevel().equalsIgnoreCase(level))).collect(Collectors.toList());
        }
        // Giữ tin có ID danh mục nằm trong danh sách đã chọn.
        if (categoryIds != null && !categoryIds.isEmpty()) {
            jobs = jobs.stream().filter(job -> job.getCategory() != null && categoryIds.contains(job.getCategory().getId())).collect(Collectors.toList());
        }

        // Sắp ngày tạo mới nhất trước, ngày null ở cuối; bước này sắp lại thứ tự từ query.
        jobs.sort(Comparator.comparing(Job::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return jobs;
    }

    // true: tin khớp ít nhất một khoảng kinh nghiệm; false: loại khỏi kết quả lọc.
    private boolean matchesExperience(String experience, List<String> selectedRanges) {
        // Hiện tại, mô tả trống cũng được xem là nhóm không yêu cầu kinh nghiệm (none).
        if (!hasText(experience)) return selectedRanges.contains("none");
        String value = experience.trim().toLowerCase(Locale.ROOT);
        if (value.contains("không") || NO_EXP_PATTERN.matcher(value).find() || value.contains("not required") || value.contains("no experience")) return selectedRanges.contains("none");
        // Ví dụ "1-3 năm" -> [1, 3]; nếu chỉ có một số thì dùng cho cả hai đầu khoảng.
        List<Integer> numbers = extractNumbers(value);
        if (numbers.isEmpty()) return false;
        double jobMin = numbers.get(0);
        double jobMax = numbers.size() >= 2 ? numbers.get(1) : jobMin;
        // Quy tắc hiện tại: số đầu lớn hơn 5 thì xem khoảng có giới hạn trên rất lớn.
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
            // Hai khoảng giao nhau thì khớp, ví dụ tin 2-4 năm khớp bộ lọc 1-3 năm.
            if (jobMin <= filterMax && jobMax >= filterMin) return true;
        }
        return false;
    }

    // Đổi về khoảng USD rồi kiểm tra giao nhau với các khoảng lương đã chọn.
    private boolean matchesSalary(BigDecimal salaryMin, BigDecimal salaryMax, String currency, List<String> selectedRanges) {
        // Lương thỏa thuận (cả hai đầu null) không khớp bộ lọc số tiền này.
        if (salaryMin == null && salaryMax == null) return false;
        double jobMin = salaryMin != null ? salaryMin.doubleValue() : salaryMax.doubleValue();
        double jobMax = salaryMax != null ? salaryMax.doubleValue() : salaryMin.doubleValue();
        // Ngoài đơn vị VND, code còn suy đoán số tiền lớn hơn 100000 là VND.
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

    // Tách các số nguyên trong chuỗi, ví dụ "Từ 1 đến 3 năm" -> [1, 3].
    private List<Integer> extractNumbers(String text) {
        List<Integer> numbers = new ArrayList<>();
        for (String part : text.split("[^0-9]+")) {
            if (!part.isEmpty()) {
                try { numbers.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) {}
            }
        }
        return numbers;
    }

    // Chỉ true khi chuỗi khác null và có nội dung sau khi bỏ khoảng trắng đầu/cuối.
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // =====================================================
    // DANH SÁCH QUẢN TRỊ TOÀN HỆ THỐNG VÀ DANH SÁCH RIÊNG CỦA RECRUITER
    // =====================================================

    @Transactional(readOnly = true)
    public List<Job> findAllJobsForManagement(String keyword, String status) {
        // Bản hai tham số dùng lại bản ba tham số, không lọc địa điểm.
        return findAllJobsForManagement(keyword, null, status);
    }

    @Transactional(readOnly = true)
    public List<Job> findAllJobsForManagement(String keyword, String location, String status) {
        // Chuỗi trống -> null để repository hiểu là không áp dụng bộ lọc đó.
        String normalizedKeyword = hasText(keyword) ? keyword.trim() : null;
        String normalizedLocation = hasText(location) ? location.trim() : null;
        JobStatus jobStatus = null;
        if (hasText(status)) {
            try { jobStatus = JobStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Trạng thái tin tuyển dụng không hợp lệ: " + status); }
        }
        // unpaged(): lấy toàn bộ kết quả, không phân trang tại DB. Hàm này không giới hạn chủ tin.
        return jobRepository.findAllForManagement(normalizedKeyword, normalizedLocation, jobStatus, Pageable.unpaged()).getContent();
    }

    @Transactional(readOnly = true)
    public Page<Job> findAllJobsForManagementPage(String keyword, String location, String status, int page) {
        // Lấy danh sách rồi cắt trang trong Java; PageRequest ở đây chỉ mô tả trang kết quả.
        int pageSize = 6;
        int safePage = Math.max(page, 0);
        List<Job> allJobs = findAllJobsForManagement(keyword, location, status);
        Pageable pageable = PageRequest.of(safePage, pageSize);
        int start = (int) pageable.getOffset();
        if (start >= allJobs.size()) return new PageImpl<>(List.of(), pageable, allJobs.size());
        int end = Math.min(start + pageable.getPageSize(), allJobs.size());
        return new PageImpl<>(allJobs.subList(start, end), pageable, allJobs.size());
    }

    // Các hàm count... phục vụ số đếm trên tab/thống kê, không trả danh sách tin.
    @Transactional(readOnly = true)
    public long countAllJobs() { return jobRepository.count(); }

    @Transactional(readOnly = true)
    public long countAllJobsByStatus(JobStatus status) {
        if (status == null) return 0;
        return jobRepository.countByStatus(status);
    }

    // Controller trang recruiter/jobs gọi hàm này; mọi nhánh đều lọc theo recruiterId.
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

        // Chọn một trong bốn query: cả hai bộ lọc / chỉ tiêu đề / chỉ trạng thái / không lọc thêm.
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

    // Kiểm tra quyền sở hữu ngay trong query: ID tin và ID recruiter phải cùng khớp.
    // Dùng chung cho sửa, gửi duyệt, đóng và mở lại để tránh thao tác lên tin của người khác.
    @Transactional(readOnly = true)
    public Job getJobForRecruiter(Long jobId, Long recruiterId) {
        return jobRepository.findByIdAndRecruiter_Id(jobId, recruiterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin tuyển dụng hoặc bạn không có quyền truy cập."));
    }

    // =====================================================
    // RECRUITER: TẠO / SỬA / LƯU NHÁP / GỬI DUYỆT (QUAN TRỌNG)
    // =====================================================

    // Nhận nội dung từ job-form.html qua Controller. true = gửi duyệt, false = lưu nháp.
    public Job saveRecruiterJob(User recruiter, RecruiterJobForm form, boolean submitForReview) {
        // Bước 1: xác nhận có tài khoản và có dữ liệu form trước khi đọc các thuộc tính.
        if (recruiter == null || recruiter.getId() == null) throw new IllegalArgumentException("Không xác định được tài khoản Recruiter.");
        if (form == null) throw new IllegalArgumentException("Dữ liệu tin tuyển dụng không hợp lệ.");

        // Bước 2: kiểm tra tiêu đề, danh mục, số lượng, lương; gửi duyệt cần thêm mô tả và hạn hợp lệ.
        validateJobForm(form, submitForReview);

        // Bước 3: cả lưu nháp cũng cần có bản ghi công ty, nhưng chưa bắt buộc công ty ACTIVE.
        Company company = companyRepository.findByRecruiterId(recruiter.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có thông tin công ty."));

        // Bước 4: có jobId là sửa tin cũ; chưa có jobId là tạo đối tượng Job mới.
        // Khi sửa phải lấy đúng chủ; tin CLOSED/EXPIRED cần chuyển về nháp trước.
        Job job;
        if (form.getJobId() != null) {
            job = getJobForRecruiter(form.getJobId(), recruiter.getId());
            if (job.getStatus() == JobStatus.CLOSED || job.getStatus() == JobStatus.EXPIRED) throw new IllegalArgumentException("Tin đã đóng hoặc quá hạn. Hãy chuyển về nháp trước khi sửa.");
        } else {
            job = new Job();
            job.setRecruiter(recruiter);
            job.setCompany(company);
        }

        // Nếu có URL logo gửi lên thì cập nhật LOGO CÔNG TY, không phải avatar profile.
        if (hasText(form.getLogoUrl())) {
            String logoUrl = form.getLogoUrl().trim();
            if (!logoUrl.matches("(?i)^https?://\\S+$")) throw new IllegalArgumentException("URL logo phải bắt đầu bằng http:// hoặc https://");
            if (logoUrl.length() > 500) throw new IllegalArgumentException("URL logo không được vượt quá 500 ký tự.");
            company.setLogoUrl(logoUrl);
            companyRepository.save(company);
        }

        // Bước 5: xác nhận danh mục tồn tại, còn hoạt động và kỹ năng được chọn hợp lệ.
        JobCategory category = jobCategoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Danh mục nghề nghiệp không tồn tại."));
        if (!"ACTIVE".equalsIgnoreCase(category.getStatus())) throw new IllegalArgumentException("Danh mục nghề nghiệp này hiện không hoạt động.");

        List<Long> selectedSkills = jobSkillService.validate(category.getId(), form.getSkillIds());

        // Bước 6: chuyển dữ liệu DTO sang entity Job; các setter chưa tương đương từng câu SQL.
        // Chủ tin và công ty lấy từ dữ liệu server, không cho form tự quyết định.
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
        // BƯỚC 7: KIỂM TRA QUYỀN GHIM VÀ HẠN MỨC TẠO TIN
        // ==========================================
        // Tìm gói còn hoạt động của recruiter; null nghĩa là không có gói ACTIVE hợp lệ.
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

        // Chỉ tính lượt khi tạo bản ghi mới, kể cả NHÁP. Sửa tin cũ không tính thêm lượt.
        if (form.getJobId() == null) {
            if (activePkg == null) {
                // Chưa có gói -> áp dụng hạn mức bài miễn phí
                long currentCount = jobRepository.countByRecruiter_Id(recruiter.getId());
                if (currentCount >= ServicePackageService.FREE_JOB_LIMIT) {
                    throw new IllegalArgumentException("Bạn đã dùng hết " + ServicePackageService.FREE_JOB_LIMIT + " bài đăng miễn phí. Vui lòng mua gói.");
                }
            } else {
                // Đã có gói -> check snapshot quota
                Integer maxJobs = activePkg.getSnapshotMaxJobs();
                if (maxJobs != null) {
                    int used = activePkg.getUsedJobs() == null ? 0 : activePkg.getUsedJobs();
                    if (used >= maxJobs) {
                        throw new IllegalArgumentException("Bạn đã đăng " + used + "/" + maxJobs + " bài của gói. Vui lòng mua thêm tin.");
                    }
                    // Tăng số lượt đã dùng; nếu thao tác sau đó lỗi runtime, transaction sẽ rollback DB.
                    activePkg.setUsedJobs(used + 1);
                    userPackageRepository.save(activePkg);
                }
                // Nếu maxJobs == null (vô hạn) thì không cần trừ
            }
        }
        // ==========================================

        // Bước 8: nếu request có file thì lưu ẢNH RIÊNG CỦA TIN vào job.logoUrl.
        // Transaction DB không tự xóa file trên ổ đĩa nếu các bước phía sau thất bại.
        if (form.getJobImageFile() != null && !form.getJobImageFile().isEmpty()) {
            try {
                String jobImageUrl = fileStorageService.saveJobImage(form.getJobImageFile());
                job.setLogoUrl(jobImageUrl);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Không thể lưu ảnh Job. Vui lòng thử lại.", ex);
            }
        }

        // Bước 9: gửi -> PENDING và yêu cầu công ty ACTIVE; lưu nháp -> DRAFT.
        // Sửa tin đang ACTIVE cũng đi lại qua hai nhánh này, không giữ trạng thái công khai.
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

        // Bước 10: lưu Job qua JPA, lấy ID để lưu kỹ năng, rồi trả kết quả cho Controller.
        // Controller redirect -> đọc lại danh sách -> Model("jobs") -> jobs.html hiển thị.
        Job saved = jobRepository.save(job);
        jobSkillService.replaceRequired(saved.getId(), selectedSkills);
        return saved;
    }

    // Nút Gửi duyệt ở jobs.html chỉ gửi ID; hàm này đọc nội dung đã lưu từ DB.
    // Luồng: tìm đúng chủ -> kiểm tra nội dung/quan hệ/hạn -> PENDING -> lưu DB.
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

    // Nút Đóng: chỉ chủ tin được đóng tin ACTIVE. Đổi CLOSED, không xóa bản ghi/hồ sơ.
    public Job closeRecruiterJob(Long jobId, User recruiter) {
        if (recruiter == null || recruiter.getId() == null) throw new IllegalArgumentException("Không xác định được tài khoản Recruiter.");
        Job job = getJobForRecruiter(jobId, recruiter.getId());
        if (job.getStatus() != JobStatus.ACTIVE) throw new IllegalArgumentException("Chỉ có thể đóng tin đang ACTIVE.");
        job.setStatus(JobStatus.CLOSED);
        return jobRepository.save(job);
    }

    // Bản không truyền hạn mới dùng lại bản ba tham số với newDeadline = null.
    public Job reopenRecruiterJob(Long jobId, User recruiter) {
        return reopenRecruiterJob(jobId, recruiter, null);
    }

    // Nút Đăng lại: CLOSED/EXPIRED -> DRAFT, giữ ID cũ và phải gửi duyệt lại để công khai.
    // Có newDeadline thì kiểm tra và cập nhật; không có thì giữ hạn cũ để sửa sau.
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

    // Kiểm tra dữ liệu từ form: sai thì ném lỗi để Controller hiện thông báo, chưa lưu thành công.
    // Nháp vẫn cần tiêu đề, danh mục, số lượng và lương hợp lệ; gửi duyệt cần thêm mô tả/deadline.
    // compareTo: < 0 là nhỏ hơn, = 0 là bằng, > 0 là lớn hơn; dùng để so sánh BigDecimal.
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

    // Trạng thái công ty khác trạng thái tin: công ty chưa ACTIVE chỉ được soạn nháp.
    private void ensureCompanyCanPublish(Company company) {
        if (company == null || company.getStatus() != CompanyStatus.ACTIVE) throw new IllegalArgumentException("Công ty chưa được Admin phê duyệt. Bạn chỉ có thể lưu nháp.");
    }

    // Chuẩn hóa trường tùy chọn: "  Hà Nội  " -> "Hà Nội"; chuỗi trống/toàn khoảng trắng -> null.
    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    // Lấy hàng chờ PENDING cùng các quan hệ cần thiết theo query repository.
    @Transactional(readOnly = true)
    public List<Job> getPendingJobs() {
        return jobRepository.findPendingJobsWithDetails();
    }

    // Admin duyệt: kiểm tra lại danh mục/kỹ năng, PENDING, công ty ACTIVE và deadline.
    // Hợp lệ -> ACTIVE, ghi thời điểm công khai. Quyền ADMIN được bảo vệ ở luồng web bên ngoài.
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

    // Kiểm tra dữ liệu liên quan tại thời điểm gửi/duyệt vì danh mục, kỹ năng có thể đã thay đổi.
    private void validatePublishCategoryAndSkills(Job job) {
        if (job.getCategory() == null || !"ACTIVE".equalsIgnoreCase(job.getCategory().getStatus()))
            throw new IllegalArgumentException("Danh mục đã ngừng hoạt động. Hãy chọn danh mục và kỹ năng hợp lệ trước khi gửi hoặc duyệt tin.");
        jobSkillService.validate(job.getCategory().getId(), jobSkillService.requiredIds(job.getId()));
    }

    // Admin từ chối: lý do bắt buộc, tối đa 500 ký tự; chỉ PENDING được chuyển REJECTED.
    // Lưu lý do để recruiter đọc trên màn hình và sửa tin, không xóa nội dung tin.
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

    // Admin đưa tin CLOSED/EXPIRED về DRAFT; query lấy tin có khóa ghi trong transaction.
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

    // Chỉ tìm theo ID, không tự kiểm tra chủ/trạng thái. Optional rỗng nếu không có bản ghi.
    // Bên gọi cần kiểm tra quyền xem và xử lý trường hợp không tìm thấy theo luồng của mình.
    @Transactional(readOnly = true)
    public Optional<Job> findJobById(Long id) {
        return jobRepository.findById(id);
    }
}
