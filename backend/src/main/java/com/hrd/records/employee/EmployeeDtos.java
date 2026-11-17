package com.hrd.records.employee;

import com.hrd.records.department.DepartmentDtos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request/response payloads for employee master records. */
public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public record EmployeeSummary(Long id, String empNo, String fullName, String grade, String designation,
                                   DepartmentDtos.DepartmentSummary department, LocalDate doj,
                                   EmployeeStatus status, BigDecimal basicPay) {

        public static EmployeeSummary of(Employee employee) {
            return new EmployeeSummary(employee.getId(), employee.getEmpNo(), employee.getFullName(),
                    employee.getGrade(), employee.getDesignation(),
                    DepartmentDtos.DepartmentSummary.of(employee.getDepartment()),
                    employee.getDoj(), employee.getStatus(), employee.getBasicPay());
        }
    }

    public record EmployeeRequest(
            @NotBlank @Size(max = 20) String empNo,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 10) String grade,
            @NotBlank @Size(max = 100) String designation,
            @NotNull Long departmentId,
            @NotNull @PastOrPresent LocalDate doj,
            @NotNull EmployeeStatus status,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal basicPay) {
    }
}
