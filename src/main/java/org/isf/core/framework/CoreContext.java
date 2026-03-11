package org.isf.core.framework;

import java.util.Objects;

/**
 * Represents the immutable set of services that compose the Open Hospital core runtime.
 * The context is intentionally lightweight and only exposes the minimal cross-cutting
 * dependencies that the future plugin system should rely on.
 */
public final class CoreContext {

    private final CoreConfiguration configuration;
    private final DatabaseGateway databaseGateway;

    private CoreContext(CoreConfiguration configuration, DatabaseGateway databaseGateway) {
        this.configuration = configuration;
        this.databaseGateway = databaseGateway;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CoreConfiguration configuration() {
        return configuration;
    }

    public DatabaseGateway database() {
        return databaseGateway;
    }

    public static final class Builder {

        private CoreConfiguration configuration;
        private DatabaseGateway databaseGateway;

        private Builder() {
        }

        public Builder withConfiguration(CoreConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withDatabaseGateway(DatabaseGateway databaseGateway) {
            this.databaseGateway = databaseGateway;
            return this;
        }

        public CoreContext build() {
            CoreConfiguration resolvedConfiguration = Objects.requireNonNullElseGet(
                    configuration, LegacyConfigurationAdapter::new);
            DatabaseGateway resolvedDatabaseGateway = Objects.requireNonNullElseGet(
                    databaseGateway, LegacyDatabaseGateway::new);
            return new CoreContext(resolvedConfiguration, resolvedDatabaseGateway);
        }
    }
}
