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
package org.isf.plugin.hook;

import org.isf.plugin.model.PluginDescriptor;

/**
 * Extension point for OH-core Manager classes.
 *
 * <p>A plugin may register implementations of this interface to intercept or
 * extend the behaviour of existing Managers (e.g. {@code PatientBrowsingManager},
 * {@code AdmissionBrowsingManager}).
 *
 * <p>The mechanism is a <em>chain of responsibility</em>: the original Manager
 * remains untouched; the extension is called before ({@link #beforeOperation})
 * or after ({@link #afterOperation}) the original method.
 *
 * <h3>Example: audit log on patient creation</h3>
 * <pre>{@code
 * public class PatientAuditExtension implements OHManagerExtension {
 *
 *     @Override
 *     public String getTargetManagerClass() {
 *         return "org.isf.patient.manager.PatientBrowsingManager";
 *     }
 *
 *     @Override
 *     public void afterOperation(String methodName, Object[] args, Object result) {
 *         if ("newPatient".equals(methodName)) {
 *             auditService.log("PATIENT_CREATED", args[0]);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Registration</h3>
 * <pre>{@code
 * // inside OHPlugin.onStart():
 * ctx.managers().register(new PatientAuditExtension());
 * }</pre>
 *
 * <h3>Security</h3>
 * The PluginManager verifies that the plugin holds the necessary capabilities
 * and permissions for the target manager before registering the extension.
 * A plugin without {@code READ_PATIENT} cannot register an extension on
 * {@code PatientBrowsingManager}.
 */
public interface OHManagerExtension {

    /**
     * Fully-qualified class name of the Manager this extension targets.
     * Must match exactly one of the OH-core Manager classes.
     *
     * @return FQCN of the target Manager (e.g. {@code "org.isf.patient.manager.PatientBrowsingManager"})
     */
    String getTargetManagerClass();

    /**
     * Returns the descriptor of the plugin that owns this extension.
     * Used for logging, auditing, and permission checks.
     *
     * @return the {@link PluginDescriptor} of the owning plugin
     */
    PluginDescriptor getOwnerPlugin();

    /**
     * Called <em>before</em> the original Manager method executes.
     *
     * <p>If this method throws, the original method is <strong>not</strong>
     * executed and the exception is propagated to the caller.
     * Use with care: blocking a core method can impair system operation.
     *
     * <p>Default implementation is a no-op.
     *
     * @param methodName name of the Manager method about to be executed
     * @param args       arguments passed to the method (empty array if none)
     * @throws Exception if the operation should be blocked
     */
    default void beforeOperation(String methodName, Object[] args) throws Exception {
        // no-op
    }

    /**
     * Called <em>after</em> the original Manager method has executed.
     *
     * <p>Exceptions thrown by this method are logged but not propagated to
     * the original caller — a faulty extension must not break the core flow.
     *
     * <p>Default implementation is a no-op.
     *
     * @param methodName name of the Manager method just executed
     * @param args       arguments passed to the method
     * @param result     value returned by the method (null if void or if it threw)
     */
    default void afterOperation(String methodName, Object[] args, Object result) {
        // no-op
    }
}
