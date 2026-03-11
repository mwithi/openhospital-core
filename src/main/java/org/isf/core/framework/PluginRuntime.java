package org.isf.core.framework;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.isf.utils.exception.OHException;

/**
 * Shadow runtime that can load plugin features while preserving legacy fallback.
 */
public final class PluginRuntime {

    private final CoreContext context;
    private final PluginDiscovery pluginDiscovery;
    private final LegacyFeatureBridge legacyFeatureBridge;
    private final PluginRegistry pluginRegistry = new PluginRegistry();
    private final List<CorePlugin> startedPlugins = new ArrayList<>();

    PluginRuntime(CoreContext context, PluginDiscovery pluginDiscovery, LegacyFeatureBridge legacyFeatureBridge) {
        this.context = context;
        this.pluginDiscovery = pluginDiscovery;
        this.legacyFeatureBridge = legacyFeatureBridge;
    }

    public void start() throws OHException {
        List<CorePlugin> discoveredPlugins = new ArrayList<>(pluginDiscovery.discover());
        validateUniquePluginIds(discoveredPlugins);
        discoveredPlugins.sort(Comparator.comparingInt(CorePlugin::order).thenComparing(CorePlugin::pluginId));
        for (CorePlugin plugin : discoveredPlugins) {
            plugin.start(context, pluginRegistry);
            startedPlugins.add(plugin);
        }
    }

    private static void validateUniquePluginIds(List<CorePlugin> discoveredPlugins) throws OHException {
        Set<String> pluginIds = new HashSet<>();
        for (CorePlugin plugin : discoveredPlugins) {
            if (!pluginIds.add(plugin.pluginId())) {
                throw new OHException("Duplicate pluginId detected: " + plugin.pluginId());
            }
        }
    }

    public void stop() throws OHException {
        OHException error = null;
        for (int index = startedPlugins.size() - 1; index >= 0; index--) {
            try {
                startedPlugins.get(index).stop();
            } catch (OHException exception) {
                error = exception;
            }
        }
        startedPlugins.clear();
        if (error != null) {
            throw error;
        }
    }

    public <T> Optional<T> executeFeature(String featureId) throws OHException {
        Optional<PluginRegistry.PluginFeatureHandler<T>> pluginHandler = pluginRegistry.findFeature(featureId);
        if (pluginHandler.isPresent()) {
            return Optional.ofNullable(pluginHandler.get().execute(context));
        }
        return legacyFeatureBridge.invoke(featureId, context);
    }
}
