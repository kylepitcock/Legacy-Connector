# Legacy 1.2.5 corpus

The first measured look at real Minecraft 1.2.5 mods (2026-11-16), for [Legacy 1.2.5](Legacy-1.2.5.md). Input: the 91 archives of the MCArchive 1.2.5 category that could be downloaded
(`test-mods-mcarchive/1.2.5`, gitignored, never committed). Tool: `tools/compat/scan_125.py` (read-only: zips are opened, class files parsed, nothing is executed; the Beta counterpart is
`tools/dev/audit_b173_archive.py`, the 1.4.7 one `tools/compat/scan_147.py`). Data snapshot: `docs/data/legacy-1.2.5-corpus.json` (one record per unit). Re-run:
`python -I tools/compat/scan_125.py <dir> --md corpus.md --snapshot docs/data/legacy-1.2.5-corpus.json`.

An archive is split into **units** by install root (a zip with `SMP/` and `SSP/` folders, or a bundle of nested zips such as Smart Moving or MineFactory Reloaded, is several units): 91 archives are 100 units.

## What the corpus says

- **Pure ModLoader is the norm.** 72 units have a `mod_*` entry class (BaseMod subclass); 73 units call the `ModLoader` statics, **all in the lower-case spelling** (`registerBlock`, `addName`, `addRecipe`,
  `setInGameHook`, `getMinecraftInstance`, ...; no `AddName` anywhere). Beta's capitalised spelling does not occur in 1.2.5, so the capitalised facade in `Notch125To147` is not needed for real jars (it stays for
  Beta-spelled ones, but no longer renames a hook onto a method the class already declares: Backpack, TooManyItems, ReiMinimap and MagicWands declare `getVersion()` plus a helper `Version()`/`version()`;
  that rename produced a duplicate method).
- **Most used `ModLoader` statics** (units that call them, first 91-archive batch): getMinecraftInstance 28, registerBlock 22, registerTileEntity 21, setInGameHook 21, addName 19, addRecipe 18, getLogger 14,
  addLocalization 12, addOverride 12, getUniqueBlockModelID 12, registerEntityID 11, getPrivateValue 11, openGUI 8, addShapelessRecipe 8, addSpawn 6, addAllFuel 6, getMinecraftServerInstance 6 (server builds),
  addSmelting 5, setInGUIHook 5, isModLoaded 5. `openGUI` and `addAllFuel` are not in the 1.4.7 shim's `ModLoader` (see `tools/dev/agent-tasks/1.2.5-shim-requests.md`). Hooks declared by mods:
  `load`/`getVersion` (all), `onTickInGame` 15, `onTickInGUI` 6, `addRenderer` 5, `modsLoaded` 5, `registerAnimation` 3, `renderWorldBlock` 3, `keyboardEvent` 2, `addFuel` 2.
- **Early Forge (3.x) is the second pillar: 51 units**, in package **`forge`** (top level: `forge/MinecraftForge`, `forge/ITextureProvider`, `forge/Configuration`, `forge/NetworkMod`,
  `forge/IGuiHandler`, ...), not `net/minecraft/src/forge`. Forge 3.3.7.135 for 1.2.5 **bundles FML 2.2.48** (`cpw/mods/fml`: `@Mod` with `Mod$PreInit/Init/PostInit`, `ITickHandler`, `IWorldGenerator`,
  `FMLCommonHandler`, a reimplementation of ModLoader in `cpw.mods.fml.common.modloader`); only IC2 1.103, Forestry, PortalGun and LogisticsPipes (optional compat) touch it, all other jars are ModLoader plus
  `forge/*`. FML replaces the default-package `ModLoader`/`BaseMod` with its own classes of the same public API.
