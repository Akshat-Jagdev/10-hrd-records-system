package com.hrd.records.incentive;

import com.hrd.records.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Incentive schemes and incentive earnings. Reads are open to any
 * authenticated user; writes (scheme changes, recalculation, approve/reject)
 * are restricted to ADMIN and HR_MANAGER.
 */
@RestController
@RequestMapping("/api/v1/incentives")
@Tag(name = "Incentives")
public class IncentiveController {

    private final IncentiveService incentiveService;

    public IncentiveController(IncentiveService incentiveService) {
        this.incentiveService = incentiveService;
    }

    @GetMapping("/schemes")
    @Operation(summary = "List all incentive schemes (ordered by name)")
    public List<IncentiveDtos.SchemeSummary> listSchemes() {
        return incentiveService.listSchemes();
    }

    @GetMapping("/schemes/{id}")
    @Operation(summary = "Get a single incentive scheme")
    public IncentiveDtos.SchemeSummary getScheme(@PathVariable Long id) {
        return incentiveService.getScheme(id);
    }

    @PostMapping("/schemes")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Create an incentive scheme")
    public ResponseEntity<IncentiveDtos.SchemeSummary> createScheme(
            @Valid @RequestBody IncentiveDtos.SchemeRequest request) {
        IncentiveDtos.SchemeSummary created = incentiveService.createScheme(request);
        return ResponseEntity.created(URI.create("/api/v1/incentives/schemes/" + created.id())).body(created);
    }

    @PutMapping("/schemes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Update an incentive scheme")
    public IncentiveDtos.SchemeSummary updateScheme(@PathVariable Long id,
                                                    @Valid @RequestBody IncentiveDtos.SchemeRequest request) {
        return incentiveService.updateScheme(id, request);
    }

    @DeleteMapping("/schemes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an incentive scheme (blocked while earnings exist)")
    public void deleteScheme(@PathVariable Long id) {
        incentiveService.deleteScheme(id);
    }

    @GetMapping("/earnings")
    @Operation(summary = "Search incentive earnings (composable filters, latest period first)")
    public PageResponse<IncentiveDtos.EarningSummary> searchEarnings(
            @Parameter(description = "Exact employee id") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Exact scheme id") @RequestParam(required = false) Long schemeId,
            @Parameter(description = "Period year, e.g. 2026") @RequestParam(required = false) Integer year,
            @Parameter(description = "Period month (1-12)") @RequestParam(required = false) Integer month,
            @Parameter(description = "DRAFT, APPROVED or REJECTED") @RequestParam(required = false) IncentiveStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return incentiveService.searchEarnings(employeeId, schemeId, year, month, status, page, size);
    }

    @PostMapping("/earnings/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Recalculate DRAFT earnings for a scheme, department and period")
    public IncentiveDtos.CalculationResult calculate(@Valid @RequestBody IncentiveDtos.CalculateRequest request) {
        return incentiveService.calculate(request);
    }

    @PostMapping("/earnings/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Approve a DRAFT incentive earning")
    public IncentiveDtos.EarningSummary approve(@PathVariable Long id) {
        return incentiveService.approve(id);
    }

    @PostMapping("/earnings/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Reject a DRAFT incentive earning")
    public IncentiveDtos.EarningSummary reject(@PathVariable Long id) {
        return incentiveService.reject(id);
    }
}
