# Legacy 1.2.5 (ModLoader and early Forge): design

Design note for Minecraft 1.2.5 support, pulled forward by the maintainer on 2026-11-16 (the roadmap had it last). Back to [Home](Home.md). Status: **design**; the
only code so far is version detection (stage 1, below). Related: [Legacy b1.7.3](Legacy-b1.7.3.md) (the ModLoader-era layer that exists), [Legacy 1.4.7 estimate](Legacy-1.4.7-Estimate.md)
(the forward layer this note recommends hosting on), [Version roadmap](Version-Roadmap.md).

No 1.2.5 mod was available locally when this note was written (`test-mods-mcarchive`, `test-mods-b173`, `test-mods-147` hold 1.4.7 to Beta jars only; the
"1.2.5" hits in the file tree are mod versions of 1.7.10/1.12.2 jars). Everything below about mod internals is therefore from the API's history and from the
mapping data, not from a measured corpus; the places that need a real jar to confirm are marked **verify**. The first thing to do when a sample arrives is a corpus
page like [Legacy 1.4.7 corpus](Legacy-1.4.7-Corpus.md).

## What a 1.2.5 mod is

- **Obfuscated ("notch") names.** 1.2.5 predates MCP-named Forge dev environments for most authors: mods are compiled against the obfuscated game (`aaa`, `net/minecraft/client/Minecraft`
  kept its name) and distributed as a zip to be merged into `minecraft.jar`, or as a jar for ModLoader's `mods/` folder (ModLoader scans it). The client and the server of 1.2.5 were
  obfuscated **separately** (`Block` has a different name in each), so a mod ships a client build and a ModLoaderMP/SMP server build with the same `mod_X` class, exactly like Beta.
  Client mods dominate; many are client-only (renderers, GUIs, key binds) and many SMP mods are one jar with server classes next to the client ones.
- **`BaseMod` subclasses named `mod_*`** in the default package; ModLoader finds them by file name, constructs them and calls the hooks. Static `ModLoader` API (from the Beta layer's
  shim and from memory of 1.2.5; **verify** capitalisation on a real jar): `AddName`, `AddRecipe`, `AddShapelessRecipe`, `AddSmelting`, `RegisterBlock`, `RegisterEntityID`, `AddSpawn`,
  `RemoveSpawn`, `RegisterTileEntity`, `addOverride` (a texture file replaces or fills a slot of `terrain.png`/`items.png`), `getUniqueSpriteIndex`, `SetInGameHook`, `SetInGUIHook`,
  `RegisterKey`, `OpenGUI`, `AddArmor`, `AddLocalization`, `AddAchievementDesc`, `addAnimation`, `getPrivateValue`/`setPrivateValue`, `getMinecraftInstance`. Instance hooks of
  `BaseMod`: `load`, `modsLoaded`, `onTickInGame`, `onTickInGUI`, `keyboardEvent`, `generateSurface`, `generateNether`, `addRenderer`, `renderWorldBlock`, `renderInvBlock`,
  `takenFromCrafting`, `takenFromFurnace`, `addFuel`, `registerAnimation`. 1.3.1 lower-cased the statics; the 1.4.7 shim has the lower-case form, the Beta shim the capitalised form. `@MLProp` fields are the config.
- **ModLoaderMP** (`BaseModMp`, `ModLoaderMp`, `Packet230ModLoader`) for SMP; 1.2.5's ModLoaderMP was still the packet-230 model (Forge's custom payload packets arrive with 1.3).
- **Early Forge (3.x).** Forge for 1.2.x ships as `net.minecraft.src.forge.*` in `minecraft.jar` (`MinecraftForge`, `ITextureProvider`, `IGuiHandler`, `IOreHandler`/`registerOre`,
  `ISpecialArmor`..., `MinecraftForgeClient.preloadTexture`), no FML (FML starts with 1.3.2), no `@Mod`. **verify** the package: the Beta layer names `forge/*` as `net/minecraft/src/forge/*`
  already, which is where 1.2.x had them. Forge-era mods use `ITextureProvider.getTextureFile()` for their own sprite sheets and `Block.blockID` numeric ids from a config file.
- **Numeric ids and sheets.** Block ids 0 to 255 (Forge 3.x extended them to 4095, the on-disk Anvil format already stores 12 bits since 1.2.1), item ids 256 to 31999, 4-bit block metadata, item damage as
  subtype. Textures are indices into `terrain.png` (16x16 tiles of 16 px) and `items.png`, plus mod sheets; no `Icon`. This is the model the 1.4.7 layer already has (`SpriteSheets147`, `SheetCutter`).
- **Base class edits (jar mods).** Many 1.2.5 mods ship modified vanilla classes (`Block.class`, `RenderBlocks.class`, ...) that had to be merged into `minecraft.jar`; they cannot be
  translated as a standalone jar. Same policy as Beta: copies of game classes are dropped and reported ("Dropped N modified Minecraft class(es)"); a jar whose payload is mostly such classes is reported as not supported.

## Which deobfuscation path

