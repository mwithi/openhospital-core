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

import org.isf.plugin.hook.OHManagerExtension;
import org.isf.plugin.model.PermissionCheckResult;
import org.isf.plugin.model.PluginCapability;
import org.isf.plugin.model.PluginDescriptor;
import org.isf.plugin.model.PluginPermission;
import org.isf.plugin.registry.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of {@link PluginContext} for unit tests.
 *
 * <p>Provides realistic stub implementations of all sub-interfaces.
 * By default all permission checks return {@link PermissionCheckResult#GRANTED}.
 *
 * <h3>Minimal usage</h3>
 * <pre>{@code
 * StubPluginContext ctx = StubPluginContext.fromManifest("/manifest.json");
 * MyPlugin plugin = new MyPlugin();
 * plugin.onStart(ctx);
 *
 * ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));
 * assertThat(plugin.getProcessedCodes()).containsExactly(42);
 * }</pre>
 *
 * <h3>Testing with data</h3>
 * <pre>{@code
 * ctx.patientStore()
 *    .add(42).firstName("Mario").lastName("Rossi").done();
 *
 * plugin.onStart(ctx);
 * ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));
 * assertThat(ctx.data().accessedPatientCodes()).containsExactly(42);
 * }</pre>
 *
 * <h3>Testing permission denial</h3>
 * <pre>{@code
 * StubPluginContext ctx =
 *     StubPluginContext.builder()
 *         .permissionResult(PermissionCheckResult.DENIED_USER)
 *         .build();
 * }</pre>
 */
public final class StubPluginContext implements PluginContext {

    private final PluginDescriptor      descriptor;
    private final StubEventBus          eventBus;
    private final StubDataAccessor      data;
    private final StubPatientStore      patientStore;
    private final StubHttpClient        httpClient;
    private final StubManagerRegistry   managers;
    private final StubConfig            config;
    private final Logger                logger;
    private final PermissionCheckResult permissionResult;
    private final StubFileAccess          fileAccess;

    private StubPluginContext(Builder builder) {
        this.patientStore     = new StubPatientStore();
        this.eventBus         = new StubEventBus();
        this.data             = new StubDataAccessor(patientStore);
        this.httpClient       = new StubHttpClient();
        this.managers         = new StubManagerRegistry();
        this.config           = new StubConfig();
        this.fileAccess       = new StubFileAccess();
        this.logger           = LoggerFactory.getLogger("stub.plugin." + builder.pluginId);
        this.permissionResult = builder.permissionResult;
        this.descriptor       = PluginDescriptor.builder()
                .pluginId(builder.pluginId)
                .version(builder.version)
                .name(builder.name)
                .entryPoint(builder.pluginId + ".StubPlugin")
                .minCoreVersion(builder.minCoreVersion)
                .capabilities(builder.capabilities)
                .build();
    }

    /**
     * Creates a context whose descriptor is read from the given classpath resource.
     *
     * <p>This is the recommended way to test a plugin — it uses the real
     * {@code manifest.json} as the source of truth, so the test automatically
     * reflects any change to the manifest without requiring test code updates.
     *
     * <pre>{@code
     * StubPluginContext ctx = StubPluginContext.fromManifest("/manifest.json");
     * }</pre>
     *
     * @param resourcePath classpath path to the manifest, e.g. {@code "/manifest.json"}
     * @return a fully configured stub context
     * @throws IllegalArgumentException if the resource is not found or is malformed
     */
    public static StubPluginContext fromManifest(String resourcePath) {
        PluginDescriptor descriptor = ManifestReader.read(resourcePath);
        return new Builder()
                .pluginId(descriptor.getPluginId())
                .version(descriptor.getVersion())
                .name(descriptor.getName())
                .minCoreVersion(descriptor.getMinCoreVersion())
                .capabilities(descriptor.getCapabilities())
                .build();
    }

    /** Creates a context with all defaults: pluginId "org.isf.test", all permissions GRANTED. */
    public StubPluginContext() {
        this(new Builder());
    }

    /** Creates a context where every {@code check()} call returns the given result. */
    public StubPluginContext(PermissionCheckResult permissionResult) {
        this(new Builder().permissionResult(permissionResult));
    }

    // -------------------------------------------------------------------------
    // PluginContext contract
    // -------------------------------------------------------------------------

    @Override public PluginDescriptor      getDescriptor() { return descriptor; }
    @Override public StubEventBus          eventBus()      { return eventBus; }
    @Override public ManagerExtensionRegistry managers()   { return managers; }
    @Override public StubDataAccessor      data()          { return data; }
    @Override public StubHttpClient        httpClient()    { return httpClient; }
    @Override public PluginConfig          config()        { return config; }
    @Override public Logger                logger()        { return logger; }

    @Override
    public PermissionCheckResult check(PluginPermission permission) {
        return permissionResult;
    }

    // -------------------------------------------------------------------------
    // Extra accessors for test setup
    // -------------------------------------------------------------------------

    @Override
    public StubFileAccess files() {
        if (!descriptor.getCapabilities().contains(PluginCapability.LOG_FILE_WRITE)) {
            throw new UnsupportedOperationException(
                "This plugin did not declare PluginCapability.LOG_FILE_WRITE");
        }
        return fileAccess;
    }

    /**
     * Returns the patient store for pre-populating test data.
     * Changes to the store affect subsequent {@code withPatient()} calls.
     */
    public StubPatientStore patientStore() {
        return patientStore;
    }

    // -------------------------------------------------------------------------
    // Nested stubs
    // -------------------------------------------------------------------------

    private static final class StubManagerRegistry implements ManagerExtensionRegistry {
        private final List<OHManagerExtension> registered = new ArrayList<>();

        @Override public void register(OHManagerExtension ext) { registered.add(ext); }
        @Override public void unregisterAll()                   { registered.clear(); }

        public List<OHManagerExtension> getRegistered() {
            return Collections.unmodifiableList(registered);
        }
    }

    private static final class StubConfig implements PluginConfig {
        private final Map<String, String> values = new HashMap<>();

        @Override
        public String getString(String key, String defaultValue) {
            return values.getOrDefault(key, defaultValue);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            String v = values.get(key);
            return v != null ? Integer.parseInt(v) : defaultValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            String v = values.get(key);
            return v != null ? Boolean.parseBoolean(v) : defaultValue;
        }

        /** Pre-loads a config value for the test. */
        public void set(String key, String value) {
            values.put(key, value);
        }
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String               pluginId         = "org.isf.test";
        private String               version          = "1.0.0";
        private String               name             = "Test Plugin";
        private String               minCoreVersion   = "1.15.0";
        private List<PluginCapability> capabilities   = List.of();
        private PermissionCheckResult permissionResult = PermissionCheckResult.GRANTED;

        public Builder pluginId(String v)           { this.pluginId         = v; return this; }
        public Builder version(String v)            { this.version          = v; return this; }
        public Builder name(String v)               { this.name             = v; return this; }
        public Builder minCoreVersion(String v)     { this.minCoreVersion   = v; return this; }
        public Builder capabilities(List<PluginCapability> v) {
            this.capabilities = v; return this;
        }
        public Builder permissionResult(PermissionCheckResult v) {
            this.permissionResult = v; return this;
        }

        public StubPluginContext build() {
            return new StubPluginContext(this);
        }
    }
}
