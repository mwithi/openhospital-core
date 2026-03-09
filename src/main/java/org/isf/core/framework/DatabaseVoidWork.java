package org.isf.core.framework;

import jakarta.persistence.EntityManager;

/**
 * Functional interface for executing void work within a managed
 * {@link EntityManager} context.
 */
@FunctionalInterface
public interface DatabaseVoidWork {

    void execute(EntityManager entityManager);
}
