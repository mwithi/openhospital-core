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
package org.isf.plugin.model;

/**
 * Lifecycle state of a plugin in the PluginRegistry.
 *
 * <pre>
 * UPLOADED ──► VALIDATING ──► VALIDATED ──► INSTALLING ──► ACTIVE
 *                  │                             │             │
 *                  ▼                             ▼             ▼
 *              REJECTED                       FAILED       DISABLED
 *                                                             │
 *                                                             ▼
 *                                                         UNINSTALLING ──► REMOVED
 * </pre>
 */
public enum PluginStatus {

    /** Package has been uploaded but not yet verified. */
    UPLOADED,

    /** Signature and manifest verification in progress. */
    VALIDATING,

    /** Validation passed; waiting for administrator confirmation before install. */
    VALIDATED,

    /** Installation in progress (DB migration + classloader + Spring registration). */
    INSTALLING,

    /** Plugin is active and running. */
    ACTIVE,

    /** Plugin is installed but disabled by the administrator. */
    DISABLED,

    /** Validation failed (invalid signature, malformed manifest, unauthorized capabilities). */
    REJECTED,

    /** Error during installation or runtime loading. */
    FAILED,

    /** Uninstallation in progress. */
    UNINSTALLING,

    /** Plugin has been completely removed. */
    REMOVED
}
