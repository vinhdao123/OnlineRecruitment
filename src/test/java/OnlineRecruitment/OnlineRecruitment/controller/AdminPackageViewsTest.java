package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.controller.admin.AdminPackageController;
import OnlineRecruitment.OnlineRecruitment.entity.ServicePackage;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.service.ServicePackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AdminPackageViewsTest {

    private MockMvc mockMvc;
    private DefaultCsrfToken csrfToken;
    private UsernamePasswordAuthenticationToken authAdmin;
    private List<ServicePackage> mockPackages;

    @BeforeEach
    void setUp() {
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-12345");

        authAdmin = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin@system.vn", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        mockPackages = new ArrayList<>();

        ServicePackage p1 = new ServicePackage();
        p1.setId(1L);
        p1.setName("Gói Cơ Bản");
        p1.setDescription("Mô tả gói");
        p1.setPrice(500000.0);
        p1.setDurationDays(30);
        p1.setMaxJobs(5);
        p1.setAllowPinJob(true);
        p1.setIsActive(true);
        p1.setTargetRole(UserRole.RECRUITER);
        mockPackages.add(p1);

        // Case with null optional fields (e.g. top-up package, targetRole null, etc.)
        ServicePackage p2 = new ServicePackage();
        p2.setId(2L);
        p2.setName("Gói Topup");
        p2.setDescription(null);
        p2.setPrice(200000.0);
        p2.setDurationDays(null);
        p2.setMaxJobs(null);
        p2.setAllowPinJob(null);
        p2.setIsActive(null);
        p2.setTargetRole(null);
        mockPackages.add(p2);

        ServicePackageService mockService = new ServicePackageService(null, null, null) {
            @Override
            public List<ServicePackage> getAllPackages() {
                return mockPackages;
            }
            @Override
            public ServicePackage getById(Long id) {
                return mockPackages.get(0);
            }
        };

        AdminPackageController controller = new AdminPackageController(mockService);

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

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("Test GET /admin/packages list view rendering")
    void testAdminPackagesList() throws Exception {
        mockMvc.perform(get("/admin/packages")
                .principal(authAdmin)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/packages/list"));
    }

    @Test
    @DisplayName("Test GET /admin/packages/new create form view rendering")
    void testAdminPackageNewForm() throws Exception {
        mockMvc.perform(get("/admin/packages/new")
                .principal(authAdmin)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/packages/form"));
    }

    @Test
    @DisplayName("Test GET /admin/packages/1/edit edit form view rendering")
    void testAdminPackageEditForm() throws Exception {
        mockMvc.perform(get("/admin/packages/1/edit")
                .principal(authAdmin)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/packages/form"));
    }
}
