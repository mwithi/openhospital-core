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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates all UI contributions a plugin makes to the OH React host.
 *
 * <p>A plugin that declares {@link org.isf.plugin.model.PluginCapability#UI_ROUTES}
 * or {@link org.isf.plugin.model.PluginCapability#UI_COMPONENT_OVERRIDE} must
 * include a non-null {@code UiContribution} in its {@link org.isf.plugin.model.PluginDescriptor}.
 *
 * <h3>Consistency rules (enforced in PluginDescriptor.build())</h3>
 * <ul>
 *   <li>If {@code routes} is non-empty → capability {@code UI_ROUTES} required</li>
 *   <li>If {@code slots} is non-empty → capability {@code UI_COMPONENT_OVERRIDE} required</li>
 *   <li>If either list is non-empty → {@code bundle} must not be null</li>
 *   <li>If {@code bundle} is not null → at least one of {@code routes} or
 *       {@code slots} must be non-empty</li>
 * </ul>
 *
 * <h3>Example manifest snippet</h3>
 * <pre>{@code
 * "uiContribution": {
 *   "bundle": { "entry": "ui/radiology.js", "remoteName": "radiologyPlugin" },
 *   "routes": [
 *     { "path": "/radiology", "label": "Radiology", "menuPath": "Modules.Radiology" }
 *   ],
 *   "slots": [
 *     { "slotId": "patient.header.actions", "mode": "APPEND" }
 *   ]
 * }
 * }</pre>
 *
 * <h3>Builder usage</h3>
 * <pre>{@code
 * UiContribution.builder()
 *     .bundle("ui/radiology.js", "radiologyPlugin")
 *     .route(RouteDescriptor.open("/radiology", "Radiology", "Modules.Radiology"))
 *     .slot(new SlotContribution("patient.header.actions", SlotMode.APPEND))
 *     .build();
 * }</pre>
 */
public final class UiContribution {

    private final BundleDescriptor      bundle;
    private final List<RouteDescriptor> routes;
    private final List<SlotContribution> slots;

    private UiContribution(Builder b) {
        this.bundle = b.bundle;
        this.routes = List.copyOf(b.routes);
        this.slots  = List.copyOf(b.slots);
    }

    public BundleDescriptor       getBundle() { return bundle; }
    public List<RouteDescriptor>  getRoutes() { return routes; }
    public List<SlotContribution> getSlots()  { return slots; }

    public boolean hasRoutes() { return !routes.isEmpty(); }
    public boolean hasSlots()  { return !slots.isEmpty(); }

    public static Builder builder() { return new Builder(); }

    // -------------------------------------------------------------------------

    public static final class Builder {

        private BundleDescriptor       bundle;
        private List<RouteDescriptor>  routes = Collections.emptyList();
        private List<SlotContribution> slots  = Collections.emptyList();

        private Builder() {}

        /**
         * Sets the bundle descriptor from its two components.
         *
         * @param entry      bundle entry path inside the plugin ZIP
         * @param remoteName Module Federation remote name (JS identifier)
         */
        public Builder bundle(String entry, String remoteName) {
            this.bundle = new BundleDescriptor(entry, remoteName);
            return this;
        }

        public Builder bundle(BundleDescriptor bundle) {
            this.bundle = Objects.requireNonNull(bundle);
            return this;
        }

        public Builder routes(List<RouteDescriptor> routes) {
            this.routes = routes != null ? routes : Collections.emptyList();
            return this;
        }

        public Builder route(RouteDescriptor route) {
            this.routes = new java.util.ArrayList<>(this.routes);
            this.routes.add(Objects.requireNonNull(route));
            return this;
        }

        public Builder slots(List<SlotContribution> slots) {
            this.slots = slots != null ? slots : Collections.emptyList();
            return this;
        }

        public Builder slot(SlotContribution slot) {
            this.slots = new java.util.ArrayList<>(this.slots);
            this.slots.add(Objects.requireNonNull(slot));
            return this;
        }

        /**
         * Builds the {@link UiContribution}, validating internal consistency.
         *
         * @throws IllegalStateException if routes or slots are declared without a bundle,
         *                               or if a bundle is declared with no contributions
         */
        public UiContribution build() {
            boolean hasRoutes = routes != null && !routes.isEmpty();
            boolean hasSlots  = slots  != null && !slots.isEmpty();

            if ((hasRoutes || hasSlots) && bundle == null) {
                throw new IllegalStateException(
                    "UiContribution: routes or slots declared but no bundle specified. " +
                    "A JS bundle is required to serve UI contributions.");
            }
            if (bundle != null && !hasRoutes && !hasSlots) {
                throw new IllegalStateException(
                    "UiContribution: bundle declared but no routes or slots specified. " +
                    "A bundle without UI contributions serves no purpose.");
            }
            return new UiContribution(this);
        }
    }
}
