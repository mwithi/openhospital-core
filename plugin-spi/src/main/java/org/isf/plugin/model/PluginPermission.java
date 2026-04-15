/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2026 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 */
package org.isf.plugin.model;

/**
 * Domain data permissions that a plugin may request in its {@link PluginDescriptor}.
 *
 * <p>These permissions operate at a higher level than OH user roles
 * (ADMIN, DOCTOR, NURSE, etc.): they are plugin-level permissions granted by
 * the administrator at installation time.
 *
 * <p>Even if a plugin declares {@code READ_PATIENT}, effective data access is
 * still subject to the JWT of the user making the call — the two systems
 * overlap (logical AND).
 *
 * <h3>Naming convention</h3>
 * {@code READ_*}   — read-only access to domain entities.<br>
 * {@code WRITE_*}  — create and update (implies READ).<br>
 * {@code DELETE_*} — logical or physical deletion (implies WRITE).
 */
public enum PluginPermission {

    // ------------------------------------------------------------------
    // Patients
    // ------------------------------------------------------------------
    READ_PATIENT,
    WRITE_PATIENT,
    DELETE_PATIENT,

    // ------------------------------------------------------------------
    // Admissions / Discharges
    // ------------------------------------------------------------------
    READ_ADMISSION,
    WRITE_ADMISSION,

    // ------------------------------------------------------------------
    // OPD (Outpatient Department)
    // ------------------------------------------------------------------
    READ_OPD,
    WRITE_OPD,

    // ------------------------------------------------------------------
    // Laboratory
    // ------------------------------------------------------------------
    READ_LABORATORY,
    WRITE_LABORATORY,

    // ------------------------------------------------------------------
    // Pharmacy / Stock
    // ------------------------------------------------------------------
    READ_PHARMACY,
    WRITE_PHARMACY,

    // ------------------------------------------------------------------
    // Wards
    // ------------------------------------------------------------------
    READ_WARD,
    WRITE_WARD,

    // ------------------------------------------------------------------
    // Users and roles (admin only)
    // ------------------------------------------------------------------
    READ_USERS,
    WRITE_USERS,

    // ------------------------------------------------------------------
    // System configuration
    // ------------------------------------------------------------------
    READ_SYSTEM_CONFIG,
    WRITE_SYSTEM_CONFIG,

    // ------------------------------------------------------------------
    // Reports / aggregate statistics (no PII)
    // ------------------------------------------------------------------
    READ_REPORTS
}
