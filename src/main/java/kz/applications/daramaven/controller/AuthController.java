package kz.applications.daramaven.controller;

import jakarta.validation.Valid;
import kz.applications.daramaven.dto.AuthResponse;
import kz.applications.daramaven.dto.LoginRequest;
import kz.applications.daramaven.dto.RegisterRequest;
import kz.applications.daramaven.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }
}
