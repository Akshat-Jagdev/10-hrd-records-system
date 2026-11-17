package com.hrd.records.incentive;

import com.hrd.records.employee.EmployeeDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response payloads for incentive schemes and incentive earnings. */
public final class IncentiveDtos {

    private IncentiveDtos() {
    }

    /** One grade multiplier of a scheme, as returned in {@link SchemeSummary}. */
    public record GradeRate(String grade, BigDecimal factor) {
    }

    public record SchemeSummary(Long id, String name, String description, BigDecimal thresholdPct,
                                BigDecimal ceilingPct, BigDecimal ratePerPoint, boolean active,
                                LocalDate effectiveFrom, LocalDate effectiveTo, List<GradeRate> gradeRates) {

        public static SchemeSummary of(IncentiveScheme scheme) {
            List<GradeRate> gradeRates = scheme.getGradeRates().stream()
                    .map(rate -> new GradeRate(rate.getGrade(), rate.getFactor()))
                    .toList();
            return new SchemeSummary(scheme.getId(), scheme.getName(), scheme.getDescription(),
                    scheme.getThresholdPct(), scheme.getCeilingPct(), scheme.getRatePerPoint(), scheme.isActive(),
                    scheme.getEffectiveFrom(), scheme.getEffectiveTo(), gradeRates);
        }
    }

    public record GradeRateRequest(
            @NotBlank @Size(max = 10) String grade,
            @NotNull @DecimalMin("0.10") @Digits(integer = 2, fraction = 2) BigDecimal factor) {
    }

    public record SchemeRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 3, fraction = 2) BigDecimal thresholdPct,
            @NotNull @Digits(integer = 3, fraction = 2) BigDecimal ceilingPct,
            @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal ratePerPoint,
            @NotNull boolean active,
            @NotNull LocalDate effectiveFrom,
            LocalDate effectiveTo,
            @Valid @NotNull List<GradeRateRequest> gradeRates) {
    }

    public record CalculateRequest(
            @NotNull Long schemeId,
            @NotNull Long departmentId,
            @NotNull @Min(2000) @Max(2100) Integer year,
            @NotNull @Min(1) @Max(12) Integer month) {
    }

    /** Summary of one calculation run; the created rows are DRAFT and replace any earlier DRAFT rows. */
    public record CalculationResult(Long schemeId, String schemeName, Long departmentId, int year, int month,
                                    BigDecimal achievementPct, int draftCount, BigDecimal totalDraftAmount) {
    }

    public record EarningSummary(Long id, EmployeeDtos.EmployeeSummary employee, Long schemeId, String schemeName,
                                 int year, int month, BigDecimal achievementPct, BigDecimal amount,
                                 IncentiveStatus status, String decidedBy, Instant decidedAt) {

        public static EarningSummary of(IncentiveEarning earning) {
            return new EarningSummary(earning.getId(), EmployeeDtos.EmployeeSummary.of(earning.getEmployee()),
                    earning.getScheme().getId(), earning.getScheme().getName(), earning.getYear(),
                    earning.getMonth(), earning.getAchievementPct(), earning.getAmount(), earning.getStatus(),
                    earning.getDecidedBy(), earning.getDecidedAt());
        }
    }
}
