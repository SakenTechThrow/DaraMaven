package kz.applications.daramaven.service;

import kz.applications.daramaven.dto.*;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.repository.UserProfileRepository;
import kz.applications.daramaven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

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

        auditLogService.log(
                user,
                "PASSWORD_CHANGED",
                "USER",
                user.getId(),
                "User changed password"
        );

        return "Password changed successfully";
    }
    public PageResponse<UserResponse> getAllUsersForAdmin(
            int page,
            int size,
            String email,
            String role,
            Boolean active,
            Boolean deleted,
            String sortBy,
            String sortDirection
    ){
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        Specification<User> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (email != null && !email.isBlank()){
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("email")),
                                "%" + email.toLowerCase() + "%"
                        )
                );
            }
            if (role != null && !role.isBlank()){
                predicates.add(
                        criteriaBuilder.equal(root.get("role"), role)
                );
            }

            if (active != null){
                predicates.add(
                        criteriaBuilder.equal(root.get("active"), active)
                );
            }

            if (deleted != null){
                predicates.add(
                        criteriaBuilder.equal(root.get("deleted"), deleted)
                );
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserResponse> usersPage = userRepository.findAll(specification, pageable)
                .map(this::mapToUserResponse);

        return PageResponse.<UserResponse>builder()
                .content(usersPage.getContent())
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .last(usersPage.isLast())
                .build();

    }
    public UserResponse getUserByIdForAdmin(Long id) {
        User user = findUserById(id);
        return mapToUserResponse(user);
    }
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request){
        User user = findUserById(id);
        User currentAdmin = getCurrentUser();
        String oldRole = user.getRole();

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

        auditLogService.log(
                currentAdmin,
                "USER_ROLE_CHANGED",
                "USER",
                savedUser.getId(),
                "Changed role from " + oldRole + " to " + newRole+" for user "+savedUser.getEmail()

        );


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
        if (Boolean.TRUE.equals(userToDelete.getDeleted())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User already deleted"
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

        userToDelete.setDeleted(true);
        userToDelete.setDeletedAt(LocalDateTime.now());

        userToDelete.setActive(false);

        userRepository.save(userToDelete);

        refreshTokenService.revokeAllTokensByUser(userToDelete);

        auditLogService.log(
                currentAdmin,
                "USER_SOFT_DELETED",
                "USER",
                userToDelete.getId(),
                "Admin soft deleted user: " + userToDelete.getEmail()
        );

        return "User soft deleted successfully";
    }

    @Transactional
    public UserResponse restoreUserForAdmin(Long id){
        User user = findUserById(id);
        User currentAdmin = getCurrentUser();

        if (!Boolean.TRUE.equals(user.getDeleted())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User is not deleted"
            );
        }

        user.setDeleted(false);
        user.setDeletedAt(null);
        user.setActive(true);

        User restoredUser = userRepository.save(user);

        auditLogService.log(
                currentAdmin,
                "USER_RESTORED",
                "USER",
                restoredUser.getId(),
                "Admin restored user: " + restoredUser.getEmail()
        );


        return mapToUserResponse(restoredUser);

    }

    @Transactional
    public UserResponse blockUserForAdmin(Long id, BlockUserRequest request){
        User currentAdmin = getCurrentUser();
        User userToBlock = findUserById(id);

        if (currentAdmin.getId().equals(userToBlock.getId())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin cannot block himself"
            );
        }

        if (Boolean.TRUE.equals(userToBlock.getDeleted())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot block deleted user"
            );
        }

        if (!userToBlock.getActive()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User is already blocked"
            );
        }

        userToBlock.setActive(false);
        userToBlock.setBlockedReason(request.getReason());
        userToBlock.setBlockedAt(LocalDateTime.now());

        User blockedUser = userRepository.save(userToBlock);

        refreshTokenService.revokeAllTokensByUser(userToBlock);

        auditLogService.log(
                currentAdmin,
                "USER_BLOCKED",
                "USER",
                blockedUser.getId(),
                "Admin blocked user " + blockedUser.getEmail() + ". Reason: " + request.getReason()
        );

        return mapToUserResponse(blockedUser);
    }

    @Transactional
    public UserResponse unblockUserForAdmin(Long id){
        User userToUnblock = findUserById(id);
        User currentAdmin = getCurrentUser();

        if (Boolean.TRUE.equals(userToUnblock.getDeleted())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot unblock deleted user. Restore user first"
            );
        }
        if (Boolean.TRUE.equals(userToUnblock.getActive())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User is already active"
            );
        }

        userToUnblock.setActive(true);
        userToUnblock.setBlockedReason(null);
        userToUnblock.setBlockedAt(null);

        User unblockedUser = userRepository.save(userToUnblock);

        auditLogService.log(
                currentAdmin,
                "USER_UNBLOCKED",
                "USER",
                unblockedUser.getId(),
                "Admin unblocked user: " + unblockedUser.getEmail()
        );

        return mapToUserResponse(unblockedUser);
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
                .deleted(user.getDeleted())
                .deletedAt(user.getDeletedAt())
                .blockedReason(user.getBlockedReason())
                .blockedAt(user.getBlockedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
