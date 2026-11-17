package com.hrd.records.production;

import com.hrd.records.department.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * One department's production figure for a single month. The (department,
 * year, month) period is unique; achievement percentage is always derived
 * from planned vs actual, never stored.
 */
@Entity
@Table(name = "production_records", uniqueConstraints = @UniqueConstraint(name = "uk_production_period",
        columnNames = {"department_id", "year", "month"}))
public class ProductionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal plannedTonnes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal actualTonnes;

    protected ProductionRecord() {
    }

    public ProductionRecord(Department department, int year, int month,
                            BigDecimal plannedTonnes, BigDecimal actualTonnes) {
        this.department = department;
        this.year = year;
        this.month = month;
        this.plannedTonnes = plannedTonnes;
        this.actualTonnes = actualTonnes;
    }

    public Long getId() {
        return id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getPlannedTonnes() {
        return plannedTonnes;
    }

    public void setPlannedTonnes(BigDecimal plannedTonnes) {
        this.plannedTonnes = plannedTonnes;
    }

    public BigDecimal getActualTonnes() {
        return actualTonnes;
    }

    public void setActualTonnes(BigDecimal actualTonnes) {
        this.actualTonnes = actualTonnes;
    }
}
