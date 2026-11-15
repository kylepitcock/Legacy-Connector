# Coremod structural additions (interfaces, fields, methods a coremod adds to a Minecraft class)

Status: recording half done (`CoremodHookDiscovery.additions`, `CoremodHookScan.writeStructure`, list `.legacyconnector/cache/coremod-structure-<mc>.txt`,
`kind TAB class TAB name TAB desc TAB access` per line). Applying is done for the common shape (2026-11-15): an added interface owned by a mod jar, with the added instance methods, becomes a generated
mixin on the shim class (`CoremodAdditionMixins`, config `legacyconnector.legacy.<jar>.coremod-additions.json`, let through by `LegacyMixinPlugin`); added
instance fields are kept in `CoremodState` per game object. Verified on a 1.7.10 server: Factorization `Chunk`/`Entity` and Logistics Pipes
`ItemStack`/`TileEntity` mixins apply, the `IExtraChunkData` ClassCastException is gone. Not done: static members, constructors, added methods without an
interface (`BlockRailBase.func_149743_a`), 1.12.2 runs (same code path, untested), saving the added state with the world.

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
