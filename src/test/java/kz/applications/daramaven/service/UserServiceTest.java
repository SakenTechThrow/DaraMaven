package kz.applications.daramaven.service;

import kz.applications.daramaven.dto.BlockUserRequest;
import kz.applications.daramaven.dto.ChangePasswordRequest;
import kz.applications.daramaven.dto.UserResponse;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.repository.UserProfileRepository;
import kz.applications.daramaven.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static  org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    @InjectMocks
    private UserService userService;

    @Test
        void changePassword_shouldChangePassword_whenOldPasswordIsCorrect(){
        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .password("old-encoded-password")
                .role("USER")
                .active(true)
                .deleted(false)
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-password");
        request.setNewPassword("new-password");


        doReturn(user)
                .when(userService)
                .getCurrentUser();

        when(passwordEncoder.matches("old-password", "old-encoded-password"))
                .thenReturn(true);

        when(passwordEncoder.encode("new-password"))
                .thenReturn("new-encoded-password");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        String result = userService.changePassword(request);

        assertEquals("Password changed successfully", result);
        assertEquals("new-encoded-password", user.getPassword());

        verify(passwordEncoder).matches("old-password", "old-encoded-password");
        verify(passwordEncoder).encode("new-password");
        verify(userRepository).save(user);

        verify(auditLogService).log(
                eq(user),
                eq("PASSWORD_CHANGED"),
                eq("USER"),
                eq(1L),
                eq("User changed password")
        );
    }
    @Test
    void changePassword_shouldThrowException_whenOldPasswordIsWrong(){
        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .password("old-encoded-password")
                .role("USER")
                .active(true)
                .deleted(false)
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong-password");
        request.setNewPassword("new-password");


        doReturn(user)
                .when(userService)
                .getCurrentUser();
        when(passwordEncoder.matches("wrong-password", "old-encoded-password"))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                ()->userService.changePassword(request)
        );

        assertEquals("Old password is incorrect", exception.getReason());

        verify(passwordEncoder).matches("wrong-password", "old-encoded-password");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));

        verify(auditLogService, never()).log(
                any(),
                anyString(),
                anyString(),
                anyLong(),
                anyString()
        );

    }

    @Test
    void blockUserForAdmin_shouldBlockUser_whenDataIsValid(){
        User admin = User.builder()
                .id(1L)
                .email("admin@mail.com")
                .role("ADMIN")
                .active(true)
                .deleted(false)
                .build();

        User userToBlock = User.builder()
                .id(2L)
                .email("user@mail.com")
                .role("USER")
                .active(true)
                .deleted(false)
                .build();

        BlockUserRequest request = new BlockUserRequest();
        request.setReason("Violation of platform rules");

        doReturn(admin)
                .when(userService)
                .getCurrentUser();

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(userToBlock));
        when(userRepository.save(any(User.class)))
                .thenReturn(userToBlock);

        UserResponse response = userService.blockUserForAdmin(2L, request);

        assertEquals(2L, response.getId());
        assertEquals("user@mail.com", response.getEmail());
        assertEquals(false, response.getActive());

        assertEquals(false, userToBlock.getActive());
        assertEquals("Violation of platform rules", userToBlock.getBlockedReason());

        assertNotNull(userToBlock.getBlockedAt());

        verify(userRepository).findById(2L);
        verify(userRepository).save(userToBlock);
        verify(refreshTokenService).revokeAllTokensByUser(userToBlock);

        verify(auditLogService).log(
                eq(admin),
                eq("USER_BLOCKED"),
                eq("USER"),
                eq(2L),
                eq("Admin blocked user user@mail.com. Reason: Violation of platform rules")
        );
    }

    @Test
    void deleteUserForAdmin_shouldSoftDeleteUser_whenDataIsValid() {
        User admin = User.builder()
                .id(1L)
                .email("admin@mail.com")
                .role("ADMIN")
                .active(true)
                .deleted(false)
                .build();

        User userToDelete = User.builder()
                .id(2L)
                .email("user@mail.com")
                .role("USER")
                .active(true)
                .deleted(false)
                .build();

        doReturn(admin)
                .when(userService)
                .getCurrentUser();

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(userToDelete));

        when(userRepository.save(any(User.class)))
                .thenReturn(userToDelete);

        String result = userService.deleteUserForAdmin(2L);

        assertEquals("User soft deleted successfully", result);

        assertEquals(true, userToDelete.getDeleted());
        assertEquals(false, userToDelete.getActive());

        assertNotNull(userToDelete.getDeletedAt());

        verify(userRepository).findById(2L);
        verify(userRepository).save(userToDelete);
        verify(refreshTokenService).revokeAllTokensByUser(userToDelete);

        verify(auditLogService).log(
                eq(admin),
                eq("USER_SOFT_DELETED"),
                eq("USER"),
                eq(2L),
                eq("Admin soft deleted user: user@mail.com")
        );

    }

}
