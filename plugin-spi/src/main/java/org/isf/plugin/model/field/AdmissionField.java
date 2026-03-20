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
 * Enumeration of all accessible fields in the Admission domain.
 *
 * <p>Covers both in-patient admissions and OPD (Out-Patient Department) visits.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * FieldPermission.read(
 *     AdmissionField.ADMISSION_DATE,
 *     AdmissionField.WARD_CODE,
 *     AdmissionField.DIAGNOSIS_IN)
 *   .purpose("Generate ward occupancy report")
 * }</pre>
 */
public enum AdmissionField implements DomainField {

    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    ADMISSION_ID     ("admissionId",      FieldSensitivity.ID,        false),
    PATIENT_CODE     ("patientCode",      FieldSensitivity.ID,        false),

    // -------------------------------------------------------------------------
    // Administrative / organisational — INTERNAL
    // -------------------------------------------------------------------------

    WARD_CODE        ("wardCode",         FieldSensitivity.INTERNAL,  false),
    ADMISSION_TYPE   ("admissionType",    FieldSensitivity.INTERNAL,  false),
    DISCHARGE_TYPE   ("dischargeType",    FieldSensitivity.INTERNAL,  false),
    BED_CODE         ("bedCode",          FieldSensitivity.INTERNAL,  false),

    // -------------------------------------------------------------------------
    // Dates — PERSONAL (indirectly identify a person's presence)
    // -------------------------------------------------------------------------

    ADMISSION_DATE   ("admissionDate",    FieldSensitivity.PERSONAL,  false),
    DISCHARGE_DATE   ("dischargeDate",    FieldSensitivity.PERSONAL,  false),

    // -------------------------------------------------------------------------
    // Clinical — GDPR Art. 9
    // -------------------------------------------------------------------------

    DIAGNOSIS_IN     ("diagnosisIn",      FieldSensitivity.CLINICAL,  true),
    DIAGNOSIS_OUT    ("diagnosisOut",     FieldSensitivity.CLINICAL,  true),
    OPERATION        ("operation",        FieldSensitivity.CLINICAL,  true),
    OPERATION_DATE   ("operationDate",    FieldSensitivity.CLINICAL,  false),
    NOTE             ("note",             FieldSensitivity.CLINICAL,  true),
    TREATMENT        ("treatment",        FieldSensitivity.CLINICAL,  true),
    WEIGHT           ("weight",           FieldSensitivity.CLINICAL,  true),
    TEMPERATURE      ("temperature",      FieldSensitivity.CLINICAL,  true),
    BLOOD_PRESSURE   ("bloodPressure",    FieldSensitivity.CLINICAL,  true),
    SATURATIONS      ("saturations",      FieldSensitivity.CLINICAL,  true);

    private final String           name;
    private final FieldSensitivity sensitivity;
    private final boolean          writeable;

    AdmissionField(String name, FieldSensitivity sensitivity, boolean writeable) {
        this.name        = name;
        this.sensitivity = sensitivity;
        this.writeable   = writeable;
    }

    @Override public String           fieldName()   { return name; }
    @Override public FieldSensitivity sensitivity() { return sensitivity; }
    @Override public boolean          writeable()   { return writeable; }
}
