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
package org.isf.plugin.event;

import java.util.function.Consumer;

/**
 * Asynchronous publish/subscribe event bus between the OH runtime and plugins.
 *
 * <p>The concrete implementation lives in {@code openhospital-api} (Spring context)
 * and uses Spring's {@code ApplicationEventPublisher} internally.
 * Plugins receive only this zero-Spring interface via
 * {@link org.isf.plugin.registry.PluginContext} — they have no visibility
 * into the implementation.
 *
 * <h3>Delivery guarantees</h3>
 * <ul>
 *   <li>Handlers are invoked on a thread separate from the publisher
 *       (fire-and-forget): the publisher is never blocked by slow handlers.</li>
 *   <li>Exceptions thrown by a handler are logged and not propagated,
 *       so a faulty plugin cannot break the core.</li>
 *   <li>Handlers are automatically deregistered when the plugin enters
 *       {@code DISABLED} or {@code REMOVED} state.</li>
 * </ul>
 *
 * <h3>Ordering</h3>
 * Delivery order is not guaranteed across different handlers. If a plugin
 * requires a specific order, it must manage that internally.
 */
public interface PluginEventBus {

    /**
     * Publishes an event to all subscribers registered for its type.
     *
     * <p>The call returns immediately (non-blocking). Handlers are invoked
     * asynchronously.
     *
     * @param event the event to publish, must not be null
     * @throws NullPointerException if {@code event} is null
     */
    void publish(OHPluginEvent event);

    /**
     * Registers a handler for a specific event type.
     *
     * <p>A plugin may register multiple handlers for the same event type.
     * Handlers are invoked in registration order.
     *
     * @param <E>       the event type
     * @param eventType the class of the event to listen for
     * @param handler   the function to invoke when the event is published
     * @throws NullPointerException if {@code eventType} or {@code handler} is null
     */
    <E extends OHPluginEvent> void subscribe(Class<E> eventType, Consumer<E> handler);

    /**
     * Deregisters all handlers registered by the current plugin for the given event type.
     *
     * @param <E>       the event type
     * @param eventType the class of the event to deregister
     */
    <E extends OHPluginEvent> void unsubscribe(Class<E> eventType);

    /**
     * Deregisters all handlers registered by the current plugin for any event type.
     * Called automatically during {@code onStop}.
     */
    void unsubscribeAll();
}
