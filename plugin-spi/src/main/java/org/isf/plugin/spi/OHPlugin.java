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
 * {@code entryPoint} field of the {@link PluginDescriptor}.
 *
 * <h3>Lifecycle contract</h3>
 * <ol>
 *   <li>{@link #getDescriptor()} — called immediately after instantiation;
 *       must return the same descriptor as the manifest.json.</li>
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
 *     private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
 *         .pluginId("org.isf.radiology")
 *         .version("1.0.0")
 *         .name("Radiology Module")
 *         .entryPoint("org.isf.radiology.RadiologyPlugin")
 *         .minCoreVersion("1.15.0")
 *         .capabilities(List.of(PluginCapability.API_EXTENSION))
 *         .permissions(List.of(PluginPermission.READ_PATIENT))
 *         .build();
 *
 *     public PluginDescriptor getDescriptor() { return DESCRIPTOR; }
 *
 *     public void onStart(PluginContext ctx) {
 *         ctx.eventBus().subscribe(
 *             OHDomainEvents.PatientCreated.class,
 *             event -> ctx.logger().info("New patient: {}", event.getLastName()));
 *     }
 * }
 * }</pre>
 */
public interface OHPlugin {

    /**
     * Returns the immutable descriptor of this plugin.
     *
     * <p>The recommended implementation is to return a compile-time static
     * constant, not a new object on every call.
     *
     * @return the {@link PluginDescriptor} of this plugin
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
