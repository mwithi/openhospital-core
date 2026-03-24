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

/**
 * Open Hospital Plugin SPI — zero-framework Service Provider Interface.
 *
 * <p>This module defines the public contract between the Open Hospital runtime
 * and third-party plugins. It has no dependency on Spring, Hibernate, or any
 * other framework — only on the SLF4J API.
 *
 * <h3>Security guarantee</h3>
 * No package in this module is {@code open}. This means that
 * {@code setAccessible(true)} via reflection on any class in this module
 * throws {@link java.lang.reflect.InaccessibleObjectException} at runtime,
 * even for code running in the same JVM. A plugin cannot bypass the
 * {@link org.isf.plugin.registry.PluginContext} interface to obtain a
 * reference to the concrete {@code PluginContextImpl} and extract
 * internal Spring beans from it.
 *
 * <h3>Packages exported to plugin developers</h3>
 * All seven exported packages are part of the stable public API.
 * Breaking changes to any exported type require a MAJOR version bump
 * of {@code OH-plugin-spi}.
 */
module org.isf.plugin.spi {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    // SLF4J API — optional at compile time (requires static), mandatory at runtime
    // for modules that actually use logging. Using "requires static" avoids the
    // Eclipse JPMS error when slf4j-api is not yet installed in the local repo,
    // while still allowing PluginContext.logger() to return org.slf4j.Logger.
    requires static org.slf4j;

    // java.sql is needed by MigrationScript.MigrationContext (Connection type).
    // It is part of the JDK and always available.
    requires java.sql;

    // -------------------------------------------------------------------------
    // Exported packages — the stable public API surface
    // -------------------------------------------------------------------------

    /** Core SPI interfaces: OHPlugin, MigrationScript, OHPluginLifecycleException. */
    exports org.isf.plugin.spi;

    /** Value objects and enums: PluginDescriptor, PluginCapability, PluginStatus,
     *  PluginPermission, FieldPermission, ExternalConnection. */
    exports org.isf.plugin.model;

    /** Domain field enumerations: DomainField, FieldSensitivity,
     *  PatientField, AdmissionField, LaboratoryField, WardField, PharmacyField. */
    exports org.isf.plugin.model.field;

    /** UI contribution types: UiContribution, RouteDescriptor,
     *  SlotContribution, BundleDescriptor. */
    exports org.isf.plugin.model.ui;

    /** Domain events: OHPluginEvent, OHDomainEvents, PluginEventBus. */
    exports org.isf.plugin.event;

    /** Extension point for OH-core Managers: OHManagerExtension. */
    exports org.isf.plugin.hook;

    /** Runtime context and registry: PluginContext, PluginRegistry. */
    exports org.isf.plugin.registry;

    /** Security policy SPI: PluginSecurityPolicy, ValidationResult. */
    exports org.isf.plugin.security;

    // -------------------------------------------------------------------------
    // NO "opens" declarations — this is intentional.
    //
    // Not opening any package means reflection-based access to classes in
    // this module is blocked by the JVM, even with setAccessible(true).
    // This is the key encapsulation guarantee: the PluginContextImpl
    // (in openhospital-api) cannot be reached from plugin code via reflection.
    //
    // If a future dependency (e.g. a serialization library) requires deep
    // reflection access, add a narrowly-scoped "opens ... to <module>" rather
    // than a blanket "opens" or "open module".
    // -------------------------------------------------------------------------
}
