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

import org.isf.plugin.model.OhPlugin;
import org.isf.plugin.model.OhPlugin.PluginStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data repository for {@link OhPlugin}.
 *
 * <p>
 * Intentionally minimal — all business logic lives in {@code PluginRegistryImpl} in {@code openhospital-api}.
 */
public interface PluginRepository extends JpaRepository<OhPlugin, String> {

	/**
	 * Returns all plugins in the given status. Used at startup to load all ACTIVE plugins and call {@code onStart()} on each.
	 */
	List<OhPlugin> findByStatus(PluginStatus status);

	/**
	 * Returns {@code true} if a plugin with the given id already exists, regardless of status. Used to prevent duplicate installations.
	 */
	boolean existsByPluginId(String pluginId);

	/**
	 * Returns all plugins ordered by name — used by the admin list endpoint.
	 */
	List<OhPlugin> findAllByOrderByNameAsc();

	/**
	 * Returns all active plugins whose declared minCoreVersion is compatible with the given OH core version string.
	 *
	 * <p>
	 * Compatibility check is done in Java after loading, not in SQL, because semver comparison is not natively supported in MariaDB. This query simply loads
	 * all ACTIVE plugins; the caller filters by version.
	 */
	@Query("SELECT p FROM OhPlugin p WHERE p.status = 'ACTIVE'")
	List<OhPlugin> findAllActive();
}
