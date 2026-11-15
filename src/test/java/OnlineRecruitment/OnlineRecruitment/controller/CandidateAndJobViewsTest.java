package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.controller.candidate.CandidateProfileController;
import OnlineRecruitment.OnlineRecruitment.controller.job.JobDetailController;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SavedJobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.WishlistRepository;
import OnlineRecruitment.OnlineRecruitment.service.CandidateService;
import OnlineRecruitment.OnlineRecruitment.service.JobService;
import OnlineRecruitment.OnlineRecruitment.service.JobSkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CandidateAndJobViewsTest {

    private MockMvc mockMvc;
    private DefaultCsrfToken csrfToken;
    private UsernamePasswordAuthenticationToken candidateAuth;
    private User testCandidateUser;
    private Candidate testCandidate;
    private Job testJob;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-candidate");

        // 1. Setup Candidate & User
        testCandidateUser = new User();
        testCandidateUser.setId(301L);
        testCandidateUser.setFullName("Lê Thị Ứng Viên");
        testCandidateUser.setEmail("candidate@gmail.com");
        testCandidateUser.setPhone("0912345678");
        testCandidateUser.setLocation("Hồ Chí Minh");
        testCandidateUser.setRole(UserRole.CANDIDATE);
        testCandidateUser.setStatus(UserStatus.ACTIVE);
        testCandidateUser.setCreatedAt(LocalDateTime.now().minusDays(15));

        testCandidate = new Candidate();
        testCandidate.setId(401L);
        testCandidate.setUser(testCandidateUser);
        testCandidate.setGender("Nữ");
        testCandidate.setDob(LocalDate.of(1998, 5, 20));
        testCandidate.setAddress("Quận 1, TP. Hồ Chí Minh");
        testCandidate.setDesiredPosition("Senior Frontend React Engineer");
        testCandidate.setAboutMe("Lập trình viên giao diện giàu kinh nghiệm với React, Tailwind CSS và TypeScript.");
        testCandidate.setLinkedinUrl("https://linkedin.com/in/candidate");
        testCandidate.setGithubUrl("https://github.com/candidate");

        // 2. Setup Job & Company
        testCompany = new Company();
        testCompany.setId(501L);
        testCompany.setCompanyName("VNG Corporation");
        testCompany.setLogoUrl("/images/vng-logo.png");
        testCompany.setAddress("Khu chế xuất Tân Thuận, Quận 7, TP.HCM");
        testCompany.setCompanySize("1,000+ nhân viên");

        testJob = new Job();
        testJob.setId(601L);
        testJob.setTitle("Senior Fullstack Developer (Spring Boot & React)");
        testJob.setDescription("Mô tả: Tham gia thiết kế và triển khai kiến trúc microservices và giao diện người dùng.");
        testJob.setRequirements("Yêu cầu: Tối thiểu 3 năm kinh nghiệm với Java Spring Boot và React.");
        testJob.setBenefits("Quyền lợi: Lương tháng 13, bảo hiểm sức khỏe cao cấp, thưởng dự án.");
        testJob.setLocation("TP. Hồ Chí Minh");
        testJob.setSalaryMin(BigDecimal.valueOf(25000000));
        testJob.setSalaryMax(BigDecimal.valueOf(45000000));
        testJob.setSalaryCurrency("VND");
        testJob.setExperience("3 - 5 năm");
        testJob.setLevel("Senior");
        testJob.setEmploymentType(EmploymentType.FULL_TIME);
        testJob.setStatus(JobStatus.ACTIVE);
        testJob.setCompany(testCompany);
        testJob.setCreatedAt(LocalDateTime.now().minusDays(2));

        // 3. Mock Repositories & Services
        UserRepository mockUserRepo = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findByEmail".equals(method.getName()) || "findByUsername".equals(method.getName())) {
                        return Optional.of(testCandidateUser);
                    }
                    return null;
                }
        );

        JobRepository mockJobRepo = (JobRepository) Proxy.newProxyInstance(
                JobRepository.class.getClassLoader(),
                new Class<?>[]{JobRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(testJob);
                    }
                    return null;
                }
        );

        SavedJobRepository mockSavedJobRepo = (SavedJobRepository) Proxy.newProxyInstance(
                SavedJobRepository.class.getClassLoader(),
                new Class<?>[]{SavedJobRepository.class},
                (proxy, method, args) -> false
        );

        JobSkillService mockJobSkills = new JobSkillService(null, null) {
            @Override
            public List<String> requiredNames(Long jobId) {
                return List.of("Java", "Spring Boot", "React");
            }
        };

        CandidateService mockCandidateService = new CandidateService(null, null, null) {
            @Override
            public Candidate getOrCreateCandidate(User user) {
                return testCandidate;
            }
        };

        JobService mockJobService = new JobService(null, null, null, null, null, null, null) {
            @Override
            public List<Job> filterJobs(String keyword, String locationKeyword,
                                        List<String> locations, List<String> experiences,
                                        List<String> salaryRanges, List<String> levels,
                                        List<Long> categoryIds) {
                return List.of(testJob);
            }

            @Override
            public List<String> getAllLocations() {
                return List.of("Hà Nội", "Hồ Chí Minh", "Đà Nẵng");
            }

            @Override
            public List<JobCategory> getActiveCategories() {
                return Collections.emptyList();
            }
        };

        CandidateRepository mockCandidateRepo = (CandidateRepository) Proxy.newProxyInstance(
                CandidateRepository.class.getClassLoader(),
                new Class<?>[]{CandidateRepository.class},
                (proxy, method, args) -> {
                    if ("findByUserId".equals(method.getName())) {
                        return Optional.of(testCandidate);
                    }
                    return null;
                }
        );

        WishlistRepository mockWishlistRepo = new WishlistRepository(null) {
            @Override
            public List<Long> findJobIdsByCandidateId(Long candidateId) {
                return Collections.emptyList();
            }
        };

        CvRepository mockCvRepo = (CvRepository) Proxy.newProxyInstance(
                CvRepository.class.getClassLoader(),
                new Class<?>[]{CvRepository.class},
                (proxy, method, args) -> Collections.emptyList()
        );

        JobController jobController = new JobController(
                mockJobService, mockUserRepo, mockCandidateRepo, mockCvRepo, mockWishlistRepo
        );

        JobDetailController jobDetailController = new JobDetailController(
                mockJobRepo, mockUserRepo, mockSavedJobRepo, mockJobSkills
        );

        CandidateProfileController candidateProfileController = new CandidateProfileController(
                mockUserRepo, mockCandidateService, null
        );

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.addDialect(new org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect());

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        || UserDetails.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return org.springframework.security.core.userdetails.User.withUsername(testCandidateUser.getEmail())
                        .password("password")
                        .authorities("ROLE_CANDIDATE")
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(jobController, jobDetailController, candidateProfileController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setViewResolvers(viewResolver)
                .build();

        candidateAuth = new UsernamePasswordAuthenticationToken(
                testCandidateUser.getEmail(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );
        // Do not set global SecurityContextHolder to avoid missing WebSecurityExpressionHandler in standalone MockMvc
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /jobs - Render danh sách việc làm với bộ lọc và danh sách thẻ việc làm")
    void testShowJobListing() throws Exception {
        mockMvc.perform(get("/jobs")
                        .principal(candidateAuth)
                        .requestAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("job/job"))
                .andExpect(content().string(containsString("Tìm Việc Làm Phù Hợp Với Bạn")))
                .andExpect(content().string(containsString("Bộ Lọc Tìm Kiếm")))
                .andExpect(content().string(containsString("Senior Fullstack Developer")))
                .andExpect(content().string(containsString("VNG Corporation")))
                .andExpect(content().string(containsString("Ứng tuyển")));
    }

    @Test
    @DisplayName("GET /jobs/601 - Render chi tiết tin tuyển dụng với mô tả, quyền lợi và CTA ứng tuyển")
    void testShowJobDetail() throws Exception {
        mockMvc.perform(get("/jobs/601")
                        .principal(candidateAuth)
                        .requestAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("job/job-detail"))
                .andExpect(content().string(containsString("Senior Fullstack Developer")))
                .andExpect(content().string(containsString("VNG Corporation")))
                .andExpect(content().string(containsString("Mô tả công việc")))
                .andExpect(content().string(containsString("Yêu cầu ứng viên")))
                .andExpect(content().string(containsString("Quyền lợi được hưởng")))
                .andExpect(content().string(containsString("TP. Hồ Chí Minh")));
    }

    @Test
    @DisplayName("GET /candidate/profile - Render hồ sơ ứng viên chế độ xem với thông tin cá nhân và định hướng")
    void testShowCandidateProfileViewMode() throws Exception {
        mockMvc.perform(get("/candidate/profile")
                        .principal(candidateAuth)
                        .requestAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/profile"))
                .andExpect(content().string(containsString("Lê Thị Ứng Viên")))
                .andExpect(content().string(containsString("Senior Frontend React Engineer")))
                .andExpect(content().string(containsString("Chỉnh sửa hồ sơ")));
    }

    @Test
    @DisplayName("GET /candidate/profile?edit=true - Render hồ sơ ứng viên chế độ chỉnh sửa với form cập nhật")
    void testShowCandidateProfileEditMode() throws Exception {
        mockMvc.perform(get("/candidate/profile")
                        .param("edit", "true")
                        .principal(candidateAuth)
                        .requestAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/profile"))
                .andExpect(content().string(containsString("Chỉnh sửa thông tin hồ sơ")))
                .andExpect(content().string(containsString("Lưu thay đổi")))
                .andExpect(content().string(containsString("Hủy bỏ")));
    }
}
