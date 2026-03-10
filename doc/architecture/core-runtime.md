# Core Runtime Preparation

## Goal
Establish a first-class API for the runtime services that every module or future plugin will rely on. The new API isolates configuration and data access responsibilities so that incremental refactors can detach from static helpers and legacy singletons.

## Key concepts
- **CoreConfiguration** &mdash; unified read-only view of application properties; legacy code continues to populate values via `settings.properties`.
- **DatabaseGateway** &mdash; façade encapsulating `EntityManager` management and providing a safe execution model for database work units.
- **CoreContext/CoreRuntime** &mdash; immutable container that wires the default adapters while allowing overrides when modules are migrated.

## Why now?
This is step 1 of the microkernel migration. Introducing the API without moving any business logic keeps the legacy deployment untouched while giving developers a stable contract to target.

## Next steps
- Introduce plugin runtime infrastructure that consumes `CoreContext`.
- Incrementally migrate non-critical modules to the new abstractions by providing dedicated adapters or implementations.
- Remove direct usage of `GeneralData` and `DbJpaUtil` from refactored components in favour of the new contracts.
