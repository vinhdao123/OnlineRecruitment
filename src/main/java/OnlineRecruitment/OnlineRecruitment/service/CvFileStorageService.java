package OnlineRecruitment.OnlineRecruitment.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Lưu trữ và kiểm tra bảo mật file vật lý của module CV: ảnh đại diện CV, file CV tải lên (PDF/Word).
 */
@Service
public class CvFileStorageService {

    public static final long MAX_CV_FILE_BYTES = 5L * 1024 * 1024;      // 5 MB
    public static final long MAX_AVATAR_FILE_BYTES = 2L * 1024 * 1024;  // 2 MB

    private static final Set<String> ALLOWED_CV_EXTS = Set.of(".pdf", ".doc", ".docx");
    private static final Set<String> ALLOWED_AVATAR_EXTS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final String DEFAULT_AVATAR_PATH = "static/images/cv-sample-avatar.jpg";

    private static Path baseDirPath() {
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    // ==========================================
    // KIỂM TRA ĐỊNH DẠNG & NỘI DUNG FILE
    // ==========================================

    /** Trích xuất phần mở rộng (chữ thường), ví dụ ".pdf"; rỗng nếu không hợp lệ. */
    public String extractLowerExtension(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "";
        }
        String fileName = Paths.get(originalName.replace("\\", "/")).getFileName().toString().trim();
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx <= 0 || dotIdx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIdx).toLowerCase(Locale.ROOT);
    }

    /** Chấp nhận file PDF hoặc Word (không phân biệt chữ hoa/thường). */
    public boolean isSupportedCvFile(String originalName) {
        return ALLOWED_CV_EXTS.contains(extractLowerExtension(originalName));
    }

    /** Kiểm tra dung lượng, đuôi file (không phân biệt hoa thường) và magic bytes thực của CV. */
    public void validateCvFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file!");
        }
        if (file.getSize() > MAX_CV_FILE_BYTES) {
            throw new IllegalArgumentException("File CV không được vượt quá 5MB!");
        }
        String ext = extractLowerExtension(file.getOriginalFilename());
        if (!ALLOWED_CV_EXTS.contains(ext)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file PDF, DOC hoặc DOCX!");
        }
        byte[] header = readHeaderBytes(file, 16);
        if (!matchesCvMagicBytes(ext, header)) {
            throw new IllegalArgumentException("Nội dung file không đúng định dạng PDF/Word hợp lệ!");
        }
    }

    /** Kiểm tra dung lượng, đuôi file ảnh và magic bytes thực của ảnh đại diện. */
    public void validateAvatarFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file ảnh!");
        }
        if (file.getSize() > MAX_AVATAR_FILE_BYTES) {
            throw new IllegalArgumentException("Ảnh đại diện không được vượt quá 2MB!");
        }
        String ext = extractLowerExtension(file.getOriginalFilename());
        if (!ALLOWED_AVATAR_EXTS.contains(ext)) {
            throw new IllegalArgumentException("Ảnh đại diện chỉ chấp nhận JPG, PNG, WEBP hoặc GIF!");
        }
        byte[] header = readHeaderBytes(file, 16);
        if (!matchesImageMagicBytes(ext, header)) {
            throw new IllegalArgumentException("Nội dung file ảnh không hợp lệ!");
        }
    }

    private byte[] readHeaderBytes(MultipartFile file, int maxLen) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] buf = new byte[maxLen];
            int n = is.read(buf);
            if (n <= 0) {
                return new byte[0];
            }
            byte[] actual = new byte[n];
            System.arraycopy(buf, 0, actual, 0, n);
            return actual;
        }
    }

    private boolean matchesCvMagicBytes(String ext, byte[] b) {
        return switch (ext) {
            case ".pdf" -> b.length >= 5
                    && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-';
            case ".docx" -> b.length >= 4
                    && b[0] == 0x50 && b[1] == 0x4B && (b[2] == 0x03 || b[2] == 0x05) && (b[3] == 0x04 || b[3] == 0x06);
            case ".doc" -> b.length >= 8
                    && (b[0] & 0xFF) == 0xD0 && (b[1] & 0xFF) == 0xCF
                    && (b[2] & 0xFF) == 0x11 && (b[3] & 0xFF) == 0xE0
                    && (b[4] & 0xFF) == 0xA1 && (b[5] & 0xFF) == 0xB1
                    && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0xE1;
            default -> false;
        };
    }

    private boolean matchesImageMagicBytes(String ext, byte[] b) {
        return switch (ext) {
            case ".jpg", ".jpeg" -> b.length >= 3
                    && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case ".png" -> b.length >= 8
                    && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                    && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
            case ".gif" -> b.length >= 6
                    && new String(b, 0, 6, StandardCharsets.US_ASCII).startsWith("GIF8");
            case ".webp" -> b.length >= 12
                    && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }

    // ==========================================
    // LƯU FILE (TÊN FILE UUID AN TOÀN)
    // ==========================================

    /**
     * Lưu file CV tải lên vào thư mục riêng tư storage/cvs/ với tên UUID chuẩn hoá
     * (khớp pattern storage/cvs/<uuid>.<ext> để recruiter tải an toàn, không lộ ra static /uploads).
     */
    public String storeCvFile(MultipartFile file) throws IOException {
        validateCvFile(file);
        String ext = extractLowerExtension(file.getOriginalFilename());
        String safeFileName = UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + ext;

        Path dir = baseDirPath().resolve("storage").resolve("cvs").normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(safeFileName).normalize();
        if (!target.startsWith(dir)) {
            throw new SecurityException("Đường dẫn lưu file không hợp lệ.");
        }
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "storage/cvs/" + safeFileName;
    }

    /** Lưu ảnh đại diện CV vào uploads/avatars và trả về URL dạng /uploads/avatars/{tên file}. */
    public String storeAvatar(MultipartFile avatarFile) {
        try {
            validateAvatarFile(avatarFile);
            String ext = extractLowerExtension(avatarFile.getOriginalFilename());
            String safeFileName = "cv_avatar_" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + ext;

            Path dir = baseDirPath().resolve("uploads").resolve("avatars").normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(safeFileName).normalize();
            if (!target.startsWith(dir)) {
                throw new SecurityException("Đường dẫn lưu ảnh không hợp lệ.");
            }
            try (InputStream is = avatarFile.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // Đồng bộ sang static/uploads/avatars để phục vụ ResourceHandler nếu có
            try {
                Path staticDir = baseDirPath().resolve("src/main/resources/static/uploads/avatars").normalize();
                Files.createDirectories(staticDir);
                Files.copy(target, staticDir.resolve(safeFileName).normalize(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}

            return "/uploads/avatars/" + safeFileName;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi lưu ảnh: " + e.getMessage());
        }
    }

    // ==========================================
    // TÌM FILE AN TOÀN (CHẶN PATH TRAVERSAL)
    // ==========================================

    private File existingRegularFile(Path root, String relative) {
        Path normRoot = root.toAbsolutePath().normalize();
        Path resolved = normRoot.resolve(relative).normalize();
        if (!resolved.startsWith(normRoot)) {
            return null;
        }
        File f = resolved.toFile();
        return (f.exists() && f.isFile()) ? f : null;
    }

    /** Tìm file CV tải lên theo URL lưu trong DB (hỗ trợ cả storage/cvs/ mới và /uploads/cv/ cũ); null nếu không thấy. */
    public File resolveCvFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) return null;
        String cleanUrl = fileUrl.trim().replace("\\", "/");
        if (cleanUrl.contains("..")) return null;
        String relative = cleanUrl.startsWith("/") ? cleanUrl.substring(1) : cleanUrl;

        Path base = baseDirPath();
        // 1. Đường dẫn tương đối trực tiếp (storage/cvs/... hoặc uploads/cv/...)
        File f = existingRegularFile(base.resolve("storage").resolve("cvs"), relative.startsWith("storage/cvs/") ? relative.substring("storage/cvs/".length()) : relative);
        if (f != null) return f;

        f = existingRegularFile(base.resolve("uploads").resolve("cv"), relative.startsWith("uploads/cv/") ? relative.substring("uploads/cv/".length()) : relative);
        if (f != null) return f;

        // 2. Fallback theo tên file (cho dữ liệu cũ)
        String fileName = Paths.get(cleanUrl).getFileName().toString();
        f = existingRegularFile(base.resolve("storage").resolve("cvs"), fileName);
        if (f != null) return f;

        f = existingRegularFile(base.resolve("uploads").resolve("cv"), fileName);
        if (f != null) return f;

        f = existingRegularFile(base.resolve("uploads"), fileName);
        if (f != null) return f;

        f = existingRegularFile(base.resolve("src/main/resources/static"), relative);
        if (f != null) return f;

        return existingRegularFile(base.resolve("target/classes/static"), relative);
    }

    /** Tìm ảnh đại diện theo URL lưu trong DB; null nếu không thấy. */
    public File resolveAvatarFile(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) return null;
        String cleanUrl = avatarUrl.trim().replace("\\", "/");
        if (cleanUrl.contains("..")) return null;
        String relative = cleanUrl.startsWith("/") ? cleanUrl.substring(1) : cleanUrl;

        Path base = baseDirPath();
        File f = existingRegularFile(base, relative);
        if (f != null && (f.toPath().toAbsolutePath().normalize().startsWith(base.resolve("uploads"))
                || f.toPath().toAbsolutePath().normalize().startsWith(base.resolve("src/main/resources/static")))) {
            return f;
        }

        f = existingRegularFile(base.resolve("src/main/resources/static"), relative);
        if (f != null) return f;

        String fileName = Paths.get(cleanUrl).getFileName().toString();
        f = existingRegularFile(base.resolve("uploads").resolve("avatars"), fileName);
        if (f != null) return f;

        return existingRegularFile(base.resolve("uploads"), fileName);
    }

    /** Ảnh avatar mặc định (ưu tiên file trong source, sau đó classpath); null nếu không có. */
    public Resource getDefaultAvatar() {
        try {
            File defFile = baseDirPath().resolve("src/main/resources/" + DEFAULT_AVATAR_PATH).toFile();
            if (defFile.exists()) {
                return new FileSystemResource(defFile);
            }
            Resource defaultAvatar = new ClassPathResource(DEFAULT_AVATAR_PATH);
            if (defaultAvatar.exists()) {
                return defaultAvatar;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
