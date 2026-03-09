package org.isf.core.framework;

import jakarta.persistence.EntityManager;

/**
 * Functional interface representing a unit of work executed within a managed
 * {@link EntityManager} context.
 *
 * @param <T> the result type produced by the unit of work
 */
@FunctionalInterface
public interface DatabaseWork<T> {

    T execute(EntityManager entityManager);
}