- **ModLoaderMP**: 11 units (Backpack, MagicWands, ElementalCreepers server, MorePistons, Smart Moving MP, SDKs, ...) use `BaseModMp` / `ModLoaderMp` (`sendPacket`, `registerNetClientHandlerEntity`,
  `initialize`, `registerGUI`, `getModInstance`) and `Packet230ModLoader`. The distribution is in the corpus (`ModLoaderMP 1.2.5 v1.zip` client, `ModLoaderMP Server 1.2.5 v1.zip` with the server `ModLoader`).
  The 1.2.5 `ModLoader` also has a packet-250 channel API (`registerPacketChannel`, `sendPacket`, `receivePacket`, `serverChat`); Forge `NetworkMod`/`IPacketHandler` is the third route (IC2, Forestry, BuildCraft
  3.1.5, Railcraft, MFR, Thaumcraft, EE2).
- **Client and server are separate builds**, paired by the `mod_X` name: 29 mod classes occur in more than one unit (mod_Backpack, mod_Railcraft, mod_IC2, mod_Forestry, mod_LogisticsPipes, mod_MagicWands,
  mod_ObsidianIngots, mod_NotEnoughItems, ...). 60 units are client builds, 21 server builds, 7 are **CraftBukkit/MCPC builds** (`*_bukkit.zip`, `*-mcpc.jar`, class
  `net/minecraft/server/mod_X`, MCP-style names `net/minecraft/server/World`: not notch-named, a third naming that `Mc125Jars` cannot and should not host). BuildCraft 3.1.5 ships as six client modules (core, builders,
  energy, factory, transport, silicon) and LogisticsPipes-client re-bundles the same `mod_BuildCraft*` classes.
