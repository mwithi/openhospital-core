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

import java.sql.Connection;

/**
 * Descriptor for a DB migration belonging to a plugin that declares
 * {@link org.isf.plugin.model.PluginCapability#DB_MIGRATION}.
 *
 * <h3>SQL-based usage (recommended)</h3>
 * Place scripts in {@code db/migration/} inside the plugin JAR using Flyway's
 * naming convention: {@code V1__Create_table.sql}. Table names must be prefixed
 * with {@code p_<pluginId>_} (e.g. {@code p_org_isf_radiology_studies}) to
 * avoid conflicts with OH-core tables.
 *
 * <h3>Programmatic usage (optional)</h3>
 * Implement this interface only when the migration requires Java logic beyond
 * plain SQL. The runner calls it after the SQL scripts of the same version.
 */
public interface MigrationScript {

    /**
     * Flyway-compatible version string, e.g. {@code "1"}, {@code "1.1"}, {@code "2"}.
     * Must be unique within the plugin.
     *
     * @return version string, never null
     */
    String getVersion();

    /**
     * Human-readable description used in the Flyway history log,
     * e.g. {@code "Create radiology studies table"}.
     *
     * @return description, never null
     */
    String getDescription();

    /**
     * Executes the programmatic migration.
     *
     * <p>Do not call commit or rollback — Flyway manages the transaction.
     *
     * @param context execution context providing access to the DB connection
     * @throws Exception if the migration cannot be completed
     */
    void migrate(MigrationContext context) throws Exception;

    // -------------------------------------------------------------------------
    // Inner interface
    // -------------------------------------------------------------------------

    /**
     * Context passed to {@link #migrate(MigrationContext)}.
     * Provides access to the JDBC connection without exposing Spring or JPA.
     */
    interface MigrationContext {

        /**
         * The active JDBC connection. Do not close it.
         *
         * @return the current {@link Connection}
         */
        Connection getConnection();

        /**
         * Identifier of the plugin being migrated, e.g. {@code "org.isf.radiology"}.
         *
         * @return pluginId, never null
         */
        String getPluginId();

        /**
         * Prefix that the plugin must use for its table names,
         * computed as {@code "p_" + pluginId.replace('.', '_') + "_"}.
         * Example: {@code "p_org_isf_radiology_"}.
         *
         * @return table prefix, never null
         */
        String getTablePrefix();
    }
}
