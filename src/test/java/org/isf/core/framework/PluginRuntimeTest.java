package org.isf.core.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.isf.utils.exception.OHException;
import org.junit.jupiter.api.Test;

class PluginRuntimeTest {

    @Test
    void shouldUsePluginFeatureBeforeLegacyFallback() throws OHException {
        StubPlugin plugin = new StubPlugin();
        PluginRuntime runtime = new PluginRuntime(
                CoreContext.builder().build(),
                () -> List.of(plugin),
                new StubLegacyFeatureBridge("legacy-value"));

        runtime.start();

        assertThat(runtime.executeFeature("patients.lookup"))
                .contains("plugin-value");
        assertThat(plugin.started).isTrue();
    }

    @Test
    void shouldFallbackToLegacyFeatureWhenPluginIsMissing() throws OHException {
        PluginRuntime runtime = new PluginRuntime(
                CoreContext.builder().build(),
                List::of,
                new StubLegacyFeatureBridge("legacy-value"));

        runtime.start();

        assertThat(runtime.executeFeature("patients.lookup"))
                .contains("legacy-value");
    }

    @Test
    void shouldFailWhenPluginIdsAreDuplicated() {
        CorePlugin first = new StubPluginWithId("duplicate.id");
        CorePlugin second = new StubPluginWithId("duplicate.id");
        PluginRuntime runtime = new PluginRuntime(
                CoreContext.builder().build(),
                () -> List.of(first, second),
                new NoOpLegacyFeatureBridge());

        assertThatThrownBy(runtime::start)
                .isInstanceOf(OHException.class)
                .hasMessageContaining("Duplicate pluginId detected: duplicate.id");
    }

    @Test
    void shouldStopPluginsInReverseOrder() throws OHException {
        java.util.concurrent.atomic.AtomicInteger stopCounter = new java.util.concurrent.atomic.AtomicInteger();
        TrackingPlugin first = new TrackingPlugin("first", 1, stopCounter);
        TrackingPlugin second = new TrackingPlugin("second", 2, stopCounter);
        PluginRuntime runtime = new PluginRuntime(
                CoreContext.builder().build(),
                () -> List.of(first, second),
                new NoOpLegacyFeatureBridge());

        runtime.start();
        runtime.stop();

        assertThat(first.events).containsExactly("start", "stop");
        assertThat(second.events).containsExactly("start", "stop");
        assertThat(first.stopOrder).isGreaterThan(second.stopOrder);
    }


    private static final class StubLegacyFeatureBridge implements LegacyFeatureBridge {

        private final String value;

        private StubLegacyFeatureBridge(String value) {
            this.value = value;
        }

        @Override
        public <T> Optional<T> invoke(String featureId, CoreContext context) {
            @SuppressWarnings("unchecked")
            T castedValue = (T) value;
            return Optional.of(castedValue);
        }
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


    private static final class StubPluginWithId implements CorePlugin {

        private final String pluginId;

        private StubPluginWithId(String pluginId) {
            this.pluginId = pluginId;
        }

        @Override
        public String pluginId() {
            return pluginId;
        }

        @Override
        public void start(CoreContext context, PluginRegistry registry) {
            // no-op
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
