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
package org.isf.plugin.model;

import org.isf.plugin.model.field.DomainField;
import org.isf.plugin.model.field.FieldSensitivity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A fine-grained data permission declaring exactly which fields of a given
 * domain a plugin needs to read or write, and why.
 *
 * <p>Replaces the coarse-grained {@link PluginPermission} enum for data access
 * declarations. A plugin manifest now declares:
 *
 * <pre>{@code
 * // Instead of:
 * permissions: ["READ_PATIENT"]   // vague — what fields exactly?
 *
 * // Now:
 * fieldPermissions: [
 *   {
 *     "domain":   "Patient",
 *     "access":   "READ",
 *     "fields":   ["firstName", "lastName", "birthDate"],
 *     "purpose":  "Display patient header on radiology report"
 *   }
 * ]
 * }</pre>
 *
 * <h3>Builder API</h3>
 * <pre>{@code
 * // Read-only — most common case
 * FieldPermission.read(
 *     PatientField.FIRST_NAME,
 *     PatientField.LAST_NAME,
 *     PatientField.BIRTH_DATE)
 *   .purpose("Display patient header on radiology report");
 *
 * // Write — plugin creates/updates data
 * FieldPermission.write(
 *     LaboratoryField.RESULT,
 *     LaboratoryField.EXAM_DATE)
 *   .purpose("Store radiology report as laboratory result");
 * }</pre>
 *
 * <h3>Relationship with PluginPermission</h3>
 * {@link PluginPermission} is retained for non-data permissions that do not
 * map to specific fields: {@code READ_USERS}, {@code WRITE_SYSTEM_CONFIG},
 * {@code READ_REPORTS}. For all patient/clinical/pharmacy data access,
 * {@code FieldPermission} is the preferred declaration.
 *
 * <h3>Privacy guarantees</h3>
 * At runtime, {@code PluginContextImpl} uses the declared field list to
 * filter the domain view — a plugin that declared only {@code firstName}
 * and {@code lastName} will receive {@code null} for all other fields
 * even if it calls their getter. No code changes in the plugin are needed
 * to enforce this; the filtering is transparent.
 */
public final class FieldPermission {

    /** The access level requested by the plugin for the declared fields. */
    public enum Access {
        /** Plugin reads the declared fields. Does not imply write. */
        READ,
        /**
         * Plugin creates or modifies the declared fields.
         * Implicitly includes READ. Only valid for fields where
         * {@link DomainField#writeable()} is {@code true}.
         */
        WRITE
    }

    private final String            domainName;
    private final Class<? extends DomainField> domainClass;
    private final List<DomainField> fields;
    private final Access            access;
    private final String            purpose;

    private FieldPermission(Builder builder) {
        this.domainName  = builder.domainName;
        this.domainClass = builder.domainClass;
        this.fields      = List.copyOf(builder.fields);
        this.access      = builder.access;
        this.purpose     = builder.purpose;
    }

    // -------------------------------------------------------------------------
    // Static factory methods — the intended entry points
    // -------------------------------------------------------------------------

    /**
     * Starts building a READ permission for the given fields.
     *
     * @param fields one or more domain fields to request read access for
     * @return a builder to complete with {@link Builder#purpose(String)}
     * @throws IllegalArgumentException if no fields are provided
     */
    @SafeVarargs
    public static <F extends DomainField> Builder read(F... fields) {
        return new Builder(Access.READ, fields);
    }

    /**
     * Starts building a WRITE permission for the given fields.
     *
     * <p>All specified fields must have {@link DomainField#writeable()}
     * returning {@code true}; non-writable fields cause an
     * {@link IllegalArgumentException} during {@link Builder#purpose(String)}.
     *
     * @param fields one or more domain fields to request write access for
     * @return a builder to complete with {@link Builder#purpose(String)}
     * @throws IllegalArgumentException if no fields are provided
     */
    @SafeVarargs
    public static <F extends DomainField> Builder write(F... fields) {
        return new Builder(Access.WRITE, fields);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** Short name of the domain, e.g. {@code "Patient"}, {@code "Laboratory"}. */
    public String getDomainName() { return domainName; }

    /** The enum class representing the domain, for runtime type checks. */
    public Class<? extends DomainField> getDomainClass() { return domainClass; }

    /** Immutable list of fields this permission covers. */
    public List<DomainField> getFields() { return fields; }

    /** Whether this is a read or write permission. */
    public Access getAccess() { return access; }

    /**
     * Human-readable justification shown to the administrator during
     * plugin install review. Must explain specifically why this data is needed.
     */
    public String getPurpose() { return purpose; }

    /**
     * Returns the highest sensitivity level among all declared fields.
     * Used by the admin UI to flag permissions requiring special attention.
     */
    public FieldSensitivity maxSensitivity() {
        return fields.stream()
                .map(DomainField::sensitivity)
                .max(java.util.Comparator.comparingInt(Enum::ordinal))
                .orElse(FieldSensitivity.ID);
    }

    /**
     * Returns {@code true} if any declared field has
     * {@link FieldSensitivity#SENSITIVE} classification.
     * Such permissions require explicit administrator approval.
     */
    public boolean requiresExplicitApproval() {
        return fields.stream()
                .anyMatch(f -> f.sensitivity() == FieldSensitivity.SENSITIVE);
    }

    /**
     * Returns the list of field names as strings — used for manifest
     * serialization and audit logging.
     */
    public List<String> fieldNames() {
        return fields.stream()
                .map(DomainField::fieldName)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String toString() {
        return "FieldPermission{domain='" + domainName + '\'' +
               ", access=" + access +
               ", fields=" + fieldNames() + '}';
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static final class Builder {

        private final Access              access;
        private final List<DomainField>   fields;
        private final String              domainName;
        private final Class<? extends DomainField> domainClass;
        private       String              purpose;

        @SafeVarargs
        private <F extends DomainField> Builder(Access access, F... fields) {
            if (fields == null || fields.length == 0) {
                throw new IllegalArgumentException(
                    "FieldPermission requires at least one field");
            }
            this.access      = access;
            this.fields      = Arrays.asList(fields);
            this.domainClass = fields[0].getClass();
            // derive domain name from enum class name: "PatientField" → "Patient"
            String simpleName = domainClass.getSimpleName();
            this.domainName  = simpleName.endsWith("Field")
                    ? simpleName.substring(0, simpleName.length() - 5)
                    : simpleName;
        }

        /**
         * Completes the permission with a mandatory purpose statement and
         * validates all constraints.
         *
         * @param purpose human-readable justification, shown to the administrator
         * @return the immutable {@link FieldPermission}
         * @throws IllegalArgumentException if purpose is blank, or if WRITE is
         *         requested for a non-writable field
         */
        public FieldPermission purpose(String purpose) {
            if (purpose == null || purpose.isBlank()) {
                throw new IllegalArgumentException(
                    "FieldPermission.purpose must not be blank — " +
                    "explain specifically why you need this data");
            }
            if (access == Access.WRITE) {
                List<String> nonWriteable = fields.stream()
                        .filter(f -> !f.writeable())
                        .map(DomainField::fieldName)
                        .collect(Collectors.toList());
                if (!nonWriteable.isEmpty()) {
                    throw new IllegalArgumentException(
                        "The following fields are read-only and cannot be " +
                        "requested with WRITE access: " + nonWriteable);
                }
            }
            this.purpose = purpose;
            return new FieldPermission(this);
        }
    }

}
