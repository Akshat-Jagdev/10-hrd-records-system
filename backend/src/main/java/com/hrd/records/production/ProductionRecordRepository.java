package com.hrd.records.production;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Access to monthly production figures, including filtered/paged search via Specifications. */
public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long>,
        JpaSpecificationExecutor<ProductionRecord> {

    Optional<ProductionRecord> findByDepartmentIdAndYearAndMonth(Long departmentId, int year, int month);

    boolean existsByDepartmentId(Long departmentId);
}
