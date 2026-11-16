# Legacy 1.2.5 (ModLoader and early Forge): design

Design note for Minecraft 1.2.5 support, pulled forward by the maintainer on 2026-11-16 (the roadmap had it last). Back to [Home](Home.md). Status: **hosted behind an opt-in** (stages 1 to 3 below);
the corpus measurements are in [Legacy 1.2.5 corpus](Legacy-1.2.5-Corpus.md). Related: [Legacy b1.7.3](Legacy-b1.7.3.md) (the ModLoader-era layer that exists), [Legacy 1.4.7 estimate](Legacy-1.4.7-Estimate.md)
(the forward layer this note recommends hosting on), [Version roadmap](Version-Roadmap.md).

The first draft of this note (2026-11-16 morning) was written without a 1.2.5 jar. Since then 91 real archives from MCArchive were measured
([Legacy 1.2.5 corpus](Legacy-1.2.5-Corpus.md), `tools/compat/scan_125.py`) and the real `ModLoader.class`, `BaseMod.class`, ModLoaderMP and Forge 3.3.7.135 were read with `javap`; what they
changed is marked **measured** below, and the corrected statements replace the guesses. The member tables for the shim worker are in `tools/dev/agent-tasks/1.2.5-shim-requests.md`.

## What a 1.2.5 mod is

- **Obfuscated ("notch") names.** 1.2.5 predates MCP-named Forge dev environments for most authors: mods are compiled against the obfuscated game (`aaa`, `net/minecraft/client/Minecraft`
  kept its name) and distributed as a zip to be merged into `minecraft.jar`, or as a jar for ModLoader's `mods/` folder (ModLoader scans it). The client and the server of 1.2.5 were
  obfuscated **separately** (`Block` has a different name in each), so a mod ships a client build and a ModLoaderMP/SMP server build with the same `mod_X` class, exactly like Beta.
  Client mods dominate; many are client-only (renderers, GUIs, key binds) and many SMP mods are one jar with server classes next to the client ones.
