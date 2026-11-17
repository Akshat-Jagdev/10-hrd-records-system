package com.hrd.records.incentive;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * An incentive scheme: the rules that turn a department's monthly production
 * achievement into per-employee incentive amounts. Points only pay between
 * the threshold and the ceiling; each grade of employee can be paid at its
 * own multiplier via the {@link SchemeGradeRate} children.
 */
@Entity
@Table(name = "incentive_schemes")
public class IncentiveScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal thresholdPct;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ceilingPct;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerPoint;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchemeGradeRate> gradeRates = new ArrayList<>();

    protected IncentiveScheme() {
    }

    public IncentiveScheme(String name, String description, BigDecimal thresholdPct, BigDecimal ceilingPct,
                           BigDecimal ratePerPoint, boolean active, LocalDate effectiveFrom,
                           LocalDate effectiveTo) {
        this.name = name;
        this.description = description;
        this.thresholdPct = thresholdPct;
        this.ceilingPct = ceilingPct;
        this.ratePerPoint = ratePerPoint;
        this.active = active;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    /**
     * Links a grade rate to this scheme, keeping both sides of the
     * bidirectional relationship consistent.
     */
    public void addGradeRate(SchemeGradeRate gradeRate) {
        gradeRates.add(gradeRate);
        gradeRate.setScheme(this);
    }

    /**
     * Unlinks a grade rate from this scheme, keeping both sides consistent.
     * The orphan is deleted on flush because the collection removes orphans.
     */
    public void removeGradeRate(SchemeGradeRate gradeRate) {
        gradeRates.remove(gradeRate);
        gradeRate.setScheme(null);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getThresholdPct() {
        return thresholdPct;
    }

    public void setThresholdPct(BigDecimal thresholdPct) {
        this.thresholdPct = thresholdPct;
    }

    public BigDecimal getCeilingPct() {
        return ceilingPct;
    }

    public void setCeilingPct(BigDecimal ceilingPct) {
        this.ceilingPct = ceilingPct;
    }

    public BigDecimal getRatePerPoint() {
        return ratePerPoint;
    }

    public void setRatePerPoint(BigDecimal ratePerPoint) {
        this.ratePerPoint = ratePerPoint;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public List<SchemeGradeRate> getGradeRates() {
        return gradeRates;
    }
}
