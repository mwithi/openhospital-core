package org.isf.core.framework;

/**
 * Entry point used to bootstrap the new core infrastructure. The runtime exposes a
 * {@link CoreContext} configured either with the provided components or with
 * backward-compatible defaults.
 */
public final class CoreRuntime {

    private final CoreContext context;

    private CoreRuntime(CoreContext context) {
        this.context = context;
    }

    /**
     * Bootstraps the runtime using the provided builder customisations.
     *
     * @param customizer customizes the builder before the context is created
     * @return the configured runtime instance
     */
    public static CoreRuntime bootstrap(CoreRuntimeCustomizer customizer) {
        CoreContext.Builder builder = CoreContext.builder();
        customizer.customize(builder);
        return new CoreRuntime(builder.build());
    }

    /**
     * Bootstraps the runtime using only legacy defaults.
     *
     * @return the configured runtime instance
     */
    public static CoreRuntime legacyDefaults() {
        return new CoreRuntime(CoreContext.builder().build());
    }

    public CoreContext context() {
        return context;
    }

    @FunctionalInterface
    public interface CoreRuntimeCustomizer {

        void customize(CoreContext.Builder builder);
    }
}
