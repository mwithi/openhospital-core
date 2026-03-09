package org.isf.core.framework;

import java.util.Optional;

import org.isf.utils.exception.OHException;

/**
 * Default bridge used until a specific legacy adapter is provided.
 */
public final class NoOpLegacyFeatureBridge implements LegacyFeatureBridge {

    @Override
    public <T> Optional<T> invoke(String featureId, CoreContext context) throws OHException {
        return Optional.empty();
    }
}
