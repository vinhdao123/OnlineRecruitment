package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.CvFileStorageService;
import OnlineRecruitment.OnlineRecruitment.service.CvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * File của CV: ảnh đại diện, xem / tải file CV tải lên, upload CV từ máy tính.
 */
@Controller
@RequestMapping("/candidate/cvs")
@Slf4j
public class CvFileController extends AbstractCvController {

    private final CvService cvService;
    private final CvFileStorageService fileStorageService;

    public CvFileController(UserRepository userRepository,
                            CvService cvService,
                            CvFileStorageService fileStorageService) {
        super(userRepository);
        this.cvService = cvService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> getCvAvatar(@PathVariable Long id, Authentication auth) {
        try {
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                Long userId = getUserId(auth);
                Cv cv = cvService.getCvDetail(id, userId);
                File file = fileStorageService.resolveAvatarFile(cv.getAvatarUrl());
                if (file != null && file.exists()) {
                    String mime = Files.probeContentType(file.toPath());
                    MediaType mediaType = (mime != null) ? MediaType.parseMediaType(mime) : MediaType.IMAGE_JPEG;
                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .body(new FileSystemResource(file));
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải avatar CV {}: {}", id, e.getMessage());
        }

        // Luôn trả về ảnh avatar mặc định, không bao giờ để lỗi vỡ ảnh
        Resource defaultAvatar = fileStorageService.getDefaultAvatar();
        if (defaultAvatar != null) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(defaultAvatar);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getCvFile(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            Cv cv = cvService.getCvDetail(id, userId);
            if (cv.getFileUrl() == null || cv.getFileUrl().trim().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = fileStorageService.resolveCvFile(cv.getFileUrl());
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            String mime = Files.probeContentType(file.toPath());
            MediaType mediaType;
            if (mime != null) {
                mediaType = MediaType.parseMediaType(mime);
            } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            String downloadName = cv.getTitle() != null && !cv.getTitle().isEmpty() ? cv.getTitle() : file.getName();
            if (!downloadName.toLowerCase().endsWith(".pdf") && file.getName().toLowerCase().endsWith(".pdf")) {
                downloadName += ".pdf";
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadName + "\"")
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            log.error("Lỗi khi xem file CV {}: ", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadCvFile(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            Cv cv = cvService.getCvDetail(id, userId);
            if (cv.getFileUrl() == null || cv.getFileUrl().trim().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = fileStorageService.resolveCvFile(cv.getFileUrl());
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            String originalName = file.getName();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(originalName, StandardCharsets.UTF_8).build().toString())
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            log.error("Lỗi khi tải file CV {}: ", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/upload")
    public String uploadCv(@RequestParam("cvFile") MultipartFile file,
                           @RequestParam(value = "title", required = false) String customTitle,
                           Authentication auth, RedirectAttributes ra) {
        try {
            String fileUrl = fileStorageService.storeCvFile(file);

            // Xác định tên/tiêu đề CV an toàn (không giữ đường dẫn thư mục từ client)
            String originalName = file.getOriginalFilename();
            String safeBaseName = (originalName != null && !originalName.isBlank())
                    ? Paths.get(originalName.replace("\\", "/")).getFileName().toString()
                    : "CV Upload";
            String cvTitle = (customTitle != null && !customTitle.trim().isEmpty()) ? customTitle.trim() : safeBaseName;

            cvService.uploadCvFile(fileUrl, cvTitle, getUserId(auth));

            ra.addFlashAttribute("successMessage", "Tải CV lên thành công với tên: \"" + cvTitle + "\"!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi tải file: " + e.getMessage());
        }
        return "redirect:/candidate/cvs";
    }
}
