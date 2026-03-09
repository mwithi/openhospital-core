package org.isf.core.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.isf.utils.exception.OHException;

/**
 * Registry used by plugins to expose feature handlers to the runtime.
 */
public final class PluginRegistry {

    private final Map<String, PluginFeatureHandler<?>> featureHandlers = new HashMap<>();

    public <T> void registerFeature(String featureId, PluginFeatureHandler<T> featureHandler) {
        featureHandlers.put(featureId, featureHandler);
    }

    <T> Optional<PluginFeatureHandler<T>> findFeature(String featureId) {
        @SuppressWarnings("unchecked")
        PluginFeatureHandler<T> featureHandler = (PluginFeatureHandler<T>) featureHandlers.get(featureId);
        return Optional.ofNullable(featureHandler);
    }

    public interface PluginFeatureHandler<T> {

        T execute(CoreContext context) throws OHException;
    }
}
