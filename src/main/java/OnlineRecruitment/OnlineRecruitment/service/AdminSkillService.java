package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.entity.Skill;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Giữ quy tắc của bản cũ, tương thích UNIQUE(name) trong database bản main. */
@Service
public class AdminSkillService {
    private final SkillRepository skillRepository;
    private final JobCategoryRepository categoryRepository;
    private final OnlineRecruitment.OnlineRecruitment.repository.JobSkillRepository jobSkills;

    public AdminSkillService(SkillRepository skillRepository, JobCategoryRepository categoryRepository,
                             OnlineRecruitment.OnlineRecruitment.repository.JobSkillRepository jobSkills) {
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.jobSkills = jobSkills;
    }

    @Transactional(readOnly = true)
    public List<Skill> getAll() { return skillRepository.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public Skill getById(Long id) {
        return skillRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỹ năng."));
    }

    @Transactional(readOnly = true)
    public List<JobCategory> getCategories() { return categoryRepository.findAll(); }

    @Transactional
    public void create(String name, Long categoryId) {
        String cleaned = cleanName(name);
        JobCategory category = activeCategory(categoryId);
        if (skillRepository.existsByNameIgnoreCase(cleaned))
            throw new IllegalArgumentException("Tên kỹ năng đã tồn tại. Hãy sửa hoặc kích hoạt lại kỹ năng cũ.");
        Skill skill = new Skill();
        skill.setName(cleaned);
        skill.setStatus("ACTIVE");
        skill.setCategory(category);
        saveSkill(skill);
    }

    @Transactional
    public void update(Long id, String name, Long categoryId, String status) {
        Skill skill = getById(id);
        Long oldCategory = skill.getCategory() == null ? null : skill.getCategory().getId();
        if (!java.util.Objects.equals(oldCategory, categoryId) && jobSkills.isUsed(id))
            throw new IllegalArgumentException("Kỹ năng đang được dùng trong tin tuyển dụng, không thể chuyển sang danh mục khác.");
        String cleaned = cleanName(name);
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status))
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        if (skillRepository.existsByNameIgnoreCaseAndIdNot(cleaned, id))
            throw new IllegalArgumentException("Tên kỹ năng đã tồn tại. Hãy sửa kỹ năng cũ.");
        // Khi cập nhật kỹ năng đang ACTIVE, Category phải còn hoạt động.
        JobCategory category = "ACTIVE".equals(status) ? activeCategory(categoryId)
                : categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại."));
        skill.setName(cleaned);
        skill.setCategory(category);
        skill.setStatus(status);
        saveSkill(skill);
    }

    private void saveSkill(Skill skill) {
        try {
            skillRepository.saveAndFlush(skill);
        } catch (org.springframework.dao.DataAccessException ex) {
            throw new IllegalArgumentException("Không thể lưu kỹ năng. Tên có thể đã tồn tại hoặc dữ liệu không hợp lệ.", ex);
        }
    }

    private JobCategory activeCategory(Long id) {
        if (id == null) throw new IllegalArgumentException("Vui lòng chọn danh mục ngành nghề.");
        JobCategory category = categoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại."));
        if (!"ACTIVE".equals(category.getStatus())) throw new IllegalArgumentException("Danh mục đã ngừng hoạt động.");
        return category;
    }

    private String cleanName(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Tên kỹ năng không được để trống.");
        String name = input.trim().replaceAll("\\s+", " ");
        if (name.length() > 150) throw new IllegalArgumentException("Tên kỹ năng tối đa 150 ký tự.");
        return name;
    }
}
