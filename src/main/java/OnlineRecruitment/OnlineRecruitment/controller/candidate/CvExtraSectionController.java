package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.ActivityRequest;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.AwardRequest;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.CertificateRequest;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.ReferenceRequest;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.CvExtraSectionService;
import OnlineRecruitment.OnlineRecruitment.service.CvService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Các tiểu mục bổ sung của CV: Chứng chỉ, Giải thưởng, Hoạt động ngoại khóa, Người tham chiếu.
 */
@Controller
@RequestMapping("/candidate/cvs")
public class CvExtraSectionController extends AbstractCvController {

    private final CvService cvService;
    private final CvExtraSectionService extraSectionService;

    public CvExtraSectionController(UserRepository userRepository,
                                    CvService cvService,
                                    CvExtraSectionService extraSectionService) {
        super(userRepository);
        this.cvService = cvService;
        this.extraSectionService = extraSectionService;
    }

    // ---------- CHỨNG CHỈ ----------

    @GetMapping("/{id}/certificates")
    public String listCertificate(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        return showSection(id, auth, model, ra,
                userId -> cvService.getCvDetail(id, userId),
                "certificates", userId -> extraSectionService.getCertificates(id, userId),
                "certRequest", new CertificateRequest(), "candidate/cv/certificate", null);
    }

    @PostMapping("/{id}/certificates/add")
    public String addCertificate(@PathVariable Long id, @Valid @ModelAttribute("certRequest") CertificateRequest req,
                                 BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            model.addAttribute("certificates", extraSectionService.getCertificates(id, userId));
            return "candidate/cv/certificate";
        }
        try {
            extraSectionService.addCertificate(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm chứng chỉ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/certificates";
    }

    @PostMapping("/{id}/certificates/{certId}/delete")
    public String deleteCertificate(@PathVariable Long id, @PathVariable("certId") Long certId, Authentication auth, RedirectAttributes ra) {
        return deleteSectionItem(id, ra,
                () -> extraSectionService.deleteCertificate(id, certId, getUserId(auth)),
                "Đã xóa chứng chỉ!", "/certificates");
    }

    // ---------- GIẢI THƯỞNG ----------

    @GetMapping("/{id}/awards")
    public String listAward(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        return showSection(id, auth, model, ra,
                userId -> cvService.getCvDetail(id, userId),
                "awards", userId -> extraSectionService.getAwards(id, userId),
                "awardRequest", new AwardRequest(), "candidate/cv/award", null);
    }

    @PostMapping("/{id}/awards/add")
    public String addAward(@PathVariable Long id, @Valid @ModelAttribute("awardRequest") AwardRequest req,
                           BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            model.addAttribute("awards", extraSectionService.getAwards(id, userId));
            return "candidate/cv/award";
        }
        try {
            extraSectionService.addAward(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm giải thưởng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/awards";
    }

    @PostMapping("/{id}/awards/{awardId}/delete")
    public String deleteAward(@PathVariable Long id, @PathVariable("awardId") Long awardId, Authentication auth, RedirectAttributes ra) {
        return deleteSectionItem(id, ra,
                () -> extraSectionService.deleteAward(id, awardId, getUserId(auth)),
                "Đã xóa giải thưởng!", "/awards");
    }

    // ---------- HOẠT ĐỘNG NGOẠI KHÓA ----------

    @GetMapping("/{id}/activities")
    public String listActivity(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        return showSection(id, auth, model, ra,
                userId -> cvService.getCvDetail(id, userId),
                "activities", userId -> extraSectionService.getActivities(id, userId),
                "activityRequest", new ActivityRequest(), "candidate/cv/activity", null);
    }

    @PostMapping("/{id}/activities/add")
    public String addActivity(@PathVariable Long id, @Valid @ModelAttribute("activityRequest") ActivityRequest req,
                              BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            model.addAttribute("activities", extraSectionService.getActivities(id, userId));
            return "candidate/cv/activity";
        }
        try {
            extraSectionService.addActivity(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm hoạt động thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/activities";
    }

    @PostMapping("/{id}/activities/{activityId}/delete")
    public String deleteActivity(@PathVariable Long id, @PathVariable("activityId") Long activityId, Authentication auth, RedirectAttributes ra) {
        return deleteSectionItem(id, ra,
                () -> extraSectionService.deleteActivity(id, activityId, getUserId(auth)),
                "Đã xóa hoạt động!", "/activities");
    }

    // ---------- NGƯỜI THAM CHIẾU ----------

    @GetMapping("/{id}/references")
    public String listReference(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        return showSection(id, auth, model, ra,
                userId -> cvService.getCvDetail(id, userId),
                "references", userId -> extraSectionService.getReferences(id, userId),
                "referenceRequest", new ReferenceRequest(), "candidate/cv/reference", null);
    }

    @PostMapping("/{id}/references/add")
    public String addReference(@PathVariable Long id, @Valid @ModelAttribute("referenceRequest") ReferenceRequest req,
                               BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            model.addAttribute("references", extraSectionService.getReferences(id, userId));
            return "candidate/cv/reference";
        }
        try {
            extraSectionService.addReference(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm người tham chiếu thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/references";
    }

    @PostMapping("/{id}/references/{refId}/delete")
    public String deleteReference(@PathVariable Long id, @PathVariable("refId") Long refId, Authentication auth, RedirectAttributes ra) {
        return deleteSectionItem(id, ra,
                () -> extraSectionService.deleteReference(id, refId, getUserId(auth)),
                "Đã xóa người tham chiếu!", "/references");
    }
}
