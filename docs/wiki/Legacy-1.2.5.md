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
capitalised calls (only Beta mods spell them so; 1.2.5 itself is lower case, see above) (`AddName`, `RegisterBlock`, `AddRecipe`, `AddShapelessRecipe`, `AddSmelting`, `SetInGameHook`, ...; set `Notch125To147.MODLOADER_STATICS`, 34 names) are rewritten to them, and
capitalised `BaseMod` hooks that a mod declares (`Load`, `ModsLoaded`, `OnTickInGame`, `GenerateSurface`, `Version` -> `getVersion`, ... `BASEMOD_HOOKS`) are renamed to the lower-case ones. A rename keeps
working for whichever spelling a 1.2.5 jar uses, since calls that are already lower-case pass through. Tests (`Notch125To147Test`, `Mc125JarsTest`): a synthetic `mod_*` class assembled in the test
registers a block, a name, two recipes and reads a field through the capitalised API; the calls end on `ModLoader.registerBlock`/`addName`/`addRecipe`/`addShapelessRecipe` and on 1.4.7 names; a
subclass of a game class has its override and inherited call renamed; every renamed static exists in the 1.4.7 shim source; with the real table (`-Pmappings125`) 60 real 1.2.5 client calls are
rewritten and at least 85% resolve in 1.4.7's table, and the inspector hosts the jar as 1.4.7. Known gaps of the facade: `AddAchievementDesc`, `AddAllFuel` and `OpenGUI` have no lower-case
counterpart in the shim yet (they stay as they are and would be synthesized empty); a hook whose parameter list changed between 1.2.5 and 1.4.7 (for example `OnTickInGame(Minecraft)` against
`onTickInGame(float, Minecraft)`) is renamed but not adapted; `ModLoaderMP` (`BaseModMp`, packet 230) is not mapped at all.

### Run time: ModLoader, ModLoaderMP and Forge 3.x in the shim (2026-11-16)

Measured on the real classes of the corpus (`ModLoader 1.2.5.zip`, `ModLoaderMP 1.2.5 v1.zip` and its server zip, `forge-1.2.5-3.3.7.135-client|server|src.zip`; Forge 3.3.7 ships FML 2.2.48 beside `forge/*`),
not on recollection. What the shim now has and what each piece does (source: `src/main/java/legacyconnector/v1_6_4/net/minecraft/src`, bridges in `org/.../v1_6_4/bridge`):

| Part | Real 1.2.5 API (measured) | In the shim |
|---|---|---|
| `ModLoader` statics | lower case, as in 1.4.7; extra over the 1.4.7 shim: `addAchievementDesc`, `addAllFuel`, `addAllRenderers`, `dispenseEntity`, `loadImage`, `onTick`, `openGUI`, `populateChunk`, `receivePacket`, `registerAllKeys`, `registerAllTextureOverrides`, `registerPacketChannel`, `removeBiome`, `renderBlockIsItemFull3D`, `renderInvBlock`, `renderWorldBlock`, `sendPacket`, `serverChat`, `serverConnect`, `serverDisconnect`, `registerTileEntity(Class, String, TileEntitySpecialRenderer)`, `addSpawn`/`removeSpawn` by entity name, `addAnimation(TextureFX)`, `DEBUG`, `VERSION`, `props` | all present. `openGUI` opens the screen for the local player; `getUniqueBlockModelID(mod, inv)` registers an `ISimpleBlockRenderingHandler` that calls the mod's `renderWorldBlock`/`renderInvBlock` (this also fixes 1.4.7 and 1.5.2 ModLoader block models); `registerPacketChannel`/`sendPacket` ride the payload channels (`Network164`); `addAchievementDesc` writes the language entries |
| `BaseMod` hooks | `addFuel`, `addRenderer`, `dispenseEntity`, `generateNether/Surface`, `keyboardEvent`, `onItemPickup`, `onTickInGame(float, Minecraft)`, `onTickInGUI`, `receiveChatPacket`, `receiveCustomPacket(Packet250CustomPayload)`, `registerAnimation`, `render*`, `serverConnect`, `serverDisconnect`, `takenFromCrafting/Furnace` | all declared; `addFuel` is a fuel handler, `generate*` world generators, ticks are tick handlers |
| `@MLProp` | static fields of the `mod_*` class, `name/info/min/max` | `core/MLPropConfig` reads `config/<class>.cfg` before `load()` (shared with Beta, which now delegates to it) |
| `ModTextureStatic`, `ModTextureAnimation` | `TextureFX` subclasses over a `BufferedImage` strip | classes in `net/minecraft/src`, played by `Animations147` |
| ModLoaderMP | `BaseModMp` (`getId`, `handlePacket(Packet230ModLoader)` client, `handlePacket(Packet230ModLoader, EntityPlayerMP)` server, `handleGUI`, `handleLogin`, `handleSendKey`), `ModLoaderMp` (`initialize`, `sendPacket`, `sendPacketTo`, `sendPacketToAll`, `registerGUI`, `getModInstance`, `sendKey`, chat helpers), `Packet230ModLoader` (modId, packetType, `dataInt/Float/Double/String`) | `BaseModMp`, `ModLoaderMp`, `Packet230ModLoader`; the packet travels as the payload of a custom payload on the channel `ModLoaderMP` (one registration per side, the mod found by its id); the loader names these classes (and `forge/*`) into `net/minecraft/src` (`Notch125To147.MODLOADER_MP`) |
| Forge 3.x (`forge/*`, no `net.minecraft.src` prefix in the jars) | `MinecraftForge` (about 60 statics), `MinecraftForgeClient`, `Configuration`/`Property`, `MessageManager`, `NetworkMod`, `ITextureProvider`, `IGuiHandler`, `IOreHandler`, `ISidedInventory`, `oredict.OreDictionary`, the handler interfaces, `ArmorProperties`, `EnumHelper`, `DimensionManager`, `ForgeHooks(Client)`, patched game classes (`EntityPlayer.openGui(BaseMod, ...)`, `Block.getHardness(int)`, `World.isBlockSolidOnSide(x, y, z, side)`, `FurnaceRecipes.addSmelting(id, meta, stack)`) | `net/minecraft/src/forge/*` in the shim, 50 classes: ore registrations are the shim's ore dictionary, crafting/fuel/pickup/interact/bonemeal/arrow/save handlers are its registries and events (`ForgeEvents`), GUI handlers the FML GUI registry (one `getGuiElement` for both sides), channels `Network164`, item renderers adapted to the shim's registry, `ISidedInventory` derived from the start/size pair, `Configuration` is the 3.x file format with block id allocation; `ITextureProvider.getTextureFile()` is `Block.getTextureFile()` which the sheet cutter already reads |
| Capabilities by default | IC2 1.103 `ic2.api.IEnergySink` (`demandsEnergy`, `injectEnergy(Direction, int)`), BuildCraft 3.1 `buildcraft.api.IPowerReceptor`/`PowerProvider`, `buildcraft.api.ILiquidContainer` | `ForeignEnergy1710` (`EarlyEu`, `EarlyMj`), `Liquids125` + `FluidBridge.forTile` |
| Members the join cannot name (`mcc125$<obfuscated>`) | font `b`, `Container.a(slot)`, slot click with a shift flag, chat line, explosions, `canPlace`, tooltips, profiler statics, `installResource`, ... | `Redirects125` through rows of `forward/1.4.7/member-manual.tsv` and `call-redirects.tsv` (about 15 members; the rest of the list is below) |

