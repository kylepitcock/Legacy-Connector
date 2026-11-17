# Forge 1.20.1 spawn compatibility probe

This is an isolated, test-only Forge 1.20.1 mod used to exercise real legacy
bytecode against the Legacy Connector development runtime. It is not a
production dependency and should not be included in a modpack.

The fixture is pinned to Minecraft 1.20.1 / Forge 47.3.0 and uses official
Mojang mappings at compile time. ForgeGradle reobfuscates the output to the
SRG names expected by the Legacy Connector translator.

## Build

Run from the main Legacy Connector checkout:

```powershell
.\gradlew.bat -p .\testmods\forge1201-spawn-probe build --no-daemon
```

ForgeGradle's 1.20.1 MCP processing requires a discoverable JDK 17 even when
the main Legacy Connector build continues to use Java 21. The source-runtime
workspace README explains how to register a custom JDK 17 path in the user's
Gradle properties. Do not commit a machine-specific JDK path here.

The expected reobfuscated artifact is:

```
testmods/forge1201-spawn-probe/build/libs/legacyconnector-spawn-probe-1.0.0.jar
```

## Run inside the existing runClient profile

Copy the probe into the versioned legacy-mod folder that the loader scans:

```powershell
New-Item -ItemType Directory -Force .\run\mods\1.20.1 | Out-Null
Copy-Item .\testmods\forge1201-spawn-probe\build\libs\legacyconnector-spawn-probe-1.0.0.jar .\run\mods\1.20.1\
New-Item -ItemType Directory -Force .\run\spawnprobe | Out-Null
Copy-Item .\testmods\forge1201-spawn-probe\spawnprobe.js .\run\spawnprobe\
$probeDir = (Resolve-Path .\run\spawnprobe).Path
.\gradlew.bat runClient "-PserverScript=$probeDir" -PserverScriptMod=legacyspawnprobe --no-daemon
```

Create or open a world. Once the integrated server has ticked, the development
server-script runner loads `SpawnProbe` through the fixture mod's classloader
and writes the result to:

```
run/spawnprobe/spawnprobe.out
```

The script is renamed to `spawnprobe.js.done` after execution. Copy a fresh
`spawnprobe.js` into the directory before each subsequent run.

Expected successful output starts with `LegacySpawnProbe: 7/7 passed`.
Any failed assertion or thrown exception is included in the result file.

## Entity type registration

The probe entity type is registered through Forge's `DeferredRegister` from
the mod constructor. Do not construct it from `SpawnProbe` static
initialization: the server script runs after the entity-type registry is
frozen, and an unregistered `EntityType.Builder.build(...)` at that point
fails with `IllegalStateException: Registry is already frozen`.

## Entity attributes

Because `SpawnProbeMob` extends `Zombie`, the mod registers a default
attribute supplier for its custom entity type using Forge's
`EntityAttributeCreationEvent` on the mod event bus and
`Zombie.createAttributes().build()`. Registering the EntityType alone is not
sufficient: without the matching attribute supplier, Minecraft cannot create
the mob and its attribute map remains unavailable during construction.

## What it covers

1. Explicit Forge 1.20.1 `EntityType.create(..., CompoundTag, ...)` with a
   non-null tag, preserving finalizeSpawn-before-Consumer order.
2. The same create overload with an explicit null tag.
3. Explicit Forge 1.20.1 `EntityType.spawn(..., CompoundTag, ...)`.
4. Forge 1.20.1 `EntityType.spawn(ItemStack)` with legacy root custom data.
5. ItemStack spawn with no root tag, checking nullable `getTag()` semantics.
6. A direct virtual five-argument `Mob.finalizeSpawn` call.
7. A nested create with an explicit null tag from inside a legacy Mob override.
   The override also calls `super.finalizeSpawn(..., tag)`, testing special-call
   adaptation and nested scope isolation.

These are focused runtime checks, not a claim that all EntityType or Mob
subclasses are compatible. The vanilla Wolf/AgeableMob dispatch difference
remains a separate audit item.
