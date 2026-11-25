package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.CvGalleryTemplateVm;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.CvTemplateService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Thư viện mẫu CV và bộ tùy biến: /candidate/cvs/gallery, /candidate/cvs/templates, /candidate/cvs/customizer.
 */
@Controller
@RequestMapping("/candidate/cvs")
public class CvTemplateController extends AbstractCvController {

    private final CvTemplateService templateService;

    public CvTemplateController(UserRepository userRepository, CvTemplateService templateService) {
        super(userRepository);
        this.templateService = templateService;
    }

    @GetMapping("/gallery")
    public String gallery(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "category", required = false) String category,
            Model model) {
        model.addAllAttributes(templateService.buildGalleryPage(category, page, size));
        return "candidate/cv/gallery";
    }

    @GetMapping("/templates")
    public String listTemplates(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "category", required = false) String category,
            Model model) {
        model.addAllAttributes(templateService.buildGalleryPage(category, page, size));
        return "candidate/cv/templates";
    }

    @GetMapping("/customizer")
    public String customizer(
            @RequestParam(value = "templateCode", required = false) String templateCode,
            @RequestParam(value = "cvId", required = false) Long cvId,
            @RequestParam(value = "accentColor", required = false) String accentColor,
            Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Long userId = getUserId(auth);
            Map<String, Object> previewData = templateService.getPreviewModelForCustomizer(
                    cvId, templateCode, null, accentColor, null, userId);
            model.addAllAttributes(previewData);

            List<CvGalleryTemplateVm> galleryTemplates = templateService.buildGalleryTemplates();
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
}
