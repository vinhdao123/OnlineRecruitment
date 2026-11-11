package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.CertificateRequest;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.CvGalleryTemplateVm;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.CvSummaryVm;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.ReferenceRequest;
import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.CvCertificate;
import OnlineRecruitment.OnlineRecruitment.entity.CvReference;
import OnlineRecruitment.OnlineRecruitment.entity.CvTemplate;
import OnlineRecruitment.OnlineRecruitment.entity.CvTemplateVersion;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvActivityRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvAwardRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvCertificateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvEducationRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvProjectRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvReferenceRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvSkillRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvTemplateVersionRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvWorkExperienceRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.service.CvApplicationPreviewService;
import OnlineRecruitment.OnlineRecruitment.service.CvExtraSectionService;
import OnlineRecruitment.OnlineRecruitment.service.CvFileStorageService;
import OnlineRecruitment.OnlineRecruitment.service.CvOwnershipService;
import OnlineRecruitment.OnlineRecruitment.service.CvSectionService;
import OnlineRecruitment.OnlineRecruitment.service.CvService;
import OnlineRecruitment.OnlineRecruitment.service.CvTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm tra các controller và service của module CV sau khi tách nhỏ và vá lỗi:
 * - Không xung đột route khi đăng ký cả 5 controller cùng lúc.
 * - Các route chính, file, template, section, extra-section phản hồi đúng.
 * - Batch load chi tiết CV không gây N+1 query, lưu đủ trường chứng chỉ / tham chiếu, và kiểm tra file upload an toàn.
 */
class CvModuleSplitTest {

    private UserRepository userRepository;
    private CvRepository cvRepository;
    private CvTemplateVersionRepository templateVersionRepository;
    private CvService cvService;
    private CvTemplateService templateService;
    private CvFileStorageService fileStorageService;
    private CvSectionService sectionService;
    private CvExtraSectionService extraSectionService;

    private MockMvc mockMvc;
    private Authentication auth;
    private User user;
    private Cv sampleCv;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        cvRepository = mock(CvRepository.class);
        templateVersionRepository = mock(CvTemplateVersionRepository.class);
        cvService = mock(CvService.class);
        templateService = mock(CvTemplateService.class);
        fileStorageService = mock(CvFileStorageService.class);
        sectionService = mock(CvSectionService.class);
        extraSectionService = mock(CvExtraSectionService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new CvController(userRepository, cvService, templateService, fileStorageService),
                new CvTemplateController(userRepository, templateService),
                new CvFileController(userRepository, cvService, fileStorageService),
                new CvSectionController(userRepository, cvService, sectionService),
                new CvExtraSectionController(userRepository, cvService, extraSectionService)
        ).build();

        user = new User();
        user.setId(10L);
        user.setEmail("candidate@example.com");
        user.setFullName("Nguyen Van A");

        auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), "unused",
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")));

        sampleCv = new Cv();
        sampleCv.setId(99L);
        sampleCv.setTitle("Java Developer CV");
        sampleCv.setIsUploaded(false);
        sampleCv.setIsDefault(true);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(cvService.getCvDetail(99L, 10L)).thenReturn(sampleCv);
    }

    @Test
    void coreCvRoutesDispatchCleanly() throws Exception {
        CvSummaryVm vm = new CvSummaryVm(99L, "Java Developer CV", "Java Dev",
                "ACTIVE", true, LocalDateTime.now(), "Hiện đại (2 cột)", null, false, null);
        when(cvService.listMyCvsPaged(10L, null, 0, 6)).thenReturn(new PageImpl<>(List.of(vm)));
        when(cvService.getDefaultCvTitle(10L)).thenReturn("Java Developer CV");
        when(cvService.getCvListWithDetailsForCvs(List.of(vm), 10L)).thenReturn(Map.of());

        mockMvc.perform(get("/candidate/cvs").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/cv/list"))
                .andExpect(model().attribute("totalItems", 1L))
                .andExpect(model().attribute("defaultCvTitle", "Java Developer CV"));

        when(cvService.getFullCvForPreview(99L, 10L)).thenReturn(Map.of("cv", sampleCv, "templateCode", "MODERN"));
        mockMvc.perform(get("/candidate/cvs/99/preview").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/cv/preview"));

        mockMvc.perform(post("/candidate/cvs/99/rename").param("title", "Backend CV").principal(auth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/candidate/cvs"));
        verify(cvService).renameCv(99L, "Backend CV", 10L);
    }

    @Test
    void templateAndCustomizerRoutesDispatchCleanly() throws Exception {
        when(templateService.buildGalleryPage(null, 0, 4)).thenReturn(Map.of("templates", List.of()));
        mockMvc.perform(get("/candidate/cvs/gallery"))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/cv/gallery"));

        when(templateService.buildGalleryPage(null, 0, 6)).thenReturn(Map.of("templates", List.of()));
        mockMvc.perform(get("/candidate/cvs/templates"))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/cv/templates"));

        CvGalleryTemplateVm tpl = new CvGalleryTemplateVm("STANDARD", "Tiêu chuẩn", "Tiêu chuẩn", 1L, List.of("#1e293b"), List.of("ATS"));
        when(templateService.buildGalleryTemplates()).thenReturn(List.of(tpl));
        when(templateService.getPreviewModelForCustomizer(null, "STANDARD", null, null, null, 10L))
                .thenReturn(Map.of("templateCode", "STANDARD"));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/candidate/cvs/customizer").param("templateCode", "STANDARD").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/cv/customizer"))
                .andExpect(model().attributeExists("activeTemplate", "galleryTemplates"));
    }

    @Test
    void fileAndSectionAndExtraSectionRoutesDispatchCleanly() throws Exception {
        when(fileStorageService.resolveAvatarFile(null)).thenReturn(null);
        when(fileStorageService.getDefaultAvatar()).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        // Người dùng chưa đăng nhập hoặc không phải chủ CV -> trả ảnh mặc định, không lộ avatar thật
        mockMvc.perform(get("/candidate/cvs/99/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
        verify(fileStorageService, never()).resolveAvatarFile(any());

        when(sectionService.getWorkExperiences(99L, 10L)).thenReturn(List.of());
        mockMvc.perform(get("/candidate/cvs/99/work-experiences").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/cv/work-experience"))
                .andExpect(model().attributeExists("cv", "works", "workRequest"));

        mockMvc.perform(post("/candidate/cvs/99/work-experiences/5/delete").principal(auth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/candidate/cvs/99/work-experiences"));
        verify(sectionService).deleteWorkExperience(99L, 5L, 10L);

        when(extraSectionService.getCertificates(99L, 10L)).thenReturn(List.of());
        mockMvc.perform(get("/candidate/cvs/99/certificates").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/cv/certificate"))
                .andExpect(model().attributeExists("cv", "certificates", "certRequest"));

        mockMvc.perform(post("/candidate/cvs/99/certificates/7/delete").principal(auth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/candidate/cvs/99/certificates"));
        verify(extraSectionService).deleteCertificate(99L, 7L, 10L);
    }

    @Test
    void cvServiceIncludesTemplateInfoAndBatchLoadsCardDetails() {
        CvOwnershipService ownership = mock(CvOwnershipService.class);
        CvSectionService sections = mock(CvSectionService.class);
        CvExtraSectionService extras = mock(CvExtraSectionService.class);
        CvTemplateService templates = new CvTemplateService(templateVersionRepository, cvRepository, ownership, sections);
        CvService realService = new CvService(cvRepository, templateVersionRepository, ownership, sections, extras, templates);

        CvTemplate tpl = new CvTemplate(1L, 1L, "Hiện đại (2 cột)", "MODERN", "ACTIVE");
        CvTemplateVersion ver = new CvTemplateVersion(4L, 1L, 1, "MODERN", true, tpl);
        Cv cv = new Cv();
        cv.setId(55L);
        cv.setTemplateVersionId(4L);
        cv.setTemplateVersion(ver);

        Candidate cand = new Candidate();
        cand.setId(77L);
        when(ownership.getCandidate(10L)).thenReturn(cand);
        when(cvRepository.findByCandidateIdAndIdIn(77L, List.of(55L))).thenReturn(List.of(cv));
        when(sections.getWorkExperiencesByCvIds(List.of(55L))).thenReturn(Map.of());
        when(sections.getEducationsByCvIds(List.of(55L))).thenReturn(Map.of());
        when(sections.getSkillsByCvIds(List.of(55L))).thenReturn(Map.of());
        when(sections.getProjectsByCvIds(List.of(55L))).thenReturn(Map.of());

        CvSummaryVm summary = new CvSummaryVm();
        summary.setId(55L);
        Map<Long, Map<String, Object>> details = realService.getCvListWithDetailsForCvs(List.of(summary), 10L);

        assertNotNull(details.get(55L));
        assertEquals("MODERN", details.get(55L).get("templateCode"));
        assertEquals("Hiện đại (2 cột)", details.get(55L).get("templateName"));
        verify(sections).getWorkExperiencesByCvIds(List.of(55L));
        verify(sections, never()).getWorkExperiences(anyLong(), anyLong());
    }

    @Test
    void resolveTemplateInfoFallsBackToRepositoryWhenAssociationNotLoaded() {
        CvOwnershipService ownership = mock(CvOwnershipService.class);
        CvSectionService sections = mock(CvSectionService.class);
        CvTemplateService templates = new CvTemplateService(templateVersionRepository, cvRepository, ownership, sections);

        CvTemplate tpl = new CvTemplate(2L, 1L, "Mẫu Truyền thống (Classic)", "CLASSIC", "ACTIVE");
        CvTemplateVersion ver = new CvTemplateVersion(10012L, 2L, 1, "CLASSIC", true, tpl);
        when(templateVersionRepository.findWithTemplateById(10012L)).thenReturn(Optional.of(ver));

        Cv cv = new Cv();
        cv.setId(36L);
        cv.setTemplateVersionId(10012L);
        cv.setTemplateVersion(null);

        CvTemplateService.TemplateInfo info = templates.resolveTemplateInfo(cv);
        assertEquals("CLASSIC", info.code());
        assertEquals("Mẫu Truyền thống (Classic)", info.name());
    }

    @Test
    void extraSectionServicePersistsCredentialIdDescriptionAndRelationship() {
        CvOwnershipService ownership = mock(CvOwnershipService.class);
        CvCertificateRepository certRepo = mock(CvCertificateRepository.class);
        CvAwardRepository awardRepo = mock(CvAwardRepository.class);
        CvActivityRepository activityRepo = mock(CvActivityRepository.class);
        CvReferenceRepository refRepo = mock(CvReferenceRepository.class);

        CvExtraSectionService service = new CvExtraSectionService(ownership, certRepo, awardRepo, activityRepo, refRepo);

        CertificateRequest certReq = new CertificateRequest("IELTS 7.5", "IDP", null, null, "CERT-999", "https://example.com", "Listening 8.5");
        service.addCertificate(10L, certReq, 1L);
        ArgumentCaptor<CvCertificate> certCap = ArgumentCaptor.forClass(CvCertificate.class);
        verify(certRepo).save(certCap.capture());
        assertEquals("CERT-999", certCap.getValue().getCredentialId());
        assertEquals("Listening 8.5", certCap.getValue().getDescription());

        ReferenceRequest refReq = new ReferenceRequest("Tran Van B", "Tech Lead", "ABC", "b@abc.com", "0901234567", "Quản lý trực tiếp");
        service.addReference(10L, refReq, 1L);
        ArgumentCaptor<CvReference> refCap = ArgumentCaptor.forClass(CvReference.class);
        verify(refRepo).save(refCap.capture());
        assertEquals("Quản lý trực tiếp", refCap.getValue().getRelationship());
    }

    @Test
    void storageServiceSanitizesAndValidatesCvFilesSafely() throws Exception {
        CvFileStorageService realStorage = new CvFileStorageService();
        assertTrue(realStorage.isSupportedCvFile("Resume.PDF"));
        assertTrue(realStorage.isSupportedCvFile("CV.DocX"));
        assertNull(realStorage.resolveCvFile("../../pom.xml"));
        assertNull(realStorage.resolveAvatarFile("../pom.xml"));

        // File giả mạo đuôi .pdf nhưng nội dung text thuần -> bị từ chối
        MockMultipartFile fakePdf = new MockMultipartFile("cvFile", "evil.pdf", "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> realStorage.storeCvFile(fakePdf));

        // File PDF viết hoa (.PDF) có magic bytes %PDF- hợp lệ + tên chứa ../ -> được sanitize thành storage/cvs/<uuid>.pdf
        MockMultipartFile validPdf = new MockMultipartFile("cvFile", "../../My CV.PDF", "application/pdf",
                "%PDF-1.4\n1 0 obj\n".getBytes(StandardCharsets.US_ASCII));
        String storedPath = realStorage.storeCvFile(validPdf);
        try {
            assertTrue(storedPath.matches("storage/cvs/[a-f0-9-]+\\.pdf"), "Unexpected stored path: " + storedPath);
            assertNotNull(realStorage.resolveCvFile(storedPath));
        } finally {
            Files.deleteIfExists(Path.of(storedPath));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void applicationPreviewServiceBuildsFullCvAndTemplateFromSnapshotOrDbFallback() {
        CandidateRepository candRepo = mock(CandidateRepository.class);
        CvWorkExperienceRepository workRepo = mock(CvWorkExperienceRepository.class);
        CvEducationRepository eduRepo = mock(CvEducationRepository.class);
        CvSkillRepository skillRepo = mock(CvSkillRepository.class);
        CvProjectRepository projRepo = mock(CvProjectRepository.class);
        CvCertificateRepository certRepo = mock(CvCertificateRepository.class);
        CvAwardRepository awardRepo = mock(CvAwardRepository.class);
        CvActivityRepository actRepo = mock(CvActivityRepository.class);
        CvReferenceRepository refRepo = mock(CvReferenceRepository.class);

        CvOwnershipService ownership = mock(CvOwnershipService.class);
        CvSectionService sections = mock(CvSectionService.class);
        CvTemplateService templates = new CvTemplateService(templateVersionRepository, cvRepository, ownership, sections);

        CvApplicationPreviewService service = new CvApplicationPreviewService(
                candRepo, cvRepository, templateVersionRepository, templates,
                workRepo, eduRepo, skillRepo, projRepo, certRepo, awardRepo, actRepo, refRepo);

        User candUser = new User();
        candUser.setFullName("Bùi Ngọc Thạch");
        candUser.setEmail("candidate01@gmail.com");
        Candidate cand = new Candidate();
        cand.setId(1L);
        cand.setUser(candUser);
        when(candRepo.findById(1L)).thenReturn(Optional.of(cand));

        CvTemplate modernTpl = new CvTemplate(2L, 1L, "Mẫu Hiện đại (Modern)", "MODERN", "ACTIVE");
        CvTemplateVersion modernVer = new CvTemplateVersion(10008L, 2L, 1, "MODERN", true, modernTpl);
        when(templateVersionRepository.findWithTemplateById(10008L)).thenReturn(Optional.of(modernVer));

        Cv dbCv = new Cv();
        dbCv.setId(1L);
        dbCv.setTitle("CV Java Developer (Test)");
        dbCv.setTemplateVersionId(10008L);
        dbCv.setTemplateVersion(modernVer);
        dbCv.setAccentColor("#0f766e");
        when(cvRepository.findWithTemplateById(1L)).thenReturn(Optional.of(dbCv));

        // 1. Snapshot cũ không đầy đủ -> fallback nạp full CV + template MODERN từ DB
        Map<String, Object> incompleteSnap = Map.of("cv", Map.of("id", 1, "title", "CV Java Developer"), "workExperiences", List.of());
        Map<String, Object> fallbackModel = service.buildPreviewModel(1L, 1L, 1L, null, incompleteSnap, null, null, null);
        assertEquals("MODERN", fallbackModel.get("templateCode"));
        assertEquals("#0f766e", fallbackModel.get("accentColor"));
        verify(eduRepo).findByCvIdOrderByDisplayOrderAsc(1L);

        // 2. Snapshot đầy đủ -> dựng từ JSON snapshot (kèm parsed LocalDate & skillName)
        Map<String, Object> fullSnap = Map.of(
                "cv", Map.of("title", "Snapshot CV", "accentColor", "#1e3a8a", "templateVersionId", 10008L),
                "workExperiences", List.of(Map.of("company_name", "FPT", "job_title", "Backend Dev", "start_date", "2024-01-15", "is_current", true)),
                "educations", List.of(Map.of("institution", "FPTU", "degree", "Kỹ sư")),
                "skills", List.of(Map.of("skillName", "Java", "proficiency", "ADVANCED")),
                "projects", List.of()
        );
        Map<String, Object> snapModel = service.buildPreviewModel(1L, 1L, 10008L, null, fullSnap, null, null, null);
        assertEquals("MODERN", snapModel.get("templateCode"));
        assertEquals("#1e3a8a", snapModel.get("accentColor"));
        List<?> works = (List<?>) snapModel.get("works");
        assertEquals(1, works.size());
    }
}
