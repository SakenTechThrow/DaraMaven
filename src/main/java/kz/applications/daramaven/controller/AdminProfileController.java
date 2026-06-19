package kz.applications.daramaven.controller;

import kz.applications.daramaven.dto.ProfileResponse;
import kz.applications.daramaven.dto.UpdateProfileRequest;
import kz.applications.daramaven.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/{id}/profile")
    public ProfileResponse getUserProfileByAdmin(@PathVariable Long id){
        return userProfileService.getProfileByUserIdForAdmin(id);
    }

    @PatchMapping("/{id}/profile")
    public ProfileResponse updateUserProfileByAdmin(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request
            ){
        return userProfileService.updateProfileByUserIdForAdmin(id, request);
    }
}
