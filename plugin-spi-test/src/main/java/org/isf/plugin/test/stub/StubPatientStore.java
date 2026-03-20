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
package org.isf.plugin.test.stub;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory patient store used by {@link StubDataAccessor}.
 *
 * <p>Populate it before running a test, then verify the plugin's reactions:
 *
 * <pre>{@code
 * ctx.patientStore()
 *    .add(42)
 *    .firstName("Mario").lastName("Rossi")
 *    .birthDate("1970-06-15").sex("M")
 *    .done();
 *
 * plugin.onStart(ctx);
 * ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));
 *
 * assertThat(plugin.getLastProcessedName()).isEqualTo("Mario Rossi");
 * }</pre>
 */
public final class StubPatientStore {

    /** Internal representation of a patient entry. */
    public static final class PatientEntry {
        private final int    patientCode;
        private String firstName;
        private String lastName;
        private String birthDate;
        private String sex;
        private String address;
        private String telephone;
        private String bloodType;
        private String wardCode;

        private final StubPatientStore store;

        PatientEntry(int patientCode, StubPatientStore store) {
            this.patientCode = patientCode;
            this.store       = store;
        }

        public PatientEntry firstName(String v)  { this.firstName  = v; return this; }
        public PatientEntry lastName(String v)   { this.lastName   = v; return this; }
        public PatientEntry birthDate(String v)  { this.birthDate  = v; return this; }
        public PatientEntry sex(String v)        { this.sex        = v; return this; }
        public PatientEntry address(String v)    { this.address    = v; return this; }
        public PatientEntry telephone(String v)  { this.telephone  = v; return this; }
        public PatientEntry bloodType(String v)  { this.bloodType  = v; return this; }
        public PatientEntry wardCode(String v)   { this.wardCode   = v; return this; }

        /** Finalises the entry and adds it to the store. */
        public StubPatientStore done() {
            store.entries.put(patientCode, this);
            return store;
        }

        // package-private getters used by StubPatientView
        int    patientCode() { return patientCode; }
        String firstName()   { return firstName; }
        String lastName()    { return lastName; }
        String birthDate()   { return birthDate; }
        String sex()         { return sex; }
        String address()     { return address; }
        String telephone()   { return telephone; }
        String bloodType()   { return bloodType; }
        String wardCode()    { return wardCode; }
    }

    private final Map<Integer, PatientEntry> entries = new HashMap<>();

    /**
     * Starts building a new patient entry with the given code.
     * Call {@link PatientEntry#done()} to commit it to the store.
     */
    public PatientEntry add(int patientCode) {
        return new PatientEntry(patientCode, this);
    }

    /** Returns the entry for the given patient code, or {@code null} if absent. */
    PatientEntry get(int patientCode) {
        return entries.get(patientCode);
    }

    /** Returns an unmodifiable view of all stored entries. */
    public Map<Integer, PatientEntry> all() {
        return Collections.unmodifiableMap(entries);
    }

    /** Removes all entries from the store. */
    public void clear() {
        entries.clear();
    }
}
