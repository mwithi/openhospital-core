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
package org.isf.plugin.security;

import org.isf.plugin.model.PluginDescriptor;

import java.io.InputStream;
import java.util.List;

/**
 * Security policy applied to every plugin package before installation.
 *
 * <p>The PluginManager (in openhospital-api) applies all registered policies
 * in sequence. If even one fails, installation is rejected and the plugin
 * is left in {@link org.isf.plugin.model.PluginStatus#REJECTED} state.
 *
 * <p>Concrete implementations live in {@code openhospital-api}:
 * <ul>
 *   <li>{@code SignatureVerificationPolicy} — verifies the GPG/PGP signature</li>
 *   <li>{@code ChecksumVerificationPolicy} — verifies the SHA-256 checksum</li>
 *   <li>{@code ManifestAllowlistPolicy} — checks declared capabilities against the admin allowlist</li>
 *   <li>{@code CoreVersionCompatibilityPolicy} — semver compatibility with the running OH-core</li>
 *   <li>{@code DependencyResolutionPolicy} — checks that all declared dependencies are active</li>
 * </ul>
 *
 * <p>This SPI interface allows system plugins and tests to register custom
 * policies without modifying the PluginManager source.
 */
public interface PluginSecurityPolicy {

    /**
     * Human-readable name of this policy, used in logs and error messages.
     *
     * @return policy name, never null
     */
    String getPolicyName();

    /**
     * Executes the security check on the plugin package.
     *
     * @param context validation context providing access to the package and descriptor
     * @return the validation result
     */
    ValidationResult validate(ValidationContext context);

    // =========================================================================
    // Inner types
    // =========================================================================

    /**
     * Context passed to {@link #validate(ValidationContext)}.
     * Provides access to package data without exposing concrete implementations.
     */
    interface ValidationContext {

        /**
         * Descriptor extracted from the package's {@code manifest.json}.
         * May be null if the manifest is malformed — in that case the policy
         * must return {@link ValidationResult#failure}.
         */
        PluginDescriptor getDescriptor();

        /**
         * Stream of the raw package content (.zip or .tar.gz).
         * The stream is open and positioned at the beginning.
         * Do not close it — it is managed by the PluginManager.
         */
        InputStream getPackageStream();

        /**
         * SHA-256 checksum of the package, computed by the PluginManager
         * before passing it to the policy. Hex-encoded, lowercase.
         */
        String getPackageSha256();

        /**
         * Version of OH-core currently running, e.g. {@code "1.15.0"}.
         */
        String getCurrentCoreVersion();

        /**
         * List of currently installed and active plugins.
         * Used by the dependency resolution policy.
         */
        List<PluginDescriptor> getActivePlugins();
    }

    /**
     * Immutable result of a validation check.
     */
    final class ValidationResult {

        private final boolean valid;
        private final String  reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid  = valid;
            this.reason = reason;
        }

        /** Creates a successful result. */
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        /**
         * Creates a failure result with the reason shown to the administrator.
         *
         * @param reason explanation of the failure, must not be blank
         */
        public static ValidationResult failure(String reason) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                    "reason must not be blank in a failure ValidationResult");
            }
            return new ValidationResult(false, reason);
        }

        public boolean isValid()   { return valid; }
        public String  getReason() { return reason; }

        @Override
        public String toString() {
            return valid ? "ValidationResult{OK}"
                         : "ValidationResult{FAILED: " + reason + "}";
        }
    }
}
