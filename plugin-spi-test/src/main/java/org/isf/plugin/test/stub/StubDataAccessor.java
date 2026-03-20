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

import org.isf.plugin.registry.PluginContext.PatientView;
import org.isf.plugin.registry.PluginContext.PluginAuthorizationException;
import org.isf.plugin.registry.PluginContext.PluginDataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * In-memory implementation of {@link PluginDataAccessor} for unit tests.
 *
 * <p>Backed by a {@link StubPatientStore}. Pre-populate the store before
 * running the test, then verify that the plugin accessed the expected patients:
 *
 * <pre>{@code
 * ctx.patientStore()
 *    .add(42).firstName("Mario").lastName("Rossi").done();
 *
 * plugin.onStart(ctx);
 * ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));
 *
 * // verify access was recorded
 * assertThat(ctx.data().accessedPatientCodes()).containsExactly(42);
 * }</pre>
 *
 * <p>If a requested patient code is not in the store, the consumer is still
 * called with a {@link PatientView} whose fields all return {@code null}
 * (except {@code getPatientCode()}). This mirrors production behaviour where
 * a plugin may receive an event for a patient that has since been deleted.
 */
public final class StubDataAccessor implements PluginDataAccessor {

    private final StubPatientStore store;
    private final List<Integer>    accessLog = new ArrayList<>();

    StubDataAccessor(StubPatientStore store) {
        this.store = store;
    }

    @Override
    public void withPatient(int patientCode, Consumer<PatientView> consumer)
            throws PluginAuthorizationException {
        accessLog.add(patientCode);
        StubPatientStore.PatientEntry entry = store.get(patientCode);
        consumer.accept(new StubPatientView(patientCode, entry));
    }

    // -------------------------------------------------------------------------
    // Inspection API
    // -------------------------------------------------------------------------

    /**
     * Returns the list of patient codes accessed via {@code withPatient()},
     * in the order they were accessed. Useful to verify data access patterns.
     */
    public List<Integer> accessedPatientCodes() {
        return Collections.unmodifiableList(accessLog);
    }

    /** Clears the access log without affecting the patient store. */
    public void clearAccessLog() {
        accessLog.clear();
    }

    // -------------------------------------------------------------------------
    // PatientView implementation
    // -------------------------------------------------------------------------

    private static final class StubPatientView implements PatientView {

        private final int                            code;
        private final StubPatientStore.PatientEntry  entry;

        StubPatientView(int code, StubPatientStore.PatientEntry entry) {
            this.code  = code;
            this.entry = entry;
        }

        @Override public int    getPatientCode() { return code; }
        @Override public String getFirstName()   { return entry != null ? entry.firstName()  : null; }
        @Override public String getLastName()    { return entry != null ? entry.lastName()   : null; }
        @Override public String getBirthDate()   { return entry != null ? entry.birthDate()  : null; }
        @Override public String getSex()         { return entry != null ? entry.sex()        : null; }
        @Override public String getAddress()     { return entry != null ? entry.address()    : null; }
        @Override public String getTelephone()   { return entry != null ? entry.telephone()  : null; }
        @Override public String getBloodType()   { return entry != null ? entry.bloodType()  : null; }
        @Override public String getWardCode()    { return entry != null ? entry.wardCode()   : null; }
    }
}
