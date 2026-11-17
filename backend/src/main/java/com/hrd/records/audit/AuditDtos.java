package com.hrd.records.audit;

import java.time.Instant;

/** Response payloads for the audit trail. */
public final class AuditDtos {

    private AuditDtos() {
    }

    /** oldValue/newValue are stored as jsonb and returned as raw JSON objects, not escaped strings. */
    public record AuditEntry(Long id, Instant occurredAt, String username, AuditAction action, String entity,
                             String entityId, Object oldValue, Object newValue) {

        public static AuditEntry of(AuditLog log) {
            return new AuditEntry(log.getId(), log.getOccurredAt(), log.getUsername(), log.getAction(),
                    log.getEntity(), log.getEntityId(), log.getOldValue(), log.getNewValue());
        }
    }
}
