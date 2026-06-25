package kz.applications.daramaven.service;

import jakarta.servlet.http.HttpServletRequest;
import kz.applications.daramaven.dto.AuthResponse;
import kz.applications.daramaven.dto.LoginRequest;
import kz.applications.daramaven.dto.RegisterRequest;
import kz.applications.daramaven.entity.RefreshToken;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions. assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUser_whenEmailDoesNotExist(){

        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@mail.com");
        request.setPassword("123456");

        when(userRepository.existsByEmail("user@mail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("123456"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        String result = authService.register(request);

        assertEquals("User registered successfully", result);

        verify(userRepository).existsByEmail("user@mail.com");
        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));

        verify(auditLogService).log(
                isNull(),
                eq("USER_REGISTERED"),
                eq("USER"),
                eq(1L),
                eq("New user registered: user@mail.com")
        );
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists(){
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@mail.com");
        request.setPassword("123456");

        when(userRepository.existsByEmail("user@mail.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already exist", exception.getReason());

        verify(userRepository).existsByEmail("user@mail.com");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid(){
        LoginRequest request = new LoginRequest();
        request.setEmail("user@mail.com");
        request.setPassword("123456");

        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .password("encoded-password")
                .role("USER")
                .active(true)
                .deleted(false)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .token("refresh-token-value")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .userAgent("Postman")
                .ipAddress("127.0.0.1")
                .build();

        when(userRepository.findByEmail("user@mail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("123456", "encoded-password"))
                .thenReturn(true);

        when(jwtService.generateAccessToken("user@mail.com", "USER"))
                .thenReturn("access-token-value");

        when(httpRequest.getHeader("User-Agent"))
                .thenReturn("Postman");

        when(httpRequest.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(refreshTokenService.createRefreshToken(user, "Postman", "127.0.0.1"))
                .thenReturn(refreshToken);

        when(jwtService.getAccessExpiration())
                .thenReturn(900000L);

        AuthResponse response = authService.login(request, httpRequest);

        assertEquals("access-token-value", response.getAccessToken());
        assertEquals("refresh-token-value", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900000L, response.getExpiresIn());

        verify(userRepository).findByEmail("user@mail.com");
        verify(passwordEncoder).matches("123456", "encoded-password");
        verify(jwtService).generateAccessToken("user@mail.com", "USER");
        verify(refreshTokenService).createRefreshToken(user, "Postman", "127.0.0.1");
        verify(auditLogService).log(
                eq(user),
                eq("USER_LOGIN"),
                eq("USER"),
                eq(1L),
                eq("User logged in: user@mail.com"),
                eq(httpRequest)
        );
    }

    @Test
    void login_shouldThrowException_whenEmailIsInvalid(){
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@mail.com");
        request.setPassword("123456");

        when(userRepository.findByEmail("wrong@mail.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                ()-> authService.login(request, httpRequest)
        );

        assertEquals("Invalid credentials", exception.getReason());

        verify(userRepository).findByEmail("wrong@mail.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        verify(refreshTokenService, never()).createRefreshToken(any(), anyString(), anyString());
    }

    @Test
    void login_shouldThrowException_whenPasswordIsInvalid(){
        LoginRequest request = new LoginRequest();
        request.setEmail("user@mail.com");
        request.setPassword("wrong-password");

        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .password("encoded-password")
                .role("USER")
                .active(true)
                .deleted(false)
                .build();

        when(userRepository.findByEmail("user@mail.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request, httpRequest)
        );

        assertEquals("Invalid credentials", exception.getReason());

        verify(userRepository).findByEmail("user@mail.com");
        verify(passwordEncoder).matches("wrong-password", "encoded-password");
        verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        verify(refreshTokenService, never()).createRefreshToken(any(), anyString(), anyString());
    }

    @Test
    void login_shouldThrowException_whenUserIsDeleted() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@mail.com");
        request.setPassword("123456");

        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .password("encoded-password")
                .role("USER")
                .active(true)
                .deleted(true)
                .build();

        when(userRepository.findByEmail("user@mail.com"))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                ()-> authService.login(request, httpRequest)
        );

        assertEquals("User account is deleted", exception.getReason());

        verify(userRepository).findByEmail("user@mail.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        verify(refreshTokenService, never()).createRefreshToken(any(), anyString(), anyString());
    }

    @Test
    void login_shouldThrowsException_whenUserIsBlocked(){
        LoginRequest request = new LoginRequest();
        request.setEmail("user@mail.com");
        request.setPassword("123456");

        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .password("encoded-password")
                .role("USER")
                .active(false)
                .deleted(false)
                .build();

        when(userRepository.findByEmail("user@mail.com"))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                ()->authService.login(request, httpRequest)
        );

        assertEquals("User is blocked", exception.getReason());

        verify(userRepository).findByEmail("user@mail.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        verify(refreshTokenService, never()).createRefreshToken(any(), anyString(), anyString());
    }

}
