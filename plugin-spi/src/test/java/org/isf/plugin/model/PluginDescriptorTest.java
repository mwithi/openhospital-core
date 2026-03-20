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

import org.isf.plugin.model.FieldPermission;
import org.isf.plugin.model.PluginDescriptor.ExternalConnection;
import org.isf.plugin.model.PluginDescriptor.ExternalConnection.Direction;
import org.isf.plugin.model.field.PatientField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PluginDescriptor}.
 * No Spring, no DB — pure Java logic.
 */
@DisplayName("PluginDescriptor")
class PluginDescriptorTest {

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("build() — happy path")
    class HappyPath {

        @Test
        @DisplayName("builds a valid descriptor with all fields set")
        void shouldBuildValidDescriptor() {
            PluginDescriptor d = validBuilder().build();

            assertThat(d.getPluginId()).isEqualTo("org.isf.radiology");
            assertThat(d.getVersion()).isEqualTo("1.0.0");
            assertThat(d.getName()).isEqualTo("Radiology Module");
            assertThat(d.getEntryPoint()).isEqualTo("org.isf.radiology.RadiologyPlugin");
            assertThat(d.getMinCoreVersion()).isEqualTo("1.15.0");
            assertThat(d.getCapabilities()).containsExactly(
                    PluginCapability.API_EXTENSION, PluginCapability.DB_MIGRATION);
            assertThat(d.getPermissions()).containsExactly(PluginPermission.READ_PATIENT);
            assertThat(d.getDependencies()).isEmpty();
            assertThat(d.getExternalConnections()).isEmpty();
        }

