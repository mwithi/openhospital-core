/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2024 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.plugin.model.field;

/**
 * Privacy sensitivity classification for a domain field.
 *
 * <p>Used by the Plugin Permission Explorer (Dev Kit) to visually warn the
 * developer about what they are requesting access to, and by the administrator
 * UI to highlight high-sensitivity fields during plugin install approval.
 *
 * <p>The levels are ordered from least to most sensitive. A plugin requesting
 * a {@code SENSITIVE} field must provide a stronger justification in the
 * {@code purpose} field and requires explicit administrator approval
 * regardless of the plugin's other permissions.
 *
 * <h3>Mapping to GDPR categories</h3>
 * <ul>
 *   <li>{@code ID} and {@code INTERNAL} — not personal data by themselves</li>
 *   <li>{@code PERSONAL} — personal data (Art. 4 GDPR)</li>
 *   <li>{@code CLINICAL} — health data, special category (Art. 9 GDPR)</li>
 *   <li>{@code SENSITIVE} — health data requiring explicit consent</li>
 * </ul>
 */
public enum FieldSensitivity {

    /**
     * Opaque system identifier. Not personal data on its own.
     * Example: {@code patientCode}, {@code admissionId}.
     */
    ID,

    /**
     * Organisational or administrative code. Not personal data.
     * Example: {@code wardCode}, {@code examCode}, {@code dischargeTypeCode}.
     */
    INTERNAL,

    /**
     * Personal data identifying a natural person (GDPR Art. 4).
     * Example: {@code firstName}, {@code lastName}, {@code birthDate}.
     */
    PERSONAL,

    /**
     * Health data — special category under GDPR Art. 9.
     * Requires documented justification.
     * Example: {@code diagnosis}, {@code result}, {@code prescription}.
     */
    CLINICAL,

    /**
     * Highly sensitive health or identity data.
     * Requires explicit administrator approval at install time,
     * regardless of other granted permissions.
     * Example: {@code taxCode}, {@code hivStatus}, {@code mentalHealthNotes}.
     */
    SENSITIVE
}
