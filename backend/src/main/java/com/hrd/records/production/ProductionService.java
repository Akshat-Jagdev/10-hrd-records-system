package com.hrd.records.production;

import com.hrd.records.audit.AuditAction;
import com.hrd.records.audit.AuditService;
import com.hrd.records.common.ConflictException;
import com.hrd.records.common.NotFoundException;
import com.hrd.records.common.PageResponse;
import com.hrd.records.department.Department;
import com.hrd.records.department.DepartmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monthly production figures. The (department, year, month) period is unique,
 * and every mutation is written to the audit trail with before/after snapshots.
 */
@Service
public class ProductionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductionRecordRepository repository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public ProductionService(ProductionRecordRepository repository,
                             DepartmentRepository departmentRepository,
                             AuditService auditService) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    /**
     * Filtered, paged listing. Filters are composable and implemented with a
     * JPA Specification (never `:param is null` tricks, which break on
     * PostgreSQL/Hibernate parameter typing).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductionDtos.ProductionSummary> search(Long departmentId, Integer year, Integer month,
                                                                 int page, int size) {
        Specification<ProductionRecord> spec = Specification.where(null);
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (year != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("year"), year));
        }
        if (month != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("month"), month));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(
                Sort.Order.desc("year"), Sort.Order.desc("month"), Sort.Order.asc("department.code")));
        return PageResponse.of(repository.findAll(spec, pageable).map(ProductionDtos.ProductionSummary::of));
    }

    @Transactional(readOnly = true)
    public ProductionDtos.ProductionSummary get(Long id) {
        return ProductionDtos.ProductionSummary.of(requireRecord(id));
    }

    @Transactional
    public ProductionDtos.ProductionSummary create(ProductionDtos.ProductionRecordRequest request) {
        Department department = requireDepartment(request.departmentId());
        repository.findByDepartmentIdAndYearAndMonth(department.getId(), request.year(), request.month())
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "A production record already exists for this department, year and month");
                });

        ProductionRecord saved = repository.save(new ProductionRecord(department, request.year(), request.month(),
                request.plannedTonnes(), request.actualTonnes()));
        ProductionDtos.ProductionSummary summary = ProductionDtos.ProductionSummary.of(saved);
        auditService.record(AuditAction.CREATE, "ProductionRecord", String.valueOf(saved.getId()), null, summary);
        return summary;
    }

    @Transactional
    public ProductionDtos.ProductionSummary update(Long id, ProductionDtos.ProductionRecordRequest request) {
        ProductionRecord record = requireRecord(id);
        Department department = requireDepartment(request.departmentId());
        repository.findByDepartmentIdAndYearAndMonth(department.getId(), request.year(), request.month())
                .filter(existing -> !existing.getId().equals(record.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "A production record already exists for this department, year and month");
                });

        ProductionDtos.ProductionSummary before = ProductionDtos.ProductionSummary.of(record);
        record.setDepartment(department);
        record.setYear(request.year());
        record.setMonth(request.month());
        record.setPlannedTonnes(request.plannedTonnes());
        record.setActualTonnes(request.actualTonnes());
        ProductionDtos.ProductionSummary after = ProductionDtos.ProductionSummary.of(record);
        auditService.record(AuditAction.UPDATE, "ProductionRecord", String.valueOf(record.getId()), before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        ProductionRecord record = requireRecord(id);
        auditService.record(AuditAction.DELETE, "ProductionRecord", String.valueOf(record.getId()),
                ProductionDtos.ProductionSummary.of(record), null);
        repository.delete(record);
    }

    private ProductionRecord requireRecord(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Production record " + id + " not found"));
    }

    private Department requireDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department " + id + " not found"));
    }
}
