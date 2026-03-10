package org.isf.core.framework;

import jakarta.persistence.EntityManager;

import org.isf.utils.db.DbJpaUtil;
import org.isf.utils.exception.OHException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges the new {@link DatabaseGateway} abstraction with the historical
 * {@link DbJpaUtil} helper. The adapter takes care of opening and safely closing the
 * {@link EntityManager} used by the provided units of work.
 */
final class LegacyDatabaseGateway implements DatabaseGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyDatabaseGateway.class);

    @Override
    public <T> T query(DatabaseWork<T> callback) throws OHException {
        try (ManagedEntityManager manager = ManagedEntityManager.open()) {
            return callback.execute(manager.entityManager());
        }
    }

    @Override
    public void execute(DatabaseVoidWork callback) throws OHException {
        try (ManagedEntityManager manager = ManagedEntityManager.open()) {
            callback.execute(manager.entityManager());
        }
    }

    private static final class ManagedEntityManager implements AutoCloseable {

        private final EntityManager entityManager;

        private ManagedEntityManager(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        static ManagedEntityManager open() throws OHException {
            DbJpaUtil dbJpaUtil = new DbJpaUtil();
            dbJpaUtil.open();
            return new ManagedEntityManager(dbJpaUtil.getEntityManager());
        }

        EntityManager entityManager() {
            return entityManager;
        }

        @Override
        public void close() {
            if (entityManager != null && entityManager.isOpen()) {
                try {
                    // DbJpaUtil stores this EntityManager in a static field used by legacy code.
                    // Clearing it ensures unit of work isolation without breaking the singleton.
                    entityManager.clear();
                } catch (RuntimeException runtimeException) {
                    LOGGER.warn("Unable to clear EntityManager", runtimeException);
                }
            }
        }
    }
}