Not done (needs the loader to name the members first): the `mcc125$` members that mods **override** (`Block.onBlockActivated(world, x, y, z, player)`, `Item.onItemUse(stack, player, world, x, y, z, side)`, `BlockContainer.getBlockEntity()`,
`Block.onBlockRemoval`, `Slot.onCrafting`, `Container.slotClick(slot, button, shift, player)` as an override): the game calls the 1.3+ shape, so a 1.2.5 override is never called. The names are the **obfuscated** letters of the client or the server
(`mcc125$b` is a different method on the two sides), so a shim hook keyed by them would serve one side only; the loader should emit the Feather name (`mcc125$use`, `mcc125$getBlockEntity`) and the shim then adds one virtual adapter per hook
(`Block.onBlockActivated` calling the 1.2.5 shape when the class overrides it, the way `getHardness(int)` is already done).

Server self test (`-Pmappings125 -Dlegacyconnector.mc125=true`, `./gradlew runServerSubTest -PsubTestDir=run-sub125`, one jar in `mods/1.2.5/`, fresh config, world and cache): MagicWands server (a `BaseModMp`: 6 items, 6 recipes, 0 failed),
Backpack server (`BaseModMp`: 1 item, 17 recipes, 0 failed), Elemental Creepers server (15 entity types, ENTITY SELF TEST 15 alive, 0 failed). Everything else in this section is compiled and linked (`checkLegacyMod`) only.

### Run time, round 2 (2026-11-16; unit-tested, not yet run in a game)

- **ModLoader tick hooks** (`ModLoader.Ticks`, rules in `bridge/ModLoaderHooks125`): ModLoader 1.2.5's `onTick` ran each `setInGameHook`/`setInGUIHook` mod when the world time had moved since the last tick or when it was registered without `useClock`
  (so `useClock` hooks run once per world tick, not once per frame), and **removed a hook whose callback answered false** (a mod that never overrides `onTickInGame` therefore ran once). Both rules are implemented; the render tick of the layer's tick
  registry is the frame. The 1.2.5 server declares `BaseMod.onTickInGame(MinecraftServer)` as `void`, the shim as `boolean`: a mod's void override is a different method to the JVM, so the server tick finds it by name and invokes it by reflection (loader request in
  `1.2.5-loader-requests-from-shim.md` for a generic return-kind bridge). `ModLoader.onTick` (what the patched game class called) runs the same rules; nothing else calls it.
