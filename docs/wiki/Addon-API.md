# Addon API: writing addons for legacy mods

Legacy Connector runs unmodified Beta 1.7.3, 1.4.7, 1.5.2, 1.6.4, 1.7.10, 1.12.2 and 1.20.1 Forge mods on NeoForge 1.21.1. The **addon API**
lets other NeoForge 1.21.1 mods build on those legacy mods with modern code (see [Support per legacy version](#support-per-legacy-version)
for what works for which version). Examples of what an addon can do:

- give modern items and blocks (copper, deepslate, bee nests, blocks of other modern mods) Thaumcraft 4 aspects;
- expose a legacy tile entity (an essentia jar, an Iron Chest, a RotaryCraft machine) as a NeoForge capability
  (`IItemHandler`, `IFluidHandler`, `IEnergyStorage`) so modern pipes and Create can use it;
- call a legacy mod's own API with modern content, react to the legacy load phases (and to a legacy mod failing or missing a
  dependency) with modern code;
- look up the modern counterpart of a legacy fluid, bucket or creative tab, or ask which legacy version an object belongs to.

The API lives in the package `org.kylepitcock.legacyconnector.api` (and `api.event`). It is part of the normal Legacy Connector
jar. Addons are ordinary NeoForge mods that compile against the API and the NeoForge 1.21.1 types only.

## Stability promise

- `api` and `api.event` are public API. Signatures contain only JDK, Minecraft/NeoForge 1.21.1 and API types, never classes of the
  legacy shim. Legacy objects (a 1.7.10 `ItemStack`, a `TileEntity`, a `World`) are passed as `Object`.
- While the mod is 0.x, PATCH releases (0.3.x -> 0.3.y) never break the API; a MINOR release may, and the release notes say so.
  From 1.0.0 on it is semantic versioning. `LegacyApi.API_VERSION` is the API's own `major.minor` for additions.
- `api.internal` is not API. The legacy shim (`legacyconnector.v1_7_10`, `v1_6_4`, `v1_12_2`, `vb1_7_3`, `v1_20_1`) is not API either and changes
  freely: reach into legacy objects by their **original legacy names** through `LegacyAccess`, exactly as the legacy mod would.
- The unit test `ApiSignatureTest` fails the build when a public or protected signature of `api` / `api.event` mentions a shim or runtime type, and
  `LegacySupportTest` fails when the support matrix below differs from the one in the code. Additions only: API 0.3 added to 0.2 and changed nothing
  (`LegacyVersion.VB1_7_3`, `LegacyApi.versionOf` / `versionOfNamespace` / `failedMods`, `LegacyMod.missingDependencies` / `failedPhase` (default methods),
  `LegacyAddon.onLegacyModFailed` (default method), `LegacyModFailedEvent`, `LegacyFluids`, `LegacyTabs`, `LegacySupport`,
  `LegacyCapabilities.INVENTORY_ITEMS_B173`, Beta support in the conversions and numeric id lookups).

## Getting the API

Compile against the API jar, do not bundle it (at runtime it is inside the Legacy Connector jar):

```groovy
dependencies {
    // From this repository: ./gradlew apiJar  ->  build/dist/legacyconnector-api-<version>.jar (classes and sources)
    compileOnly files('libs/legacyconnector-api-<version>.jar')
    // A maven coordinate will be published with 1.0; placeholder:
    // compileOnly 'org.kylepitcock.legacyconnector:legacyconnector-api:<version>'
}
```

Declare the dependency in your `neoforge.mods.toml` so Legacy Connector loads first:

```toml
[[dependencies.my_addon]]
modId = "legacyconnector"
type = "required"
versionRange = "[<version>,)"
ordering = "AFTER"
side = "BOTH"
```

## The entry point and how addons are found

Implement `LegacyAddon` (public class, public no-argument constructor; every method has an empty default) and name it in your
`neoforge.mods.toml` with a mod property:

```toml
[modproperties.my_addon]
legacyconnector-addon = "com.example.MyAddon"      # or a list: ["com.example.A", "com.example.B"]
```

Legacy Connector reads the `legacyconnector-addon` property of every installed mod, so an addon needs no other registration. A
`META-INF/services/org.kylepitcock.legacyconnector.api.LegacyAddon` file is read as well. The class is created once. Addons run in
order of `priority()` (lower first), then by class name. `requiredLegacyMods()` lists legacy mod ids (for example
`List.of("thaumcraft")`): while one is missing or failed, the addon gets no callback at all, so it needs no `isLoaded` checks.
Exceptions in a callback are logged and do not stop the game or other addons.

## Lifecycle

```
 NeoForge registry events (modern mods register)
        |  last RegisterEvent
        v
 DISCOVERED        all legacy mods known, none constructed       onLegacyModsDiscovered / LegacyModsDiscoveredEvent
        |  legacy construction, preInit, [1.12.2 registry events], init, IMC, postInit, deferred registrations
        v
 REGISTERED        legacy content registered, registries open    onLegacyRegistered / LegacyRegistrationFinishedEvent
        |  RegisterCapabilitiesEvent                             registerCapabilities(LegacyCapabilities)
        |  FMLCommonSetupEvent, legacy loadComplete
        v
 LOAD_COMPLETE     registries frozen, main thread                onLegacyLoadComplete / LegacyLoadCompleteEvent
```

