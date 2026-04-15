/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2026 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
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
package org.isf.plugin.spi;

import org.isf.plugin.event.OHDomainEvents;
import org.isf.plugin.event.OHPluginEvent;
import org.isf.plugin.event.PluginEventBus;
import org.isf.plugin.hook.OHManagerExtension;
import org.isf.plugin.model.PluginPermission;
import org.isf.plugin.registry.PluginContext;
import org.isf.plugin.model.PermissionCheckResult;
import org.isf.plugin.model.PluginCapability;
import org.isf.plugin.model.PluginDescriptor;
import org.isf.plugin.registry.PluginContext.PluginAuthorizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the lifecycle contract of {@link OHPlugin} using in-memory stub
 * implementations — no Spring, no DB.
 *
 * <p>Also demonstrates how a third-party plugin can be tested in isolation.
 */
@DisplayName("OHPlugin lifecycle")
class OHPluginLifecycleTest {

    private StubPluginContext ctx;
    private TrackingPlugin    plugin;

    @BeforeEach
    void setUp() {
        ctx    = new StubPluginContext();
        plugin = new TrackingPlugin();
    }

    @Test
    @DisplayName("onStart is called and the plugin subscribes to events")
    void onStartRegistersEventHandler() throws Exception {
        plugin.onStart(ctx);

        assertThat(plugin.isStarted()).isTrue();
        assertThat(ctx.eventBus().subscribedTypes())
                .contains(OHDomainEvents.PatientCreated.class);
    }

    @Test
    @DisplayName("onStop deregisters all listeners")
    void onStopDeregistersListeners() throws Exception {
        plugin.onStart(ctx);
        plugin.onStop(ctx);

        assertThat(plugin.isStopped()).isTrue();
        assertThat(ctx.eventBus().subscribedTypes()).isEmpty();
    }

    @Test
    @DisplayName("onInstall and onUninstall have default no-op implementations")
    void defaultLifecycleMethods() {
        assertThatNoException().isThrownBy(() -> plugin.onInstall(ctx));
        assertThatNoException().isThrownBy(() -> plugin.onUninstall(ctx));
    }

    @Test
    @DisplayName("plugin reacts correctly to a PatientCreated event (ID-only)")
    void pluginReactsToPatientCreatedEvent() throws Exception {
        plugin.onStart(ctx);

        // PatientCreated now carries only the patientCode — no PII
        OHDomainEvents.PatientCreated event = new OHDomainEvents.PatientCreated(42);

        ctx.eventBus().publish(event);

        assertThat(plugin.getReceivedPatientCodes()).containsExactly(42);
    }

    @Test
    @DisplayName("check() returns GRANTED when both plugin and user are authorized")
    void checkReturnsGrantedByDefault() {
        assertThat(ctx.check(PluginPermission.READ_PATIENT))
                .isEqualTo(PermissionCheckResult.GRANTED);
    }

    @Test
    @DisplayName("check() returns DENIED_PLUGIN when plugin lacks permission")
    void checkReturnsDeniedPluginWhenPluginLacks() {
        StubPluginContext deniedCtx = new StubPluginContext(PermissionCheckResult.DENIED_PLUGIN);

        assertThat(deniedCtx.check(PluginPermission.READ_PATIENT))
                .isEqualTo(PermissionCheckResult.DENIED_PLUGIN);
    }

    @Test
    @DisplayName("check() returns DENIED_USER when user lacks role")
    void checkReturnsDeniedUserWhenUserLacks() {
        StubPluginContext deniedCtx = new StubPluginContext(PermissionCheckResult.DENIED_USER);

        assertThat(deniedCtx.check(PluginPermission.READ_PATIENT))
                .isEqualTo(PermissionCheckResult.DENIED_USER);
    }

    @Test
    @DisplayName("isDenied() is true for both DENIED_PLUGIN and DENIED_USER")
    void isDeniedCoversAllDeniedStates() {
        assertThat(PermissionCheckResult.DENIED_PLUGIN.isDenied()).isTrue();
        assertThat(PermissionCheckResult.DENIED_USER.isDenied()).isTrue();
        assertThat(PermissionCheckResult.GRANTED.isDenied()).isFalse();
    }

    @Test
    @DisplayName("PluginAuthorizationException carries the check result")
    void authExceptionCarriesCheckResult() {
        PluginAuthorizationException ex = new PluginAuthorizationException(
                "org.isf.test",
                PluginPermission.READ_PATIENT,
                PermissionCheckResult.DENIED_USER);

        assertThat(ex.getCheckResult()).isEqualTo(PermissionCheckResult.DENIED_USER);
        assertThat(ex.getMessage()).contains("role");
    }

