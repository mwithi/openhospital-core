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
 * Enumeration of all accessible fields in the Pharmacy / Stock domain.
 *
 * <p>Pharmacy movements tied to a patient carry clinical sensitivity.
 * Stock-level data (quantities, lots) is organisational.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * FieldPermission.read(
 *     PharmacyField.MEDICAL_CODE,
 *     PharmacyField.QUANTITY,
 *     PharmacyField.MOVEMENT_DATE)
 *   .purpose("Reconcile drug dispensing with radiology contrast usage")
 * }</pre>
 */
public enum PharmacyField implements DomainField {

    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    MOVEMENT_ID      ("movementId",        FieldSensitivity.ID,        false),
    PATIENT_CODE     ("patientCode",        FieldSensitivity.ID,        false),

    // -------------------------------------------------------------------------
    // Stock / organisational
    // -------------------------------------------------------------------------

    MEDICAL_CODE     ("medicalCode",        FieldSensitivity.INTERNAL,  false),
    MEDICAL_DESC     ("medicalDescription", FieldSensitivity.INTERNAL,  false),
    QUANTITY         ("quantity",           FieldSensitivity.INTERNAL,  false),
    LOT_CODE         ("lotCode",            FieldSensitivity.INTERNAL,  false),
    EXPIRY_DATE      ("expiryDate",         FieldSensitivity.INTERNAL,  false),
    WARD_CODE        ("wardCode",           FieldSensitivity.INTERNAL,  false),
    MOVEMENT_DATE    ("movementDate",       FieldSensitivity.INTERNAL,  false),
    MOVEMENT_TYPE    ("movementType",       FieldSensitivity.INTERNAL,  false),

    // -------------------------------------------------------------------------
    // Clinical (when tied to a patient)
    // -------------------------------------------------------------------------

    DOSE             ("dose",               FieldSensitivity.CLINICAL,  true),
    FREQUENCY        ("frequency",          FieldSensitivity.CLINICAL,  true),
    NOTE             ("note",               FieldSensitivity.CLINICAL,  true);

    private final String           name;
    private final FieldSensitivity sensitivity;
    private final boolean          writeable;

    PharmacyField(String name, FieldSensitivity sensitivity, boolean writeable) {
        this.name        = name;
        this.sensitivity = sensitivity;
        this.writeable   = writeable;
    }

    @Override public String           fieldName()   { return name; }
    @Override public FieldSensitivity sensitivity() { return sensitivity; }
    @Override public boolean          writeable()   { return writeable; }
}
