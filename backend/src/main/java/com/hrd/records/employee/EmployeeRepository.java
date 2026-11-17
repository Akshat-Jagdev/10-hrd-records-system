package com.hrd.records.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmpNo(String empNo);

    boolean existsByEmpNo(String empNo);

    boolean existsByDepartmentId(Long departmentId);

    long countByStatus(EmployeeStatus status);

    List<Employee> findByDepartmentId(Long departmentId);
}
