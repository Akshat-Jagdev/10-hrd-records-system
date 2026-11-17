package com.hrd.records.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Append-only trail of mutations: who changed what, when, and the before/after
 * snapshots. Rows are never updated or deleted by the application.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    /** Denormalized on purpose so entries survive user deletion. */
    @Column(nullable = false, updatable = false, length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private AuditAction action;

    @Column(nullable = false, updatable = false, length = 50)
    private String entity;

    @Column(nullable = false, updatable = false, length = 64)
    private String entityId;

    /** DTO snapshot before the change; null on CREATE. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private JsonNode oldValue;

    /** DTO snapshot after the change; null on DELETE. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private JsonNode newValue;

    protected AuditLog() {
    }

    public AuditLog(Instant occurredAt, String username, AuditAction action, String entity, String entityId,
                    JsonNode oldValue, JsonNode newValue) {
        this.occurredAt = occurredAt;
        this.username = username;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getUsername() {
        return username;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntity() {
        return entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public JsonNode getOldValue() {
        return oldValue;
    }

    public JsonNode getNewValue() {
        return newValue;
    }
}
