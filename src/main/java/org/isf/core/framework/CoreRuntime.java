package org.isf.core.framework;

import java.util.Objects;

/**
 * Entry point used to bootstrap the new core infrastructure. The runtime exposes a
 * {@link CoreContext} configured either with the provided components or with
 * backward-compatible defaults.
 */
public final class CoreRuntime {

    private final CoreContext context;
    private final PluginRuntime pluginRuntime;

    private CoreRuntime(CoreContext context, PluginRuntime pluginRuntime) {
        this.context = context;
        this.pluginRuntime = pluginRuntime;
    }

    /**
     * Bootstraps the runtime using the provided builder customisations.
     *
     * @param customizer customizes the builder before the runtime is created
     * @return the configured runtime instance
     */
    public static CoreRuntime bootstrap(CoreRuntimeCustomizer customizer) {
        Builder builder = builder();
        customizer.customize(builder);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Bootstraps the runtime using only legacy defaults.
     *
     * @return the configured runtime instance
     */
    public static CoreRuntime legacyDefaults() {
        return builder().build();
    }

    public CoreContext context() {
        return context;
    }

    public PluginRuntime plugins() {
        return pluginRuntime;
    }

    public static final class Builder {

        private final CoreContext.Builder contextBuilder = CoreContext.builder();
        private PluginDiscovery pluginDiscovery;
        private LegacyFeatureBridge legacyFeatureBridge;

        private Builder() {
        }

        public Builder withConfiguration(CoreConfiguration configuration) {
            contextBuilder.withConfiguration(configuration);
            return this;
        }

        public Builder withDatabaseGateway(DatabaseGateway databaseGateway) {
            contextBuilder.withDatabaseGateway(databaseGateway);
            return this;
        }

        public Builder withPluginDiscovery(PluginDiscovery pluginDiscovery) {
            this.pluginDiscovery = pluginDiscovery;
            return this;
        }

        public Builder withLegacyFeatureBridge(LegacyFeatureBridge legacyFeatureBridge) {
            this.legacyFeatureBridge = legacyFeatureBridge;
            return this;
        }

        public CoreRuntime build() {
            CoreContext context = contextBuilder.build();
            PluginDiscovery resolvedDiscovery = Objects.requireNonNullElseGet(pluginDiscovery,
                    ServiceLoaderPluginDiscovery::new);
            LegacyFeatureBridge resolvedBridge = Objects.requireNonNullElseGet(legacyFeatureBridge,
                    NoOpLegacyFeatureBridge::new);
            PluginRuntime pluginRuntime = new PluginRuntime(context, resolvedDiscovery, resolvedBridge);
            return new CoreRuntime(context, pluginRuntime);
        }
    }

    @FunctionalInterface
    public interface CoreRuntimeCustomizer {

        void customize(Builder builder);
    }
}
