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
package org.isf.plugin.model.ui;

import java.util.Objects;

/**
 * Locates the JavaScript/TypeScript bundle that implements the plugin's UI.
 *
 * <h3>Module Federation</h3>
 * OH-ui uses Vite with {@code @originjs/vite-plugin-federation} (Module Federation).
 * Each plugin is a <em>remote</em> module. The host ({@code openhospital-ui})
 * loads the plugin bundle at runtime using the {@link #remoteName} as the
 * federation remote identifier.
 *
 * <h3>Bundle location in the plugin package</h3>
 * The bundle file must be present in the plugin ZIP/TAR.GZ at the path
 * specified by {@link #entry}. The PluginManager extracts it and serves it
 * as a static asset from {@code /api/plugins/<pluginId>/ui/<entry>}.
 *
 * <h3>Security</h3>
 * Bundle files are served with a strict Content Security Policy that restricts
 * the origins from which the plugin JS can load additional resources.
 * The allowed origins are derived from the plugin's {@code externalConnections}
 * declarations — no undeclared external resource can be loaded.
 *
 * @param entry      path to the bundle entry file inside the plugin ZIP,
 *                   e.g. {@code "ui/radiology-plugin.js"}
 * @param remoteName Module Federation remote name, e.g. {@code "radiologyPlugin"}.
 *                   Must be a valid JavaScript identifier (no hyphens, no dots).
 */
public record BundleDescriptor(
        String entry,
        String remoteName
) {
    private static final java.util.regex.Pattern JS_IDENTIFIER =
            java.util.regex.Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");

    public BundleDescriptor {
        Objects.requireNonNull(entry,      "BundleDescriptor.entry must not be null");
        Objects.requireNonNull(remoteName, "BundleDescriptor.remoteName must not be null");

        if (entry.isBlank()) {
            throw new IllegalArgumentException(
                "BundleDescriptor.entry must not be blank");
        }
        if (!JS_IDENTIFIER.matcher(remoteName).matches()) {
            throw new IllegalArgumentException(
                "BundleDescriptor.remoteName must be a valid JavaScript identifier " +
                "(letters, digits, _ and $ only, cannot start with digit): " + remoteName);
        }
    }
}
