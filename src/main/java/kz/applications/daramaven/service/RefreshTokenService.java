package kz.applications.daramaven.service;

import kz.applications.daramaven.config.JwtConfig;
import kz.applications.daramaven.dto.SessionResponse;
import kz.applications.daramaven.entity.RefreshToken;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    public RefreshToken createRefreshToken(User user, String userAgent, String ipAddress){
        String tokenValue = UUID.randomUUID().toString();

        LocalDateTime expiresAt = LocalDateTime.now()
                .plus(Duration.ofMillis(jwtConfig.getRefreshExpiration()));

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken findValidRefreshToken(String token){
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid refresh token"
        ));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid refresh token"
            );
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token expired"
            );
        }
        return refreshToken;
    }
    public void revokeRefreshToken(RefreshToken refreshToken){
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllTokensByUser(User user){
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId());

        for (RefreshToken token : activeTokens){
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    public List<SessionResponse> getActiveSessionsForUser(User user){
        List<RefreshToken> activeTokens =
                refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId());

        return activeTokens.stream()
                .map(this::mapToSessionResponse)
                .toList();
    }

    @Transactional
    public String revokeSessionById(User currentUser, Long sessionId){
        RefreshToken session = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Session not found"
                ));

        if (!session.getUser().getId().equals(currentUser.getId())){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot revoke another users session"
            );
        }

        if (Boolean.TRUE.equals(session.getRevoked())){
            return "Session already revoked";
        }

        session.setRevoked(true);
        refreshTokenRepository.save(session);

        return "Session revoked successfully";
    }

    private SessionResponse mapToSessionResponse(RefreshToken refreshToken){
        return SessionResponse.builder()
                .id(refreshToken.getId())
                .createdAt(refreshToken.getCreatedAt())
                .expiresAt(refreshToken.getExpiresAt())
                .userAgent(refreshToken.getUserAgent())
                .ipAddress(refreshToken.getIpAddress())
                .revoked(refreshToken.getRevoked())
                .build();
    }
}
