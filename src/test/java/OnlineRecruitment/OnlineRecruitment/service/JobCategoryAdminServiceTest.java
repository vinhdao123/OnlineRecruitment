package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobCategoryAdminServiceTest {
    JobCategoryRepository repository = mock(JobCategoryRepository.class);
    JobCategoryAdminService service = new JobCategoryAdminService(repository);

    @Test
    void normalizedDuplicateNameIsRejected() {
        when(repository.existsByNameIgnoreCase("Công nghệ thông tin")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create("  Công nghệ   thông tin ", null));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deactivateAndReactivatePreserveCategoryId() {
        JobCategory category = new JobCategory(); category.setId(3L); category.setName("IT");
        when(repository.findById(3L)).thenReturn(Optional.of(category));
        when(repository.saveAndFlush(category)).thenReturn(category);
        assertEquals("INACTIVE", service.update(3L, "IT", null, "INACTIVE").getStatus());
        assertEquals("ACTIVE", service.update(3L, "IT", null, "ACTIVE").getStatus());
        assertEquals(3L, category.getId());
    }

    @Test
    void invalidStatusCannotBeSaved() {
        when(repository.findById(3L)).thenReturn(Optional.of(new JobCategory()));
        assertThrows(IllegalArgumentException.class, () -> service.update(3L, "IT", null, "DELETED"));
        verify(repository, never()).saveAndFlush(any());
    }
}
