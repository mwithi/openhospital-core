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
package org.isf.plugin.test.stub;

import org.isf.plugin.event.OHPluginEvent;
import org.isf.plugin.event.PluginEventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * In-memory implementation of {@link PluginEventBus} for unit tests.
 *
 * <p>In addition to the standard publish/subscribe contract, this stub
 * exposes inspection methods so tests can verify plugin behaviour:
 *
 * <pre>{@code
 * plugin.onStart(ctx);
 *
 * // verify subscription
 * assertThat(ctx.eventBus().subscribedTypes())
 *         .contains(OHDomainEvents.PatientCreated.class);
 *
 * // fire an event and verify plugin reaction
 * ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));
 * assertThat(ctx.eventBus().publishedEvents()).hasSize(1);
 * }</pre>
 */
public final class StubEventBus implements PluginEventBus {

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, List<Consumer>> handlers   = new HashMap<>();
    private final List<OHPluginEvent>           published  = new ArrayList<>();

    // -------------------------------------------------------------------------
    // PluginEventBus contract
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <E extends OHPluginEvent> void subscribe(Class<E> eventType,
                                                    Consumer<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @Override
    public <E extends OHPluginEvent> void unsubscribe(Class<E> eventType) {
        handlers.remove(eventType);
    }

    @Override
    public void unsubscribeAll() {
        handlers.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(OHPluginEvent event) {
        published.add(event);
        List<Consumer> eventHandlers =
                handlers.getOrDefault(event.getClass(), List.of());
        for (Consumer h : eventHandlers) {
            h.accept(event);
        }
    }

    // -------------------------------------------------------------------------
    // Inspection API — for use in test assertions only
    // -------------------------------------------------------------------------

    /**
     * Returns the set of event types that currently have at least one subscriber.
     * Useful to verify that a plugin subscribed to the expected events in
     * {@code onStart}.
     */
    public Set<Class<?>> subscribedTypes() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    /**
     * Returns all events that have been published through this bus,
     * in publication order.
     */
    public List<OHPluginEvent> publishedEvents() {
        return Collections.unmodifiableList(published);
    }

    /**
     * Returns the number of handlers registered for the given event type.
     */
    public int subscriberCount(Class<? extends OHPluginEvent> eventType) {
        List<?> h = handlers.get(eventType);
        return h == null ? 0 : h.size();
    }

    /**
     * Clears all published events from the history without affecting subscribers.
     * Useful to reset state between test steps.
     */
    public void clearPublished() {
        published.clear();
    }
}
