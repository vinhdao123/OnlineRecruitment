package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequiredArgsConstructor
public class ApplicationCvDownloadController {
    private final JdbcTemplate jdbc;
    @GetMapping("/recruiter/applications/{id}/cv-file")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication auth) {
        var rows = jdbc.queryForList("""
                SELECT a.cv_file_path_snapshot AS path
                FROM dbo.applications a
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.users recruiter ON recruiter.id = j.recruiter_id
                WHERE a.id = ? AND recruiter.email = ?
                """, id, auth.getName());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Object value = rows.get(0).get("path");
        if (!(value instanceof String stored) || !stored.matches("storage/cvs/[a-f0-9-]+\\.(pdf|doc|docx)"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Path base = Path.of("storage/cvs").toAbsolutePath().normalize();
        Path file = Path.of(stored).toAbsolutePath().normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "attachment; filename=\"application-" + id + stored.substring(stored.lastIndexOf('.')) + "\"")
                .cacheControl(CacheControl.noStore()).body(new FileSystemResource(file));
    }
}