- **`BaseMod` subclasses named `mod_*`** in the default package; ModLoader finds them by file name, constructs them and calls the hooks. **Measured** (real `ModLoader 1.2.5.zip`, 2012-04-02): the statics and the hooks are **lower case**, exactly the 1.4.7 spelling (the capitalised
  `AddName`/`OnTickInGame` is Beta's only; no corpus jar uses it). Statics (all `public static`): `addAchievementDesc(Achievement,String,String)`, `addAllFuel`, `addAllRenderers`, `addAnimation(TextureFX)`, `addArmor`,
  `addBiome`/`removeBiome`, `addLocalization` (2 overloads), `addName` (2), `addOverride(String,String)` returning the sprite index and `addOverride(String,String,int)`, `addRecipe`, `addShapelessRecipe`, `addSmelting`,
  `addSpawn`/`removeSpawn` (Class or String, with or without biomes), `dispenseEntity`, `genericContainerRemoval`, `getLoadedMods`, `getLogger`, `getMinecraftInstance`, `getPrivateValue`/`setPrivateValue` (name or index),
  `getUniqueBlockModelID`, `getUniqueEntityId`, `getUniqueSpriteIndex`, `isGUIOpen`, `isModLoaded`, `loadConfig`/`saveConfig`, `loadImage`, `onItemPickup`, `openGUI`, `registerBlock` (2), `registerEntityID` (2),
  `registerKey`, `registerPacketChannel`, `registerTileEntity` (2, the second with a renderer), `renderBlockIsItemFull3D`, `sendPacket`, `serverChat`, `setInGameHook`, `setInGUIHook`, `takenFromCrafting`,
  `takenFromFurnace`, `throwException`, plus the callbacks the patched game classes call (`onTick`, `populateChunk`, `receivePacket`, `registerAllKeys`, `registerAllTextureOverrides`, `renderInvBlock`,
  `renderWorldBlock`, `serverConnect`, `serverDisconnect`); fields `DEBUG`, `VERSION`, `props`, `cfgLoggingLevel`. Instance hooks of `BaseMod`: abstract `getVersion()` and `load()`; `modsLoaded`, `onTickInGame(float, Minecraft)`
  (client; the server build has `onTickInGame(MinecraftServer)`), `onTickInGUI(float, Minecraft, GuiScreen)`, `keyboardEvent`, `generateSurface`/`generateNether(World,Random,int,int)`, `addRenderer(Map)`,
  `renderWorldBlock`, `renderInvBlock`, `takenFromCrafting`, `takenFromFurnace`, `addFuel`, `registerAnimation`, `onItemPickup`, `dispenseEntity`, `getName`, `getPriorities`, `receiveChatPacket(String)`,
  `receiveCustomPacket(Packet250CustomPayload)`, `serverConnect(NetClientHandler)`, `serverDisconnect()`. `@MLProp` fields are the config. ModLoader 1.2.5 is itself a jar mod: it ships 13 patched game classes
  (NetClientHandler, RenderBlocks, BlockDispenser, ...) plus `ModLoader`, `BaseMod`, `MLProp`, `ModTextureStatic`, `ModTextureAnimation`, `EntityRendererProxy` unobfuscated.
- **ModLoaderMP** (`BaseModMp`, `ModLoaderMp`, `Packet230ModLoader`, `NetClientHandlerEntity`, `ISpawnable`) for SMP; **measured**: still the packet-230 model (`modId`, `packetType`, int/float/double/String arrays;
  `BaseModMp.handlePacket`, `handleGUI(int)`, `handleTileEntityPacket`), 11 corpus units use it. ModLoader 1.2.5 itself already has the packet-250 channel API (`registerPacketChannel`, `sendPacket`, `receivePacket`);
  Forge's `NetworkMod`/`IPacketHandler` ride the same packet 250.
- **Early Forge (3.x).** **Measured** (Forge 3.3.7.135, 51 corpus units): the package is the top-level **`forge`** (`forge/MinecraftForge`, `forge/ITextureProvider`, `forge/IGuiHandler`,
  `forge/IOreHandler`, `forge/ISpecialArmor`, `forge/MinecraftForgeClient.preloadTexture`, `forge/Configuration`, `forge/NetworkMod`, `forge/oredict/*`), not `net.minecraft.src.forge`. It is **not** FML-free:
  Forge 3.3.x for 1.2.5 bundles FML 2.2.48 (`cpw/mods/fml`: `@Mod` with `Mod$PreInit/Init/PostInit`, `ITickHandler`, `IWorldGenerator`, `FMLCommonHandler`, ModLoader reimplemented as
  `cpw.mods.fml.common.modloader`) and replaces the default-package `ModLoader`/`BaseMod` by FML's versions of the same API. Only IC2, Forestry, PortalGun and LogisticsPipes reference `cpw.mods.fml`, in
  optional compat code. Forge-era mods use `ITextureProvider.getTextureFile()` for their own sprite sheets and `Block.blockID` numeric ids from a config file.
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

## Stage 2: the join (done 2026-11-16)

`./gradlew :loader:genMappings125` (opt-in: nothing is fetched or generated by the default build; `-Pmappings125` additionally puts the result into the loader jar and the test classpath).
Inputs are pinned by SHA-256 in `loader/build.gradle` (Calamus `1.2.5-client`, `1.2.5-server`, `1.4.7`; Feather `1.2.5-client+build.26`, `1.2.5-server+build.26`, `1.4.7+build.31`, all classifier `v2`,
`maven.ornithemc.net`, CC0) and printed on every run. `mapping/Mc125Mappings` joins official 1.2.5 to readable name (Feather) to official 1.4.7, per side, on class name and on member name plus
descriptor in readable names; Feather keeps one name (or one propagated intermediary id) for a member that did not change, which is what makes the join work. Output (build-local, never committed):
`legacyconnector/mappings/1.2.5/notch-to-147.tsv` (`C`, `F`, `M` rows with owner, name and descriptor on both sides) and `join-report.txt`.
Committed derived data: only `detect/notch-1.2.5.txt.gz` (stage 1: the obfuscated names and descriptors of Calamus 1.2.5, 14,000 rows, no readable names), justified as a CC0-derived table; the 1.4.7 side is the
existing `legacy/1.4.7/notch.txt`. All 1,699 joined classes, 5,101 of 5,103 joined methods and all 6,335 joined fields land on names that table has (checked by script, the two misses are `MinecraftServer` members).

| | Classes joined | Members joined |
|---|---|---|
| client | 1009 of 1032 (98%) | 6965 of 7384 (94%) |
| server | 730 of 744 (98%) | 4870 of 5151 (95%) |

(First version of the join, readable name and descriptor only: 993 / 6781 client, 706 / 4657 server. The real corpus showed what it missed, and four more tiers were added in the order the
loader tries them: the same Calamus intermediary name in both versions (the member did not change, Feather only named it differently; +256 client members, but only when the descriptors
agree up to class types: 91 joins with different primitives were wrong and are refused), the same name with the class types of the descriptor ignored (1.3 retyped `Minecraft.thePlayer` and
`theWorld`), a field that is the only one of its name, and `loader/src/main/resources/legacyconnector/mappings/1.2.5/manual-aliases.tsv` for pure renames found by hand. A member that still has no
counterpart is renamed to `mcc125$<obfuscated name>` instead of keeping its short name: the obfuscation is re-randomized every release, so `AxisAlignedBB.b(DDDDDD)` of 1.2.5 would have bound
to an unrelated `b(DDDDDD)` of 1.4.7. The marker never exists, which the link report shows and the synthesizer stubs. `genMappings125` also lists "leftover candidates" (the one unjoined member and
the one unclaimed member of a class with the same kind and descriptor shape): by hand they are mostly different members, so they are never joined.)

What does not join is the real size of stage 3. Classes with no 1.4.7 counterpart (anonymous classes `Name__digits` are omitted here, they follow their outer class):
`CreativeInteractionManager`, `SurvivalInteractionManager`, `MultiplayerInteractionManager` (1.3 reworked the client's game mode and integrated server: `client`); `MultiplayerWorld`, `MultiplayerChunkCache`
(`client/world`); `ChatScreen` variants and `WorldSaveConflictScreen` (`client/gui`); `FireballRenderer`; `BossEntity` (`entity/mob`); `LogItem`, `MapItem`, `PaintingItem`, `SaplingItem`
(`item`: 1.3 made these data-driven); `AddItemPacket`, `LoadWorldChunkPacket`, `RemoveEntityPacket` (`network/packet`); `PortalForcer`, `ScheduledTick`, `ChunkCache`, `NetherLiquidFallFeature`,
`RareGlowstoneClusterFeature` (`world`, `world/chunk`, `world/gen`); server only: `CommandHandler`, `CommandSource`, `ServerProperties`, `MinecraftServerAccess`, `PendingCommand` and the server GUI
(`server/gui`, nine classes, never used by mods); the `isom` preview applet (client, 7 classes, irrelevant). Members that do not join concentrate in `client` (70 of 249: the `Minecraft` class
itself), `client/resource` (21 of 40: 1.3 reworked resource loading), `client/world` (23 of 34), `network` (49 of 115: `Connection` and the packet handler moved to the 1.3 networking), `server` (98 of 278),
`entity/projectile` (17 of 46), `entity/mob` (40 of 670), `world` (54 of 350: biome/chunk access), `world/chunk` (37 of 222), `enchantment` (12 of 89), `client/gui` (32 of 542), `client/render`
(37 of 1051). A mod that touches only blocks, items, recipes, tile entities, entities and worldgen sees less than 5% unjoined members; the unjoined ones are the networking, the client game mode, the
resource layer and the 1.3 world/chunk access.

## Stage 3 status (2026-11-16): hosted behind an opt-in

`-Dlegacyconnector.mc125=true` (and a build made with `-Pmappings125`): a detected 1.2.5 jar (or any jar in `mods/1.2.5/`) becomes a 1.4.7 jar for the rest of the pipeline.
`LegacyVersion.V1_2_5` is a source version only (last in the enum, same prefixes and forward chain as 1.4.7); `LegacyJar.version` is `V1_4_7`, `sourceVersion` is `V1_2_5`, `notchSide` is the
client or server obfuscation the jar uses. `transform/Notch125To147` runs first in `LegacyJarTransformer.readPatched` (right where `BetaDeobfuscator` runs for Beta jars): game classes, fields
and methods to 1.4.7's names through the join table, resolved up the supertypes (jar classes first, then the 1.4.7 hierarchy of `notch.txt`) because obfuscated member names are unique only per class;
overrides in mod classes are renamed with the game method; copies of game classes are dropped and reported. Without the opt-in, or in a build without the table, a 1.2.5 jar is only reported as before.

The capitalised `ModLoader`/`BaseMod` facade is a **rename**, not a second class: the 1.4.7 shim's `ModLoader` already has the lower-case statics (`addName`, `registerBlock`, `addRecipe`, ...), so the
capitalised calls (`AddName`, `RegisterBlock`, `AddRecipe`, `AddShapelessRecipe`, `AddSmelting`, `SetInGameHook`, ...; set `Notch125To147.MODLOADER_STATICS`, 34 names) are rewritten to them, and
capitalised `BaseMod` hooks that a mod declares (`Load`, `ModsLoaded`, `OnTickInGame`, `GenerateSurface`, `Version` -> `getVersion`, ... `BASEMOD_HOOKS`) are renamed to the lower-case ones. A rename keeps
working for whichever spelling a 1.2.5 jar uses, since calls that are already lower-case pass through. Tests (`Notch125To147Test`, `Mc125JarsTest`): a synthetic `mod_*` class assembled in the test
registers a block, a name, two recipes and reads a field through the capitalised API; the calls end on `ModLoader.registerBlock`/`addName`/`addRecipe`/`addShapelessRecipe` and on 1.4.7 names; a
subclass of a game class has its override and inherited call renamed; every renamed static exists in the 1.4.7 shim source; with the real table (`-Pmappings125`) 60 real 1.2.5 client calls are
rewritten and at least 85% resolve in 1.4.7's table, and the inspector hosts the jar as 1.4.7. Known gaps of the facade: `AddAchievementDesc`, `AddAllFuel` and `OpenGUI` have no lower-case
counterpart in the shim yet (they stay as they are and would be synthesized empty); a hook whose parameter list changed between 1.2.5 and 1.4.7 (for example `OnTickInGame(Minecraft)` against
`onTickInGame(float, Minecraft)`) is renamed but not adapted; `ModLoaderMP` (`BaseModMp`, packet 230) is not mapped at all.

### Offline link probe over the real corpus (2026-11-16)

`python -I tools/compat/probe.py <dir> --mc 1.2.5 --dry-run --no-history` (passes `-Pmappings125` to `checkLegacyMod`; `-Pmc125Unresolved=<file>` also appends every game member the join could not name).
91 archives, translated as one mod set, client builds loaded (server builds are left out by `Mc125Builds`, as on a client):

| | links | unresolved | not a mod / skipped |
|---|---|---|---|
| first run (detection fixed for the inherited-member case only) | 19 | 18 | 52 not-legacy |
| after: `BaseModMp` / Forge `NetworkMod` / in-jar base chains as entry points, FML-optional jars, API distributions skipped, client/server builds selected, the join tiers above | **30** | **8** | 31 not-legacy (API zips, patches, NEI plugins, wrappers of nested zips, 7 CraftBukkit builds), 21 server builds left out |

The 8 unresolved: NEWTON gravity (a 1.2.3 jar, not 1.2.5), SDKs Mods, herobrine's installer, and five jars that call the static `Profiler.startSection/endSection` (IC2, Somnia, Zeppelin, TwilightForest,
Tropicraft; static in 1.2.5, instance methods in the shim). "Links" includes calls to synthesized empty stubs: 1424 distinct stubbed members (1801 stubs) across the 38 translated client units,
ranked in `tools/dev/agent-tasks/1.2.5-shim-requests.md`; 300 distinct 1.2.5 game members had no 1.4.7 counterpart. `verifyFailures` is 0 for all.

**Largest open loader-side item: overridden hooks whose signature changed in 1.3.** A mod's override that has no 1.4.7 counterpart keeps an obfuscated name nothing calls, so the hook silently
never runs (the stub list shows only the calls, not these). The corpus has `Item.onItemUse(stack, player, world, x, y, z, side)` (1.4.7 adds the hit position floats), `Block.onBlockActivated`
(floats and the side), `Block.addCollidingBlockToList` (1.4.7: `List` and the colliding entity), `BlockContainer.getBlockEntity()` (1.4.7: `createNewTileEntity(World)`), `Block.onBlockPlacedBy`
(`updateMetadataOnPlaced` in Feather 1.2.5 names), `Slot.onItemRemoved`, `Container.onClickSlot`, `EntityItem.item`, ... The mechanism for this exists (`forward/*/bridge-methods.tsv`: a method with the
old shape gets a bridge with the new one), but it needs the join to rename the override first (an alias row whose descriptor may differ in arity) and one bridge row per hook; not done yet.

### First server self test (2026-11-16)

Setup: loader and runtime built with `-Pmappings125` (`./gradlew runServerSubTest -Pmappings125 -PautostopTicks=300`; the property puts the join table into the loader and sets
`-Dlegacyconnector.mc125=true` on the server), one archive in `run-subtest/mods/1.2.5/`, fresh world, config and translation cache each time. The chain works end to end (discovery as
"legacy 1.4.7 mod" with the 1.2.5 evidence, translation, construction); what stops each mod is the missing API, not the translation:

| Mod (build) | Result |
|---|---|
| BucketCraft 1.2.5 (client build, 0 stubs) | translated, constructed, then "Skipping the client-only legacy mod on a dedicated server (it touched client classes during construction)": correct for a client build |
| ElementalCreepers 2.3 server | translated; `NoClassDefFoundError: BaseModMp` (ModLoaderMP is not in the shim) |
| BlockHelper 0.8.3 server | translated; `NoClassDefFoundError: forge/NetworkMod` (Forge 3.x `forge/*` is not in the shim) |

No corpus mod loads on a server yet: every server build needs `BaseModMp`/`ModLoaderMp` or `forge/*` (46 of 59 entry classes extend `forge/NetworkMod`, 9 `BaseModMp`), the work of the shim worker.
Client builds only load on a client (their game classes fit the client names); a client run is the lead's.

### What was checked against primary sources on disk

- 1.4.7's `ModLoader.class` and `BaseMod.class` (`test-mods-mcarchive/1.4.7/ModLoader 1.4.7.zip`, read with `javap`): lower-case statics (`addName`, `registerBlock`, `addRecipe`, `addOverride`,
  `getUniqueSpriteIndex`, `setInGameHook`, ...) and lower-case `BaseMod` hooks with `getVersion()`, `load()`, `modsLoaded()`, `onTickInGame(float, Minecraft)`. No capitalised declaration.