Each phase calls the addons first, then posts the event on every mod's event bus (`IModBusEvent`, so `modBus.addListener(...)`
works for mods that are not addons), then runs actions queued with `LegacyApi.runWhen(phase, action)`.

### The lifecycle per platform

Every platform goes through the same three phase boundaries: `LegacyLifecycle` calls `AddonHost.enter(phase)` once per game launch for all
legacy mods together (a pack with mods of several versions still gets one `DISCOVERED`, one `REGISTERED` and one `LOAD_COMPLETE`).

| Platform | `DISCOVERED` | Between `DISCOVERED` and `REGISTERED` | `REGISTERED` | `LOAD_COMPLETE` |
| --- | --- | --- | --- | --- |
| 1.7.10, 1.6.4, 1.5.2, 1.4.7 (one platform, the 1.7.10 one) | before any constructor | construction, preInit, init, IMC, postInit | after the deferred registrations (block items, recipes, fluids, creative tabs) | after the legacy `loadComplete`, in `FMLCommonSetupEvent` work |
| 1.12.2 (`Platform1122`) | same | also posts FML's `RegistryEvent.Register<T>` events after preInit | same | same |
| Beta 1.7.3 (`PlatformB173`) | same | the `BaseMod` constructors (they register content), then `ModsLoaded()` runs as the postInit step; Beta has no preInit or init step | same | same |
| Forge 1.20.1 (in place) | right after the last registry event, only the ids of the mods are known | none: the mods are NeoForge mods and registered through their own events already | immediately after `DISCOVERED` | same |

Facts that hold on every platform:

- With no legacy mod installed none of the phases runs: addon callbacks are never called, `LegacyApi.phase()` stays `NOT_STARTED` and
  actions queued with `runWhen` wait. (An addon that must react to "no legacy mods" checks `LegacyApi.hasLegacyMods()` in its own `FMLCommonSetupEvent`.)
- A legacy mod that fails keeps the phases going for the others. The failure is announced at once with `onLegacyModFailed(mod, phase)` and a
  `LegacyModFailedEvent` (mod bus), where `phase` is `construction`, `preInit`, `init`, `imc`, `postInit` or `loadComplete`.
  `LegacyMod.missingDependencies()` lists the required dependencies the loader found not installed ("X requires Y (range), which is not
  installed"), usually the reason; it is filled before any phase (it comes from the loader), so an addon can warn at `DISCOVERED`. A client-only
  legacy mod skipped on a dedicated server is not listed and not a failure. Failures of 1.20.1 mods are FML's (`failed()` is false for them).
- `registerCapabilities` is called while NeoForge fires `RegisterCapabilitiesEvent` (after `REGISTERED`, before `LOAD_COMPLETE`).

Which phase to use: feed modern content into a legacy mod's tables at **LOAD_COMPLETE** (everything legacy is complete, all modern
registry entries exist; Thaumcraft registers its own vanilla aspects in postInit, so earlier registrations could be overwritten).
Use REGISTERED only when the legacy mod must see your data before its own loadComplete.

## API reference

| Class | Purpose |
| --- | --- |
| `LegacyApi` | `mods()`, `mods(version)`, `failedMods()`, `mod(id)`, `isLoaded(id)`, `hasLegacyMods()`, `phase()`, `runWhen(phase, action)`, `classOf(modId, className)`, `versionOf(object)`, `versionOfNamespace(ns)`, `API_VERSION` ("0.3") |
| `LegacyMod` | `id()`, `legacyId()`, `name()`, `version()`, `minecraftVersion()`, `failed()`, `failureMessage()`, `classLoader()`, `classOf(name)`, `instance()`, `file()`, `missingDependencies()`, `failedPhase()` |
| `LegacyVersion` | `V1_4_7`, `V1_5_2`, `V1_6_4`, `V1_7_10`, `V1_12_2`, `VB1_7_3` (id `b1.7.3`), `V1_20_1`; `id()`, `family()` (1.4.7 to 1.6.4 share the 1.7.10 shim), `usesNumericIds()`, `isInPlace()` (1.20.1), `isBeta()` |
| `LegacyPhase` | `NOT_STARTED`, `DISCOVERED`, `REGISTERED`, `LOAD_COMPLETE` |
| `LegacyAddon` | the entry point (see above): `priority`, `requiredLegacyMods`, `onLegacyModsDiscovered`, `onLegacyRegistered`, `onLegacyLoadComplete`, `onLegacyModFailed`, `registerCapabilities` |
| `LegacyAccess` | reflection on legacy objects: `invokeStatic`, `invoke`, `newInstance`, `getStatic`, `getField`, `setField`, `setStatic`, `isInstance`, `hasMethod` |
| `LegacyItems` | `toLegacy(ItemStack, version)`, `fromLegacy(Object)` |
| `LegacyBlocks` | `legacyBlock`, `legacyMeta`, `legacyState` (1.12.2), `fromLegacy`, `fromLegacyState`, `legacyTile(BlockEntity)`, `legacyTile(Level, BlockPos)`, `tileVersion`, `versionOf` |
| `LegacyWorlds` | `toLegacy(Level, version)` |
| `LegacyContentHooks` | `itemKey(id or stack, version)` -> `LegacyItemKey(version, legacyName, damage, legacyItem)`, `legacyStack(id, count, version)`, `legacyBlockStack(id, count, version)`, `modernItemId(legacyStack)`, `numericBlockId` / `numericItemId` / `legacyBlockById` / `legacyItemById` (numeric-id versions and Beta 1.7.3) |
| `LegacyCapabilities` | `forTile(modId, className, capability, adapter)`, `forAnyTile(capability, adapter)`, adapter constants |
| `LegacyFluids` | `modernFluid(legacyFluid)`, `legacyFluid(modern, version)`, `fromLegacy(legacyFluidStack)`, `toLegacy(FluidStack, version)`, `bucketOf(legacyFluid)` (added in 0.3) |
| `LegacyTabs` | `modernTab(legacyTab)` -> creative tab id, `modernTabs(mod)` (added in 0.3) |
| `LegacySupport` | the support matrix as data: `of(Feature, LegacyVersion)` -> `Support(level, note)` with `Level` `WORKS` / `LIMITED` / `NO_OP` / `NOT_APPLICABLE` (added in 0.3) |
| `event.*` | `LegacyModsDiscoveredEvent`, `LegacyRegistrationFinishedEvent`, `LegacyLoadCompleteEvent`, `LegacyModFailedEvent` (mod, phase, message, missing dependencies) |

