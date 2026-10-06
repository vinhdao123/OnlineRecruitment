package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class JobController {

    private final JobService jobService;


    public JobController(JobService jobService) {
        this.jobService = jobService;
    }


    @GetMapping("/jobs")
    public String jobs(

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String location,

            @RequestParam(required = false)
            List<String> locations,

            @RequestParam(required = false)
            List<String> experiences,

            @RequestParam(required = false)
            List<String> salaryRanges,

            @RequestParam(required = false)
            List<String> levels,

            @RequestParam(required = false)
            List<Long> categoryIds,

            @RequestParam(defaultValue = "newest")
            String sort,

            Model model) {


        // ==========================================
        // LỌC
        // ==========================================

        List<Job> jobs =
                jobService.filterJobs(

                        keyword,

                        location,

                        locations,

                        experiences,

                        salaryRanges,

                        levels,

                        categoryIds
                );


        // ==========================================
        // SẮP XẾP
        // ==========================================

        if ("salary-high".equals(sort)) {

            jobs.sort((job1, job2) -> {

                Double salary1 =
                        job1.getSalaryMax();

                Double salary2 =
                        job2.getSalaryMax();


                if (salary1 == null) {
                    return 1;
                }

                if (salary2 == null) {
                    return -1;
                }


                return salary2.compareTo(salary1);
            });


        } else if ("salary-low".equals(sort)) {

            jobs.sort((job1, job2) -> {

                Double salary1 =
                        job1.getSalaryMax();

                Double salary2 =
                        job2.getSalaryMax();


                if (salary1 == null) {
                    return 1;
                }

                if (salary2 == null) {
                    return -1;
                }


                return salary1.compareTo(salary2);
            });
        }


        // ==========================================
        // DỮ LIỆU CHO HTML
        // ==========================================

        model.addAttribute(
                "jobs",
                jobs
        );


        model.addAttribute(
                "locationsList",
                jobService.getAllLocations()
        );


        model.addAttribute(
                "categories",
                jobService.getAllCategories()
        );


        model.addAttribute(
                "keyword",
                keyword
        );


        model.addAttribute(
                "location",
                location
        );


        model.addAttribute(
                "locations",
                locations
        );


        model.addAttribute(
                "experiences",
                experiences
        );


        model.addAttribute(
                "salaryRanges",
                salaryRanges
        );


        model.addAttribute(
                "levels",
                levels
        );


        model.addAttribute(
                "categoryIds",
                categoryIds
        );


        model.addAttribute(
                "sort",
                sort
        );


        return "job/job";
    }
}