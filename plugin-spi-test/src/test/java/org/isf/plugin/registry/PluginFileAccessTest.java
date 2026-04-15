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

import org.isf.plugin.model.PluginCapability;
import org.isf.plugin.test.stub.StubPluginContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Writer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PluginFileAccess} and its stub implementation.
 */
@DisplayName("PluginFileAccess")
class PluginFileAccessTest {

    private StubPluginContext ctx;

    @BeforeEach
    void setUp() {
        ctx = StubPluginContext.builder()
                .capabilities(List.of(PluginCapability.LOG_FILE_WRITE))
                .build();
    }

    // -------------------------------------------------------------------------
    // PluginContext.files() default behaviour
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PluginContext.files() default")
    class DefaultBehaviour {

        @Test
        @DisplayName("throws UnsupportedOperationException when capability not declared")
        void throwsWhenCapabilityMissing() {
            StubPluginContext noFiles = new StubPluginContext();
            assertThatThrownBy(noFiles::files)
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // StubFileAccess — write and inspect
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("StubFileAccess — write and inspect")
    class StubFileAccessTests {

        @Test
        @DisplayName("written content is captured in memory")
        void writtenContentCaptured() throws Exception {
            try (Writer w = ctx.files().openLogWriter("audit.log", true)) {
                w.write("[2024-01-15] CREATED - Mario Rossi (code: 42)\n");
            }

            assertThat(ctx.files().writtenTo("audit.log"))
                    .contains("CREATED")
                    .contains("Mario Rossi");
        }

        @Test
        @DisplayName("multiple writes to the same file are concatenated")
        void multipleWritesConcatenated() throws Exception {
            try (Writer w = ctx.files().openLogWriter("audit.log", true)) {
                w.write("line one\n");
            }
            try (Writer w = ctx.files().openLogWriter("audit.log", true)) {
                w.write("line two\n");
            }

            assertThat(ctx.files().writtenTo("audit.log"))
                    .contains("line one")
                    .contains("line two");
        }

        @Test
        @DisplayName("append=false overwrites previous content")
        void appendFalseOverwrites() throws Exception {
            try (Writer w = ctx.files().openLogWriter("audit.log", true)) {
                w.write("old content\n");
            }
            try (Writer w = ctx.files().openLogWriter("audit.log", false)) {
                w.write("new content\n");
            }

            assertThat(ctx.files().writtenTo("audit.log"))
                    .doesNotContain("old content")
                    .contains("new content");
        }

        @Test
        @DisplayName("writes to different files are tracked separately")
        void differentFilesSeparate() throws Exception {
            try (Writer w = ctx.files().openLogWriter("audit.log", true)) {
                w.write("patient event\n");
            }
            try (Writer w = ctx.files().openLogWriter("errors.log", true)) {
                w.write("error event\n");
            }

            assertThat(ctx.files().writtenTo("audit.log")).contains("patient event");
            assertThat(ctx.files().writtenTo("errors.log")).contains("error event");
            assertThat(ctx.files().allWritten()).hasSize(2);
        }

        @Test
        @DisplayName("wasWrittenTo returns false for untouched files")
        void wasWrittenToReturnsFalseForUntouched() {
            assertThat(ctx.files().wasWrittenTo("audit.log")).isFalse();
        }

        @Test
        @DisplayName("wasWrittenTo returns true after a write")
        void wasWrittenToReturnsTrueAfterWrite() throws Exception {
            try (Writer w = ctx.files().openLogWriter("audit.log", true)) {
                w.write("something\n");
            }
            assertThat(ctx.files().wasWrittenTo("audit.log")).isTrue();
        }

        @Test
        @DisplayName("clear resets all captured content")
        void clearResetsContent() throws Exception {
            try (Writer w = ctx.files().openLogWriter("audit.log", true)) {
                w.write("something\n");
            }
            ctx.files().clear();

            assertThat(ctx.files().wasWrittenTo("audit.log")).isFalse();
            assertThat(ctx.files().allWritten()).isEmpty();
        }

        @Test
        @DisplayName("logDirectory returns a non-null path")
        void logDirectoryNotNull() {
            assertThat(ctx.files().logDirectory()).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // Path validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("path validation")
    class PathValidation {

        @Test
        @DisplayName("blank relativePath → IllegalArgumentException")
        void blankPathRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ctx.files().openLogWriter("  ", true))
                    .withMessageContaining("blank");
        }

        @Test
        @DisplayName("path with .. → IllegalArgumentException")
        void dotDotPathRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ctx.files()
                            .openLogWriter("../other-plugin/secret.log", true))
                    .withMessageContaining("..");
        }
    }
}
