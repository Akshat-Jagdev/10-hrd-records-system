package com.hrd.records.department;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Department reference data - readable by any authenticated user, writable by ADMIN/HR_MANAGER. */
@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(summary = "List all departments ordered by code")
    public List<DepartmentDtos.DepartmentSummary> list() {
        return departmentService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single department")
    public DepartmentDtos.DepartmentSummary get(@PathVariable Long id) {
        return departmentService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Create a department")
    public ResponseEntity<DepartmentDtos.DepartmentSummary> create(
            @Valid @RequestBody DepartmentDtos.DepartmentRequest request) {
        DepartmentDtos.DepartmentSummary created = departmentService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/departments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Update a department's code, name or type")
    public DepartmentDtos.DepartmentSummary update(@PathVariable Long id,
                                                   @Valid @RequestBody DepartmentDtos.DepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a department (blocked while employees or production records reference it)")
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