The Beta layer names a jar through two CC0 tables fetched at build time and never committed: Babric intermediary (official to `class_17`) and biny (intermediary to readable names) (`BetaMappings`,
`BetaDeobfuscator`, task `genBetaMappings`). The 1.4.7 layer instead ships `notch.txt` (1.4.7 notch names to MCP/SRG names, derived at development time, see `tools/extract_legacy_signatures.py`).

Candidates for 1.2.5, checked on 2026-11-16 (metadata and artifacts fetched, not committed):

| Source | 1.2.5 available | Licence | Verdict |
|---|---|---|---|
| **Ornithe Calamus intermediary** (`net.ornithemc:calamus-intermediary:1.2.5-client` and `1.2.5-server`, classifier `v2`, `maven.ornithemc.net/releases`) | yes, client (1032 classes) and server separately | CC0 (repo LICENSE) | **use**: official (notch) to stable intermediary, per side |
| **Ornithe Feather** (`net.ornithemc:feather:1.2.5-client+build.26` / `1.2.5-server+build.26`, classifier `v2`) | yes, 1033 client classes, about 15,700 mapping lines, parameters named | CC0 (repo LICENSE) | **use**: intermediary to readable (Yarn style) names |
| MCP 6.x/7.x for 1.2.5 (`mcp62`) | only from archives (not on the Forge maven: `de.oceanlabs.mcp:mcp` starts at 1.10) | MCP's terms do not allow redistribution | **avoid**: no mirror to fetch at build time with a clear licence |
| Legacy Fabric intermediary | starts at 1.3 | CC0 | no 1.2.5 |
| Babric intermediary + biny | Beta 1.7.3 only | CC0 | the Beta path; wrong version |

Decision: **Calamus + Feather 1.2.5, client and server, fetched by Gradle at build time exactly like `genBetaMappings`, never committed.** The same two artifacts exist for 1.4.7 (`calamus-intermediary:1.4.7`,
`feather:1.4.7+build.31`) and Beta 1.7.3 (`feather:b1.7.3-client+build.26`), and Ornithe propagates names across versions, which is what the host decision below uses.

Measured on the real artifacts (2026-11-16):

| Comparison (Feather names) | Classes | Methods (by name) | Fields (by name) |
|---|---|---|---|
| 1.2.5 client | 1033 | 3622 | 3923 |
| 1.2.5 classes that also exist in 1.4.7 | **994 (96%)** | **3438 (95%)** | **3689 (94%)** |
| 1.2.5 classes that also exist in b1.7.3 | 650 (63%) | 2237 (62%) | |
| b1.7.3 classes that also exist in 1.2.5 | 650 of 679 (96%) | 2237 of 2342 (95%) | |

A mod written for 1.2.5 therefore sees a game that is, class for class, 96% the 1.4.7 game, and that has about 380 classes Beta does not (enchanting, potions and brewing, villagers and
the village generator, strongholds, the End, experience orbs, the AI task system, food stats, Anvil chunks, more mobs and blocks).

## Which host layer

**(a) Map 1.2.5 names onto the 1.4.7 forward chain**, versus **(b) extend the Beta platform with 1.2.5 names.**

| | (a) 1.4.7 chain | (b) Beta platform |
|---|---|---|
| Classes of 1.2.5 whose behaviour already exists | 994 of 1033 (96%), hosted by the 1.7.10 shim (121k lines) through the 1.4.7/1.5.2/1.6.4 forward tables | 650 of 1033 (63%), hosted by a 12.5k-line shim plus 20.7k lines of bridges written for Beta |
| Name join needed | Feather 1.2.5 to Feather 1.4.7 (same name) to 1.4.7 notch (Calamus 1.4.7) to the existing `notch.txt`; the rest of the pipeline is untouched | Feather 1.2.5 to Feather b1.7.3 to Babric official to biny: the shim is in biny names, Feather and biny agree on only 253 of 1033 class names, so a join is needed too |
| Numeric ids, `blocksList`, sprite sheets, `SheetCutter`, animations | exist (`SpriteSheets147`, `Animations147`) | exist for Beta (`AssetsB173`) |
| Entities | 1.4.7 has the AI task system 1.2.5 has (Grimoire of Gaia's 48 mobs run on it); full `LegacyLivingHost`/`LegacyMobHost` | Beta mobs use the old pre-AI path; supported animal subclasses only |
| World height | 256, like 1.2.5 (Anvil, since 1.2.1) | 128 |
| ModLoader statics implemented today | lower-case form: `addAnimation addArmor addBiome addCommand addEntityTracker addLocalization addName addOverride addRecipe addShapelessRecipe addSmelting addSpawn addTrade getUniqueSpriteIndex registerBlock registerEntityID registerKey registerTileEntity removeSpawn setInGUIHook setInGameHook setPrivateValue ...` (43 members) | capitalised form (32 members): `AddName AddRecipe AddSmelting RegisterBlock RegisterEntityID AddSpawn OpenGUI SetInGameHook ...` plus `AddAchievementDesc`, `AddAllFuel`, `loadImage` |
| `BaseMod` hooks | `getVersion load modsLoaded onTickInGame(float, Minecraft) addRenderer renderWorldBlock takenFromCrafting ...` (lower case, FML-era) | `Version ModsLoaded OnTickInGame(Minecraft) GenerateSurface ...` (capitalised, Beta signatures) |
| What would be built | a capitalised-name facade for `ModLoader`/`BaseMod` over the 1.4.7 one; a `1.2.5 notch to 1.4.7 notch` rewriter; side detection | the 380 missing game systems; names join; height |
| Risk | small behavioural drift between 1.2.5 and 1.4.7 (1.3 changed the packet layer, the inventory and the integrated server; most content mods are not affected) | most of the work is new game systems |

**Recommendation: (a).** Host 1.2.5 on the 1.4.7 forward chain, and take from the Beta layer only what is version-neutral and can be called without touching it: the idea of a mod-set wide index of moved
default-package classes (the 1.4.7 layer already moves `mod_X` classes), the client/server build selection of two jars with one `mod_X` class (`BetaJars.selectBuilds`; a new, separate class for 1.2.5 until a
shared helper is worth a hook), the ShockAhPI-style "default-package class that no jar has is another API" rule, and the Beta test tooling (`tools/dev/test_b173_one.py` shape). Reasons: 96% against 63% class
coverage, the AI/256-height/numeric-id model matches, and the forward chain is where the maintained, tested game systems are. The Beta code stays untouched by this plan (no hooks are required so far).

The deobfuscator for 1.2.5 is then **one new step in front of the 1.4.7 path**: rename a 1.2.5 jar's notch references to 1.4.7 notch names through the Feather join (class by class; members by name and
descriptor), then hand the jar to the unchanged 1.4.7 pipeline as a `1.2.5`-tagged variant that also gets the capitalised `ModLoader` facade. Classes with no 1.4.7 counterpart (39 in the client) are kept under
synthesized names and reported, so a mod that uses them fails at link time with a clear report instead of at run time.

