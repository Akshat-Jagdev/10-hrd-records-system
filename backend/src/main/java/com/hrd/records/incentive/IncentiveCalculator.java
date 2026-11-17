package com.hrd.records.incentive;

import com.hrd.records.employee.Employee;
import com.hrd.records.employee.EmployeeStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Pure, stateless incentive arithmetic: achievement percentage, per-employee
 * amount and eligibility. No persistence and no Spring - everything here can
 * be reasoned about (and reused) independently of the database.
 */
public final class IncentiveCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private IncentiveCalculator() {
    }

    /**
     * Achievement as a percentage of plan: {@code actual * 100 / planned},
     * rounded half-up to 2 decimals. A null or zero plan yields zero, since
     * there is nothing to measure achievement against.
     *
     * @param planned planned production for the period (null or zero allowed)
     * @param actual  actual production for the period (null treated as zero)
     * @return achievement percentage at scale 2, never null
     */
    public static BigDecimal achievementPct(BigDecimal planned, BigDecimal actual) {
        if (planned == null || planned.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal safeActual = actual == null ? BigDecimal.ZERO : actual;
        return safeActual.multiply(HUNDRED).divide(planned, 2, RoundingMode.HALF_UP);
    }

    /**
     * Incentive amount for one employee. Achievement is capped at the ceiling,
     * the threshold is subtracted (only points above threshold and below the
     * ceiling pay), and the remaining effective points are multiplied by the
     * rate per point and the grade factor. At or below the threshold the
     * amount is zero.
     *
     * @param achievementPct the employee's achievement percentage
     * @param thresholdPct   percentage below which nothing pays
     * @param ceilingPct     percentage above which extra points stop paying
     * @param ratePerPoint   money paid per effective point at factor 1
     * @param gradeFactor    per-grade multiplier (null treated as 1)
     * @return the incentive amount at scale 2, rounded half-up, never negative
     */
    public static BigDecimal amount(BigDecimal achievementPct, BigDecimal thresholdPct, BigDecimal ceilingPct,
                                    BigDecimal ratePerPoint, BigDecimal gradeFactor) {
        BigDecimal achievement = achievementPct == null ? BigDecimal.ZERO : achievementPct;
        BigDecimal ceiling = ceilingPct == null ? achievement : ceilingPct;
        BigDecimal threshold = thresholdPct == null ? BigDecimal.ZERO : thresholdPct;
        BigDecimal rate = ratePerPoint == null ? BigDecimal.ZERO : ratePerPoint;
        BigDecimal factor = gradeFactor == null ? BigDecimal.ONE : gradeFactor;

        BigDecimal effectivePoints = achievement.min(ceiling).subtract(threshold);
        if (effectivePoints.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return effectivePoints.multiply(rate).multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Whether an employee participates in a period's calculation: they must be
     * ACTIVE and have joined on or before the last day of that month.
     *
     * @param employee the candidate employee
     * @param year     period year
     * @param month    period month (1-12)
     * @return true if the employee is counted in the calculation
     */
    public static boolean isEligible(Employee employee, int year, int month) {
        return employee.getStatus() == EmployeeStatus.ACTIVE
                && employee.getDoj() != null
                && !employee.getDoj().isAfter(YearMonth.of(year, month).atEndOfMonth());
    }
}
