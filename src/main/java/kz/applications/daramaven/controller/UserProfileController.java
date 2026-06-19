package kz.applications.daramaven.controller;

import kz.applications.daramaven.dto.CreateProfileRequest;
import kz.applications.daramaven.dto.ProfileResponse;
import kz.applications.daramaven.dto.UpdateProfileRequest;
import kz.applications.daramaven.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/profile")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @PostMapping
    public ProfileResponse createMyProfile(@RequestBody CreateProfileRequest request){
        return userProfileService.createMyProfile(request);
    }

    @GetMapping
    public ProfileResponse getMyProfile(){
        return userProfileService.getMyProfile();
    }

    @PatchMapping
    public ProfileResponse updateMyProfile(@RequestBody UpdateProfileRequest request){
        return userProfileService.updateMyProfile(request);
    }

    @DeleteMapping
    public String deleteMyProfile(){
        return userProfileService.deleteMyProfile();
    }

}