        @Test
        @DisplayName("returned capability list is immutable")
        void capabilitiesAreImmutable() {
            PluginDescriptor d = validBuilder().build();
            assertThatThrownBy(() -> d.getCapabilities().add(PluginCapability.UI_ROUTES))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("returned permission list is immutable")
        void permissionsAreImmutable() {
            PluginDescriptor d = validBuilder().build();
            assertThatThrownBy(() -> d.getPermissions().add(PluginPermission.WRITE_PATIENT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("fieldPermissions are stored and returned correctly")
        void fieldPermissionsStoredCorrectly() {
            FieldPermission fp = FieldPermission.read(
                    PatientField.FIRST_NAME, PatientField.LAST_NAME)
                    .purpose("Display patient name on report");

            PluginDescriptor d = validBuilder()
                    .fieldPermissions(List.of(fp))
                    .build();

            assertThat(d.getFieldPermissions()).hasSize(1);
            assertThat(d.getFieldPermissions().get(0).getDomainName()).isEqualTo("Patient");
        }

        @Test
        @DisplayName("requiresExplicitApproval is true when any fieldPermission has SENSITIVE field")
        void requiresExplicitApprovalWhenSensitiveField() {
            FieldPermission fp = FieldPermission.read(
                    PatientField.TAX_CODE)
                    .purpose("Verify identity against national registry");

            PluginDescriptor d = validBuilder()
                    .fieldPermissions(List.of(fp))
                    .build();

            assertThat(d.requiresExplicitApproval()).isTrue();
        }

        @Test
        @DisplayName("returned externalConnections list is immutable")
        void externalConnectionsAreImmutable() {
            PluginDescriptor d = validBuilder()
                    .externalConnections(List.of(pacsConnection()))
                    .build();
            assertThatThrownBy(() -> d.getExternalConnections().add(pacsConnection()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("hasCapability returns true/false correctly")
        void hasCapabilityShouldWork() {
            PluginDescriptor d = validBuilder().build();

            assertThat(d.hasCapability(PluginCapability.API_EXTENSION)).isTrue();
            assertThat(d.hasCapability(PluginCapability.UI_ROUTES)).isFalse();
        }

        @Test
        @DisplayName("requiresPermission returns true/false correctly")
        void requiresPermissionShouldWork() {
            PluginDescriptor d = validBuilder().build();

            assertThat(d.requiresPermission(PluginPermission.READ_PATIENT)).isTrue();
            assertThat(d.requiresPermission(PluginPermission.WRITE_PATIENT)).isFalse();
        }

        @Test
        @DisplayName("equals and hashCode are based on pluginId + version")
        void equalsByPluginIdAndVersion() {
            PluginDescriptor d1 = validBuilder().build();
            PluginDescriptor d2 = validBuilder().name("Different name").build();
            PluginDescriptor d3 = validBuilder().version("2.0.0").build();

            assertThat(d1).isEqualTo(d2);
            assertThat(d1).isNotEqualTo(d3);
            assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
        }

        @Test
        @DisplayName("null optional lists are treated as empty")
        void nullListsTreatedAsEmpty() {
            PluginDescriptor d = validBuilder()
                    .capabilities(null)
                    .permissions(null)
                    .dependencies(null)
                    .externalConnections(null)
                    .build();

            assertThat(d.getCapabilities()).isEmpty();
            assertThat(d.getPermissions()).isEmpty();
            assertThat(d.getDependencies()).isEmpty();
            assertThat(d.getExternalConnections()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Missing required fields
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("build() — missing required fields")
    class MissingRequiredFields {

        @Test
        @DisplayName("null pluginId → IllegalArgumentException")
        void missingPluginId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> validBuilder().pluginId(null).build())
                    .withMessageContaining("pluginId");
        }

        @Test
        @DisplayName("null version → IllegalArgumentException")
        void missingVersion() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> validBuilder().version(null).build())
                    .withMessageContaining("version");
        }

        @Test
        @DisplayName("null entryPoint → IllegalArgumentException")
        void missingEntryPoint() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> validBuilder().entryPoint(null).build())
                    .withMessageContaining("entryPoint");
        }

        @Test
        @DisplayName("null minCoreVersion → IllegalArgumentException")
        void missingMinCoreVersion() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> validBuilder().minCoreVersion(null).build())
                    .withMessageContaining("minCoreVersion");
        }
    }

    // -------------------------------------------------------------------------
    // Pattern validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("build() — pattern validation")
    class PatternValidation {

        @ParameterizedTest(name = "invalid pluginId: ''{0}''")
        @ValueSource(strings = {
                "radiology",
                "Org.isf.radiology",
                "org.isf.radiology!",
                "org..isf",
                "1.isf.module",
                "",
        })
        @DisplayName("invalid pluginIds → IllegalArgumentException")
        void invalidPluginIds(String invalidId) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> validBuilder().pluginId(invalidId).build())
                    .withMessageContaining("pluginId");
        }

        @ParameterizedTest(name = "valid pluginId: ''{0}''")
        @ValueSource(strings = {
                "org.isf.radiology",
                "com.example.myplugin",
                "org.isf.lab.advanced",
        })
        @DisplayName("valid pluginIds → no exception")
        void validPluginIds(String validId) {
            assertThatNoException()
                    .isThrownBy(() -> validBuilder().pluginId(validId).build());
        }

