package com.hrd.records.user;

/** Application roles. Authority exposed to Spring Security is {@code ROLE_<name>}. */
public enum Role {
    /** Full access, including user administration. */
    ADMIN,
    /** HR data steward: employees, departments, production, incentive approval. */
    HR_MANAGER,
    /** Read-only access to business data. */
    VIEWER,
    /** Read-only access to business data plus the audit trail. */
    AUDITOR
}
