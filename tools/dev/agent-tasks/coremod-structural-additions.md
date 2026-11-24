# Coremod structural additions (interfaces, fields, methods a coremod adds to a Minecraft class)

Status: recording half done (`CoremodHookDiscovery.additions`, `CoremodHookScan.writeStructure`, list `.legacyconnector/cache/coremod-structure-<mc>.txt`,
`kind TAB class TAB name TAB desc TAB access` per line). Applying is done for the common shape (2026-11-15): an added interface owned by a mod jar, with the added instance methods, becomes a generated
mixin on the shim class (`CoremodAdditionMixins`, config `legacyconnector.legacy.<jar>.coremod-additions.json`, let through by `LegacyMixinPlugin`); added
instance fields are kept in `CoremodState` per game object. Verified on a 1.7.10 server: Factorization `Chunk`/`Entity` and Logistics Pipes
`ItemStack`/`TileEntity` mixins apply, the `IExtraChunkData` ClassCastException is gone.

Round 2 (2026-11-15, TRANSFORMER_VERSION 69), per shape, with the local jars as evidence (probe: `./gradlew probeCoremodHooks`, which now marks added static
members; 1.7.10 set = SpaceCore, 1Legends, ChromatiCraft, CoFHCore, DragonAPI, Fisk, Galacticraft, GalaxySpace, RotaryCraft, Thaumcraft, Betweenlands, Anti-Gravity,
Factorization, Logistics Pipes, OpenBlocks, OpenModsLib, EnderCore, CodeChickenCore; 1.12.2 set = AppleCore, AutoRegLib, Charm, Quark, RandomThings):

| shape | local example | state |
|---|---|---|
| interface of a mod jar + instance methods + instance fields | Factorization, Logistics Pipes (1.7.10); AppleCore `FoodStats`/`BlockCake` (1.12.2) | done. 1.12.2: `IAppleCoreFoodStats` on `FoodStats`, `IEdible`/`IEdibleBlock` on `BlockCake` are generated and applied (checked with a server dev script: `getInterfaces()` of the shim classes), no ClassCast/NoSuchMethod/AbstractMethod, self-test lines identical to the run without the change |
| instance methods without an interface | Factorization `BlockRailBase.func_149743_a`, Fisk `BlockButton/BlockLiquid/BlockLeavesBase/BlockBasePressurePlate.func_149743_a`, `BlockBed.func_149668_a`, `EntityLivingBase.func_145780_a`, ChromatiCraft `getLightOpacity` on `BlockDirt/Grass/Liquid`, DragonAPI `func_149673_e`/`getSeed`/furnace `_redirect`, CoFH `cofh_collideCheck` (`Entity`), `cofh_validate` (`TileEntity`), `cofh_canEnchantApply` (`Item`) | done: the same generated mixin, no interface; applied for the transformer's own jar only (the bodies call that jar's hook classes), skipped when the shim class is missing or declares a method of that name (the shim's implementation wins), and not when the change also adds an interface (that interface's owner generates the whole class). The translator renames the declared SRG method like any override (`func_149743_a` is `addCollisionBoxesToList` in the generated class; applied: `BlockRailBase` declares it at run time) |
| added static fields / static methods | none (the probe lists no added static member in any local 1.7.10 or 1.12.2 coremod; `~ field ... access 25 -> 9` entries are final-removals) | no example, not built. Plan if one shows up: fields in a per-class holder in `CoremodState` (key = class name), static methods as static methods of the mixin (a mixin may merge static methods into the shim class); calls from mod code to a static method the shim lacks already link through the synthesized `$$Missing` class and would need `ShimLinker` to redirect them to the generated method |
| field initialised in an inserted constructor store | Betweenlands `MinecraftServer.sleepPerTick = 50` (plain constant), AppleCore `FoodStats(EntityPlayer)` (back reference, handled by `CoremodState.owner`) | the only constant store is read through reflection only (`DebugHandlerClient`), by no accessor, so nothing observes it: not built. When an accessor reads a constant-initialised field, the read of a never-written slot must return that constant: `CoremodState.getInt/getLong/...` return 0 for an absent slot; extend `state()` to pass the default parsed from the constructor diff (`aload 0; ldc c; putfield`). Other shapes (computed values, calls) cannot be replayed lazily and stay a documented gap |
| persistence (`writeToNBT`/`readFromNBT` patched too) | none | investigated: no transformer that adds state to `ItemStack`, `TileEntity`, `Entity`, `Chunk` (Factorization, Logistics Pipes) or `FoodStats`/`BlockCake`/`EntityLivingBase` (AppleCore) also patches the NBT read/write methods (compared in the probe output and the 1.12.2 hook list); the added state is transient in all local cases (colliders, kinematics, add-info, timers, flags), so nothing needs saving with the world. If a coremod ever does, store the slots in the object's own NBT through the shim's `mcc$` write hooks, not a side file |