### Calling a legacy mod: `LegacyAccess`

You cannot name legacy classes at compile time (their signatures use the shim's legacy Minecraft types), so call them by name.
Methods are found by name and by the argument values (the first method whose parameters accept them, preferring exact types;
`null` matches any reference parameter; an `Integer` argument fits `int`, `long`, ...). Handles are cached. Varargs methods take
an explicit array. Failures throw `LegacyAccess.LegacyAccessException`; the called method's exception is its cause.

```java
Class<?> api = LegacyApi.classOf("thaumcraft", "thaumcraft.api.ThaumcraftApi").orElseThrow();
Object list = LegacyAccess.newInstance(LegacyApi.classOf("thaumcraft", "thaumcraft.api.aspects.AspectList").orElseThrow());
LegacyAccess.invoke(list, "add", LegacyAccess.getStatic(aspectClass, "METAL"), 5);
LegacyAccess.invokeStatic(api, "registerObjectTag",
        LegacyContentHooks.legacyStack(ResourceLocation.parse("minecraft:copper_ingot"), 1, LegacyVersion.V1_7_10).orElseThrow(), list);
```

### Conversions

A modern item that the legacy version did not have (copper ingot) converts to a legacy stand-in item with a stable identity: the
same modern item always gives the same legacy item and damage, so a legacy mod can key data by it. `LegacyContentHooks.itemKey`
returns that key (legacy name and damage) for modern ids; `legacyStack` gives the stack to pass to a legacy API.

Beta 1.7.3 stacks are the Beta shim's `ItemStack` (item id, count, damage); `toLegacy` gives `null` for an empty stack, as Beta did, and a modern item
Beta never had rides one reserved item so the stack survives a round trip. The block of a modern state is `Block.BLOCKS[id]` with the metadata from
`legacyMeta`.

For 1.7.10, `LegacyItems.toLegacy` returns a live view of the modern stack (changes to count or damage reach it) and `null` for an
empty stack, as 1.7.10 did. For 1.12.2 it returns a copy and `ItemStack.EMPTY`.

`LegacyBlocks.legacyTile(blockEntity)` returns the mod's own `TileEntity` instance for the block entity that hosts it (empty for
vanilla and modern block entities). `LegacyBlocks.versionOf(state)` tells which legacy version a block state belongs to.

### Fluids, buckets, creative tabs, versions

```java
Optional<Fluid> modern = LegacyFluids.modernFluid(legacyFluid);               // 1.4.7-1.7.10, 1.12.2, 1.20.1
Optional<ItemStack> bucket = LegacyFluids.bucketOf(legacyFluid);              // vanilla's for water and lava, else the mod's own
Optional<ResourceLocation> tab = LegacyTabs.modernTab(legacyCreativeTab);     // look it up in BuiltInRegistries.CREATIVE_MODE_TAB
Optional<LegacyVersion> v = LegacyApi.versionOf(itemStack);                   // which legacy version owns this stack, block, tile, class ...
```

Every legacy fluid has a modern fluid and fluid type (id `<owner mod>:<fluid name>`, vanilla water and lava stay vanilla). A 1.7.10-family bucket
is whatever the mod registered with `FluidContainerRegistry`; a 1.12.2 fluid without a bucket of its own gets a generated one. Tabs are
registered when legacy loading ends (the lookups return empty before). Beta 1.7.3 has no fluids and no tab objects: `LegacyFluids` answers empty
and `LegacyTabs.modernTabs(mod)` returns the one tab made for the mod. `versionOf` tells 1.4.7, 1.5.2 and 1.6.4 apart from 1.7.10 by the mod that
owns a registry id or class loader; for a plain shim object it can only answer the shim's family (1.7.10).

### Capabilities

