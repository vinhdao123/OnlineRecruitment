
package OnlineRecruitment.OnlineRecruitment.service.serviceJob;

import OnlineRecruitment.OnlineRecruitment.dto.JobForm;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Job2;
import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Skill;

import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.CategoryRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.SkillRepository;

import OnlineRecruitment.OnlineRecruitment.util.SalaryRanges;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class JobService {

    private static final int PAGE_SIZE = 6;
    private static final int MANAGE_PAGE_SIZE = 10;

    private static final Set<String> STATUSES = Set.of(
            "DRAFT",
            "PENDING",
            "APPROVED",
            "REJECTED",
            "CLOSED"
    );

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;

    public JobService(
            JobRepository jobRepository,
            CompanyRepository companyRepository,
            CategoryRepository categoryRepository,
            SkillRepository skillRepository) {

        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
    }

    // ================= PHÂN QUYỀN NHÀ TUYỂN DỤNG =================

    public boolean canManage(Job2 job, Long userId) {
        return job != null
                && userId != null
                && job.getRecruiterId() != null
                && userId.equals(job.getRecruiterId());
    }

    public boolean canEdit(Job2 job, Long userId) {
        return canManage(job, userId)
                && !"CLOSED".equals(job.getStatus());
    }

    private Job2 loadOwned(Long id, Long userId) {
        Job2 job = getById(id);

        if (!canManage(job, userId)) {
            throw new AccessDeniedException(
                    "Bạn không có quyền thao tác trên tin này"
            );
        }

        return job;
    }

    // ================= TÌM KIẾM VÀ ĐỌC DỮ LIỆU =================

    @Transactional(readOnly = true)
    public Page<Job2> search(
            String keyword,
            String location,
            int page) {

        return jobRepository.search(
                clean(keyword),
                clean(location),
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<Job2> findMine(
            Long recruiterId,
            String keyword,
            String status,
            int page) {

        return jobRepository.findMine(
                recruiterId,
                clean(keyword),
                cleanStatus(status),
                PageRequest.of(
                        Math.max(page, 0),
                        MANAGE_PAGE_SIZE
                )
        );
    }

    @Transactional(readOnly = true)
    public Job2 getById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy công việc: " + id
                ));
    }

    @Transactional(readOnly = true)
    public List<String> getLocations() {
        return jobRepository.findAllLocations();
    }

    @Transactional(readOnly = true)
    public List<JobCategory> getCategories() {
        return categoryRepository.findByStatusOrderByName("ACTIVE");
    }

    @Transactional(readOnly = true)
    public List<Skill> getSkills() {
        return skillRepository.findByStatusOrderByName("ACTIVE");
    }

    // ================= TẠO TIN TUYỂN DỤNG =================

    @Transactional
    public Job2 create(JobForm form, Long recruiterId) {
        if (recruiterId == null) {
            throw new AccessDeniedException(
                    "Bạn cần đăng nhập bằng tài khoản nhà tuyển dụng"
            );
        }

        Company company = companyRepository
                .findFirstByRecruiterId(recruiterId)
                .orElseThrow(() -> new IllegalStateException(
                        "Nhà tuyển dụng chưa có hồ sơ công ty"
                ));

        Job2 job = new Job2();
        job.setCompany(company);
        job.setRecruiterId(recruiterId);

        apply(job, form);

        return jobRepository.save(job);
    }

    // ================= LẤY DỮ LIỆU FORM CHỈNH SỬA =================

    @Transactional(readOnly = true)
    public JobForm getForm(Long id, Long userId) {
        Job2 job = loadOwned(id, userId);

        if (!canEdit(job, userId)) {
            throw new AccessDeniedException(
                    "Tin đã đóng, không thể chỉnh sửa"
            );
        }

        JobForm form = new JobForm();

        form.setTitle(job.getTitle());
        form.setLevel(job.getLevel());

        if (job.getCategory() != null) {
            form.setCategoryId(job.getCategory().getId());
        }

        form.setLocation(job.getLocation());
        form.setQuantity(job.getQuantity());
        form.setDeadline(job.getDeadline());

        if (job.getSkills() != null) {
            form.setSkillIds(
                    job.getSkills()
                            .stream()
                            .map(Skill::getId)
                            .toList()
            );
        }

        form.setSalary(job.getSalary());
        form.setExperience(job.getExperience());
        form.setDescription(job.getDescription());
        form.setRequirements(job.getRequirements());
        form.setBenefits(job.getBenefits());

        return form;
    }

    // ================= CẬP NHẬT TIN =================

    @Transactional
    public Job2 update(Long id, JobForm form, Long userId) {
        Job2 job = loadOwned(id, userId);

        if (!canEdit(job, userId)) {
            throw new AccessDeniedException(
                    "Tin đã đóng, không thể chỉnh sửa"
            );
        }

        apply(job, form);

        return jobRepository.save(job);
    }

    // ================= ĐÓNG TIN =================

    @Transactional
    public void close(Long id, Long userId) {
        Job2 job = loadOwned(id, userId);

        job.setStatus("CLOSED");
        jobRepository.save(job);
    }

    // ================= XÓA TIN =================

    @Transactional
    public void delete(Long id, Long userId) {
        Job2 job = loadOwned(id, userId);

        jobRepository.delete(job);
        jobRepository.flush();
    }

    // ================= ADMIN DUYỆT TIN =================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<Job2> findForReview(
            String keyword,
            String status,
            int page) {

        return jobRepository.findForReview(
                clean(keyword),
                cleanStatus(status),
                PageRequest.of(
                        Math.max(page, 0),
                        MANAGE_PAGE_SIZE
                )
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void approve(Long id) {
        Job2 job = getById(id);

        requirePending(job);

        job.setStatus("APPROVED");
        job.setRejectionReason(null);

        jobRepository.save(job);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void reject(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập lý do từ chối"
            );
        }

        Job2 job = getById(id);

        requirePending(job);

        job.setStatus("REJECTED");
        job.setRejectionReason(reason.trim());

        jobRepository.save(job);
    }

    private void requirePending(Job2 job) {
        if (!"PENDING".equals(job.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ xử lý được tin đang chờ duyệt"
            );
        }
    }

    // ================= HÀM TIỆN ÍCH =================

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanStatus(String status) {
        String value = clean(status).toUpperCase();

        return STATUSES.contains(value) ? value : "";
    }

    // ================= ÁP DỤNG DỮ LIỆU FORM =================

    private void apply(Job2 job, JobForm form) {
        if (form == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu tin tuyển dụng không hợp lệ"
            );
        }

        if (form.getTitle() == null
                || form.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập tiêu đề công việc"
            );
        }

        if (form.getDescription() == null
                || form.getRequirements() == null) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập mô tả và yêu cầu công việc"
            );
        }

        if (form.getCategoryId() == null) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn danh mục công việc"
            );
        }

        job.setTitle(form.getTitle().trim());
        job.setLevel(form.getLevel());

        // Job2 sử dụng entity JobCategory thuộc package entity.
        JobCategory category =
                categoryRepository.getReferenceById(
                        form.getCategoryId()
                );

        job.setCategory(category);

        job.setLocation(form.getLocation());
        job.setQuantity(form.getQuantity());
        job.setDeadline(form.getDeadline());

        if (job.getSkills() == null) {
            throw new IllegalStateException(
                    "Danh sách skills của Job2 chưa được khởi tạo"
            );
        }

        job.getSkills().clear();

        if (form.getSkillIds() != null
                && !form.getSkillIds().isEmpty()) {
            job.getSkills().addAll(
                    skillRepository.findAllById(form.getSkillIds())
            );
        }

        BigDecimal[] range =
                SalaryRanges.toRange(form.getSalary());

        job.setSalaryMin(range[0]);
        job.setSalaryMax(range[1]);

        job.setExperience(form.getExperience());
        job.setDescription(form.getDescription().trim());
        job.setRequirements(form.getRequirements().trim());
        job.setBenefits(form.getBenefits());

        // Tạo mới hoặc sửa tin thì gửi Admin duyệt lại.
        job.setStatus("PENDING");
        job.setRejectionReason(null);
    }
    // ================= THỐNG KÊ DASHBOARD =================

    @Transactional(readOnly = true)
    public long totalJobs() {
        return jobRepository.count();
    }

    @Transactional(readOnly = true)
    public long jobsByStatus(JobStatus status) {
        return status == null
                ? 0
                : jobRepository.countByStatus(status.name());
    }

}