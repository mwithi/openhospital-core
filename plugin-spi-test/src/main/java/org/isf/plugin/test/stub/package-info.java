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
 * In-memory stub implementations of all {@code OH-plugin-spi} interfaces.
 *
 * <p>Add {@code OH-plugin-spi-test} with {@code scope: test} to your plugin's
 * {@code pom.xml} and use these stubs to test your plugin without Spring,
 * a database, or a running OH instance.
 *
 * <h3>Entry point</h3>
 * {@link org.isf.plugin.test.stub.StubPluginContext} is the single object you
 * need. It wires together all other stubs internally.
 *
 * <h3>Available stubs</h3>
 * <ul>
 *   <li>{@link org.isf.plugin.test.stub.StubPluginContext} — implements
 *       {@code PluginContext}; entry point for all tests</li>
 *   <li>{@link org.isf.plugin.test.stub.StubEventBus} — implements
 *       {@code PluginEventBus}; exposes {@code subscribedTypes()},
 *       {@code publishedEvents()}</li>
 *   <li>{@link org.isf.plugin.test.stub.StubDataAccessor} — implements
 *       {@code PluginDataAccessor}; backed by {@link org.isf.plugin.test.stub.StubPatientStore}</li>
 *   <li>{@link org.isf.plugin.test.stub.StubPatientStore} — fluent builder
 *       for in-memory patient data</li>
 *   <li>{@link org.isf.plugin.test.stub.StubHttpClient} — implements
 *       {@code PluginHttpClient}; pre-configurable responses, fails loud
 *       if an unexpected URL is requested</li>
 * </ul>
 */
package org.isf.plugin.test.stub;
