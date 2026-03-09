package org.isf.core.framework;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.isf.utils.exception.OHException;
import org.junit.jupiter.api.Test;

class CoreRuntimeTest {

    @Test
    void shouldUsePluginFeatureBeforeLegacyFallback() throws OHException {
        StubPlugin plugin = new StubPlugin();
        CoreRuntime runtime = CoreRuntime.builder()
                .withPluginDiscovery(() -> List.of(plugin))
                .withLegacyFeatureBridge((featureId, context) -> Optional.of("legacy-value"))
                .build();

        runtime.plugins().start();

        assertThat(runtime.plugins().executeFeature("patients.lookup"))
                .contains("plugin-value");
        assertThat(plugin.started).isTrue();
    }

    @Test
    void shouldFallbackToLegacyFeatureWhenPluginIsMissing() throws OHException {
        CoreRuntime runtime = CoreRuntime.builder()
                .withPluginDiscovery(List::of)
                .withLegacyFeatureBridge((featureId, context) -> Optional.of("legacy-value"))
                .build();

        runtime.plugins().start();

        assertThat(runtime.plugins().executeFeature("patients.lookup"))
                .contains("legacy-value");
    }

    @Test
    void shouldStopPluginsInReverseOrder() throws OHException {
        java.util.concurrent.atomic.AtomicInteger stopCounter = new java.util.concurrent.atomic.AtomicInteger();
        TrackingPlugin first = new TrackingPlugin("first", 1, stopCounter);
        TrackingPlugin second = new TrackingPlugin("second", 2, stopCounter);
        CoreRuntime runtime = CoreRuntime.builder()
                .withPluginDiscovery(() -> List.of(first, second))
                .build();

        runtime.plugins().start();
        runtime.plugins().stop();

        assertThat(first.events).containsExactly("start", "stop");
        assertThat(second.events).containsExactly("start", "stop");
        assertThat(first.stopOrder).isGreaterThan(second.stopOrder);
    }

    private static final class StubPlugin implements CorePlugin {

        private boolean started;

        @Override
        public String pluginId() {
            return "patients";
        }

        @Override
        public void start(CoreContext context, PluginRegistry registry) {
            started = true;
            registry.registerFeature("patients.lookup", ctx -> "plugin-value");
        }
    }

    private static final class TrackingPlugin implements CorePlugin {

        private final String id;
        private final int pluginOrder;
        private final java.util.ArrayList<String> events = new java.util.ArrayList<>();
        private final java.util.concurrent.atomic.AtomicInteger stopCounter;
        private int stopOrder;

        private TrackingPlugin(String id, int pluginOrder, java.util.concurrent.atomic.AtomicInteger stopCounter) {
            this.id = id;
            this.pluginOrder = pluginOrder;
            this.stopCounter = stopCounter;
        }

        @Override
        public String pluginId() {
            return id;
        }

        @Override
        public int order() {
            return pluginOrder;
        }

        @Override
        public void start(CoreContext context, PluginRegistry registry) {
            events.add("start");
        }

        @Override
        public void stop() {
            events.add("stop");
            stopOrder = stopCounter.incrementAndGet();
        }
    }
}
