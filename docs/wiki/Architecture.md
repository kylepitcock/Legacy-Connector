# Architecture

How the loader, the shim and the runtime fit together. Back to [Home](Home.md).

Legacy Connector translates a legacy mod jar at load time, links it against a hand-written shim of the old API (plus a synthesized API for whatever the shim lacks) and bridges its registrations to the modern registries.

```
mods/SomeMod-1.7.10.jar
        │  loader (FML locator service, runs before mod discovery)
        ▼
  1. Inspect      detect the legacy version (mcmod.info, @Mod, FML/Forge references)
  2. Translate    relocate Minecraft/Forge/FML classes into the shim namespace, SRG -> MCP names,
                  Java 8 -> 21 fixes (removed JDK APIs, reflection on final fields, Guava, LWJGL 2 -> 3)
  3. Link         every legacy API reference is resolved against, in order:
                    a. the hand-written shim   - legacy API re-implemented on top of NeoForge (real behaviour)
                    b. the synthesized API     - generated for whatever the shim lacks (links, default behaviour)
  4. Resources    lang files, textures and models converted to 1.21 formats
        ▼
.legacyconnector/cache/SomeMod-<hash>.jar  +  legacyconnector-synth-1.7.10-<hash>.jar  ->  loaded by NeoForge
        │  runtime (normal NeoForge mod)
        ▼
  Bridges         legacy registrations -> modern registries (blocks, items with metadata, tile entities,
                  recipes -> data pack, ore dictionary -> tags, GUIs -> menus/screens, networking, world gen, ...)
```

## Generic first, per-mod code only as a last resort

The layer is not written for specific mods. Mods are made to work by implementing the **legacy API** they
were written against:

- **Hand-written shim** (`src/main/java/legacyconnector/v1_7_10/...`): `Block`, `Item`, `ItemStack`, `World`, `Entity`,
  `GameRegistry`, `Container`, `GuiContainer`, `TileEntitySpecialRenderer`, `SimpleNetworkWrapper`, ... as views
  over the modern objects.
- **Synthesized API** (`loader/.../synth/`): any legacy class or member the shim does not implement yet is generated
  at load time from the legacy class hierarchy (shipped as `loader/src/main/resources/legacyconnector/legacy/<ver>/hierarchy.txt`)
  and the descriptors the mods use. Generated classes keep their real supertypes (a synthesized `BlockColored` *is*
  a `Block`), constructors chain to the closest real one, methods return safe defaults, and calls on hand-written
  classes for missing methods are dispatched to the mod's own override when it has one. This is what lets an
  arbitrary mod *load* instead of crashing with `NoClassDefFoundError`; the first call to each stub is logged so the
  most-used gaps can be implemented properly.

Mod-specific code is the exception: it is used only when no generic fix works, only for popular mods, and it is kept small
and isolated (today: the DragonAPI crafting/smelting hooks in `CoremodHooks`).

## Legacy loader behaviour that is reproduced

- FML `@Optional.Interface` / `@Optional.Method` stripping for absent mods, and removal of interfaces from APIs that
  exist nowhere (what some mods' own coremods did).
- Integrations with mods that are not installed: methods that need an absent API are replaced by a lazy
  `NoClassDefFoundError`, so the rest of the class still loads (the JVM verifies whole classes at once).
- Access transformers: the shim is public and non-final throughout, as mods compiled against AT-widened Minecraft.
- FML's Scala language adapter (`object` mods) and the libraries 1.7.10 shipped: Scala 2.11, Trove, vecmath.
- Coremods: their plugins are constructed and set up as FML did (construct all, then `getASMTransformerClass` /
  `injectData` / setup hook by sorting index; mod containers count as loaded mods). Their class transformers run over
  the legacy mod classes before translation, in a stand-in LaunchWrapper class loader with the legacy FML API, so
  coremods that patch *mod* classes work: CoFH `@Strippable`, AE2 `@Integration`, OpenMods, Steve's Addons, patcher
  mods fixing other mods' API mismatches. Transformers aimed at Minecraft's own classes find nothing to patch.
- LaunchWrapper's `Launch.blackboard` / `Launch.classLoader`.
- Resource URLs: mod jars are served from NeoForge's `union:` file system; mods that list their own jar's files
  (checking for `jar:`/`file:` URLs) get the equivalent `jar:file:` URL.
- Mods linking against each other's classes (e.g. RotaryCraft extends DragonAPI) are linked as one modset; APIs
  shaded into several jars are merged, preferring the copy from the mod that owns the API.
