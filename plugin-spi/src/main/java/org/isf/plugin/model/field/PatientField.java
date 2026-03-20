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
 * Enumeration of all accessible fields in the Patient domain.
 *
 * <p>Use these constants when building a {@link FieldPermission} to declare
 * exactly which patient data your plugin needs to read or write.
 *
 * <h3>Usage in manifest builder</h3>
 * <pre>{@code
 * FieldPermission.read(
 *     PatientField.FIRST_NAME,
 *     PatientField.LAST_NAME,
 *     PatientField.BIRTH_DATE)
 *   .purpose("Display patient header on radiology report")
 * }</pre>
 *
 * <h3>Note on TAX_CODE</h3>
 * {@link #TAX_CODE} has {@link FieldSensitivity#SENSITIVE} classification.
 * Requesting it will trigger a separate administrator approval step at
 * install time and will appear prominently in the permission review UI.
 */
public enum PatientField implements DomainField {

    // -------------------------------------------------------------------------
    // Identifiers — sensitivity: ID (not PII on their own)
    // -------------------------------------------------------------------------

    PATIENT_CODE     ("patientCode",      FieldSensitivity.ID,        false),

    // -------------------------------------------------------------------------
    // Demographics — sensitivity: PERSONAL (GDPR Art. 4)
    // -------------------------------------------------------------------------

    FIRST_NAME       ("firstName",        FieldSensitivity.PERSONAL,  true),
    LAST_NAME        ("lastName",         FieldSensitivity.PERSONAL,  true),
    BIRTH_DATE       ("birthDate",        FieldSensitivity.PERSONAL,  true),
    SEX              ("sex",              FieldSensitivity.PERSONAL,  true),
    ADDRESS          ("address",          FieldSensitivity.PERSONAL,  true),
    CITY             ("city",             FieldSensitivity.PERSONAL,  true),
    TELEPHONE        ("telephone",        FieldSensitivity.PERSONAL,  true),

    // -------------------------------------------------------------------------
    // Sensitive identifiers — sensitivity: SENSITIVE (requires explicit approval)
    // -------------------------------------------------------------------------

    TAX_CODE         ("taxCode",          FieldSensitivity.SENSITIVE, false),

    // -------------------------------------------------------------------------
    // Clinical — sensitivity: CLINICAL (GDPR Art. 9)
    // -------------------------------------------------------------------------

    ANAMNESIS        ("anamnesis",        FieldSensitivity.CLINICAL,  true),
    ALLERGIES        ("allergies",        FieldSensitivity.CLINICAL,  true),
    CURRENT_ILLNESS  ("currentIllness",   FieldSensitivity.CLINICAL,  true),
    FAMILY_ANAMNESIS ("familyAnamnesis",  FieldSensitivity.CLINICAL,  true),
    PERSONAL_HISTORY ("personalHistory",  FieldSensitivity.CLINICAL,  true),
    HEIGHT           ("height",           FieldSensitivity.CLINICAL,  true),
    WEIGHT           ("weight",           FieldSensitivity.CLINICAL,  true),
    BLOOD_TYPE       ("bloodType",        FieldSensitivity.CLINICAL,  true),

    // -------------------------------------------------------------------------
    // Administrative — sensitivity: INTERNAL
    // -------------------------------------------------------------------------

    WARD_CODE        ("wardCode",         FieldSensitivity.INTERNAL,  false),
    STATUS           ("status",           FieldSensitivity.INTERNAL,  false),
    NEXT_KIN         ("nextKin",          FieldSensitivity.PERSONAL,  true),
    NEXT_KIN_PHONE   ("nextKinPhone",     FieldSensitivity.PERSONAL,  true);

    // -------------------------------------------------------------------------

    private final String           name;
    private final FieldSensitivity sensitivity;
    private final boolean          writeable;

    PatientField(String name, FieldSensitivity sensitivity, boolean writeable) {
        this.name        = name;
        this.sensitivity = sensitivity;
        this.writeable   = writeable;
    }

    @Override public String           fieldName()   { return name; }
    @Override public FieldSensitivity sensitivity() { return sensitivity; }
    @Override public boolean          writeable()   { return writeable; }
}
