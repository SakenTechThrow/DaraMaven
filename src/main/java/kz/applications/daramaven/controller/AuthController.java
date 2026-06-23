package kz.applications.daramaven.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kz.applications.daramaven.dto.*;
import kz.applications.daramaven.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request){
        String message = authService.register(request);
        return ApiResponse.success(message);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest){
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
            ){
        return authService.refresh(request, httpRequest);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request){
        String message = authService.logout(request);
        return ApiResponse.success(message);
    }

    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(){
        String message = authService.logoutAll();
        return ApiResponse.success(message);
    }
}
