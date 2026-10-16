package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JobService {

    private static final double VND_TO_USD_RATE = 25000.0;
    private static final Pattern NO_EXP_PATTERN = Pattern.compile("\\bno\\b", Pattern.CASE_INSENSITIVE);

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;

    public JobService(
            JobRepository jobRepository,
            JobCategoryRepository jobCategoryRepository) {

        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
    }

    // =====================================================
    // LẤY JOB MỚI NHẤT (CHỈ ACTIVE)
    // =====================================================

    public List<Job> getNewestJobs() {
        return jobRepository.findActiveJobs();
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

        // Chỉ lấy job ACTIVE, công ty ACTIVE và chưa hết hạn
        List<Job> jobs = jobRepository.findActiveJobs();

        // 1. TÌM THEO TÊN CÔNG VIỆC
        if (keyword != null && !keyword.trim().isEmpty()) {
            String keywordLower = keyword.trim().toLowerCase();
            jobs = jobs.stream()
                    .filter(job -> job.getTitle() != null && job.getTitle().toLowerCase().contains(keywordLower))
                    .collect(Collectors.toList());
        }

        // 2. TÌM THEO Ô ĐỊA ĐIỂM TRÊN THANH SEARCH
        if (locationKeyword != null && !locationKeyword.trim().isEmpty()) {
            String locationLower = locationKeyword.trim().toLowerCase();
            jobs = jobs.stream()
                    .filter(job -> job.getLocation() != null && job.getLocation().toLowerCase().contains(locationLower))
                    .collect(Collectors.toList());
        }

        // 3. CHECKBOX ĐỊA ĐIỂM
        if (locations != null && !locations.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job -> {
                        if (job.getLocation() == null) return false;
                        return locations.stream().anyMatch(loc -> job.getLocation().equalsIgnoreCase(loc));
                    })
                    .collect(Collectors.toList());
        }

        // 4. KINH NGHIỆM
        if (experiences != null && !experiences.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job -> matchesExperience(job.getExperience(), experiences))
                    .collect(Collectors.toList());
        }

        // 5. MỨC LƯƠNG
        if (salaryRanges != null && !salaryRanges.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job -> matchesSalary(
                            job.getSalaryMin(),
                            job.getSalaryMax(),
                            job.getSalaryCurrency(),
                            salaryRanges))
                    .collect(Collectors.toList());
        }

        // 6. CẤP BẬC
        if (levels != null && !levels.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job -> {
                        if (job.getLevel() == null) return false;
                        return levels.stream().anyMatch(level -> job.getLevel().equalsIgnoreCase(level));
                    })
                    .collect(Collectors.toList());
        }

        // 7. NGÀNH NGHỀ
        if (categoryIds != null && !categoryIds.isEmpty()) {
            jobs = jobs.stream()
                    .filter(job -> {
                        if (job.getCategory() == null) return false;
                        return categoryIds.contains(job.getCategory().getId());
                    })
                    .collect(Collectors.toList());
        }

        // SẮP XẾP MỚI NHẤT
        jobs.sort(
                Comparator.comparing(
                        Job::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        return jobs;
    }

    // =====================================================
    // KIỂM TRA KINH NGHIỆM
    // =====================================================

    private boolean matchesExperience(String experience, List<String> selectedRanges) {
        if (experience == null || experience.trim().isEmpty()) {
            return selectedRanges.contains("none");
        }

        String value = experience.trim().toLowerCase();

        // Không cần kinh nghiệm: dùng \bno\b để tránh khớp nhầm "knowledge", "technology"
        if (value.contains("không")
                || NO_EXP_PATTERN.matcher(value).find()
                || value.contains("not required")
                || value.contains("no experience")) {
            return selectedRanges.contains("none");
        }

        List<Integer> numbers = extractNumbers(value);
        if (numbers.isEmpty()) return false;

        double jobMin = numbers.get(0);
        double jobMax = numbers.size() >= 2 ? numbers.get(1) : jobMin;

        if (jobMin > 5) {
            jobMax = Double.MAX_VALUE;
        }

        for (String range : selectedRanges) {
            double filterMin;
            double filterMax;

            switch (range) {
                case "0-1":
                    filterMin = 0; filterMax = 1; break;
                case "1-3":
                    filterMin = 1; filterMax = 3; break;
                case "3-5":
                    filterMin = 3; filterMax = 5; break;
                case "5+":
                    filterMin = 5; filterMax = Double.MAX_VALUE; break;
                default:
                    continue;
            }

            if (jobMin <= filterMax && jobMax >= filterMin) {
                return true;
            }
        }

        return false;
    }

    // =====================================================
    // KIỂM TRA MỨC LƯƠNG — Quy đổi VND sang USD theo tỉ giá 25.000
    // =====================================================

    private boolean matchesSalary(
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String currency,
            List<String> selectedRanges) {

        if (salaryMin == null && salaryMax == null) {
            return false;
        }

        double jobMin = salaryMin != null ? salaryMin.doubleValue() : salaryMax.doubleValue();
        double jobMax = salaryMax != null ? salaryMax.doubleValue() : salaryMin.doubleValue();

        // Nếu lương ghi theo VND (hoặc giá trị > 100.000), quy đổi về USD
        if ("VND".equalsIgnoreCase(currency) || jobMin > 100000 || jobMax > 100000) {
            jobMin = jobMin / VND_TO_USD_RATE;
            jobMax = jobMax / VND_TO_USD_RATE;
        }

        for (String range : selectedRanges) {
            double filterMin;
            double filterMax;

            switch (range) {
                case "under-1000":
                    filterMin = 0; filterMax = 1000; break;
                case "1000-2000":
                    filterMin = 1000; filterMax = 2000; break;
                case "2000-3000":
                    filterMin = 2000; filterMax = 3000; break;
                case "3000+":
                    filterMin = 3000; filterMax = Double.MAX_VALUE; break;
                default:
                    continue;
            }

            if (jobMin <= filterMax && jobMax >= filterMin) {
                return true;
            }
        }

        return false;
    }

    private List<Integer> extractNumbers(String text) {
        List<Integer> numbers = new ArrayList<>();
        String[] parts = text.split("[^0-9]+");

        for (String part : parts) {
            if (!part.isEmpty()) {
                try {
                    numbers.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return numbers;
    }
}