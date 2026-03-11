package org.isf.core.framework;

import java.util.Optional;

/**
 * Represents the immutable runtime configuration exposed by the Open Hospital core.
 * <p>
 * The interface has intentionally small surface area so that legacy configuration sources
 * (for example {@code settings.properties}) can be gradually replaced without impacting
 * the components that depend on configuration values. Plugins and refactored modules
 * will consume this interface instead of directly accessing static utility classes.
 */
public interface CoreConfiguration {

    /**
     * Returns an optional string value for the provided configuration key.
     *
     * @param key the configuration property name
     * @return the optional value
     */
    Optional<String> get(String key);

    /**
     * Returns the configuration value or a fallback when the key is not present.
     *
     * @param key the configuration property name
     * @param defaultValue value returned when the key is missing
     * @return the resolved value or the provided default
     */
    default String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /**
     * Resolves a configuration flag as boolean.
     *
     * @param key the configuration property name
     * @param defaultValue value returned when the key is missing or invalid
     * @return the resolved boolean value
     */
    default boolean getBoolean(String key, boolean defaultValue) {
        return get(key).map(value -> "YES".equalsIgnoreCase(value) || Boolean.parseBoolean(value))
                .orElse(defaultValue);
    }

    /**
     * Resolves a configuration entry as integer.
     *
     * @param key the configuration property name
     * @param defaultValue value returned when the key is missing or invalid
     * @return the resolved integer value
     */
    default int getInt(String key, int defaultValue) {
        return get(key).map(value -> {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }).orElse(defaultValue);
    }
}
