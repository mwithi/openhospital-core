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
 * Enumeration of all accessible fields in the Ward domain.
 *
 * <p>Ward data is mostly organisational and carries lower sensitivity
 * than patient or clinical data. It is commonly needed by plugins that
 * generate occupancy reports or manage bed allocation.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * FieldPermission.read(
 *     WardField.WARD_CODE,
 *     WardField.DESCRIPTION,
 *     WardField.BED_COUNT)
 *   .purpose("Generate ward occupancy dashboard")
 * }</pre>
 */
public enum WardField implements DomainField {

    WARD_CODE        ("wardCode",          FieldSensitivity.INTERNAL,  false),
    DESCRIPTION      ("description",       FieldSensitivity.INTERNAL,  false),
    TELEPHONE        ("telephone",         FieldSensitivity.INTERNAL,  false),
    FAX              ("fax",               FieldSensitivity.INTERNAL,  false),
    EMAIL            ("email",             FieldSensitivity.INTERNAL,  false),
    BED_COUNT        ("bedCount",          FieldSensitivity.INTERNAL,  false),
    AVAILABLE_BEDS   ("availableBeds",     FieldSensitivity.INTERNAL,  false),
    NURSE_COUNT      ("nurseCount",        FieldSensitivity.INTERNAL,  false),
    DOCTOR_COUNT     ("doctorCount",       FieldSensitivity.INTERNAL,  false);

    private final String           name;
    private final FieldSensitivity sensitivity;
    private final boolean          writeable;

    WardField(String name, FieldSensitivity sensitivity, boolean writeable) {
        this.name        = name;
        this.sensitivity = sensitivity;
        this.writeable   = writeable;
    }

    @Override public String           fieldName()   { return name; }
    @Override public FieldSensitivity sensitivity() { return sensitivity; }
    @Override public boolean          writeable()   { return writeable; }
}