- The Beta ModLoader jars (`test-mods-mcarchive/b1.7.3`) call a mix: `AddName`, `AddRecipe`, `RegisterBlock`, `SetInGameHook`, `OnTickInGame` capitalised next to `addOverride`,
  `getUniqueSpriteIndex` lower-case.
- **1.2.5 itself (2026-11-16, the real `ModLoader 1.2.5.zip`, `ModLoaderMP 1.2.5 v1.zip` (client and server), `forge-1.2.5-3.3.7.135-{client,server,src}.zip` and 87 mod units):**
  every static and hook is **lower case** and identical to the 1.4.7 spelling; no corpus jar calls a capitalised static. The facade rename above is therefore not needed for real 1.2.5 jars
  (it is kept for Beta-spelled ones). One bug of it was found on real data: Backpack, MagicWands, TooManyItems and ReiMinimap declare `getVersion()` and a helper `Version()`/`version()`, and renaming
  `Version` onto `getVersion` made a duplicate method; a hook is now renamed only when its class does not already declare the target. Full member tables (`BaseMod`, `ModLoader`, `MLProp`,
  `ModTextureStatic`, `ModTextureAnimation`, `EntityRendererProxy`, ModLoaderMP client and server, the server `ModLoader`) are in `tools/dev/agent-tasks/1.2.5-shim-requests.md`.
  Differences to the 1.6.4 shim's `ModLoader`: missing there are `addAchievementDesc`, `addAllFuel`, `addAllRenderers`, `dispenseEntity`, `loadImage`, `onTick`, `openGUI`, `populateChunk`, `receivePacket`,
  `registerAllKeys`, `registerAllTextureOverrides`, `removeBiome`, `renderBlockIsItemFull3D`, `renderInvBlock`, `renderWorldBlock`, `sendPacket`, `serverChat`, `serverConnect`, `serverDisconnect`; its
  `BaseMod` lacks `dispenseEntity`, `receiveChatPacket`, `receiveCustomPacket(Packet250CustomPayload)`, `serverConnect`, `serverDisconnect`.
