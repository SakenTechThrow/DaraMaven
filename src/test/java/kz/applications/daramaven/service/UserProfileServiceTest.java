package kz.applications.daramaven.service;

import kz.applications.daramaven.dto.CreateProfileRequest;
import kz.applications.daramaven.dto.ProfileResponse;
import kz.applications.daramaven.dto.UpdateProfileRequest;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.entity.UserProfile;
import kz.applications.daramaven.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserService userService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void createMyProfile_shouldCreateProfile_whenProfileDoesNotExist(){
        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .role("USER")
                .active(true)
                .deleted(false)
                .build();

        CreateProfileRequest request = new CreateProfileRequest();
        request.setFirstName("Ali");
        request.setLastName("Nurbek");
        request.setPhone("+77071234567");
        request.setCity("Almaty");
        request.setBirthDate(LocalDate.of(2000, 5, 15));
        request.setBio("Java backend developer");

        when(userService.getCurrentUser())
                .thenReturn(user);

        when(userProfileRepository.existsByUserId(1L))
                .thenReturn(false);

        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> {
                    UserProfile profile = invocation.getArgument(0);
                    profile.setId(10L);
                    return profile;
                });

        ProfileResponse response = userProfileService.createMyProfile(request);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("user@mail.com", response.getEmail());
        assertEquals("Ali", response.getFirstName());
        assertEquals("Nurbek", response.getLastName());
        assertEquals("+77071234567", response.getPhone());
        assertEquals("Almaty", response.getCity());
        assertEquals(LocalDate.of(2000, 5, 15), response.getBirthDate());
        assertEquals("Java backend developer", response.getBio());

        verify(userService).getCurrentUser();
        verify(userProfileRepository).existsByUserId(1L);
        verify(userProfileRepository).save(any(UserProfile.class));

        verify(auditLogService).log(
                eq(user),
                eq("PROFILE_CREATED"),
                eq("PROFILE"),
                eq(10L),
                eq("User created profile")
        );

    }

    void createMyProfile_shouldThrowException_whenProfileAlreadyExists() {
        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .build();

        CreateProfileRequest request = new CreateProfileRequest();
        request.setFirstName("Ali");

        when(userService.getCurrentUser())
                .thenReturn(user);

        when(userProfileRepository.existsByUserId(1L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                ()->userProfileService.createMyProfile(request)
        );

        assertEquals("Profile already exists", exception.getReason());

        verify(userService).getCurrentUser();
        verify(userProfileRepository).existsByUserId(1L);
        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(auditLogService, never()).log(any(), anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void updateMyProfile_shouldUpdateProfile_whenProfileExists() {
        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .build();

        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .firstName("OldName")
                .lastName("OldLastName")
                .phone("+77070000000")
                .city("OldCity")
                .birthDate(LocalDate.of(2000, 1, 1))
                .bio("Old bio")
                .build();

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("NewName");
        request.setCity("Astana");
        request.setBio("New bio");

        when(userService.getCurrentUser())
                .thenReturn(user);
        when(userProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenReturn(profile);

        ProfileResponse response = userProfileService.updateMyProfile(request);

        assertEquals(10L, response.getId());
        assertEquals("NewName", response.getFirstName());
        assertEquals("OldLastName", response.getLastName());
        assertEquals("+77070000000", response.getPhone());
        assertEquals("Astana", response.getCity());
        assertEquals("New bio", response.getBio());

        verify(userService).getCurrentUser();
        verify(userProfileRepository).findByUserId(1L);
        verify(userProfileRepository).save(profile);

        verify(auditLogService).log(
                eq(user),
                eq("PROFILE_UPDATED"),
                eq("PROFILE"),
                eq(10L),
                eq("User updated profile")
        );
    }


}
