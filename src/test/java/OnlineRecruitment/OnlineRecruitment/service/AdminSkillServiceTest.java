package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.entity.Skill;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobSkillRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.SkillRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSkillServiceTest {
    SkillRepository skills = mock(SkillRepository.class);
    JobCategoryRepository categories = mock(JobCategoryRepository.class);
    JobSkillRepository jobSkills = mock(JobSkillRepository.class);
    AdminSkillService service = new AdminSkillService(skills, categories, jobSkills);

    JobCategory category(String status) {
        JobCategory category = new JobCategory(); category.setId(3L); category.setStatus(status);
        return category;
    }

    @Test
    void normalizedDuplicateIsRejectedAcrossCategoriesToRespectDatabaseUniqueName() {
        when(categories.findById(3L)).thenReturn(Optional.of(category("ACTIVE")));
        when(skills.existsByNameIgnoreCase("Java")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(" Java ", 3L));
        verify(skills, never()).saveAndFlush(any());
    }

    @Test
    void reactivationPreservesSkillIdAndCategory() {
        Skill skill = new Skill(5L, "Java", "INACTIVE"); skill.setCategory(category("ACTIVE"));
        when(skills.findById(5L)).thenReturn(Optional.of(skill));
        when(categories.findById(3L)).thenReturn(Optional.of(skill.getCategory()));
        service.update(5L, "  Java  ", 3L, "ACTIVE");
        assertEquals(5L, skill.getId()); assertEquals("ACTIVE", skill.getStatus());
        assertEquals("Java", skill.getName()); assertEquals(3L, skill.getCategory().getId());
    }

    @Test
    void invalidStatusCannotBeSaved() {
        when(skills.findById(5L)).thenReturn(Optional.of(new Skill()));
        assertThrows(IllegalArgumentException.class, () -> service.update(5L, "Java", 3L, "DELETED"));
        verify(skills, never()).saveAndFlush(any());
    }

    @Test
    void usedSkillCannotMoveCategory() {
        Skill skill = new Skill(5L, "Java", "ACTIVE"); skill.setCategory(category("ACTIVE"));
        when(skills.findById(5L)).thenReturn(Optional.of(skill)); when(jobSkills.isUsed(5L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.update(5L, "Java", 9L, "ACTIVE"));
        verify(skills, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsInactiveCategory() {
        when(categories.findById(3L)).thenReturn(Optional.of(category("INACTIVE")));
        assertThrows(IllegalArgumentException.class, () -> service.create("Java", 3L));
    }

    @Test
    void canDeactivateSkillInInactiveCategoryWithoutDeletingReferences() {
        Skill skill = new Skill(5L, "Java", "ACTIVE"); skill.setCategory(category("INACTIVE"));
        when(skills.findById(5L)).thenReturn(Optional.of(skill));
        when(categories.findById(3L)).thenReturn(Optional.of(skill.getCategory()));
        service.update(5L, "Java", 3L, "INACTIVE");
        assertEquals("INACTIVE", skill.getStatus());
        verify(skills).saveAndFlush(skill);
        verify(skills, never()).delete(any());
    }
}
