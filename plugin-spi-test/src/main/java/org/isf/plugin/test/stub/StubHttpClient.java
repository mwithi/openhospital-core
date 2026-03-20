/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2024 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 */
package org.isf.plugin.test.stub;

import org.isf.plugin.registry.PluginContext.PluginHttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of {@link PluginHttpClient} for unit tests.
 *
 * <p>By default all calls throw {@link SecurityException} — this ensures
 * tests that do not expect network calls fail loudly if the plugin makes one.
 *
 * <p>Use {@link #when(String)} to pre-configure responses:
 *
 * <pre>{@code
 * ctx.httpClient()
 *    .when("https://pacs.hospital.org/studies")
 *    .thenReturn("{\"studies\": []}");
 *
 * plugin.onStart(ctx);
 * ctx.eventBus().publish(new OHDomainEvents.PatientAdmitted(42));
 *
 * assertThat(ctx.httpClient().requestedUrls())
 *         .containsExactly("https://pacs.hospital.org/studies");
 * }</pre>
 */
public final class StubHttpClient implements PluginHttpClient {

    private final Map<String, String> getResponses  = new HashMap<>();
    private final Map<String, String> postResponses = new HashMap<>();
    private final List<String>        requestLog    = new ArrayList<>();

    // -------------------------------------------------------------------------
    // PluginHttpClient contract
    // -------------------------------------------------------------------------

    @Override
    public String get(String url) {
        requestLog.add("GET " + url);
        if (!getResponses.containsKey(url)) {
            throw new SecurityException(
                "StubHttpClient: no response configured for GET " + url +
                ". Use ctx.httpClient().when(url).thenReturn(body) to configure it.");
        }
        return getResponses.get(url);
    }

    @Override
    public String post(String url, String body) {
        requestLog.add("POST " + url);
        if (!postResponses.containsKey(url)) {
            throw new SecurityException(
                "StubHttpClient: no response configured for POST " + url +
                ". Use ctx.httpClient().whenPost(url).thenReturn(body) to configure it.");
        }
        return postResponses.get(url);
    }

    // -------------------------------------------------------------------------
    // Fluent configuration API
    // -------------------------------------------------------------------------

    /** Starts configuring a GET response for the given URL. */
    public ResponseBuilder when(String url) {
        return new ResponseBuilder(url, false);
    }

    /** Starts configuring a POST response for the given URL. */
    public ResponseBuilder whenPost(String url) {
        return new ResponseBuilder(url, true);
    }

    public final class ResponseBuilder {
        private final String  url;
        private final boolean isPost;

        ResponseBuilder(String url, boolean isPost) {
            this.url    = url;
            this.isPost = isPost;
        }

        public StubHttpClient thenReturn(String body) {
            if (isPost) postResponses.put(url, body);
            else        getResponses.put(url, body);
            return StubHttpClient.this;
        }
    }

    // -------------------------------------------------------------------------
    // Inspection API
    // -------------------------------------------------------------------------

    /**
     * Returns all HTTP requests made, in order, as strings like
     * {@code "GET https://..."} or {@code "POST https://"}.
     */
    public List<String> requestedUrls() {
        return Collections.unmodifiableList(requestLog);
    }

    /** Clears the request log without affecting configured responses. */
    public void clearLog() {
        requestLog.clear();
    }
}
