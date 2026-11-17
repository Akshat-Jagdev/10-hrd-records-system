package com.hrd.records.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request/response payloads for department administration. */
public final class DepartmentDtos {

    private DepartmentDtos() {
    }

    public record DepartmentSummary(Long id, String code, String name, DepartmentType type) {

        public static DepartmentSummary of(Department department) {
            return new DepartmentSummary(department.getId(), department.getCode(), department.getName(),
                    department.getType());
        }
    }

    public record DepartmentRequest(
            @NotBlank @Size(max = 10) String code,
            @NotBlank @Size(max = 100) String name,
            @NotNull DepartmentType type) {
    }
}
