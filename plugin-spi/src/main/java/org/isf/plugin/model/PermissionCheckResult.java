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

/**
 * Three-state result of a permission check performed via
 * {@link org.isf.plugin.registry.PluginContext#check(PluginPermission)}.
 *
 * <p>A boolean would only distinguish "yes" from "no". This enum also
 * distinguishes <em>why</em> access is denied, allowing the plugin to
 * return accurate feedback to the end user.
 *
 * <h3>Meanings</h3>
 * <ul>
 *   <li>{@link #GRANTED} — both the plugin and the current user are authorized.
 *       Proceed with the operation.</li>
 *   <li>{@link #DENIED_PLUGIN} — the plugin itself is not authorized.
 *       This is a configuration issue: the permission was either not declared
 *       in the manifest or not approved by the administrator at install time.
 *       The operation is blocked regardless of who the current user is.</li>
 *   <li>{@link #DENIED_USER} — the plugin is authorized, but the current
 *       authenticated user does not hold the required OH role.
 *       A different user with the right role could perform the same operation.</li>
 * </ul>
 *
 * <h3>Usage — distinguishing the reason</h3>
 * <pre>{@code
 * return switch (ctx.check(PluginPermission.READ_PATIENT)) {
 *     case GRANTED       -> ResponseEntity.ok(loadData(patientCode));
 *     case DENIED_PLUGIN -> ResponseEntity.status(403)
 *                               .body("Plugin not configured for patient access");
 *     case DENIED_USER   -> ResponseEntity.status(403)
 *                               .body("Your role does not allow reading patient data");
 * };
 * }</pre>
 *
 * <h3>Usage — simple guard clause</h3>
 * <pre>{@code
 * if (ctx.check(READ_PATIENT).isDenied()) {
 *     return ResponseEntity.status(403).build();
 * }
 * }</pre>
 */
public enum PermissionCheckResult {

    /**
     * Both the plugin holds the permission and the current user
     * has the corresponding OH role. Access is fully granted.
     */
    GRANTED,

    /**
     * The plugin does not hold this permission — either it was not declared
     * in the manifest, or the administrator did not approve it at install time.
     * The operation is blocked regardless of who the current user is.
     */
    DENIED_PLUGIN,

    /**
     * The plugin holds the permission, but the currently authenticated user
     * does not have the required OH role for this operation.
     * The operation is blocked for this user but may succeed for others.
     */
    DENIED_USER;

    /**
     * Returns {@code true} only if access is fully granted.
     *
     * @return {@code true} if this result is {@link #GRANTED}
     */
    public boolean isGranted() {
        return this == GRANTED;
    }

    /**
     * Returns {@code true} if access is denied for any reason.
     *
     * <p>Use this for simple guard clauses when the specific reason
     * does not matter to the caller.
     *
     * @return {@code true} if this result is {@link #DENIED_PLUGIN}
     *         or {@link #DENIED_USER}
     */
    public boolean isDenied() {
        return this != GRANTED;
    }
}
