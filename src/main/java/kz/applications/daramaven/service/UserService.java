package kz.applications.daramaven.service;

import kz.applications.daramaven.dto.*;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.repository.UserProfileRepository;
import kz.applications.daramaven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileRepository userProfileRepository;

    public User getCurrentUser(){
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
        ));
    }
    public UserResponse getMe(){
        User user = getCurrentUser();
        return mapToUserResponse(user);
    }

    public UserResponse updateMe(UpdateUserRequest request){
        User user = getCurrentUser();
        if (request.getEmail() == null || request.getEmail().isBlank()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }
        if (request.getEmail().equals(user.getEmail())){
            return mapToUserResponse(user);
        }
        if (userRepository.existsByEmail(request.getEmail())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already exist"
            );
        }
        user.setEmail(request.getEmail());
        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }
    public String changePassword(ChangePasswordRequest request){
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Old password is incorrect"
            );
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password changed successfully";
    }
    public Page<UserResponse> getAllUsersForAdmin(int page, int size){
        Pageable pageable = PageRequest.of(page, size);

        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }
    public UserResponse getUserByIdForAdmin(Long id) {
        User user = findUserById(id);
        return mapToUserResponse(user);
    }
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request){
        User user = findUserById(id);

        String newRole = request.getRole();
        if (!"USER".equals(newRole) && !"ADMIN".equals(newRole)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role"
            );
        }
        if ("ADMIN".equals(user.getRole()) && "USER".equals(newRole)){
            long adminCount = userRepository.countByRole("ADMIN");

            if (adminCount <= 1){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot remove role from the last admin"
                );
            }
        }
        user.setRole(newRole);
        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request){
        User user = findUserById(id);
        user.setActive(request.getActive());
        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    @Transactional
    public String deleteUserForAdmin(Long id){
        User currentAdmin = getCurrentUser();
        User userToDelete = findUserById(id);

        if (currentAdmin.getId().equals(userToDelete.getId())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin cannot delete himself"
            );
        }

        if ("ADMIN".equals(userToDelete.getRole())){
            long adminCount = userRepository.countByRole("ADMIN");

            if (adminCount <= 1){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot delete the last admin"
                );
            }
        }

        if (userProfileRepository.existsByUserId(userToDelete.getId())){
            userProfileRepository.deleteByUserId(userToDelete.getId());
        }

        userRepository.delete(userToDelete);
        return "User deleted successfully";
    }
    private User findUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private UserResponse mapToUserResponse(User user){
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