- **`populateChunk`**: the dispatch (overworld to `generateSurface`, Nether to `generateNether`, chunk coordinates `<< 4`) is one method shared with the world generator the layer registers for every ModLoader mod (placed through the same legacy world generator path as the 1.4.7 and FML mods, no post-generation
  pass); the public `populateChunk` additionally seeds ModLoader's own chunk random (world seed and chunk position, `chunkRandom`). `registerAllKeys` stays an identity: `registerKey` already lists the key and calls `keyboardEvent` through the FML key binding registry (press, or every tick while held with `allowRepeat`).
  `registerAllTextureOverrides` is covered by `addOverride`/`getUniqueSpriteIndex` (`SpriteSheets147`, the sheet cutter) and by `registerAnimation`, which runs when the mod loads.
- **Checked against the real classes** (`javap -p -c` of `ModLoader 1.2.5.zip`, `ModLoaderMP 1.2.5 v1.zip` client and server, and the readable Forge 3.3.7 source `forge-1.2.5-3.3.7.135-src.zip`):
  - Tick: clock is `World.getWorldTime` (`xd.w()`, 0 without a world); an in-game hook is skipped only when `clock == lastClock && useClock`; a false answer calls `Iterator.remove`; the GUI hooks skip a `useClock` hook only when the clock is unchanged AND a world exists (on the title screen they run every frame; fixed, it was once per tick); `clock` is stored after the GUI hooks; mod keys are handled once per clock change (`Keyboard.isKeyDown`, negative codes + 100 are mouse buttons, fire on press or while held with `allowRepeat`), also with a screen open; `registerAllTextureOverrides` clears the animation list, calls every mod's `registerAnimation`, uploads the animations and then the `ModTextureStatic` overrides, once per texture pack; `setIn*Hook` is `put`/`remove` into a map. The layer uses the total world time instead of the day time (a modern day time stops with `doDaylightCycle` off; 1.2.5's advanced every tick).
  - `populateChunk` (client and server): `r = new Random(seed); a = r.nextLong()/2*2+1; b = r.nextLong()/2*2+1; r.setSeed(chunkX*a + chunkZ*b ^ seed)`, one random for all mods, `generateSurface` when the provider's `toString` is `RandomLevelSource`, `generateNether` for `HellRandomLevelSource`, chunk coordinates `<< 4`; the unit test holds the resulting first `nextLong` for seed 42, chunk (3, -7).
  - Server `onTick`: clock = overworld `getWorldTime`, hooks run when changed or `!useClock`, the callback is `void` (nothing unregisters).
  - ModLoaderMP: `BaseModMp.getId()` is `toString().hashCode()` (the hash of "name version", not of the class name; fixed); `registerGUI` keeps the first mod and logs "RegisterGUI error: inventoryType already registered." (fixed); the client's `handleGUI` opens the screen with `ModLoader.openGUI` and copies the window id from the open-window packet to `player.openContainer` (the layer's menu bridge does that part); packet types 0 and 1 of the id `"ModLoaderMP".hashCode()` are the mod check and the tile entity packet (`dataInt = {x, y, z, type, modId, ints...}` to `handleTileEntityPacket`, not implemented), `"Spawn".hashCode()` custom entity spawns (not implemented).
  - Forge 3.3.7 (`ForgeHooks`): **`IPickupHandler` returns true to continue** (false stops the pickup; so does a stack used up) and **`IEntityInteractHandler` true = continue, false = cancel**; both were inverted in the first version and are fixed. `onMinecartInteract(cart, player, canceled)` gets the running AND of the earlier answers and the cart blocks the interaction when the result is false (a handler returns "the player may interact"); chat handlers chain the message (`onServerChat`, `onServerCommandSay`, `onClientChatRecv`: each gets the previous result, null cancels); `onChatCommand`/`onServerCommand`/hoe/bonemeal/special spawn: first true wins; sleep: first non-null; chunk-load handlers all fill the same set (one ticker, union forced). `ArmorProperties.StandardizeList` and `compareTo` (`AbsorbMax * 100 / AbsorbRatio`, int difference) are now Forge's exact algorithm in the shared armour record (the layer's version was a simplified one; `ArmorDistribution125Test`). Still different: Forge 3.3's `ApplyArmor` returns `int` and keeps `entity.carryoverDamage`; the layer's returns the float left and has no carry-over.
