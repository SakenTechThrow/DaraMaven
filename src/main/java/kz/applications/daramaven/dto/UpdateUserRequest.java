package kz.applications.daramaven.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    @Email(message = "Email should be valid")
    private String email;
}
