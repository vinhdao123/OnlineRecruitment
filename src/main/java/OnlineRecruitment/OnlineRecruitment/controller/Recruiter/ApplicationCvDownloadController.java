package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.service.CvFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class ApplicationCvDownloadController {

    private final JdbcTemplate jdbc;
    private final CvFileStorageService fileStorageService;

    @GetMapping("/recruiter/applications/{id}/cv-file")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @RequestParam(value = "inline", defaultValue = "false") boolean inline,
            Authentication auth) {
        String loginName = auth.getName();
        var rows = jdbc.queryForList("""
                SELECT a.cv_file_path_snapshot AS path
                FROM dbo.applications a
                JOIN dbo.jobs j ON j.id = a.job_id
                LEFT JOIN dbo.companies c ON c.id = j.company_id
                JOIN dbo.users recruiter ON (recruiter.id = c.recruiter_id OR recruiter.id = j.recruiter_id)
                WHERE a.id = ? AND (recruiter.email = ? OR recruiter.username = ?)
                """, id, loginName, loginName);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Object value = rows.get(0).get("path");
        if (!(value instanceof String stored)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String normalized = stored.trim().replace("\\", "/");
        if (!normalized.matches("(?i)(storage/cvs/[a-f0-9-]+|/?uploads/cv/[^/\\\\]+)\\.(pdf|doc|docx)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        File file = fileStorageService.resolveCvFile(normalized);
        if (file == null || !file.isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String ext = file.getName().substring(file.getName().lastIndexOf('.')).toLowerCase(Locale.ROOT);
        MediaType mediaType = ".pdf".equals(ext) ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition disposition = (inline && ".pdf".equals(ext))
                ? ContentDisposition.inline().filename("application-" + id + ext, StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment().filename("application-" + id + ext, StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.noStore())
                .body(new FileSystemResource(file));
    }

    @GetMapping("/recruiter/applications/{id}/cv-avatar")
    public ResponseEntity<Resource> getApplicationCvAvatar(@PathVariable Long id, Authentication auth) {
        String loginName = auth.getName();
        var rows = jdbc.queryForList("""
                SELECT cv.avatar_url AS avatarUrl
                FROM dbo.applications a
                JOIN dbo.jobs j ON j.id = a.job_id
                LEFT JOIN dbo.companies c ON c.id = j.company_id
                JOIN dbo.users recruiter ON (recruiter.id = c.recruiter_id OR recruiter.id = j.recruiter_id)
                LEFT JOIN dbo.cv cv ON cv.id = a.cv_id
                WHERE a.id = ? AND (recruiter.email = ? OR recruiter.username = ?)
                """, id, loginName, loginName);
        if (!rows.isEmpty()) {
            Object raw = rows.get(0).get("avatarUrl");
            if (raw instanceof String avatarUrl && !avatarUrl.isBlank()) {
                File file = fileStorageService.resolveAvatarFile(avatarUrl);
                if (file != null && file.exists()) {
                    try {
                        String mime = Files.probeContentType(file.toPath());
                        MediaType mediaType = (mime != null) ? MediaType.parseMediaType(mime) : MediaType.IMAGE_JPEG;
                        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(file));
                    } catch (Exception ignored) {}
                }
            }
        }
        Resource defaultAvatar = fileStorageService.getDefaultAvatar();
        if (defaultAvatar != null) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(defaultAvatar);
        }
        return ResponseEntity.notFound().build();
    }
}
