
package OnlineRecruitment.OnlineRecruitment.controller.company;

import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companyRepository;

    // Hiển thị danh sách công ty ACTIVE và tìm kiếm
    @GetMapping("/company-profile")
    public String companyProfile(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            Model model) {

        List<Company> companies =
                companyRepository.findByStatus(CompanyStatus.ACTIVE);

        // Tìm theo tên công ty
        if (keyword != null && !keyword.trim().isEmpty()) {
            String nameKeyword = keyword.trim().toLowerCase(Locale.ROOT);

            companies = companies.stream()
                    .filter(company ->
                            company.getCompanyName() != null
                                    && company.getCompanyName()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(nameKeyword))
                    .toList();
        }

        // Tìm theo địa điểm
        if (location != null && !location.trim().isEmpty()) {
            String addressKeyword =
                    location.trim().toLowerCase(Locale.ROOT);

            companies = companies.stream()
                    .filter(company ->
                            company.getAddress() != null
                                    && company.getAddress()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(addressKeyword))
                    .toList();
        }

        model.addAttribute("companies", companies);
        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);

        return "company/company-profile";
    }

    // Hiển thị chi tiết một công ty theo ID
    @GetMapping("/company-profile/{id}")
    public String companyDetail(
            @PathVariable Long id,
            Model model) {

        Company company = companyRepository.findApprovedById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy công ty đang hoạt động"
                        )
                );

        model.addAttribute("company", company);

        return "company/company-detail";
    }
}
