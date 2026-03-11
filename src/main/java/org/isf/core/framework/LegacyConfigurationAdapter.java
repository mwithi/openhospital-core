package org.isf.core.framework;

import java.util.Optional;
import java.util.Properties;

import org.isf.generaldata.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy adapter that exposes the {@code settings.properties} content through the
 * {@link CoreConfiguration} contract. It keeps the existing property resolution logic
 * untouched while removing the need for callers to reference {@code GeneralData}
 * directly.
 */
final class LegacyConfigurationAdapter implements CoreConfiguration {

    static final String LEGACY_SETTINGS_FILE = "settings.properties";

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyConfigurationAdapter.class);

    private final Properties properties;

    LegacyConfigurationAdapter() {
        this(ConfigurationProperties.loadPropertiesFile(LEGACY_SETTINGS_FILE, LOGGER));
    }

    LegacyConfigurationAdapter(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }
}
