
package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobCategoryAdminService {

    private final JobCategoryRepository jobCategoryRepository;

    public JobCategoryAdminService(
            JobCategoryRepository jobCategoryRepository) {
        this.jobCategoryRepository = jobCategoryRepository;
    }

    public List<JobCategory> findAll() {
        return jobCategoryRepository.findAll();
    }

    public JobCategory findById(Long id) {
        return jobCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy danh mục tuyển dụng."));
    }

    @Transactional
    public JobCategory create(String name, String description) {
        String normalizedName = normalizeName(name);

        if (jobCategoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException(
                    "Tên danh mục đã tồn tại.");
        }

        JobCategory category = new JobCategory();
        category.setName(normalizedName);
        category.setDescription(normalizeDescription(description));
        category.setStatus("ACTIVE");

        // createdAt và updatedAt được thiết lập trong @PrePersist.
        return saveCategory(category);
    }

    @Transactional
    public JobCategory update(
            Long id, String name, String description, String status) {

        JobCategory category = findById(id);
        String normalizedName = normalizeName(name);

        if (jobCategoryRepository
                .existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new IllegalArgumentException(
                    "Tên danh mục đã được sử dụng.");
        }

        category.setName(normalizedName);
        category.setDescription(normalizeDescription(description));
        category.setStatus(normalizeStatus(status));

        // Thay đổi thông tin trên chính ID hiện có; các Job liên kết không bị thay khóa ngoại.
        return saveCategory(category);
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Tên danh mục không được để trống.");
        }

        // Chuẩn hóa khoảng trắng liên tiếp để tránh "IT  Developer" và "IT Developer".
        String normalized = name.trim().replaceAll("\\s+", " ");

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "Tên danh mục không được vượt quá 150 ký tự.");
        }

        return normalized;
    }

    private String normalizeStatus(String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new IllegalArgumentException("Trạng thái danh mục không hợp lệ.");
        }
        return status;
    }

    // Flush ngay trong service để bắt được lỗi UNIQUE từ SQL Server trước khi trả thành công.
    private JobCategory saveCategory(JobCategory category) {
        try {
            return jobCategoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Không thể lưu danh mục. Tên danh mục có thể đã tồn tại hoặc dữ liệu không hợp lệ.", ex);
        } catch (DataAccessException ex) {
            throw new IllegalArgumentException(
                    "Lưu danh mục thất bại do lỗi cơ sở dữ liệu. Vui lòng thử lại sau.", ex);
        }
    }

    private String normalizeDescription(String description) {
        return description == null || description.trim().isEmpty()
                ? null
                : description.trim();
    }
}
