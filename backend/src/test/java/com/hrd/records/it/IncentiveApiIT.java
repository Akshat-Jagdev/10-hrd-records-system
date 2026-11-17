package com.hrd.records.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Incentive calculation and approval over the V3 seed: scheme 1 is active for
 * 2024-06 and department 1 (MELT) has a production record and eligible
 * employees for that period, so a calculation must produce DRAFT earnings.
 *
 * <p>Both mutating tests call {@code calculate} themselves first, so neither
 * depends on the other's leftovers or on method ordering (recalculating a
 * period replaces its DRAFT rows, and APPROVED rows are never touched).
 */
class IncentiveApiIT extends AbstractIntegrationTest {

    /** scheme 1 (Production Incentive 2024-25), department 1 (MELT), June 2024. */
    private static final String CALCULATE_REQUEST_JSON = """
            {
              "schemeId": 1,
              "departmentId": 1,
              "year": 2024,
              "month": 6
            }
            """;

    @Test
    void hrManagerCalculationCreatesDraftEarnings() throws Exception {
        String token = login("hr.manager", "HrManager@HRD-Demo");

        mockMvc.perform(post("/api/v1/incentives/earnings/calculate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CALCULATE_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeId").value(1))
                .andExpect(jsonPath("$.departmentId").value(1))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.month").value(6))
                .andExpect(jsonPath("$.draftCount", greaterThan(0)));
    }

    @Test
    void hrManagerCanApproveADraftEarning() throws Exception {
        String token = login("hr.manager", "HrManager@HRD-Demo");

        // Guarantee the period has DRAFT rows regardless of which test ran first.
        mockMvc.perform(post("/api/v1/incentives/earnings/calculate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CALCULATE_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftCount", greaterThan(0)));

        String drafts = mockMvc.perform(get("/api/v1/incentives/earnings")
                        .queryParam("status", "DRAFT")
                        .queryParam("schemeId", "1")
                        .queryParam("year", "2024")
                        .queryParam("month", "6")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThan(0)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Number earningId = read(drafts, "$.content[0].id");

        mockMvc.perform(post("/api/v1/incentives/earnings/{id}/approve", earningId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(earningId.intValue()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("hr.manager"));
    }

    @Test
    void viewerCannotRunCalculation() throws Exception {
        String token = login("viewer", "Viewer@HRD-Demo");

        mockMvc.perform(post("/api/v1/incentives/earnings/calculate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CALCULATE_REQUEST_JSON))
                .andExpect(status().isForbidden());
    }
}
