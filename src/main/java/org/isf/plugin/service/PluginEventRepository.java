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
package org.isf.plugin.service;

import java.util.List;

import org.isf.plugin.model.OhPluginEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link OhPluginEvent}.
 */
public interface PluginEventRepository extends JpaRepository<OhPluginEvent, Long> {

	/**
	 * Returns the full lifecycle history of a plugin ordered by time. Uses the denormalised {@code pluginIdCopy} column so the history survives plugin
	 * deletion.
	 */
	List<OhPluginEvent> findByPluginIdCopyOrderByOccurredAtAsc(String pluginId);

	/**
	 * Returns the last N events across all plugins — used by the admin dashboard to show recent activity.
	 */
	List<OhPluginEvent> findTop50ByOrderByOccurredAtDesc();
}
