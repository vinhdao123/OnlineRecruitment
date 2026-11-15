package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.controller.Recruiter.RecruiterProfileController;
import OnlineRecruitment.OnlineRecruitment.controller.company.CompanyController;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class RecruiterViewsTest {

    private MockMvc mockMvc;
    private DefaultCsrfToken csrfToken;
    private UsernamePasswordAuthenticationToken auth;
    private User testUser;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-12345");

        testUser = new User();
        testUser.setId(101L);
        testUser.setFullName("Nguyễn Văn Tuyển Dụng");
        testUser.setEmail("recruiter@techcorp.vn");
        testUser.setPhone("0988776655");
        testUser.setLocation("Hà Nội");
        testUser.setRole(UserRole.RECRUITER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(30));

        testCompany = new Company();
        testCompany.setId(202L);
        testCompany.setCompanyName("TechCorp Global Vietnam");
        testCompany.setTaxCode("0109988776");
        testCompany.setEmail("contact@techcorp.vn");
        testCompany.setPhone("02439998888");
        testCompany.setAddress("Tòa nhà Keangnam, Mễ Trì, Nam Từ Liêm, Hà Nội");
        testCompany.setWebsite("https://techcorp.vn");
        testCompany.setCompanySize("100 - 499 nhân viên");
        testCompany.setDescription("TechCorp là tập đoàn công nghệ hàng đầu chuyên cung cấp giải pháp AI và Cloud.");
        testCompany.setStatus(CompanyStatus.ACTIVE);
        testCompany.setRecruiter(testUser);

        UserRepository mockUserRepo = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findByEmail".equals(method.getName()) || "findByUsername".equals(method.getName())) {
                        return Optional.of(testUser);
                    }
                    return null;
                }
        );

        CompanyRepository mockCompanyRepo = (CompanyRepository) Proxy.newProxyInstance(
                CompanyRepository.class.getClassLoader(),
                new Class<?>[]{CompanyRepository.class},
                (proxy, method, args) -> {
                    if ("findByRecruiterId".equals(method.getName())) {
                        return Optional.of(testCompany);
                    }
                    if ("findByStatus".equals(method.getName())) {
                        return List.of(testCompany);
                    }
                    if ("findApprovedById".equals(method.getName())) {
                        return Optional.of(testCompany);
                    }
                    return null;
                }
        );

        RecruiterProfileController profileController = new RecruiterProfileController(
                mockUserRepo, mockCompanyRepo, null, null, null, null
        );

        CompanyController companyController = new CompanyController(mockCompanyRepo);

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
                return org.springframework.security.core.userdetails.User.withUsername(testUser.getEmail())
                        .password("password")
                        .authorities("ROLE_RECRUITER")
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(profileController, companyController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setViewResolvers(viewResolver)
                .build();

        auth = new UsernamePasswordAuthenticationToken(
                testUser.getEmail(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );
    }

    @Test
    @DisplayName("GET /recruiter/profile - Chế độ Xem (View Mode) hiển thị đầy đủ thông tin Doanh nghiệp & Đại diện")
    void testShowRecruiterProfileViewMode() throws Exception {
        mockMvc.perform(get("/recruiter/profile")
                        .principal(auth)
                        .requestAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("recruiter/profile"))
                .andExpect(content().string(containsString("Nguyễn Văn Tuyển Dụng")))
                .andExpect(content().string(containsString("TechCorp Global Vietnam")))
                .andExpect(content().string(containsString("0109988776")))
                .andExpect(content().string(containsString("contact@techcorp.vn")))
                .andExpect(content().string(containsString("Chỉnh sửa hồ sơ")));
    }

    @Test
    @DisplayName("GET /recruiter/profile?edit=true - Chế độ Chỉnh sửa (Edit Mode) hiển thị form input & nút lưu")
    void testShowRecruiterProfileEditMode() throws Exception {
        mockMvc.perform(get("/recruiter/profile")
                        .param("edit", "true")
                        .principal(auth)
                        .requestAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("recruiter/profile"))
                .andExpect(content().string(containsString("Chỉnh Sửa Hồ Sơ Doanh Nghiệp")))
                .andExpect(content().string(containsString("Lưu thay đổi & Cập nhật")))
                .andExpect(content().string(containsString("Hủy bỏ")));
    }

    @Test
    @DisplayName("GET /company-profile - Danh bạ Doanh nghiệp public hiển thị công ty đã kích hoạt")
    void testShowPublicCompanyDirectory() throws Exception {
        mockMvc.perform(get("/company-profile")
                        .requestAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("company/company-profile"))
                .andExpect(content().string(containsString("TechCorp Global Vietnam")))
                .andExpect(content().string(containsString("Khám Phá Các Công Ty Hàng Đầu")));
    }

    @Test
    @DisplayName("GET /company-profile/202 - Chi tiết Doanh nghiệp public hiển thị đầy đủ thông tin")
    void testShowPublicCompanyDetail() throws Exception {
        mockMvc.perform(get("/company-profile/202")
                        .requestAttr("org.springframework.security.web.csrf.CsrfToken", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("company/company-detail"))
                .andExpect(content().string(containsString("TechCorp Global Vietnam")))
                .andExpect(content().string(containsString("0109988776")))
                .andExpect(content().string(containsString("Giới thiệu doanh nghiệp")));
    }
}
