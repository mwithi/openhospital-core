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
package org.isf.plugin.model.ui;

import java.util.Objects;

/**
 * Declares that a plugin contributes a React component to an existing
 * UI extension slot ({@code <PluginSlot>}) in the OH host application.
 *
 * <h3>Slot mechanism</h3>
 * The OH React host renders named {@code <PluginSlot id="...">} markers
 * throughout the UI. Plugins declare which slots they target and how their
 * component should be placed relative to existing content.
 *
 * <h3>Slot IDs</h3>
 * Slot IDs follow the pattern {@code <domain>.<area>.<position>}, e.g.:
 * <ul>
 *   <li>{@code patient.header.actions} — action buttons in the patient header</li>
 *   <li>{@code patient.detail.sidebar} — sidebar of the patient detail page</li>
 *   <li>{@code admission.form.extra} — extra fields in the admission form</li>
 *   <li>{@code dashboard.widgets} — widget area on the main dashboard</li>
 * </ul>
 * The catalogue of available slot IDs is maintained in the OH Plugin Dev Kit
 * documentation and the {@code oh-plugin:list-slots} Maven goal.
 *
 * @param slotId the identifier of the target slot, never null or blank
 * @param mode   how the plugin component is placed relative to existing slot content
 */
public record SlotContribution(
        String   slotId,
        SlotMode mode
) {
    /**
     * Controls how a plugin component is placed within a slot.
     */
    public enum SlotMode {
        /** Add the plugin component after existing slot content. */
        APPEND,
        /** Add the plugin component before existing slot content. */
        PREPEND,
        /**
         * Replace all existing slot content with the plugin component.
         * Use with care — only one plugin can REPLACE a given slot.
         * If multiple plugins declare REPLACE on the same slot, the
         * PluginManager rejects the second installation with a conflict error.
         */
        REPLACE
    }

    public SlotContribution {
        Objects.requireNonNull(slotId, "SlotContribution.slotId must not be null");
        Objects.requireNonNull(mode,   "SlotContribution.mode must not be null");
        if (slotId.isBlank()) {
            throw new IllegalArgumentException(
                "SlotContribution.slotId must not be blank");
        }
    }
}
