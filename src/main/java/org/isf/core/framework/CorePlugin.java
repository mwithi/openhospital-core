package org.isf.core.framework;

import org.isf.utils.exception.OHException;

/**
 * Contract for plugins managed by the shadow runtime introduced during step 2.
 */
public interface CorePlugin {

    String pluginId();

    default int order() {
        return 0;
    }

    void start(CoreContext context, PluginRegistry registry) throws OHException;

    default void stop() throws OHException {
        // default no-op
    }
}
