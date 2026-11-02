package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class JobWorkflowServiceTest {
    @Mock JobRepository jobs;
    @Mock JobCategoryRepository categories;
    @Mock CompanyRepository companies;
    @InjectMocks JobService service;

    private Job job(JobStatus status) {
        Job j = new Job(); j.setId(7L); j.setStatus(status);
        Company c = new Company(); c.setStatus(CompanyStatus.ACTIVE); j.setCompany(c);
        j.setDeadline(LocalDate.now().plusDays(10));
        return j;
    }
    @Test void JOB_01_adminApprovesPending() {
        Job j=job(JobStatus.PENDING);
        when(jobs.findByIdWithDetails(7L)).thenReturn(Optional.of(j));
        when(jobs.save(j)).thenReturn(j);
        Job result=service.approveJob(7L);
        assertEquals(JobStatus.ACTIVE,result.getStatus());
        assertNotNull(result.getPublishedAt());
        verify(jobs).save(j);
    }
    @Test void JOB_02_cannotApproveDraft() {
        when(jobs.findByIdWithDetails(7L)).thenReturn(Optional.of(job(JobStatus.DRAFT)));
        assertThrows(IllegalArgumentException.class,()->service.approveJob(7L));
        verify(jobs,never()).save(any());
    }
    @Test void JOB_03_adminRejectsPendingWithReason() {
        Job j=job(JobStatus.PENDING);
        when(jobs.findByIdWithDetails(7L)).thenReturn(Optional.of(j));
        when(jobs.save(j)).thenReturn(j);
        Job result=service.rejectJob(7L,"  Thiếu thông tin  ");
        assertEquals(JobStatus.REJECTED,result.getStatus());
        assertEquals("Thiếu thông tin",result.getRejectionReason());
    }
    @Test void JOB_04_rejectionRequiresReason() {
        assertThrows(IllegalArgumentException.class,()->service.rejectJob(7L," "));
        verifyNoInteractions(jobs);
    }
    @Test void JOB_05_recruiterClosesActiveJob() {
        User recruiter = new User(); recruiter.setId(12L);
        Job j=job(JobStatus.ACTIVE);
        when(jobs.findByIdAndRecruiter_Id(7L,12L)).thenReturn(Optional.of(j));
        when(jobs.save(j)).thenReturn(j);
        assertEquals(JobStatus.CLOSED,service.closeRecruiterJob(7L,recruiter).getStatus());
    }
    @Test void JOB_06_recruiterCannotClosePendingJob() {
        User recruiter = new User(); recruiter.setId(12L);
        when(jobs.findByIdAndRecruiter_Id(7L,12L)).thenReturn(Optional.of(job(JobStatus.PENDING)));
        assertThrows(IllegalArgumentException.class,()->service.closeRecruiterJob(7L,recruiter));
        verify(jobs,never()).save(any());
    }
    @Test void JOB_07_recruiterOwnershipEnforced() {
        assertThrows(IllegalArgumentException.class,()->service.getJobForRecruiter(7L,99L));
        verify(jobs).findByIdAndRecruiter_Id(7L,99L);
    }
    @Test void JOB_08_reopenClosedJob() {
        User recruiter = new User(); recruiter.setId(12L);
        Job j=job(JobStatus.CLOSED);
        when(jobs.findByIdAndRecruiter_Id(7L,12L)).thenReturn(Optional.of(j));
        when(jobs.save(j)).thenReturn(j);
        assertEquals(JobStatus.ACTIVE,service.reopenRecruiterJob(7L,recruiter).getStatus());
        assertNotNull(j.getPublishedAt());
    }
}
