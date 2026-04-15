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

import org.isf.plugin.event.PluginEventBus;
import org.isf.plugin.model.PermissionCheckResult;
import org.isf.plugin.hook.OHManagerExtension;
import org.isf.plugin.model.PluginDescriptor;
import org.isf.plugin.model.PluginPermission;
import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * Runtime context injected into a plugin during its lifecycle.
 *
 * <p>This is the <strong>only</strong> gateway between the plugin and the OH
 * runtime. Plugins must not look up the Spring {@code ApplicationContext}, use
 * static accessors to OH-core Managers, or open network connections directly:
 * they must operate exclusively through the services exposed by this interface.
 *
 * <p>The concrete implementation ({@code PluginContextImpl}) lives in
 * {@code openhospital-api} and is constructed by the PluginManager for each
 * plugin. Every plugin receives its own isolated instance.
 *
 * <h3>Privacy by design</h3>
 * <ul>
 *   <li>{@link #data()} — access to domain data is explicit, audited, and
 *       field-limited to what the plugin declared in its manifest.</li>
 *   <li>{@link #httpClient()} — outbound HTTP is pre-configured to allow only
 *       the hosts declared in the plugin's {@code externalConnections} block.</li>
 *   <li>{@link #currentUserCan(PluginPermission)} — combines plugin-level
 *       permission with the current user's OH role in a single check, so
 *       plugins never need to know internal role names.</li>
 * </ul>
 *
 * <h3>Available services</h3>
 * <ul>
 *   <li>{@link #eventBus()} — publish/subscribe for domain events</li>
 *   <li>{@link #managers()} — registration of {@link OHManagerExtension}s</li>
 *   <li>{@link #data()} — purpose-limited, audited access to domain data</li>
 *   <li>{@link #httpClient()} — allowlist-enforced HTTP client</li>
 *   <li>{@link #logger()} — SLF4J logger pre-configured with the pluginId</li>
 *   <li>{@link #config()} — access to the plugin's own configuration</li>
 * </ul>
 */
public interface PluginContext {

    /**
     * Returns the descriptor of the plugin this context belongs to.
     *
     * @return immutable {@link PluginDescriptor}
     */
    PluginDescriptor getDescriptor();

    /**
     * Event bus for publishing and receiving domain events.
     *
     * @return the {@link PluginEventBus} instance dedicated to this plugin
     */
    PluginEventBus eventBus();

    /**
     * Registry for Manager extensions owned by this plugin.
     *
     * @return the {@link ManagerExtensionRegistry} instance dedicated to this plugin
     */
    ManagerExtensionRegistry managers();

    /**
     * Purpose-limited, audited access to OH domain data.
     *
     * <p>Every call through this accessor is logged with pluginId, userId,
     * timestamp, and declared purpose. Only the fields listed in the plugin's
     * manifest {@code permissions} block are accessible — other fields are
     * returned as {@code null} even if present in the underlying entity.
     *
     * @return the {@link PluginDataAccessor} for this plugin
     */
    PluginDataAccessor data();

    /**
     * HTTP client pre-configured to allow connections only to the hosts
     * declared in this plugin's {@code externalConnections} manifest block.
     *
     * <p>Any attempt to connect to an undeclared host throws
     * {@link SecurityException} before the connection is opened.
     *
     * <p>If the plugin declared no {@code externalConnections}, every call
     * on this client throws {@link SecurityException}.
     *
     * @return the {@link PluginHttpClient} for this plugin
     */
    PluginHttpClient httpClient();

    /**
     * Sandboxed filesystem access for writing log files.
     *
     * <p>Only available if the plugin declared
     * {@link org.isf.plugin.model.PluginCapability#LOG_FILE_WRITE}.
     * Calling this method without that capability throws
     * {@link UnsupportedOperationException}.
     *
     * @return the {@link PluginFileAccess} for this plugin's log directory
     * @throws UnsupportedOperationException if {@code LOG_FILE_WRITE} was not declared
     */
    default PluginFileAccess files() {
        throw new UnsupportedOperationException(
            "This plugin did not declare PluginCapability.LOG_FILE_WRITE");
    }

    /**
     * SLF4J logger pre-configured with the pluginId as the logger name.
     *
     * @return SLF4J logger
     */
    Logger logger();

    /**
     * Access to the plugin's own configuration properties.
     * Reads from {@code plugin-<pluginId>.properties} in the OH config directory.
     *
     * @return the {@link PluginConfig} for this plugin
     */
    PluginConfig config();

    /**
     * Checks whether this operation is permitted, returning a three-state
     * result that distinguishes between a plugin-level denial and a
     * user-level denial.
     *
     * <p>This is the single authorization check to use inside plugin code.
     * It evaluates two conditions in sequence:
     * <ol>
     *   <li>Does the plugin hold this permission (declared in manifest and
     *       approved by the administrator)?</li>
     *   <li>Does the currently authenticated user hold the corresponding
     *       OH role?</li>
     * </ol>
     * If condition 1 fails the result is {@link PermissionCheckResult#DENIED_PLUGIN}.
     * If condition 2 fails the result is {@link PermissionCheckResult#DENIED_USER}.
     * Both conditions must be true to return {@link PermissionCheckResult#GRANTED}.
     *
     * <h3>Distinguishing the reason</h3>
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
     * <h3>Simple guard clause</h3>
     * <pre>{@code
     * if (ctx.check(READ_PATIENT).isDenied()) {
     *     return ResponseEntity.status(403).build();
     * }
     * }</pre>
     *
     * @param permission the permission to check
     * @return a {@link PermissionCheckResult} — never null
     */
    PermissionCheckResult check(PluginPermission permission);

    // =========================================================================
    // Inner interfaces
    // =========================================================================

    /**
     * Purpose-limited, scope-bound access to OH domain data.
     *
     * <p>The accessor pattern ensures that domain objects cannot be stored
     * outside the callback scope — the plugin receives the data, uses it,
     * and it is gone. This prevents unintentional caching of PII.
     */
    interface PluginDataAccessor {

        /**
         * Executes an action with a read-only, field-limited view of a patient.
         *
         * <p>The {@link PatientView} object passed to the consumer is valid
         * only for the duration of the consumer call. Storing a reference
         * to it outside the lambda results in stale or nulled-out data.
         *
         * <p>Requires a {@link org.isf.plugin.model.FieldPermission} READ declaration
         *    for the Patient domain in the plugin manifest.
         * Every call is recorded in the audit log.
         *
         * @param patientCode the patient identifier (from a domain event)
         * @param consumer    the action to perform with the patient data
         * @throws PluginAuthorizationException if READ_PATIENT is not granted
         */
        void withPatient(int patientCode, Consumer<PatientView> consumer)
                throws PluginAuthorizationException;
    }

    /**
     * Read-only, field-limited projection of a patient entity.
     *
     * <p>Only the fields declared in the plugin manifest's
     * {@code fieldPermissions} block for the Patient domain are populated.
     * Any field not declared in the manifest returns {@code null} — even if
     * the plugin calls the getter. This filtering is enforced transparently
     * by {@code PluginContextImpl}; no code change is needed in the plugin.
     *
     * <p>{@link org.isf.plugin.model.field.PatientField#TAX_CODE} is absent
     * from this interface by design. It requires a separate declaration with
     * {@link org.isf.plugin.model.field.FieldSensitivity#SENSITIVE} and
     * explicit administrator approval — even then it is never returned here;
     * it is delivered through a dedicated audited accessor.
     *
     * <p>Getters map 1:1 to {@link org.isf.plugin.model.field.PatientField}
     * constants, making it easy to cross-reference the manifest declaration
     * with the runtime view.
     */
    interface PatientView {
        /** Always populated — ID fields are always available. */
        int    getPatientCode();
        /** {@code null} if {@code PatientField.FIRST_NAME} not declared. */
        String getFirstName();
        /** {@code null} if {@code PatientField.LAST_NAME} not declared. */
        String getLastName();
        /** {@code null} if {@code PatientField.BIRTH_DATE} not declared. */
        String getBirthDate();
        /** {@code null} if {@code PatientField.SEX} not declared. */
        String getSex();
        /** {@code null} if {@code PatientField.ADDRESS} not declared. */
        String getAddress();
        /** {@code null} if {@code PatientField.TELEPHONE} not declared. */
        String getTelephone();
        /** {@code null} if {@code PatientField.BLOOD_TYPE} not declared. */
        String getBloodType();
        /** {@code null} if {@code PatientField.WARD_CODE} not declared. */
        String getWardCode();
        // TAX_CODE is deliberately absent — requires SENSITIVE field declaration
        // and explicit administrator approval; delivered via separate audited accessor
    }

    /**
     * Allowlist-enforced HTTP client for outbound plugin connections.
     *
     * <p>The implementation validates the target host against the plugin's
     * {@code externalConnections} manifest block before opening any connection.
     */
    interface PluginHttpClient {

        /**
         * Performs a GET request to the given URL.
         *
         * @param url the target URL
         * @return the response body as a string
         * @throws SecurityException if the host is not in the plugin's allowlist
         * @throws java.io.IOException if the request fails
         */
        String get(String url) throws Exception;

        /**
         * Performs a POST request to the given URL.
         *
         * @param url  the target URL
         * @param body the request body
         * @return the response body as a string
         * @throws SecurityException if the host is not in the plugin's allowlist
         * @throws java.io.IOException if the request fails
         */
        String post(String url, String body) throws Exception;
    }

    /**
     * Registry for {@link OHManagerExtension} instances.
     */
    interface ManagerExtensionRegistry {

        /**
         * Registers an extension for an OH-core Manager.
         *
         * @param extension the extension to register
         * @throws SecurityException     if the plugin lacks the required permissions
         * @throws IllegalStateException if the target manager does not exist
         */
        void register(OHManagerExtension extension);

        /**
         * Deregisters all extensions registered by this plugin.
         * Called automatically during {@code onStop}.
         */
        void unregisterAll();
    }

    /**
     * Typed access to the plugin's configuration properties.
     */
    interface PluginConfig {

        /**
         * @param key          property key
         * @param defaultValue value if the key does not exist
         * @return the property value or {@code defaultValue}
         */
        String getString(String key, String defaultValue);

        /**
         * @param key          property key
         * @param defaultValue value if the key does not exist or is not an integer
         * @return the integer value or {@code defaultValue}
         */
        int getInt(String key, int defaultValue);

        /**
         * @param key          property key
         * @param defaultValue value if the key does not exist
         * @return the boolean value or {@code defaultValue}
         */
        boolean getBoolean(String key, boolean defaultValue);
    }

    /**
     * Thrown when a plugin attempts an operation it is not authorized to perform.
     * Carries the {@link PermissionCheckResult} so that the caller — or the
     * HTTP exception handler in {@code openhospital-api} — can produce an
     * accurate error message without additional logic.
     *
     * <p>Mapped to HTTP 403 by the plugin endpoint exception handler.
     */
    class PluginAuthorizationException extends Exception {

        private final PluginPermission      requiredPermission;
        private final PermissionCheckResult checkResult;

        public PluginAuthorizationException(String pluginId,
                                            PluginPermission permission,
                                            PermissionCheckResult checkResult) {
            super(buildMessage(pluginId, permission, checkResult));
            this.requiredPermission = permission;
            this.checkResult        = checkResult;
        }

        private static String buildMessage(String pluginId,
                                           PluginPermission permission,
                                           PermissionCheckResult result) {
            return switch (result) {
                case DENIED_PLUGIN -> "Plugin '" + pluginId + "' is not authorized " +
                                      "for " + permission + " — permission not granted " +
                                      "at install time";
                case DENIED_USER   -> "Current user does not have the required role " +
                                      "for " + permission + " (plugin '" + pluginId + "')";
                case GRANTED       -> throw new IllegalArgumentException(
                                      "Cannot create PluginAuthorizationException " +
                                      "for a GRANTED result");
            };
        }

        public PluginPermission      getRequiredPermission() { return requiredPermission; }
        public PermissionCheckResult getCheckResult()        { return checkResult; }
    }
}
