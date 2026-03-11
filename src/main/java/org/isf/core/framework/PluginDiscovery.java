package org.isf.core.framework;

import java.util.List;

/**
 * Discovers available plugins.
 */
@FunctionalInterface
public interface PluginDiscovery {

    List<CorePlugin> discover();
}
