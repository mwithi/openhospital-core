# Core Runtime Preparation

## Goal
Establish a first-class API for the runtime services that every module or future plugin will rely on. The new API isolates configuration and data access responsibilities so that incremental refactors can detach from static helpers and legacy singletons.

## Key concepts
- **CoreConfiguration** &mdash; unified read-only view of application properties; legacy code continues to populate values via `settings.properties`.
- **DatabaseGateway** &mdash; façade encapsulating `EntityManager` management and providing a safe execution model for database work units.
- **CoreContext/CoreRuntime** &mdash; immutable container that wires the default adapters while allowing overrides when modules are migrated.
- **PluginRuntime** &mdash; shadow runtime that discovers plugins, starts/stops them, and resolves feature calls with a legacy fallback bridge.

## Why now?
This work covers step 2 of the microkernel migration: introducing a plugin runtime in shadow mode. Legacy deployment remains unchanged because:
- plugin discovery defaults to `ServiceLoader` and starts no plugins unless they are explicitly provided,
- feature execution falls back to `LegacyFeatureBridge`,
- the default bridge is no-op, so existing call paths remain untouched.

## Next steps
- Expose pilot features (for example reporting) through `PluginRuntime#executeFeature` while keeping legacy implementations behind `LegacyFeatureBridge`.
- Add concrete plugin packages with `META-INF/services` registration.
- Remove direct usage of legacy static helpers from modules once corresponding plugin handlers are stable.
