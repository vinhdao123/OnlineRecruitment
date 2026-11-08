package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.*;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.CvTemplateVersion;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvTemplateVersionRepository;
import OnlineRecruitment.OnlineRecruitment.service.CvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/candidate/cvs")
@RequiredArgsConstructor
@Slf4j
public class CvController {

    private final CvService cvService;
    private final CvRepository cvRepository;
    private final UserRepository userRepository;
    private final CvTemplateVersionRepository templateVersionRepository;

    // Helper lấy userId
    private Long getUserId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return user.getId();
    }

    // Taskbar dùng chung cần currentUser (họ tên, avatar) trên mọi trang của module CV
    @ModelAttribute("currentUser")
    public User populateCurrentUser(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByEmail(auth.getName())
                    .or(() -> userRepository.findByUsername(auth.getName()))
                    .orElse(null);
        }
        return null;
    }

    @GetMapping
    public String list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            Authentication auth, Model model) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .or(() -> userRepository.findByUsername(auth.getName()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            Page<CvSummaryVm> cvPage = cvService.listMyCvsPaged(user.getId(), keyword, page, size);
            List<CvSummaryVm> cvs = cvPage.getContent();
            Map<Long, Map<String, Object>> cvDataMap = cvService.getCvListWithDetailsForCvs(cvs, user.getId());
            String defaultCvTitle = cvService.listMyCvs(user.getId()).stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsDefault()))
                    .map(CvSummaryVm::getTitle)
                    .findFirst()
                    .orElse(null);

            model.addAttribute("cvs", cvs);
            model.addAttribute("cvPage", cvPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", cvPage.getTotalPages());
            model.addAttribute("totalItems", cvPage.getTotalElements());
            model.addAttribute("size", size);
            model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
            model.addAttribute("hasCvs", cvPage.getTotalElements() > 0);
            model.addAttribute("profileUser", user);
            model.addAttribute("currentUser", user);
            model.addAttribute("cvDataMap", cvDataMap);
            model.addAttribute("defaultCvTitle", defaultCvTitle);
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("hasCvs", false);
            model.addAttribute("cvs", new ArrayList<CvSummaryVm>());
            model.addAttribute("cvDataMap", new HashMap<Long, Map<String, Object>>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalItems", 0);
            model.addAttribute("keyword", "");
        }
        return "candidate/cv/list";
    }

    // ==========================================
    // THƯ VIỆN MẪU CV & BỘ TÙY BIẾN
    // ==========================================

    @GetMapping("/gallery")
    public String gallery(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "4") int size,
            @RequestParam(value = "category", required = false) String category,
            Model model) {
        List<CvGalleryTemplateVm> allTemplates = buildGalleryTemplates();

        List<CvGalleryTemplateVm> filtered = allTemplates;
        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category.trim())) {
            String cat = category.trim();
            filtered = allTemplates.stream()
                    .filter(t -> t.getCode().equalsIgnoreCase(cat)
                            || (t.getTags() != null && t.getTags().stream().anyMatch(tag -> tag.equalsIgnoreCase(cat)))
                            || (t.getFilterLabel() != null && t.getFilterLabel().equalsIgnoreCase(cat)))
                    .toList();
        }

        int totalItems = filtered.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
        int validPage = totalPages <= 1 ? 0 : Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = validPage * size;
        int toIndex = Math.min(fromIndex + size, totalItems);

        List<CvGalleryTemplateVm> pagedTemplates = (fromIndex < totalItems)
                ? filtered.subList(fromIndex, toIndex)
                : Collections.emptyList();

        model.addAttribute("templates", pagedTemplates);
        model.addAttribute("allTemplatesCount", allTemplates.size());
        model.addAttribute("currentPage", validPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("size", size);
        model.addAttribute("selectedCategory", category != null && !category.trim().isEmpty() ? category.trim() : "ALL");
        return "candidate/cv/gallery";
    }

    @GetMapping("/customizer")
    public String customizer(
            @RequestParam(value = "templateCode", required = false) String templateCode,
            @RequestParam(value = "cvId", required = false) Long cvId,
            @RequestParam(value = "accentColor", required = false) String accentColor,
            Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            Long selectedCvId = cvId;
            Map<String, Object> previewData = cvService.getPreviewModelForCustomizer(
                    selectedCvId, templateCode, null, accentColor, null, userId);
            model.addAllAttributes(previewData);

            List<CvGalleryTemplateVm> galleryTemplates = buildGalleryTemplates();
            String activeCode = String.valueOf(previewData.get("templateCode"));
            CvGalleryTemplateVm activeTemplate = galleryTemplates.stream()
                    .filter(t -> t.getCode().equals(activeCode))
                    .findFirst()
                    .orElse(galleryTemplates.get(0));

            model.addAttribute("galleryTemplates", galleryTemplates);
            model.addAttribute("activeTemplate", activeTemplate);
            model.addAttribute("profileUser", userRepository.findById(userId).orElse(null));
            return "candidate/cv/customizer";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/candidate/cvs/gallery";
        }
    }

    private static String normalizeTemplateCode(String code) {
        return code == null ? "" : code.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    private CvGalleryTemplateVm galleryItem(String code, String name, String filterLabel, Map<String, Long> versionByCode,
                                            List<String> colors, List<String> tags) {
        return new CvGalleryTemplateVm(code, name, filterLabel, versionByCode.get(code), colors, tags);
    }

    /** 5 mẫu CV chuẩn; versionId được ghép theo mã mẫu (cv_templates.code) đang publish trong DB. */
    private List<CvGalleryTemplateVm> buildGalleryTemplates() {
        Map<String, Long> versionByCode = new HashMap<>();
        try {
            for (CvTemplateVersion v : templateVersionRepository.findByIsPublishedTrue()) {
                if (v.getTemplate() != null) {
                    versionByCode.merge(normalizeTemplateCode(v.getTemplate().getCode()), v.getId(), Math::max);
                }
            }
        } catch (Exception ignored) {
            // Không ghép được phiên bản -> nút "Tạo hồ sơ mới" vẫn dẫn tới form chọn mẫu
        }

        List<CvGalleryTemplateVm> list = new ArrayList<>();
        list.add(galleryItem("STANDARD", "Tiêu chuẩn", "Tiêu chuẩn", versionByCode,
                List.of("#1e293b", "#475569", "#1e3a8a", "#450a0a"), List.of("ATS", "Đơn giản")));
        list.add(galleryItem("MINIMALIST", "Tiêu chuẩn (Ít kinh nghiệm)", "Tiêu chuẩn (Ít kinh nghiệm)", versionByCode,
                List.of("#1e40af", "#065f46", "#9f1239", "#854d0e"), List.of("ATS", "Đơn giản", "Chuyên nghiệp")));
        list.add(galleryItem("CREATIVE", "Thanh lịch", "Thanh lịch", versionByCode,
                List.of("#be123c", "#4338ca", "#15803d", "#d97706"), List.of("ATS", "Đơn giản", "Hiện đại")));
        list.add(galleryItem("MODERN", "Hiện đại (2 cột)", "Hiện đại (2 cột)", versionByCode,
                List.of("#0f766e", "#0284c7", "#2563eb", "#4f46e5"), List.of("Chuyên nghiệp", "Hiện đại", "2 cột")));
        list.add(galleryItem("ATS_FRIENDLY", "Chuẩn ATS", "Chuẩn ATS", versionByCode,
                List.of("#111827", "#1e40af", "#065f46", "#7c2d12"), List.of("ATS", "Đơn giản", "Chuyên nghiệp")));
        return list;
    }
    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "sourceCvId", required = false) Long sourceCvId,
            @RequestParam(value = "accentColor", required = false) String accentColor,
            @RequestParam(value = "title", required = false) String title,
            Authentication auth, Model model) {

        CreateCvRequest req = new CreateCvRequest();

        if (templateId != null) {
            req.setTemplateVersionId(templateId);
        }
        if (accentColor != null && accentColor.matches("^#[0-9A-Fa-f]{6}$")) {
            req.setAccentColor(accentColor);
        }
        if (title != null && !title.trim().isEmpty()) {
            req.setTitle(title.trim());
        } else {
            User user = populateCurrentUser(auth);
            if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                req.setTitle("CV " + user.getFullName());
            }
        }

        model.addAttribute("cvRequest", req);
        model.addAttribute("templates", cvService.getAvailableTemplates());
        model.addAttribute("isEdit", false);

        if (sourceCvId != null) {
            model.addAttribute("sourceCvId", sourceCvId);
        }

        return "candidate/cv/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("cvRequest") CreateCvRequest req,
                         BindingResult result,
                         @RequestParam(value = "sourceCvId", required = false) Long sourceCvId,
                         Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("templates", cvService.getAvailableTemplates());
            model.addAttribute("isEdit", false);
            return "candidate/cv/form";
        }
        try {
            Long newCvId = cvService.createCvAndReturnId(req, getUserId(auth));

            if (sourceCvId != null) {
                cvService.copyCvData(sourceCvId, newCvId, getUserId(auth));
            }

            ra.addFlashAttribute("successMessage", "Tạo CV thành công!");
            return "redirect:/candidate/cvs";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("templates", cvService.getAvailableTemplates());
            model.addAttribute("isEdit", false);
            return "candidate/cv/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(value = "edit", required = false, defaultValue = "false") boolean isEdit,
                         Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Cv cv = cvService.getCvDetail(id, getUserId(auth));
            if (Boolean.TRUE.equals(cv.getIsUploaded())) {
                isEdit = false;
            }
            model.addAttribute("cv", cv);
            model.addAttribute("isEditMode", isEdit);

            if (isEdit) {
                UpdateCvRequest req = new UpdateCvRequest(cv.getTitle(), cv.getProfessionalTitle(), cv.getSummary(), cv.getAccentColor(), cv.getAvatarUrl());
                model.addAttribute("cvRequest", req);
            }
            return "candidate/cv/detail";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }
    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("cvRequest") UpdateCvRequest req,
                         BindingResult result,
                         @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                         Authentication auth, Model model, RedirectAttributes ra) {
        Cv existing = cvService.getCvDetail(id, getUserId(auth));
        if (Boolean.TRUE.equals(existing.getIsUploaded())) {
            ra.addFlashAttribute("errorMessage", "CV tải lên từ máy tính không hỗ trợ chỉnh sửa trực tuyến!");
            return "redirect:/candidate/cvs/" + id;
        }

        if (result.hasErrors()) {
            model.addAttribute("cvId", id);
            model.addAttribute("isEditMode", true);
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            return "candidate/cv/detail";
        }
        try {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    String uploadDir = System.getProperty("user.dir") + "/uploads/";
                    File dir = new File(uploadDir);
                    if (!dir.exists()) dir.mkdirs();

                    String fileName = System.currentTimeMillis() + "_" + avatarFile.getOriginalFilename();
                    Path path = Paths.get(uploadDir + fileName);
                    Files.copy(avatarFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                    // Đồng bộ sang static/uploads để phục vụ ResourceHandler nếu có
                    try {
                        String staticDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
                        File sDir = new File(staticDir);
                        if (!sDir.exists()) sDir.mkdirs();
                        Files.copy(path, Paths.get(staticDir + fileName), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ignored) {}

                    req.setAvatarUrl("/uploads/" + fileName);
                } catch (java.io.IOException e) {
                    throw new RuntimeException("Lỗi khi lưu ảnh: " + e.getMessage());
                }
            }

            cvService.updateCv(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Cập nhật thông tin CV thành công!");
            return "redirect:/candidate/cvs/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("cvId", id);
            model.addAttribute("isEditMode", true);
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            return "candidate/cv/detail";
        }
    }

    @PostMapping("/{id}/default")
    public String setDefault(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.setDefault(id, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã đặt làm CV mặc định!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs";
    }

    @PostMapping("/{id}/clone")
    public String clone(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.cloneCv(id, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã nhân bản CV thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs";
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.softDelete(id, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa CV!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs";
    }

    // ==========================================
    // CÁC ROUTE CHO PHẦN NỘI DUNG CV
    // ==========================================

    private File resolveFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) return null;
        String cleanUrl = fileUrl.trim().replace("\\", "/");
        String relative = cleanUrl.startsWith("/") ? cleanUrl.substring(1) : cleanUrl;

        // 1. user.dir + relative
        File f = new File(System.getProperty("user.dir"), relative);
        if (f.exists() && f.isFile()) return f;

        // 2. static/ + relative
        f = new File(System.getProperty("user.dir") + "/src/main/resources/static", relative);
        if (f.exists() && f.isFile()) return f;

        // 3. Tên file trong uploads/cv
        String fileName = Paths.get(cleanUrl).getFileName().toString();
        f = new File(System.getProperty("user.dir") + "/uploads/cv", fileName);
        if (f.exists() && f.isFile()) return f;

        // 4. Tên file trong uploads
        f = new File(System.getProperty("user.dir") + "/uploads", fileName);
        if (f.exists() && f.isFile()) return f;

        // 5. target/classes/static
        f = new File(System.getProperty("user.dir") + "/target/classes/static", relative);
        if (f.exists() && f.isFile()) return f;

        return null;
    }

    private File resolveAvatarFile(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) return null;
        String cleanUrl = avatarUrl.trim().replace("\\", "/");
        String relative = cleanUrl.startsWith("/") ? cleanUrl.substring(1) : cleanUrl;

        File f = new File(System.getProperty("user.dir"), relative);
        if (f.exists() && f.isFile()) return f;

        f = new File(System.getProperty("user.dir") + "/src/main/resources/static", relative);
        if (f.exists() && f.isFile()) return f;

        String fileName = Paths.get(cleanUrl).getFileName().toString();
        f = new File(System.getProperty("user.dir") + "/uploads", fileName);
        if (f.exists() && f.isFile()) return f;

        f = new File(System.getProperty("user.dir") + "/uploads/avatars", fileName);
        if (f.exists() && f.isFile()) return f;

        return null;
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> getCvAvatar(@PathVariable Long id) {
        try {
            Cv cv = cvRepository.findById(id).orElse(null);
            String avatarUrl = (cv != null) ? cv.getAvatarUrl() : null;

            File file = resolveAvatarFile(avatarUrl);
            if (file != null && file.exists()) {
                String mime = Files.probeContentType(file.toPath());
                MediaType mediaType = (mime != null) ? MediaType.parseMediaType(mime) : MediaType.IMAGE_JPEG;
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .body(new FileSystemResource(file));
            }
        } catch (Exception e) {
            log.warn("Không thể tải avatar CV {}: {}", id, e.getMessage());
        }

        // Luôn trả về ảnh avatar mặc định, không bao giờ để lỗi vỡ ảnh
        try {
            File defFile = new File(System.getProperty("user.dir") + "/src/main/resources/static/images/cv-sample-avatar.jpg");
            if (defFile.exists()) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(new FileSystemResource(defFile));
            }
            Resource defaultAvatar = new ClassPathResource("static/images/cv-sample-avatar.jpg");
            if (defaultAvatar.exists()) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(defaultAvatar);
            }
        } catch (Exception ignored) {}

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getCvFile(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            Cv cv = cvService.getCvDetail(id, userId);
            if (cv.getFileUrl() == null || cv.getFileUrl().trim().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = resolveFile(cv.getFileUrl());
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            String mime = Files.probeContentType(file.toPath());
            MediaType mediaType;
            if (mime != null) {
                mediaType = MediaType.parseMediaType(mime);
            } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            String downloadName = cv.getTitle() != null && !cv.getTitle().isEmpty() ? cv.getTitle() : file.getName();
            if (!downloadName.toLowerCase().endsWith(".pdf") && file.getName().toLowerCase().endsWith(".pdf")) {
                downloadName += ".pdf";
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadName + "\"")
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            log.error("Lỗi khi xem file CV {}: ", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadCvFile(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            Cv cv = cvService.getCvDetail(id, userId);
            if (cv.getFileUrl() == null || cv.getFileUrl().trim().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = resolveFile(cv.getFileUrl());
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            String originalName = file.getName();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(originalName, StandardCharsets.UTF_8).build().toString())
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            log.error("Lỗi khi tải file CV {}: ", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/preview")
    public String previewCv(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            Cv cv = cvService.getCvDetail(id, userId);
            if (Boolean.TRUE.equals(cv.getIsUploaded())) {
                return "redirect:/candidate/cvs/" + id + "/file";
            }
            java.util.Map<String, Object> cvData = cvService.getFullCvForPreview(id, userId);
            model.addAllAttributes(cvData);
            return "candidate/cv/preview";
        } catch (Exception e) {
            log.error("Lỗi khi tải trang xem trước CV {}: ", id, e);
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @GetMapping("/{id}/work-experiences")
    public String listWorkExp(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            try {
                model.addAttribute("works", cvService.getWorkExperiences(id, userId));
            } catch (Exception ex) {
                model.addAttribute("works", Collections.emptyList());
            }
            model.addAttribute("workRequest", new WorkExperienceRequest());
            return "candidate/cv/work-experience";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/work-experiences/add")
    public String addWorkExp(@PathVariable Long id, @Valid @ModelAttribute("workRequest") WorkExperienceRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("works", cvService.getWorkExperiences(id, getUserId(auth)));
            return "candidate/cv/work-experience";
        }
        try {
            cvService.addWorkExperience(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm kinh nghiệm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/work-experiences";
    }

    @PostMapping("/{id}/work-experiences/{workId}/delete")
    public String deleteWorkExp(@PathVariable Long id, @PathVariable Long workId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteWorkExperience(id, workId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa kinh nghiệm!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/work-experiences";
    }

    @GetMapping("/{id}/educations")
    public String listEdu(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            try {
                model.addAttribute("educations", cvService.getEducations(id, userId));
            } catch (Exception ex) {
                model.addAttribute("educations", Collections.emptyList());
            }
            model.addAttribute("eduRequest", new EducationRequest());
            return "candidate/cv/education";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/educations/add")
    public String addEdu(@PathVariable Long id, @Valid @ModelAttribute("eduRequest") EducationRequest req,
                         BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("educations", cvService.getEducations(id, getUserId(auth)));
            return "candidate/cv/education";
        }
        try {
            cvService.addEducation(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm học vấn thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/educations";
    }

    @PostMapping("/{id}/educations/{eduId}/delete")
    public String deleteEdu(@PathVariable Long id, @PathVariable Long eduId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteEducation(id, eduId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa học vấn!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/educations";
    }

    @GetMapping("/{id}/skills")
    public String listSkills(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            try {
                model.addAttribute("cvSkills", cvService.getSkills(id, userId));
            } catch (Exception ex) {
                model.addAttribute("cvSkills", Collections.emptyList());
            }
            model.addAttribute("allSkills", cvService.getAllAvailableSkills());
            model.addAttribute("skillRequest", new SkillRequest());
            return "candidate/cv/skills";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/skills/add")
    public String addSkill(@PathVariable Long id, @Valid @ModelAttribute("skillRequest") SkillRequest req,
                           BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("cvSkills", cvService.getSkills(id, getUserId(auth)));
            model.addAttribute("allSkills", cvService.getAllAvailableSkills());
            return "candidate/cv/skills";
        }
        try {
            cvService.addSkill(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm kỹ năng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/skills";
    }

    @PostMapping("/{id}/skills/{skillId}/delete")
    public String deleteSkill(@PathVariable Long id, @PathVariable Long skillId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteSkill(id, skillId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa kỹ năng!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/skills";
    }

    @GetMapping("/{id}/projects")
    public String listProjects(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            try {
                model.addAttribute("projects", cvService.getProjects(id, userId));
            } catch (Exception ex) {
                model.addAttribute("projects", Collections.emptyList());
            }
            model.addAttribute("projectRequest", new ProjectRequest());
            return "candidate/cv/project";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/projects/add")
    public String addProject(@PathVariable Long id, @Valid @ModelAttribute("projectRequest") ProjectRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("projects", cvService.getProjects(id, getUserId(auth)));
            return "candidate/cv/project";
        }
        try {
            cvService.addProject(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm dự án thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/projects";
    }

    @PostMapping("/{id}/projects/{projectId}/delete")
    public String deleteProject(@PathVariable Long id, @PathVariable Long projectId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteProject(id, projectId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa dự án!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/projects";
    }

    // ---------- CHỨNG CHỈ ----------
    @GetMapping("/{id}/certificates")
    public String listCertificate(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            try {
                model.addAttribute("certificates", cvService.getCertificates(id, userId));
            } catch (Exception ex) {
                model.addAttribute("certificates", Collections.emptyList());
            }
            model.addAttribute("certRequest", new CertificateRequest());
            return "candidate/cv/certificate";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/certificates/add")
    public String addCertificate(@PathVariable Long id, @Valid @ModelAttribute("certRequest") CertificateRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("certificates", cvService.getCertificates(id, getUserId(auth)));
            return "candidate/cv/certificate";
        }
        try {
            cvService.addCertificate(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm chứng chỉ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/certificates";
    }

    @PostMapping("/{id}/certificates/{certId}/delete")
    public String deleteCertificate(@PathVariable Long id, @PathVariable("certId") Long certId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteCertificate(id, certId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa chứng chỉ!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/certificates";
    }

    // ---------- GIẢI THƯỞNG ----------
    @GetMapping("/{id}/awards")
    public String listAward(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            try {
                model.addAttribute("awards", cvService.getAwards(id, userId));
            } catch (Exception ex) {
                model.addAttribute("awards", Collections.emptyList());
            }
            model.addAttribute("awardRequest", new AwardRequest());
            return "candidate/cv/award";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/awards/add")
    public String addAward(@PathVariable Long id, @Valid @ModelAttribute("awardRequest") AwardRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("awards", cvService.getAwards(id, getUserId(auth)));
            return "candidate/cv/award";
        }
        try {
            cvService.addAward(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm giải thưởng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/awards";
    }

    @PostMapping("/{id}/awards/{awardId}/delete")
    public String deleteAward(@PathVariable Long id, @PathVariable("awardId") Long awardId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteAward(id, awardId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa giải thưởng!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/awards";
    }

    // ---------- HOẠT ĐỘNG NGOẠI KHÓA ----------
    @GetMapping("/{id}/activities")
    public String listActivity(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            try {
                model.addAttribute("activities", cvService.getActivities(id, userId));
            } catch (Exception ex) {
                model.addAttribute("activities", Collections.emptyList());
            }
            model.addAttribute("activityRequest", new ActivityRequest());
            return "candidate/cv/activity";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/activities/add")
    public String addActivity(@PathVariable Long id, @Valid @ModelAttribute("activityRequest") ActivityRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("activities", cvService.getActivities(id, getUserId(auth)));
            return "candidate/cv/activity";
        }
        try {
            cvService.addActivity(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm hoạt động thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/activities";
    }

    @PostMapping("/{id}/activities/{activityId}/delete")
    public String deleteActivity(@PathVariable Long id, @PathVariable("activityId") Long activityId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteActivity(id, activityId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa hoạt động!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/activities";
    }

    // ---------- NGƯỜI THAM CHIẾU ----------
    @GetMapping("/{id}/references")
    public String listReference(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            try {
                model.addAttribute("references", cvService.getReferences(id, userId));
            } catch (Exception ex) {
                model.addAttribute("references", Collections.emptyList());
            }
            model.addAttribute("referenceRequest", new ReferenceRequest());
            return "candidate/cv/reference";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/references/add")
    public String addReference(@PathVariable Long id, @Valid @ModelAttribute("referenceRequest") ReferenceRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("references", cvService.getReferences(id, getUserId(auth)));
            return "candidate/cv/reference";
        }
        try {
            cvService.addReference(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm người tham chiếu thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/references";
    }

    @PostMapping("/{id}/references/{refId}/delete")
    public String deleteReference(@PathVariable Long id, @PathVariable("refId") Long refId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteReference(id, refId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa người tham chiếu!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/references";
    }

    @GetMapping("/templates")
    public String listTemplates(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "category", required = false) String category,
            Model model) {
        List<CvGalleryTemplateVm> allTemplates = buildGalleryTemplates();

        List<CvGalleryTemplateVm> filtered = allTemplates;
        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category.trim())) {
            String cat = category.trim();
            filtered = allTemplates.stream()
                    .filter(t -> t.getCode().equalsIgnoreCase(cat)
                            || (t.getTags() != null && t.getTags().stream().anyMatch(tag -> tag.equalsIgnoreCase(cat)))
                            || (t.getFilterLabel() != null && t.getFilterLabel().equalsIgnoreCase(cat)))
                    .toList();
        }

        int totalItems = filtered.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
        int validPage = totalPages <= 1 ? 0 : Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = validPage * size;
        int toIndex = Math.min(fromIndex + size, totalItems);

        List<CvGalleryTemplateVm> pagedTemplates = (fromIndex < totalItems)
                ? filtered.subList(fromIndex, toIndex)
                : Collections.emptyList();

        model.addAttribute("templates", pagedTemplates);
        model.addAttribute("allTemplatesCount", allTemplates.size());
        model.addAttribute("currentPage", validPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("size", size);
        model.addAttribute("selectedCategory", category != null && !category.trim().isEmpty() ? category.trim() : "ALL");
        return "candidate/cv/templates";
    }

    @PostMapping("/upload")
    public String uploadCv(@RequestParam("cvFile") MultipartFile file,
                           @RequestParam(value = "title", required = false) String customTitle,
                           Authentication auth, RedirectAttributes ra) {
        try {
            if (file.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng chọn file!");
                return "redirect:/candidate/cvs";
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || !(originalName.endsWith(".pdf") || originalName.endsWith(".doc") || originalName.endsWith(".docx"))) {
                ra.addFlashAttribute("errorMessage", "Chỉ chấp nhận file PDF hoặc Word!");
                return "redirect:/candidate/cvs";
            }

            String uploadDir = System.getProperty("user.dir") + "/uploads/cv/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String fileName = System.currentTimeMillis() + "_" + originalName;
            Path path = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // Đồng bộ sang static/uploads/cv
            try {
                String staticDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/cv/";
                File sDir = new File(staticDir);
                if (!sDir.exists()) sDir.mkdirs();
                Files.copy(path, Paths.get(staticDir + fileName), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}

            // Xác định tên/tiêu đề CV
            String cvTitle = (customTitle != null && !customTitle.trim().isEmpty()) ? customTitle.trim() : originalName;

            // Gọi hàm với tên CV được chỉ định
            cvService.uploadCvFile("/uploads/cv/" + fileName, cvTitle, getUserId(auth));

            ra.addFlashAttribute("successMessage", "Tải CV lên thành công với tên: \"" + cvTitle + "\"!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi tải file: " + e.getMessage());
        }
        return "redirect:/candidate/cvs";
    }

    @GetMapping("/{id}/rename")
    public String renameCvGet(@PathVariable Long id) {
        return "redirect:/candidate/cvs/" + id;
    }

    @PostMapping("/{id}/rename")
    public String renameCv(@PathVariable Long id, @RequestParam("title") String newTitle,
                           @RequestHeader(value = "Referer", required = false) String referer,
                           Authentication auth, RedirectAttributes ra) {
        try {
            cvService.renameCv(id, newTitle, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã đổi tên CV thành: \"" + newTitle.trim() + "\"!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        if (referer != null && referer.contains("/candidate/cvs/" + id)) {
            return "redirect:/candidate/cvs/" + id;
        }
        return "redirect:/candidate/cvs";
    }
}