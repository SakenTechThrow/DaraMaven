package kz.applications.daramaven.service;

import jakarta.servlet.http.HttpServletRequest;
import kz.applications.daramaven.dto.AuditLogResponse;
import kz.applications.daramaven.dto.PageResponse;
import kz.applications.daramaven.entity.AuditLog;
import kz.applications.daramaven.entity.User;
import kz.applications.daramaven.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(
            User actor,
            String action,
            String targetType,
            Long targetId,
            String description
    ){
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .description(description)
                .build();

        auditLogRepository.save(auditLog);
    }

    public void log(
            User actor,
            String action,
            String targetType,
            Long targetId,
            String description,
            HttpServletRequest request
    ){
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .description(description)
                .ipAddress(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();

        auditLogRepository.save(auditLog);
    }

    public PageResponse<AuditLogResponse> getAuditLogsForAdmin(int page, int size){
        Pageable pageable = PageRequest.of(page, size);

        Page<AuditLogResponse> auditLogsPage = auditLogRepository.findAll(pageable)
                .map(this::mapToAuditLogResponse);

        return PageResponse.<AuditLogResponse>builder()
                .content(auditLogsPage.getContent())
                .page(auditLogsPage.getNumber())
                .size(auditLogsPage.getSize())
                .totalElements(auditLogsPage.getTotalElements())
                .last(auditLogsPage.isLast())
                .build();
    }

    private AuditLogResponse mapToAuditLogResponse(AuditLog auditLog){
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorUserId(auditLog.getActorUserId())
                .actorEmail(auditLog.getActorEmail())
                .action(auditLog.getAction())
                .targetType(auditLog.getTargetType())
                .targetId(auditLog.getTargetId())
                .description(auditLog.getDescription())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
