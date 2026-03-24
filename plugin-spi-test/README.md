# OH-plugin-spi-test

In-memory stub implementations of all `OH-plugin-spi` interfaces.

Add this artefact with `scope: test` to your plugin's `pom.xml` to test your
plugin without Spring, a database, or a running OH instance.

```xml
<dependency>
    <groupId>org.isf</groupId>
    <artifactId>OH-plugin-spi-test</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

---

## Entry point

`StubPluginContext` is the only object you need to create. It wires together
all other stubs internally:

```java
StubPluginContext ctx = new StubPluginContext();
MyPlugin plugin = new MyPlugin();
plugin.onStart(ctx);
```

---

## Stub reference

### StubPluginContext

The root stub. Implements `PluginContext`.

```java
// default — all permissions GRANTED, no capabilities
StubPluginContext ctx = new StubPluginContext();

// specific permission result for all checks
StubPluginContext ctx = new StubPluginContext(PermissionCheckResult.DENIED_USER);

// full builder — declare capabilities, pluginId, etc.
StubPluginContext ctx = StubPluginContext.builder()
    .pluginId("com.example.myplugin")
    .capabilities(List.of(
            PluginCapability.EVENT_LISTENER,
            PluginCapability.LOG_FILE_WRITE))
    .permissionResult(PermissionCheckResult.GRANTED)
    .build();
```

Extra accessors for test setup:
- `ctx.patientStore()` — pre-load patient data
- `ctx.files()` — inspect log file writes (requires `LOG_FILE_WRITE` capability)

### StubPluginContext.fromManifest

Load the descriptor directly from the plugin's generated `manifest.json`:

```java
StubPluginContext ctx = StubPluginContext.fromManifest("/manifest.json");
```

`manifest.json` is **generated** by `plugin-maven-plugin` during
`mvn package -P package-plugin` — it lands in `target/classes/` and is
therefore on the test classpath when tests run as part of the same build.
It does **not** exist in `src/` and must not be created there by hand.

This is the most realistic option: your tests run against the actual capabilities
and permissions declared in your `PluginDescriptor`, so any inconsistency between
the descriptor and the test setup is caught immediately.

### StubEventBus

Implements `PluginEventBus`. Accessible via `ctx.eventBus()`.

```java
// publish an event and trigger plugin reactions
ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));

// verify the plugin subscribed to the expected event types
assertThat(ctx.eventBus().subscribedTypes())
        .contains(OHDomainEvents.PatientCreated.class);

// verify what was published (in order)
assertThat(ctx.eventBus().publishedEvents()).hasSize(1);

// count subscribers for a specific type
assertThat(ctx.eventBus().subscriberCount(OHDomainEvents.PatientCreated.class))
        .isEqualTo(1);

// clear published history between test steps
ctx.eventBus().clearPublished();
```

### StubPatientStore + StubDataAccessor

`StubDataAccessor` is accessible via `ctx.data()`. Pre-populate the
`StubPatientStore` via `ctx.patientStore()` before running the test:

```java
ctx.patientStore()
   .add(42)
   .firstName("Mario").lastName("Rossi")
   .birthDate("1970-06-15").sex("M").wardCode("MED")
   .done()
   .add(99)
   .firstName("Anna").lastName("Verdi")
   .done();

plugin.onStart(ctx);
ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));

// verify the plugin accessed the expected patient codes
assertThat(ctx.data().accessedPatientCodes()).containsExactly(42);

// reset access log between test steps
ctx.data().clearAccessLog();
```

If a patient code is not in the store, `withPatient()` still calls the consumer
with a view whose fields all return `null` (except `getPatientCode()`). This
mirrors production behaviour where a patient may have been deleted between event
publication and data access.

### StubFileAccess

Implements `PluginFileAccess`. Accessible via `ctx.files()`.

The plugin must declare `PluginCapability.LOG_FILE_WRITE` — calling `ctx.files()`
without it throws `UnsupportedOperationException`, exactly as in production.

Writes are captured in memory — no actual files are created on disk:

```java
ctx = StubPluginContext.builder()
        .capabilities(List.of(
                PluginCapability.EVENT_LISTENER,
                PluginCapability.LOG_FILE_WRITE))
        .build();

plugin.onStart(ctx);
ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));

// verify content written to a specific file
assertThat(ctx.files().writtenTo("audit.log"))
        .contains("CREATED")
        .contains("Mario Rossi");

// check whether a file was written at all
assertThat(ctx.files().wasWrittenTo("audit.log")).isTrue();

// inspect all files written and their content
assertThat(ctx.files().allWritten()).hasSize(1);

// clear all captured content between test steps
ctx.files().clear();
```

### StubHttpClient

Implements `PluginHttpClient`. Accessible via `ctx.httpClient()`.

By default all calls throw `SecurityException` — unexpected HTTP calls fail
loudly, helping you catch network calls the plugin should not be making:

```java
// pre-configure a GET response
ctx.httpClient()
   .when("https://pacs.hospital.org/studies")
   .thenReturn("{\"studies\": []}");

// pre-configure a POST response
ctx.httpClient()
   .whenPost("https://hl7.hospital.org/messages")
   .thenReturn("ACK");

// verify calls made by the plugin (format: "GET url" or "POST url")
assertThat(ctx.httpClient().requestedUrls())
        .containsExactly("GET https://pacs.hospital.org/studies");

// clear request log between test steps
ctx.httpClient().clearLog();
```

---

## Complete example

```java
@DisplayName("PatientAuditPlugin")
class PatientAuditPluginTest {

    private StubPluginContext ctx;
    private PatientAuditPlugin plugin;

    @BeforeEach
    void setUp() throws Exception {
        ctx = StubPluginContext.builder()
                .capabilities(List.of(
                        PluginCapability.EVENT_LISTENER,
                        PluginCapability.LOG_FILE_WRITE))
                .build();

        ctx.patientStore()
           .add(42).firstName("Mario").lastName("Rossi").done();

        plugin = new PatientAuditPlugin();
        plugin.onStart(ctx);
    }

    @Test
    void pluginSubscribesToPatientEvents() {
        assertThat(ctx.eventBus().subscribedTypes())
                .contains(OHDomainEvents.PatientCreated.class);
    }

    @Test
    void patientCreatedWritesLogEntry() {
        ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));

        assertThat(ctx.files().writtenTo("audit.log"))
                .contains("CREATED")
                .contains("Mario Rossi")
                .contains("code: 42");
    }

    @Test
    void onStopDeregistersSubscriptions() throws Exception {
        plugin.onStop(ctx);
        assertThat(ctx.eventBus().subscribedTypes()).isEmpty();
    }
}
```
