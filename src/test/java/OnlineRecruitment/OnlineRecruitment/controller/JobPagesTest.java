//package OnlineRecruitment.OnlineRecruitment.controller;
//
//import OnlineRecruitment.OnlineRecruitment.controller.Recruiter.RecruiterJobController;
//import OnlineRecruitment.OnlineRecruitment.controller.Recruiter.RecruiterJobArchiveController;
//import OnlineRecruitment.OnlineRecruitment.controller.admin.*;
//import OnlineRecruitment.OnlineRecruitment.entity.*;
//import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
//import OnlineRecruitment.OnlineRecruitment.service.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.web.csrf.DefaultCsrfToken;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.thymeleaf.spring6.SpringTemplateEngine;
//import org.thymeleaf.spring6.view.ThymeleafViewResolver;
//import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//class JobPagesTest {
//    JobService jobs = mock(JobService.class);
//    UserRepository users = mock(UserRepository.class);
//    AdminSkillService skills = mock(AdminSkillService.class);
//    JobCategoryAdminService categories = mock(JobCategoryAdminService.class);
//    MockMvc mvc;
//    User recruiter;
//    Job job;
//    UsernamePasswordAuthenticationToken auth;
//
//    @BeforeEach
//    void setUp() {
//        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
//        templates.setPrefix("templates/"); templates.setSuffix(".html");
//        templates.setCharacterEncoding("UTF-8"); templates.setTemplateMode("HTML");
//        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(templates);
//        engine.addDialect(new org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect());
//        ThymeleafViewResolver views = new ThymeleafViewResolver();
//        views.setTemplateEngine(engine); views.setCharacterEncoding("UTF-8");
//        mvc = MockMvcBuilders.standaloneSetup(  new RecruiterJobController(jobs, users),
//                new RecruiterJobArchiveController(jobs, users), new AdminJobController(jobs),
//                new AdminSkillController(skills), new AdminJobCategoryController(categories))
//                .setViewResolvers(views).build();
//        recruiter = new User(); recruiter.setId(1L); recruiter.setEmail("recruiter@example.com");
//        auth = new UsernamePasswordAuthenticationToken(recruiter.getEmail(), "unused",
//                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
//        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
//        when(users.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
//        Company company = new Company(); company.setCompanyName("Example");
//        JobCategory category = new JobCategory(); category.setId(3L); category.setName("IT");
//        category.setStatus("ACTIVE"); category.setCreatedAt(LocalDateTime.now());
//        job = new Job(); job.setId(4L); job.setTitle("Java Developer"); job.setCompany(company);
//        job.setCategory(category); job.setRecruiter(recruiter); job.setStatus(JobStatus.EXPIRED);
//        job.setDeadline(LocalDate.now().minusDays(1)); job.setCreatedAt(LocalDateTime.now());
//        job.setUpdatedAt(LocalDateTime.now());
//        when(jobs.getActiveCategories()).thenReturn(List.of(category));
//    }
//
//    @AfterEach
//    void clearAuthentication() {
//        org.springframework.security.core.context.SecurityContextHolder.clearContext();
//    }
//
//    @Test
//    void expiredArchiveRendersReopenActionAndSeparateTab() throws Exception {
//        when(jobs.findMyJobs(1L, null, "EXPIRED")).thenReturn(List.of(job));
//        String html = mvc.perform(get("/recruiter/jobs/archive").param("tab", "expired").principal(auth))
//                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
//        assertTrue(html.contains("/recruiter/jobs/4/reopen"));
//        assertTrue(html.contains("Quá hạn"));
//    }
//
//    @Test
//    void recruiterListUsesOnlyOwnedJobs() throws Exception {
//        when(jobs.findMyJobs(1L, null, null)).thenReturn(List.of(job));
//        mvc.perform(get("/recruiter/jobs").principal(auth)).andExpect(status().isOk());
//        verify(jobs).findMyJobs(1L, null, null);
//        verify(jobs, never()).findAllJobsForManagement(any(), any(), any());
//    }
//
//    @Test
//    void invalidSalaryRendersSameFormAndKeepsTitle() throws Exception {
//        var result = mvc.perform(post("/recruiter/jobs/save").principal(auth)
//                        .param("action", "submit").param("title", "Nội dung đã soạn").param("salaryMin", "abc"))
//                .andExpect(status().isOk()).andExpect(view().name("recruiter/job-form"))
//                .andExpect(model().attributeHasFieldErrors("jobForm", "salaryMin")).andReturn();
//        var form = (OnlineRecruitment.OnlineRecruitment.dto.RecruiterJobForm)
//                result.getModelAndView().getModel().get("jobForm");
//        assertEquals("Nội dung đã soạn", form.getTitle());
//        assertTrue(result.getResponse().getContentAsString().contains("id=\"title\""));
//        verify(jobs, never()).saveRecruiterJob(any(), any(), anyBoolean());
//    }
//
//    @Test
//    void expiredAdminPageShowsReopenAndHidesApproval() throws Exception {
//        when(jobs.findAllJobsForManagement(null, null, "EXPIRED")).thenReturn(List.of(job));
//        String html = mvc.perform(get("/admin/jobs").param("status", "EXPIRED")
//                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test")))
//                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
//        assertTrue(html.contains("/admin/jobs/4/reopen"));
//        assertFalse(html.contains("/admin/jobs/4/approve"));
//    }
//
//    @Test
//    void pendingAdminPageShowsApprovalAndRejection() throws Exception {
//        job.setStatus(JobStatus.PENDING);
//        when(jobs.findAllJobsForManagement(null, null, "PENDING")).thenReturn(List.of(job));
//        String html = mvc.perform(get("/admin/jobs").param("status", "PENDING")
//                        .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test")))
//                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
//        assertTrue(html.contains("/admin/jobs/4/approve"));
//        assertTrue(html.contains("/admin/jobs/4/reject"));
//        assertFalse(html.contains("/admin/jobs/4/reopen"));
//    }
//
//    @Test
//    void categoryAndSkillPagesRender() throws Exception {
//        when(categories.findAll()).thenReturn(List.of(job.getCategory()));
//        when(skills.getAll()).thenReturn(List.of(new Skill(5L, "Java", "INACTIVE")));
//        when(skills.getCategories()).thenReturn(List.of(job.getCategory()));
//        mvc.perform(get("/admin/categories")).andExpect(status().isOk());
//        mvc.perform(get("/admin/categories/new")).andExpect(status().isOk());
//        mvc.perform(get("/admin/skills")).andExpect(status().isOk());
//        mvc.perform(get("/admin/skills/new")).andExpect(status().isOk());
//    }
//
//    @Test
//    void allJobsPageUsesOldSidebarWithMainAdminLinks() throws Exception {
//        when(jobs.findAllJobsForManagement(null, null, "")).thenReturn(List.of(job));
//        String html = mvc.perform(get("/admin/jobs"))
//                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
//        assertTrue(html.contains("/admin/categories"));
//        assertTrue(html.contains("/admin/skills"));
//        assertTrue(html.contains("/admin/companies"));
//        assertTrue(html.contains("class=\"sidebar\""));
//        verify(jobs).findAllJobsForManagement(null, null, "");
//    }
//
//    @Test
//    void skillEditRendersCurrentCategoryAndKeepsDataAfterValidationError() throws Exception {
//        Skill skill = new Skill(5L, "Java", "ACTIVE"); skill.setCategory(job.getCategory());
//        when(skills.getById(5L)).thenReturn(skill);
//        when(skills.getCategories()).thenReturn(List.of(job.getCategory()));
//        String html = mvc.perform(get("/admin/skills/5/edit"))
//                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
//        assertTrue(html.contains("name=\"categoryId\""));
//        assertTrue(html.contains("selected=\"selected\""));
//        mvc.perform(post("/admin/skills/5").param("name", "").param("categoryId", "3").param("status", "ACTIVE"))
//                .andExpect(status().isOk()).andExpect(view().name("admin/skills/form"))
//                .andExpect(model().attribute("skillId", 5L))
//                .andExpect(model().attributeHasFieldErrors("skillForm", "name"));
//        verify(skills, never()).update(any(), any(), any(), any());
//    }
//}
