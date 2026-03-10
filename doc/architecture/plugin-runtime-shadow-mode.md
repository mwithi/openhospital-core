# Plugin Runtime Shadow Mode (Step 2)

Step 2 introduces a plugin runtime that can be adopted incrementally without changing legacy execution paths.

## Components
- `CorePlugin`: plugin lifecycle contract (`start`/`stop`) plus ordering metadata.
- `PluginDiscovery`: abstraction for plugin discovery.
- `ServiceLoaderPluginDiscovery`: default discovery based on `ServiceLoader`.
- `PluginRegistry`: in-memory registry of feature handlers exposed by plugins.
- `PluginRuntime`: orchestrates plugin lifecycle and executes feature handlers with fallback to `LegacyFeatureBridge`.
- `LegacyFeatureBridge` + `NoOpLegacyFeatureBridge`: fallback to legacy behavior when a feature has no plugin handler.

## Compatibility
The plugin runtime is introduced in shadow mode and is not wired to existing legacy flows by default. This keeps deployments unchanged while allowing pilot features to be migrated behind explicit runtime usage.