- **Hooks for overrides of unjoined members** (the loader emits them as `mcc125$<Feather name>` with the mod's own 1.2.5 descriptor; `Override125HooksTest` with synthetic subclasses): `Block.mcc125$updateMetadataOnPlaced(World, int, int, int, int side)` (called by `ItemBlock.placeBlockAt` after the block is set), `EntityAnimal.mcc125$makeChild(EntityAnimal)EntityAnimal` (reached from `createChild`), and the portal hooks `Teleporter.mcc125$onDimensionChanged/findNetherPortal/generateNetherPortal(World, Entity)` (Twilight Forest, Tropicraft): the layer's dimension travel calls `Teleporter.placeInPortal`, which runs 1.2.5's find, else generate and find again, with the forcer's world. Not run in a game.
- **Forge 3.x handlers** (`forge/ForgeEvents`, `ForgeClientEvents`): each registration subscribes to the shim's event of the same purpose, in the order of how many corpus units reference it: `ISpecialArmor` (4 units besides the Forge zips; now the shim's own `ISpecialArmor`, so the layer's armour path
  `ApplyArmor` applies a 3.x piece, `ArmorProperties` is a subclass of the shared record, the covariant redeclaration gives the bridge method; parameter types are `EntityLivingBase` because the loader renames `EntityLiving` in a 1.2.5 jar's signatures), `ISleepHandler` (3 units; `PlayerSleepInBedEvent.result`, the interface now returns `EnumStatus`),
  `ISoundHandler` (3: sound manager setup on `SoundLoadEvent`, entity sounds on `PlaySoundAtEntityEvent`; the `SoundPoolEntry` members have no modern counterpart), `IChatHandler` (server chat, player and console commands, `say`, client chat), `IMinecartHandler` (Railcraft, 2: update, collision, interact events), `IChunkLoadHandler`
  (`addActiveChunks` each world tick forces the named chunks and releases dropped ones, `bridge/ChunkAnchors125`; `canUnloadChunk`/`canUpdateEntity` have no native hook and are not called), `IDestroyToolHandler`, `IHoeHandler`, `ISpecialMobSpawnHandler`, `IHighlightHandler`, `IRenderWorldLastHandler`
  (the last three only Forge's own jars reference). `IPickupHandler`, `IEntityInteractHandler` (4 units each) were already events. Not mapped: `IMinecartCollisionHandler` installation (descriptor, see the loader requests), achievement pages (stored, listed in the one achievements screen).
- **Capabilities by default**: `bridge/SpecialInventory125` (`buildcraft.api.ISpecialInventory`, 15 corpus units bundle it: the machine's `addItem`/`extractItem` rules as a one-slot item handler) and `bridge/PipeEntry125` (`IPipeEntry`: items enter through `entityEntering` as a passive item) are returned by
  `BuiltinAdapters.inventory1710` before the generic inventory wrapper; the energy (IC2 1.103, BC 3.1 power) and liquid adapters were there.
- **ModLoaderMP**: `registerGUI(mod, id)` registers an FML GUI handler (server element: the container the server staged with `ModLoader.openGUI(player, id, inventory, container)`, client element: the mod's `handleGUI(id)`); the channel ends are plain methods (`receiveOnClient`, `receiveOnServer`) and a loopback test sends encoded
  packet 230 to the right mod by id. Server-build `BaseMod` shapes: `receiveCustomPacket(packet, player)` (payload channels), `receiveChatPacket(text, player)`, `serverDisconnect(player)`. Tests: `ModLoaderHooks125Test`, `ModLoaderMpLoopbackTest`, `ForgeHandlers125Test`, `ChunkAnchors125Test`, `SpecialInventory125Test`.

### Run time, round 3: classes with no 1.4.7 counterpart (2026-11-16)

The loader worker's list of classes corpus mods name (`LegacyChunkCache`, `LoadWorldChunkPacket`, `AddItemPacket`, `BossEntity`, `WorldSaveConflictScreen`, `MultiplayerWorld$BlockReset`, `CommandHandler`, `ChunkCache`,
`SaplingItem`, `LogItem`) counted every class that names them. Re-counted with the real Calamus/Feather names, the side taken from the jar and game-class copies excluded: only `BossEntity` (IC2 1.103 mining laser, Twilight
Forest hydra, client and server), `CommandSource` (Mystcraft, PortalGun, server) and `ChunkCache` (Tropicraft's `World` subclass) are named by retained mod code; the rest only by patched game classes (Forge, ModLoader(MP),
NEWTON, Mystcraft) that the loader drops, so they need nothing. `BossEntity` is `EntityDragonBase` in 1.2.5 (read with `javap` from the Mojang jar): the shim class `net/minecraft/src/EntityDragonBase` has its rules
(constructor sets `maxHealth` 100, `takeDamage(part, source, int)` forwards to `attackEntityFrom`, which refuses, `doTakeDamage` is the living entity's own handling; `EntityDragonBase125Test`). Mapping the 1.2.5 class onto it, and a class
tier by Calamus intermediary id (`CommandSource`, `ClientWorld`, `ServerChunkCache`, ... which Feather names differently in 1.4.7) are loader requests (`1.2.5-loader-requests-from-shim.md`, section 4). Not run in a game.

### Run time, round 4: ModLoaderMP tile entity packets and custom entity spawns (2026-11-16; unit-tested, not run in a game)

Read from the real classes (`javap -p -c` of `ModLoaderMP 1.2.5 v1.zip` client and `ModLoaderMP Server 1.2.5 v1.zip`: `ModLoaderMp`, `ISpawnable`, `NetClientHandlerEntity`, `EntityTrackerEntry2`, the patched `NetClientHandler` `adl` and `EntityTrackerEntry` `jj`):

- **Tile entity packet.** `ModLoaderMp.getTileEntityPacket(mod, x, y, z, type, ints, floats, strings)` builds a packet with the reserved id `"ModLoaderMP".hashCode()`, type 1, `dataInt = {mod id, x, y, z, type, ints...}`; the mod returns it from
  `getDescriptionPacket()` and `sendTileEntityPacket(tile)` sends `tile.getDescriptionPacket()` to everyone. The client's `handleAllPackets` hands type 1 to the mod whose `getId()` equals `dataInt[0]` (`handleTileEntityPacket(x, y, z, type, rest, floats, strings)`; fewer than 5 ints logs
  "Bad TileEntityPacket received."). In the layer the packet is a custom payload on the channel `ModLoaderMP`, which the block entity sync (`LegacyBlockEntity.descriptionTag`, the same path 1.6.4 tiles use) already carries in the update tag and delivers on the client, also for chunk-load tags.
- **Custom spawns.** `ISpawnable` is the interface of the mod's own entity (client build `spawn(Packet230ModLoader)`, server build `getSpawnPacket()`: both defaults in the shim). The server's `EntityTrackerEntry.getSpawnPacket` called `getSpawnPacket()` for an entity of a class registered with
  `registerEntityTrackerEntry(class, hasOwner, id)`, set the id to `"Spawn".hashCode()` and the type to `id` (`id - 256` above 127); the client's `handleAllPackets` looked the type up in `registerNetClientHandlerEntity` (stored folded the same way; ids over 255 and duplicates are refused with the real messages) and called `spawn` on a new entity.
  Here `ISpawnable extends IEntityAdditionalSpawnData`: the entity's native spawn packet (the layer's own entity-spawn path) carries the encoded packet, the client side reads it and calls `spawn`. Entities of unregistered classes, build without `getSpawnPacket` or null packets send nothing.
  `hasOwner` entries of entities that are not `ISpawnable` (the vehicle spawn packet carried the owner's id, the client stored the entity in the public `owner` field) go through `LegacySpawnExtras` (a new provider hook of `LegacySpawnData`): owner id (the entity's own when `owner` is null) on the server, the field set on the client, with the real error messages. `registerEntityTracker(class, range, interval)` sets the type's tracking
  range and update interval (`LegacyHosts.setTracking`, also after registration). Tests: `ModLoaderMpSpawnTileTest`. The owner path and the tracker settings are not unit-tested (they need a world and entity types).

### Run time, round 5: differences from the real ModLoader and Forge 3.x code (2026-11-16; unit-tested, not run in a game)

- **Server tick hook**: the loader's return-kind bridge (`return-bridges.tsv`) adds `onTickInGame(MinecraftServer)Z` next to a mod's `void` override, so the shim calls it directly; the reflection helper `ModLoaderHooks125.callServerTick` is gone.
- **`ArmorProperties.ApplyArmor`** (Forge 3.3.7 source): returns `int` and keeps the fraction in `entity.carryoverDamage` (`damage += carry; carry = (int) damage % 25; return (int) (damage / 25)`, in 1/25 heart units). The shared method returns the float left, where nothing is lost, so the layer's own damage path needs no carry;
  for a mod that calls the `...)I` method itself, `forge.ArmorProperties.mcc125$applyArmor` is that rule on top of the shared result (carry per entity). The loader's descriptor row still points `ApplyArmor(...)I` calls at the shared float method with `f2i`: pointing it at `mcc125$applyArmor` is a loader request (no corpus mod calls it, only Forge's dropped jars).
- **Mod keys with a screen open**: `ModLoader.onTick` read `Keyboard.isKeyDown` for every registered key whatever screen was open; the modern key mappings are released when a screen opens. `bridge/ModKeys125` polls the raw state (GLFW) of every key registered with `registerKey`, from the client tick that runs even for a mod with no tick hook, and fires `keyboardEvent` on a press (every poll while held with `allowRepeat`) when a screen is open; the layer's key handler fires without one, and a key still held when a screen opens is not a new press.
- **`registerAnimation` after a resource reload**: `registerAllTextureOverrides` cleared the animations and had every mod register again for each texture pack. `ModLoader.addAnimation` now records what mods add, and when the selected resource packs change the animations are dropped and every mod's `registerAnimation` runs again (a plain reload with the same packs keeps them: the animation looks its sprite up every tick, so it follows the re-stitched atlas).
- **`IChunkLoadHandler.canUnloadChunk`** (`ChunkProvider.dropChunk`: false keeps the chunk): no native veto exists, so a chunk is asked when it loads and again every 100 ticks while refused, and a refused chunk is kept with the forced chunks together with the `addActiveChunks` ones. **`canUpdateEntity`** (entities next to an unloaded chunk may still update) has no counterpart: entities tick in loaded chunks only; the layer logs that once when a handler registers.
- **`IMinecartCollisionHandler`**: the descriptor row for `EntityMinecart.setCollisionHandler(forge/IMinecartCollisionHandler)` landed in the loader (round 5); the 3.x interface has the four methods of the shared one and extends it (checked against the 3.3.7 source and `EntityMinecart` patch), so an installed handler is the one the minecart code consults.
- **Achievement pages**: `forge.AchievementPage` now extends the shim's own page class and `MinecraftForge.registerAchievementPage` registers it there too (duplicate names throw with Forge's message), so the pages show in the achievements screen with the cycle button; achievements in no page stay on "Minecraft".
- **Tracker settings** (`registerEntityTracker`) now apply to subclasses too, as `handleEntityTrackers`' `isInstance` did (a subclass with its own setting keeps it).
- **Chat redirects** from the loader (`GuiIngame.clearChat()`, `translate(String)`, `Redirects125.clearChat/translate`, rows in `member-manual.tsv`): `clearChat` empties the chat history (the real method cleared its two line lists); `translate` translates the key with the language table and adds the result as a chat line (the real body).

