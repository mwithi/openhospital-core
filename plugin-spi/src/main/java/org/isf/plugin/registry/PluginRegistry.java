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
package org.isf.plugin.registry;

import org.isf.plugin.model.PluginDescriptor;
import org.isf.plugin.model.PluginStatus;
import org.isf.plugin.spi.OHPlugin;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Central registry for Open Hospital plugins.
 *
 * <p>This SPI interface defines the contract of the PluginManager.
 * The concrete implementation ({@code PluginRegistryImpl}) lives in
 * {@code openhospital-api} as a Spring {@code @Service} bean.
 *
 * <p>This interface is in {@code plugin-spi} for two reasons:
 * <ol>
 *   <li>Allows unit tests to mock the registry without depending on Spring.</li>
 *   <li>Allows advanced plugins to query the state of other plugins via
 *       {@link PluginContext}.</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * All mutating operations (install, enable, disable, uninstall) are
 * synchronized internally by the implementation. Read-only operations
 * ({@link #getPlugin}, {@link #listPlugins}) are safe for concurrent reads.
 */
public interface PluginRegistry {

    /**
     * Installs a plugin from the provided package stream.
     *
     * <p>The installation process, in order:
     * <ol>
     *   <li>Compute SHA-256 checksum of the package</li>
     *   <li>Apply all registered {@link org.isf.plugin.security.PluginSecurityPolicy} instances</li>
     *   <li>Extract and parse {@code manifest.json}</li>
     *   <li>Run DB migrations (if {@code DB_MIGRATION} is declared)</li>
     *   <li>Load the JAR into the plugin classloader</li>
     *   <li>Instantiate the {@code entryPoint} class</li>
     *   <li>Call {@link OHPlugin#onInstall(PluginContext)}</li>
     *   <li>Call {@link OHPlugin#onStart(PluginContext)}</li>
     *   <li>Persist {@code ACTIVE} state to the database</li>
     * </ol>
     *
     * <p>If any step fails, the process is rolled back and the plugin is
     * left in {@code FAILED} state.
     *
     * @param packageStream stream of the .zip or .tar.gz package
     * @param filename      original filename (used to determine the archive format)
     * @return the {@link PluginDescriptor} of the installed plugin
     * @throws PluginInstallException if installation fails for any reason
     */
    PluginDescriptor install(InputStream packageStream, String filename)
            throws PluginInstallException;

    /**
     * Enables a previously disabled plugin.
     * Calls {@link OHPlugin#onStart(PluginContext)} if not already active.
     *
     * @param pluginId the plugin identifier
     * @throws PluginInstallException if the plugin does not exist or cannot be enabled
     */
    void enable(String pluginId) throws PluginInstallException;

    /**
     * Disables an active plugin without removing it.
     * Calls {@link OHPlugin#onStop(PluginContext)}.
     *
     * @param pluginId the plugin identifier
     * @throws PluginInstallException if the plugin does not exist or cannot be disabled
     */
    void disable(String pluginId) throws PluginInstallException;

    /**
     * Uninstalls a plugin, removing all its resources.
     *
     * <p>The uninstall process:
     * <ol>
     *   <li>Call {@link OHPlugin#onStop(PluginContext)}</li>
     *   <li>Call {@link OHPlugin#onUninstall(PluginContext)}</li>
     *   <li>Drop plugin DB tables (optional, configurable via {@code removeData})</li>
     *   <li>Remove the JAR and assets from the filesystem</li>
     *   <li>Update state to {@code REMOVED}</li>
     * </ol>
     *
     * @param pluginId   the plugin identifier
     * @param removeData if {@code true}, also drops the plugin's DB tables
     * @throws PluginInstallException if the plugin does not exist or uninstall fails
     */
    void uninstall(String pluginId, boolean removeData) throws PluginInstallException;

    /**
     * Returns the descriptor of a plugin by ID.
     *
     * @param pluginId the plugin identifier
     * @return an {@link Optional} containing the descriptor, or empty if not found
     */
    Optional<PluginDescriptor> getPlugin(String pluginId);

    /**
     * Returns the current status of a plugin.
     *
     * @param pluginId the plugin identifier
     * @return an {@link Optional} with the status, or empty if the plugin does not exist
     */
    Optional<PluginStatus> getStatus(String pluginId);

    /**
     * Lists all registered plugins in any state.
     *
     * @return immutable list of all descriptors
     */
    List<PluginDescriptor> listPlugins();

    /**
     * Lists plugins currently in {@code ACTIVE} state.
     *
     * @return immutable list of active plugin descriptors
     */
    List<PluginDescriptor> listActivePlugins();

    // =========================================================================
    // Exception
    // =========================================================================

    /**
     * Exception thrown when a registry operation fails.
     */
    class PluginInstallException extends Exception {

        private final String pluginId;

        public PluginInstallException(String pluginId, String message) {
            super("[" + pluginId + "] " + message);
            this.pluginId = pluginId;
        }

        public PluginInstallException(String pluginId, String message, Throwable cause) {
            super("[" + pluginId + "] " + message, cause);
            this.pluginId = pluginId;
        }

        public String getPluginId() { return pluginId; }
    }
}
