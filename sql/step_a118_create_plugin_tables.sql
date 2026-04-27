-- ============================================================================
-- OH Plugin System — table creation script
-- Run once on a fresh installation, or via update_X_Y-X_Z.sql on upgrade.
--
-- To apply manually:
--   MariaDB> source create_plugin_tables.sql
-- ============================================================================

-- ----------------------------------------------------------------------------
-- OH_PLUGIN
-- One row per installed plugin. PLG_ID is the natural primary key.
-- PLG_MANIFEST_JSON stores the manifest as approved by the administrator —
-- used at startup to reconstruct PluginDescriptor without re-reading the JAR.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS OH_PLUGIN (
    PLG_ID            VARCHAR(255) NOT NULL,
    PLG_VERSION       VARCHAR(50)  NOT NULL,
    PLG_NAME          VARCHAR(255) NOT NULL,
    PLG_STATUS        VARCHAR(50)  NOT NULL,
    PLG_INSTALLED_AT  DATETIME     NOT NULL,
    PLG_INSTALLED_BY  VARCHAR(255) NOT NULL,
    PLG_JAR_PATH      VARCHAR(500) NOT NULL,
    PLG_MANIFEST_JSON TEXT         NOT NULL,
    PLG_LOCK_VERSION  INT,

    CONSTRAINT PK_PLUGIN
        PRIMARY KEY (PLG_ID),
    CONSTRAINT CHK_PLUGIN_STATUS
        CHECK (PLG_STATUS IN ('VALIDATING', 'ACTIVE', 'DISABLED', 'FAILED'))
);

-- ----------------------------------------------------------------------------
-- OH_PLUGIN_APPROVAL
-- One row per approved item (capability, permission, field, connection).
-- Rows are never deleted — permanent approval audit trail.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS OH_PLUGIN_APPROVAL (
    PLA_ID            BIGINT       NOT NULL AUTO_INCREMENT,
    PLA_PLUGIN_ID     VARCHAR(255) NOT NULL,
    PLA_APPROVAL_TYPE VARCHAR(50)  NOT NULL,
    PLA_ITEM_KEY      VARCHAR(500) NOT NULL,
    PLA_APPROVED_BY   VARCHAR(255) NOT NULL,
    PLA_APPROVED_AT   DATETIME     NOT NULL,
    PLA_PURPOSE_SHOWN TEXT         NOT NULL,

    CONSTRAINT PK_PLUGIN_APPROVAL
        PRIMARY KEY (PLA_ID),
    CONSTRAINT FK_APPROVAL_PLUGIN
        FOREIGN KEY (PLA_PLUGIN_ID) REFERENCES OH_PLUGIN (PLG_ID),
    CONSTRAINT CHK_APPROVAL_TYPE
        CHECK (PLA_APPROVAL_TYPE IN
            ('CAPABILITY', 'PERMISSION', 'FIELD', 'CONNECTION', 'SENSITIVE'))
);

