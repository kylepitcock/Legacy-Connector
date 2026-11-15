# Coremod hook replay for 1.6.4, 1.5.2 and 1.4.7

Status (2026-11-15): the three lists are replayed through the 1.7.10 engine (`CoremodSites`); no new places were needed for the jars on the test machine.

## How it fits

- The pre-1.7 layers run on the 1.7.10 shim and platform (`LegacyMods.family` is "1.7.10"), so `CoremodSites.load()` already reads all
  `coremod-hooks-<mc>.txt` lists, `ready()` waits for their mods, `bind()` finds the hook class in the translated mod and `report()` counts them (one
  start-up line for all versions; the hook method is found by name and size when a parameter class was renamed by the forward layer).
- What was missing was the KEY: places are keyed by 1.7.10 class and method names; the lists of 1.4.7 name flat `net/minecraft/src/<Name>` classes and the
  ones of 1.5.2/1.6.4 name classes the forward tables rename. `loader/.../transform/PreSeventeenKeys` (called by `CoremodHookScan.write`) rewrites every
  Minecraft class in a site key and a hook descriptor through `ForwardRenames.map` (flat names, 1.5.2 and 1.6.4 class renames). Method names stay SRG
  ids as scanned: methods that exist in both versions have the same id (`EntityPlayer.func_71065_l` is the same in 1.6.4, 1.5.2, 1.7.10); a renumbered method
  is reported as "no place yet", never mis-replayed. `TRANSFORMER_VERSION` 67 rewrites the lists.
- `CoremodSites` itself is untouched.

## Hooks found (jars of `test-mods-164/-152/-147`, vanilla client jars of the exact official files)

`CoremodHookProbe` (`./gradlew probeCoremodHooks -Psites=true -PvanillaJar=client-<mc>.jar -PlegacyJar=...`) and a server run with the jars.
Coremod jars present on the machine: CCC (all three), NEI (all three), OpenModsLib/OpenBlocks, Resonant Engine, Universal Electricity, TreeCapitator,
IC2 (1.6.4), TConstruct (1.6.4), AE rv14, Evoc, PowerCrystalsCore, immibis core/microblocks (1.5.2/1.4.7), CustomPlayerModels, BufferFix, MiscPeripherals, Xcraft.

| MC | Transformer | Site (1.7.10 names) | Hook | Place |
| --- | --- | --- | --- | --- |
| 1.6.4, 1.5.2 | OpenBlocks `OpenBlocksClassTransformer` | `EntityPlayer.func_71065_l()Z#after:aload 0` | `openblocks.Hooks.isInBed(EntityPlayer)Z` | `CoremodSites.IN_BED` (replayed) |
| 1.6.4 | NEI `NEITransformer` | `Block.<init>(ILMaterial;)V#replace:<init>()V` | `IDConflictReporter.blockConstructed(Block,I)` | none: reports block id clashes, the id facade has none |
| 1.4.7 | Xcraft `OreTransformer` | `Block.func_71893_a(...)V#after:aload 2` and `#tail` | `OreHarvest.begin/end` | none: a vein-mining mod's own bracket around breaking, no modern place |
| 1.4.7, 1.5.2, 1.6.4 | CustomPlayerModels (client) | `RenderPlayer`, `EntityRenderer`, `TileEntitySkullRenderer`, `GuiScreen` GL and screen calls | `CPMASMClientHooks.*` | none (client rendering of a skin mod) |
| 1.6.4 | Evoc, PowerCrystalsCore, Resonant, OpenModsLib `MapGenStructure`, NEI GUI classes | inline rewrites, added methods, no static hook call | - | nothing to replay (structure list only) |

No hook of CodeChickenCore, TreeCapitator, BufferFix (1.4.7 `TcpConnection`: inline) or immibis was a static call.

## Server run (1.6.4 + 1.5.2 + 1.4.7 jars, port 25594)

"Legacy coremods put 22 hook(s) into their Minecraft's classes; 1 hook method(s) are replayed at 1 place(s) of the modern game, 19 place(s) have no equivalent
yet"; "A coremod hook failed": 0. (22 counts a hook once per version list; the mixed 1.4.7/1.5.2/1.6.4 test directory shows each version's transformers run
over every pre-1.7 vanilla jar, since one coremod class loader serves the three versions: a 1.4.7 mod's hooks are listed under 1.5.2 and 1.6.4 as well.
Unrealistic for a real pack, which has one Minecraft version.)

## Left

- A scan that restricts each list to the transformers of that version's jars.
- Places for pre-1.7 only methods (`Block.<init>` with the id, `World.getBlockId`...) if a mod's hook there ever has a modern meaning.
- IC2, TConstruct and AE rv14 (1.6.4) listed in the probe but their transformers changed none of the vanilla classes there (not tried on a server run).
