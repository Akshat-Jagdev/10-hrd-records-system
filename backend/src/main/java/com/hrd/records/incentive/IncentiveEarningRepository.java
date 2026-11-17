package com.hrd.records.incentive;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IncentiveEarningRepository extends JpaRepository<IncentiveEarning, Long>,
        JpaSpecificationExecutor<IncentiveEarning> {

    boolean existsByEmployeeId(Long employeeId);

    boolean existsBySchemeId(Long schemeId);

    long countByStatus(IncentiveStatus status);

    /**
     * Bulk-deletes the DRAFT rows of one scheme/department/period so a
     * recalculation starts clean. Callers must run inside a transaction
     * ({@code @Transactional}); decided (APPROVED/REJECTED) rows are kept.
     */
    @Modifying
    @Query("delete from IncentiveEarning e where e.status = com.hrd.records.incentive.IncentiveStatus.DRAFT"
            + " and e.scheme.id = :schemeId and e.year = :year and e.month = :month"
            + " and e.employee.department.id = :departmentId")
    int deleteDraftBySchemeAndDepartmentAndPeriod(Long schemeId, Long departmentId, int year, int month);
}
