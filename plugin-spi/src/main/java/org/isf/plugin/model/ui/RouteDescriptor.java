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

import org.isf.plugin.model.PluginPermission;

import java.util.Objects;

/**
 * Describes a new route (page) that a plugin contributes to the OH React UI.
 *
 * <h3>Routing in OH-ui</h3>
 * OH-ui currently uses React Router with a central {@code Private} wrapper
 * that performs authentication-only checks (logged in vs. not logged in).
 * Route-level permission enforcement does not yet exist in the router.
 * The {@link #permission} field is therefore forward-compatible:
 * when Phase 4 introduces permission-aware routing, the field will be
 * read by the router to filter visible routes. Until then, any authenticated
 * user can navigate to the route — the actual data protection is enforced
 * server-side via {@code ctx.check()} in the plugin's REST endpoints.
 *
 * <h3>Why PluginPermission instead of role names</h3>
 * OH role names (ADMIN, DOCTOR, NURSE, ...) are an internal implementation
 * detail. Exposing them in the SPI would couple third-party plugins to OH
 * internals that may change. {@link PluginPermission} is the stable public
 * contract — {@code PluginContextImpl} owns the mapping from permission
 * to OH role, keeping it in one place.
 *
 * @param path       URL path for this route, e.g. {@code "/radiology"}.
 *                   Must start with {@code /} and contain only URL-safe characters.
 * @param label      Human-readable label shown in the navigation menu.
 * @param menuPath   Dot-separated path in the navigation hierarchy,
 *                   e.g. {@code "Modules.Radiology"}. Used to place the
 *                   menu item in the correct submenu.
 * @param permission Optional permission required to see this route.
 *                   {@code null} means any authenticated user can access it.
 *                   When Phase 4 implements permission-aware routing, this
 *                   field will be enforced by the React Router wrapper.
 */
public record RouteDescriptor(
        String           path,
        String           label,
        String           menuPath,
        PluginPermission permission
) {
    public RouteDescriptor {
        Objects.requireNonNull(path,     "RouteDescriptor.path must not be null");
        Objects.requireNonNull(label,    "RouteDescriptor.label must not be null");
        Objects.requireNonNull(menuPath, "RouteDescriptor.menuPath must not be null");

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(
                "RouteDescriptor.path must start with '/': " + path);
        }
        if (label.isBlank()) {
            throw new IllegalArgumentException(
                "RouteDescriptor.label must not be blank");
        }
        if (menuPath.isBlank()) {
            throw new IllegalArgumentException(
                "RouteDescriptor.menuPath must not be blank");
        }
        // permission may be null — means no restriction beyond authentication
    }

    /**
     * Convenience factory for a route with no permission restriction.
     * Any authenticated user can access it.
     */
    public static RouteDescriptor open(String path, String label, String menuPath) {
        return new RouteDescriptor(path, label, menuPath, null);
    }

    /**
     * Convenience factory for a permission-restricted route.
     */
    public static RouteDescriptor restricted(String path, String label,
                                             String menuPath,
                                             PluginPermission permission) {
        return new RouteDescriptor(path, label, menuPath,
                Objects.requireNonNull(permission));
    }

    /** Returns {@code true} if this route requires a specific permission. */
    public boolean isRestricted() {
        return permission != null;
    }
}
