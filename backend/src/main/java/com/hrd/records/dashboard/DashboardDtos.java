package com.hrd.records.dashboard;

import com.hrd.records.audit.AuditDtos;

import java.math.BigDecimal;
import java.util.List;

/** Response payloads for the dashboard aggregation endpoint. */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** Headline counters plus the trailing 12 production periods and the 5 most recent audit entries. */
    public record DashboardSummary(long activeEmployees, long totalEmployees, long departments,
                                   long productionDepartments, long pendingIncentives,
                                   List<MonthlyProduction> productionTrend,
                                   List<AuditDtos.AuditEntry> recentAudit) {
    }

    /** One calendar month of production summed across all production departments. */
    public record MonthlyProduction(int year, int month, BigDecimal plannedTonnes, BigDecimal actualTonnes,
                                    BigDecimal achievementPct) {
    }
}
