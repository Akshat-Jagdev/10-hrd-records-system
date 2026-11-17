package com.hrd.records.dashboard;

import com.hrd.records.audit.AuditDtos;
import com.hrd.records.audit.AuditRepository;
import com.hrd.records.department.DepartmentRepository;
import com.hrd.records.department.DepartmentType;
import com.hrd.records.employee.EmployeeRepository;
import com.hrd.records.employee.EmployeeStatus;
import com.hrd.records.incentive.IncentiveEarningRepository;
import com.hrd.records.incentive.IncentiveStatus;
import com.hrd.records.production.ProductionRecord;
import com.hrd.records.production.ProductionRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only aggregation for the dashboard: headcount and department counters,
 * the pending incentive workload, the trailing 12 production periods and the
 * latest audit activity. This module owns no data of its own, it only reads
 * the other modules' repositories, so nothing here is audited.
 */
@Service
public class DashboardService {

    private static final int TREND_PERIODS = 12;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductionRecordRepository productionRecordRepository;
    private final IncentiveEarningRepository incentiveEarningRepository;
    private final AuditRepository auditRepository;

    public DashboardService(EmployeeRepository employeeRepository,
                            DepartmentRepository departmentRepository,
                            ProductionRecordRepository productionRecordRepository,
                            IncentiveEarningRepository incentiveEarningRepository,
                            AuditRepository auditRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.productionRecordRepository = productionRecordRepository;
        this.incentiveEarningRepository = incentiveEarningRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardSummary summary() {
        return new DashboardDtos.DashboardSummary(
                employeeRepository.countByStatus(EmployeeStatus.ACTIVE),
                employeeRepository.count(),
                departmentRepository.count(),
                departmentRepository.findAll().stream()
                        .filter(department -> department.getType() == DepartmentType.PRODUCTION)
                        .count(),
                incentiveEarningRepository.countByStatus(IncentiveStatus.DRAFT),
                productionTrend(),
                auditRepository.findTop5ByOrderByOccurredAtDesc().stream()
                        .map(AuditDtos.AuditEntry::of)
                        .toList());
    }

    /**
     * Groups every production record by calendar month (summing planned and
     * actual tonnes across departments), ordered oldest to newest and truncated
     * to the last 12 periods.
     */
    private List<DashboardDtos.MonthlyProduction> productionTrend() {
        Map<Period, List<ProductionRecord>> byPeriod = productionRecordRepository.findAll().stream()
                .collect(Collectors.groupingBy(record -> new Period(record.getYear(), record.getMonth())));
        List<DashboardDtos.MonthlyProduction> monthly = byPeriod.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toMonthlyProduction(entry.getKey(), entry.getValue()))
                .toList();
        return monthly.size() <= TREND_PERIODS
                ? monthly
                : List.copyOf(monthly.subList(monthly.size() - TREND_PERIODS, monthly.size()));
    }

    private DashboardDtos.MonthlyProduction toMonthlyProduction(Period period, List<ProductionRecord> records) {
        BigDecimal planned = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        for (ProductionRecord record : records) {
            planned = planned.add(record.getPlannedTonnes());
            actual = actual.add(record.getActualTonnes());
        }
        BigDecimal achievementPct = planned.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : actual.multiply(HUNDRED).divide(planned, 2, RoundingMode.HALF_UP);
        return new DashboardDtos.MonthlyProduction(period.year(), period.month(), planned, actual, achievementPct);
    }

    /** Sortable (year, month) grouping key. */
    private record Period(int year, int month) implements Comparable<Period> {

        @Override
        public int compareTo(Period other) {
            int byYear = Integer.compare(year, other.year);
            return byYear != 0 ? byYear : Integer.compare(month, other.month);
        }
    }
}
