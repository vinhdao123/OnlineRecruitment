package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.CvGalleryTemplateVm;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.CvTemplateOptionVm;
import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.CvTemplateVersion;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mẫu CV: danh sách mẫu, thư viện mẫu (gallery), xác định mẫu của 1 CV và bộ tùy biến (customizer).
 */
@Service
@RequiredArgsConstructor
public class CvTemplateService {

    public static final String DEFAULT_TEMPLATE_CODE = "STANDARD";
    public static final String DEFAULT_TEMPLATE_NAME = "Tiêu chuẩn";

    private static final Set<String> CUSTOMIZER_TEMPLATE_CODES =
            Set.of("STANDARD", "MINIMALIST", "CREATIVE", "MODERN", "ATS_FRIENDLY");
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final CvTemplateVersionRepository templateVersionRepository;
    private final CvRepository cvRepository;
    private final CvOwnershipService ownershipService;
    private final CvSectionService sectionService;

    // ==========================================
    // DANH SÁCH MẪU (FORM TẠO / SỬA CV)
    // ==========================================

    @Transactional(readOnly = true)
    public List<CvTemplateOptionVm> getAvailableTemplates() {
        return templateVersionRepository.findByIsPublishedTrue().stream()
                .map(v -> new CvTemplateOptionVm(
                        v.getId(),
                        v.getTemplate() != null ? v.getTemplate().getName() : "Template",
                        v.getVersionNumber()
                ))
                .toList();
    }

    // ==========================================
    // XÁC ĐỊNH MẪU CỦA MỘT CV
    // ==========================================

    /** Mã mẫu (đã chuẩn hoá in hoa) và tên mẫu của một CV. */
    public record TemplateInfo(String code, String name) {}

    @Transactional(readOnly = true)
    public TemplateInfo resolveTemplateInfo(Cv cv) {
        String templateCode = DEFAULT_TEMPLATE_CODE;
        String templateName = DEFAULT_TEMPLATE_NAME;

        CvTemplateVersion tv = cv.getTemplateVersion();
        if (tv != null && Hibernate.isInitialized(tv)
                && tv.getTemplate() != null && Hibernate.isInitialized(tv.getTemplate())) {
            templateCode = tv.getTemplate().getCode();
            templateName = tv.getTemplate().getName();
        } else if (cv.getTemplateVersionId() != null) {
            CvTemplateVersion loaded = templateVersionRepository.findWithTemplateById(cv.getTemplateVersionId())
                    .or(() -> templateVersionRepository.findById(cv.getTemplateVersionId()))
                    .orElse(null);
            if (loaded != null && loaded.getTemplate() != null) {
                templateCode = loaded.getTemplate().getCode();
                templateName = loaded.getTemplate().getName();
            } else if (loaded != null && loaded.getLayoutDefinition() != null) {
                templateCode = loaded.getLayoutDefinition();
            }
        }
        if (templateCode == null || templateCode.isBlank()) {
            templateCode = DEFAULT_TEMPLATE_CODE;
        }
        return new TemplateInfo(templateCode.trim().toUpperCase(), templateName);
    }

    // ==========================================
    // THƯ VIỆN MẪU (GALLERY)
    // ==========================================

    private static String normalizeTemplateCode(String code) {
        return code == null ? "" : code.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    private CvGalleryTemplateVm galleryItem(String code, String name, String filterLabel, Map<String, Long> versionByCode,
                                            List<String> colors, List<String> tags) {
        return new CvGalleryTemplateVm(code, name, filterLabel, versionByCode.get(code), colors, tags);
    }

    /** 5 mẫu CV chuẩn; versionId được ghép theo mã mẫu (cv_templates.code) đang publish trong DB. */
    @Transactional(readOnly = true)
    public List<CvGalleryTemplateVm> buildGalleryTemplates() {
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

    /**
     * Lọc theo danh mục + phân trang thư viện mẫu.
     * Trả về các thuộc tính model: templates, allTemplatesCount, currentPage, totalPages, totalItems, size, selectedCategory.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> buildGalleryPage(String category, int page, int size) {
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

        Map<String, Object> model = new HashMap<>();
        model.put("templates", pagedTemplates);
        model.put("allTemplatesCount", allTemplates.size());
        model.put("currentPage", validPage);
        model.put("totalPages", totalPages);
        model.put("totalItems", totalItems);
        model.put("size", size);
        model.put("selectedCategory", category != null && !category.trim().isEmpty() ? category.trim() : "ALL");
        return model;
    }

    // ==========================================
    // BỘ TÙY BIẾN MẪU CV (CUSTOMIZER)
    // ==========================================

    /**
     * Dựng dữ liệu xem trước cho bộ tùy biến mẫu CV.
     *
     * @param selectedCvId CV cần xem trước; null = CV mặc định (nếu có), 0 = dùng dữ liệu mẫu
     * @param templateCode mã mẫu (STANDARD, MINIMALIST, CREATIVE, MODERN, ATS_FRIENDLY)
     * @param fontFamily   font tùy chọn (null = mặc định của mẫu)
     * @param accentColor  màu chủ đạo dạng #RRGGBB (null = lấy màu của CV hoặc mặc định)
     * @param avatarUrl    ảnh đại diện tùy chọn (null = dùng ảnh của CV / ảnh mẫu)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPreviewModelForCustomizer(Long selectedCvId, String templateCode, String fontFamily,
                                                            String accentColor, String avatarUrl, Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        List<Cv> myCvs = cvRepository
                .findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE");

        Cv selected = null;
        if (selectedCvId == null) {
            selected = myCvs.isEmpty() ? null : myCvs.get(0);
        } else if (selectedCvId > 0) {
            selected = myCvs.stream().filter(c -> c.getId().equals(selectedCvId)).findFirst().orElse(null);
        }

        String code = templateCode != null ? templateCode.trim().toUpperCase() : "";
        if (!CUSTOMIZER_TEMPLATE_CODES.contains(code)) {
            code = DEFAULT_TEMPLATE_CODE;
        }

        String color;
        if (accentColor != null && HEX_COLOR.matcher(accentColor).matches()) {
            color = accentColor;
        } else if (selected != null && selected.getAccentColor() != null && HEX_COLOR.matcher(selected.getAccentColor()).matches()) {
            color = selected.getAccentColor();
        } else {
            color = "#2563EB";
        }

        Map<String, Object> model = new HashMap<>();
        model.put("cvOptions", myCvs);
        model.put("previewCv", selected);
        model.put("isSample", selected == null);
        model.put("selectedCvId", selected != null ? selected.getId() : 0L);
        model.put("templateCode", code);
        model.put("accentColor", color);
        model.put("fontFamily", fontFamily);
        model.put("avatarUrl", avatarUrl != null && !avatarUrl.isBlank()
                ? avatarUrl
                : (selected != null ? selected.getAvatarUrl() : null));

        if (selected != null) {
            Long id = selected.getId();
            model.put("works", sectionService.getWorkExperiences(id, userId));
            model.put("educations", sectionService.getEducations(id, userId));
            model.put("skills", sectionService.getSkills(id, userId));
            model.put("projects", sectionService.getProjects(id, userId));
        } else {
            model.put("works", List.of());
            model.put("educations", List.of());
            model.put("skills", List.of());
            model.put("projects", List.of());
        }
        return model;
    }
}
