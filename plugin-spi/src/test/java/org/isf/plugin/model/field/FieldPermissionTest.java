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

import org.isf.plugin.model.FieldPermission;
import org.isf.plugin.model.FieldPermission.Access;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link FieldPermission} and the domain field enums.
 */
@DisplayName("FieldPermission")
class FieldPermissionTest {

    // -------------------------------------------------------------------------
    // Happy path — read
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("read() — happy path")
    class ReadHappyPath {

        @Test
        @DisplayName("builds a valid READ permission with purpose")
        void buildsReadPermission() {
            FieldPermission p = FieldPermission.read(
                    PatientField.FIRST_NAME,
                    PatientField.LAST_NAME,
                    PatientField.BIRTH_DATE)
                    .purpose("Display patient name on report header");

            assertThat(p.getAccess()).isEqualTo(Access.READ);
            assertThat(p.getDomainName()).isEqualTo("Patient");
            assertThat(p.fieldNames()).containsExactly("firstName", "lastName", "birthDate");
            assertThat(p.getPurpose()).isEqualTo("Display patient name on report header");
        }

        @Test
        @DisplayName("domain name is derived from enum class name")
        void domainNameDerivedCorrectly() {
            assertThat(FieldPermission.read(PatientField.FIRST_NAME)
                    .purpose("test").getDomainName()).isEqualTo("Patient");

            assertThat(FieldPermission.read(LaboratoryField.RESULT)
                    .purpose("test").getDomainName()).isEqualTo("Laboratory");

            assertThat(FieldPermission.read(AdmissionField.WARD_CODE)
                    .purpose("test").getDomainName()).isEqualTo("Admission");

            assertThat(FieldPermission.read(WardField.BED_COUNT)
                    .purpose("test").getDomainName()).isEqualTo("Ward");
        }

        @Test
        @DisplayName("returned field list is immutable")
        void fieldListIsImmutable() {
            FieldPermission p = FieldPermission.read(PatientField.FIRST_NAME)
                    .purpose("test");
            assertThat(p.getFields()).hasSize(1);
        }

        @Test
        @DisplayName("maxSensitivity returns the highest level among declared fields")
        void maxSensitivityReturnsHighest() {
            FieldPermission p = FieldPermission.read(
                    PatientField.PATIENT_CODE,   // ID
                    PatientField.FIRST_NAME,     // PERSONAL
                    PatientField.ANAMNESIS)      // CLINICAL
                    .purpose("test");

            assertThat(p.maxSensitivity()).isEqualTo(FieldSensitivity.CLINICAL);
        }

        @Test
        @DisplayName("requiresExplicitApproval is false when no SENSITIVE field")
        void noExplicitApprovalForNonSensitive() {
            FieldPermission p = FieldPermission.read(
                    PatientField.FIRST_NAME,
                    PatientField.BIRTH_DATE)
                    .purpose("test");

            assertThat(p.requiresExplicitApproval()).isFalse();
        }

        @Test
        @DisplayName("requiresExplicitApproval is true when TAX_CODE is included")
        void explicitApprovalRequiredForTaxCode() {
            FieldPermission p = FieldPermission.read(
                    PatientField.FIRST_NAME,
                    PatientField.TAX_CODE)
                    .purpose("Verify patient identity against national registry");

            assertThat(p.requiresExplicitApproval()).isTrue();
            assertThat(p.maxSensitivity()).isEqualTo(FieldSensitivity.SENSITIVE);
        }
    }

    // -------------------------------------------------------------------------
    // Happy path — write
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("write() — happy path")
    class WriteHappyPath {

        @Test
        @DisplayName("builds a valid WRITE permission for writable fields")
        void buildsWritePermission() {
            FieldPermission p = FieldPermission.write(
                    LaboratoryField.RESULT,
                    LaboratoryField.NOTE)
                    .purpose("Store radiology report as lab result");

            assertThat(p.getAccess()).isEqualTo(Access.WRITE);
            assertThat(p.getDomainName()).isEqualTo("Laboratory");
            assertThat(p.fieldNames()).containsExactly("result", "note");
        }
    }

