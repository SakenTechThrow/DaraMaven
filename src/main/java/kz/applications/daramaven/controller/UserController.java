package kz.applications.daramaven.controller;

import jakarta.validation.Valid;
import kz.applications.daramaven.dto.ApiResponse;
import kz.applications.daramaven.dto.ChangePasswordRequest;
import kz.applications.daramaven.dto.UpdateUserRequest;
import kz.applications.daramaven.dto.UserResponse;
import kz.applications.daramaven.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getMe(){
        return userService.getMe();
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateUserRequest request){
        return userService.updateMe(request);
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request){
        String message = userService.changePassword(request);
        return ApiResponse.success(message);
    }
}
