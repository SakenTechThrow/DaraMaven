package kz.applications.daramaven.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProfileResponse {
    private Long id;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    private LocalDate birthDate;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
