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

## Mods distributed as a folder

A legacy mod can also be a folder in `mods/` (MCHeli: `mods/mcheli/` with `mcmod.info`, the class packages and `assets/`, plus content packs the user adds as zips). FML's own locator ignores folders. The loader packs such a folder into `.legacyconnector/dirmods/<name>.jar` (rebuilt only when a file's name, size or time changes; zips/jars at the folder's top level are left out) and translates that jar like any other. The legacy `ModContainer.getSource()` of the mod stays the real folder, so code that lists its content with `java.io.File` (and reads the content packs next to it) works; for jar mods it is the jar, unchanged.

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

Legacy content enters the game through the **native extension points** a 1.21.1 mod would use, and the layer only translates
the legacy API into them: biomes through the pack's biome placement, world generators as placed features in biome modifiers,
village buildings as extra pieces of the vanilla village structure (vanilla's layout untouched), structure chests through loot tables (and global loot modifiers),
spawns through biome modifiers, recipes and tags as a data pack, containers as capabilities. Post-generation decorators,
mixins that override a vanilla result, or filling things directly are avoided where a native hook exists: they ignore the pack's
other mods (legacy village pieces overlapped houses, legacy chests skipped the pack's loot, a biome override was bypassed by
TerraBlender).

Mod-specific code is the exception: it is used only when no generic fix works, only for popular mods, and it is kept small
and isolated (today: the DragonAPI crafting/smelting hooks in `CoremodHooks`).

### Mod ids, mod checks and failing handlers

- A legacy mod whose modern id a modern mod of the pack also uses (declares, or names as a dependency) loads under `<id>_legacy`
  (legacy Witchery is `witchery_legacy` next to mods that integrate with Witchery: Resurrected); its registry namespace stays
  `<id>`, so items, textures and saves are unchanged.
- Legacy `Loader.isModLoaded` only counts legacy mods and bridged libraries, never a modern mod of the same id: legacy
  integrations call the legacy API (ChromatiCraft registered its Forestry bee frame for the modern Forestry and failed).
- A legacy event handler that fails 1,000 times in a row without one success is unregistered with one log line (handlers of a
  mod that failed to load threw about 1,000 exceptions a second). `events.skip` switches handlers off by hand.
- A creative tab that gets the same stack twice skips the duplicate instead of failing the tab build (`CreativeTabDuplicateMixin`).
- Mixins of other mods are never applied to classes of translated legacy jars (`LegacyMixinPlugin`, a Mixin extension that empties the mixin set of a class whose jar carries the provenance resource). A legacy mod and its modern port share class names (Mekanism `RenderFlame` vs Iris's mixin for the 1.21 one), and the failed injection would take the legacy mod down. This mod's own configs (`legacyconnector*`) still apply.

## Legacy loader behaviour that is reproduced

- FML `@Optional.Interface` / `@Optional.Method` stripping for absent mods, and removal of interfaces from APIs that
  exist nowhere (what some mods' own coremods did).
- Integrations with mods that are not installed: methods that need an absent API are replaced by a lazy
  `NoClassDefFoundError`, so the rest of the class still loads (the JVM verifies whole classes at once).
- Access transformers: the shim is public and non-final throughout, as mods compiled against AT-widened Minecraft.
- FML's Scala language adapter (`object` mods) and the libraries 1.7.10 shipped: Scala 2.11, Trove, vecmath.
- Bundled libraries (Scala 2.11 + akka, typesafe config, Trove, vecmath, javax.activation) are resources of the loader jar, not jar-in-jar entries
  (those always enter the game module layer and clash with modern mods bundling the same packages, e.g. ScalableCatsForce's Scala 2.13:
  `ResolutionException: Modules X and Y export package scala.annotation`). `BundledLibraries` hands a library group to FML only when a legacy
  jar references it and no other jar of the mods folder (or jar nested in one) has a package of it; another Trove/config/activation then serves
  the legacy mods, while a foreign Scala (not 2.11) makes the Scala-using legacy mods skipped with a message in the report. `-PembeddedLibs`
  (dev) tests this path with the libraries read from `build/legacy-libs`.
- APIs shaded by legacy jars that a modern mod of the pack also ships (DragonAPI carries the 1.7.10 AE2 API, `appeng.api.*`, next to modern AE2)
  are relocated in every legacy jar to `legacyconnector_relocated/<version>/...` (`CrossVersionPackages.modern`), so the two never export the same
  package. Classes only the modern mod has keep their name; packages of `@Mod` classes are never relocated.
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
