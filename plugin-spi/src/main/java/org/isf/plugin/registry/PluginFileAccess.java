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
package org.isf.plugin.registry;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

/**
 * Sandboxed filesystem access for plugins that declare
 * {@link org.isf.plugin.model.PluginCapability#LOG_FILE_WRITE}.
 *
 * <p>A plugin can only write files inside its designated log directory.
 * The absolute path of that directory is configured in
 * {@code settings.properties} via {@code plugin.log.dir} and resolved
 * by {@code PluginContextImpl} as {@code ${plugin.log.dir}/${pluginId}/}.
 * The plugin never sees or constructs the absolute path — it only works
 * with relative names via this interface.
 *
 * <p>All path traversal attempts (e.g. {@code "../other-plugin/secret.log"})
 * are rejected with {@link IllegalArgumentException} by the implementation.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * try (Writer w = ctx.files().openLogWriter("audit.log", true)) {
 *     w.write("[2024-01-15 10:23] CREATED - Mario Rossi (code: 42)\n");
 * }
 * }</pre>
 */
public interface PluginFileAccess {

    /**
     * Opens a {@link Writer} to a file inside the plugin's log directory.
     *
     * <p>Parent directories inside the log directory are created automatically
     * if they do not exist. The path must be relative and must not contain
     * {@code ..} segments.
     *
     * @param relativePath relative path of the log file,
     *                     e.g. {@code "audit.log"} or {@code "2024/january.log"}
     * @param append       if {@code true}, new content is appended to the
     *                     existing file; if {@code false} the file is overwritten
     * @return a {@link Writer} backed by the file — caller must close it
     * @throws IllegalArgumentException if {@code relativePath} is blank,
     *                                  absolute, or contains {@code ..}
     * @throws IOException              if the file cannot be opened
     */
    Writer openLogWriter(String relativePath, boolean append) throws IOException;

    /**
     * Returns the absolute path of this plugin's log directory.
     *
     * <p>Intended for read-only display purposes, e.g. in the admin UI
     * or in a startup log message. Do not use this path to construct
     * file paths manually — use {@link #openLogWriter} instead.
     *
     * @return the absolute log directory path, which is guaranteed to exist
     */
    Path logDirectory();
}