All legacy tile entities of a version share one modern block entity type (`legacyconnector:legacy_tile_1_7_10`,
`legacyconnector:legacy_tile_1_12_2`). A rule therefore says: *if the hosted legacy tile is an instance of class X of mod M, answer
capability C with this adapter*:

```java
@Override
public void registerCapabilities(LegacyCapabilities caps) {
    caps.forTile("thaumcraft", "thaumcraft.common.tiles.TileJarFillable",
            Capabilities.FluidHandler.BLOCK, (tile, side, host) -> new JarView(tile));
}
```

`registerCapabilities` is called while NeoForge fires `RegisterCapabilitiesEvent`. Addon rules are asked before the built-in ones; an
adapter returning `null` passes to the next rule. The class is looked up lazily by name (class, superclass or interface); with a
missing mod or class the rule never matches. NeoForge caches capability lookups until `Level.invalidateCapabilities(pos)`: return a
handle that reads the legacy tile on each call instead of copying its state.

**Already built in** (do not register these again):

| Legacy | NeoForge capability | Constant |
| --- | --- | --- |
| 1.7.10 `IInventory`, `ISidedInventory` | `ItemHandler.BLOCK` | `LegacyCapabilities.INVENTORY_ITEMS` |
| 1.7.10 Forge `IFluidHandler` | `FluidHandler.BLOCK` | `LegacyCapabilities.FLUIDS_1710` |
| 1.7.10 CoFH `IEnergyReceiver`/`IEnergyProvider` (1 RF = 1 FE) | `EnergyStorage.BLOCK` | `LegacyCapabilities.RF_ENERGY` |
| Beta 1.7.3 `IInventory` tile (no sides) | `ItemHandler.BLOCK` | `LegacyCapabilities.INVENTORY_ITEMS_B173` |
| 1.12.2 tile answering Forge `ITEM_HANDLER_CAPABILITY` | `ItemHandler.BLOCK` | `LegacyCapabilities.FORGE_ITEMS_1122` |
| 1.12.2 tile answering Forge `CapabilityEnergy.ENERGY` | `EnergyStorage.BLOCK` | `LegacyCapabilities.FORGE_ENERGY_1122` |
| 1.12.2 tile answering Forge `FLUID_HANDLER_CAPABILITY` | `FluidHandler.BLOCK` | `LegacyCapabilities.FORGE_FLUIDS_1122` |
| 1.7.10 Forge `IFluidTank` tile (not an `IFluidHandler`) | `FluidHandler.BLOCK` (one tank) | built in |
| IC2 `IEnergySink` / `IEnergySource` tiles (`energy.fePerEu`, default 4 FE = 1 EU; `energy.fePerEu.in`/`.out` set one direction, 0 = off) | `EnergyStorage.BLOCK` | built in (reflection) |
| BuildCraft `IPowerReceptor` tiles (`energy.fePerMj`, default 10 FE = 1 MJ; `energy.fePerMj.in` = 0 turns it off; receive only) | `EnergyStorage.BLOCK` | built in (reflection) |

