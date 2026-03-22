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

import org.isf.plugin.model.ui.UiContribution;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable descriptor of an Open Hospital plugin.
 *
 * <p>Corresponds to the {@code manifest.json} file contained in the plugin's
 * ZIP/TAR.GZ package. It is deserialized by the PluginPackageReader (in
 * openhospital-api) <em>before</em> any loading operation, so malformed
 * packages can be rejected before the JAR is even opened.
 *
 * <h3>Example manifest.json</h3>
 * <pre>{@code
 * {
 *   "pluginId":      "org.isf.radiology",
 *   "version":       "2.1.0",
 *   "name":          "Radiology Module",
 *   "description":   "DICOM image management and radiology reports",
 *   "vendor":        "Informatici Senza Frontiere",
 *   "license":       "GPL-3.0",
 *   "minCoreVersion":"1.15.0",
 *   "maxCoreVersion":"2.x",
 *   "entryPoint":    "org.isf.radiology.RadiologyPlugin",
 *   "capabilities":  ["API_EXTENSION","DB_MIGRATION","UI_ROUTES"],
 *   "permissions":   ["READ_PATIENT","READ_LABORATORY"],
 *   "dependencies":  [],
 *   "externalConnections": [
 *     {
 *       "host":      "pacs.hospital.org",
 *       "port":      11112,
 *       "protocol":  "DICOM",
 *       "purpose":   "Send DICOM study to hospital PACS server",
 *       "direction": "OUTBOUND"
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>Use the builder to construct instances; all required fields are validated
 * in {@link Builder#build()}.
 *
 * <h3>Privacy by design</h3>
 * <p>The {@code externalConnections} field is the formal declaration of every
 * outbound network connection the plugin is allowed to open. An empty list
 * means the plugin must not make any network calls outside the OH runtime.
 * The PluginManager enforces this declaration at install time and at runtime
 * via the restricted {@code PluginClassLoader}.
 */
public final class PluginDescriptor {

    /** Pattern for pluginId: reverse-domain notation, e.g. "org.isf.radiology". */
    private static final Pattern PLUGIN_ID_PATTERN =
            Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)+$");

    /** Pattern for semantic version, e.g. "1.2.3" or "1.2.3-SNAPSHOT". */
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^\\d+\\.\\d+\\.\\d+(-[A-Za-z0-9.]+)?$");

    /**
     * Pattern for the entryPoint fully-qualified class name.
     * At least one package segment is required — a bare class name is rejected.
     */
    private static final Pattern FQCN_PATTERN =
            Pattern.compile("^([a-zA-Z_$][a-zA-Z\\d_$]*\\.)+[a-zA-Z_$][a-zA-Z\\d_$]*$");

    // -------------------------------------------------------------------------
    // Required fields
    // -------------------------------------------------------------------------

    /** Unique plugin identifier in reverse-domain notation. */
    private final String pluginId;

    /** Semantic version of the plugin. */
    private final String version;

    /** Human-readable display name. */
    private final String name;

    /** Fully-qualified class name of the class implementing {@link org.isf.plugin.spi.OHPlugin}. */
    private final String entryPoint;

    /** Minimum OH-core version required (inclusive). */
    private final String minCoreVersion;

    // -------------------------------------------------------------------------
    // Optional fields
    // -------------------------------------------------------------------------

    /** Short description of the plugin. */
    private final String description;

    /** Author or vendor of the plugin. */
    private final String vendor;

    /** License identifier, e.g. "GPL-3.0", "MIT". */
    private final String license;

    /**
     * Maximum compatible OH-core version (inclusive, null means no upper bound).
     * The wildcard "2.x" is accepted for minor/patch.
     */
    private final String maxCoreVersion;

    /**
     * Capabilities declared by the plugin. Only the capabilities listed here
     * may be exercised; any attempt to use undeclared capabilities at runtime
     * is blocked by the security gate.
     *
     * @see PluginCapability
     */
    private final List<PluginCapability> capabilities;

    /**
     * Data permissions required by the plugin.
     * The authorization system checks these before allowing installation.
     *
     * @see PluginPermission
     */
    private final List<PluginPermission> permissions;

    /**
     * Plugin IDs this plugin depends on (must all be installed and active
     * before this plugin can be activated).
     */
    private final List<String> dependencies;

    /**
     * Fine-grained field-level data permissions.
     *
     * <p>Declares exactly which fields of each domain the plugin needs to
     * read or write, and the documented purpose for each access.
     * This replaces the coarse-grained {@link PluginPermission} entries for
     * data access — {@code PluginPermission} is retained only for non-data
     * permissions such as {@code READ_USERS} or {@code WRITE_SYSTEM_CONFIG}.
     *
     * @see FieldPermission
     */
    private final List<FieldPermission> fieldPermissions;

    /**
     * UI contributions this plugin makes to the OH React host application.
     *
     * <p>Null if the plugin has no UI contributions (BE-only plugins).
     * Must be non-null if {@link PluginCapability#UI_ROUTES} or
     * {@link PluginCapability#UI_COMPONENT_OVERRIDE} is declared.
     *
     * @see UiContribution
     */
    private final UiContribution uiContribution;

    /**
     * Explicit declaration of all outbound network connections this plugin
     * may open. An empty list means the plugin is not allowed to make any
     * external network calls.
     *
     * <p>Privacy by design: the administrator reviews and approves each entry
     * at install time. The PluginClassLoader blocks any connection attempt
     * toward a host not present in this list.
     *
     * @see ExternalConnection
     */
    private final List<ExternalConnection> externalConnections;

    // -------------------------------------------------------------------------
    // Private constructor — use the builder
    // -------------------------------------------------------------------------

    private PluginDescriptor(Builder builder) {
        this.pluginId            = builder.pluginId;
        this.version             = builder.version;
        this.name                = builder.name;
        this.entryPoint          = builder.entryPoint;
        this.minCoreVersion      = builder.minCoreVersion;
        this.description         = builder.description;
        this.vendor              = builder.vendor;
        this.license             = builder.license;
        this.maxCoreVersion      = builder.maxCoreVersion;
        this.capabilities        = List.copyOf(builder.capabilities);
        this.permissions         = List.copyOf(builder.permissions);
        this.dependencies        = List.copyOf(builder.dependencies);
        this.externalConnections  = List.copyOf(builder.externalConnections);
        this.fieldPermissions     = List.copyOf(builder.fieldPermissions);
        this.uiContribution       = builder.uiContribution;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getPluginId()       { return pluginId; }
    public String getVersion()        { return version; }
    public String getName()           { return name; }
    public String getEntryPoint()     { return entryPoint; }
    public String getMinCoreVersion() { return minCoreVersion; }
    public String getDescription()    { return description; }
    public String getVendor()         { return vendor; }
    public String getLicense()        { return license; }
    public String getMaxCoreVersion() { return maxCoreVersion; }

    public List<PluginCapability>    getCapabilities()        { return capabilities; }
    public List<PluginPermission>    getPermissions()         { return permissions; }
    public List<String>              getDependencies()        { return dependencies; }
    public List<ExternalConnection>  getExternalConnections() { return externalConnections; }

    public List<FieldPermission> getFieldPermissions() { return fieldPermissions; }

    /** Returns the UI contribution block, or {@code null} for BE-only plugins. */
    public UiContribution getUiContribution() { return uiContribution; }

    /** Returns {@code true} if this plugin contributes anything to the React UI. */
    public boolean hasUiContribution() { return uiContribution != null; }

    /**
     * Returns {@code true} if any declared {@link FieldPermission} requires
     * explicit administrator approval (i.e. contains a SENSITIVE field).
     */
    public boolean requiresExplicitApproval() {
        return fieldPermissions.stream().anyMatch(FieldPermission::requiresExplicitApproval);
    }

    /** Returns {@code true} if the plugin declares the given capability. */
    public boolean hasCapability(PluginCapability capability) {
        return capabilities.contains(capability);
    }

    /** Returns {@code true} if the plugin requires the given permission. */
    public boolean requiresPermission(PluginPermission permission) {
        return permissions.contains(permission);
    }

    /**
     * Returns {@code true} if this plugin declares at least one external
     * connection — i.e. it is allowed to make outbound network calls.
     */
    public boolean hasExternalConnections() {
        return !externalConnections.isEmpty();
    }

    /**
     * Returns {@code true} if the given host and port are declared in the
     * {@code externalConnections} block of this plugin's manifest.
     * Used by the PluginClassLoader at runtime to validate connection attempts.
     *
     * @param host the target hostname or IP address
     * @param port the target port
     * @return {@code true} if the connection is explicitly declared
     */
    public boolean isConnectionAllowed(String host, int port) {
        return externalConnections.stream()
                .anyMatch(c -> c.host().equalsIgnoreCase(host) && c.port() == port);
    }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginDescriptor)) return false;
        PluginDescriptor that = (PluginDescriptor) o;
        return Objects.equals(pluginId, that.pluginId) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, version);
    }

    @Override
    public String toString() {
        return "PluginDescriptor{pluginId='" + pluginId + '\'' +
               ", version='" + version + '\'' +
               ", name='" + name + '\'' +
               ", capabilities=" + capabilities +
               ", externalConnections=" + externalConnections.size() + '}';
    }

    // =========================================================================
    // ExternalConnection
    // =========================================================================

    /**
     * Declares a single external network connection a plugin is allowed to open.
     *
     * <p>Every field is required. The administrator reviews all declared connections
     * at install time and must explicitly approve each one before the plugin
     * is activated.
     *
     * @param host      hostname or IP address of the remote endpoint
     * @param port      TCP/UDP port of the remote endpoint
     * @param protocol  application protocol, e.g. "HTTPS", "DICOM", "HL7-MLLP", "FHIR"
     * @param purpose   human-readable explanation of why this connection is needed;
     *                  shown to the administrator during install
     * @param direction whether the plugin initiates the connection, receives it, or both
     */
    public record ExternalConnection(
            String    host,
            int       port,
            String    protocol,
            String    purpose,
            Direction direction
    ) {
        /** Direction of the network connection relative to the plugin. */
        public enum Direction {
            /** Plugin opens a connection to the remote host. */
            OUTBOUND,
            /** Plugin listens for incoming connections (rare, requires firewall config). */
            INBOUND,
            /** Plugin both initiates and receives on this endpoint. */
            BOTH
        }

        public ExternalConnection {
            Objects.requireNonNull(host,      "ExternalConnection.host must not be null");
            Objects.requireNonNull(protocol,  "ExternalConnection.protocol must not be null");
            Objects.requireNonNull(purpose,   "ExternalConnection.purpose must not be null");
            Objects.requireNonNull(direction, "ExternalConnection.direction must not be null");
            if (host.isBlank()) {
                throw new IllegalArgumentException("ExternalConnection.host must not be blank");
            }
            if (purpose.isBlank()) {
                throw new IllegalArgumentException("ExternalConnection.purpose must not be blank");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                    "ExternalConnection.port must be between 1 and 65535, got: " + port);
            }
        }
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        // required
        private String pluginId;
        private String version;
        private String name;
        private String entryPoint;
        private String minCoreVersion;

        // optional with defaults
        private String description       = "";
        private String vendor            = "";
        private String license           = "unknown";
        private String maxCoreVersion    = null;
        private List<PluginCapability>   capabilities        = Collections.emptyList();
        private List<PluginPermission>   permissions         = Collections.emptyList();
        private List<String>             dependencies        = Collections.emptyList();
        private List<ExternalConnection> externalConnections = Collections.emptyList();
        private List<FieldPermission>    fieldPermissions    = Collections.emptyList();
        private UiContribution           uiContribution      = null;

        private Builder() {}

        public Builder pluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder entryPoint(String entryPoint) {
            this.entryPoint = entryPoint;
            return this;
        }

        public Builder minCoreVersion(String minCoreVersion) {
            this.minCoreVersion = minCoreVersion;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder vendor(String vendor) {
            this.vendor = vendor;
            return this;
        }

        public Builder license(String license) {
            this.license = license;
            return this;
        }

        public Builder maxCoreVersion(String maxCoreVersion) {
            this.maxCoreVersion = maxCoreVersion;
            return this;
        }

        public Builder capabilities(List<PluginCapability> capabilities) {
            this.capabilities = capabilities != null ? capabilities : Collections.emptyList();
            return this;
        }

        public Builder permissions(List<PluginPermission> permissions) {
            this.permissions = permissions != null ? permissions : Collections.emptyList();
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies = dependencies != null ? dependencies : Collections.emptyList();
            return this;
        }

        public Builder externalConnections(List<ExternalConnection> externalConnections) {
            this.externalConnections = externalConnections != null
                    ? externalConnections : Collections.emptyList();
            return this;
        }

        public Builder uiContribution(UiContribution uiContribution) {
            this.uiContribution = uiContribution;
            return this;
        }

        public Builder fieldPermissions(List<FieldPermission> fieldPermissions) {
            this.fieldPermissions = fieldPermissions != null
                    ? fieldPermissions : Collections.emptyList();
            return this;
        }

        /**
         * Builds the {@link PluginDescriptor}, validating all required fields.
         *
         * @throws IllegalArgumentException if a required field is null, blank, or malformed.
         */
        public PluginDescriptor build() {
            requireNonBlank(pluginId,       "pluginId");
            requireNonBlank(version,        "version");
            requireNonBlank(name,           "name");
            requireNonBlank(entryPoint,     "entryPoint");
            requireNonBlank(minCoreVersion, "minCoreVersion");

            if (!PLUGIN_ID_PATTERN.matcher(pluginId).matches()) {
                throw new IllegalArgumentException(
                    "pluginId '" + pluginId + "' is invalid: use reverse-domain notation " +
                    "(e.g. org.isf.radiology)");
            }
            if (!VERSION_PATTERN.matcher(version).matches()) {
                throw new IllegalArgumentException(
                    "version '" + version + "' is invalid: use semver " +
                    "(e.g. 1.2.3 or 1.2.3-SNAPSHOT)");
            }
            if (!FQCN_PATTERN.matcher(entryPoint).matches()) {
                throw new IllegalArgumentException(
                    "entryPoint '" + entryPoint +
                    "' is not a valid fully-qualified class name");
            }
            for (String dep : dependencies) {
                if (!PLUGIN_ID_PATTERN.matcher(dep).matches()) {
                    throw new IllegalArgumentException(
                        "Dependency '" + dep + "' is not a valid pluginId");
                }
            }

            // UI consistency: capabilities must match uiContribution
            if (uiContribution != null) {
                if (uiContribution.hasRoutes() &&
                        !capabilities.contains(PluginCapability.UI_ROUTES)) {
                    throw new IllegalArgumentException(
                        "UiContribution declares routes but PluginCapability.UI_ROUTES " +
                        "is not listed in capabilities");
                }
                if (uiContribution.hasSlots() &&
                        !capabilities.contains(PluginCapability.UI_COMPONENT_OVERRIDE)) {
                    throw new IllegalArgumentException(
                        "UiContribution declares slots but " +
                        "PluginCapability.UI_COMPONENT_OVERRIDE is not listed in capabilities");
                }
            }
            if (capabilities.contains(PluginCapability.UI_ROUTES) && uiContribution == null) {
                throw new IllegalArgumentException(
                    "PluginCapability.UI_ROUTES declared but no uiContribution specified");
            }
            if (capabilities.contains(PluginCapability.UI_COMPONENT_OVERRIDE)
                    && uiContribution == null) {
                throw new IllegalArgumentException(
                    "PluginCapability.UI_COMPONENT_OVERRIDE declared but " +
                    "no uiContribution specified");
            }

            return new PluginDescriptor(this);
        }

        private static void requireNonBlank(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                    "Required field '" + fieldName +
                    "' is missing or blank in the plugin manifest");
            }
        }
    }
}
