package OnlineRecruitment.OnlineRecruitment.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final String UPLOAD_DIR = "uploads";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

    public String saveAvatar(MultipartFile file) throws IOException {
        return saveFile(file, "avatars");
    }

    public String saveCompanyLogo(MultipartFile file) throws IOException {
        return saveFile(file, "logos");
    }

    public String saveFile(MultipartFile file, String subDirectory) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );

        String extension = "";
        int extIndex = originalFilename.lastIndexOf('.');
        if (extIndex >= 0) {
            extension = originalFilename.substring(extIndex).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ! Chỉ chấp nhận ảnh: .jpg, .jpeg, .png, .gif, .webp");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Dung lượng file vượt quá giới hạn cho phép (tối đa 10MB)!");
        }

        Path targetDirectory = Paths.get(UPLOAD_DIR, subDirectory);
        if (!Files.exists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        String uniqueFilename = UUID.randomUUID() + extension;
        Path targetPath = targetDirectory.resolve(uniqueFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Trả về URL dạng /uploads/avatars/uuid.ext
        return "/" + UPLOAD_DIR + "/" + subDirectory + "/" + uniqueFilename;
    }
}
