package com.hrd.records.department;

import com.hrd.records.audit.AuditAction;
import com.hrd.records.audit.AuditService;
import com.hrd.records.common.ConflictException;
import com.hrd.records.common.NotFoundException;
import com.hrd.records.employee.EmployeeRepository;
import com.hrd.records.production.ProductionRecordRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Department administration. Codes are unique; every mutation is audited, and
 * departments still holding employees or production records cannot be deleted.
 */
@Service
public class DepartmentService {

    private final DepartmentRepository repository;
    private final EmployeeRepository employeeRepository;
    private final ProductionRecordRepository productionRecordRepository;
    private final AuditService auditService;

    public DepartmentService(DepartmentRepository repository,
                             EmployeeRepository employeeRepository,
                             ProductionRecordRepository productionRecordRepository,
                             AuditService auditService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.productionRecordRepository = productionRecordRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentSummary> list() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "code"))
                .stream().map(DepartmentDtos.DepartmentSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDtos.DepartmentSummary get(Long id) {
        return DepartmentDtos.DepartmentSummary.of(requireDepartment(id));
    }

    @Transactional
    public DepartmentDtos.DepartmentSummary create(DepartmentDtos.DepartmentRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new ConflictException("Department code '" + request.code() + "' is already in use");
        }
        Department saved = repository.save(new Department(request.code(), request.name(), request.type()));
        DepartmentDtos.DepartmentSummary summary = DepartmentDtos.DepartmentSummary.of(saved);
        auditService.record(AuditAction.CREATE, "Department", String.valueOf(saved.getId()), null, summary);
        return summary;
    }

    @Transactional
    public DepartmentDtos.DepartmentSummary update(Long id, DepartmentDtos.DepartmentRequest request) {
        Department department = requireDepartment(id);
        DepartmentDtos.DepartmentSummary before = DepartmentDtos.DepartmentSummary.of(department);
        if (!department.getCode().equals(request.code()) && repository.existsByCode(request.code())) {
            throw new ConflictException("Department code '" + request.code() + "' is already in use");
        }
        department.setCode(request.code());
        department.setName(request.name());
        department.setType(request.type());
        DepartmentDtos.DepartmentSummary after = DepartmentDtos.DepartmentSummary.of(department);
        auditService.record(AuditAction.UPDATE, "Department", String.valueOf(department.getId()), before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        Department department = requireDepartment(id);
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new ConflictException("Department '" + department.getCode() + "' still has employees assigned");
        }
        if (productionRecordRepository.existsByDepartmentId(id)) {
            throw new ConflictException("Department '" + department.getCode() + "' still has production records");
        }
        auditService.record(AuditAction.DELETE, "Department", String.valueOf(department.getId()),
                DepartmentDtos.DepartmentSummary.of(department), null);
        repository.delete(department);
    }

    private Department requireDepartment(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department " + id + " not found"));
    }
}