Legacy tile entities are exposed as capabilities **by default**, so modern hoppers, Create belts/funnels/arms/vaults, modern pipes and energy
cables work with them without an addon; addons only add what is missing. Side handling: NeoForge's `Direction` maps to `ForgeDirection`
by index (DOWN, UP, NORTH, SOUTH, WEST, EAST), a null side is `UNKNOWN`/all slots; `canInsertItem`/`canExtractItem`/`getAccessibleSlotsFromSide`
are honoured; simulation never mutates (NeoForge's wrappers copy stacks); `markDirty` is called after changes. IC2 sinks get at most one
packet of their tier's voltage per call (8 * 4^tier EU) so a machine is never overloaded; an IC2 tile is not double handled because IC2's own
cables and BuildCraft pipes are legacy blocks that never ask for the capability. Cached lookups (Create) are invalidated on every neighbour
change of a legacy tile block (`Level.invalidateCapabilities(pos)`), and NeoForge itself does it on placement and removal.

**Items** (by default): 1.7.10 `IEnergyContainerItem` (1 RF = 1 FE), IC2 `IElectricItem`/`ISpecialElectricItem` through `ElectricItem.manager`
(charge/discharge with the item's tier; `energy.fePerEu`) and `IFluidContainerItem` become `EnergyStorage.ITEM` / `FluidHandler.ITEM`;
1.12.2 items that attach capabilities in `Item.initCapabilities` become `ItemHandler.ITEM`/`FluidHandler.ITEM`/`EnergyStorage.ITEM`, looked
up again on every call on a view of the live stack (stacks are copied constantly, nothing is cached). Tools: legacy pickaxes, axes, spades,
hoes, swords and shears, or any item whose Forge `getToolClasses` names `pickaxe/axe/shovel/hoe/sword/shears`, answer the matching
`ItemAbilities` (`*_DIG`, `AXE_STRIP/SCRAPE/WAX_OFF`, `SHOVEL_FLATTEN`, `HOE_TILL`, `SHEARS_HARVEST`) in both versions. Already existing:
ore dictionary names as `c:` tags (`OreDictBridge`), burn time, crafting remainders, destroy speed, enchantment value.

Not done: a view of modern block entities as legacy `IInventory`/`IFluidHandler` (legacy pipes pushing into a Create vault or a modern
chest: legacy code asks `getTileEntity(...) instanceof IInventory`, which needs a TileEntity view per modern block entity), BuildCraft 7 MJ
batteries (only the 6.x `PowerHandler` API), extracting from BuildCraft engines, 1.12.2 fluid textures for the modern fluid types,
`FIRESTARTER_LIGHT` and attribute modifiers for legacy items. An addon can supply these with `forTile` + `LegacyAccess`.

## Support per legacy version

`LegacyMod.minecraftVersion()` tells the version of every mod. The legacy objects behind the `Object`s of the API depend on it:

- **1.4.7, 1.5.2, 1.6.4** run on the 1.7.10 shim, so their `ItemStack`, `Block`, `TileEntity` and `World` objects are the 1.7.10 shim's (`LegacyVersion.family()`
  is `V1_7_10`); what differs is the numeric block and item id space (one per version, `usesNumericIds()`).
- **1.7.10** is the reference: every feature below exists for it.
- **1.12.2** has its own shim (`IBlockState`, metadata through the block's own `getStateFromMeta`; an item with subtypes is one modern item per subtype, a
  fluid is a real modern fluid with a bucket).
- **Beta 1.7.3** has its own shim and platform (`legacyconnector.vb1_7_3`): numeric ids (blocks 0-255, items 256 and up) that the mods claim themselves,
  no capabilities, fluids or energy in the old game, so those rows say "n.a.".
- **1.20.1** mods are remapped in place, so "legacy" blocks, items, stacks, block entities and levels **are** the modern objects: the conversions return them
  unchanged, there is no metadata, and `legacyTile` is the block entity itself.

How to read the table: "yes" works; "limited" works with the restriction in brackets; "no-op" means the concept exists in that version but nothing is
provided built in (the call answers empty; an addon can add it with `forTile` and `LegacyAccess`); "n.a." means the version has no such concept. A call
for a feature that is "no-op" or "n.a." never throws: it returns `Optional.empty()`, an empty list or an empty stack. The same data is available in code
as `LegacySupport.of(feature, version)`. The table is generated from `LegacySupport` (a unit test fails when they differ; run it with the environment variable `API_DOCS_UPDATE=1`
to rewrite this block).

<!-- support-matrix:begin (generated from LegacySupport; run the test with API_DOCS_UPDATE=1 to rewrite) -->

| API feature | 1.4.7 | 1.5.2 | 1.6.4 | 1.7.10 | 1.12.2 | b1.7.3 | 1.20.1 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `LegacyApi.mods()` / `mod(id)` / `isLoaded` / `mods(version)` | yes | yes | yes | yes | yes | yes | limited (mods are plain NeoForge mods: `failed()` is false, `instance()` is empty) |
| Phases, addon callbacks, mod bus events, `runWhen` | yes | yes | yes | yes | yes | yes | limited (DISCOVERED and REGISTERED run back to back after the last registry event) |
| `onLegacyModFailed` / `LegacyModFailedEvent` / `failedPhase()` / `missingDependencies()` | yes | yes | yes | yes | yes | yes | n.a. (FML loads these mods; its own error screen reports them) |
| `LegacyApi.versionOf(object)` / `versionOfNamespace` | yes | yes | yes | yes | yes | yes | yes |
| `LegacyAccess` on mod objects | yes | yes | yes | yes | yes | yes | yes |
| `LegacyItems.toLegacy` / `fromLegacy` | yes (live view, null for empty) | yes (live view, null for empty) | yes (live view, null for empty) | yes (live view, null for empty) | yes (copy, EMPTY for empty) | yes (null for empty; a modern item Beta lacks rides one reserved item) | yes (the modern stack itself) |
| `LegacyContentHooks.itemKey` / `legacyStack` / `legacyBlockStack` / `modernItemId` | yes (stand-in for modern content) | yes (stand-in for modern content) | yes (stand-in for modern content) | yes (stand-in for modern content) | yes (stand-in item for modern content) | yes (name = modern id) | yes (key = registry id) |
| `LegacyBlocks.legacyBlock` / `legacyMeta` / `fromLegacy(block, meta)` / `versionOf` | yes | yes | yes | yes | yes | yes | limited (the block is the modern block; no metadata) |
| `LegacyBlocks.legacyState` / `fromLegacyState` | n.a. | n.a. | n.a. | n.a. | yes (`IBlockState`) | n.a. | yes (the state itself) |
| `LegacyBlocks.legacyTile` / `tileVersion` | yes | yes | yes | yes | yes | yes | yes (the block entity itself) |
| `LegacyWorlds.toLegacy` | yes | yes | yes | yes | yes | yes | yes (the level itself) |
| `numericBlockId` / `numericItemId` / `legacyBlockById` / `legacyItemById` | yes | yes | yes | n.a. | n.a. | yes (ids 0-255 / items 256+) | n.a. |
| `LegacyCapabilities.forTile` / `forAnyTile` (addon rules) | yes | yes | yes | yes | yes | yes | limited (only the block entity types of the 1.20.1 mods) |
| Built-in item handler capability of tiles | yes | yes | yes | yes | yes | yes (`IInventory`, no sides) | yes (native (Forge capability bridged)) |
| Built-in fluid handler capability of tiles | yes (ITankContainer) | yes (ITankContainer) | yes (ITankContainer) | yes (IFluidHandler / IFluidTank) | yes (Forge fluid handler) | n.a. (Beta has no fluids API) | yes (native (Forge capability bridged)) |
| Built-in energy capability: RF / Forge Energy | limited (only mods that use the RF API) | limited (only mods that use the RF API) | limited (only mods that use the RF API) | yes (1 RF = 1 FE) | yes (Forge Energy and Redstone Flux) | n.a. (Beta has no energy API) | yes (native (Forge capability bridged)) |
| Built-in energy capability: IC2 EU | no-op (not built in) | no-op (not built in) | no-op (not built in) | yes (`energy.fePerEu`) | no-op (not built in) | n.a. (Beta has no energy API) | no-op (the mods' own capabilities) |
| Built-in energy capability: BuildCraft MJ | no-op (not built in) | no-op (not built in) | no-op (not built in) | yes (receive only; `energy.fePerMj`) | no-op (not built in) | n.a. (Beta has no energy API) | no-op (the mods' own capabilities) |
| Built-in item/fluid/energy capabilities of items | limited (RF items, fluid containers) | limited (RF items, fluid containers) | limited (RF items, fluid containers) | yes (RF, IC2 EU, fluid containers) | yes (`initCapabilities`) | n.a. (no capability system) | yes (native (Forge capability bridged)) |
| `LegacyFluids.modernFluid` / `legacyFluid` / `fromLegacy` / `toLegacy` | yes | yes | yes | yes | yes | n.a. (Beta has no fluid registry) | yes (the fluid is modern already) |
| `LegacyFluids.bucketOf` | limited (buckets registered with the container registry) | limited (buckets registered with the container registry) | limited (buckets registered with the container registry) | limited (buckets registered with the container registry) | yes (also generated buckets) | n.a. (Beta has no fluid registry) | yes (the fluid's bucket item) |
| `LegacyTabs.modernTab` / `modernTabs` | yes | yes | yes | yes | yes | limited (one tab per mod, no tab objects) | yes (the tab itself) |

<!-- support-matrix:end -->

The matrix is checked against a mixed pack by the example addon `examples/version-probe` (see below).

Limits:

- 1.4.7, 1.5.2 and 1.6.4 have no separate shim classes, so `legacyBlock(state, V1_6_4)` returns the same kind of object as for 1.7.10; only the id tables differ.
  A stand-in for modern content takes the next free id of the version's table (a high id such as 31998 when the table is large), so it is not stable across runs.
- `numericItemId`/`numericBlockId` claim a free id for an object nobody claimed (as the legacy code would do on first use) on 1.4.7 to 1.6.4. On Beta they
  only read the id the mod's object has (Beta ids are fixed by the mods' own constants and `@MLProp` configuration).
- For 1.20.1 the item "damage" of a key is the stack's damage value, not a metadata value; `LegacyMod.instance()` is empty because FML does not keep NeoForge
  mod instances.
- Addon `forTile` rules for 1.20.1 apply only to block entity types registered by the translated 1.20.1 mods, not to other modern block entities.
- A modern item with no counterpart in Beta (copper ingot) is not an id Beta code can name: it travels as one reserved Beta item that keeps the modern stack, so
  `LegacyContentHooks.itemKey` gives the modern id as the name; a Beta mod's own tables cannot be keyed by it.

## Worked example: a 1.7.10 mod

A modern addon that gives modern content to the (made-up) 1.7.10 mod `examplemod`, which has the API class `examplemod.api.MachineApi` with
`static void registerFuel(ItemStack stack, int burnTime)` and a tank tile `examplemod.tile.TileTank`:

```java
public final class ExampleAddon implements LegacyAddon {
    @Override public List<String> requiredLegacyMods() { return List.of("examplemod"); }   // no callback while it is missing or failed

    @Override public void onLegacyLoadComplete() {                                          // everything legacy is complete here
        Class<?> api = LegacyApi.classOf("examplemod", "examplemod.api.MachineApi").orElseThrow();
        // a modern item as the 1.7.10 stack the mod's API wants (a stand-in item for content 1.7.10 never had: copper ingot)
        Object stack = LegacyContentHooks.legacyStack(ResourceLocation.parse("minecraft:copper_ingot"), 1, LegacyVersion.V1_7_10).orElseThrow();
        LegacyAccess.invokeStatic(api, "registerFuel", stack, 1600);
    }

    @Override public void registerCapabilities(LegacyCapabilities caps) {                   // while NeoForge fires RegisterCapabilitiesEvent
        // only the bottom side of the tank: wrap the built-in 1.7.10 fluid adapter
        caps.forTile("examplemod", "examplemod.tile.TileTank", Capabilities.FluidHandler.BLOCK, (tile, side, host) -> {
            IFluidHandler all = LegacyCapabilities.FLUIDS_1710.adapt(tile, side, host);
            return all == null || side != Direction.DOWN ? null : new DrainOnly(all);       // null: the next rule, finally the built-ins, answer
        });
    }

    @Override public void onLegacyModFailed(LegacyMod mod, String phase) {
        if (mod.id().equals("examplemod")) LOG.warn("examplemod failed in {}: {}", phase, mod.missingDependencies());
    }
}
```

Reading a legacy object back: `LegacyBlocks.legacyTile(level, pos)` gives the mod's `TileTank` instance, `LegacyAccess.invoke(tile, "getTank")` calls its
original method names. The real example is `examples/thaumcraft-modern-aspects` (Thaumcraft 4 aspects for copper, deepslate, ...).

## Worked example: a 1.12.2 mod

The same calls with `LegacyVersion.V1_12_2`. Differences an addon sees: states are `IBlockState`s, an empty stack is `ItemStack.EMPTY`, and tiles answer
capabilities themselves, which is already bridged (`FORGE_ITEMS_1122`, `FORGE_FLUIDS_1122`, `FORGE_ENERGY_1122`, `RF_ENERGY_1122`):

```java
BlockState modern = level.getBlockState(pos);
Object state = LegacyBlocks.legacyState(modern).orElse(null);                                // the mod's IBlockState (empty for blocks of other versions)
int meta = LegacyBlocks.legacyMeta(modern, LegacyVersion.V1_12_2).orElse(0);                // getMetaFromState of the block
Object lava = LegacyFluids.legacyFluid(Fluids.LAVA, LegacyVersion.V1_12_2).orElseThrow();  // the Forge Fluid "lava"
Optional<ItemStack> bucket = LegacyFluids.bucketOf(lava);                                    // its modern bucket

// feed a modern item to a 1.12.2 mod's API; the stand-in item is registered nowhere but has a stable name and damage
Object legacyStack = LegacyContentHooks.legacyStack(ResourceLocation.parse("minecraft:copper_ingot"), 1, LegacyVersion.V1_12_2).orElseThrow();
LegacyAccess.invokeStatic(LegacyApi.classOf("examplemod", "examplemod.api.MachineApi").orElseThrow(), "registerFuel", legacyStack, 1600);
```

A 1.12.2 block with metadata is one modern block with a `meta` property; an item with subtypes is one modern item per subtype (`mod:name`, `mod:name_1`, ...), and
`LegacyContentHooks.itemKey` returns the subtype's legacy name and damage. A 1.12.2 fluid is a real modern fluid with a bucket
(`LegacyFluids.bucketOf`), so modern pipes and tanks handle it.

## 1.4.7, 1.5.2 and 1.6.4

These versions run on the 1.7.10 shim with their own numeric id space per version. Use the 1.7.10 examples with `LegacyVersion.V1_6_4` (or 1.5.2, 1.4.7) so the
stand-in items and ids come from that version's table:

```java
OptionalInt id = LegacyContentHooks.numericItemId(legacyItemOrStack, LegacyVersion.V1_6_4);   // Item.itemID
Optional<Object> block = LegacyContentHooks.legacyBlockById(1234, LegacyVersion.V1_6_4);       // Block.blocksList[1234]
```

`LegacyApi.versionOf(modClass)` and `mods(version)` tell the three apart. Built in: inventories, `ITankContainer` liquids (as fluids), RF where a mod uses the
RF API; IC2 EU and BuildCraft MJ are not bridged for these versions (see the table). `LegacyAccess` uses the names the mod itself uses (the MCP names of its era
for Minecraft classes).

## Beta 1.7.3

Beta mods are ModLoader `BaseMod`s on their own platform. The API treats them as a full version with the old game's limits:

- `LegacyApi.mods(LegacyVersion.VB1_7_3)`, `mod(id)`, phases, `onLegacyModFailed` and the conversions (`LegacyItems`, `LegacyBlocks` with `Block.BLOCKS[id]` and 4-bit
  metadata, `legacyTile`, `LegacyWorlds`) work; `numericBlockId` / `numericItemId` / `legacyBlockById` / `legacyItemById` read the mod's fixed ids.
- Tiles that implement Beta's `IInventory` are `ItemHandler.BLOCK` capabilities by default (no sides in Beta); addon rules apply to all Beta tiles.
- There is no energy, no fluid registry, no capability system and no creative tab objects in the old game: `LegacyFluids` answers empty, `LegacySupport` says
  `NOT_APPLICABLE`, and `LegacyTabs.modernTabs(mod)` returns the single tab Legacy Connector makes per mod.
- `LegacyAccess` calls use Beta's MCP names (`World`, `ItemStack`, `Block`, `BlockEntity`) as the mod's own deobfuscated code does.

## Forge 1.20.1

Forge 1.20.1 mods are translated in place (class files remapped to the 1.21.1 names), so after translation they are ordinary NeoForge mods: their blocks, items,
block entities, levels and creative tabs are the modern objects, and the conversions return them unchanged. What an addon can do:

- everything a NeoForge mod can do to another NeoForge mod: events (`RegisterEvent`, `RegisterCapabilitiesEvent`, ...), registry lookups under the mod's namespace, mixins,
  data packs; the legacy API adds `LegacyApi.mods(V1_20_1)` / `mod(id)` / `versionOf`, the lifecycle phases (`DISCOVERED` and `REGISTERED` back to back, then
  `LOAD_COMPLETE`) and `forTile` rules on the mods' block entity types (the adapter's "tile" is the block entity);
- call the mod's classes by name with `LegacyAccess` (`classOf` uses the mod's own class loader; use the 1.21.1 names of Minecraft types because the classes were remapped);
- the mods' own capabilities (item, fluid, energy) are already NeoForge's: Forge `ForgeCapabilities` queries are bridged (`Forge1201Runtime`), no adapter is needed.

What it cannot: `failed()` is always false and there are no failure events (FML loads the mods and reports its own errors), `instance()` is empty (FML keeps no instance of a
NeoForge mod), `missingDependencies()` is empty, there is no legacy `meta` or `IBlockState`. Nothing is unreachable that needs new API: the objects are the modern ones.

## Data packs and assets of legacy mods

Legacy Connector generates data and assets for the legacy mods at load time; no API is needed to adjust them, an addon uses the normal modern mechanisms:

| What | Where | How to change it |
| --- | --- | --- |
| Recipes | data pack `legacyconnector_legacy_recipes` (below every other pack); ids `<mod>:<kind>/<output>` | a data file of the same id in your mod or a world data pack replaces it, a `neoforge:false` condition removes it; see [Recipes](Recipes.md) |
| Loot (structure chests, fishing, mob drops registered through the legacy API) and tags (ore dictionary as `c:` tags) | data pack `legacyconnector_legacy_data` (on top) | tags merge across packs: add your entries in your own tag file; for loot use global loot modifiers (`neoforge:loot_modifiers`) or ship a pack at `Pack.Position.TOP` from `AddPackFindersEvent` (an addon loads after Legacy Connector with `ordering = AFTER`, so its finder runs later); check with `/datapack list` |
| Models, blockstates, textures, lang | resource pack `legacyconnector_legacy_assets` (on top) | a resource pack of your own above it, or the usual client events as for any mod |
| Worldgen, spawns, biomes, structures | biome modifiers and placed features registered natively | add your own biome modifier or a removing one; see [World generation](World-Generation.md) |

The generated files are readable at runtime through the resource manager like any pack's; `-PassetsDump=<dir>` writes the generated assets of a test run to disk. Forge 1.20.1
mods ship their own data pack in their jar (translated), which loads like any NeoForge mod's.

## Example addon

`examples/version-probe` (a standalone build like the one below) logs per legacy mod and per API call what the API returns for every
version: lines start with `PROBE` (mod list with versions, stand-in item keys and round trips, numeric ids, items, blocks and tile
entities of every mod pushed through the conversions, the three lifecycle events, and a `PROBE SUMMARY` of ok/bad counters per version). Use it as
the smoke test of the API: put one mod per version in `run-subtest/mods/<version>/`, add `version-probe-1.0.0.jar` and run
`./gradlew runServerSubTest`; `grep PROBE run-subtest/logs/latest.log` should show no `BAD` or `FAILED`. It also logs the `LegacySupport` matrix (`PROBE support`), the `versionOf` check per sampled
item and block, and any failure announced through `onLegacyModFailed` (`PROBE failed`).

`examples/thaumcraft-modern-aspects` in this repository (a standalone Gradle build, not part of the release jar):

- registers Thaumcraft aspects for `copper_ingot`, `raw_iron`, `raw_copper`, `amethyst_shard`, `honeycomb`, `deepslate` and `bee_nest`
  in `onLegacyLoadComplete`, through `ThaumcraftApi.registerObjectTag`;
- exposes Thaumcraft's essentia jar (`TileJarFillable`) as a read-only `IFluidHandler`.

Build and install:

```
./gradlew apiJar                                   # in the repository root: build/dist/legacyconnector-api-<version>.jar (both examples pick it up; -PlegacyApiJar=<path> overrides)
cd examples/thaumcraft-modern-aspects
../../gradlew build                                # -> build/libs/thaumcraft-modern-aspects-1.0.0.jar
```

Copy the jar into the `mods` folder next to Legacy Connector, Thaumcraft 4.2.3.5 (1.7.10) and its dependencies (Baubles/Curios
bridge is automatic). Check the log for `Legacy addon example.tcmodern.ThaumcraftModernAspects` and
`Registered Thaumcraft aspects for 7 modern items/blocks`. In game, open a Thaumonomicon/Thaumometer: scanning (or `/give`n and
inspecting with the Thaumometer) a copper ingot should show Metallum and Permutatio.

## Limits and notes

- Server and client: addon callbacks run on both; capabilities and recipes are server logic.
- Legacy mods that fail to load are listed with `failed() == true`; `isLoaded` is false for them; `failedMods()`, `failedPhase()`, `missingDependencies()` and `onLegacyModFailed` say why.
- Names used with `LegacyAccess` are the legacy (deobfuscated MCP) names the mod itself uses (Beta 1.7.3: Beta's MCP names); do not use SRG or shim names.
- 1.7.10 (and 1.4.7 to 1.6.4) has no flattening: modern-only blocks and items are stand-ins whose damage is always 0; a modern block with block states
  reports one stand-in per block, not per state.
- No tile-entity events (placed/broken) are exposed yet; use NeoForge's own block events with `LegacyBlocks.legacyTile`.