    // -------------------------------------------------------------------------
    // Validation errors
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validation errors")
    class ValidationErrors {

        @Test
        @DisplayName("no fields → IllegalArgumentException at factory method")
        void noFieldsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> FieldPermission.read())
                    .withMessageContaining("at least one field");
        }

        @Test
        @DisplayName("blank purpose → IllegalArgumentException")
        void blankPurposeRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            FieldPermission.read(PatientField.FIRST_NAME).purpose(""))
                    .withMessageContaining("purpose");
        }

        @Test
        @DisplayName("null purpose → IllegalArgumentException")
        void nullPurposeRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            FieldPermission.read(PatientField.FIRST_NAME).purpose(null))
                    .withMessageContaining("purpose");
        }

        @Test
        @DisplayName("WRITE on non-writable field → IllegalArgumentException")
        void writeOnReadOnlyFieldRejected() {
            // PatientField.TAX_CODE is writeable=false
            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            FieldPermission.write(PatientField.TAX_CODE)
                                           .purpose("test"))
                    .withMessageContaining("read-only")
                    .withMessageContaining("taxCode");
        }

        @Test
        @DisplayName("WRITE on non-writable field — PATIENT_CODE is read-only")
        void writeOnPatientCodeRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            FieldPermission.write(PatientField.PATIENT_CODE)
                                           .purpose("test"))
                    .withMessageContaining("patientCode");
        }
    }

    // -------------------------------------------------------------------------
    // Domain field enum metadata
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DomainField metadata")
    class DomainFieldMetadata {

        @Test
        @DisplayName("PatientField.FIRST_NAME has correct metadata")
        void patientFirstNameMetadata() {
            assertThat(PatientField.FIRST_NAME.fieldName()).isEqualTo("firstName");
            assertThat(PatientField.FIRST_NAME.sensitivity()).isEqualTo(FieldSensitivity.PERSONAL);
            assertThat(PatientField.FIRST_NAME.writeable()).isTrue();
        }

        @Test
        @DisplayName("PatientField.TAX_CODE is SENSITIVE and read-only")
        void taxCodeIsSensitiveAndReadOnly() {
            assertThat(PatientField.TAX_CODE.sensitivity()).isEqualTo(FieldSensitivity.SENSITIVE);
            assertThat(PatientField.TAX_CODE.writeable()).isFalse();
        }

        @Test
        @DisplayName("PatientField.PATIENT_CODE is ID sensitivity")
        void patientCodeIsId() {
            assertThat(PatientField.PATIENT_CODE.sensitivity()).isEqualTo(FieldSensitivity.ID);
            assertThat(PatientField.PATIENT_CODE.writeable()).isFalse();
        }

        @Test
        @DisplayName("LaboratoryField.RESULT is CLINICAL and writable")
        void labResultIsClinicalAndWriteable() {
            assertThat(LaboratoryField.RESULT.sensitivity()).isEqualTo(FieldSensitivity.CLINICAL);
            assertThat(LaboratoryField.RESULT.writeable()).isTrue();
        }

        @Test
        @DisplayName("WardField fields are all INTERNAL sensitivity")
        void wardFieldsAreInternal() {
            for (WardField f : WardField.values()) {
                assertThat(f.sensitivity())
                        .as("WardField.%s should be INTERNAL", f.name())
                        .isEqualTo(FieldSensitivity.INTERNAL);
            }
        }

        @Test
        @DisplayName("AdmissionField clinical fields are CLINICAL sensitivity")
        void admissionClinicalFields() {
            assertThat(AdmissionField.DIAGNOSIS_IN.sensitivity())
                    .isEqualTo(FieldSensitivity.CLINICAL);
            assertThat(AdmissionField.DIAGNOSIS_OUT.sensitivity())
                    .isEqualTo(FieldSensitivity.CLINICAL);
        }
    }
}
