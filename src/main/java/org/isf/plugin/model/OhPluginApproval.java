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
 * Records a single item explicitly approved by an administrator during
 * plugin installation.
 *
 * <p>Each declared capability, permission, field permission, and external
 * connection produces one row. This gives the audit trail granularity: you
 * can see exactly which administrator approved exactly which permission,
 * when, and after reading which purpose statement.
 *
 * <p>Rows are never updated — they are inserted once at approval time and
 * kept forever for auditability. Uninstalling a plugin does NOT delete
 * these rows.
 *
 * <h3>Example rows for the patient-audit plugin</h3>
 * <pre>
 * | approval_type | item_key                        | purpose_shown                          |
 * |---------------|---------------------------------|----------------------------------------|
 * | CAPABILITY    | EVENT_LISTENER                  | (no purpose — capability)              |
 * | CAPABILITY    | LOG_FILE_WRITE                  | (no purpose — capability)              |
 * | PERMISSION    | READ_PATIENT                    | (no purpose — permission)              |
 * | FIELD         | Patient.firstName+lastName      | Identify patient in audit log entry    |
 * </pre>
 */
@Entity
@Table(name = "OH_PLUGIN_APPROVAL")
public class OhPluginApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PLA_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PLA_PLUGIN_ID", nullable = false)
    private OhPlugin plugin;

    /**
     * Category of the approved item.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "PLA_APPROVAL_TYPE", nullable = false, length = 50)
    private ApprovalType approvalType;

    /**
     * The specific item approved. Format depends on {@code approvalType}:
     * <ul>
     *   <li>{@code CAPABILITY} — enum name, e.g. {@code LOG_FILE_WRITE}</li>
     *   <li>{@code PERMISSION} — enum name, e.g. {@code READ_PATIENT}</li>
     *   <li>{@code FIELD} — {@code Domain.field1+field2}, e.g. {@code Patient.firstName+lastName}</li>
     *   <li>{@code CONNECTION} — {@code host:port}, e.g. {@code pacs.hospital.org:11112}</li>
     *   <li>{@code SENSITIVE} — field name requiring explicit approval, e.g. {@code Patient.taxCode}</li>
     * </ul>
     */
    @Column(name = "PLA_ITEM_KEY", nullable = false, length = 500)
    private String itemKey;

    @Column(name = "PLA_APPROVED_BY", nullable = false, length = 255)
    private String approvedBy;

    @Column(name = "PLA_APPROVED_AT", nullable = false)
    private LocalDateTime approvedAt;

    /**
     * The exact {@code purpose} text shown to the administrator at approval
     * time. Stored verbatim so that future audits can verify what was
     * disclosed to the approver.
     */
    @Column(name = "PLA_PURPOSE_SHOWN", nullable = false, columnDefinition = "TEXT")
    private String purposeShown;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected OhPluginApproval() {}

    public OhPluginApproval(OhPlugin plugin, ApprovalType approvalType,
                             String itemKey, String approvedBy,
                             LocalDateTime approvedAt, String purposeShown) {
        this.plugin       = plugin;
        this.approvalType = approvalType;
        this.itemKey      = itemKey;
        this.approvedBy   = approvedBy;
        this.approvedAt   = approvedAt;
        this.purposeShown = purposeShown;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long          getId()           { return id; }
    public OhPlugin      getPlugin()       { return plugin; }
    public ApprovalType  getApprovalType() { return approvalType; }
    public String        getItemKey()      { return itemKey; }
    public String        getApprovedBy()   { return approvedBy; }
    public LocalDateTime getApprovedAt()   { return approvedAt; }
    public String        getPurposeShown() { return purposeShown; }

    // -------------------------------------------------------------------------
    // Approval type enum
    // -------------------------------------------------------------------------

    public enum ApprovalType {
        /** A declared {@link org.isf.plugin.model.PluginCapability}. */
        CAPABILITY,
        /** A declared {@link org.isf.plugin.model.PluginPermission}. */
        PERMISSION,
        /** A declared {@link org.isf.plugin.model.FieldPermission} block. */
        FIELD,
        /** A declared {@link org.isf.plugin.model.PluginDescriptor.ExternalConnection}. */
        CONNECTION,
        /** A SENSITIVE field requiring explicit extra approval. */
        SENSITIVE
    }
}
