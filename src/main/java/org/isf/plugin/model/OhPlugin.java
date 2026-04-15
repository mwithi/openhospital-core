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
package org.isf.plugin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Represents an installed OH plugin.
 *
 * <p>One row per plugin. The {@code pluginId} (reverse-domain, e.g.
 * {@code org.isf.plugin.example.patientaudit}) is the natural primary key
 * and is validated by {@link org.isf.plugin.model.PluginDescriptor} at
 * install time.
 *
 * <p>{@code manifestJson} stores the manifest exactly as it was read from the
 * ZIP at install time — after the administrator approved it. If the JAR is
 * replaced on disk, OH can detect the discrepancy by comparing the live
 * manifest with the stored one.
 */
@Entity
@Table(name = "OH_PLUGIN")
public class OhPlugin {

    /**
     * Plugin identifier in reverse-domain notation, e.g.
     * {@code org.isf.plugin.example.patientaudit}. No hyphens allowed.
     */
    @Id
    @Column(name = "PLG_ID", nullable = false, length = 255)
    private String pluginId;

    @Column(name = "PLG_VERSION", nullable = false, length = 50)
    private String version;

    @Column(name = "PLG_NAME", nullable = false, length = 255)
    private String name;

    /**
     * Lifecycle status of the plugin.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "PLG_STATUS", nullable = false, length = 50)
    private PluginStatus status;

    @Column(name = "PLG_INSTALLED_AT", nullable = false)
    private LocalDateTime installedAt;

    @Column(name = "PLG_INSTALLED_BY", nullable = false, length = 255)
    private String installedBy;

    /**
     * Absolute path to the plugin JAR on the OH server filesystem.
     * Stored at install time. If the file is missing or the manifest
     * inside it diverges from {@code manifestJson}, the plugin is
     * moved to {@code FAILED} status on next startup.
     */
    @Column(name = "PLG_JAR_PATH", nullable = false, length = 500)
    private String jarPath;

    /**
     * The full content of {@code manifest.json} as approved by the
     * administrator at install time. Used to:
     * <ul>
     *   <li>Reconstruct {@link org.isf.plugin.model.PluginDescriptor}
     *       on every OH startup without re-reading the JAR.</li>
     *   <li>Detect JAR tampering by comparing with the live manifest.</li>
     * </ul>
     */
    @Column(name = "PLG_MANIFEST_JSON", nullable = false, columnDefinition = "TEXT")
    private String manifestJson;

    @Column(name = "PLG_LOCK_VERSION")
    private Integer lockVersion;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected OhPlugin() {}

    public OhPlugin(String pluginId, String version, String name,
                    PluginStatus status, LocalDateTime installedAt,
                    String installedBy, String jarPath, String manifestJson) {
        this.pluginId     = pluginId;
        this.version      = version;
        this.name         = name;
        this.status       = status;
        this.installedAt  = installedAt;
        this.installedBy  = installedBy;
        this.jarPath      = jarPath;
        this.manifestJson = manifestJson;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String        getPluginId()     { return pluginId; }
    public String        getVersion()      { return version; }
    public String        getName()         { return name; }
    public PluginStatus  getStatus()       { return status; }
    public LocalDateTime getInstalledAt()  { return installedAt; }
    public String        getInstalledBy()  { return installedBy; }
    public String        getJarPath()      { return jarPath; }
    public String        getManifestJson() { return manifestJson; }
    public Integer       getLockVersion()  { return lockVersion; }

    public void setVersion(String version)           { this.version = version; }
    public void setName(String name)                 { this.name = name; }
    public void setStatus(PluginStatus status)       { this.status = status; }
    public void setJarPath(String jarPath)           { this.jarPath = jarPath; }
    public void setManifestJson(String manifestJson) { this.manifestJson = manifestJson; }
    public void setLockVersion(Integer lockVersion)  { this.lockVersion = lockVersion; }

    // -------------------------------------------------------------------------
    // Status enum
    // -------------------------------------------------------------------------

    /**
     * Lifecycle states of a plugin.
     *
     * <pre>
     *  VALIDATING → (admin approves) → ACTIVE
     *  ACTIVE     → (admin disables) → DISABLED
     *  DISABLED   → (admin enables)  → ACTIVE
     *  ACTIVE     → (startup error)  → FAILED
     *  *          → (admin removes)  → (row deleted)
     * </pre>
     */
    public enum PluginStatus {
        /** Uploaded, manifest validated, waiting for administrator approval. */
        VALIDATING,
        /** Approved, installed, running. */
        ACTIVE,
        /** Manually disabled by administrator — JAR still present. */
        DISABLED,
        /** {@code onInstall} or {@code onStart} threw an exception. */
        FAILED
    }
}
