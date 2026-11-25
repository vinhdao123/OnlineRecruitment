package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Skill;
import OnlineRecruitment.OnlineRecruitment.repository.JobSkillRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JobSkillService {
    private final SkillRepository skills;
    private final JobSkillRepository links;
    public record Option(Long id, String name, Long categoryId, boolean active) {}

    @Transactional(readOnly = true)
    public List<Option> options(List<Long> selectedIds) {
        Map<Long, Skill> available = new LinkedHashMap<>();
        skills.findByStatusAndCategory_StatusOrderByNameAsc("ACTIVE", "ACTIVE").forEach(s -> available.put(s.getId(), s));
        // Keep obsolete selections visible so the recruiter can explicitly remove them.
        if (selectedIds != null && !selectedIds.isEmpty())
            skills.findAllById(selectedIds).forEach(s -> available.putIfAbsent(s.getId(), s));
        return available.values().stream().map(s -> new Option(s.getId(), s.getName(),
                s.getCategory() == null ? null : s.getCategory().getId(),
                "ACTIVE".equals(s.getStatus()) && s.getCategory() != null && "ACTIVE".equals(s.getCategory().getStatus()))).toList();
    }

    @Transactional(readOnly = true)
    public List<Long> validate(Long categoryId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        if (ids.size() > 100 || ids.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Danh sách kỹ năng không hợp lệ (tối đa 100 kỹ năng).");
        List<Long> unique = new ArrayList<>(new LinkedHashSet<>(ids));
        List<Skill> selected = skills.findAllById(unique);
        if (selected.size() != unique.size()) throw new IllegalArgumentException("Có kỹ năng không tồn tại.");
        for (Skill skill : selected) {
            if (!"ACTIVE".equals(skill.getStatus()) || skill.getCategory() == null
                    || !Objects.equals(categoryId, skill.getCategory().getId())
                    || !"ACTIVE".equals(skill.getCategory().getStatus()))
                throw new IllegalArgumentException("Chỉ được chọn kỹ năng đang hoạt động và thuộc danh mục công việc đã chọn.");
        }
        return unique;
    }

    @Transactional(readOnly = true)
    public List<Long> requiredIds(Long jobId) { return links.findRequiredSkillIds(jobId); }

    @Transactional(readOnly = true)
    public List<String> requiredNames(Long jobId) {
        List<Long> ids = links.findRequiredSkillIds(jobId);
        if (ids.isEmpty()) return List.of();
        return skills.findAllById(ids).stream().map(Skill::getName).sorted().toList();
    }

    @Transactional
    public void replaceRequired(Long jobId, List<Long> ids) { links.replaceRequired(jobId, ids); }
}
