package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.CreateCvRequest;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.CvSummaryVm;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.UpdateCvRequest;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.CvFileStorageService;
import OnlineRecruitment.OnlineRecruitment.service.CvService;
import OnlineRecruitment.OnlineRecruitment.service.CvTemplateService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vòng đời CV: danh sách, tạo mới, chi tiết / chỉnh sửa, đặt mặc định, nhân bản, xoá, đổi tên, xem trước.
 *
 * Các route khác của /candidate/cvs nằm ở:
 * - {@link CvTemplateController}: gallery, templates, customizer
 * - {@link CvFileController}: avatar, xem / tải file, upload
 * - {@link CvSectionController}: kinh nghiệm, học vấn, kỹ năng, dự án
 * - {@link CvExtraSectionController}: chứng chỉ, giải thưởng, hoạt động, tham chiếu
 */
@Controller
@RequestMapping("/candidate/cvs")
@Slf4j
public class CvController extends AbstractCvController {

    private final CvService cvService;
    private final CvTemplateService templateService;
    private final CvFileStorageService fileStorageService;

    public CvController(UserRepository userRepository,
                        CvService cvService,
                        CvTemplateService templateService,
                        CvFileStorageService fileStorageService) {
        super(userRepository);
        this.cvService = cvService;
        this.templateService = templateService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            Authentication auth, Model model) {
        try {
            User user = getAuthenticatedUser(auth);

            Page<CvSummaryVm> cvPage = cvService.listMyCvsPaged(user.getId(), keyword, page, size);
            List<CvSummaryVm> cvs = cvPage.getContent();
            Map<Long, Map<String, Object>> cvDataMap = cvService.getCvListWithDetailsForCvs(cvs, user.getId());
            String defaultCvTitle = cvService.getDefaultCvTitle(user.getId());

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
            log.error("Lỗi khi tải danh sách CV cho user: {}", auth != null ? auth.getName() : "anonymous", e);
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
        model.addAttribute("templates", templateService.getAvailableTemplates());
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
            model.addAttribute("templates", templateService.getAvailableTemplates());
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
            log.error("Lỗi khi tạo CV cho user: {}", auth != null ? auth.getName() : "anonymous", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("templates", templateService.getAvailableTemplates());
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
            model.addAttribute("templateName", templateService.resolveTemplateInfo(cv).name());
            model.addAttribute("templates", templateService.getAvailableTemplates());

            if (isEdit) {
                UpdateCvRequest req = new UpdateCvRequest(cv.getTemplateVersionId(), cv.getTitle(), cv.getProfessionalTitle(), cv.getSummary(), cv.getAccentColor(), cv.getAvatarUrl());
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
            model.addAttribute("templates", templateService.getAvailableTemplates());
            return "candidate/cv/detail";
        }
        try {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                req.setAvatarUrl(fileStorageService.storeAvatar(avatarFile));
            }

            cvService.updateCv(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Cập nhật thông tin CV thành công!");
            return "redirect:/candidate/cvs/" + id;
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật CV {} cho user: {}", id, auth != null ? auth.getName() : "anonymous", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("cvId", id);
            model.addAttribute("isEditMode", true);
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("templates", templateService.getAvailableTemplates());
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

    @GetMapping("/{id}/preview")
    public String previewCv(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            Cv cv = cvService.getCvDetail(id, userId);
            if (Boolean.TRUE.equals(cv.getIsUploaded())) {
                return "redirect:/candidate/cvs/" + id + "/file";
            }
            model.addAllAttributes(cvService.getFullCvForPreview(id, userId));
            return "candidate/cv/preview";
        } catch (Exception e) {
            log.error("Lỗi khi tải trang xem trước CV {}: ", id, e);
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/candidate/cvs";
        }
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
