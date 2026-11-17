package com.hrd.records.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.jayway.jsonpath.JsonPath.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared base for all MockMvc + Testcontainers integration tests.
 *
 * <p>Starts a throwaway PostgreSQL 16 container per test class and wires it into
 * the Spring context via {@link ServiceConnection} (Flyway then applies V1-V3,
 * so every class runs against freshly seeded demo data). {@code disabledWithoutDocker}
 * keeps these tests green-by-skipping on machines without a Docker daemon; CI
 * runs them with {@code mvn verify} (failsafe picks up {@code *IT} classes).
 *
 * <p>{@link DirtiesContext} matters: failsafe runs every IT class in a single
 * JVM, and the Testcontainers JUnit extension restarts the static container
 * (on a new random host port) for each class. Closing the Spring context after
 * every class forces the next class to rebuild its datasource against the new
 * port, and gives each IT class a completely isolated database - test classes
 * share no state and never depend on execution order.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "app.jwt.secret=" + AbstractIntegrationTest.TEST_JWT_SECRET)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    /** HS256 key material, 64 chars, well above the 32-byte minimum of {@code app.jwt.secret}. */
    static final String TEST_JWT_SECRET =
            "test-jwt-secret-0123456789abcdef0123456789abcdef0123456789abcdef";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Exchanges demo credentials (seeded by V2) for a bearer token.
     *
     * @throws AssertionError if the login does not return 200 with a token
     */
    protected String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(body, "$.token");
    }
}
