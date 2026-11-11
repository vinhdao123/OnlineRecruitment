package OnlineRecruitment.OnlineRecruitment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = "file:" + System.getProperty("user.dir") + "/uploads/";
        String avatarDir = "file:" + System.getProperty("user.dir") + "/uploads/avatars/";
        // Không map uploads/cv/ ra static public; file CV được phục vụ qua controller có kiểm tra quyền.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir, avatarDir, "file:src/main/resources/static/uploads/");
    }
}
