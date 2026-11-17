package com.hrd.records.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Employee master-data endpoint: RBAC on writes, paginated reads, and the
 * authentication boundary. Demo accounts come from the V2 seed.
 */
class EmployeeApiIT extends AbstractIntegrationTest {

    /** Valid {@code EmployeeRequest} payload referencing seeded department 1 (MELT). */
    private static final String NEW_EMPLOYEE_JSON = """
            {
              "empNo": "E9001",
              "fullName": "Integration Test Employee",
              "grade": "E2",
              "designation": "Furnace Operator",
              "departmentId": 1,
              "doj": "2024-03-01",
              "status": "ACTIVE",
              "basicPay": 45500.00
            }
            """;

    @Test
    void hrManagerCanCreateEmployee() throws Exception {
        String token = login("hr.manager", "HrManager@HRD-Demo");

        mockMvc.perform(post("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_EMPLOYEE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.empNo").value("E9001"))
                .andExpect(jsonPath("$.department.id").value(1));
    }

    @Test
    void viewerCannotCreateEmployee() throws Exception {
        String token = login("viewer", "Viewer@HRD-Demo");

        mockMvc.perform(post("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_EMPLOYEE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCanListEmployeesWithPageResponseShape() throws Exception {
        String token = login("viewer", "Viewer@HRD-Demo");

        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$.totalPages", greaterThan(0)))
                .andExpect(jsonPath("$.content[0].empNo").isNotEmpty());
    }

    @Test
    void listingEmployeesWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }
}
