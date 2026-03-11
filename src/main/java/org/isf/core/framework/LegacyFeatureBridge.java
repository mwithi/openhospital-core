package org.isf.core.framework;

import java.util.Optional;

import org.isf.utils.exception.OHException;

/**
 * Fallback bridge to legacy implementations for features not yet pluginized.
 */
public interface LegacyFeatureBridge {

    <T> Optional<T> invoke(String featureId, CoreContext context) throws OHException;
}
