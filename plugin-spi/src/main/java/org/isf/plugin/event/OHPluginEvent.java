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
package org.isf.plugin.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable base class for all domain events published toward OH plugins.
 *
 * <h3>Design</h3>
 * Events are immutable value objects. They must not hold references to Spring
 * beans, an {@code EntityManager}, or JPA sessions — only plain serializable
 * data. This ensures that a plugin can receive and process events without
 * depending on Spring in its own classpath.
 *
 * <h3>Extension</h3>
 * Create concrete subclasses for each domain event:
 * <pre>{@code
 * public final class OHDomainEvents {
 *     public static final class PatientCreated extends OHPluginEvent {
 *         private final int patientCode;
 *         public PatientCreated(int patientCode, String firstName, String lastName) {
 *             super("PATIENT_CREATED", "openhospital-core");
 *             this.patientCode = patientCode;
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Publishing (core/api side)</h3>
 * <pre>{@code
 * pluginContext.eventBus().publish(
 *     new OHDomainEvents.PatientCreated(patient.getCode(), ...));
 * }</pre>
 *
 * <h3>Subscribing (plugin side)</h3>
 * <pre>{@code
 * ctx.eventBus().subscribe(OHDomainEvents.PatientCreated.class, event -> {
 *     log.info("New patient: {}", event.getLastName());
 * });
 * }</pre>
 */
public abstract class OHPluginEvent {

    /** Unique identifier of this event instance. */
    private final String eventId;

    /** Event type used for routing and logging, e.g. "PATIENT_CREATED". */
    private final String eventType;

    /** Source of the event, e.g. "openhospital-core" or "org.isf.radiology". */
    private final String source;

    /** UTC timestamp of when this event was created. */
    private final Instant occurredAt;

    protected OHPluginEvent(String eventType, String source) {
        this.eventId    = UUID.randomUUID().toString();
        this.eventType  = Objects.requireNonNull(eventType, "eventType must not be null");
        this.source     = Objects.requireNonNull(source, "source must not be null");
        this.occurredAt = Instant.now();
    }

    public String  getEventId()    { return eventId; }
    public String  getEventType()  { return eventType; }
    public String  getSource()     { return source; }
    public Instant getOccurredAt() { return occurredAt; }

    @Override
    public String toString() {
        return "OHPluginEvent{eventId='" + eventId + '\'' +
               ", eventType='" + eventType + '\'' +
               ", source='" + source + '\'' +
               ", occurredAt=" + occurredAt + '}';
    }
}
