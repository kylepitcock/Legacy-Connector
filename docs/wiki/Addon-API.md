# Addon API: writing addons for legacy mods

Legacy Connector runs unmodified 1.7.10 and 1.12.2 Forge mods on NeoForge 1.21.1. The **addon API** lets other NeoForge 1.21.1
mods build on those legacy mods with modern code. Examples of what an addon can do:

- give modern items and blocks (copper, deepslate, bee nests, blocks of other modern mods) Thaumcraft 4 aspects;
- expose a legacy tile entity (an essentia jar, an Iron Chest, a RotaryCraft machine) as a NeoForge capability
  (`IItemHandler`, `IFluidHandler`, `IEnergyStorage`) so modern pipes and Create can use it;
- call a legacy mod's own API with modern content, react to the legacy load phases with modern code.

The API lives in the package `org.kylepitcock.legacyconnector.api` (and `api.event`). It is part of the normal Legacy Connector
jar. Addons are ordinary NeoForge mods that compile against the API and the NeoForge 1.21.1 types only.

## Stability promise

- `api` and `api.event` are public API. Signatures contain only JDK, Minecraft/NeoForge 1.21.1 and API types, never classes of the
  legacy shim. Legacy objects (a 1.7.10 `ItemStack`, a `TileEntity`, a `World`) are passed as `Object`.
- While the mod is 0.x, PATCH releases (0.3.x -> 0.3.y) never break the API; a MINOR release may, and the release notes say so.
  From 1.0.0 on it is semantic versioning. `LegacyApi.API_VERSION` is the API's own `major.minor` for additions.
- `api.internal` is not API. The legacy shim (`legacyconnector.v1_7_10`, `legacyconnector.v1_12_2`) is not API either and changes
  freely: reach into legacy objects by their **original legacy names** through `LegacyAccess`, exactly as the legacy mod would.

## Getting the API

Compile against the API jar, do not bundle it (at runtime it is inside the Legacy Connector jar):

```groovy
dependencies {
    // From this repository: ./gradlew apiJar  ->  build/dist/legacyconnector-api-<version>.jar (classes and sources)
    compileOnly files('libs/legacyconnector-api-0.3.6.jar')
    // A maven coordinate will be published with 1.0; placeholder:
    // compileOnly 'org.kylepitcock.legacyconnector:legacyconnector-api:0.3.6'
}
```

Declare the dependency in your `neoforge.mods.toml` so Legacy Connector loads first:

```toml
[[dependencies.my_addon]]
modId = "legacyconnector"
type = "required"
versionRange = "[0.3.6,)"
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

Which phase to use: feed modern content into a legacy mod's tables at **LOAD_COMPLETE** (everything legacy is complete, all modern
registry entries exist; Thaumcraft registers its own vanilla aspects in postInit, so earlier registrations could be overwritten).
Use REGISTERED only when the legacy mod must see your data before its own loadComplete.

## API reference

| Class | Purpose |
| --- | --- |
| `LegacyApi` | `mods()`, `mod(id)`, `isLoaded(id)`, `hasLegacyMods()`, `phase()`, `runWhen(phase, action)`, `classOf(modId, className)`, `API_VERSION` |
| `LegacyMod` | `id()`, `legacyId()`, `name()`, `version()`, `minecraftVersion()`, `failed()`, `failureMessage()`, `classLoader()`, `classOf(name)`, `instance()`, `file()` |
| `LegacyVersion` | `V1_7_10`, `V1_12_2` |
| `LegacyPhase` | `NOT_STARTED`, `DISCOVERED`, `REGISTERED`, `LOAD_COMPLETE` |
| `LegacyAddon` | the entry point (see above) |
| `LegacyAccess` | reflection on legacy objects: `invokeStatic`, `invoke`, `newInstance`, `getStatic`, `getField`, `setField`, `setStatic`, `isInstance`, `hasMethod` |
| `LegacyItems` | `toLegacy(ItemStack, version)`, `fromLegacy(Object)` |
| `LegacyBlocks` | `legacyBlock`, `legacyMeta`, `legacyState` (1.12.2), `fromLegacy`, `fromLegacyState`, `legacyTile(BlockEntity)`, `legacyTile(Level, BlockPos)`, `tileVersion`, `versionOf` |
| `LegacyWorlds` | `toLegacy(Level, version)` |
| `LegacyContentHooks` | `itemKey(id or stack, version)` -> `LegacyItemKey(version, legacyName, damage, legacyItem)`, `legacyStack(id, count, version)`, `legacyBlockStack(id, count, version)`, `modernItemId(legacyStack)` |
| `LegacyCapabilities` | `forTile(modId, className, capability, adapter)`, `forAnyTile(capability, adapter)`, adapter constants |
| `event.*` | `LegacyModsDiscoveredEvent`, `LegacyRegistrationFinishedEvent`, `LegacyLoadCompleteEvent` |

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

For 1.7.10, `LegacyItems.toLegacy` returns a live view of the modern stack (changes to count or damage reach it) and `null` for an
empty stack, as 1.7.10 did. For 1.12.2 it returns a copy and `ItemStack.EMPTY`.

`LegacyBlocks.legacyTile(blockEntity)` returns the mod's own `TileEntity` instance for the block entity that hosts it (empty for
vanilla and modern block entities). `LegacyBlocks.versionOf(state)` tells which legacy version a block state belongs to.

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
| 1.12.2 tile answering Forge `ITEM_HANDLER_CAPABILITY` | `ItemHandler.BLOCK` | `LegacyCapabilities.FORGE_ITEMS_1122` |
| 1.12.2 tile answering Forge `CapabilityEnergy.ENERGY` | `EnergyStorage.BLOCK` | `LegacyCapabilities.FORGE_ENERGY_1122` |
| 1.12.2 tile answering Forge `FLUID_HANDLER_CAPABILITY` | `FluidHandler.BLOCK` | `LegacyCapabilities.FORGE_FLUIDS_1122` |
| 1.7.10 Forge `IFluidTank` tile (not an `IFluidHandler`) | `FluidHandler.BLOCK` (one tank) | built in |
| IC2 `IEnergySink` / `IEnergySource` tiles (`energy.fePerEu`, default 4 FE = 1 EU) | `EnergyStorage.BLOCK` | built in (reflection) |
| BuildCraft `IPowerReceptor` tiles (`energy.fePerMj`, default 10 FE = 1 MJ; receive only) | `EnergyStorage.BLOCK` | built in (reflection) |

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

## Example addon

`examples/thaumcraft-modern-aspects` in this repository (a standalone Gradle build, not part of the release jar):

- registers Thaumcraft aspects for `copper_ingot`, `raw_iron`, `raw_copper`, `amethyst_shard`, `honeycomb`, `deepslate` and `bee_nest`
  in `onLegacyLoadComplete`, through `ThaumcraftApi.registerObjectTag`;
- exposes Thaumcraft's essentia jar (`TileJarFillable`) as a read-only `IFluidHandler`.

Build and install:

```
./gradlew apiJar                                   # in the repository root: build/dist/legacyconnector-api-<version>.jar
cd examples/thaumcraft-modern-aspects
../../gradlew build                                # -> build/libs/thaumcraft-modern-aspects-1.0.0.jar
```

Copy the jar into the `mods` folder next to Legacy Connector, Thaumcraft 4.2.3.5 (1.7.10) and its dependencies (Baubles/Curios
bridge is automatic). Check the log for `Legacy addon example.tcmodern.ThaumcraftModernAspects` and
`Registered Thaumcraft aspects for 7 modern items/blocks`. In game, open a Thaumonomicon/Thaumometer: scanning (or `/give`n and
inspecting with the Thaumometer) a copper ingot should show Metallum and Permutatio.

## Limits and notes

- Server and client: addon callbacks run on both; capabilities and recipes are server logic.
- Legacy mods that fail to load are listed with `failed() == true`; `isLoaded` is false for them.
- Names used with `LegacyAccess` are the legacy (deobfuscated MCP) names the mod itself uses; do not use SRG or shim names.
- 1.7.10 has no flattening: modern-only blocks and items are stand-ins whose damage is always 0; a modern block with block states
  reports one stand-in per block, not per state.
- No tile-entity events (placed/broken) are exposed yet; use NeoForge's own block events with `LegacyBlocks.legacyTile`.
