package kz.applications.daramaven.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SessionResponse {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String userAgent;
    private String ipAddress;
    private Boolean revoked;
}
