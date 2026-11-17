package com.hrd.records.incentive;

import com.hrd.records.employee.Employee;
import com.hrd.records.employee.EmployeeStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pure {@link IncentiveCalculator} arithmetic: achievement
 * percentage, per-employee amount and period eligibility.
 */
class IncentiveCalculatorTest {

    // ---- achievementPct ----

    @Test
    void achievementPctNormalCase() {
        assertThat(IncentiveCalculator.achievementPct(new BigDecimal("200"), new BigDecimal("150")))
                .isEqualTo(new BigDecimal("75.00"));
    }

    @Test
    void achievementPctZeroPlannedYieldsZero() {
        assertThat(IncentiveCalculator.achievementPct(BigDecimal.ZERO, new BigDecimal("150"))).isZero();
    }

    @Test
    void achievementPctNullPlannedYieldsZero() {
        assertThat(IncentiveCalculator.achievementPct(null, new BigDecimal("150"))).isZero();
    }

    @Test
    void achievementPctNullActualTreatedAsZero() {
        assertThat(IncentiveCalculator.achievementPct(new BigDecimal("200"), null)).isZero();
    }

    @Test
    void achievementPctRoundsHalfUpToTwoDecimals() {
        // 123.45 * 100 / 1000 = 12.345 -> 12.35 (HALF_UP)
        assertThat(IncentiveCalculator.achievementPct(new BigDecimal("1000"), new BigDecimal("123.45")))
                .isEqualTo(new BigDecimal("12.35"));
        // 1 * 100 / 6 = 16.666... -> 16.67
        assertThat(IncentiveCalculator.achievementPct(new BigDecimal("6"), BigDecimal.ONE))
                .isEqualTo(new BigDecimal("16.67"));
    }

    // ---- amount ----

    @Test
    void amountBelowThresholdIsZero() {
        BigDecimal amount = IncentiveCalculator.amount(new BigDecimal("50"), new BigDecimal("60"),
                new BigDecimal("120"), new BigDecimal("10"), BigDecimal.ONE);
        assertThat(amount).isZero();
    }

    @Test
    void amountExactlyAtThresholdIsZero() {
        BigDecimal amount = IncentiveCalculator.amount(new BigDecimal("60"), new BigDecimal("60"),
                new BigDecimal("120"), new BigDecimal("10"), BigDecimal.ONE);
        assertThat(amount).isZero();
    }

    @Test
    void amountAboveCeilingClampsToCeiling() {
        // achievement 150 capped at ceiling 120: (120 - 80) * 10 * 1 = 400.00
        BigDecimal amount = IncentiveCalculator.amount(new BigDecimal("150"), new BigDecimal("80"),
                new BigDecimal("120"), new BigDecimal("10"), BigDecimal.ONE);
        assertThat(amount).isEqualTo(new BigDecimal("400.00"));
    }

    @Test
    void amountNormalCaseRoundsToTwoDecimals() {
        // (110.555 - 90) = 20.555 points * 2.5 * 1.2 = 61.665 -> 61.67
        BigDecimal amount = IncentiveCalculator.amount(new BigDecimal("110.555"), new BigDecimal("90"),
                new BigDecimal("120"), new BigDecimal("2.5"), new BigDecimal("1.2"));
        assertThat(amount).isEqualTo(new BigDecimal("61.67"));
    }

    @Test
    void amountNegativeEffectivePointsIsZero() {
        // negative achievement below a zero threshold: effective points -10 -> 0
        BigDecimal amount = IncentiveCalculator.amount(new BigDecimal("-10"), BigDecimal.ZERO,
                new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ONE);
        assertThat(amount).isZero();
    }

    // ---- isEligible ----

    private Employee employee(LocalDate doj, EmployeeStatus status) {
        return new Employee("E001", "Test Employee", "A", "Operator", null, doj, status,
                new BigDecimal("1000"));
    }

    @Test
    void eligibleWhenActiveAndJoinedBeforeMonthEnd() {
        assertThat(IncentiveCalculator.isEligible(employee(LocalDate.of(2026, 1, 15), EmployeeStatus.ACTIVE),
                2026, 1)).isTrue();
    }

    @Test
    void eligibleWhenDojIsExactlyTheLastDayOfTheMonth() {
        assertThat(IncentiveCalculator.isEligible(employee(LocalDate.of(2026, 1, 31), EmployeeStatus.ACTIVE),
                2026, 1)).isTrue();
    }

    @Test
    void notEligibleWhenActiveButJoinedAfterMonthEnd() {
        assertThat(IncentiveCalculator.isEligible(employee(LocalDate.of(2026, 2, 1), EmployeeStatus.ACTIVE),
                2026, 1)).isFalse();
    }

    @Test
    void notEligibleWhenRetired() {
        assertThat(IncentiveCalculator.isEligible(employee(LocalDate.of(2020, 1, 1), EmployeeStatus.RETIRED),
                2026, 1)).isFalse();
    }

    @Test
    void notEligibleWhenTransferred() {
        assertThat(IncentiveCalculator.isEligible(employee(LocalDate.of(2020, 1, 1), EmployeeStatus.TRANSFERRED),
                2026, 1)).isFalse();
    }
}
