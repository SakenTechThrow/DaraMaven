package kz.applications.daramaven.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    private LocalDate birthDate;
    private String bio;
}
