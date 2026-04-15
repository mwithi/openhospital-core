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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Audit trail of lifecycle events for an installed plugin.
 *
 * <p>One row is appended for every significant event — install, start, stop,
 * enable, disable, uninstall, or failure. Rows are never updated or deleted,
 * even after the plugin is uninstalled, so the full history is always
 * available.
 *
 * <p>The {@code plugin} foreign key may reference a row that no longer exists
 * in {@code OH_PLUGIN} after uninstall — this is intentional. If referential
 * integrity is enforced at DB level, use {@code ON DELETE SET NULL} and make
 * {@code plugin} nullable.
 */
@Entity
@Table(name = "OH_PLUGIN_EVENT")
public class OhPluginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PLE_ID")
    private Long id;

    /**
     * The plugin this event belongs to. Nullable to survive plugin deletion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLE_PLUGIN_ID", nullable = true)
    private OhPlugin plugin;

    /**
     * The pluginId at the time of the event, stored separately so the history
     * is readable even after the plugin row is deleted.
     */
    @Column(name = "PLE_PLUGIN_ID_COPY", nullable = false, length = 255)
    private String pluginIdCopy;

    @Enumerated(EnumType.STRING)
    @Column(name = "PLE_EVENT_TYPE", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "PLE_OCCURRED_AT", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "PLE_TRIGGERED_BY", nullable = false, length = 255)
    private String triggeredBy;

    /**
     * Optional free-text detail — used for error messages, stack trace
     * summaries, or contextual information (e.g. the old/new version on
     * upgrade).
     */
    @Column(name = "PLE_DETAIL", columnDefinition = "TEXT")
    private String detail;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected OhPluginEvent() {}

    public OhPluginEvent(OhPlugin plugin, EventType eventType,
                          LocalDateTime occurredAt, String triggeredBy,
                          String detail) {
        this.plugin       = plugin;
        this.pluginIdCopy = plugin.getPluginId();
        this.eventType    = eventType;
        this.occurredAt   = occurredAt;
        this.triggeredBy  = triggeredBy;
        this.detail       = detail;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long          getId()           { return id; }
    public OhPlugin      getPlugin()       { return plugin; }
    public String        getPluginIdCopy() { return pluginIdCopy; }
    public EventType     getEventType()    { return eventType; }
    public LocalDateTime getOccurredAt()   { return occurredAt; }
    public String        getTriggeredBy()  { return triggeredBy; }
    public String        getDetail()       { return detail; }

    // -------------------------------------------------------------------------
    // Event type enum
    // -------------------------------------------------------------------------

    /**
     * Significant lifecycle events recorded in the audit trail.
     */
    public enum EventType {
        /** Plugin ZIP uploaded and manifest validated — awaiting admin approval. */
        UPLOADED,
        /** Administrator approved the plugin. */
        APPROVED,
        /** {@code onInstall()} called successfully. */
        INSTALLED,
        /** {@code onStart()} called successfully. */
        STARTED,
        /** {@code onStop()} called successfully. */
        STOPPED,
        /** Plugin manually disabled by administrator. */
        DISABLED,
        /** Plugin re-enabled by administrator. */
        ENABLED,
        /** {@code onUninstall()} called — plugin row will be deleted. */
        UNINSTALLED,
        /** Any lifecycle method threw an exception — see {@code detail}. */
        FAILED
    }
}