    @Test
    @DisplayName("files() throws UnsupportedOperationException by default")
    void filesThrowsWhenCapabilityNotDeclared() {
        assertThatThrownBy(() -> ctx.files())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // =========================================================================
    // Stub implementations
    // =========================================================================

    /** Test plugin that tracks lifecycle method invocations. */
    static final class TrackingPlugin implements OHPlugin {

        private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
                .pluginId("org.isf.test.tracking")
                .version("1.0.0")
                .name("Tracking Test Plugin")
                .entryPoint("org.isf.plugin.spi.OHPluginLifecycleTest$TrackingPlugin")
                .minCoreVersion("1.15.0")
                .capabilities(List.of(PluginCapability.EVENT_LISTENER))
                .build();

        private boolean             started          = false;
        private boolean             stopped          = false;
        private final List<Integer> receivedPatients = new ArrayList<>();

        @Override
        public PluginDescriptor getDescriptor() { return DESCRIPTOR; }

        @Override
        public void onStart(PluginContext ctx) {
            ctx.eventBus().subscribe(OHDomainEvents.PatientCreated.class,
                    event -> receivedPatients.add(event.getPatientCode()));
            started = true;
        }

        @Override
        public void onStop(PluginContext ctx) {
            ctx.eventBus().unsubscribeAll();
            stopped = true;
        }

        public boolean       isStarted()               { return started; }
        public boolean       isStopped()               { return stopped; }
        public List<Integer> getReceivedPatientCodes() { return List.copyOf(receivedPatients); }
    }

    /** In-memory implementation of PluginEventBus for tests. */
    static final class StubEventBus implements PluginEventBus {

        @SuppressWarnings("rawtypes")
        private final Map<Class<?>, List<Consumer>> handlers = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <E extends OHPluginEvent> void subscribe(Class<E> eventType, Consumer<E> handler) {
            handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        }

        @Override
        public <E extends OHPluginEvent> void unsubscribe(Class<E> eventType) {
            handlers.remove(eventType);
        }

        @Override
        public void unsubscribeAll() { handlers.clear(); }

        @Override
        @SuppressWarnings("unchecked")
        public void publish(OHPluginEvent event) {
            List<Consumer> eventHandlers = handlers.getOrDefault(event.getClass(), List.of());
            for (Consumer h : eventHandlers) {
                h.accept(event);
            }
        }

        public Set<Class<?>> subscribedTypes() { return handlers.keySet(); }
    }

    /** Stub implementation of PluginContext for tests. */
    static final class StubPluginContext implements PluginContext {

        private final StubEventBus        bus;
        private final StubManagerRegistry managers;
        private final Logger              logger;
        private final PermissionCheckResult checkResult;

        StubPluginContext() { this(PermissionCheckResult.GRANTED); }

        StubPluginContext(PermissionCheckResult checkResult) {
            this.bus         = new StubEventBus();
            this.managers    = new StubManagerRegistry();
            this.logger      = LoggerFactory.getLogger("test.plugin");
            this.checkResult = checkResult;
        }

        @Override
        public PluginDescriptor getDescriptor() {
            return PluginDescriptor.builder()
                    .pluginId("org.isf.test")
                    .version("1.0.0")
                    .name("Test Plugin")
                    .entryPoint("org.isf.test.TestPlugin")
                    .minCoreVersion("1.15.0")
                    .build();
        }

        @Override public StubEventBus             eventBus()   { return bus; }
        @Override public ManagerExtensionRegistry managers()   { return managers; }
        @Override public Logger                   logger()     { return logger; }
        @Override public PluginConfig             config()     { return new StubConfig(); }
        @Override public PluginDataAccessor       data()       { return new StubDataAccessor(); }
        @Override public PluginHttpClient         httpClient() { return new StubHttpClient(); }

        @Override
        public PermissionCheckResult check(PluginPermission permission) {
            return checkResult;
        }
    }

    static final class StubManagerRegistry implements PluginContext.ManagerExtensionRegistry {
        private final List<OHManagerExtension> registered = new ArrayList<>();

        @Override public void register(OHManagerExtension ext) { registered.add(ext); }
        @Override public void unregisterAll()                   { registered.clear(); }
    }

    static final class StubConfig implements PluginContext.PluginConfig {
        @Override public String  getString(String key, String def)   { return def; }
        @Override public int     getInt(String key, int def)         { return def; }
        @Override public boolean getBoolean(String key, boolean def) { return def; }
    }

    static final class StubDataAccessor implements PluginContext.PluginDataAccessor {
        @Override
        public void withPatient(int patientCode, Consumer<PluginContext.PatientView> consumer)
                throws PluginAuthorizationException {
            consumer.accept(new StubPatientView(patientCode));
        }
    }

    static final class StubPatientView implements PluginContext.PatientView {
        private final int code;
        StubPatientView(int code) { this.code = code; }

        @Override public int    getPatientCode() { return code; }
        @Override public String getFirstName()   { return "Test"; }
        @Override public String getLastName()    { return "Patient"; }
        @Override public String getBirthDate()   { return "1980-01-01"; }
        @Override public String getSex()         { return null; }
        @Override public String getAddress()     { return null; }
        @Override public String getTelephone()   { return null; }
        @Override public String getBloodType()   { return null; }
        @Override public String getWardCode()    { return null; }
    }

    static final class StubHttpClient implements PluginContext.PluginHttpClient {
        @Override public String get(String url)              { return "{}"; }
        @Override public String post(String url, String body){ return "{}"; }
    }
}
