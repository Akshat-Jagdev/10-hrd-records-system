package com.hrd.records.audit;

import com.hrd.records.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Read-only audit trail with optional filters. Filters are composable and
 * implemented with a JPA Specification (never `:param is null` tricks, which
 * break on PostgreSQL/Hibernate parameter typing).
 */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER','AUDITOR')")
@Tag(name = "Audit")
public class AuditController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Search the audit trail (latest first)")
    public PageResponse<AuditDtos.AuditEntry> search(
            @Parameter(description = "Entity name, e.g. Employee") @RequestParam(required = false) String entity,
            @RequestParam(required = false) String username,
            @Parameter(description = "CREATE, UPDATE or DELETE") @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuditAction parsedAction = action == null || action.isBlank() ? null
                : AuditAction.valueOf(action.trim().toUpperCase());

        Specification<AuditLog> spec = Specification.where(null);
        if (entity != null && !entity.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("entity")), entity.trim().toLowerCase()));
        }
        if (username != null && !username.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("username")), username.trim().toLowerCase()));
        }
        if (parsedAction != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), parsedAction));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt", "id"));
        Page<AuditDtos.AuditEntry> result = repository.findAll(spec, pageable).map(AuditDtos.AuditEntry::of);
        return PageResponse.of(result);
    }
}
