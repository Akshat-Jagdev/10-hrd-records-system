package com.hrd.records.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Persists an audit entry for every business mutation. Callers pass DTO
 * snapshots; this service serializes them and stamps the acting username.
 */
@Service
public class AuditService {

    private final AuditRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records one mutation.
     *
     * @param oldValue DTO snapshot before the change (null on CREATE)
     * @param newValue DTO snapshot after the change (null on DELETE)
     */
    public void record(AuditAction action, String entity, String entityId, Object oldValue, Object newValue) {
        AuditLog entry = new AuditLog(Instant.now(), currentUsername(), action, entity, entityId,
                toTree(oldValue), toTree(newValue));
        repository.save(entry);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return authentication.getName();
    }

    private JsonNode toTree(Object value) {
        return value == null ? null : objectMapper.valueToTree(value);
    }
}