## Stages

1. **Detection, "1.2.5 (not supported yet)"** (done 2026-11-16: `loader/.../Mc125Jars`, table `detect/notch-1.2.5.txt.gz` written by `tools/gen_notch_125.py` from Calamus, 14,000 obfuscated
   names of the client and server; a jar with at least 15 references to obfuscated members of which 60% resolve in 1.2.5 and clearly better than in 1.4.7/1.5.2/1.6.4 is reported under
   "Mods of a Minecraft version that is not supported yet" in `legacyconnector-report.txt` and not loaded; `mods/1.2.5/` is taken without the check; tests `Mc125JarsTest` on synthetic jars;
   not yet run on a real 1.2.5 jar). Before it, a jar like that was handled as follows: Today a 1.2.5 ModLoader jar is not recognised as such: `BetaJars` rejects it (fewer than 60% of its references resolve in the b1.7.3 names) and the
   1.4.7 chooser takes any bare `BaseMod` jar without evidence "for the oldest supported version", so it would be hosted as 1.4.7 and fail at link time or misbehave. Detection fingerprints the notch names against the 1.2.5
   tables and reports the jar as 1.2.5, not loaded. Unlocks: honest reports on the compatibility site, a corpus count of 1.2.5 jars, no mis-hosting.
2. **Mapping generation and the join** (`genMappings125`, opt-in like `genBetaMappings`): Calamus and Feather 1.2.5 client and server, 1.4.7; write the `1.2.5 notch to 1.4.7 notch` table at build time and
   expose it as `NotchNames`-style lookups; `checkLegacyMod` can then report how many references of a 1.2.5 jar resolve (an offline "probe" tier on `docs/data/mods.json`). Unlocks: measuring
   which 1.2.5 mods can link at all, without running the game.
3. **Host: the capitalised `ModLoader`/`BaseMod` facade and the 1.2.5 version family** (`LegacyVersion.V1_2_5` forward to 1.4.7, side selection of the two builds): server self test loads ModLoader content
   mods (blocks, items, recipes, textures from `addOverride`, tile entities, spawn registrations). Unlocks: the first playable 1.2.5 content mods on a server and in a client. After this: ModLoaderMP packets, early
   Forge (`net.minecraft.src.forge`) behind its own flag, RedPower 2/Tekkit-era mods as the stress test, and the Beta-scope rules of CLAUDE.md apply (implement shared APIs and run the mods' callbacks, no mod identity hardcoding).

## Open questions (need a real 1.2.5 jar)

- Capitalisation of the 1.2.5 `BaseMod` hooks and of the `ModLoader` statics (the facade must accept whichever the jar uses; a case-insensitive hook lookup is the cheap safe form).
- The Forge 3.x package (`net.minecraft.src.forge` vs `forge`) and whether common jars carry their own copy of ModLoader/Forge (the Beta layer drops bundled copies).
- What share of the jars are mostly base class edits; the audit tool for the Beta archive (`tools/dev/audit_b173_archive.py`) is the model.
- Server jars: 1.2.5 server mods typically bundle the ModLoaderMP server class `ModLoaderMp`; check the separate-obfuscation fingerprint on both sides.
