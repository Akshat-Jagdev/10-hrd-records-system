package com.hrd.records.incentive;

import com.hrd.records.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One employee's incentive for one scheme, department and period. Rows are
 * created as DRAFT by the calculation flow and then approved or rejected by a
 * manager; decidedBy/decidedAt capture that decision.
 */
@Entity
@Table(name = "incentive_earnings")
public class IncentiveEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private IncentiveScheme scheme;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal achievementPct;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncentiveStatus status;

    @Column(length = 50)
    private String decidedBy;

    private Instant decidedAt;

    protected IncentiveEarning() {
    }

    public IncentiveEarning(Employee employee, IncentiveScheme scheme, int year, int month,
                            BigDecimal achievementPct, BigDecimal amount, IncentiveStatus status) {
        this.employee = employee;
        this.scheme = scheme;
        this.year = year;
        this.month = month;
        this.achievementPct = achievementPct;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public IncentiveScheme getScheme() {
        return scheme;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public BigDecimal getAchievementPct() {
        return achievementPct;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public IncentiveStatus getStatus() {
        return status;
    }

    public void setStatus(IncentiveStatus status) {
        this.status = status;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
