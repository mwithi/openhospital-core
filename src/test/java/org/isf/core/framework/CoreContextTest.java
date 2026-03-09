package org.isf.core.framework;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.isf.utils.exception.OHException;
import org.junit.jupiter.api.Test;

class CoreContextTest {

    @Test
    void shouldUseLegacyDefaultsWhenNoOverridesProvided() {
        CoreContext context = CoreContext.builder().build();

        assertThat(context.configuration()).isInstanceOf(LegacyConfigurationAdapter.class);
        assertThat(context.database()).isInstanceOf(LegacyDatabaseGateway.class);
    }

    @Test
    void shouldApplyCustomComponentsWhenProvided() throws OHException {
        StubConfiguration configuration = new StubConfiguration();
        StubDatabaseGateway gateway = new StubDatabaseGateway();

        CoreContext context = CoreContext.builder()
                .withConfiguration(configuration)
                .withDatabaseGateway(gateway)
                .build();

        assertThat(context.configuration()).isSameAs(configuration);
        assertThat(context.database()).isSameAs(gateway);
    }

    private static final class StubConfiguration implements CoreConfiguration {

        @Override
        public Optional<String> get(String key) {
            return Optional.empty();
        }
    }

    private static final class StubDatabaseGateway implements DatabaseGateway {

        @Override
        public <T> T query(DatabaseWork<T> callback) {
            return null;
        }

        @Override
        public void execute(DatabaseVoidWork callback) {
            // no-op
        }
    }
}
