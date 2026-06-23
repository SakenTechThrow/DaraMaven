package kz.applications.daramaven.service;

import jakarta.servlet.http.HttpServletRequest;
import kz.applications.daramaven.dto.*;
import kz.applications.daramaven.entity.RefreshToken;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public String register(RegisterRequest request){
        if (userRepository.existsByEmail(request.getEmail())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exist");
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .active(true)
                .deleted(false)
                .build();
        User savedUser = userRepository.save(user);

        auditLogService.log(
                null,
                "USER_REGISTERED",
                "USER",
                savedUser.getId(),
                "New user registered: " + savedUser.getEmail()
        );

        return "User registered successfully";
    }
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest){
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"
                ));

        if (Boolean.TRUE.equals(user.getDeleted())){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User account is deleted"
            );
        }

        if (!user.getActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is blocked"
            );
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
            );
        }

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole()
        );

        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user,
                userAgent,
                ipAddress
        );

        auditLogService.log(
                user,
                "USER_LOGIN",
                "USER",
                user.getId(),
                "User logged in: " + user.getEmail(),
                httpRequest
        );

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtService.getAccessExpiration()
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest){
        RefreshToken oldRefreshToken = refreshTokenService.findValidRefreshToken(
                request.getRefreshToken()
        );

        User user = oldRefreshToken.getUser();

        if (Boolean.TRUE.equals(user.getDeleted())){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User account is deleted"
            );
        }
        if (!user.getActive()){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is blocked"
            );
        }

        refreshTokenService.revokeRefreshToken(oldRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole()
        );

        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                user,
                userAgent,
                ipAddress
        );

        auditLogService.log(
                user,
                "TOKEN_REFRESHED",
                "SESSION",
                newRefreshToken.getId(),
                "User refreshed access token",
                httpRequest
        );

        return new AuthResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                "Bearer",
                jwtService.getAccessExpiration()
        );
    }

    @Transactional
    public String logout(LogoutRequest request){
        User currentUser = userService.getCurrentUser();

        RefreshToken refreshToken = refreshTokenService.findValidRefreshToken(
                request.getRefreshToken()
        );

        if (!refreshToken.getUser().getId().equals(currentUser.getId())){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot logout another user's session"
            );
        }

        refreshTokenService.revokeRefreshToken(refreshToken);

        auditLogService.log(
                currentUser,
                "USER_LOGOUT",
                "SESSION",
                refreshToken.getId(),
                "User logged out from one session"
        );

        return "Logged out successfully";
    }

    @Transactional
    public String logoutAll() {
        User currentUser = userService.getCurrentUser();

        refreshTokenService.revokeAllTokensByUser(currentUser);

        auditLogService.log(
                currentUser,
                "USER_LOGOUT_ALL",
                "SESSION",
                currentUser.getId(),
                "User logged out from all devices"
        );

        return "Logged out from all devices successfully";
    }

}
