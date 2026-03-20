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

`StubPluginContext` is all you need. It wires together all other stubs:

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
// default — all permissions GRANTED
StubPluginContext ctx = new StubPluginContext();

// specific permission result
StubPluginContext ctx = new StubPluginContext(PermissionCheckResult.DENIED_USER);

// full builder
StubPluginContext ctx = StubPluginContext.builder()
    .pluginId("com.example.myplugin")
    .permissionResult(PermissionCheckResult.DENIED_PLUGIN)
    .capabilities(List.of(PluginCapability.EVENT_LISTENER))
    .build();
```

Extra accessor: `ctx.patientStore()` — returns the `StubPatientStore` for
pre-loading test data.

### StubEventBus

Implements `PluginEventBus`. Accessible via `ctx.eventBus()`.

```java
// publish an event to trigger plugin reaction
ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));

// verify subscriptions
assertThat(ctx.eventBus().subscribedTypes())
        .contains(OHDomainEvents.PatientCreated.class);

// verify what was published
assertThat(ctx.eventBus().publishedEvents()).hasSize(1);

// count subscribers for a type
assertThat(ctx.eventBus().subscriberCount(OHDomainEvents.PatientCreated.class))
        .isEqualTo(1);

// reset published log between test steps
ctx.eventBus().clearPublished();
```

### StubPatientStore + StubDataAccessor

`StubDataAccessor` is accessible via `ctx.data()`. It reads from the
`StubPatientStore` accessible via `ctx.patientStore()`.

```java
// pre-load a patient
ctx.patientStore()
   .add(42)
   .firstName("Mario").lastName("Rossi")
   .birthDate("1970-06-15").sex("M")
   .done();

// trigger the plugin
ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));

// verify the plugin accessed the patient
assertThat(ctx.data().accessedPatientCodes()).containsExactly(42);

// reset access log between test steps
ctx.data().clearAccessLog();
```

If a patient code is not in the store, `withPatient()` still calls the consumer
with a view whose fields all return `null` (except `getPatientCode()`).

### StubHttpClient

Implements `PluginHttpClient`. Accessible via `ctx.httpClient()`.

By default all calls throw `SecurityException` — unexpected HTTP calls fail
loudly in tests.

```java
// configure a response
ctx.httpClient()
   .when("https://pacs.hospital.org/studies")
   .thenReturn("{\"studies\": []}");

ctx.httpClient()
   .whenPost("https://hl7.hospital.org/messages")
   .thenReturn("ACK");

// verify what was called
assertThat(ctx.httpClient().requestedUrls())
        .containsExactly("GET https://pacs.hospital.org/studies");

// clear log between steps
ctx.httpClient().clearLog();
```
