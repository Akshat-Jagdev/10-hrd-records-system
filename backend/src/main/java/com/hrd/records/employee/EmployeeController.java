package com.hrd.records.employee;

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
 * Employee master data. Reads are open to any authenticated user; writes are
 * restricted to ADMIN and HR_MANAGER.
 */
@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "Search employees (composable filters, ordered by empNo)")
    public PageResponse<EmployeeDtos.EmployeeSummary> search(
            @Parameter(description = "Employee number, case-insensitive contains") @RequestParam(required = false) String empNo,
            @Parameter(description = "Full name, case-insensitive contains") @RequestParam(required = false) String name,
            @Parameter(description = "Exact department id") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "ACTIVE, RETIRED or TRANSFERRED") @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return employeeService.search(empNo, name, departmentId, status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single employee")
    public EmployeeDtos.EmployeeSummary get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Create an employee")
    public ResponseEntity<EmployeeDtos.EmployeeSummary> create(
            @Valid @RequestBody EmployeeDtos.EmployeeRequest request) {
        EmployeeDtos.EmployeeSummary created = employeeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @Operation(summary = "Update an employee")
    public EmployeeDtos.EmployeeSummary update(@PathVariable Long id,
                                               @Valid @RequestBody EmployeeDtos.EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an employee (blocked while incentive records exist)")
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }
}
