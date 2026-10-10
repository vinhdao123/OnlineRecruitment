package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.CV;
import OnlineRecruitment.OnlineRecruitment.entity.JobApplication;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Job2;
import OnlineRecruitment.OnlineRecruitment.repository.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.CVRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.JobApplicationRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.UserRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateApplicationService {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final CVRepository cvRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.cv-dir:uploads/cvs}")
    private String uploadDirectory;

    @Transactional
    public void apply(
            String email,
            Long jobId,
            String fullName,
            String formEmail,
            String phone,
            String position,
            String address,
            String summary,
            String skills,
            String template,
            String[] experienceTitles,
            String[] experiencePeriods,
            String[] experienceDescriptions,
            String[] educationSchools,
            String[] educationPeriods,
            String[] educationDescriptions,
            MultipartFile cvFile
    ) throws IOException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy tài khoản."));

        Candidate candidate = candidateRepository.findByUser(user)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Tài khoản chưa có hồ sơ ứng viên."));

        Job2 job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tin tuyển dụng."));

        if (jobApplicationRepository.existsByCandidate_IdAndJob_Id(
                candidate.getId(), job.getId())) {
            throw new IllegalArgumentException(
                    "Bạn đã ứng tuyển công việc này rồi.");
        }

        String cvFilePath = "MANUAL";
        String cvContent = buildCvContent(
                fullName,
                formEmail,
                phone,
                position,
                address,
                summary,
                skills,
                experienceTitles,
                experiencePeriods,
                experienceDescriptions,
                educationSchools,
                educationPeriods,
                educationDescriptions
        );

        // Nếu người dùng tải file CV lên thì lưu file.
        if (cvFile != null && !cvFile.isEmpty()) {
            String originalName = StringUtils.cleanPath(
                    cvFile.getOriginalFilename() == null
                            ? "cv"
                            : cvFile.getOriginalFilename()
            );

            String extension = StringUtils.getFilenameExtension(originalName);

            if (extension == null
                    || !(extension.equalsIgnoreCase("pdf")
                    || extension.equalsIgnoreCase("doc")
                    || extension.equalsIgnoreCase("docx"))) {
                throw new IllegalArgumentException(
                        "CV chỉ chấp nhận file PDF, DOC hoặc DOCX.");
            }

            if (cvFile.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException(
                        "Dung lượng CV không được vượt quá 10 MB.");
            }

            Path directory = Paths.get(uploadDirectory)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(directory);

            String storedName = UUID.randomUUID() + "." + extension;
            Path destination = directory.resolve(storedName).normalize();

            if (!destination.startsWith(directory)) {
                throw new IllegalArgumentException("Tên file không hợp lệ.");
            }

            Files.copy(cvFile.getInputStream(), destination);
            cvFilePath = destination.toString();
        }

        // Lưu CV vào database.
        CV cv = new CV();
        cv.setCandidate(candidate);
        cv.setTitle(StringUtils.hasText(position)
                ? position.trim()
                : "CV ứng tuyển");
        cv.setContent(cvContent);
        cv.setTemplate(StringUtils.hasText(template)
                ? template
                : "default");
        cv.setIsDefault(false);
        cv.setStatus("ACTIVE");

        CV savedCv = cvRepository.save(cv);

        // Tạo đơn ứng tuyển.
        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setCv(savedCv);
        application.setCvFilePath(cvFilePath);
        application.setStatus("SUBMITTED");

        jobApplicationRepository.save(application);
    }

    private String buildCvContent(
            String fullName,
            String email,
            String phone,
            String position,
            String address,
            String summary,
            String skills,
            String[] experienceTitles,
            String[] experiencePeriods,
            String[] experienceDescriptions,
            String[] educationSchools,
            String[] educationPeriods,
            String[] educationDescriptions
    ) throws JacksonException {

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("fullName", fullName);
        content.put("email", email);
        content.put("phone", phone);
        content.put("position", position);
        content.put("address", address);
        content.put("summary", summary);
        content.put("skills", skills);

        content.put("experienceTitles", experienceTitles);
        content.put("experiencePeriods", experiencePeriods);
        content.put("experienceDescriptions", experienceDescriptions);

        content.put("educationSchools", educationSchools);
        content.put("educationPeriods", educationPeriods);
        content.put("educationDescriptions", educationDescriptions);

        return objectMapper.writeValueAsString(content);
    }
}
