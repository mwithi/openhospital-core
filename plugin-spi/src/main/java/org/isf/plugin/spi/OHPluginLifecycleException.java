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

/**
 * Checked exception thrown during a plugin lifecycle phase
 * (install, start, stop, uninstall).
 *
 * <p>Using a checked exception forces explicit handling in the PluginManager:
 * every lifecycle error must be logged, the plugin set to {@code FAILED} state,
 * and the error reported to the administrator — never silently swallowed.
 */
public class OHPluginLifecycleException extends Exception {

    private final String pluginId;
    private final LifecyclePhase phase;

    /** The lifecycle phase during which the exception occurred. */
    public enum LifecyclePhase {
        INSTALL, START, STOP, UNINSTALL
    }

    public OHPluginLifecycleException(String pluginId, LifecyclePhase phase, String message) {
        super("[" + pluginId + " / " + phase + "] " + message);
        this.pluginId = pluginId;
        this.phase    = phase;
    }

    public OHPluginLifecycleException(String pluginId, LifecyclePhase phase,
                                      String message, Throwable cause) {
        super("[" + pluginId + " / " + phase + "] " + message, cause);
        this.pluginId = pluginId;
        this.phase    = phase;
    }

    public String         getPluginId() { return pluginId; }
    public LifecyclePhase getPhase()    { return phase; }
}
