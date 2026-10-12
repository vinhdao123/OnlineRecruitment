
package OnlineRecruitment.OnlineRecruitment.dto;

import OnlineRecruitment.OnlineRecruitment.entity.User;

public record RecruiterProfileDto(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String location
) {
    public static RecruiterProfileDto from(User user) {
        return new RecruiterProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getLocation()
        );
    }
}