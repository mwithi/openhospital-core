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
 * Enumeration of all accessible fields in the Laboratory domain.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * FieldPermission.read(
 *     LaboratoryField.EXAM_CODE,
 *     LaboratoryField.RESULT,
 *     LaboratoryField.EXAM_DATE)
 *   .purpose("Attach lab results to radiology report")
 * }</pre>
 */
public enum LaboratoryField implements DomainField {

    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    LAB_ID           ("labId",            FieldSensitivity.ID,        false),
    PATIENT_CODE     ("patientCode",       FieldSensitivity.ID,        false),

    // -------------------------------------------------------------------------
    // Administrative / organisational
    // -------------------------------------------------------------------------

    EXAM_CODE        ("examCode",          FieldSensitivity.INTERNAL,  false),
    EXAM_DATE        ("examDate",          FieldSensitivity.PERSONAL,  false),
    STATUS           ("status",            FieldSensitivity.INTERNAL,  false),
    MATERIAL         ("material",          FieldSensitivity.INTERNAL,  false),

    // -------------------------------------------------------------------------
    // Clinical results — GDPR Art. 9
    // -------------------------------------------------------------------------

    RESULT           ("result",            FieldSensitivity.CLINICAL,  true),
    RESULT_VALUE     ("resultValue",       FieldSensitivity.CLINICAL,  true),
    RESULT_UNIT      ("resultUnit",        FieldSensitivity.CLINICAL,  false),
    REFERENCE_RANGE  ("referenceRange",    FieldSensitivity.CLINICAL,  false),
    NOTE             ("note",              FieldSensitivity.CLINICAL,  true);

    private final String           name;
    private final FieldSensitivity sensitivity;
    private final boolean          writeable;

    LaboratoryField(String name, FieldSensitivity sensitivity, boolean writeable) {
        this.name        = name;
        this.sensitivity = sensitivity;
        this.writeable   = writeable;
    }

    @Override public String           fieldName()   { return name; }
    @Override public FieldSensitivity sensitivity() { return sensitivity; }
    @Override public boolean          writeable()   { return writeable; }
}
