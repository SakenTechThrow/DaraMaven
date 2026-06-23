package kz.applications.daramaven.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponse {
    private Long id;

    private Long actorUserId;
    private String actorEmail;

    private String action;

    private String targetType;
    private Long targetId;

    private String description;

    private String ipAddress;
    private String userAgent;

    private LocalDateTime createdAt;
}
