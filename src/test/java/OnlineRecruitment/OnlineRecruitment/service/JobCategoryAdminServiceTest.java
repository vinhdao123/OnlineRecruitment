package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
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
class JobCategoryAdminServiceTest {
    @Mock JobCategoryRepository repository;
    @InjectMocks JobCategoryAdminService service;

    @Test void CAT_01_createNormalizesAndActivates() {
        when(repository.saveAndFlush(any(JobCategory.class))).thenAnswer(i -> i.getArgument(0));
        JobCategory saved = service.create("  Công nghệ   thông tin ", "  Java backend  ");
        assertEquals("Công nghệ thông tin", saved.getName());
        assertEquals("Java backend", saved.getDescription());
        assertEquals("ACTIVE", saved.getStatus());
        verify(repository).saveAndFlush(any(JobCategory.class));
    }
    @Test void CAT_02_rejectsDuplicate() {
        when(repository.existsByNameIgnoreCase("IT")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(" IT ", null));
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void CAT_03_rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> service.create(" ", null));
        verifyNoInteractions(repository);
    }
    @Test void CAT_04_updatesExistingWithoutChangingId() {
        JobCategory existing = new JobCategory(); existing.setId(5L); existing.setStatus("ACTIVE");
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(JobCategory.class))).thenAnswer(i -> i.getArgument(0));
        JobCategory result = service.update(5L, "  Design ", "updated", "INACTIVE");
        assertEquals(5L, result.getId());
        assertEquals("Design", result.getName());
        assertEquals("INACTIVE", result.getStatus());
    }
    @Test void CAT_05_rejectsInvalidStatus() {
        when(repository.findById(5L)).thenReturn(Optional.of(new JobCategory()));
        assertThrows(IllegalArgumentException.class, () -> service.update(5L, "IT", "", "DELETED"));
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void CAT_06_missingCategory() {
        assertThrows(IllegalArgumentException.class, () -> service.findById(999L));
    }
}
