# Dependency / API census (1.7.10 and 1.12.2 Modrinth probe)

Source: `probe-1710b.json` (698 jars: 511 links, 69 missing-dependency, 50 unresolved, 21 obsolete, 45 not legacy) and `probe-1122b.json`
(708 jars: 49 unresolved, 26 missing-dependency). Counts are jars. Updated by the dependency-bridge worker.

## What "unresolved" really was

The `missing` list of an unresolved record is `LinkReport.missingShimMembers/missingLibraryMembers`. In 57 of the 99 unresolved jars the only unresolved references were of this kind: the owner
class was **the mod's own class or an absent mod's API class**, not a bridged API: `OptionalStripper` removes `@Optional.Method` members and
`@Optional.Interface` interfaces for absent mods, but the guarded callers (behind `Loader.isModLoaded`, or themselves optional) still reference
them, and the link check counted every such reference as a defect. Examples: `BaublesHelper.getBaubles` (Draconic Evolution),
`ColytraUtil.wornElytraBauble`, `ModuleToolbox.isHolyToolbox`, `CompatHooksImpl.getInjectableBluePower`, `AgriAkkamaddiCrop.getBlock` (class lost its
absent-API interface), `IGasStack.getAmount` (CraftTweaker interface).

Fix (commit "Link check: references to @Optional.Method members / interfaces removed..."): such references are reported under "Optional
integrations not available" (`optional` array of the CheckJar record), not as unresolved. Re-probing the same jars as one set:
**1.7.10 unresolved 50 -> 18, 1.12.2 unresolved 49 -> 24** (the rest became `links`/`missing-dependency`).

Still unresolved after that (none is a Baubles/Waila/NEI/CoFH gap): LWJGL 3 vs LWJGL 2 calls (`org.lwjgl.opengl.GL13.nglClientActiveTexture`,
`ARBSync`, `PointerBuffer`, `ALC11` capture: 8 jars), JNA/ASM/log4j/Guava/JDK/`sun.misc.Unsafe`/mixin internals bundled or expected by the mod
(`com.sun.jna` 3, `org.objectweb.asm` 2, `org.spongepowered.asm` 3, `Unsafe.defineClass`, `Stream.toArray`), engine-patching mods
(`xonin.backhand.api`, `kpan.b_line_break`, `net.minecraft.server.integrated.IntegratedServer.<init>` LoTAS, `chylex.hee` HEE), IC2 forks
(`ic2.core.*` in ic2additions/InfiniStack: the IC2 *implementation*, not `ic2.api`), Xaero's `xaero.map/common` (version skew between jars).

## Bridged APIs: what the probe's "stubbed" list (synthesized, no behaviour) still shows

Stubbed members under the bridged packages, by jars (1.7.10): `codechicken.nei.*` GTNH-fork internals (NEIClientConfig 10, API 10: `addOption`,
`addSearchProvider`, `addItemVariant`, `registerBookmarkContainerHandler`; GuiRecipe, ItemPanel, BookmarkPanel, HandlerInfo, RecipeCatalysts: GUI
panels of the NEI window, no JEI equivalent), `baubles.api.expanded.*` (8 jars), `baubles.common.items.ItemRing` (3), `baubles.common.*`
sync/network (5), `BaubleType.UNIVERSAL` (3). 1.12.2: Waila internals `mcp.mobius.waila.config/overlay/utils` (3), Baubles `PacketHandler`,
`SlotBauble` (2), NEI `API.add*` (1).

| API (jars) | class | status |
|---|---|---|
| Baubles Expanded: `BaubleExpandedSlots`, `BaubleItemHelper`, `IBaubleExpanded`, `BaubleType.TRINKET/HEAD/BODY/CHARM/UNIVERSAL` (8) | (a) | **done**: Curios slots `curio/head/body/charm`, the Baubles view has 8 slots (0 amulet, 1-2 ring, 3 belt, 4 head, 5 body, 6 charm, 7 trinket), `slotsOf` maps type names/`BaubleType` to Curios slot tags and `canEquip` |
| `baubles.common.items.ItemRing` (3: addons extend it) | (a) | **done**: real `Item` subclass, ring bauble |
| `baubles.common.event.EventHandlerNetwork.syncBaubles`, `InventoryBaubles.blockEvents/readNBT/saveNBT` (3) | (a) | **done**: Curios syncs/persists itself |
| `NEIClientConfig.get/setBooleanSetting/IntSetting`, `logger` (10) | (a) | **done** (settings map, defaults) |
| NEI `API.addOption/addSearchProvider/addItemFilter/addSortOption/addLayoutStyle/addFastTransferExemptContainer`, `HandlerInfo`, `GuiRecipe`, `ItemPanel`, `BookmarkPanel` (GTNH NEI window) | (a), no JEI equivalent | open: stays synthesized (links, no behaviour) |
| `baubles.common.network.PacketHandler.INSTANCE`, `PacketSyncBauble` (5) | (a) | open: Baubles' own packets; Curios has its own sync |
| Waila 1.12 `config.FormattingConfig`, `overlay.DisplayUtil`, `utils.ModIdentification`, `cbcore.LangUtil` (3) | (a) | open: tooltip rendering internals of Waila addons; no Jade equivalent |
| `cofh.api.energy.*`, `buildcraft.api.*`, `ic2.api.*`, `thaumcraft.api`, `appeng.api` | - | no unresolved/stubbed members of the bridged energy APIs in either probe |

## Missing-dependency (69 + 26 jars): all but a few are hard content-mod dependencies

(c) the user must install the mod (jars listed by number of dependants; 1.7.10): simplecore 13 (SimpleOres family), fusion 10 (its addons),
Thaumcraft 16 (many version ranges), AE2 (appliedenergistics2) 5, Jinryu/Dragon Block C core 4+3, TerraFirmaCraft Plus 3, gregtech 3, Witchery 2,
Galacticraft Core/Mars 2 each, Forestry 1, Avaritia 1, ProjectE 1, Chisel 1, ImmersiveEngineering 1, Ars Magica's AnimationAPI 1.
1.12.2: llibrary 11 (Ice and Fire, Rats, Fossils, Lepidodendron, Jurassic Reborn, Spartan Fire), geckolib3 4, avaritia 2, slashblade 2, AE2 1,
projecte 1, libvulpes 1, eleccore 1, customnpcs 1, techreborn_compat 1, thaumcraft 1, biolib 1, teslacorelib 1 (`Industrial Foregoing`).
58 of 69 (1.7.10) and 22 of 26 (1.12.2) records say `linksWithoutDependency` (everything else links), so the missing library is the only gap.

(b)/(a) candidates among them: **CoFHCore** (4 jars, 1.7.10: Ars Magica 2, Elytra Mining, IronMan, RFDrills). The RF bridge covers
`cofh.api.energy` only; these mods declare `required-after:CoFHCore` but link without it (3 of 4). Not changed: marking CoFHCore as provided
would hide real gaps (cofh.core/cofh.lib are not bridged). **teslacorelib** (1.12.2) could be treated the same way once Tesla is bridged
to NeoForge energy.