Left as known gaps: a methods-only mixin for a client-only class (`GuiBeacon$PowerButton.func_191745_a`, `ItemRenderer`) is generated only when the shim has the
class; the interface of a coremod that is owned by a different mod jar than the transformer (DragonAPI `BlockGrass` implements AgriCraft's `ISoilContainer`) is
generated into the interface's jar, so its methods call classes of another jar (works only when both are in the same class loader layer; unchecked).

## The case (Factorization 0.8.109, 1.7.10)

`probeCoremodHooks` shows for `net/minecraft/world/chunk/Chunk`: `+ implements factorization/coremodhooks/IExtraChunkData`, `+ field constant_colliders [Entity`,
`+ method getConstantColliders()`/`setConstantColliders(..)` (plain getfield/putfield bodies), plus a hook call in `func_76588_a` that passes `this` as Object and the
hook class casts it to `IExtraChunkData` (server `ClassCastException: Chunk cannot be cast to ...IExtraChunkData`).

## Why it is not trivial

1. The shim `Chunk` is compiled into the runtime jar and widened at build time (`ShimAccessWidener`), so it cannot differ per mod set. The interface lives in the
   mod's jar (another class loader/layer than the shim), so a shim class cannot name it statically.
2. Shim `Chunk` objects are NOT stable. `World.mcc$chunkOrNull` and `ChunkProviderServer.originalLoadChunk` build `new Chunk(world, access)` on every call, and a
   chunk of the chunk source can be an `ImposterProtoChunk` around the loaded `LevelChunk`. A field added to the shim object would be lost on the next lookup.

## Plan

- State: keep it on the modern chunk (stable): an identity map `WeakHashMap<ChunkAccess, Object[]>` keyed by the unwrapped chunk (`ImposterProtoChunk.getWrapped()`), or a NeoForge
  data attachment (non-serialized). Every added field of a class becomes a slot in that array; no per-view state.
- Interface and methods: do it in the mod, not the shim. The loader (translation of the mod jar, so TRANSFORMER_VERSION bump) reads the structure list, then
  - generates one class per extended game class in the mod's own namespace, e.g. `mcc/coremod/Chunk$Ext implements IExtraChunkData`, holding the shim `Chunk` and
    the slots; its methods are the added methods' bodies with `getfield/putfield <Chunk>.field` rewritten to the slot array and other `this` uses to the held shim view;
  - rewrites `checkcast`/`instanceof` of an added interface in mod code to `CoremodStructure.cast(obj, I.class)`/`is(obj, I.class)`, which for a shim `Chunk` returns the
    (cached per modern chunk, so stable) extension object and otherwise the object itself; invokeinterface calls then land on the generated class.
  - For `Object`-typed hook parameters (as here) the hook's own `checkcast` is rewritten the same way, so the added hook call keeps working.
- Run time: `CoremodSites1122`-style table is not needed; the list is read by the loader at translation, so only the side table class (`CoremodStructure`) is runtime code.
- Open: what to do with added methods that call other added/hooked code; added static members; a transformer that adds a superclass change (not seen yet).

## Structural additions to ANOTHER mod's classes (C2, 2026-11-24)

Case: Logistics Pipes' transformer (`LogisticsClassTransformer`) extends classes of other jars of the set: 1.7.10 Thermal Dynamics `TileTDBase`,
`TravelingItem` (`+ field lpRoutingInformation Object`, hook calls into `logisticspipes.asm.td.ThermalDynamicsHooks`), `RenderDuctItems`; 1.6.4 Thermal
Expansion `ConduitBase`, `ConduitItem`, `ItemRoute`, `TravelingItem` (`+ field routedLPInfo ItemRoutingInformation`) and BuildCraft `Pipe`,
`PipeTransportItems`. Other mods' classes are not "vanilla targets": `CoremodHookScan` (which shows the original Minecraft client jar to the transformers
once) does not see them.

Finding (the design answer; it needs no new application step):

- **Discovery**: nothing has to be found first. The locator's patcher (`LegacyModLocator`, `transformer.patcher(...)`, called from
  `LegacyJarTransformer.readPatched` for BOTH the peer scan and the translation) hands every class of every jar of a legacy version to that version's
  transformers (`LegacyCoreModTransformers.apply`), as LaunchWrapper offered every class to every transformer. A transformer that matches
  `cofh.thermaldynamics.duct.item.TravelingItem` by name therefore rewrites it when the Thermal Dynamics jar is read. `CoremodModTargets.scan` is the
  same pass as a listing (before/after nodes per changed class; `CoremodHookProbe` prints it as "== mod classes: set"), and `addedReferences` names the
  types an added member needs from another jar (the coremod jar's), for diagnostics.
- **Application at translation of the target jar**: the changed class is what the translation, the link check and the `PeerClasses` of the set see, so the
  member exists for the jar that uses it (Logistics Pipes reading `TravelingItem.lpRoutingInformation`) and the added hook calls link against the coremod's
  jar like any cross-jar reference. Structural additions to Minecraft classes need the mixin route (`CoremodAdditionMixins`) because the shim class is
  compiled; classes of other mods are real class files, so patching the bytes is the whole job.
- **Cache key**: the translated jar of the TARGET depends on the coremod jar, so the key must contain it. It does: `LegacyModLocator.modSetDigests` hashes
  EVERY legacy jar of a version into one digest used for every jar of that version (plus bridges, side, relocation inputs); adding, removing or updating
  Logistics Pipes changes the digest of the Thermal Dynamics entry. `TRANSFORMER_VERSION` is 269.
- **Visibility**: the locator logs once per jar "a legacy coremod of this mod set changes classes of X (first: Y)" (jars without a coremod of their own).

What was missing (and is added):

1. The offline check never ran coremods, so Logistics Pipes showed `TravelingItem.lpRoutingInformation` as 1 unresolved member against the unpatched jar.
   `./gradlew checkLegacyMod -Pcoremods=true -PlegacyJar=...` now runs the set's transformers like the game (flag `legacyconnector.check.coremods`; it sets
   `legacyconnector.runtimeClasses` and an FML game directory so the shim `Loader` initialises). Result 1.7.10 (LP 0.9.3.132 + TD + CoFHCore + CoFHLib +
   BuildCraft + TE + CCC + NEI): LP 1 unresolved -> 0, "coremod patched ThermalDynamics: TileTDBase, TravelingItem"; LP's own `ModDependent*` stripping runs too.
