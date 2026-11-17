package com.hrd.records.production;

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

/**
 * Monthly production figures. Reads are open to any authenticated user;
 * writes require ADMIN or HR_MANAGER.
 */
@RestController
@RequestMapping("/api/v1/production")
@Tag(name = "Production")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping
    @Operation(summary = "Search production records (latest period first)")
    public PageResponse<ProductionDtos.ProductionSummary> search(
            @Parameter(description = "Filter by department id") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "Filter by year, e.g. 2026") @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by month, 1-12") @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productionService.search(departmentId, year, month, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single production record")
    public ProductionDtos.ProductionSummary get(@PathVariable Long id) {
        return productionService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Record a department's production for a month")
    public ResponseEntity<ProductionDtos.ProductionSummary> create(
            @Valid @RequestBody ProductionDtos.ProductionRecordRequest request) {
        ProductionDtos.ProductionSummary created = productionService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/production/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Update a production record")
    public ProductionDtos.ProductionSummary update(@PathVariable Long id,
            @Valid @RequestBody ProductionDtos.ProductionRecordRequest request) {
        return productionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a production record")
    public void delete(@PathVariable Long id) {
        productionService.delete(id);
    }
}
