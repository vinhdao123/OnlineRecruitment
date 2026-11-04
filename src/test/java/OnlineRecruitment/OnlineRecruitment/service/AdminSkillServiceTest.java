package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.entity.Skill;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminSkillServiceTest {
    @Mock SkillRepository skills;
    @Mock JobCategoryRepository categories;
    @InjectMocks AdminSkillService service;

    private JobCategory category(String status) {
        JobCategory c = new JobCategory(); c.setId(2L); c.setStatus(status); return c;
    }
    @Test void SK_01_createSkillInActiveCategory() {
        when(categories.findById(2L)).thenReturn(Optional.of(category("ACTIVE")));
        service.create("  Spring   Boot ", 2L);
        verify(skills).save(argThat(s -> s.getName().equals("Spring Boot")
          && s.getStatus().equals("ACTIVE") && s.getCategory().getId().equals(2L)));
    }
    @Test void SK_02_rejectDuplicateInCategory() {
        when(categories.findById(2L)).thenReturn(Optional.of(category("ACTIVE")));
        when(skills.existsByNameIgnoreCaseAndCategoryId("Java",2L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create("Java",2L));
        verify(skills,never()).save(any());
    }
    @Test void SK_03_rejectInactiveCategory() {
        when(categories.findById(2L)).thenReturn(Optional.of(category("INACTIVE")));
        assertThrows(IllegalArgumentException.class, () -> service.create("Java",2L));
        verify(skills,never()).save(any());
    }
    @Test void SK_04_updateAndDeactivateSkill() {
        Skill skill = new Skill(); skill.setId(8L); skill.setStatus("ACTIVE");
        when(skills.findById(8L)).thenReturn(Optional.of(skill));
        when(categories.findById(2L)).thenReturn(Optional.of(category("INACTIVE")));
        service.update(8L,"  SQL  Server ",2L,"INACTIVE");
        assertEquals("SQL Server",skill.getName());
        assertEquals("INACTIVE",skill.getStatus());
        verify(skills).save(skill);
    }
    @Test void SK_05_rejectInvalidStatus() {
        when(skills.findById(8L)).thenReturn(Optional.of(new Skill()));
        assertThrows(IllegalArgumentException.class, () -> service.update(8L,"Git",2L,"DELETED"));
        verify(skills,never()).save(any());
    }
    @Test void SK_06_rejectBlankSkillName() {
        assertThrows(IllegalArgumentException.class, () -> service.create("  ",2L));
        verifyNoInteractions(categories,skills);
    }
}
