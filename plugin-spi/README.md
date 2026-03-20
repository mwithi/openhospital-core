# OH-plugin-spi

Service Provider Interface for Open Hospital plugins.

This module defines the public contract between the Open Hospital runtime and
any third-party plugin. It is a single JAR with **zero framework dependencies**
— only Java 17 and the SLF4J API — that defines everything a plugin can do,
declare, and request.

---

## Contents

- [Architecture overview](#architecture-overview)
- [Quick start](#quick-start)
- [Package reference](#package-reference)
- [Permission system](#permission-system)
- [Privacy by design](#privacy-by-design)
- [UI contributions](#ui-contributions)
- [Security guarantees](#security-guarantees)
- [Testing your plugin](#testing-your-plugin)
- [Integration into Open Hospital](#integration-into-open-hospital)
- [Building](#building)
- [Version history](#version-history)

---

## Architecture overview

![Module map](docs/module-map.svg)

```
openhospital-core          publishes domain events
openhospital-api           implements PluginRegistry and PluginContext
openhospital-gui           includes the SPI JAR (no Spring)
third-party plugin         implements OHPlugin, declared via ServiceLoader
```

The SPI is the only dependency between a third-party plugin and OH. A plugin
compiled against `spi:1.0.0` works on any OH release that supports that SPI
version, regardless of internal Spring or Hibernate upgrades.

---

## Quick start

### 1. Add the dependency

```xml
<dependency>
    <groupId>org.isf</groupId>
    <artifactId>OH-plugin-spi</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>  <!-- OH provides it at runtime -->
</dependency>
```

### 2. Implement OHPlugin

```java
public class MyPlugin implements OHPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
        .pluginId("com.example.myplugin")        // reverse-domain format
        .version("1.0.0")                         // semver
        .name("My Plugin")
        .entryPoint("com.example.myplugin.MyPlugin")
        .minCoreVersion("1.15.0")
        .capabilities(List.of(PluginCapability.EVENT_LISTENER))
        .fieldPermissions(List.of(
            FieldPermission.read(PatientField.FIRST_NAME, PatientField.LAST_NAME)
                           .purpose("Log new patient registrations")))
        .build();

    @Override
    public PluginDescriptor getDescriptor() { return DESCRIPTOR; }

    @Override
    public void onStart(PluginContext ctx) {
        ctx.eventBus().subscribe(
            OHDomainEvents.PatientCreated.class,
            event -> ctx.data().withPatient(
                event.getPatientCode(),
                view -> ctx.logger().info("New patient: {} {}",
                    view.getFirstName(), view.getLastName())));
    }
}
```

### 3. Register via ServiceLoader

Create `src/main/resources/META-INF/services/org.isf.plugin.spi.OHPlugin`:

```
com.example.myplugin.MyPlugin
```

### 4. Package as ZIP

```
myplugin-1.0.0.zip
├── myplugin-1.0.0.jar
├── manifest.json
└── db/migration/          (optional — if DB_MIGRATION capability declared)
    └── V1__initial.sql
```

---

## Package reference

| Package | Key types | Responsibility |
|---------|-----------|----------------|
| `model` | `PluginDescriptor`, `FieldPermission`, `PermissionCheckResult`, `ExternalConnection` | Value objects describing a plugin's identity, capabilities, and data access |
| `model.field` | `DomainField`, `FieldSensitivity`, `PatientField`, `AdmissionField`, `LaboratoryField`, `WardField`, `PharmacyField` | Domain field catalogues with privacy sensitivity metadata |
| `model.ui` | `UiContribution`, `RouteDescriptor`, `SlotContribution`, `BundleDescriptor` | Descriptors for React UI contributions |
| `spi` | `OHPlugin`, `MigrationScript`, `OHPluginLifecycleException` | Primary lifecycle interfaces |
| `registry` | `PluginContext`, `PluginDataAccessor`, `PatientView`, `PluginHttpClient` | Runtime gateway — the only contact point between a plugin and OH |
| `event` | `OHDomainEvents`, `OHPluginEvent`, `PluginEventBus` | ID-only domain events |
| `hook` | `OHManagerExtension` | Chain-of-responsibility extension point for OH-core Managers |
| `security` | `PluginSecurityPolicy`, `ValidationResult` | Install-time security policy SPI |

---

## Permission system

Authorization uses two complementary mechanisms.

### Field-level permissions

Instead of declaring `READ_PATIENT`, a plugin declares exactly which fields it
needs and why:

```java
FieldPermission.read(
    PatientField.FIRST_NAME,
    PatientField.LAST_NAME,
    PatientField.BIRTH_DATE)
  .purpose("Display patient header on radiology report")
```

Each `DomainField` carries `fieldName()`, `sensitivity()`, and `writeable()`.
The type system prevents mixing fields from different domains in the same
`FieldPermission` — a compile error, not a runtime one.

**Sensitivity levels:**

| Level | Meaning | GDPR |
|-------|---------|------|
| `ID` | Opaque system identifier | Not personal data |
| `INTERNAL` | Organisational code | Not personal data |
| `PERSONAL` | Identifies a person in society | Art. 4 |
| `CLINICAL` | Describes a person as a patient | Art. 9 |
| `SENSITIVE` | Universal identifier or socially stigmatised data (e.g. `TAX_CODE`, `hivStatus`) | Art. 9 + explicit admin approval |

### Three-state permission check

```java
return switch (ctx.check(PluginPermission.READ_PATIENT)) {
    case GRANTED       -> ResponseEntity.ok(loadData(patientCode));
    case DENIED_PLUGIN -> ResponseEntity.status(403)
                             .body("Plugin not configured for patient access");
    case DENIED_USER   -> ResponseEntity.status(403)
                             .body("Your role does not allow reading patient data");
};
```

Or as a guard clause:

```java
if (ctx.check(READ_PATIENT).isDenied()) {
    return ResponseEntity.status(403).build();
}
```

`DENIED_PLUGIN` means the plugin lacks the permission (not declared or not
approved at install time). `DENIED_USER` means the plugin has the permission
but the current user lacks the required OH role.

---

## Privacy by design

### ID-only events

All `OHDomainEvents` carry only opaque identifiers — never PII or clinical
data. A `PatientCreated` event carries only `patientCode`. To access patient
data, the plugin must explicitly call `ctx.data().withPatient(...)`, which
requires a `READ` `FieldPermission` in the manifest and writes an audit entry.

### Scope-bound data access

The `PatientView` passed to `withPatient()` is valid only inside that call.
This prevents the plugin from caching patient data outside the request scope.

### Network allowlist

Every outbound connection must be declared in the manifest as an
`ExternalConnection`. The `PluginClassLoader` (in `api`) blocks any connection
to an undeclared host at runtime.

```java
new ExternalConnection("pacs.hospital.org", 11112, "DICOM",
    "Send DICOM study to hospital PACS", Direction.OUTBOUND)
```

A plugin can declare multiple connections — one per distinct host/port.

---

## UI contributions

Plugins that contribute to the React UI declare a `UiContribution` block:

```java
UiContribution.builder()
    .bundle("ui/radiology.js", "radiologyPlugin")   // Module Federation remote
    .route(RouteDescriptor.open(
        "/radiology", "Radiology", "Modules.Radiology"))
    .slot(new SlotContribution(
        "patient.header.actions", SlotMode.APPEND))
    .build()
```

**`RouteDescriptor`** declares a new page. The optional `permission` field
uses `PluginPermission` — not OH role names. OH-ui currently performs
authentication-only routing; permission-aware routing is planned for Phase 4.

**`SlotContribution`** injects a React component into a named `<PluginSlot>`.
Mode `REPLACE` is exclusive — only one plugin may replace a given slot.

**`BundleDescriptor`** locates the JS bundle in the plugin ZIP and specifies
the Module Federation `remoteName` (must be a valid JS identifier).

`PluginDescriptor.build()` validates consistency: `UI_ROUTES` capability
requires a `UiContribution` with at least one route; `UI_COMPONENT_OVERRIDE`
requires at least one slot; a bundle without contributions is rejected.

---

## Security guarantees

### Maven enforcer

The build fails if Spring, Hibernate, or JPA enter the compile classpath.
This is executable documentation — if someone adds a forbidden dependency
by mistake, the build breaks immediately with a clear message.

### JPMS encapsulation

`module-info.java` exports all public packages but opens none.
`setAccessible(true)` on any class in this module throws
`InaccessibleObjectException` at runtime, even for code running in the
same JVM. A plugin cannot bypass `PluginContext` to reach the Spring
`ApplicationContext` via reflection.

### Install-time security policies

The `PluginSecurityPolicy` SPI allows `openhospital-api` to register policies
applied in sequence before any plugin is activated. Planned implementations:
GPG signature, SHA-256 checksum, capability allowlist, semver compatibility,
dependency resolution.

---

## Testing your plugin

Add the test utilities artefact:

```xml
<dependency>
    <groupId>org.isf</groupId>
    <artifactId>OH-plugin-spi-test</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

Then use `StubPluginContext` — no Spring, no database:

```java
@Test
void pluginSubscribesToPatientEvents() throws Exception {
    StubPluginContext ctx = new StubPluginContext();
    MyPlugin plugin = new MyPlugin();

    plugin.onStart(ctx);

    assertThat(ctx.eventBus().subscribedTypes())
            .contains(OHDomainEvents.PatientCreated.class);
}

@Test
void pluginLogsNewPatientName() throws Exception {
    StubPluginContext ctx = new StubPluginContext();
    ctx.patientStore()
       .add(42).firstName("Mario").lastName("Rossi").done();

    MyPlugin plugin = new MyPlugin();
    plugin.onStart(ctx);
    ctx.eventBus().publish(new OHDomainEvents.PatientCreated(42));

    assertThat(ctx.data().accessedPatientCodes()).containsExactly(42);
}

@Test
void pluginHandlesDeniedPermission() throws Exception {
    StubPluginContext ctx = new StubPluginContext(PermissionCheckResult.DENIED_USER);
    MyPlugin plugin = new MyPlugin();
    plugin.onStart(ctx);

    // verify the plugin behaves correctly when permission is denied
    assertThat(plugin.getLastError()).contains("role");
}
```

See the `OH-plugin-spi-test` README for the full stub API reference.

---

## Integration into Open Hospital

This module is designed to live as a submodule of `openhospital-core`:

```
openhospital-core/
├── pom.xml                    (add <module>plugin-spi</module>)
├── plugin-spi/
│   ├── pom.xml
│   └── src/
└── src/
```

**Two-step build** on first setup:

```bash
cd plugin-spi && mvn clean install
cd ..         && mvn clean install
```

**Dependencies in sibling modules:**

```xml
<!-- in openhospital-core, openhospital-api, openhospital-gui pom.xml -->
<dependency>
    <groupId>org.isf</groupId>
    <artifactId>OH-plugin-spi</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Eclipse:** File → Import → Maven → Existing Maven Projects → select the
`plugin-spi/` folder.

---

## Building

```bash
# compile and run all 96 tests
mvn clean verify

# install to local Maven repository (required before building core)
mvn clean install
```

Requirements: Java 17+, Maven 3.8+.

---

## Version history

| Version | Changes |
|---------|---------|
| v1 | Core SPI — `OHPlugin`, `PluginContext`, events, hooks, security |
| v2 | Bug fixes — FQCN pattern, Eclipse project files |
| v3 | Privacy by design — `ExternalConnection`, ID-only events, `PluginDataAccessor` |
| v4 | Field-level permissions — `FieldPermission`, `PatientField`, `AdmissionField`, `LaboratoryField`, `WardField`, `PharmacyField` |
| v5 | JPMS `module-info.java`, Surefire `--add-opens`, clean import pass |
| v6 | Three-state `PermissionCheckResult`, single `check()` method replaces three |
| v7 | UI contributions — `UiContribution`, `RouteDescriptor`, `SlotContribution`, `BundleDescriptor` |
