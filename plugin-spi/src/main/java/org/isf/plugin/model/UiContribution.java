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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Declares all contributions a plugin makes to the Open Hospital React UI.
 *
 * <p>This is the Java-side descriptor of the UI bundle carried in the plugin
 * package. It does not contain JavaScript — it describes the bundle's metadata
 * so that the OH runtime ({@code openhospital-api}) and the React host
 * ({@code openhospital-ui}) can load and integrate it correctly.
 *
 * <h3>Relationship with PluginCapability</h3>
 * A non-null {@link UiContribution} in the {@link PluginDescriptor} requires
 * at least one of:
 * <ul>
 *   <li>{@link PluginCapability#UI_ROUTES} — if {@link #routes()} is non-empty</li>
 *   <li>{@link PluginCapability#UI_COMPONENT_OVERRIDE} — if {@link #slots()} is non-empty</li>
 * </ul>
 * Both require a non-null {@link #bundle()}. These constraints are enforced
 * in {@link PluginDescriptor.Builder#build()}.
 *
 * <h3>Example manifest.json fragment</h3>
 * <pre>{@code
 * "uiContributions": {
 *   "routes": [
 *     {
 *       "path":       "/radiology",
 *       "label":      "Radiology",
 *       "menuPath":   "Modules > Radiology",
 *       "permission": "READ_LABORATORY"
 *     }
 *   ],
 *   "slots": [
 *     {
 *       "slotId": "patient.header.actions",
 *       "mode":   "APPEND"
 *     }
 *   ],
 *   "widgets": [],
 *   "bundle": {
 *     "entry":      "ui/radiology-plugin.js",
 *     "remoteName": "radiologyPlugin"
 *   }
 * }
 * }</pre>
 *
 * @param routes  new pages/routes contributed to the React Router
 * @param slots   overrides or extensions of existing UI component slots
 * @param widgets autonomous components inserted into named positions
 * @param bundle  the JS bundle descriptor — required if routes, slots,
 *                or widgets are non-empty
 */
public record UiContribution(
        List<RouteDescriptor>  routes,
        List<SlotContribution> slots,
        List<WidgetDescriptor> widgets,
        BundleDescriptor       bundle
) {
    /**
     * Compact constructor — validates consistency between contributions and bundle.
     */
    public UiContribution {
        routes  = routes  != null ? List.copyOf(routes)  : Collections.emptyList();
        slots   = slots   != null ? List.copyOf(slots)   : Collections.emptyList();
        widgets = widgets != null ? List.copyOf(widgets) : Collections.emptyList();

        boolean hasContributions = !routes.isEmpty() || !slots.isEmpty() || !widgets.isEmpty();
        if (hasContributions && bundle == null) {
            throw new IllegalArgumentException(
                "UiContribution: a BundleDescriptor is required when routes, " +
                "slots, or widgets are declared");
        }
    }

    /** Returns {@code true} if this contribution declares at least one element. */
    public boolean isEmpty() {
        return routes.isEmpty() && slots.isEmpty() && widgets.isEmpty();
    }

    // =========================================================================
    // Nested records
    // =========================================================================

    /**
     * Describes a new page/route contributed to the React Router.
     *
     * <p>The {@code permission} field is optional. When present, the OH-ui
     * router will check the current user's permissions (via the
     * {@code permissionSlice} in Redux state) before rendering the route.
     * When absent, the route is visible to any authenticated user.
     *
     * <p>Note: as of OH-ui develop branch, route-level permission checks are
     * not yet implemented in the router. The {@code permission} field is
     * forward-compatible — it will be honoured once OH-ui Fase 4 adds
     * permission-aware routing. Until then, protection relies solely on the
     * plugin's REST endpoints returning 403 for unauthorized users.
     *
     * @param path       URL path, e.g. {@code "/radiology"}
     * @param label      display label in navigation menus
     * @param menuPath   dot-separated or angle-bracket path in the menu tree,
     *                   e.g. {@code "Modules > Radiology"}
     * @param permission optional OH permission required to see this route;
     *                   {@code null} means any authenticated user
     */
    public record RouteDescriptor(
            String           path,
            String           label,
            String           menuPath,
            PluginPermission permission
    ) {
        public RouteDescriptor {
            Objects.requireNonNull(path,  "RouteDescriptor.path must not be null");
            Objects.requireNonNull(label, "RouteDescriptor.label must not be null");
            if (path.isBlank()) {
                throw new IllegalArgumentException(
                    "RouteDescriptor.path must not be blank");
            }
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException(
                    "RouteDescriptor.path must start with '/', got: " + path);
            }
            if (label.isBlank()) {
                throw new IllegalArgumentException(
                    "RouteDescriptor.label must not be blank");
            }
        }
    }

    /**
     * Describes a contribution to an existing UI component slot.
     *
     * <p>Slots are named extension points declared in the OH-ui React components
     * via a {@code <PluginSlot name="..."/>} element. A plugin can append,
     * prepend, or fully replace the default content of a slot.
     *
     * @param slotId the slot identifier as declared in the OH-ui source,
     *               e.g. {@code "patient.header.actions"}
     * @param mode   how this contribution integrates with existing slot content
     */
    public record SlotContribution(
            String   slotId,
            SlotMode mode
    ) {
        /** How a plugin contribution integrates with existing slot content. */
        public enum SlotMode {
            /** Add after the existing slot content. */
            APPEND,
            /** Add before the existing slot content. */
            PREPEND,
            /** Completely replace the existing slot content. */
            REPLACE
        }

        public SlotContribution {
            Objects.requireNonNull(slotId, "SlotContribution.slotId must not be null");
            Objects.requireNonNull(mode,   "SlotContribution.mode must not be null");
            if (slotId.isBlank()) {
                throw new IllegalArgumentException(
                    "SlotContribution.slotId must not be blank");
            }
        }
    }

    /**
     * Describes an autonomous UI widget contributed to a named position.
     *
     * <p>Unlike routes (full pages) or slots (inline extensions), a widget
     * is a self-contained component inserted into a specific position in the
     * layout without replacing existing content.
     *
     * @param widgetId a unique identifier for this widget within the plugin,
     *                 e.g. {@code "radiology.patient.summary"}
     * @param slotId   the position where the widget appears,
     *                 e.g. {@code "patient.detail.sidebar"}
     * @param label    display label for accessibility and admin UI
     */
    public record WidgetDescriptor(
            String widgetId,
            String slotId,
            String label
    ) {
        public WidgetDescriptor {
            Objects.requireNonNull(widgetId, "WidgetDescriptor.widgetId must not be null");
            Objects.requireNonNull(slotId,   "WidgetDescriptor.slotId must not be null");
            Objects.requireNonNull(label,    "WidgetDescriptor.label must not be null");
            if (widgetId.isBlank() || slotId.isBlank() || label.isBlank()) {
                throw new IllegalArgumentException(
                    "WidgetDescriptor fields must not be blank");
            }
        }
    }

    /**
     * Describes the JavaScript bundle carried by the plugin package.
     *
     * <p>The bundle is loaded by the OH-ui React host using Vite Module
     * Federation. The {@code remoteName} must match the name declared in
     * the plugin's Vite build configuration.
     *
     * @param entry      path to the bundle file inside the plugin ZIP,
     *                   e.g. {@code "ui/radiology-plugin.js"}
     * @param remoteName Module Federation remote name,
     *                   e.g. {@code "radiologyPlugin"}
     */
    public record BundleDescriptor(
            String entry,
            String remoteName
    ) {
        public BundleDescriptor {
            Objects.requireNonNull(entry,      "BundleDescriptor.entry must not be null");
            Objects.requireNonNull(remoteName, "BundleDescriptor.remoteName must not be null");
            if (entry.isBlank()) {
                throw new IllegalArgumentException(
                    "BundleDescriptor.entry must not be blank");
            }
            if (remoteName.isBlank()) {
                throw new IllegalArgumentException(
                    "BundleDescriptor.remoteName must not be blank");
            }
        }
    }
}