- **Base class edits are a minority**: 26 of 100 units carry classes with the name of a 1.2.5 game class; only 4 are mostly replaced game classes (`jar-mod`: Checkpoints, NeoMelaphyre, Zeppelin guiControls,
  NEWTON gravity which is a 1.2.3 jar), 9 are patches without a `mod_*` class (AudioMod, GuiAPI, Smart Moving standalone, SinglePlayerCommands, moapi, Mystcraft's Forge patch), 8 are ordinary mods that ship one to three
  edited game classes (NEI and TooManyItems `GuiContainer`, Somnia, ReiMinimap, Zeppelin, MorePistons, Mystcraft). The ModLoader, ModLoaderMP and Forge zips are the five API distributions with bundled
  `ModLoader`/`BaseMod`/`MLProp`.
- **Bundled ModLoader/Forge copies**: only the API distributions have them. No mod jar carries its own `ModLoader`, so no "drop bundled ModLoader" step is needed for mods; the user-supplied API zips
  (ModLoader, ModLoaderMP, Forge, AudioMod, GuiAPI) must be recognised and skipped, as in Beta.
- **Detection**: 87 units fit 1.2.5 (at least 60% of their obfuscated member references resolve, and clearly better than in 1.4.7); the others are not notch-named (7 CraftBukkit/MCPC builds, libraries without
  game references: NEI plugins with too few references, WorldEdit, herobrine's installer) or **not 1.2.5 at all**: NEWTON gravity enabler (`1-2-3`) fits 1.2.5 at 41% (1.2.3 has its own obfuscation; unsupported).
  The 1.4.7 fit of every 1.2.5 unit is at most 6%. The first run of `Mc125Jars` on real jars measured 58% and 42% for Smart Moving: javac names the receiver class for inherited members
  (`EntityPlayer.posX`, declared in `Entity`), which the table (declaring class only) missed; the fingerprint now also accepts a member whose name and descriptor exist in the table when its owner is a game
  class (Smart Moving: 98% and 100%).

## Link probe after the second round (2026-11-16)

`probe.py --mc 1.2.5` over the 91 archives, nested wrappers and install roots expanded: 47 translated client/server units (of 100 archives' units; the other builds are left out by `Mc125Builds`, 30 are not
mods), **38 link and 9 do not** (first run: 19 / 18). 1411 distinct stubbed members (1776 stubs; first run 1424 / 1801; the later rounds moved the work from stubs to bridges), `verifyFailures` 0. Of the 9: NEWTON
(1.2.3), SDKs, herobrine, five BuildCraft 3.1.5 modules that clash with the BuildCraft classes LogisticsPipes-client re-bundles (an artifact of probing both as one set), IC2. 929 overrides of changed-signature
game methods in the translated units, all 929 bridged (see Legacy-1.2.5.md).

## Units

Status: `mod` (a `mod_*` mod), `mod-with-patches` (mod + a few edited game classes), `jar-mod` (mostly replaced game classes), `game-patch` (edited game classes, no mod class), `api-distribution` (ModLoader,
ModLoaderMP, Forge zips), `craftbukkit-build`, `library`. "fit" = share of obfuscated member references that resolve in the 1.2.5 tables of the unit's side / in the 1.4.7 table.

| units with a `mod_*` entry class | 72 |
| unit | status | side | classes | entry | APIs | replaced vanilla | bundled | 1.2.5 / 1.4.7 fit |
|---|---|---|---|---|---|---|---|---|
| AdvancedMachines_4.0_bukkit.zip | craftbukkit-build | craftbukkit | 14 | mod_IC2AdvancedMachines | ModLoader, Forge3, IC2 | 0 (0%) | - | - / - |
| AdvancedMachines_4.0_client.zip | mod | client | 17 | mod_IC2AdvancedMachines | ModLoader, Forge3, IC2 | 0 (0%) | - | 100% / 0% |
| AudioMod 1.2.5.zip | game-patch | client | 16 | - | AudioMod | 1 (6%) | - | 100% / 5% |
| Backpack_CLIENT_1-2-5.zip | mod | client | 5 | mod_Backpack | ModLoader, ModLoaderMP | 0 (0%) | - | 100% / 3% |
| Backpack_SERVER_1-2-5.zip | mod | server | 4 | mod_Backpack | ModLoader, ModLoaderMP | 0 (0%) | - | 100% / 0% |
| BlockHelper-1.2.5-0.8.3-client.jar | mod | client | 21 | mod_BlockHelper | ModLoader, Forge3, Packet250, BuildCraft, IC2 | 0 (0%) | - | 100% / 0% |
| BlockHelper-1.2.5-0.8.3-mcpc.jar | craftbukkit-build | craftbukkit | 18 | mod_BlockHelper | ModLoader, Forge3, BuildCraft, IC2 | 0 (0%) | - | - / - |
| BlockHelper-1.2.5-0.8.3-server.jar | mod | server | 19 | mod_BlockHelper | ModLoader, Forge3, BuildCraft, IC2 | 0 (0%) | - | 100% / 0% |
| BucketCraft_1-2-5.zip | mod | client | 12 | mod_BucketCraft | ModLoader | 0 (0%) | - | 100% / 0% |
| Checkpoints_v1-2-5.zip | jar-mod | client | 8 | - | - | 2 (25%) | - | 100% / 6% |
| CraftableSpawners_1-2-5.zip | mod | client | 3 | mod_CraftableMobSpawners | ModLoader | 0 (0%) | - | 100% / 0% |
| CustomRecipes_3-7_mc1-2-5.zip | mod | client | 2 | mod_CustomRecipes | ModLoader | 0 (0%) | - | 100% / 0% |
| EE2ClientV1.4.6.7.jar | mod | client | 192 | mod_EE | ModLoader, Forge3, Packet250, BuildCraft, IC2, NEI | 0 (0%) | - | 100% / 0% |
| EE2ServerV1.4.6.7.jar | mod | server | 158 | mod_EE | ModLoader, Forge3, BuildCraft, IC2 | 0 (0%) | - | 100% / 0% |
| ElementalCreepers_v2.3_mp_MC1.2.5.zip | mod | client | 23 | mod_ElementalCreepers | ModLoader | 0 (0%) | - | 100% / 0% |
| ElementalCreepers_v2.3_server_MC1.2.5.zip | mod | server | 21 | mod_ElementalCreepers | ModLoader, ModLoaderMP | 0 (0%) | - | 100% / 0% |
| ElementalCreepers_v2.3_sp_MC1.2.5.zip | mod | client | 23 | mod_ElementalCreepers | ModLoader | 0 (0%) | - | 100% / 0% |
| GOL_1-2-5.zip | mod | client | 5 | mod_Gol | ModLoader | 0 (0%) | - | 100% / 0% |
| GuiAPI-0.14.4-1.2.5.zip | game-patch | client | 645 | - | GuiAPI | 1 (0%) | - | 100% / 3% |
| InvTweaks-1.41b-1.2.4.zip | mod | client | 37 | mod_InvTweaks | ModLoader | 0 (0%) | - | 100% / 0% |
| LogisticsPipes-client-0.4.1.56.jar | mod | client | 477 | mod_BuildCraftBuilders, mod_BuildCraftCore | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| LogisticsPipes-server-0.4.1.56.jar | mod | server | 143 | mod_LogisticsPipes | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| MC 1.2.5 - Smart Moving 7.6.zip!SmartMoving Standalone Minecraft-ki-rk.zip | game-patch | client | 30 | - | - | 3 (10%) | - | 100% / 1% |
| MC 1.2.5 - Smart Moving 7.6.zip!SmartMoving Standalone hu-vq.zip | game-patch | client | 27 | - | PlayerAPI | 2 (7%) | - | 100% / 0% |
| MC 1.2.5 - Smart Moving 7.6.zip!SmartMoving for ModLoader.zip | mod | client | 35 | mod_SmartMoving | ModLoader, PlayerAPI | 0 (0%) | - | 98% / 0% |
| MC 1.2.5 - Smart Moving 7.6.zip!SmartMoving Client for ModLoaderMp.zip | mod | client | 35 | mod_SmartMovingMp | ModLoader, ModLoaderMP, PlayerAPI | 0 (0%) | - | 98% / 0% |
| MC 1.2.5 - Smart Moving 7.6.zip!SmartMoving Server for ModLoaderMp.zip | mod | server | 12 | mod_SmartMovingMp | ModLoader, ModLoaderMP, PlayerAPI | 0 (0%) | - | 100% / 0% |
| MagicWands_Client_1.2.5_v1.zip | mod | client | 8 | mod_MagicWands | ModLoader, ModLoaderMP | 0 (0%) | - | 100% / 0% |
| MagicWands_Server_1.2.5_v1.zip | mod | server | 9 | mod_MagicWands | ModLoader, ModLoaderMP | 0 (0%) | - | 100% / 0% |
| MineFactoryReloadedBC3.zip!MineFactoryReloaded_Client_1.5.3.zip | mod | client | 60 | mod_MineFactory, mod_MineFactoryCompat_Grass | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| MineFactoryReloadedBC3.zip!MineFactoryReloaded_Server_1.5.3.zip | mod | server | 57 | mod_MineFactory, mod_MineFactoryCompat_Grass | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 1% |
| ModLoader 1.2.5.zip | api-distribution | client | 19 | - | ModLoader, Packet250 | 13 (100%) | 6 | 100% / 1% |
| ModLoaderMP 1.2.5 v1.zip | api-distribution | client | 9 | - | ModLoader, ModLoaderMP, Packet250 | 3 (75%) | 5 | 100% / 0% |
| ModLoaderMP Server 1.2.5 v1.zip | api-distribution | server | 21 | - | ModLoader, ModLoaderMP, Packet250 | 12 (100%) | 9 | 100% / 0% |
| More Pistons v0.9.6.zip | mod-with-patches | client | 14 | mod_MorePistons | ModLoader, ModLoaderMP | 1 (7%) | - | 100% / 0% |
| NEI_BuildcraftPlugin 1.0.3.6.zip | library | client | 12 | - | ModLoader, Forge3, BuildCraft, NEI | 0 (0%) | - | 100% / 0% |
| NEI_ForestryPlugin 1.1.1.20.zip | library | client | 18 | - | ModLoader, Forge3, NEI, Forestry | 0 (0%) | - | 100% / 0% |
| NEI_IC2Plugin 1.0.3.11.zip | library | client | 16 | - | ModLoader, IC2, NEI | 0 (0%) | - | 100% / 0% |
| NEI_RailCraftPlugin 1.2.2.2_mistaqur.zip | library | client | 9 | - | Forge3, NEI, Forestry | 0 (0%) | - | 100% / 0% |
| NEI_RedPowerPlugin 1.3.zip | library | client | 15 | - | ModLoader, NEI | 0 (0%) | - | 100% / 0% |
| NEI_ThaumcraftPlugin 1.1.1.19.zip | library | unknown | 8 | - | NEI | 0 (0%) | - | 100% / 0% |
| NEI_ThermalExpansion_1.0.1.2.zip | library | unknown | 7 | - | NEI | 0 (0%) | - | 100% / 0% |
| NEWTON_gravity_enabler_1-2-3_v2.zip | jar-mod | client | 27 | mod_NEWTONge | ModLoader | 24 (89%) | - | 41% / 0% |
| NeoMelaphyre_1-2-5.zip | jar-mod | client | 2 | mod_Melaphyre | ModLoader | 1 (50%) | - | 100% / 0% |
| NotEnoughItems-Bukkit 1.2.1.zip | craftbukkit-build | craftbukkit | 11 | mod_NotEnoughItems | ModLoader, NEI | 0 (0%) | - | - / - |
| NotEnoughItems-Client 1.2.1.zip | mod-with-patches | client | 102 | mod_NotEnoughItems | ModLoader, Forge3, NEI | 1 (1%) | - | 100% / 0% |
| NotEnoughItems-Server 1.2.1.zip | mod | server | 11 | mod_NotEnoughItems | ModLoader, NEI | 0 (0%) | - | 100% / 0% |
| ObsidianHilt.zip | mod | client | 5 | mod_obsidianhilt | ModLoader | 0 (0%) | - | 100% / 0% |
| ObsidianIngots-1.2.5-3.1.5.zip `Obsidian Ingots 3.1.5/SMP or Multiplayer/ObsidianIngots/` | mod | server | 18 | mod_ObsidianIngots | ModLoader, Forge3 | 0 (0%) | - | 100% / 0% |
| ObsidianIngots-1.2.5-3.1.5.zip `Obsidian Ingots 3.1.5/SSP or Singleplayer/ObsidianIngots/` | mod | client | 21 | mod_ObsidianIngots | ModLoader, Forge3 | 0 (0%) | - | 100% / 0% |
| Railcraft_Client_5.0.0.zip | mod | client | 323 | mod_Railcraft | ModLoader, Forge3, BuildCraft, IC2 | 0 (0%) | - | 100% / 0% |
| Railcraft_Server_5.0.0.zip | mod | server | 277 | mod_Railcraft | ModLoader, Forge3, BuildCraft, IC2 | 0 (0%) | - | 100% / 1% |
| SDKs Mods 1.2.5 v1.zip `minecraft/` | mod | client | 131 | mod_SdkFlasher, mod_SdkFps | ModLoader, ModLoaderMP, Forge3 | 0 (0%) | 1 | 100% / 0% |
| SinglePlayerCommands-MC1.2.5_V3.2.2.jar!WorldEdit.jar | library | unknown | 468 | - | Bukkit/CraftBukkit | 0 (0%) | - | - / - |
| SinglePlayerCommands-MC1.2.5_V3.2.2.jar | game-patch | client | 59 | - | AudioMod | 1 (2%) | - | 100% / 0% |
| SinglePlayerCommands-MC1.2.5_V3.2.2.zip!WorldEdit.jar | library | unknown | 468 | - | Bukkit/CraftBukkit | 0 (0%) | - | - / - |
| SinglePlayerCommands-MC1.2.5_V3.2.2.zip | game-patch | client | 25 | - | AudioMod | 1 (4%) | - | 100% / 0% |
| SmokingChimney_1-2-5.zip | mod | client | 4 | mod_Chimney | ModLoader | 0 (0%) | - | 100% / 0% |
| SneakyPipes-0.1.1.zip | mod | client | 8 | mod_SneakyPipes | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| ThaumCraft-2.1.3.zip | mod | client | 336 | mod_ThaumCraft | ModLoader, Forge3, BuildCraft, IC2 | 0 (0%) | - | 100% / 0% |
| Timber_1.2.5.zip | mod | client | 2 | mod_Timber | ModLoader | 0 (0%) | - | 100% / 0% |
| TooManyItems2012_04_13_1.2.5.zip | mod-with-patches | client | 21 | mod_TooManyItems | ModLoader | 1 (5%) | - | 100% / 1% |
| Unofficial Tale of Kingdoms v1.3.0 - Updated by arkonick.zip | mod | client | 75 | mod_HeroesGuild | ModLoader | 0 (0%) | - | 100% / 1% |
| _1.2.5_ Somnia v23.zip | mod-with-patches | client | 8 | mod_Somnia | ModLoader, Forge3 | 1 (12%) | - | 100% / 0% |
| _1.2.5_ReiMinimap_v3.0_06.zip | mod-with-patches | client | 34 | mod_ReiMinimap | ModLoader | 1 (3%) | - | 100% / 1% |
| buildcraft-client-A-core-3.1.5.zip | mod | client | 155 | mod_BuildCraftCore | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| buildcraft-client-B-builders-3.1.5.zip | mod | client | 39 | mod_BuildCraftBuilders | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| buildcraft-client-B-energy-3.1.5.zip | mod | client | 38 | mod_BuildCraftEnergy | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| buildcraft-client-B-factory-3.1.5.zip | mod | client | 42 | mod_BuildCraftFactory | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| buildcraft-client-B-transport-3.1.5.zip | mod | client | 83 | mod_BuildCraftTransport | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| buildcraft-client-C-silicon-3.1.5.zip | mod | client | 9 | mod_BuildCraftSilicon | ModLoader, Forge3, BuildCraft | 0 (0%) | - | 100% / 0% |
| ee3-client-v0.1a.jar | mod | client | 44 | mod_EE3 | ModLoader, Forge3 | 0 (0%) | - | 100% / 0% |
| ee3-server-v0.1a.jar | mod | server | 43 | mod_EE3 | ModLoader, Forge3 | 0 (0%) | - | 100% / 0% |
| forestry-client-A-1.4.8.6_bc2.2.jar | mod | client | 760 | mod_Forestry | ModLoader, Forge3, BuildCraft, IC2, Forestry | 0 (0%) | - | 100% / 0% |
| forestry-server-A-1.4.8.6_bc2.2.jar | mod | server | 664 | mod_Forestry | ModLoader, Forge3, BuildCraft, IC2, Forestry | 0 (0%) | - | 100% / 0% |
| forge-1.2.5-3.3.7.135-client.zip | api-distribution | client | 274 | - | ModLoader, Forge3, AudioMod | 117 (62%) | 84 | 100% / 1% |
| forge-1.2.5-3.3.7.135-server.zip | api-distribution | server | 204 | - | ModLoader, Forge3 | 92 (66%) | 65 | 100% / 2% |
| herobrine2.3.zip!herobrine2.3/herobrine2.0/herobrine2/EasyModInstaller.jar | library | unknown | 11 | - | - | 0 (0%) | - | - / - |
| herobrine2.3.zip!herobrine2.3/herobrine2.0/herobrine2/lib/appframework-1.0.3.jar | library | unknown | 108 | - | - | 0 (0%) | - | - / - |
| herobrine2.3.zip!herobrine2.3/herobrine2.0/herobrine2/lib/swing-worker-1.1.jar | library | unknown | 13 | - | - | 0 (0%) | - | - / - |
| herobrine2.3.zip `herobrine2.3/herobrine2.0/herobrine2/into_minecraft.jar/` | mod | client | 27 | mod_sucker | ModLoader | 0 (0%) | - | 100% / 0% |
| industrialcraft-2-client_1.103.jar | mod | client | 377 | mod_IC2 | ModLoader, Forge3, AudioMod, BuildCraft, IC2 | 0 (0%) | - | 100% / 0% |
| industrialcraft-2-server_1.103.jar | mod | server | 338 | mod_IC2 | ModLoader, Forge3, BuildCraft, IC2 | 0 (0%) | - | 100% / 0% |
| moapi-1.2.5.zip | game-patch | client | 33 | - | - | 4 (12%) | - | 100% / 1% |
| mod_Zeppelin-client-1.2.5.0.31.zip | mod-with-patches | client | 34 | mod_Zeppelin | ModLoader | 1 (3%) | - | 100% / 0% |
| mod_Zeppelin-guiControls-1.2.5.0.31.zip | jar-mod | client | 1 | - | - | 1 (100%) | - | 100% / 0% |
| mod_zAdvancedSolarPanel_3_0_0_client.zip | mod | client | 12 | mod_zAdvancedSolarPanel | ModLoader, Forge3, IC2 | 0 (0%) | - | 100% / 0% |
| mod_zAdvancedSolarPanel_3_0_0_server.zip | mod | server | 10 | mod_zAdvancedSolarPanel | ModLoader, Forge3, IC2 | 0 (0%) | - | 100% / 3% |
| mod_zAdvanced_Solar_Panel_3_0_0_bukkit_r2.zip | craftbukkit-build | craftbukkit | 10 | mod_zAdvancedSolarPanel | ModLoader, Forge3, IC2 | 0 (0%) | - | - / - |
| mystcraft-client-1.2.5-0.9.01.0-Forge-3.3.8.152.zip | mod-with-patches | client | 300 | mod_Mystcraft | ModLoader, Forge3 | 6 (2%) | 1 | 100% / 1% |
| mystcraft-client-1.2.5-0.9.01.0.zip | game-patch | client | 305 | - | Forge3 | 10 (3%) | 5 | 100% / 1% |
| mystcraft-server-1.2.5-0.9.01.0-Forge-3.3.8.152.zip | mod-with-patches | server | 295 | mod_Mystcraft | ModLoader, Forge3 | 9 (3%) | 2 | 100% / 1% |
| mystcraft-server-1.2.5-0.9.01.0.zip | game-patch | server | 300 | - | Forge3 | 16 (5%) | 5 | 100% / 0% |
| portalgun1.2.5v3_bukkit.zip | craftbukkit-build | craftbukkit | 29 | mod_PortalGun | ModLoader, Forge3 | 0 (0%) | - | - / - |
| portalgun1.2.5v3_server.zip | mod | server | 29 | mod_PortalGun | ModLoader, Forge3 | 0 (0%) | - | 100% / 0% |
| thermalexpansion-1.0.2.1_bc2-mcpc-r1.zip | craftbukkit-build | craftbukkit | 139 | mod_ThermalExpansionCore, mod_ThermalExpansionEnergy | ModLoader, Forge3, Bukkit/CraftBukkit, BuildCraft, IC2 | 0 (0%) | - | - / - |
| thermalexpansion-1.0.2.1_bc2-mcpc-r2.zip | craftbukkit-build | craftbukkit | 139 | mod_ThermalExpansionCore, mod_ThermalExpansionEnergy | ModLoader, Forge3, Bukkit/CraftBukkit, BuildCraft, IC2 | 0 (0%) | - | - / - |
| tropicraftv3.0.zip | mod | client | 310 | mod_BeachFloatHandler, mod_EntMover | ModLoader, Forge3, AudioMod | 0 (0%) | - | 100% / 0% |
| twilightforest-1.10.1-server.zip | mod | server | 197 | mod_TwilightForest | ModLoader, Forge3 | 0 (0%) | - | 100% / 1% |
| twilightforest-1.10.1.zip | mod | client | 233 | mod_TwilightForest | ModLoader, Forge3 | 0 (0%) | - | 100% / 0% |
