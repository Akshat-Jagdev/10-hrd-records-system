package com.hrd.records.department;

/**
 * Kind of department. Production shops earn incentives; support departments
 * do not.
 */
public enum DepartmentType {

    /** A production shop whose output is measured monthly and whose employees can earn incentives. */
    PRODUCTION,

    /** A support department (HR, accounts, maintenance and similar) that never earns incentives. */
    SUPPORT
}
