package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.controller.job.JobDetailController;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobDetailControllerTest {
    JobRepository jobs = mock(JobRepository.class);
    UserRepository users = mock(UserRepository.class);
    SavedJobRepository saved = mock(SavedJobRepository.class);
    JobDetailController controller = new JobDetailController(jobs, users, saved);

    Job fixture(String role) {
        User user = new User(); user.setId(1L); user.setEmail("user@example.com");
        Job job = new Job(); job.setId(5L); job.setRecruiter(user);
        job.setStatus(JobStatus.ACTIVE);
        Company company = new Company(); company.setStatus(CompanyStatus.ACTIVE); job.setCompany(company);
        when(jobs.findById(5L)).thenReturn(Optional.of(job));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return job;
    }

    @Test
    void adminAndRecruiterDoNotQueryCandidateWishlist() {
        for (String role : List.of("ROLE_ADMIN", "ROLE_RECRUITER")) {
            fixture(role);
            var auth = new UsernamePasswordAuthenticationToken("user@example.com", "unused",
                    List.of(new SimpleGrantedAuthority(role)));
            var model = new ExtendedModelMap();
            assertEquals("job/job-detail", controller.detail(5L, auth, model));
            assertEquals(false, model.get("saved"));
        }
        verifyNoInteractions(saved);
    }

    @Test
    void candidateGetsActualSavedStatus() {
        fixture("ROLE_CANDIDATE");
        when(saved.existsByUser_IdAndJob_Id(1L, 5L)).thenReturn(true);
        var auth = new UsernamePasswordAuthenticationToken("user@example.com", "unused",
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")));
        var model = new ExtendedModelMap();
        controller.detail(5L, auth, model);
        assertEquals(true, model.get("saved"));
    }
}
