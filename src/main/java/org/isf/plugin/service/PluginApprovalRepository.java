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

import org.isf.plugin.model.OhPluginApproval;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link OhPluginApproval}.
 */
public interface PluginApprovalRepository extends JpaRepository<OhPluginApproval, Long> {

	/**
	 * Returns all approvals for a given plugin, ordered by approval time. Used to reconstruct the full approval history in the admin detail view.
	 */
	List<OhPluginApproval> findByPluginPluginIdOrderByApprovedAtAsc(String pluginId);
}