-- ----------------------------------------------------------------------------
-- OH_PLUGIN_EVENT
-- Lifecycle audit trail. PLE_PLUGIN_ID is nullable (ON DELETE SET NULL)
-- so history survives plugin uninstallation.
-- PLE_PLUGIN_ID_COPY preserves the pluginId for human-readable queries
-- after the plugin row is deleted.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS OH_PLUGIN_EVENT (
    PLE_ID             BIGINT       NOT NULL AUTO_INCREMENT,
    PLE_PLUGIN_ID      VARCHAR(255),
    PLE_PLUGIN_ID_COPY VARCHAR(255) NOT NULL,
    PLE_EVENT_TYPE     VARCHAR(50)  NOT NULL,
    PLE_OCCURRED_AT    DATETIME     NOT NULL,
    PLE_TRIGGERED_BY   VARCHAR(255) NOT NULL,
    PLE_DETAIL         TEXT,

    CONSTRAINT PK_PLUGIN_EVENT
        PRIMARY KEY (PLE_ID),
    CONSTRAINT FK_EVENT_PLUGIN
        FOREIGN KEY (PLE_PLUGIN_ID) REFERENCES OH_PLUGIN (PLG_ID)
        ON DELETE SET NULL,
    CONSTRAINT CHK_EVENT_TYPE
        CHECK (PLE_EVENT_TYPE IN
            ('UPLOADED', 'APPROVED', 'INSTALLED', 'STARTED',
             'STOPPED', 'DISABLED', 'ENABLED', 'UNINSTALLED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS IDX_PLUGIN_STATUS
    ON OH_PLUGIN (PLG_STATUS);

CREATE INDEX IF NOT EXISTS IDX_APPROVAL_PLUGIN
    ON OH_PLUGIN_APPROVAL (PLA_PLUGIN_ID);

CREATE INDEX IF NOT EXISTS IDX_EVENT_PLUGIN_COPY
    ON OH_PLUGIN_EVENT (PLE_PLUGIN_ID_COPY);

CREATE INDEX IF NOT EXISTS IDX_EVENT_OCCURRED
    ON OH_PLUGIN_EVENT (PLE_OCCURRED_AT DESC);
   
-- Plugin permissions
INSERT INTO OH_PERMISSIONS (P_ID_A, P_NAME, P_DESCRIPTION, P_ACTIVE, P_CREATED_BY, P_CREATED_DATE, P_LAST_MODIFIED_BY, P_LAST_MODIFIED_DATE)
    VALUES (172, 'plugins.create', 'Upload and install a new plugin', 1, NULL, NULL, NULL, NULL);
INSERT INTO OH_PERMISSIONS (P_ID_A, P_NAME, P_DESCRIPTION, P_ACTIVE, P_CREATED_BY, P_CREATED_DATE, P_LAST_MODIFIED_BY, P_LAST_MODIFIED_DATE)
    VALUES (173, 'plugins.read',   'List and view installed plugins', 1, NULL, NULL, NULL, NULL);
INSERT INTO OH_PERMISSIONS (P_ID_A, P_NAME, P_DESCRIPTION, P_ACTIVE, P_CREATED_BY, P_CREATED_DATE, P_LAST_MODIFIED_BY, P_LAST_MODIFIED_DATE)
    VALUES (174, 'plugins.update', 'Approve, enable, disable, or update a plugin', 1, NULL, NULL, NULL, NULL);
INSERT INTO OH_PERMISSIONS (P_ID_A, P_NAME, P_DESCRIPTION, P_ACTIVE, P_CREATED_BY, P_CREATED_DATE, P_LAST_MODIFIED_BY, P_LAST_MODIFIED_DATE)
    VALUES (175, 'plugins.delete', 'Uninstall a plugin', 1, NULL, NULL, NULL, NULL);
 
-- Grant full plugin management to admin
INSERT INTO OH_GROUPPERMISSION (GP_UG_ID_A, GP_P_ID_A, GP_ACTIVE, GP_CREATED_BY, GP_CREATED_DATE, GP_LAST_MODIFIED_BY, GP_LAST_MODIFIED_DATE)
    VALUES ('admin', 172, 1, NULL, NULL, NULL, NULL);
INSERT INTO OH_GROUPPERMISSION (GP_UG_ID_A, GP_P_ID_A, GP_ACTIVE, GP_CREATED_BY, GP_CREATED_DATE, GP_LAST_MODIFIED_BY, GP_LAST_MODIFIED_DATE)
    VALUES ('admin', 173, 1, NULL, NULL, NULL, NULL);
INSERT INTO OH_GROUPPERMISSION (GP_UG_ID_A, GP_P_ID_A, GP_ACTIVE, GP_CREATED_BY, GP_CREATED_DATE, GP_LAST_MODIFIED_BY, GP_LAST_MODIFIED_DATE)
    VALUES ('admin', 174, 1, NULL, NULL, NULL, NULL);
INSERT INTO OH_GROUPPERMISSION (GP_UG_ID_A, GP_P_ID_A, GP_ACTIVE, GP_CREATED_BY, GP_CREATED_DATE, GP_LAST_MODIFIED_BY, GP_LAST_MODIFIED_DATE)
    VALUES ('admin', 175, 1, NULL, NULL, NULL, NULL);
