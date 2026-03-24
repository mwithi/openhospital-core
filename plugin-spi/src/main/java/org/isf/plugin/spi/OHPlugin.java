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
package org.isf.plugin.spi;

import org.isf.plugin.event.OHDomainEvents;
import org.isf.plugin.model.PluginDescriptor;
import org.isf.plugin.registry.PluginContext;

/**
 * Primary SPI interface that every Open Hospital plugin must implement.
 *
 * <p>The class implementing this interface is the one specified in the
 * {@code entryPoint} field of {@code manifest.json}.
 *
 * <h3>Source of truth: manifest.json</h3>
 * The plugin descriptor is defined exclusively in {@code manifest.json},
 * which is read by the PluginManager before any plugin code runs.
 * The plugin accesses its own descriptor via {@link PluginContext#getDescriptor()}.
 * There is no {@code getDescriptor()} method on this interface — the manifest
 * is the single source of truth, with no risk of divergence between code and JSON.
 *
 * <h3>Lifecycle contract</h3>
 * <ol>
 *   <li>{@link #onInstall(PluginContext)} — called exactly once at first install.</li>
 *   <li>{@link #onStart(PluginContext)} — called on every system startup when ACTIVE.</li>
 *   <li>{@link #onStop(PluginContext)} — called before shutdown or disabling.</li>
 *   <li>{@link #onUninstall(PluginContext)} — called exactly once before removal.</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * The PluginManager guarantees that lifecycle methods are never called
 * concurrently on the same plugin instance.
 *
 * <h3>Minimal implementation example</h3>
 * <pre>{@code
 * public class RadiologyPlugin implements OHPlugin {
 *
 *     public void onStart(PluginContext ctx) throws OHPluginLifecycleException {
 *         ctx.logger().info("Starting {}", ctx.getDescriptor().getName());
 *         ctx.eventBus().subscribe(
 *             OHDomainEvents.PatientCreated.class,
 *             event -> ctx.logger().info("New patient: {}", event.getPatientCode()));
 *     }
 * }
 * }</pre>
 */
public interface OHPlugin {

    /*
     * Implementations must provide a public no-argument constructor.
     * The plugin-maven-plugin uses it during mvn package to call
     * getDescriptor() via reflection and generate manifest.json.
     */

    /**
     * Returns the descriptor of this plugin.
     *
     * <p>Used by the {@code plugin-maven-plugin} during {@code mvn package}
     * to generate {@code manifest.json} via reflection — the Mojo instantiates
     * the plugin with its no-argument constructor and calls this method.
     *
     * <p>At runtime, {@code PluginContextImpl} cross-checks the returned
     * descriptor against the manifest read from the JAR to verify consistency.
     *
     * @return the immutable {@link PluginDescriptor}, never null
     */
    PluginDescriptor getDescriptor();

    /**
     * Called exactly once when the plugin is installed for the first time.
     *
     * <p>If this method throws, the installation is rolled back and the plugin
     * is left in {@code FAILED} state. DB migrations (if any) are executed
     * before this call.
     *
     * <p>Default implementation is a no-op.
     *
     * @param ctx context provided by the runtime
     * @throws OHPluginLifecycleException if initial setup cannot be completed
     */
    default void onInstall(PluginContext ctx) throws OHPluginLifecycleException {
    }

    /**
     * Called on every system startup when the plugin is in ACTIVE state.
     *
     * <p>Register services here:
     * <ul>
     *   <li>Event subscriptions via {@link PluginContext#eventBus()}</li>
     *   <li>Manager extensions via {@link PluginContext#managers()}</li>
     * </ul>
     *
     * @param ctx context provided by the runtime
     * @throws OHPluginLifecycleException if the plugin cannot start
     */
    void onStart(PluginContext ctx) throws OHPluginLifecycleException;

    /**
     * Called when the plugin is stopped (system shutdown or disabling).
     *
     * <p>Release all resources acquired in {@link #onStart}: deregister
     * listeners, close connections, stop background threads.
     *
     * <p>Default implementation is a no-op.
     *
     * @param ctx context provided by the runtime
     */
    default void onStop(PluginContext ctx) {
    }

    /**
     * Called exactly once before the plugin is permanently removed.
     *
     * <p>Default implementation is a no-op.
     *
     * @param ctx context provided by the runtime
     */
    default void onUninstall(PluginContext ctx) {
    }
}
