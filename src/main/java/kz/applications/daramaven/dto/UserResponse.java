package kz.applications.daramaven.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
