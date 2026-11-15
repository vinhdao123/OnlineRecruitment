package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.config.BankPaymentConfig;
import OnlineRecruitment.OnlineRecruitment.config.SePayGatewayConfig;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import OnlineRecruitment.OnlineRecruitment.service.*;
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
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PricingViewsTest {

    private MockMvc mockMvc;
    private DefaultCsrfToken csrfToken;
    private UsernamePasswordAuthenticationToken authRecruiter;
    private UsernamePasswordAuthenticationToken authCandidate;
    private User testRecruiter;
    private User testCandidate;
    private ServicePackage testMainPackage;
    private ServicePackage testTopupPackage;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-12345");

        testRecruiter = new User();
        testRecruiter.setId(101L);
        testRecruiter.setFullName("Nguyễn Nhà Tuyển Dụng");
        testRecruiter.setEmail("recruiter@test.com");
        testRecruiter.setRole(UserRole.RECRUITER);
        testRecruiter.setStatus(UserStatus.ACTIVE);

        testCandidate = new User();
        testCandidate.setId(102L);
        testCandidate.setFullName("Bùi Ngọc Thạch");
        testCandidate.setEmail("candidate@test.com");
        testCandidate.setRole(UserRole.CANDIDATE);
        testCandidate.setStatus(UserStatus.ACTIVE);

        testMainPackage = new ServicePackage();
        testMainPackage.setId(1L);
        testMainPackage.setName("Gói Tuyển Dụng Cơ Bản");
        testMainPackage.setDescription("Mô tả gói cơ bản");
        testMainPackage.setPrice(500000.0);
        testMainPackage.setDurationDays(30);
        testMainPackage.setMaxJobs(5);
        testMainPackage.setPackageType("MAIN");
        testMainPackage.setTargetRole(UserRole.RECRUITER);
        testMainPackage.setIsActive(true);

        testTopupPackage = new ServicePackage();
        testTopupPackage.setId(2L);
        testTopupPackage.setName("Gói Top-up Đăng Thêm Tin");
        testTopupPackage.setDescription("Mô tả gói topup");
        testTopupPackage.setPrice(200000.0);
        testTopupPackage.setDurationDays(null); // Topup can have null durationDays
        testTopupPackage.setMaxJobs(3);
        testTopupPackage.setPackageType("TOPUP");
        testTopupPackage.setTargetRole(UserRole.RECRUITER);
        testTopupPackage.setIsActive(true);

        testPayment = new Payment();
        testPayment.setId(1001L);
        testPayment.setUser(testRecruiter);
        testPayment.setServicePackage(testMainPackage);
        testPayment.setAmount(500000.0);
        testPayment.setStatus("PENDING");
        testPayment.setPaymentMethod("BANK_TRANSFER");
        testPayment.setCreatedAt(LocalDateTime.now());

        authRecruiter = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        testRecruiter.getEmail(), "password",
                        List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );

        authCandidate = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        testCandidate.getEmail(), "password",
                        List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
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
                var p = webRequest.getUserPrincipal();
                if (p instanceof UsernamePasswordAuthenticationToken token) {
                    return token.getPrincipal();
                }
                return null;
            }
        };

        ServicePackageService mockPackageService = new ServicePackageService(null, null, null) {
            @Override
            public List<ServicePackage> getPackagesForRole(UserRole role) {
                if (role == UserRole.RECRUITER) {
                    return List.of(testMainPackage, testTopupPackage);
                }
                return Collections.emptyList();
            }
            @Override
            public UserPackage getActivePackage(User user) {
                return null;
            }
            @Override
            public ServicePackage getById(Long id) {
                return id.equals(1L) ? testMainPackage : testTopupPackage;
            }
        };

        UserService mockUserService = new UserService(null, null, null) {
            @Override
            public User getByEmail(String email) {
                if (email != null && email.contains("recruiter")) return testRecruiter;
                return testCandidate;
            }
        };

        PaymentService mockPaymentService = new PaymentService(null, null, null) {
            @Override
            public Payment getOwnedPayment(Long id, Long userId) {
                return testPayment;
            }
            @Override
            public List<Payment> getHistoryByUser(User user) {
                return List.of(testPayment);
            }
        };

        BankPaymentConfig mockBank = new BankPaymentConfig();
        SePayGatewayConfig mockGateway = new SePayGatewayConfig();
        SePayCheckoutService mockSePayCheckout = new SePayCheckoutService(mockGateway) {
            @Override
            public Map<String, String> form(Payment payment) {
                return Map.of();
            }
        };

        UserPackageRepository mockUserPackageRepo = (UserPackageRepository) Proxy.newProxyInstance(
                UserPackageRepository.class.getClassLoader(),
                new Class<?>[]{UserPackageRepository.class},
                (proxy, method, args) -> Optional.empty()
        );

        PricingController pricingController = new PricingController(
                mockPackageService,
                mockUserService,
                mockPaymentService,
                mockBank,
                mockGateway,
                mockSePayCheckout,
                mockUserPackageRepo
        );

        mockMvc = MockMvcBuilders.standaloneSetup(pricingController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("Test GET /pricing as GUEST")
    void testPricingAsGuest() throws Exception {
        mockMvc.perform(get("/pricing")
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing/index"));
    }

    @Test
    @DisplayName("Test GET /pricing as RECRUITER")
    void testPricingAsRecruiter() throws Exception {
        mockMvc.perform(get("/pricing")
                .principal(authRecruiter)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing/index"));
    }

    @Test
    @DisplayName("Test GET /pricing as CANDIDATE")
    void testPricingAsCandidate() throws Exception {
        mockMvc.perform(get("/pricing")
                .principal(authCandidate)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing/index"));
    }

    @Test
    @DisplayName("Test GET /pricing/history as RECRUITER")
    void testPricingHistoryAsRecruiter() throws Exception {
        mockMvc.perform(get("/pricing/history")
                .principal(authRecruiter)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing/history"));
    }

    @Test
    @DisplayName("Test GET /pricing/history as CANDIDATE")
    void testPricingHistoryAsCandidate() throws Exception {
        mockMvc.perform(get("/pricing/history")
                .principal(authCandidate)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing/history"));
    }

    @Test
    @DisplayName("Test GET /pricing/resume/{id} as RECRUITER")
    void testPricingResume() throws Exception {
        mockMvc.perform(get("/pricing/resume/1001")
                .principal(authRecruiter)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing/gateway-checkout"));
    }
}
