# plugin-maven-plugin

Maven plugin for Open Hospital plugin development.

Provides two goals that together produce the plugin distribution ZIP:

| Goal | Phase | Description |
|------|-------|-------------|
| `generate-manifest` | `prepare-package` | Generates `manifest.json` from `getDescriptor()` via reflection |
| `package-zip` | `package` | Assembles the distribution ZIP with JAR and manifest |

---

## How it works

### `generate-manifest`

1. Builds a `URLClassLoader` from the project's compile classpath
2. Uses `ServiceLoader` to find the `OHPlugin` implementation registered in `META-INF/services/org.isf.plugin.spi.OHPlugin`
3. Instantiates it with its no-argument constructor
4. Calls `getDescriptor()` to obtain the `PluginDescriptor`
5. Serialises it to pretty-printed JSON using Jackson
6. Writes `manifest.json` to `target/classes/` — included in the JAR automatically

### `package-zip`

1. Locates `target/{artifactId}-{version}.jar` (produced by `maven-jar-plugin`)
2. Locates `target/classes/manifest.json` (produced by `generate-manifest`)
3. Assembles `target/{artifactId}-{version}.zip` containing both files

The resulting ZIP is the file to upload to `POST /api/plugins/install`.

---

## Requirements

Your plugin class must:

- implement `OHPlugin`
- be registered in `META-INF/services/org.isf.plugin.spi.OHPlugin`
- have a **public no-argument constructor**
- return a fully populated `PluginDescriptor` from `getDescriptor()` without requiring any injected state

---

## Setup

### 1. Install locally (once after cloning)

```bash
cd openhospital-core/plugin-spi       && mvn clean install
cd ../plugin-spi-test                  && mvn clean install
cd ../plugin-maven-plugin              && mvn clean install
```

### 2. Add to your plugin's `pom.xml`

Declare both goals inside a profile to prevent Eclipse from trying to
download the plugin from Maven Central before it is installed locally:

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
              <id>generate-manifest</id>
              <goals><goal>generate-manifest</goal></goals>
            </execution>
            <execution>
              <id>package-zip</id>
              <goals><goal>package-zip</goal></goals>
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
# Build JAR + manifest.json + distribution ZIP
mvn clean package -P package-plugin

# Force re-resolution if a previous attempt was cached as failed
mvn clean package -P package-plugin -U

# Skip manifest generation only (still produces JAR and ZIP without manifest)
mvn clean package -P package-plugin -Doh.plugin.skipManifest=true

# Skip ZIP assembly only
mvn clean package -P package-plugin -Doh.plugin.skipZip=true

# Skip both
mvn clean package -P package-plugin -Doh.plugin.skipManifest=true -Doh.plugin.skipZip=true
```

### Output

```
target/
├── oh-patient-audit-plugin-1.0.0.jar    ← plugin JAR
├── oh-patient-audit-plugin-1.0.0.zip    ← upload this to POST /api/plugins/install
└── classes/
    └── manifest.json                     ← included in the JAR
```

### Verify the ZIP contents

```bash
unzip -l target/oh-patient-audit-plugin-1.0.0.zip
# Archive:  target/oh-patient-audit-plugin-1.0.0.zip
#   Length      Date    Time    Name
# ---------  ---------- -----   ----
#   12345    04-15-2026 10:23   oh-patient-audit-plugin-1.0.0.jar
#     512    04-15-2026 10:23   manifest.json
```

---

## Configuration reference

### `generate-manifest`

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `manifestFileName` | — | `manifest.json` | Output filename — do not change |
| `skip` | `oh.plugin.skipManifest` | `false` | Skip manifest generation |

### `package-zip`

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `manifestFileName` | — | `manifest.json` | Manifest filename to include in ZIP |
| `skip` | `oh.plugin.skipZip` | `false` | Skip ZIP assembly |

---

## Why a profile instead of a regular plugin declaration?

Eclipse resolves all Maven plugins declared in `<build>` at import time —
even those with `<skip>true</skip>`. If `plugin-maven-plugin` has not yet
been installed locally, Eclipse reports a resolution error for every project
that declares it.

A profile without `<activation>` is **not loaded by Eclipse** unless
explicitly enabled from the command line. This keeps the workspace clean
until you run `mvn install` on `plugin-maven-plugin`.

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
