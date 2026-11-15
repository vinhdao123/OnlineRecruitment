package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.ServicePackage;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserPackage;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoginModuleTest {

    private MockMvc mockMvc;
    private CsrfToken csrfToken;

    @BeforeEach
    void setUp() {
        HomeController homeController = new HomeController(null, null, null);

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

        this.mockMvc = MockMvcBuilders.standaloneSetup(homeController)
                .setViewResolvers(viewResolver)
                .build();

        this.csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-csrf-token-123456");
    }

    @Test
    @DisplayName("GET / - Render Trang chủ (Homepage) với Navbar và Footer đồng bộ")
    void testShowHomePage() throws Exception {
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("JOB")))
                .andExpect(content().string(containsString("PORTAL")))
                .andExpect(content().string(containsString("Dành cho Ứng viên")))
                .andExpect(content().string(containsString("Dành cho Doanh nghiệp")))
                .andReturn();

        String html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/home_preview.html"), html, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("GET /login - Hiển thị trang đăng nhập chuẩn form và các thành phần chính")
    void testShowLoginPage() throws Exception {
        mockMvc.perform(get("/login")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Đăng nhập Hệ thống")))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("/forgot-password")))
                .andExpect(content().string(containsString("/register/candidate")))
                .andExpect(content().string(containsString("/register/recruiter")));
    }

    @Test
    @DisplayName("GET /login?badCredentials - Hiển thị thông báo sai thông tin đăng nhập")
    void testLoginBadCredentials() throws Exception {
        mockMvc.perform(get("/login?badCredentials")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sai email hoặc mật khẩu. Vui lòng thử lại.")));
    }

    @Test
    @DisplayName("GET /login?locked - Hiển thị thông báo tài khoản bị khóa")
    void testLoginLocked() throws Exception {
        mockMvc.perform(get("/login?locked")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tài khoản của bạn đã bị khóa")));
    }

    @Test
    @DisplayName("GET /login?rejected - Hiển thị thông báo hồ sơ bị từ chối phê duyệt")
    void testLoginRejected() throws Exception {
        mockMvc.perform(get("/login?rejected")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hồ sơ đăng ký công ty của bạn đã bị từ chối phê duyệt")));
    }

    @Test
    @DisplayName("GET /login?inactive - Hiển thị thông báo tài khoản chờ kích hoạt")
    void testLoginInactive() throws Exception {
        mockMvc.perform(get("/login?inactive")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tài khoản chưa được kích hoạt hoặc đang chờ duyệt!")));
    }

    @Test
    @DisplayName("GET /login?logout - Hiển thị thông báo đã đăng xuất thành công")
    void testLoginLogout() throws Exception {
        mockMvc.perform(get("/login?logout")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã đăng xuất thành công khỏi hệ thống.")));
    }

    @Test
    @DisplayName("GET /login?resetSuccess - Hiển thị thông báo đặt lại mật khẩu thành công")
    void testLoginResetSuccess() throws Exception {
        mockMvc.perform(get("/login?resetSuccess")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đặt lại mật khẩu thành công! Vui lòng đăng nhập")));
    }

    @Test
    @DisplayName("GET /recruiter/dashboard - Render giao diện Recruiter Dashboard thành công")
    void testShowRecruiterDashboard() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "recruiter@company.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );

        mockMvc.perform(get("/recruiter/dashboard")
                        .principal(auth)
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("recruiter/dashboard"))
                .andExpect(content().string(containsString("Bảng điều khiển Nhà Tuyển Dụng")));
    }

    @Test
    @DisplayName("GET /recruiter/dashboard - Hiển thị tên Doanh nghiệp và Gói dịch vụ đang kích hoạt")
    void testShowRecruiterDashboardWithActivePackage() throws Exception {
        UserRepository mockUserRepo = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findByEmail".equals(method.getName()) || "findByUsername".equals(method.getName())) {
                        User user = new User();
                        user.setId(88L);
                        user.setFullName("Tập Đoàn FPT Tuyển Dụng");
                        user.setEmail("hr@fpt.com");
                        user.setRole(UserRole.RECRUITER);
                        return Optional.of(user);
                    }
                    return null;
                }
        );

        UserPackageRepository mockUserPackageRepo = (UserPackageRepository) Proxy.newProxyInstance(
                UserPackageRepository.class.getClassLoader(),
                new Class<?>[]{UserPackageRepository.class},
                (proxy, method, args) -> {
                    if ("findFirstByUserAndStatusOrderByEndDateDesc".equals(method.getName())) {
                        UserPackage pkg = new UserPackage();
                        pkg.setId(10L);
                        ServicePackage sp = new ServicePackage();
                        sp.setName("Gói Doanh Nghiệp Kim Cương");
                        pkg.setServicePackage(sp);
                        pkg.setEndDate(LocalDateTime.now().plusDays(45));
                        pkg.setStatus("ACTIVE");
                        return Optional.of(pkg);
                    }
                    return null;
                }
        );

        HomeController recruiterHomeController = new HomeController(null, mockUserRepo, mockUserPackageRepo);

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

        MockMvc customMockMvc = MockMvcBuilders.standaloneSetup(recruiterHomeController)
                .setViewResolvers(viewResolver)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "hr@fpt.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );

        customMockMvc.perform(get("/recruiter/dashboard")
                        .principal(auth)
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("recruiter/dashboard"))
                .andExpect(content().string(containsString("Tập Đoàn FPT Tuyển Dụng")))
                .andExpect(content().string(containsString("Gói Doanh Nghiệp Kim Cương")))
                .andExpect(content().string(containsString("Thời hạn đến:")));
    }

    @Test
    @DisplayName("GET /recruiter/dashboard - Null-safety khi package có servicePackage hoặc endDate là null")
    void testShowRecruiterDashboardWithNullPackageProperties() throws Exception {
        UserRepository mockUserRepo = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findByEmail".equals(method.getName()) || "findByUsername".equals(method.getName())) {
                        User user = new User();
                        user.setId(99L);
                        user.setFullName("Công Ty Khởi Nghiệp XYZ");
                        user.setEmail("founder@xyz.com");
                        user.setRole(UserRole.RECRUITER);
                        return Optional.of(user);
                    }
                    return null;
                }
        );

        UserPackageRepository mockUserPackageRepo = (UserPackageRepository) Proxy.newProxyInstance(
                UserPackageRepository.class.getClassLoader(),
                new Class<?>[]{UserPackageRepository.class},
                (proxy, method, args) -> {
                    if ("findFirstByUserAndStatusOrderByEndDateDesc".equals(method.getName())) {
                        UserPackage pkg = new UserPackage();
                        pkg.setId(11L);
                        pkg.setServicePackage(null); // NULL servicePackage
                        pkg.setEndDate(null);        // NULL endDate
                        pkg.setStatus("ACTIVE");
                        return Optional.of(pkg);
                    }
                    return null;
                }
        );

        HomeController recruiterHomeController = new HomeController(null, mockUserRepo, mockUserPackageRepo);

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

        MockMvc customMockMvc = MockMvcBuilders.standaloneSetup(recruiterHomeController)
                .setViewResolvers(viewResolver)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "founder@xyz.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );

        customMockMvc.perform(get("/recruiter/dashboard")
                        .principal(auth)
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("recruiter/dashboard"))
                .andExpect(content().string(containsString("Công Ty Khởi Nghiệp XYZ")))
                .andExpect(content().string(containsString("Chưa đăng ký gói dịch vụ")));
    }
}
