package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;

    public JobService(
            JobRepository jobRepository,
            JobCategoryRepository jobCategoryRepository) {

        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
    }


    // =====================================================
    // LẤY JOB MỚI NHẤT
    // =====================================================

    public List<Job> getNewestJobs() {

        return jobRepository.findAllByOrderByCreatedAtDesc();
    }


    // =====================================================
    // LƯƠNG CAO NHẤT
    // =====================================================

    public List<Job> getJobsSalaryHigh() {

        return jobRepository.findAllByOrderBySalaryMaxDesc();
    }


    // =====================================================
    // LƯƠNG THẤP NHẤT
    // =====================================================

    public List<Job> getJobsSalaryLow() {

        return jobRepository.findAllByOrderBySalaryMaxAsc();
    }


    // =====================================================
    // TÌM THEO TÊN
    // =====================================================

    public List<Job> searchByKeyword(String keyword) {

        return jobRepository
                .findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                        keyword
                );
    }


    // =====================================================
    // TÌM THEO ĐỊA ĐIỂM
    // =====================================================

    public List<Job> searchByLocation(String location) {

        return jobRepository
                .findByLocationContainingIgnoreCaseOrderByCreatedAtDesc(
                        location
                );
    }


    // =====================================================
    // TÌM TÊN + ĐỊA ĐIỂM
    // =====================================================

    public List<Job> searchByKeywordAndLocation(
            String keyword,
            String location) {

        return jobRepository
                .findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseOrderByCreatedAtDesc(
                        keyword,
                        location
                );
    }


    // =====================================================
    // LẤY TẤT CẢ ĐỊA ĐIỂM
    // =====================================================

    public List<String> getAllLocations() {

        return jobRepository
                .findDistinctByLocationIsNotNullOrderByLocationAsc()
                .stream()
                .map(Job::getLocation)
                .filter(Objects::nonNull)
                .filter(location -> !location.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }


    // =====================================================
    // LẤY TẤT CẢ NGÀNH NGHỀ
    // =====================================================

    public List<JobCategory> getAllCategories() {

        return jobCategoryRepository.findAll();
    }


    // =====================================================
    // BỘ LỌC
    // =====================================================

    public List<Job> filterJobs(

            String keyword,

            String locationKeyword,

            List<String> locations,

            List<String> experiences,

            List<String> salaryRanges,

            List<String> levels,

            List<Long> categoryIds) {


        // Lấy toàn bộ job từ DB
        List<Job> jobs = jobRepository.findAll();


        // =================================================
        // 1. TÌM THEO TÊN CÔNG VIỆC
        // =================================================

        if (keyword != null
                && !keyword.trim().isEmpty()) {

            String keywordLower =
                    keyword.trim().toLowerCase();

            jobs = jobs.stream()
                    .filter(job ->
                            job.getTitle() != null
                                    && job.getTitle()
                                    .toLowerCase()
                                    .contains(keywordLower)
                    )
                    .collect(Collectors.toList());
        }


        // =================================================
        // 2. TÌM THEO Ô ĐỊA ĐIỂM TRÊN THANH SEARCH
        // =================================================

        if (locationKeyword != null
                && !locationKeyword.trim().isEmpty()) {

            String locationLower =
                    locationKeyword.trim().toLowerCase();

            jobs = jobs.stream()
                    .filter(job ->
                            job.getLocation() != null
                                    && job.getLocation()
                                    .toLowerCase()
                                    .contains(locationLower)
                    )
                    .collect(Collectors.toList());
        }


        // =================================================
        // 3. CHECKBOX ĐỊA ĐIỂM
        // =================================================

        if (locations != null
                && !locations.isEmpty()) {

            jobs = jobs.stream()
                    .filter(job -> {

                        if (job.getLocation() == null) {
                            return false;
                        }

                        return locations.stream()
                                .anyMatch(location ->
                                        job.getLocation()
                                                .equalsIgnoreCase(
                                                        location
                                                )
                                );
                    })
                    .collect(Collectors.toList());
        }


        // =================================================
        // 4. KINH NGHIỆM
        // =================================================

        if (experiences != null
                && !experiences.isEmpty()) {

            jobs = jobs.stream()
                    .filter(job ->
                            matchesExperience(
                                    job.getExperience(),
                                    experiences
                            )
                    )
                    .collect(Collectors.toList());
        }


        // =================================================
        // 5. MỨC LƯƠNG
        // =================================================

        if (salaryRanges != null
                && !salaryRanges.isEmpty()) {

            jobs = jobs.stream()
                    .filter(job ->
                            matchesSalary(
                                    job.getSalaryMin(),
                                    job.getSalaryMax(),
                                    salaryRanges
                            )
                    )
                    .collect(Collectors.toList());
        }


        // =================================================
        // 6. CẤP BẬC
        // =================================================

        if (levels != null
                && !levels.isEmpty()) {

            jobs = jobs.stream()
                    .filter(job -> {

                        if (job.getLevel() == null) {
                            return false;
                        }

                        return levels.stream()
                                .anyMatch(level ->
                                        job.getLevel()
                                                .equalsIgnoreCase(level)
                                );
                    })
                    .collect(Collectors.toList());
        }


        // =================================================
        // 7. NGÀNH NGHỀ
        // =================================================

        if (categoryIds != null
                && !categoryIds.isEmpty()) {

            jobs = jobs.stream()
                    .filter(job ->
                            job.getCategoryId() != null
                                    && categoryIds.contains(
                                    job.getCategoryId()
                            )
                    )
                    .collect(Collectors.toList());
        }


        // =================================================
        // SẮP XẾP MỚI NHẤT
        // =================================================

        jobs.sort(
                Comparator.comparing(
                        Job::getCreatedAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );


        return jobs;
    }


    // =====================================================
    // KIỂM TRA KINH NGHIỆM
    // =====================================================

    private boolean matchesExperience(
            String experience,
            List<String> selectedRanges) {

        if (experience == null
                || experience.trim().isEmpty()) {

            return selectedRanges.contains("none");
        }


        String value =
                experience.trim().toLowerCase();


        // Không cần kinh nghiệm
        if (value.contains("không")
                || value.contains("no")
                || value.contains("not required")
                || value.contains("no experience")) {

            return selectedRanges.contains("none");
        }


        List<Integer> numbers =
                extractNumbers(value);


        if (numbers.isEmpty()) {
            return false;
        }


        double jobMin = numbers.get(0);

        double jobMax;


        if (numbers.size() >= 2) {

            jobMax = numbers.get(1);

        } else {

            jobMax = jobMin;
        }


        // Ví dụ:
        // 5 years → từ 5 trở lên
        // 6 years → trên 5
        if (jobMin > 5) {

            jobMax = Double.MAX_VALUE;
        }


        // Kiểm tra từng khoảng người dùng chọn
        for (String range : selectedRanges) {

            double filterMin;
            double filterMax;


            switch (range) {

                case "0-1":

                    filterMin = 0;
                    filterMax = 1;
                    break;


                case "1-3":

                    filterMin = 1;
                    filterMax = 3;
                    break;


                case "3-5":

                    filterMin = 3;
                    filterMax = 5;
                    break;


                case "5+":

                    filterMin = 5;
                    filterMax = Double.MAX_VALUE;
                    break;


                default:

                    continue;
            }


            // KHOẢNG GIAO NHAU
            if (jobMin <= filterMax
                    && jobMax >= filterMin) {

                return true;
            }
        }


        return false;
    }


    // =====================================================
    // KIỂM TRA MỨC LƯƠNG
    // =====================================================

    private boolean matchesSalary(

            Double salaryMin,

            Double salaryMax,

            List<String> selectedRanges) {


        if (salaryMin == null
                && salaryMax == null) {

            return false;
        }


        double jobMin =
                salaryMin != null
                        ? salaryMin
                        : salaryMax;


        double jobMax =
                salaryMax != null
                        ? salaryMax
                        : salaryMin;


        for (String range : selectedRanges) {

            double filterMin;
            double filterMax;


            switch (range) {

                case "under-1000":

                    filterMin = 0;
                    filterMax = 1000;
                    break;


                case "1000-2000":

                    filterMin = 1000;
                    filterMax = 2000;
                    break;


                case "2000-3000":

                    filterMin = 2000;
                    filterMax = 3000;
                    break;


                case "3000+":

                    filterMin = 3000;
                    filterMax = Double.MAX_VALUE;
                    break;


                default:

                    continue;
            }


            // KHOẢNG GIAO NHAU
            if (jobMin <= filterMax
                    && jobMax >= filterMin) {

                return true;
            }
        }


        return false;
    }


    // =====================================================
    // LẤY CÁC SỐ TRONG CHUỖI EXPERIENCE
    // =====================================================

    private List<Integer> extractNumbers(String text) {

        List<Integer> numbers =
                new ArrayList<>();

        String[] parts =
                text.split("[^0-9]+");


        for (String part : parts) {

            if (!part.isEmpty()) {

                try {

                    numbers.add(
                            Integer.parseInt(part)
                    );

                } catch (NumberFormatException ignored) {

                }
            }
        }


        return numbers;
    }
}