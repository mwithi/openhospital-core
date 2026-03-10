package org.isf.core.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers plugins through Java's ServiceLoader mechanism.
 */
public final class ServiceLoaderPluginDiscovery implements PluginDiscovery {

    @Override
    public List<CorePlugin> discover() {
        List<CorePlugin> plugins = new ArrayList<>();
        ServiceLoader.load(CorePlugin.class).forEach(plugins::add);
        return plugins;
    }
}
