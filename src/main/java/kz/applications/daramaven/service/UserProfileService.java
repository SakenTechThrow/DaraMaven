package kz.applications.daramaven.service;

import kz.applications.daramaven.dto.*;
import org.springframework.transaction.annotation.Transactional;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.entity.UserProfile;
import kz.applications.daramaven.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserService userService;

    public ProfileResponse createMyProfile(CreateProfileRequest request){
        User user = userService.getCurrentUser();

        if (userProfileRepository.existsByUserId(user.getId())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Profile already exists"
            );
        }
        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .city(request.getCity())
                .birthDate(request.getBirthDate())
                .bio(request.getBio())
                .build();
        UserProfile savedProfile = userProfileRepository.save(profile);
        return mapToProfileResponse(savedProfile);
    }
    public ProfileResponse getMyProfile(){
        User user = userService.getCurrentUser();

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found"
                ));
        return mapToProfileResponse(profile);
    }
    public ProfileResponse updateMyProfile(UpdateProfileRequest request){
        User user = userService.getCurrentUser();

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found"
                ));

        updateProfileFields(profile, request);

        UserProfile savedProfile = userProfileRepository.save(profile);
        return mapToProfileResponse(savedProfile);
    }

    public String deleteMyProfile(){
        User user = userService.getCurrentUser();
        if (!userProfileRepository.existsByUserId(user.getId())){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Profile not found"
            );
        }

        userProfileRepository.deleteByUserId(user.getId());
        return "Profile deleted successfully";
    }
    public ProfileResponse getProfileByUserIdForAdmin(Long userId){
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found"
                ));
        return mapToProfileResponse(profile);
    }

    public ProfileResponse updateProfileByUserIdForAdmin(Long userId, UpdateProfileRequest request){
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Profile not found"
                ));
        updateProfileFields(profile, request);
        UserProfile savedProfile = userProfileRepository.save(profile);
        return mapToProfileResponse(savedProfile);
    }

    private void updateProfileFields(UserProfile profile, UpdateProfileRequest request){
        if (request.getFirstName() != null){
            profile.setFirstName(request.getFirstName());
        }
        if (request.getLastName()!= null){
            profile.setLastName(request.getLastName());
        }
        if (request.getPhone() != null){
            profile.setPhone(request.getPhone());
        }
        if (request.getCity() != null){
            profile.setCity(request.getCity());
        }
        if (request.getBirthDate() != null){
            profile.setBirthDate(request.getBirthDate());
        }
        if (request.getBio() != null){
            profile.setBio(request.getBio());
        }
    }

    private ProfileResponse mapToProfileResponse(UserProfile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .email(profile.getUser().getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phone(profile.getPhone())
                .city(profile.getCity())
                .birthDate(profile.getBirthDate())
                .bio(profile.getBio())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