- Client/server: the 1.2.5 server jars use the same `ModLoader`/`BaseMod` names with client types turned into `Object` and `BaseMod.onTickInGame(MinecraftServer)` (the server build's `ModLoader` ships inside
  `ModLoaderMP Server 1.2.5 v1.zip` and the Forge server zip). A fourth, non-notch kind of build exists: CraftBukkit/MCPC jars (`*_bukkit.zip`, `*-mcpc.jar`, `net/minecraft/server/mod_X`, MCP names), 7 of 100 units.

## Answers to the open questions (all measured on the corpus, 2026-11-16)

- **Capitalisation of the hooks and statics**: lower case (see above). Not a Beta-style mix.
- **The Forge 3.x package**: top-level `forge` (`forge/MinecraftForge`, ...), not `net.minecraft.src.forge`. Forge 3.3.x for 1.2.5 also bundles FML 2.2.48 (`cpw/mods/fml`).
- **Bundled ModLoader/Forge copies**: only the API distributions (ModLoader, ModLoaderMP server, Forge client/server, AudioMod, GuiAPI) carry them; no mod jar bundles its own `ModLoader`. The distributions
  themselves must be recognised and skipped (`Mc125Jars.isApiDistribution`, below), the way the Beta layer skips its ModLoader jars.
- **Base class edits**: 26 of 100 units contain a class with the name of a 1.2.5 game class, but only 4 are mostly such classes; 9 are pure patches (no `mod_*`), 8 are ordinary mods with 1 to 3 edited
  classes. Dropping the copies and reporting them (`Notch125To147`) keeps the 8 mods usable, a pure patch has nothing left to load.
- **Server jars**: separate obfuscation confirmed (`ModLoaderMP Server`, `forestry-server`, `industrialcraft-2-server`, ... fit the server table of the join and not the client one; 21 units); ModLoaderMP
  needs `BaseModMp`/`ModLoaderMp`/`Packet230ModLoader` from its own zip (not bundled by the mods, 11 units use it).
