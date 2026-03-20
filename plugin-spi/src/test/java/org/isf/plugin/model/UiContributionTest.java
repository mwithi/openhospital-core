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

import org.isf.plugin.model.ui.BundleDescriptor;
import org.isf.plugin.model.ui.RouteDescriptor;
import org.isf.plugin.model.ui.SlotContribution;
import org.isf.plugin.model.ui.SlotContribution.SlotMode;
import org.isf.plugin.model.ui.UiContribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link UiContribution} and related types in {@code model.ui}.
 * No Spring, no DB — pure Java logic.
 */
@DisplayName("UiContribution")
class UiContributionTest {

    // -------------------------------------------------------------------------
    // UiContribution builder
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("UiContribution builder")
    class UiContributionBuilderTests {

        @Test
        @DisplayName("routes + bundle builds correctly")
        void routesWithBundleIsValid() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .route(validRoute())
                    .build();

            assertThat(ui.hasRoutes()).isTrue();
            assertThat(ui.hasSlots()).isFalse();
            assertThat(ui.getRoutes()).hasSize(1);
            assertThat(ui.getBundle().remoteName()).isEqualTo("radiologyPlugin");
        }

        @Test
        @DisplayName("slots + bundle builds correctly")
        void slotsWithBundleIsValid() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .slot(validSlot())
                    .build();

