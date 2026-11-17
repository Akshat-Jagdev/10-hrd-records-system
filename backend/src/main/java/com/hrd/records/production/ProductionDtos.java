package com.hrd.records.production;

import com.hrd.records.department.DepartmentDtos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Request/response payloads for monthly production figures. */
public final class ProductionDtos {

    private ProductionDtos() {
    }

    /**
     * One department's figure for one month. achievementPct is derived on read:
     * actual * 100 / planned at scale 2 HALF_UP, ZERO when planned is null or zero.
     */
    public record ProductionSummary(Long id, DepartmentDtos.DepartmentSummary department, int year, int month,
                                    BigDecimal plannedTonnes, BigDecimal actualTonnes, BigDecimal achievementPct) {

        public static ProductionSummary of(ProductionRecord record) {
            return new ProductionSummary(record.getId(),
                    DepartmentDtos.DepartmentSummary.of(record.getDepartment()),
                    record.getYear(), record.getMonth(),
                    record.getPlannedTonnes(), record.getActualTonnes(),
                    achievementPct(record.getPlannedTonnes(), record.getActualTonnes()));
        }

        /** actual * 100 / planned at scale 2 HALF_UP; ZERO when planned is null or zero. */
        private static BigDecimal achievementPct(BigDecimal planned, BigDecimal actual) {
            if (planned == null || planned.signum() == 0) {
                return BigDecimal.ZERO;
            }
            return actual.multiply(BigDecimal.valueOf(100)).divide(planned, 2, RoundingMode.HALF_UP);
        }
    }

    public record ProductionRecordRequest(
            @NotNull Long departmentId,
            @NotNull @Min(2000) @Max(2100) Integer year,
            @NotNull @Min(1) @Max(12) Integer month,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal plannedTonnes,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal actualTonnes) {
    }
}
