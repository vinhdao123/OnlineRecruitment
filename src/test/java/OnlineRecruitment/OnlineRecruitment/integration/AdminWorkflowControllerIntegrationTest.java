package OnlineRecruitment.OnlineRecruitment.integration;

import OnlineRecruitment.OnlineRecruitment.controller.admin.AdminJobController;
import OnlineRecruitment.OnlineRecruitment.controller.admin.AdminJobCategoryController;
import OnlineRecruitment.OnlineRecruitment.controller.admin.AdminSkillController;
import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.service.JobService;
import OnlineRecruitment.OnlineRecruitment.service.JobCategoryAdminService;
import OnlineRecruitment.OnlineRecruitment.service.AdminSkillService;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP/controller integration with real Spring MVC validation and routing, mocked Service boundary.
 * Security filter chain and SQL persistence are deliberately out of scope. */
class AdminWorkflowControllerIntegrationTest {
    MockMvc mvc;
    JobCategoryAdminService categories;
    AdminSkillService skills;
    JobService jobs;
    @BeforeEach void setup() {
        categories=mock(JobCategoryAdminService.class);
        skills=mock(AdminSkillService.class);
        jobs=mock(JobService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new AdminJobCategoryController(categories),
                new AdminSkillController(skills),
                new AdminJobController(jobs)
        ).build();
    }
    @Test void categoryListRendersCorrectView() throws Exception {
        mvc.perform(get("/admin/categories")).andExpect(status().isOk()).andExpect(view().name("admin/categories/list"));
        verify(categories).findAll();
    }
    @Test void categoryCreateCallsServiceAndRedirects() throws Exception {
        mvc.perform(post("/admin/categories").param("name","Backend").param("description","Java"))
           .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/categories"));
        verify(categories).create("Backend","Java");
    }
    @Test void categoryCreateInvalidNameDoesNotSave() throws Exception {
        mvc.perform(post("/admin/categories").param("name"," ")).andExpect(view().name("admin/categories/form"));
        verify(categories,never()).create(any(),any());
    }
    @Test void categoryUpdateCallsService() throws Exception {
        mvc.perform(post("/admin/categories/9").param("name","Java").param("status","INACTIVE"))
           .andExpect(redirectedUrl("/admin/categories"));
        verify(categories).update(9L,"Java",null,"INACTIVE");
    }
    @Test void skillListRendersCorrectView() throws Exception {
        mvc.perform(get("/admin/skills")).andExpect(status().isOk()).andExpect(view().name("admin/skills/list"));
        verify(skills).getAll();
    }
    @Test void skillCreateCallsServiceAndRedirects() throws Exception {
        mvc.perform(post("/admin/skills").param("name","Java").param("categoryId","8"))
           .andExpect(redirectedUrl("/admin/skills"));
        verify(skills).create("Java",8L);
    }
    @Test void skillInvalidCategoryDoesNotSave() throws Exception {
        mvc.perform(post("/admin/skills").param("name","Java"))
           .andExpect(view().name("admin/skills/form"));
        verify(skills,never()).create(any(),any());
    }
    @Test void skillUpdateCallsService() throws Exception {
        mvc.perform(post("/admin/skills/3").param("name","SQL").param("categoryId","8").param("status","INACTIVE"))
           .andExpect(redirectedUrl("/admin/skills"));
        verify(skills).update(3L,"SQL",8L,"INACTIVE");
    }
    @Test void jobApproveInvokesService() throws Exception {
        mvc.perform(post("/admin/jobs/5/approve")).andExpect(redirectedUrl("/admin/jobs"));
        verify(jobs).approveJob(5L);
    }
    @Test void jobRejectRequiresReason() throws Exception {
        mvc.perform(post("/admin/jobs/5/reject").param("reason"," ")).andExpect(redirectedUrl("/admin/jobs"));
        verify(jobs,never()).rejectJob(anyLong(),anyString());
    }
    @Test void jobRejectInvokesServiceWithTrimmedReason() throws Exception {
        mvc.perform(post("/admin/jobs/5/reject").param("reason","  missing salary  ")).andExpect(redirectedUrl("/admin/jobs"));
        verify(jobs).rejectJob(5L,"missing salary");
    }
    @Test void jobRejectOverlongReasonDoesNotSave() throws Exception {
        mvc.perform(post("/admin/jobs/5/reject").param("reason","X".repeat(501))).andExpect(redirectedUrl("/admin/jobs"));
        verify(jobs,never()).rejectJob(anyLong(),anyString());
    }
}