2. The 1.6.4 coremod class loader (`ShimLoader`) lacked classes that Forge 1.6 mod classes name while a transformer initialises them: it now also consults
   the 1.6.4 hierarchy data (empty class with the real supertypes: `IChatListener`) and serves a forward-renamed shim class under its old name
   (`net.minecraftforge.common.Configuration` from `config.Configuration`). A shim `CreativeTabs` built inside that loader no longer dies in the bridge's
   relocated signature.
3. (second round) The coremod class loader gives untranslated 1.4.7-1.6.4 mod code the view translated mods get, as far as it can:
   - the forward layer's class renames are applied to every class the loader defines (`forwardRenames`, `mcc$definer`; 1.6.4 `Configuration` is `config.Configuration`);
   - a shim class served there gets the facade's instance methods (`addFacadeMethods`: each public static facade method taking the class first becomes an instance method, so `Configuration.getItem(String,int,String)` links; facade classes are served as `facade/...`);
   - the shim's calls into the runtime's bridges whose descriptors name relocated shim types can never link in that loader (it serves the shim unrelocated), so they pop their operands and answer the default (`neutralizeBridgeCalls`; a game that is not running answers that way);
   - ASM 4/5 coremods subclass `MethodNode`/`ClassNode` anonymously; ASM 9 throws `IllegalStateException` for the constructors without an API level. These classes get `CompatMethodNode`/`CompatClassNode` as superclass (`AsmNodeCompat`). This was also the BuildCraft `Pipe` and Thermal Expansion failure (`LogisticsClassTransformer$3`), and Binnie Patcher's on 1.7.10.
   - LP patches TE only when its config says so (`TEPipeSupport=false` by default): `-Pcoremods=<game dir>` takes a game directory whose `config/LogisticsPipes.cfg` has `B:TEPipeSupport=true`.
   Result 1.6.4 (LP 0.7.4.dev.294 + TE 3.0.0.2 + CoFHCore 2.0.0.b7 + BuildCraft A 4.2.2, TEPipeSupport on): TE `ConduitBase`, `ConduitItem`, `ItemRoute`, `TravelingItem` and BuildCraft `Pipe`, `PipeTransportItems` are patched; LP 1 -> 0 unresolved (`TravelingItem.routedLPInfo` resolved).

### Coremods that still fail in the loader (offline `-Pcoremods=true` over the archive dirs, 2026-11-24, ranked by cost)

1. **CodeChickenCore, NotEnoughItems (1.7.10 set without a CodeChickenLib jar)**: 5 transformers/plugins (`CC_ClassWriter`, `ASMInit`, `ClassHeirachyManager`, `ModularASMTransformer`: NoClassDefFoundError). The check replaces CCL with its bridge (the shim holds the API, not the jar's `codechicken.lib.asm`), and these classes need the real `codechicken.lib.asm.*`. Likely the same in the game without a CCL jar. Fix idea: serve `codechicken.lib.asm.*` to the coremod loader only.
2. **LP 0.9.3.132 on Thermal Dynamics `RenderDuctItems`**: `ClassRenderDuctItemsHandler` finds nothing to patch in TD 1.2.1-172 and calls `exitJava` ("Legacy mod requested the game to exit"); LP's render hook for item ducts is not applied. Needs the TD build LP expects, or a look at the handler's pattern.
3. **INpureCore** (1.7.10): the plugin reads FML's relaunch `Logger` (`FMLRelaunchLog`'s log4j logger is null: `addFilter` NPE); the coremod API has no logger behind it. 1 plugin.
4. **Tropicraft 1.4.7 `RenderPlayerTransformer`** (2 classes) and **Starminer 1.6.4 `SMTransformer`**: the transformers name OBFUSCATED vanilla classes (`uk`, `c`) while the loader serves deobfuscated names only; fix: serve those names through `NotchNames` as bare classes.
5. 1.5.2: no failures.

