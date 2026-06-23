package kz.applications.daramaven.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockUserRequest {
    @NotBlank(message = "Block reason is required")
    private String reason;
}
