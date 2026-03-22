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
package org.isf.plugin.test.stub;

import org.isf.plugin.registry.PluginFileAccess;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory implementation of {@link PluginFileAccess} for unit tests.
 *
 * <p>Writes are captured in memory — no actual files are created.
 * After the test, inspect what the plugin wrote via {@link #writtenTo(String)}
 * or {@link #allWritten()}.
 *
 * <pre>{@code
 * plugin.onStart(ctx);
 * ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));
 *
 * assertThat(ctx.files().writtenTo("audit.log"))
 *         .contains("CREATED")
 *         .contains("Mario Rossi");
 * }</pre>
 */
public final class StubFileAccess implements PluginFileAccess {

    private final Map<String, StringBuilder> buffers = new LinkedHashMap<>();
    private final Path logDir;

    StubFileAccess() {
        Path dir;
        try {
            dir = java.nio.file.Files.createTempDirectory("oh-plugin-test-");
        } catch (java.io.IOException e) {
            dir = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"),
                                        "oh-plugin-test");
        }
        this.logDir = dir;
    }

    @Override
    public Writer openLogWriter(String relativePath, boolean append) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
        if (relativePath.contains("..")) {
            throw new IllegalArgumentException(
                "relativePath must not contain '..': " + relativePath);
        }
        if (!append) {
            buffers.remove(relativePath);
        }
        StringBuilder buf = buffers.computeIfAbsent(
                relativePath, k -> new StringBuilder());

        return new StringWriter() {
            @Override
            public void write(String str) {
                buf.append(str);
            }
            @Override
            public void write(char[] cbuf, int off, int len) {
                buf.append(new String(cbuf, off, len));
            }
            @Override
            public void flush() {}
            @Override
            public void close() {}
        };
    }

    @Override
    public Path logDirectory() {
        return logDir;
    }

    // -------------------------------------------------------------------------
    // Inspection API
    // -------------------------------------------------------------------------

    /**
     * Returns everything written to the given relative path so far,
     * or an empty string if nothing was written to it.
     */
    public String writtenTo(String relativePath) {
        StringBuilder buf = buffers.get(relativePath);
        return buf != null ? buf.toString() : "";
    }

    /**
     * Returns an unmodifiable view of all files written and their content.
     */
    public Map<String, String> allWritten() {
        Map<String, String> result = new LinkedHashMap<>();
        buffers.forEach((k, v) -> result.put(k, v.toString()));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns {@code true} if at least one write was made to the given path.
     */
    public boolean wasWrittenTo(String relativePath) {
        return buffers.containsKey(relativePath);
    }

    /**
     * Clears all captured content. Useful between test steps.
     */
    public void clear() {
        buffers.clear();
    }
}