            assertThat(ui.hasSlots()).isTrue();
            assertThat(ui.hasRoutes()).isFalse();
        }

        @Test
        @DisplayName("routes + slots + bundle builds correctly")
        void fullContributionIsValid() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .route(validRoute())
                    .slot(validSlot())
                    .build();

            assertThat(ui.hasRoutes()).isTrue();
            assertThat(ui.hasSlots()).isTrue();
        }

        @Test
        @DisplayName("routes without bundle → IllegalStateException")
        void routesWithoutBundleRejected() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> UiContribution.builder()
                            .route(validRoute())
                            .build())
                    .withMessageContaining("bundle");
        }

        @Test
        @DisplayName("bundle without routes or slots → IllegalStateException")
        void bundleWithoutContributionsRejected() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> UiContribution.builder()
                            .bundle(validBundle())
                            .build())
                    .withMessageContaining("routes or slots");
        }

        @Test
        @DisplayName("routes list is immutable")
        void routesListIsImmutable() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .route(validRoute())
                    .build();

            assertThatThrownBy(() -> ui.getRoutes().add(validRoute()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("slots list is immutable")
        void slotsListIsImmutable() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .slot(validSlot())
                    .build();

            assertThatThrownBy(() -> ui.getSlots().add(validSlot()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // RouteDescriptor
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("RouteDescriptor")
    class RouteDescriptorTests {

        @Test
        @DisplayName("open() creates unrestricted route")
        void openRouteHasNullPermission() {
            RouteDescriptor r = RouteDescriptor.open(
                    "/radiology", "Radiology", "Modules > Radiology");

            assertThat(r.path()).isEqualTo("/radiology");
            assertThat(r.label()).isEqualTo("Radiology");
            assertThat(r.menuPath()).isEqualTo("Modules > Radiology");
            assertThat(r.permission()).isNull();
            assertThat(r.isRestricted()).isFalse();
        }

        @Test
        @DisplayName("restricted() creates permission-gated route")
        void restrictedRouteHasPermission() {
            RouteDescriptor r = RouteDescriptor.restricted(
                    "/radiology", "Radiology", "Modules > Radiology",
                    PluginPermission.READ_PATIENT);

            assertThat(r.permission()).isEqualTo(PluginPermission.READ_PATIENT);
            assertThat(r.isRestricted()).isTrue();
        }

        @Test
        @DisplayName("path not starting with / → IllegalArgumentException")
        void pathWithoutSlashRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> RouteDescriptor.open(
                            "radiology", "Radiology", "Modules"))
                    .withMessageContaining("/");
        }

        @Test
        @DisplayName("blank label → IllegalArgumentException")
        void blankLabelRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> RouteDescriptor.open(
                            "/radiology", "  ", "Modules"))
                    .withMessageContaining("label");
        }

        @Test
        @DisplayName("blank menuPath → IllegalArgumentException")
        void blankMenuPathRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> RouteDescriptor.open(
                            "/radiology", "Radiology", ""))
                    .withMessageContaining("menuPath");
        }
    }

    // -------------------------------------------------------------------------
    // SlotContribution
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("SlotContribution")
    class SlotContributionTests {

        @Test
        @DisplayName("valid slot with APPEND mode")
        void validSlotAppend() {
            SlotContribution s = new SlotContribution(
                    "patient.header.actions", SlotMode.APPEND);

            assertThat(s.slotId()).isEqualTo("patient.header.actions");
            assertThat(s.mode()).isEqualTo(SlotMode.APPEND);
        }

        @Test
        @DisplayName("valid slot with REPLACE mode")
        void validSlotReplace() {
            SlotContribution s = new SlotContribution(
                    "patient.header.actions", SlotMode.REPLACE);

            assertThat(s.mode()).isEqualTo(SlotMode.REPLACE);
        }

        @Test
        @DisplayName("blank slotId → IllegalArgumentException")
        void blankSlotIdRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new SlotContribution("", SlotMode.APPEND))
                    .withMessageContaining("slotId");
        }

        @Test
        @DisplayName("null mode → NullPointerException")
        void nullModeRejected() {
            assertThatThrownBy(() -> new SlotContribution("patient.actions", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // BundleDescriptor
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("BundleDescriptor")
    class BundleDescriptorTests {

        @Test
        @DisplayName("valid bundle descriptor")
        void validBundle() {
            BundleDescriptor b = new BundleDescriptor(
                    "ui/radiology.js", "radiologyPlugin");

            assertThat(b.entry()).isEqualTo("ui/radiology.js");
            assertThat(b.remoteName()).isEqualTo("radiologyPlugin");
        }

        @Test
        @DisplayName("remoteName with hyphen → IllegalArgumentException")
        void hyphenInRemoteNameRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new BundleDescriptor(
                            "ui/r.js", "radiology-plugin"))
                    .withMessageContaining("JavaScript identifier");
        }

        @Test
        @DisplayName("remoteName starting with digit → IllegalArgumentException")
        void digitStartRemoteNameRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new BundleDescriptor("ui/r.js", "1plugin"))
                    .withMessageContaining("JavaScript identifier");
        }

        @Test
        @DisplayName("blank entry → IllegalArgumentException")
        void blankEntryRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new BundleDescriptor("", "radiologyPlugin"))
                    .withMessageContaining("entry");
        }
    }

    // -------------------------------------------------------------------------
    // PluginDescriptor integration
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PluginDescriptor integration")
    class DescriptorIntegrationTests {

        @Test
        @DisplayName("UI_ROUTES capability + uiContribution → valid descriptor")
        void routesWithUiRoutesCapability() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .route(validRoute())
                    .build();

            assertThatNoException().isThrownBy(() ->
                    baseBuilder()
                            .capabilities(List.of(PluginCapability.UI_ROUTES))
                            .uiContribution(ui)
                            .build());
        }

        @Test
        @DisplayName("routes without UI_ROUTES capability → IllegalArgumentException")
        void routesWithoutCapabilityRejected() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .route(validRoute())
                    .build();

            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            baseBuilder()
                                    .capabilities(List.of(PluginCapability.API_EXTENSION))
                                    .uiContribution(ui)
                                    .build())
                    .withMessageContaining("UI_ROUTES");
        }

        @Test
        @DisplayName("UI_ROUTES capability without uiContribution → IllegalArgumentException")
        void capabilityWithoutUiContributionRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            baseBuilder()
                                    .capabilities(List.of(PluginCapability.UI_ROUTES))
                                    .build())
                    .withMessageContaining("uiContribution");
        }

        @Test
        @DisplayName("slots without UI_COMPONENT_OVERRIDE → IllegalArgumentException")
        void slotsWithoutOverrideCapabilityRejected() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .slot(validSlot())
                    .build();

            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            baseBuilder()
                                    .capabilities(List.of(PluginCapability.UI_ROUTES))
                                    .uiContribution(ui)
                                    .build())
                    .withMessageContaining("UI_COMPONENT_OVERRIDE");
        }

        @Test
        @DisplayName("hasUiContribution is false by default")
        void hasUiContributionFalseByDefault() {
            PluginDescriptor d = baseBuilder().build();
            assertThat(d.hasUiContribution()).isFalse();
        }

        @Test
        @DisplayName("hasUiContribution is true when routes are declared")
        void hasUiContributionTrueWithRoutes() {
            UiContribution ui = UiContribution.builder()
                    .bundle(validBundle())
                    .route(validRoute())
                    .build();

            PluginDescriptor d = baseBuilder()
                    .capabilities(List.of(PluginCapability.UI_ROUTES))
                    .uiContribution(ui)
                    .build();

            assertThat(d.hasUiContribution()).isTrue();
            assertThat(d.getUiContribution().getRoutes()).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RouteDescriptor validRoute() {
        return RouteDescriptor.open("/radiology", "Radiology", "Modules > Radiology");
    }

    private static SlotContribution validSlot() {
        return new SlotContribution("patient.header.actions", SlotMode.APPEND);
    }

    private static BundleDescriptor validBundle() {
        return new BundleDescriptor("ui/radiology-plugin.js", "radiologyPlugin");
    }

    private static PluginDescriptor.Builder baseBuilder() {
        return PluginDescriptor.builder()
                .pluginId("org.isf.radiology")
                .version("1.0.0")
                .name("Radiology Module")
                .entryPoint("org.isf.radiology.RadiologyPlugin")
                .minCoreVersion("1.15.0");
    }
}
