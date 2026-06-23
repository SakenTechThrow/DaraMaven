package kz.applications.daramaven.controller;

import jakarta.validation.Valid;
import kz.applications.daramaven.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import kz.applications.daramaven.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ){
        return userService.getAllUsersForAdmin(
                page,
                size,
                email,
                role,
                active,
                deleted,
                sortBy,
                sortDirection
        );
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

    @PatchMapping("/{id}/block")
    public UserResponse blockUser(
            @PathVariable Long id,
            @Valid @RequestBody BlockUserRequest request
            ){
            return userService.blockUserForAdmin(id, request);
    }

    @PatchMapping("/{id}/unblock")
    public UserResponse unblockUser(@PathVariable Long id){
        return userService.unblockUserForAdmin(id);
    }

    @PatchMapping("/{id}/restore")
    public UserResponse restoreUser(@PathVariable Long id){
        return userService.restoreUserForAdmin(id);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id){
        String message = userService.deleteUserForAdmin(id);
        return ApiResponse.success(message);
    }

}
