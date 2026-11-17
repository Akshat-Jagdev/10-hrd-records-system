package com.hrd.records.incentive;

import com.hrd.records.audit.AuditAction;
import com.hrd.records.audit.AuditService;
import com.hrd.records.common.ConflictException;
import com.hrd.records.common.NotFoundException;
import com.hrd.records.common.PageResponse;
import com.hrd.records.department.Department;
import com.hrd.records.department.DepartmentRepository;
import com.hrd.records.employee.Employee;
import com.hrd.records.employee.EmployeeRepository;
import com.hrd.records.production.ProductionRecord;
import com.hrd.records.production.ProductionRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Incentive scheme administration and the monthly incentive calculation.
 * Scheme mutations are audited with before/after snapshots; a calculation
 * replaces the scheme/department/period's DRAFT rows (decided rows are kept)
 * and audits the run summary. Earnings can only be decided while DRAFT.
 */
@Service
public class IncentiveService {

    private static final int MAX_PAGE_SIZE = 100;

    private final IncentiveSchemeRepository schemeRepository;
    private final IncentiveEarningRepository earningRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductionRecordRepository productionRecordRepository;
    private final AuditService auditService;

    public IncentiveService(IncentiveSchemeRepository schemeRepository,
                            IncentiveEarningRepository earningRepository,
                            EmployeeRepository employeeRepository,
                            DepartmentRepository departmentRepository,
                            ProductionRecordRepository productionRecordRepository,
                            AuditService auditService) {
        this.schemeRepository = schemeRepository;
        this.earningRepository = earningRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.productionRecordRepository = productionRecordRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<IncentiveDtos.SchemeSummary> listSchemes() {
        return schemeRepository.findAllByOrderByNameAsc().stream().map(IncentiveDtos.SchemeSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public IncentiveDtos.SchemeSummary getScheme(Long id) {
        return IncentiveDtos.SchemeSummary.of(requireScheme(id));
    }

    @Transactional
    public IncentiveDtos.SchemeSummary createScheme(IncentiveDtos.SchemeRequest request) {
        if (schemeRepository.existsByName(request.name())) {
            throw new ConflictException("An incentive scheme named '" + request.name() + "' already exists");
        }
        validateSchemeRequest(request);
        IncentiveScheme scheme = new IncentiveScheme(request.name(), request.description(),
                request.thresholdPct(), request.ceilingPct(), request.ratePerPoint(), request.active(),
                request.effectiveFrom(), request.effectiveTo());
        applyGradeRates(scheme, request);
        IncentiveScheme saved = schemeRepository.save(scheme);
        IncentiveDtos.SchemeSummary summary = IncentiveDtos.SchemeSummary.of(saved);
        auditService.record(AuditAction.CREATE, "IncentiveScheme", String.valueOf(saved.getId()), null, summary);
        return summary;
    }

    @Transactional
    public IncentiveDtos.SchemeSummary updateScheme(Long id, IncentiveDtos.SchemeRequest request) {
        IncentiveScheme scheme = requireScheme(id);
        if (!scheme.getName().equals(request.name()) && schemeRepository.existsByName(request.name())) {
            throw new ConflictException("An incentive scheme named '" + request.name() + "' already exists");
        }
        validateSchemeRequest(request);
        IncentiveDtos.SchemeSummary before = IncentiveDtos.SchemeSummary.of(scheme);
        scheme.setName(request.name());
        scheme.setDescription(request.description());
        scheme.setThresholdPct(request.thresholdPct());
        scheme.setCeilingPct(request.ceilingPct());
        scheme.setRatePerPoint(request.ratePerPoint());
        scheme.setActive(request.active());
        scheme.setEffectiveFrom(request.effectiveFrom());
        scheme.setEffectiveTo(request.effectiveTo());
        replaceGradeRates(scheme, request);
        IncentiveDtos.SchemeSummary after = IncentiveDtos.SchemeSummary.of(scheme);
        auditService.record(AuditAction.UPDATE, "IncentiveScheme", String.valueOf(scheme.getId()), before, after);
        return after;
    }

    @Transactional
    public void deleteScheme(Long id) {
        IncentiveScheme scheme = requireScheme(id);
        if (earningRepository.existsBySchemeId(id)) {
            throw new ConflictException("Scheme '" + scheme.getName() + "' has earnings and cannot be deleted");
        }
        auditService.record(AuditAction.DELETE, "IncentiveScheme", String.valueOf(scheme.getId()),
                IncentiveDtos.SchemeSummary.of(scheme), null);
        schemeRepository.delete(scheme);
    }

    /**
     * Runs one scheme over one department and period: clears that
     * scheme/department/period's DRAFT rows, then recreates a DRAFT earning
     * for every eligible employee of the department. APPROVED/REJECTED rows
     * are never touched. Returns the run summary, which is also audited.
     */
    @Transactional
    public IncentiveDtos.CalculationResult calculate(IncentiveDtos.CalculateRequest request) {
        IncentiveScheme scheme = requireScheme(request.schemeId());
        Department department = requireDepartment(request.departmentId());

        YearMonth period = YearMonth.of(request.year(), request.month());
        boolean applies = scheme.isActive()
                && !scheme.getEffectiveFrom().isAfter(period.atEndOfMonth())
                && (scheme.getEffectiveTo() == null || !scheme.getEffectiveTo().isBefore(period.atDay(1)));
        if (!applies) {
            throw new ConflictException("Scheme is not active for that period");
        }

        ProductionRecord record = productionRecordRepository
                .findByDepartmentIdAndYearAndMonth(department.getId(), request.year(), request.month())
                .orElseThrow(() -> new NotFoundException(
                        "No production record exists for this department, year and month"));
        BigDecimal achievementPct = IncentiveCalculator.achievementPct(record.getPlannedTonnes(),
                record.getActualTonnes());

        earningRepository.deleteDraftBySchemeAndDepartmentAndPeriod(scheme.getId(), department.getId(),
                request.year(), request.month());

        List<Employee> eligible = employeeRepository.findByDepartmentId(department.getId()).stream()
                .filter(employee -> IncentiveCalculator.isEligible(employee, request.year(), request.month()))
                .toList();

        BigDecimal totalDraftAmount = BigDecimal.ZERO;
        for (Employee employee : eligible) {
            BigDecimal amount = IncentiveCalculator.amount(achievementPct, scheme.getThresholdPct(),
                    scheme.getCeilingPct(), scheme.getRatePerPoint(),
                    gradeFactorFor(scheme, employee.getGrade()));
            earningRepository.save(new IncentiveEarning(employee, scheme, request.year(), request.month(),
                    achievementPct, amount, IncentiveStatus.DRAFT));
            totalDraftAmount = totalDraftAmount.add(amount);
        }

        IncentiveDtos.CalculationResult result = new IncentiveDtos.CalculationResult(scheme.getId(),
                scheme.getName(), department.getId(), request.year(), request.month(), achievementPct,
                eligible.size(), totalDraftAmount);
        auditService.record(AuditAction.CREATE, "IncentiveEarning", String.valueOf(scheme.getId()), null, result);
        return result;
    }

    @Transactional
    public IncentiveDtos.EarningSummary approve(Long id) {
        return decide(id, IncentiveStatus.APPROVED, "approved");
    }

    @Transactional
    public IncentiveDtos.EarningSummary reject(Long id) {
        return decide(id, IncentiveStatus.REJECTED, "rejected");
    }

    /**
     * Paged earning search. All filters are optional and composable; with no
     * filters every earning is returned, newest period first, then by employee
     * number.
     */
    @Transactional(readOnly = true)
    public PageResponse<IncentiveDtos.EarningSummary> searchEarnings(Long employeeId, Long schemeId, Integer year,
                                                                     Integer month, IncentiveStatus status,
                                                                     int page, int size) {
        Specification<IncentiveEarning> spec = Specification.where(null);
        if (employeeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (schemeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("scheme").get("id"), schemeId));
        }
        if (year != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("year"), year));
        }
        if (month != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("month"), month));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort sort = Sort.by(Sort.Direction.DESC, "year")
                .and(Sort.by(Sort.Direction.DESC, "month"))
                .and(Sort.by(Sort.Direction.ASC, "employee.empNo"));
        PageRequest pageable = PageRequest.of(safePage, safeSize, sort);
        return PageResponse.of(earningRepository.findAll(spec, pageable).map(IncentiveDtos.EarningSummary::of));
    }

    /** Applies the target status to a DRAFT earning and audits the decision. */
    private IncentiveDtos.EarningSummary decide(Long id, IncentiveStatus target, String verb) {
        IncentiveEarning earning = requireEarning(id);
        if (earning.getStatus() != IncentiveStatus.DRAFT) {
            throw new ConflictException("Only DRAFT earnings can be " + verb);
        }
        IncentiveDtos.EarningSummary before = IncentiveDtos.EarningSummary.of(earning);
        earning.setStatus(target);
        earning.setDecidedBy(currentUsername());
        earning.setDecidedAt(Instant.now());
        IncentiveDtos.EarningSummary after = IncentiveDtos.EarningSummary.of(earning);
        auditService.record(AuditAction.UPDATE, "IncentiveEarning", String.valueOf(earning.getId()), before, after);
        return after;
    }

    /** Rejects ceilings below thresholds and grades listed more than once (the pair is unique in the database). */
    private void validateSchemeRequest(IncentiveDtos.SchemeRequest request) {
        if (request.ceilingPct().compareTo(request.thresholdPct()) < 0) {
            throw new ConflictException("Ceiling must be greater than or equal to threshold");
        }
        Set<String> grades = new HashSet<>();
        for (IncentiveDtos.GradeRateRequest gradeRate : request.gradeRates()) {
            if (!grades.add(gradeRate.grade())) {
                throw new ConflictException("Grade '" + gradeRate.grade() + "' appears more than once in the scheme");
            }
        }
    }

    private void applyGradeRates(IncentiveScheme scheme, IncentiveDtos.SchemeRequest request) {
        for (IncentiveDtos.GradeRateRequest gradeRate : request.gradeRates()) {
            scheme.addGradeRate(new SchemeGradeRate(gradeRate.grade(), gradeRate.factor()));
        }
    }

    /**
     * Diffs the request against the scheme's grade rates: kept grades are
     * updated in place, dropped grades are removed and new grades added. A
     * delete-all/re-insert strategy would break the unique (scheme, grade)
     * constraint because Hibernate flushes inserts before orphan deletes.
     */
    private void replaceGradeRates(IncentiveScheme scheme, IncentiveDtos.SchemeRequest request) {
        Set<String> requestedGrades = new HashSet<>();
        for (IncentiveDtos.GradeRateRequest gradeRate : request.gradeRates()) {
            requestedGrades.add(gradeRate.grade());
        }
        for (SchemeGradeRate existing : new ArrayList<>(scheme.getGradeRates())) {
            if (!requestedGrades.contains(existing.getGrade())) {
                scheme.removeGradeRate(existing);
            }
        }
        for (IncentiveDtos.GradeRateRequest gradeRate : request.gradeRates()) {
            scheme.getGradeRates().stream()
                    .filter(existing -> existing.getGrade().equals(gradeRate.grade()))
                    .findFirst()
                    .ifPresentOrElse(existing -> existing.setFactor(gradeRate.factor()),
                            () -> scheme.addGradeRate(new SchemeGradeRate(gradeRate.grade(), gradeRate.factor())));
        }
    }

    /** Grade multiplier configured on the scheme; grades without a row are paid at factor 1. */
    private BigDecimal gradeFactorFor(IncentiveScheme scheme, String grade) {
        return scheme.getGradeRates().stream()
                .filter(rate -> rate.getGrade().equals(grade))
                .findFirst()
                .map(SchemeGradeRate::getFactor)
                .orElse(BigDecimal.ONE);
    }

    private IncentiveScheme requireScheme(Long id) {
        return schemeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incentive scheme " + id + " not found"));
    }

    private IncentiveEarning requireEarning(Long id) {
        return earningRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incentive earning " + id + " not found"));
    }

    private Department requireDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department " + id + " not found"));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
