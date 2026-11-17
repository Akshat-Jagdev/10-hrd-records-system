package com.hrd.records.incentive;

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
 * Per-grade payment multiplier within a scheme. A grade that has no row here
 * is paid at factor 1. The parent {@link IncentiveScheme} owns the link; there
 * is deliberately no public setter for {@code scheme}.
 */
@Entity
@Table(name = "scheme_grade_rates", uniqueConstraints = @UniqueConstraint(columnNames = {"scheme_id", "grade"}))
public class SchemeGradeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal factor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private IncentiveScheme scheme;

    protected SchemeGradeRate() {
    }

    public SchemeGradeRate(String grade, BigDecimal factor) {
        this.grade = grade;
        this.factor = factor;
    }

    public Long getId() {
        return id;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }

    public IncentiveScheme getScheme() {
        return scheme;
    }

    /** Package-private: only {@link IncentiveScheme#addGradeRate} / {@link IncentiveScheme#removeGradeRate} set this. */
    void setScheme(IncentiveScheme scheme) {
        this.scheme = scheme;
    }
}
