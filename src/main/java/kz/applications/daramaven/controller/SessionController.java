package kz.applications.daramaven.controller;

import kz.applications.daramaven.dto.SessionResponse;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.service.RefreshTokenService;
import kz.applications.daramaven.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @GetMapping
    public List<SessionResponse> getMySessions() {
        User currentUser = userService.getCurrentUser();

        return refreshTokenService.getActiveSessionsForUser(currentUser);
    }

    @DeleteMapping("/{sessionId}")
    public String revokeSession(@PathVariable Long sessionId){
        User currentUser = userService.getCurrentUser();

        return refreshTokenService.revokeSessionById(currentUser, sessionId);
    }

}
