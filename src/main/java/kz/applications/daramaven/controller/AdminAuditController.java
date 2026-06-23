package kz.applications.daramaven.controller;

import kz.applications.daramaven.dto.AuditLogResponse;
import kz.applications.daramaven.dto.PageResponse;
import kz.applications.daramaven.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {
    private final AuditLogService auditLogService;

    @GetMapping
    public PageResponse<AuditLogResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return auditLogService.getAuditLogsForAdmin(page, size);
    }
}
