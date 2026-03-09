package org.isf.core.framework;

import org.isf.utils.exception.OHException;

/**
 * Abstraction that encapsulates data layer access for core services and future plugins.
 * The goal is to remove direct dependencies on static helpers, enabling a smoother
 * migration towards a modular, microkernel-oriented runtime.
 */
public interface DatabaseGateway {

    /**
     * Executes database logic that may return a value.
     *
     * @param <T> the result type
     * @param callback the logic to execute with the configured persistence context
     * @return the value produced by the callback
     * @throws OHException when the underlying persistence layer fails
     */
    <T> T query(DatabaseWork<T> callback) throws OHException;

    /**
     * Executes database logic that does not return a value.
     *
     * @param callback the logic to execute with the configured persistence context
     * @throws OHException when the underlying persistence layer fails
     */
    void execute(DatabaseVoidWork callback) throws OHException;
}
