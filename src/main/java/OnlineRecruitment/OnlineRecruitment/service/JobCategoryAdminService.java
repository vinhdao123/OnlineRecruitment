
package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
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
        return jobCategoryRepository.save(category);
    }

    @Transactional
    public JobCategory update(
            Long id, String name, String description) {

        JobCategory category = findById(id);
        String normalizedName = normalizeName(name);

        if (jobCategoryRepository
                .existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new IllegalArgumentException(
                    "Tên danh mục đã được sử dụng.");
        }

        category.setName(normalizedName);
        category.setDescription(normalizeDescription(description));

        // @PreUpdate sẽ tự cập nhật updatedAt.
        return jobCategoryRepository.save(category);
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Tên danh mục không được để trống.");
        }

        String normalized = name.trim();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "Tên danh mục không được vượt quá 150 ký tự.");
        }

        return normalized;
    }

    private String normalizeDescription(String description) {
        return description == null || description.trim().isEmpty()
                ? null
                : description.trim();
    }
}
