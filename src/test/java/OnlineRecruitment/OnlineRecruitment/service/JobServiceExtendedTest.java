package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.RecruiterJobForm;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Additional business-branch coverage for JobService; no real database is touched. */
@ExtendWith(MockitoExtension.class)
class JobServiceExtendedTest {
    @Mock JobRepository jobs;
    @Mock JobCategoryRepository categories;
    @Mock CompanyRepository companies;
    @InjectMocks JobService service;

    private User recruiter;
    private Company company;
    private JobCategory category;
    private RecruiterJobForm form;

    @BeforeEach void setup() {
        recruiter = new User(); recruiter.setId(10L);
        company = new Company(); company.setStatus(CompanyStatus.ACTIVE);
        category = new JobCategory(); category.setId(2L); category.setStatus("ACTIVE");
        form = new RecruiterJobForm(); form.setCategoryId(2L);
        form.setTitle(" Java Developer "); form.setDescription(" Develop services ");
        form.setQuantity(2); form.setDeadline(LocalDate.now().plusDays(10));
    }
    private void lookupForCreate() {
        when(companies.findByRecruiterId(10L)).thenReturn(Optional.of(company));
        when(categories.findById(2L)).thenReturn(Optional.of(category));
        when(jobs.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
    }
    private Job existing(JobStatus status) {
        Job j = new Job(); j.setId(7L); j.setStatus(status); j.setCompany(company);
        j.setCategory(category); j.setTitle("Old title"); j.setDescription("Old description");
        j.setDeadline(LocalDate.now().plusDays(5));
        return j;
    }
    @Test void createDraftSetsDefaultsAndTrimsValues() {
        lookupForCreate();
        Job j = service.saveRecruiterJob(recruiter, form, false);
        assertEquals(JobStatus.DRAFT, j.getStatus());
        assertEquals("Java Developer", j.getTitle());
        assertEquals("Develop services", j.getDescription());
        assertEquals("VND", j.getSalaryCurrency());
        assertEquals(2, j.getQuantity());
        assertSame(category, j.getCategory());
        assertSame(company, j.getCompany());
    }
    @Test void createAndSubmitMovesToPending() {
        lookupForCreate();
        assertEquals(JobStatus.PENDING,service.saveRecruiterJob(recruiter,form,true).getStatus());
    }
    @Test void inactiveCompanyMaySaveDraftButCannotSubmit() {
        company.setStatus(CompanyStatus.PENDING);
        lookupForCreate();
        assertEquals(JobStatus.DRAFT,service.saveRecruiterJob(recruiter,form,false).getStatus());
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,true));
    }
    @Test void editingActiveJobAlwaysRequiresReapproval() {
        form.setJobId(7L);
        Job old = existing(JobStatus.ACTIVE);
        when(companies.findByRecruiterId(10L)).thenReturn(Optional.of(company));
        when(jobs.findByIdAndRecruiter_Id(7L,10L)).thenReturn(Optional.of(old));
        when(categories.findById(2L)).thenReturn(Optional.of(category));
        when(jobs.save(old)).thenReturn(old);
        assertEquals(JobStatus.PENDING,service.saveRecruiterJob(recruiter,form,false).getStatus());
    }
    @Test void cannotEditClosedJob() {
        form.setJobId(7L);
        when(companies.findByRecruiterId(10L)).thenReturn(Optional.of(company));
        when(jobs.findByIdAndRecruiter_Id(7L,10L)).thenReturn(Optional.of(existing(JobStatus.CLOSED)));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        verify(jobs,never()).save(any());
    }
    @Test void cannotCreateWithInactiveCategory() {
        category.setStatus("INACTIVE");
        when(companies.findByRecruiterId(10L)).thenReturn(Optional.of(company));
        when(categories.findById(2L)).thenReturn(Optional.of(category));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
    }
    @Test void cannotCreateWhenCategoryMissing() {
        when(companies.findByRecruiterId(10L)).thenReturn(Optional.of(company));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
    }
    @Test void cannotCreateWhenCompanyMissing() {
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
    }
    @Test void recruiterAndFormAreRequired() {
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(null,form,false));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,null,false));
        verifyNoInteractions(jobs,companies,categories);
    }
    @Test void validatesTitleDescriptionCategoryAndQuantity() {
        form.setTitle(" ");
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        form.setTitle("x".repeat(256));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        form.setTitle("Valid"); form.setDescription(null);
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        form.setDescription("Valid"); form.setCategoryId(null);
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        form.setCategoryId(2L); form.setQuantity(0);
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        verifyNoInteractions(jobs,companies,categories);
    }
    @Test void validatesSalaryAndDeadline() {
        form.setSalaryMin(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        form.setSalaryMin(BigDecimal.ZERO); form.setSalaryMax(new BigDecimal("-3"));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        form.setSalaryMin(new BigDecimal("2000")); form.setSalaryMax(new BigDecimal("1000"));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
        form.setSalaryMax(new BigDecimal("3000")); form.setDeadline(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class,()->service.saveRecruiterJob(recruiter,form,false));
    }
    @Test void submitExistingDraft() {
        Job j=existing(JobStatus.DRAFT);
        when(jobs.findByIdAndRecruiter_Id(7L,10L)).thenReturn(Optional.of(j));
        when(jobs.save(j)).thenReturn(j);
        assertEquals(JobStatus.PENDING,service.submitExistingJob(7L,recruiter).getStatus());
    }
    @Test void submitRejectedJobClearsReason() {
        Job j=existing(JobStatus.REJECTED); j.setRejectionReason("Old reason");
        when(jobs.findByIdAndRecruiter_Id(7L,10L)).thenReturn(Optional.of(j));
        when(jobs.save(j)).thenReturn(j);
        assertEquals(JobStatus.PENDING,service.submitExistingJob(7L,recruiter).getStatus());
        assertNull(j.getRejectionReason());
    }
    @Test void submitRejectsLiveMissingFieldsAndExpiredDeadline() {
        Job j=existing(JobStatus.ACTIVE);
        when(jobs.findByIdAndRecruiter_Id(7L,10L)).thenReturn(Optional.of(j));
        assertThrows(IllegalArgumentException.class,()->service.submitExistingJob(7L,recruiter));
        j.setStatus(JobStatus.DRAFT); j.setTitle(null);
        assertThrows(IllegalArgumentException.class,()->service.submitExistingJob(7L,recruiter));
        j.setTitle("Valid"); j.setDeadline(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class,()->service.submitExistingJob(7L,recruiter));
        verify(jobs,never()).save(any());
    }
    @Test void submitRejectsInactiveCompany() {
        Job j=existing(JobStatus.DRAFT); company.setStatus(CompanyStatus.PENDING);
        when(jobs.findByIdAndRecruiter_Id(7L,10L)).thenReturn(Optional.of(j));
        assertThrows(IllegalArgumentException.class,()->service.submitExistingJob(7L,recruiter));
    }
    @Test void reopeningFailsWhenExpiredOrInactiveCompany() {
        Job j=existing(JobStatus.CLOSED);
        when(jobs.findByIdAndRecruiter_Id(7L,10L)).thenReturn(Optional.of(j));
        j.setDeadline(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class,()->service.reopenRecruiterJob(7L,recruiter));
        j.setDeadline(LocalDate.now().plusDays(1)); company.setStatus(CompanyStatus.PENDING);
        assertThrows(IllegalArgumentException.class,()->service.reopenRecruiterJob(7L,recruiter));
    }
    @Test void approvalRejectsInactiveCompanyAndExpiredDeadline() {
        Job j=existing(JobStatus.PENDING);
        when(jobs.findByIdWithDetails(7L)).thenReturn(Optional.of(j));
        company.setStatus(CompanyStatus.PENDING);
        assertThrows(IllegalArgumentException.class,()->service.approveJob(7L));
        company.setStatus(CompanyStatus.ACTIVE); j.setDeadline(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class,()->service.approveJob(7L));
        verify(jobs,never()).save(any());
    }
    @Test void rejectionRejectsTooLongReasonOrWrongStatus() {
        assertThrows(IllegalArgumentException.class,()->service.rejectJob(7L,"x".repeat(501)));
        when(jobs.findByIdWithDetails(7L)).thenReturn(Optional.of(existing(JobStatus.ACTIVE)));
        assertThrows(IllegalArgumentException.class,()->service.rejectJob(7L,"Reason"));
    }
    @Test void filtersPublicJobByTitleLocationLevelCategoryAndExperience() {
        Job j=existing(JobStatus.ACTIVE); j.setTitle("Java backend");j.setLocation("Ha Noi");
        j.setLevel("Senior");j.setExperience("2 years");
        when(jobs.findActiveJobs()).thenReturn(List.of(j));
        assertEquals(1,service.filterJobs("JAVA","ha noi",List.of("Ha Noi"),List.of("1-3"),null,List.of("senior"),List.of(2L)).size());
        assertTrue(service.filterJobs("Python",null,null,null,null,null,null).isEmpty());
        assertTrue(service.filterJobs(null,"HCM",null,null,null,null,null).isEmpty());
        assertTrue(service.filterJobs(null,null,null,List.of("5+"),null,null,null).isEmpty());
    }
    @Test void filtersSalaryUsdAndVnd() {
        Job j=existing(JobStatus.ACTIVE);
        j.setSalaryMin(new BigDecimal("25000000")); j.setSalaryMax(new BigDecimal("50000000"));j.setSalaryCurrency("VND");
        when(jobs.findActiveJobs()).thenReturn(List.of(j));
        assertEquals(1,service.filterJobs(null,null,null,null,List.of("1000-2000"),null,null).size());
        assertTrue(service.filterJobs(null,null,null,null,List.of("3000+"),null,null).isEmpty());
        j.setSalaryMin(new BigDecimal("3000")); j.setSalaryMax(new BigDecimal("4000"));j.setSalaryCurrency("USD");
        assertEquals(1,service.filterJobs(null,null,null,null,List.of("3000+"),null,null).size());
    }
    @Test void managementNormalizesInputsAndRejectsBadStatus() {
        when(jobs.findAllForManagement(eq("Java"),eq("Ha Noi"),eq(JobStatus.ACTIVE),any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existing(JobStatus.ACTIVE))));
        assertEquals(1,service.findAllJobsForManagement(" Java "," Ha Noi ","active").size());
        assertThrows(IllegalArgumentException.class,()->service.findAllJobsForManagement(null,null,"BAD-STATUS"));
    }
    @Test void managementPaginationReturnsCorrectSliceAndEmptyOutOfRange() {
        List<Job> source=java.util.stream.IntStream.range(0,8).mapToObj(i->existing(JobStatus.ACTIVE)).toList();
        when(jobs.findAllForManagement(isNull(),isNull(),isNull(),any(Pageable.class)))
                .thenReturn(new PageImpl<>(source));
        Page<Job> second=service.findAllJobsForManagementPage(null,null,null,1);
        assertEquals(2,second.getContent().size());
        assertEquals(8,second.getTotalElements());
        assertTrue(service.findAllJobsForManagementPage(null,null,null,3).isEmpty());
    }
    @Test void categoriesAndLocationsAreNormalized() {
        Job a=existing(JobStatus.ACTIVE);a.setLocation(" Ha Noi ");
        Job b=existing(JobStatus.ACTIVE);b.setLocation("Ha Noi");
        Job c=existing(JobStatus.ACTIVE);c.setLocation("  ");
        when(jobs.findDistinctByLocationIsNotNullOrderByLocationAsc()).thenReturn(List.of(a,b,c));
        assertEquals(List.of("Ha Noi"),service.getAllLocations());
        JobCategory inactive=new JobCategory();inactive.setStatus("INACTIVE");
        when(categories.findAll()).thenReturn(List.of(category,inactive));
        assertEquals(1,service.getActiveCategories().size());
        assertEquals(2,service.getAllCategories().size());
    }
    @Test void recruiterStatisticsHandleNullInputs() {
        assertEquals(0,service.countMyJobs(null));
        assertEquals(0,service.countMyJobsByStatus(null,JobStatus.ACTIVE));
        assertEquals(0,service.countMyJobsByStatus(10L,null));
        assertEquals(0,service.countAllJobsByStatus(null));
        assertThrows(IllegalArgumentException.class,()->service.findMyJobs(null,null,null));
    }
    @Test void recruiterFilteringRoutesToCorrectRepositoryMethods() {
        service.findMyJobs(10L,null,null);
        service.findMyJobs(10L," Java ",null);
        service.findMyJobs(10L,null,"active");
        service.findMyJobs(10L," Java ","active");
        verify(jobs).findByRecruiter_IdOrderByCreatedAtDesc(10L);
        verify(jobs).findByRecruiter_IdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(10L,"Java");
        verify(jobs).findByRecruiter_IdAndStatusOrderByCreatedAtDesc(10L,JobStatus.ACTIVE);
        verify(jobs).findByRecruiter_IdAndTitleContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(10L,"Java",JobStatus.ACTIVE);
        assertThrows(IllegalArgumentException.class,()->service.findMyJobs(10L,null,"bad"));
    }
}