        @ParameterizedTest(name = "invalid version: ''{0}''")
        @ValueSource(strings = { "1.0", "v1.0.0", "1.0.0.0", "latest" })
        @DisplayName("invalid versions → IllegalArgumentException")
        void invalidVersions(String invalidVersion) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> validBuilder().version(invalidVersion).build())
                    .withMessageContaining("version");
        }

        @ParameterizedTest(name = "valid version: ''{0}''")
        @ValueSource(strings = { "1.0.0", "2.10.3", "1.0.0-SNAPSHOT", "3.2.1-RC1" })
        @DisplayName("valid versions → no exception")
        void validVersions(String version) {
            assertThatNoException()
                    .isThrownBy(() -> validBuilder().version(version).build());
        }

        @Test
        @DisplayName("entryPoint without package → IllegalArgumentException")
        void invalidEntryPoint() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> validBuilder()
                            .entryPoint("RadiologyPlugin")
                            .build())
                    .withMessageContaining("entryPoint");
        }

        @Test
        @DisplayName("dependency with invalid pluginId → IllegalArgumentException")
        void invalidDependencyPluginId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> validBuilder()
                            .dependencies(List.of("invalid-dep"))
                            .build())
                    .withMessageContaining("Dependency");
        }
    }

    // -------------------------------------------------------------------------
    // ExternalConnection
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ExternalConnection")
    class ExternalConnectionTests {

        @Test
        @DisplayName("isConnectionAllowed returns true for declared host:port")
        void allowsDeclaredConnection() {
            PluginDescriptor d = validBuilder()
                    .externalConnections(List.of(pacsConnection()))
                    .build();

            assertThat(d.isConnectionAllowed("pacs.hospital.org", 11112)).isTrue();
        }

        @Test
        @DisplayName("isConnectionAllowed returns false for undeclared host")
        void blocksUndeclaredHost() {
            PluginDescriptor d = validBuilder()
                    .externalConnections(List.of(pacsConnection()))
                    .build();

            assertThat(d.isConnectionAllowed("evil.example.com", 11112)).isFalse();
        }

        @Test
        @DisplayName("isConnectionAllowed returns false for declared host but wrong port")
        void blocksWrongPort() {
            PluginDescriptor d = validBuilder()
                    .externalConnections(List.of(pacsConnection()))
                    .build();

            assertThat(d.isConnectionAllowed("pacs.hospital.org", 443)).isFalse();
        }

        @Test
        @DisplayName("hasExternalConnections returns false for empty list")
        void noExternalConnectionsByDefault() {
            PluginDescriptor d = validBuilder().build();

            assertThat(d.hasExternalConnections()).isFalse();
        }

        @Test
        @DisplayName("hasExternalConnections returns true when declared")
        void hasExternalConnectionsWhenDeclared() {
            PluginDescriptor d = validBuilder()
                    .externalConnections(List.of(pacsConnection()))
                    .build();

            assertThat(d.hasExternalConnections()).isTrue();
        }

        @Test
        @DisplayName("ExternalConnection rejects blank host")
        void rejectsBlankHost() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ExternalConnection(
                            "", 11112, "DICOM", "Test", Direction.OUTBOUND))
                    .withMessageContaining("host");
        }

        @Test
        @DisplayName("ExternalConnection rejects invalid port 0")
        void rejectsPortZero() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ExternalConnection(
                            "host.org", 0, "DICOM", "Test", Direction.OUTBOUND))
                    .withMessageContaining("port");
        }

        @Test
        @DisplayName("ExternalConnection rejects port above 65535")
        void rejectsPortAboveMax() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ExternalConnection(
                            "host.org", 65536, "DICOM", "Test", Direction.OUTBOUND))
                    .withMessageContaining("port");
        }

        @Test
        @DisplayName("ExternalConnection rejects blank purpose")
        void rejectsBlankPurpose() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ExternalConnection(
                            "host.org", 443, "HTTPS", "", Direction.OUTBOUND))
                    .withMessageContaining("purpose");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static PluginDescriptor.Builder validBuilder() {
        return PluginDescriptor.builder()
                .pluginId("org.isf.radiology")
                .version("1.0.0")
                .name("Radiology Module")
                .entryPoint("org.isf.radiology.RadiologyPlugin")
                .minCoreVersion("1.15.0")
                .capabilities(List.of(
                        PluginCapability.API_EXTENSION,
                        PluginCapability.DB_MIGRATION))
                .permissions(List.of(PluginPermission.READ_PATIENT));
    }

    private static ExternalConnection pacsConnection() {
        return new ExternalConnection(
                "pacs.hospital.org",
                11112,
                "DICOM",
                "Send DICOM study to hospital PACS server",
                Direction.OUTBOUND);
    }
}
