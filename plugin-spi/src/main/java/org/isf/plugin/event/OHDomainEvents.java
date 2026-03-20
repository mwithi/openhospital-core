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
package org.isf.plugin.event;

/**
 * Catalogue of standard domain events published by Open Hospital Core.
 *
 * <p>Each inner class is an immutable, concrete event.
 * Plugins subscribe to these events via
 * {@link PluginEventBus#subscribe(Class, java.util.function.Consumer)}.
 *
 * <h3>Privacy by design — ID-only events</h3>
 * <p>Events carry <strong>only opaque identifiers</strong>, never PII or
 * clinical data. This is a deliberate privacy-by-design constraint:
 * <ul>
 *   <li>A plugin with {@code EVENT_LISTENER} cannot harvest patient data
 *       simply by subscribing to events.</li>
 *   <li>If the plugin needs the actual data, it must explicitly call
 *       {@code PluginContext.data().withPatient(patientCode, ...)},
 *       which requires {@code READ_PATIENT} permission and leaves an
 *       audit trail.</li>
 * </ul>
 *
 * <p>New events may be added in future core releases without breaking
 * backward compatibility. Plugins that do not subscribe to a given event
 * simply ignore it.
 */
public final class OHDomainEvents {

    private OHDomainEvents() {}

    // =========================================================================
    // Patient
    // =========================================================================

    /**
     * Published after a new patient has been successfully created.
     * Carries only the patient code — use
     * {@code PluginContext.data().withPatient()} to access demographics.
     */
    public static final class PatientCreated extends OHPluginEvent {
        private final int patientCode;

        public PatientCreated(int patientCode) {
            super("PATIENT_CREATED", "openhospital-core");
            this.patientCode = patientCode;
        }

        public int getPatientCode() { return patientCode; }
    }

    /**
     * Published after a patient's data has been updated.
     * Carries only the patient code.
     */
    public static final class PatientUpdated extends OHPluginEvent {
        private final int patientCode;

        public PatientUpdated(int patientCode) {
            super("PATIENT_UPDATED", "openhospital-core");
            this.patientCode = patientCode;
        }

        public int getPatientCode() { return patientCode; }
    }

    /**
     * Published after a patient has been logically deleted.
     * Carries only the patient code.
     */
    public static final class PatientDeleted extends OHPluginEvent {
        private final int patientCode;

        public PatientDeleted(int patientCode) {
            super("PATIENT_DELETED", "openhospital-core");
            this.patientCode = patientCode;
        }

        public int getPatientCode() { return patientCode; }
    }

    // =========================================================================
    // Admission / Discharge
    // =========================================================================

    /**
     * Published when a patient is admitted to a ward.
     * Carries admission ID, patient code, and ward code.
     * Ward code is not PII — it is an organizational identifier.
     */
    public static final class PatientAdmitted extends OHPluginEvent {
        private final int    admissionId;
        private final int    patientCode;
        private final String wardCode;

        public PatientAdmitted(int admissionId, int patientCode, String wardCode) {
            super("PATIENT_ADMITTED", "openhospital-core");
            this.admissionId = admissionId;
            this.patientCode = patientCode;
            this.wardCode    = wardCode;
        }

        public int    getAdmissionId() { return admissionId; }
        public int    getPatientCode() { return patientCode; }
        public String getWardCode()    { return wardCode; }
    }

    /**
     * Published when a patient is discharged.
     * Discharge type is an organizational code, not PII.
     */
    public static final class PatientDischarged extends OHPluginEvent {
        private final int    admissionId;
        private final int    patientCode;
        private final String dischargeTypeCode;

        public PatientDischarged(int admissionId, int patientCode, String dischargeTypeCode) {
            super("PATIENT_DISCHARGED", "openhospital-core");
            this.admissionId       = admissionId;
            this.patientCode       = patientCode;
            this.dischargeTypeCode = dischargeTypeCode;
        }

        public int    getAdmissionId()       { return admissionId; }
        public int    getPatientCode()       { return patientCode; }
        public String getDischargeTypeCode() { return dischargeTypeCode; }
    }

    // =========================================================================
    // Laboratory
    // =========================================================================

    /**
     * Published when a new laboratory exam is recorded.
     * Exam code is an organizational identifier, not PII.
     */
    public static final class LaboratoryExamCreated extends OHPluginEvent {
        private final int    labId;
        private final int    patientCode;
        private final String examCode;

        public LaboratoryExamCreated(int labId, int patientCode, String examCode) {
            super("LAB_EXAM_CREATED", "openhospital-core");
            this.labId       = labId;
            this.patientCode = patientCode;
            this.examCode    = examCode;
        }

        public int    getLabId()       { return labId; }
        public int    getPatientCode() { return patientCode; }
        public String getExamCode()    { return examCode; }
    }

    // =========================================================================
    // Plugin lifecycle (published by the PluginManager itself)
    // =========================================================================

    /** Published when a plugin has been successfully activated. */
    public static final class PluginActivated extends OHPluginEvent {
        private final String pluginId;
        private final String pluginVersion;

        public PluginActivated(String pluginId, String pluginVersion) {
            super("PLUGIN_ACTIVATED", "openhospital-api");
            this.pluginId      = pluginId;
            this.pluginVersion = pluginVersion;
        }

        public String getPluginId()      { return pluginId; }
        public String getPluginVersion() { return pluginVersion; }
    }

    /** Published when a plugin has been deactivated. */
    public static final class PluginDeactivated extends OHPluginEvent {
        private final String pluginId;
        private final String reason;

        public PluginDeactivated(String pluginId, String reason) {
            super("PLUGIN_DEACTIVATED", "openhospital-api");
            this.pluginId = pluginId;
            this.reason   = reason;
        }

        public String getPluginId() { return pluginId; }
        public String getReason()   { return reason; }
    }
}
