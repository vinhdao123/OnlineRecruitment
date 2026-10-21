package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

/** Read-only overview of image URL references from the existing SQL Server database. */
@Controller
@RequestMapping("/admin/images")
@RequiredArgsConstructor
public class AdminImageController {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;

    public record ImageItem(String kind, String owner, String url) {}

    @GetMapping
    public String list(Model model) {
        List<ImageItem> images = new ArrayList<>();
        // Limit each query so the image gallery does not load the entire database into memory.
        for (User u : userRepository.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id")))) {
            if (hasUrl(u.getAvatarUrl())) images.add(new ImageItem("Avatar", u.getFullName(), u.getAvatarUrl().trim()));
        }
        for (Company c : companyRepository.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id")))) {
            if (hasUrl(c.getLogoUrl())) images.add(new ImageItem("Company logo", c.getCompanyName(), c.getLogoUrl().trim()));
        }
        for (Job j : jobRepository.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id")))) {
            if (hasUrl(j.getLogoUrl())) images.add(new ImageItem("Job logo", j.getTitle(), j.getLogoUrl().trim()));
        }
        model.addAttribute("images", images);
        model.addAttribute("activeMenu", "images");
        return "admin/images/list";
    }

    private boolean hasUrl(String url) {
        return url != null && !url.isBlank();
    }
}
