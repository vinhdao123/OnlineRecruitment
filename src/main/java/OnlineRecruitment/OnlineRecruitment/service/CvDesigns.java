package OnlineRecruitment.OnlineRecruitment.service;

import java.util.List;

/** Presentation variants use cv.template; no schema change is necessary. */
public final class CvDesigns {
    private CvDesigns() {}
    public record Design(String code, String name, String description) {}
    public static List<Design> all() {
        return List.of(
            new Design("professional", "Professional", "Một cột rõ ràng, phù hợp hồ sơ chuyên môn và hệ thống ATS."),
            new Design("modern", "Modern", "Hai cột cân đối, làm nổi bật kỹ năng và kinh nghiệm."),
            new Design("executive", "Executive", "Bố cục trang trọng với tiêu đề lớn, phù hợp quản lý và chuyên gia."));
    }
    public static String require(String code) {
        if (all().stream().noneMatch(d -> d.code().equals(code)))
            throw new IllegalArgumentException("Mẫu trình bày CV không hợp lệ.");
        return code;
    }
    public static String resolve(String code) {
        return all().stream().anyMatch(d -> d.code().equals(code)) ? code : "professional";
    }
}
