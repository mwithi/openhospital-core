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
 * Marker interface for all domain field enumerations.
 *
 * <p>Each OH domain (Patient, Admission, Laboratory, etc.) provides a concrete
 * enum implementing this interface. Plugin developers use these enums to
 * declare exactly which fields they need in the {@link FieldPermission} block
 * of their manifest — rather than requesting broad domain-level access.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * FieldPermission.read(PatientField.FIRST_NAME, PatientField.LAST_NAME)
 *                .purpose("Display patient name on report header")
 * }</pre>
 *
 * <p>Every {@code DomainField} carries metadata about its name, sensitivity
 * level, and whether it supports write access. This metadata drives:
 * <ul>
 *   <li>The Permission Explorer UI in the Plugin Dev Kit</li>
 *   <li>Runtime field filtering in {@code PluginContext.data()}</li>
 *   <li>Audit log granularity</li>
 *   <li>Administrator approval UI during plugin install</li>
 * </ul>
 */
public interface DomainField {

    /**
     * The canonical field name as it appears in the OH data model
     * and in the plugin manifest JSON.
     * Example: {@code "firstName"}, {@code "taxCode"}.
     *
     * @return field name, never null or blank
     */
    String fieldName();

    /**
     * Privacy sensitivity classification of this field.
     *
     * @return sensitivity level, never null
     */
    FieldSensitivity sensitivity();

    /**
     * Whether a plugin can request write access to this field.
     *
     * <p>{@code false} means the field is read-only for plugins —
     * it can only be written by OH-core business logic.
     *
     * @return {@code true} if write access is available to plugins
     */
    boolean writeable();
}
