package com.hrd.records.employee;

import com.hrd.records.audit.AuditAction;
import com.hrd.records.audit.AuditService;
import com.hrd.records.common.ConflictException;
import com.hrd.records.common.NotFoundException;
import com.hrd.records.common.PageResponse;
import com.hrd.records.department.Department;
import com.hrd.records.department.DepartmentRepository;
import com.hrd.records.incentive.IncentiveEarningRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Employee master data. Listings are filtered with a JPA Specification
 * (never `:param is null` tricks, which break on PostgreSQL/Hibernate
 * parameter typing), every mutation is written to the audit trail, and
 * employees with incentive history cannot be deleted.
 */
@Service
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeRepository repository;
    private final DepartmentRepository departmentRepository;
    private final IncentiveEarningRepository incentiveEarningRepository;
    private final AuditService auditService;

    public EmployeeService(EmployeeRepository repository,
                           DepartmentRepository departmentRepository,
                           IncentiveEarningRepository incentiveEarningRepository,
                           AuditService auditService) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
        this.incentiveEarningRepository = incentiveEarningRepository;
        this.auditService = auditService;
    }

    /**
     * Paged employee search. All filters are optional and composable; with no
     * filters supplied every employee is returned, ordered by empNo ascending.
     */
    @Transactional(readOnly = true)
    public PageResponse<EmployeeDtos.EmployeeSummary> search(String empNo, String name, Long departmentId,
                                                             EmployeeStatus status, int page, int size) {
        Specification<Employee> spec = Specification.where(null);
        if (empNo != null && !empNo.isBlank()) {
            String needle = "%" + empNo.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("empNo")), needle));
        }
        if (name != null && !name.isBlank()) {
            String needle = "%" + name.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("fullName")), needle));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "empNo"));
        return PageResponse.of(repository.findAll(spec, pageable).map(EmployeeDtos.EmployeeSummary::of));
    }

    @Transactional(readOnly = true)
    public EmployeeDtos.EmployeeSummary get(Long id) {
        return EmployeeDtos.EmployeeSummary.of(requireEmployee(id));
    }

    @Transactional
    public EmployeeDtos.EmployeeSummary create(EmployeeDtos.EmployeeRequest request) {
        if (repository.existsByEmpNo(request.empNo())) {
            throw new ConflictException("Employee number '" + request.empNo() + "' is already in use");
        }
        Department department = requireDepartment(request.departmentId());
        Employee employee = new Employee(request.empNo(), request.fullName(), request.grade(),
                request.designation(), department, request.doj(), request.status(), request.basicPay());
        Employee saved = repository.save(employee);
        EmployeeDtos.EmployeeSummary summary = EmployeeDtos.EmployeeSummary.of(saved);
        auditService.record(AuditAction.CREATE, "Employee", String.valueOf(saved.getId()), null, summary);
        return summary;
    }

    @Transactional
    public EmployeeDtos.EmployeeSummary update(Long id, EmployeeDtos.EmployeeRequest request) {
        Employee employee = requireEmployee(id);
        EmployeeDtos.EmployeeSummary before = EmployeeDtos.EmployeeSummary.of(employee);
        if (!employee.getEmpNo().equals(request.empNo()) && repository.existsByEmpNo(request.empNo())) {
            throw new ConflictException("Employee number '" + request.empNo() + "' is already in use");
        }
        employee.setEmpNo(request.empNo());
        employee.setFullName(request.fullName());
        employee.setGrade(request.grade());
        employee.setDesignation(request.designation());
        employee.setDepartment(requireDepartment(request.departmentId()));
        employee.setDoj(request.doj());
        employee.setStatus(request.status());
        employee.setBasicPay(request.basicPay());
        EmployeeDtos.EmployeeSummary after = EmployeeDtos.EmployeeSummary.of(employee);
        auditService.record(AuditAction.UPDATE, "Employee", String.valueOf(employee.getId()), before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = requireEmployee(id);
        if (incentiveEarningRepository.existsByEmployeeId(id)) {
            throw new ConflictException("Employee '" + employee.getEmpNo()
                    + "' has incentive records and cannot be deleted");
        }
        auditService.record(AuditAction.DELETE, "Employee", String.valueOf(employee.getId()),
                EmployeeDtos.EmployeeSummary.of(employee), null);
        repository.delete(employee);
    }

    private Employee requireEmployee(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee " + id + " not found"));
    }

    private Department requireDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department " + id + " not found"));
    }
}
