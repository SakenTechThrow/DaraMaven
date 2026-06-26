package kz.applications.daramaven.controller;

import kz.applications.daramaven.dto.AuditLogResponse;
import kz.applications.daramaven.dto.PageResponse;
import kz.applications.daramaven.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {
    private final AuditLogService auditLogService;

    @GetMapping
    public PageResponse<AuditLogResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateTo
    ){
        return auditLogService.getAuditLogsForAdmin(
                page,
                size,
                action,
                actorEmail,
                targetType,
                targetId,
                dateFrom,
                dateTo
                );
    }
}