### Run time, round 6: capabilities by default for the 1.2.5-era APIs (2026-11-16; unit-tested on test doubles of the real interfaces, not run in a game)

Interfaces read from the corpus jars with `javap` (IC2 1.103's `ic2.api.*`, Forestry 1.4.8.6's bundled BuildCraft 3.2 `buildcraft.api.liquids.*`, BuildCraft 3.1.5 core, Railcraft 5.0.0). What a capability can and cannot represent:

| API | Exposed as | Notes |
|---|---|---|
| `ic2.api.IEnergySink` (existing) | energy storage, receive | the packet is now the tier of the block's output: an `IEnergyStorage` block takes `EnergyRates.packetForOutput(getOutput())` EU (BatBox 32, MFE 128, MFSU 512, never above 2048; a block without it keeps 32); a machine given a bigger packet explodes |
| `ic2.api.IEnergyStorage` (`getStored`, `getCapacity`, `getOutput`; batboxes, MFE, MFSU) | real stored and capacity in FE on the sink adapter; a storage block that is no sink is a read-only meter (`EarlyEuMeter`: no receive, no extract) | rates: `energy.fePerEu` (readouts), `energy.fePerEu.in` (receiving), as before |
| `IEnergySource`, `IEnergyConductor`, `IEnergyEmitter` | not exposed | a 1.103 source pushes into IC2's own energy net from its tick (`EnergyNet.emitEnergyFrom`), and the storage API has no setter, so nothing can be taken from it; a conductor is a net element with no energy of its own. Representing them needs the layer to join IC2's net as a tile, which would mean a generated `IEnergySink` per modern capability holder |
| `buildcraft.api.ILiquidContainer` (existing; BuildCraft 3.1, Railcraft 5.0, Thermal Expansion, Forestry's copy) | fluid handler, one tank | the capacity is now the tile's own when it declares one (`getCapacity()` in Forestry's copy, the sum of `getLiquidSlots()` capacities in BuildCraft 3.1.5's), never below what it holds; before it was a bucket more than the contents |
| `buildcraft.api.liquids.ITankContainer` + `ILiquidTank` (Forestry 1.4.8.6's bundled API; not Forge's later `net.minecraftforge.liquids.ITankContainer`) | fluid handler, one tank per `getTanks()` entry (`LiquidsBc32`) | fill and drain by side through `fill(Orientations, LiquidStack, boolean)` / `drain(Orientations, int, boolean)`; liquid ids map to fluids like the 1.5.2 liquids (`Liquids164`) |
| `buildcraft.api.IPipeConnection`, `ILegacyPipeConnection` | not exposed | connection predicates only, no items or energy to hand over |
| RedPower 2 | none in the corpus (only its NEI plugin) | nothing to adapt |

Railcraft 5.0.0 ships only `ILiquidContainer`, `ISpecialInventory` and IC2's API, which the existing adapters cover (its tanks are `TileUtilityLiquid`, an `ILiquidContainer`). Tests: `Capabilities125Test`.

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

**Overridden hooks whose signature changed in 1.3 (done 2026-11-16, second round).** A mod's override of a game method whose 1.4.7 version has another signature (`Item.onItemUse` gets the hit
position, `Block.onBlockActivated` the side and the hit position, `addCollidingBlockToList` a `List` and the entity, `BlockContainer.getBlockEntity()` becomes `createNewTileEntity(World)`,
`Slot.onItemRemoved(stack)` gets the player, ...) was never called by the host. Now generic, from the join data and not from a list: `genMappings125` writes a `B` row for every method of a
joined class that 1.4.7 has under the same Feather name but another descriptor and that no exact join claimed (client 72, server 57; `join-report.txt` lists them), choosing the candidate with the
best parameter alignment (`OverrideBridges.align`: in order, by type, dropped arguments ignored, arguments the new signature lacks passed as zero or null; refused when a primitive or the result
kind changed or more than half the parameters have no counterpart). `Notch125To147` looks for every method a mod class declares on top of such a row (the class's supertypes, jar classes first) and, after
the rename, adds a `public` method with the 1.4.7 signature that casts and forwards to the original (`OverrideBridges.emit`); the rest of the 1.4.7 chain then sees an ordinary 1.4.7 override. The
same covers methods the join found with other class types only (the descriptor fix for references applies to those through their declaring class). Calls of such a method from mod code keep the
old arity and stay stubs. Count over the real corpus (`Mc125CorpusBridgesTest`, `-Pmappings125`, writes `loader/build/reports/mc125-bridges.txt`): **929 overrides of changed-signature methods in
69 translated units, 929 bridged** (the bridges preserve primitives and result kinds, so none was refused). Not bridgeable because 1.4.7 has no method of that name (the mod keeps an override nothing calls,
the stub list names it `mcc125$<Feather name>`): `updateMetadataOnPlaced` 35, `makeChild` 31, `isPauseScreen` 28 (now an alias row to `shouldPauseGame`), `takeDamage` 2, `findNetherPortal` 3,
`generateNetherPortal` 3, `addRespawnParticles` 4, and single cases (`addChatMessage`, `clearChat`, `beforeRemove`, `onDimensionChanged`, `populateBlockLight`, `translate`, `fill`); each is a one-line row in
`manual-aliases.tsv` once the 1.4.7 name is known (`updateMetadataOnPlaced` is 1.4.7's `onBlockPlacedBy`/`onPlaced`, `makeChild` its `createChild`), not done because they change the argument list too.

**Round 3: calls, class aliases, the remainder (2026-11-16).** Calls of game methods whose arity changed (until now stubs) go through an adapter generated into the calling class
(`CallAdapt`, `OverrideBridges.emitCall`): the old arguments are matched to the 1.4.7 parameters by type, dropped ones ignored, new primitives passed as zero and new references as null, a `super.` call
through a private instance adapter with `invokespecial` (so it cannot recurse into the override bridge); calls of methods the mod class declares itself or the join names exactly are left alone. Over the corpus
(`Mc125CorpusBridgesTest`, -Pmappings125) 280 distinct changed-signature game methods are called and adapted, 6 not (reasons in `loader/build/reports/mc125-bridges.txt`). Probe: stubs 1776 to 1621, distinct
stubbed members 1411 to 1364, 38 linking / 9 unresolved unchanged, `verifyFailures` 0. The override bridges now refuse a hook whose old signature needs a reference the new one does not carry (a `World`
for `PortalForcer.findNetherPortal`): null would be dereferenced. `Creative/SurvivalInteractionManager` (7 and 6 jars) are class-aliased to `ClientPlayerInteractionManager`
(`manual-aliases.tsv`, `c` rows); the other classes without a 1.4.7 counterpart that corpus mods name are ranked in `tools/dev/agent-tasks/1.2.5-shim-requests.md` (round 3) for the run-time worker.
Overrides never called, 929 bridged and **85 never called**: `updateMetadataOnPlaced` 35 and `makeChild` 31 are evidence-checked in the requests file
(1.2.5 calls the first right after placement with the side, 1.4.7 has no hook with that shape, the second has no Feather name), the rest are single hooks without a 1.4.7 method found by call-site evidence
(callers of the 1.2.5 method mapped through the join to their 1.4.7 callers, `Mojang 1.2.5/1.4.7 client jars` read locally, nothing committed).

**Round 4: a wrong rename and an audit of the join (2026-11-16).** `ee.BlockEETorch.c(World,IIII)V` (EE2; the post-placement hook `Block.updateMetadataOnPlaced`) came out as `onBlockDestroyedByPlayer`.
It was not a join row: the join has only an unjoined (`U`) row for it, but the mod's DECLARATION kept the obfuscated letter `c`, and the later 1.4.7 notch to MCP stage renamed that letter to the 1.4.7 member
that happens to have the same letter and descriptor (a double rename; references to unjoined members had been fixed in round 1, declarations had not). Now a mod method that overrides an unjoined game member is
named `mcc125$<Feather name>` (supertypes walked; the shim's hook names): 85 overrides in the corpus, 85 renamed, 0 left (`Mc125CorpusBridgesTest`). Audit of the join itself: the only tier that can bind two members
with different Feather names is "same Calamus intermediary id"; 20 such joins existed, **15 were wrong** (an intermediary id reused for another member: `PlayerManager.unban` joined to `removeFromWhitelist`,
`ban` to `addToWhitelist`, `Snooper.report` to `init`, `HandshakePacket.key` to `username`, `EntityMap.server` to `world`, `ProjectileAttackGoal.projectileType` to `cooldown`, `handleLoadWorldChunk` to
`handleSoundEvent`, ...) and 5 are Feather renames (`Item.MAP` to `FILLED_MAP`, `World.wakeUpAllPlayers`, ...). Only the reviewed pairs (`reviewed-name-changes.tsv`) are joined now, the rest is listed in
`join-name-rejected.tsv`; `Mc125JoinAuditTest` fails on any accepted pair that is not in the reviewed list. Corpus archives that reference a wrong join: Smart Moving's standalone game patch, the Forge client and
server zips, the ModLoader/ModLoaderMP zips and Mystcraft's server patch (all of them game-class copies the layer drops or API distributions it skips); no content mod referenced one, so the earlier link counts stand.

**Round 5 (2026-11-16).** (1) Return-kind bridges: a mod override whose method has the right name and parameters but another return kind than the shim's (ModLoaderMP's server
`BaseMod.onTickInGame(MinecraftServer)` is `void`, the shim's `boolean`) gets a bridge with the shim's descriptor (`return-bridges.tsv`; void to boolean returns true, boolean to void is dropped,
numeric widening; anything else is refused); the same rule now applies to B rows. `descriptor-rewrites.tsv` rewrites calls whose descriptor names another type or result than the shim has
(`EntityMinecart.setCollisionHandler(forge/IMinecartCollisionHandler)`, `ArmorProperties.ApplyArmor` returning int with an F2I after the float call). (2) Detection: a jar whose obfuscation resembles
1.2.5 only partly (25 to 60% of its references, nothing else fits better; NEWTON gravity is a 1.2.3 mod) is reported as "Minecraft 1.2.x release other than 1.2.5 (not supported)" instead of being hosted;
`probe.py` and `CheckJar` strip a single class folder and split install roots (SDKs Mods: `minecraft/`), which turned SDKs and herobrine's mod class from "unresolved" into ordinary links. Repacks:
when two jars carry the same `mod_X` the one that is a pure module of the contested mods wins over a repack that also carries mods of its own (LogisticsPipes' client jar bundles older BuildCraft modules),
then the one with more classes, then the file name; the loser gives up the ids AND the classes the winner carries (`LegacyJar.droppedClasses`, in the cache key, reported in its notes). The five BuildCraft 3.1.5
modules link in the set and alone (`probe.py --only buildcraft`); LogisticsPipes links alone and in the set it fails on BuildCraft 3.1.5's API (`DefaultProps`...), a real version mismatch with the BuildCraft it
bundles. (3) The renamer knows the class hierarchy of every 1.2.5 jar of the set (`Notch125To147.registerSet`): a mod class extending a class of another jar that extends a game class (BuildCraft's tile entities
in its addons) is resolved through it; before, references through such a class kept their 1.2.5 letters and the 1.4.7 stage renamed them again (TileEntity `worldObj:I`). (4) Classes: the intermediary-id class tier
is audited with member overlap (at least half of the 1.2.5 class's members exist by name in the 1.4.7 class, all for classes of at most two members) and a reviewed list (`reviewed-class-pairs.tsv`, 30 pairs:
`MultiplayerWorld` to `ClientWorld`, `MapItem` to `FilledMapItem`, `PortalForcer`, `CommandSource`, the server console classes, ...; `RemoveEntityPacket` is rejected, its members do not match);
`Mc125JoinAuditTest` fails on a pair the members confirm that nobody reviewed. `BossEntity` maps onto the shim class `net/minecraft/src/EntityDragonBase` (an `s` row in `manual-aliases.tsv`, members by Feather name,
inherited ones through `MobEntity` via an `S` row), and a field retyped in 1.4.7 (`EnderDragonPart.dragon`: BossEntity, now `IEntityMultiPart`) gets a `checkcast` back to the old type after `getfield`. The Tropicraft
`ChunkCache` case (a `World` subclass passing a `World` where the shim takes a `WorldServer`) is a run-time class check; the offline probe's `verifyFailures` stays 0 and cannot see it, so it needs a game run.
Numbers: probe 43 linking / 3 unresolved (was 38 / 9), 388 distinct stubbed members (473 stubs) (was 1360 / 1606: most of the old count came from NEWTON, SDKs and herobrine, which were not 1.2.5 mods or were wrongly
classified); ranked list in `tools/dev/agent-tasks/1.2.5-missing-members.md` (a 32, b 276, c 80).

**Side-independent names for unjoined members.** The client and the server of 1.2.5 are obfuscated separately, so `mcc125$b` meant different members on the two sides. `genMappings125` now writes
a `U` row (side, owner, letter, descriptor, Feather name) for every member that stayed unjoined and the loader names it `mcc125$<Feather name>` (`mcc125$draw`, `mcc125$onClickSlot`, `mcc125$item`);
the shim worker's call-redirect rows in `forward/1.4.7/member-manual.tsv` were re-keyed accordingly (11 rows), so server builds hit them too.

**`.zip` in `mods/1.2.5/`.** The locator reads `.zip` and `.jar` from every legacy folder (`isModArchive`); a `BucketCraft_1-2-5.zip` in `mods/1.2.5/` was translated and constructed in the first server
self test (13:52 log). The report that zips are ignored came from the offline tools: `checkLegacyMod` takes only the paths it is given, and `probe.py` adds `*.zip` for the 1.2 to 1.6 era only. No locator defect found.

**Nested wrappers and install roots in the offline check.** `CheckJar` now expands an archive that only wraps mod archives (`LegacyModLocator.bundledMods`: Smart Moving, MineFactory Reloaded) and one with
several install roots that each hold a `mod_*` class (ObsidianIngots' `SMP` and `SSP` folders; one jar per root). The locator itself does not split install roots.

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
