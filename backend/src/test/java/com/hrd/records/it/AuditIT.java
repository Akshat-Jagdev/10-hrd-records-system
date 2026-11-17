package com.hrd.records.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.jayway.jsonpath.JsonPath.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Write operations land in the audit trail with the acting username, and the
 * trail is readable by privileged roles only. Self-contained: the department
 * whose audit entry is asserted is created inside the test, so the assertion
 * never depends on another test's data or on method ordering.
 */
class AuditIT extends AbstractIntegrationTest {

    /** New unique department code; must not collide with the V2 seeds (MELT, SMS, ...). */
    private static final String NEW_DEPARTMENT_JSON = """
            {
              "code": "QACY",
              "name": "Quality Assurance Cell",
              "type": "SUPPORT"
            }
            """;

    @Test
    void departmentCreateIsRecordedInAuditTrailAsCreateByActingUser() throws Exception {
        String hrToken = login("hr.manager", "HrManager@HRD-Demo");

        String created = mockMvc.perform(post("/api/v1/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_DEPARTMENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("QACY"))
                .andReturn().getResponse().getContentAsString();
        String createdId = read(created, "$.id").toString();

        String auditorToken = login("auditor", "Auditor@HRD-Demo");

        mockMvc.perform(get("/api/v1/audit")
                        .queryParam("entity", "Department")
                        .queryParam("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.entityId == '%s')].action", createdId)
                        .value("CREATE"))
                .andExpect(jsonPath("$.content[?(@.entityId == '%s')].username", createdId)
                        .value("hr.manager"));
    }

    @Test
    void auditTrailIsForbiddenForViewer() throws Exception {
        String token = login("viewer", "Viewer@HRD-Demo");

        mockMvc.perform(get("/api/v1/audit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
