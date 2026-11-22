# Forge 1.20.1 Coremod Metadata Preflight Inventory

## Purpose

Add bounded, read-only discovery of Forge's JavaScript coremod registry to the universal Forge 1.20.1 preflight analyzer. The existing analyzer already reports the legacy manifest `FMLCorePlugin` class and all manifest attributes, but it did not parse `META-INF/coremods.json`. These are separate declaration mechanisms and must not be conflated.

Forge's loader documentation identifies `META-INF/coremods.json` as the registry used to load JavaScript coremods. Forge CoreMods are script-based wrappers around ASM and participate in ModLauncher class transformation. The preflight analyzer therefore records metadata and script-resource presence, but never executes scripts or constructs transformers.

## Discovery and parsing boundary

The analyzer reads only the exact outer-JAR resource:

`META-INF/coremods.json`

The expected root shape is a JSON object mapping a coremod identifier/name to a script resource path. The parser:
- bounds the metadata resource to 1 MiB and 4,096 declarations;
- decodes UTF-8 strictly and parses JSON strictly;
- retains names and script paths, including duplicate name occurrences;
- rejects empty/control-character names and unsafe paths (absolute paths, backslashes, colons, empty path segments, `.` and `..`);
- checks each safe path against the outer archive and marks missing script resources explicitly;
- sorts emitted declarations deterministically;
- reports absent, parsed, partially parsed, invalid JSON, invalid UTF-8 and oversized states.

A missing declared script is a referential finding, not a JSON parse failure. Malformed declarations make the metadata result partially parsed. A syntactically invalid JSON document is reported as invalid JSON while preserving any declarations already read for diagnosis.

## Explicit non-goals

This slice does not:
- read, compile, evaluate, or execute JavaScript;
- infer transformer injection points, target classes, methods, or instruction selectors from a script;
- instantiate Forge CoreMod APIs or ModLauncher transformers;
- decide whether a coremod's transformation is semantically portable to NeoForge 1.21.1;
- inspect `META-INF/coremods.json` or scripts inside nested JARs;
- replace the separate legacy manifest `FMLCorePlugin` field or infer that either declaration mechanism is equivalent.

The report is an inventory signal for Phase 1. Transformer interpretation and lifecycle-order migration remain later dedicated work.

## Unified preflight integration

The unified preflight JSON/text report adds `coremodMetadata` in schema 1.12 / analyzer 1.12.0. It reports the resource path, status, SHA-256, declaration list, valid/malformed/duplicate/missing counts and warnings. Each declaration records name, script path, resource status, duplicate-name evidence and diagnostic text.

The report limitation explicitly states that the JavaScript and nested coremod registries remain unanalyzed. The analyzer is static evidence only; it does not authorize transformation or compatibility claims.

## Targeted validation

Run the focused test classes in the isolated worktree, without `:loader:clean`:

```powershell
.\gradlew.bat :loader:test --tests org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201CoremodAnalyzerTest --tests org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201PreflightAnalyzerTest --no-daemon --console=plain
```

The new test class covers exact-path discovery, deterministic declaration order, present/missing script resources, duplicate names, malformed and unsafe entries, invalid JSON/root shape, invalid UTF-8 and oversized resources. The unified preflight tests cover JSON/text integration and the NOT_PRESENT state.

## References

- Forge FML loader description of `META-INF/coremods.json` and the coremod loading stage: https://github.com/MinecraftForge/MinecraftForge/wiki/FML
- Forge CoreMods project description: https://github.com/MinecraftForge/CoreMods
