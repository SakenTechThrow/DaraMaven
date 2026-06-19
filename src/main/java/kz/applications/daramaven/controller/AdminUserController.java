package kz.applications.daramaven.controller;

import jakarta.validation.Valid;
import kz.applications.daramaven.dto.UpdateUserRoleRequest;
import kz.applications.daramaven.dto.UpdateUserStatusRequest;
import kz.applications.daramaven.dto.UserResponse;
import kz.applications.daramaven.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public List<UserResponse> getAllUsers(){
        return userService.getAllUsersForAdmin();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id){
        return userService.getUserByIdForAdmin(id);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
            ){
        return userService.updateUserRole(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
            ){
        return userService.updateUserStatus(id, request);
    }
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id){
        return userService.deleteUserForAdmin(id);
    }
}
