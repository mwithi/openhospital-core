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
package org.isf.plugin.model;

/**
 * Capabilities that a plugin may declare in its {@link PluginDescriptor}.
 *
 * <p>The security system uses this enum as an allowlist: a plugin that attempts
 * to register REST APIs without having declared {@code API_EXTENSION} is
 * rejected at install time, not at runtime.
 *
 * <p>Separating capabilities into distinct values allows fine-grained
 * permission granting: a reporting plugin may have {@code DB_READ} without
 * {@code DB_MIGRATION}; a notification plugin may have {@code EVENT_LISTENER}
 * without touching the database at all.
 */
public enum PluginCapability {

    /**
     * The plugin registers new REST endpoints in openhospital-api.
     * Requires the package to contain a JAR with {@code @RestController} classes.
     */
    API_EXTENSION,

    /**
     * The plugin runs SQL migrations (Flyway/Liquibase) to create its own tables.
     * Scripts must be placed in {@code db/migration/} inside the plugin JAR,
     * prefixed with {@code p_<pluginId>_} to avoid conflicts with OH-core tables.
     */
    DB_MIGRATION,

    /**
     * The plugin reads data from the database via its own queries/repositories.
     * Does not imply write access.
     */
    DB_READ,

    /**
     * The plugin writes data to the database (into its own tables).
     * Implies {@code DB_READ}.
     */
    DB_WRITE,

    /**
     * The plugin registers new routes and pages in the React UI.
     * Requires the package to contain a JS/TS bundle.
     */
    UI_ROUTES,

    /**
     * The plugin overrides or extends existing UI components via the
     * {@code PluginSlot} mechanism.
     * Requires the package to contain a JS/TS bundle.
     */
    UI_COMPONENT_OVERRIDE,

    /**
     * The plugin reacts to domain events published by the system
     * (e.g. patient created, admission completed).
     * Does not imply direct database access.
     *
     * @see org.isf.plugin.event.OHPluginEvent
     */
    EVENT_LISTENER,

    /**
     * The plugin publishes custom domain events that other plugins
     * or the core system can subscribe to.
     */
    EVENT_PUBLISHER,

    /**
     * The plugin accesses the reporting API (JasperReports).
     * May add new report templates.
     */
    REPORTING,

    /**
     * The plugin makes calls to external systems (HTTP, HL7, FHIR, etc.).
     * Requires explicit configuration of allowed endpoints in the plugin policy file.
     */
    EXTERNAL_INTEGRATION,

    /**
     * The plugin writes to its designated log directory on the OH server filesystem.
     * The directory is isolated per plugin and configured in {@code settings.properties}
     * via {@code plugin.log.dir}. The plugin can only write relative paths inside
     * that directory — it cannot access any other path on the filesystem.
     *
     * <p>At runtime the absolute path is {@code ${plugin.log.dir}/${pluginId}/}.
     * The plugin never sees the absolute path — it only uses relative names via
     * {@link org.isf.plugin.registry.PluginContext#files()}.
     */
    LOG_FILE_WRITE
}
