package kz.applications.daramaven.service;

import kz.applications.daramaven.config.JwtConfig;
import kz.applications.daramaven.entity.RefreshToken;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshToken_shouldCreateAndSaveToken_whenDataIsValid(){
        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .role("USER")
                .active(true)
                .deleted(false)
                .build();

        when(jwtConfig.getRefreshExpiration())
                .thenReturn(604800000L);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    RefreshToken token = invocation.getArgument(0);
                    token.setId(10L);
                    return token;
                });

        RefreshToken result = refreshTokenService.createRefreshToken(
                user,
                "Postman",
                "127.0.0.1"
        );

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(user, result.getUser());
        assertEquals(false, result.getRevoked());
        assertEquals("Postman", result.getUserAgent());
        assertEquals("127.0.0.1", result.getIpAddress());
        assertNotNull(result.getToken());
        assertNotNull(result.getExpiresAt());

        verify(jwtConfig).getRefreshExpiration();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void findValidRefreshToken_shouldReturnToken_whenTokenIsValid(){
        User user = User.builder()
                .id(1L)
                .email("user@mail.com")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .token("valid-refresh-token")
                .user(user)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.findValidRefreshToken("valid-refresh-token");

        assertEquals(refreshToken,result);
        assertEquals("valid-refresh-token", result.getToken());

        verify(refreshTokenRepository).findByToken("valid-refresh-token");
    }

    @Test
    void findValidRefreshToken_shouldThrowException_whenTokenIsRevoked(){
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .token("revoked-token")
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(refreshToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                ()->refreshTokenService.findValidRefreshToken("revoked-token")
        );
        assertEquals("Invalid refresh token", exception.getReason());
        verify(refreshTokenRepository).findByToken("revoked-token");

    }

    @Test
    void findValidRefreshToken_shouldThrowException_whenTokenIsExpired() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .token("expired-token")
                .revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(refreshToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                ()-> refreshTokenService.findValidRefreshToken("expired-token")
        );

        assertEquals("Refresh token expired", exception.getReason());

        verify(refreshTokenRepository).findByToken("expired-token");
    }
}
