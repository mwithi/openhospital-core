# plugin-maven-plugin

Maven plugin for Open Hospital plugin development.

Provides the `generate-manifest` goal, which generates `manifest.json`
automatically from your plugin's `PluginDescriptor` — so you never write
or maintain the manifest by hand.

---

## How it works

During `mvn package`, the Mojo:

1. Builds a `URLClassLoader` from the project's compile classpath
2. Uses `ServiceLoader` to find your `OHPlugin` implementation (registered
   in `META-INF/services/org.isf.plugin.spi.OHPlugin`)
3. Instantiates the class with its no-argument constructor
4. Calls `getDescriptor()` to obtain the `PluginDescriptor`
5. Serialises it to pretty-printed JSON using Jackson
6. Writes `manifest.json` to `target/classes/` — it is included in the JAR
   automatically, with no extra configuration

---

## Requirements

Your plugin class must:

- implement `OHPlugin`
- be registered in `META-INF/services/org.isf.plugin.spi.OHPlugin`
- have a **public no-argument constructor**
- return a fully populated `PluginDescriptor` from `getDescriptor()` **without
  requiring any injected state** — the Mojo calls it before Spring or any other
  framework is active

---

## Setup

### 1. Install locally (once after cloning)

```bash
cd openhospital-core/plugin-spi       && mvn clean install
cd ../plugin-spi-test                  && mvn clean install
cd ../plugin-maven-plugin              && mvn clean install
```

### 2. Add to your plugin's pom.xml

Declare the goal inside a profile to prevent Eclipse from trying to download
the plugin from Maven Central before it is installed locally:

```xml
<profiles>
  <profile>
    <id>package-plugin</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.isf</groupId>
          <artifactId>plugin-maven-plugin</artifactId>
          <version>1.0.0</version>
          <executions>
            <execution>
              <goals><goal>generate-manifest</goal></goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

---

## Usage

```bash
# Build JAR with generated manifest.json
mvn clean package -P package-plugin

# Force re-resolution if a previous attempt was cached as failed
mvn clean package -P package-plugin -U

# Skip manifest generation (e.g. in CI pipelines that only run tests)
mvn clean verify -Dplugin.skipManifest=true
```

### Verify the manifest is in the JAR

```bash
jar tf target/myplugin-1.0.0.jar | grep manifest
# manifest.json
```

### Inspect the generated manifest

```bash
jar xf target/myplugin-1.0.0.jar manifest.json && cat manifest.json
```

---

## Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| `manifestFileName` | `manifest.json` | Output filename — do not change unless you have a specific reason |
| `plugin.skipManifest` | `false` | Set to `true` to skip generation entirely |

---

## Why a profile instead of a regular plugin declaration?

Eclipse resolves all Maven plugins declared in `<build>` at import time —
even those with `<skip>true</skip>`. If `plugin-maven-plugin` has not yet been
installed locally, Eclipse reports a resolution error for every project that
declares it.

A profile declared without `<activation>` is **not loaded by Eclipse** unless
explicitly enabled. This means the plugin declaration is invisible to Eclipse
until you activate the profile from the command line. Once the plugin is
installed locally, `mvn package -P package-plugin` works correctly.

---

## Generated manifest format

```json
{
  "pluginId" : "org.isf.plugin.example.patientaudit",
  "version" : "1.0.0",
  "name" : "Patient Audit Log",
  "description" : "Writes a log entry when a patient is registered or admitted.",
  "vendor" : "Informatici Senza Frontiere",
  "license" : null,
  "entryPoint" : "org.isf.plugin.audit.PatientAuditPlugin",
  "minCoreVersion" : "1.15.0",
  "maxCoreVersion" : null,
  "capabilities" : [ "EVENT_LISTENER", "LOG_FILE_WRITE" ],
  "permissions" : [ "READ_PATIENT" ],
  "dependencies" : [ ],
  "externalConnections" : [ ],
  "fieldPermissions" : [ {
    "domain" : "Patient",
    "access" : "READ",
    "fields" : [ "firstName", "lastName" ],
    "purpose" : "Identify patient in audit log entry"
  } ]
}
```
