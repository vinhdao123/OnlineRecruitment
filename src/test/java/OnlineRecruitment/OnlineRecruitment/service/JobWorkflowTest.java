package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.RecruiterJobForm;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobWorkflowTest {
    @Mock JobRepository jobs;
    @Mock JobCategoryRepository categories;
    @Mock CompanyRepository companies;
    @Mock FileStorageService files;
    @InjectMocks JobService service;
    User recruiter;
    Company company;
    JobCategory category;
    Job job;

    @BeforeEach
    void setUp() {
        recruiter = new User();
        recruiter.setId(1L);
        recruiter.setRole(UserRole.RECRUITER);
        recruiter.setStatus(UserStatus.ACTIVE);
        company = new Company();
        company.setId(2L);
        company.setStatus(CompanyStatus.ACTIVE);
        category = new JobCategory();
        category.setId(3L);
        category.setStatus("ACTIVE");
        job = new Job();
        job.setId(4L);
        job.setRecruiter(recruiter);
        job.setCompany(company);
        job.setCategory(category);
        job.setTitle("Java Developer");
        job.setDescription("Phát triển ứng dụng");
        job.setRequirements("Java và SQL");
        job.setLocation("Hồ Chí Minh");
        job.setQuantity(2);
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setSalaryCurrency("VND");
        job.setDeadline(LocalDate.now().plusDays(10));
    }

    void locked() {
        when(jobs.findByIdForUpdate(4L)).thenReturn(Optional.of(job));
    }

    void saved() {
        when(jobs.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @EnumSource(value = JobStatus.class, names = {"CLOSED", "EXPIRED"})
    void reopenExpiredOrClosedReturnsDraftWithoutChangingDeadlineOrIdentity(JobStatus status) {
        job.setStatus(status);
        LocalDate oldDeadline = LocalDate.now().minusDays(3);
        job.setDeadline(oldDeadline);
        job.setPublishedAt(LocalDateTime.now().minusDays(30));
        locked(); saved();
        Job result = service.reopenRecruiterJob(4L, recruiter);
        assertSame(job, result);
        assertEquals(4L, result.getId());
        assertEquals(oldDeadline, result.getDeadline());
        assertEquals(JobStatus.DRAFT, result.getStatus());
        assertNull(result.getPublishedAt());
    }

    @Test
    void adminCanReturnExpiredJobToDraftWithoutPublishingIt() {
        job.setStatus(JobStatus.EXPIRED);
        job.setDeadline(LocalDate.now().minusDays(1));
        locked(); saved();
        assertEquals(JobStatus.DRAFT, service.reopenAdminJob(4L).getStatus());
    }

    @Test
    void anotherRecruiterCannotReopenJob() {
        job.setStatus(JobStatus.CLOSED);
        User other = new User(); other.setId(99L);
        locked();
        assertThrows(IllegalArgumentException.class, () -> service.reopenRecruiterJob(4L, other));
        verify(jobs, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = JobStatus.class, names = {"DRAFT", "PENDING", "ACTIVE", "REJECTED"})
    void adminCannotReopenOtherStates(JobStatus status) {
        job.setStatus(status); locked();
        assertThrows(IllegalArgumentException.class, () -> service.reopenAdminJob(4L));
        verify(jobs, never()).save(any());
    }

    @Test
    void approvalRejectsExpiredDeadlineAndDoesNotSave() {
        job.setStatus(JobStatus.PENDING);
        job.setDeadline(LocalDate.now().minusDays(1)); locked();
        assertThrows(IllegalArgumentException.class, () -> service.approveJob(4L));
        assertEquals(JobStatus.PENDING, job.getStatus());
        verify(jobs, never()).save(any());
    }

    @Test
    void approvalRejectsInactiveCategory() {
        job.setStatus(JobStatus.PENDING); category.setStatus("INACTIVE"); locked();
        assertThrows(IllegalArgumentException.class, () -> service.approveJob(4L));
    }

    @Test
    void submissionRejectsInactiveCategoryToo() {
        job.setStatus(JobStatus.DRAFT); category.setStatus("INACTIVE"); locked();
        assertThrows(IllegalArgumentException.class, () -> service.submitExistingJob(4L, recruiter));
    }

    @Test
    void approvalRejectsLockedRecruiter() {
        job.setStatus(JobStatus.PENDING); recruiter.setStatus(UserStatus.LOCKED); locked();
        assertThrows(IllegalArgumentException.class, () -> service.approveJob(4L));
    }

    @Test
    void approvalAllowsDeadlineTodayAndSetsPublicationTime() {
        job.setStatus(JobStatus.PENDING); job.setDeadline(LocalDate.now()); locked(); saved();
        assertEquals(JobStatus.ACTIVE, service.approveJob(4L).getStatus());
        assertNotNull(job.getPublishedAt());
        assertThrows(IllegalArgumentException.class, () -> service.approveJob(4L));
    }

    @Test
    void submitIncompleteDraftDoesNotEnterReviewQueue() {
        job.setStatus(JobStatus.DRAFT); job.setRequirements(null); locked();
        assertThrows(IllegalArgumentException.class, () -> service.submitExistingJob(4L, recruiter));
        assertEquals(JobStatus.DRAFT, job.getStatus());
    }

    @Test
    void draftCanSaveIncompleteDescriptionAndPastDeadline() {
        RecruiterJobForm form = form();
        form.setDescription(null);
        form.setDeadline(LocalDate.now().minusDays(5));
        when(companies.findByRecruiterId(1L)).thenReturn(Optional.of(company));
        when(categories.findById(3L)).thenReturn(Optional.of(category)); saved();
        Job result = service.saveRecruiterJob(recruiter, form, false);
        assertEquals(JobStatus.DRAFT, result.getStatus());
        assertEquals("", result.getDescription());
    }

    @ParameterizedTest
    @EnumSource(value = JobStatus.class, names = {"ACTIVE", "PENDING"})
    void explicitSaveDraftWithdrawsLiveOrPendingJob(JobStatus status) {
        job.setStatus(status);
        RecruiterJobForm form = form(); form.setJobId(4L);
        when(companies.findByRecruiterId(1L)).thenReturn(Optional.of(company));
        when(categories.findById(3L)).thenReturn(Optional.of(category)); locked(); saved();
        assertEquals(JobStatus.DRAFT, service.saveRecruiterJob(recruiter, form, false).getStatus());
        assertNull(job.getPublishedAt());
    }

    @Test
    void submitRequiresDeadline() {
        RecruiterJobForm form = form(); form.setDescription("Mô tả");
        form.setLocation("Remote"); form.setRequirements("Java");
        assertThrows(IllegalArgumentException.class, () -> service.saveRecruiterJob(recruiter, form, true));
        verify(jobs, never()).save(any());
    }

    @Test
    void schedulerUsesConditionalExpirationUpdate() {
        new JobAutoCloseScheduler(jobs).closeExpiredJobs();
        verify(jobs).expireActiveJobs(LocalDate.now());
    }

    RecruiterJobForm form() {
        RecruiterJobForm form = new RecruiterJobForm();
        form.setTitle("Java Developer"); form.setCategoryId(3L);
        return form;
    }
}
