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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

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

    public PageResponse<AuditLogResponse> getAuditLogsForAdmin(
            int page,
            int size,
            String action,
            String actorEmail,
            String targetType,
            Long targetId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
            ){
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                );

        Specification<AuditLog> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action!=null && !action.isBlank()){
                predicates.add(
                        criteriaBuilder.equal(root.get("action"), action)
                );
            }
            if (actorEmail != null && !actorEmail.isBlank()){
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("actorEmail")),
                                "%" + actorEmail.toLowerCase() + "%"
                        )
                );
            }

            if (targetType != null && !targetType.isBlank()){
                predicates.add(
                        criteriaBuilder.equal(root.get("targetType"), targetType)
                );
            }

            if (targetId != null){
                predicates.add(
                        criteriaBuilder.equal(root.get("targetId"), targetId)
                );
            }
            if (dateFrom != null){
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), dateFrom)
                );
            }
            if (dateTo != null){
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), dateTo)
                );
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        Page<AuditLogResponse> auditLogsPage = auditLogRepository.findAll(specification, pageable)
                .map(this::mapToAuditLogResponse);

        return PageResponse.<AuditLogResponse>builder()
                .content(auditLogsPage.getContent())
                .page(auditLogsPage.getNumber())
                .size(auditLogsPage.getSize())
                .totalElements(auditLogsPage.getTotalElements())
                .totalPages(auditLogsPage.getTotalPages())
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
