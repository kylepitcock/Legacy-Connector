# Layer hot spots (JFR, headless server self test, 2026-11-17)

Source: `tools/dev/perf/baselines/2026-11-17-*.txt` (made by `tools/dev/perf/JfrLayer.java` from `-Pjfr` recordings of `tools/dev/verify.sh mixed`; see
`docs/wiki/Performance.md`, "Measuring" and "Baselines"). "Layer" = frames under `legacyconnector.` or `org.kylepitcock.legacyconnector.`.
The self test is not a game session: the server thread runs 700 ticks of self tests with hundreds of 1.12.2 / 1.7.10 mobs in a few seconds, so ranks
describe what a pack with many hosted legacy mobs pays, not an idle world. The machine was shared with other workers (timings are noisy, sample
shares and allocation are not).

## Server thread, before the fixes (mixed suite: 1.7.10 + SRParasites 1.12.2; 948 CPU samples, 14.4 GB sampled allocation)

| # | Layer frame | CPU (self, inc. callees below it) | Allocation | Called from | Status |
|---|---|---|---|---|---|
| 1 | `Registries1122$Reg.getValues` (copy of the whole registry per call) | 20.7% | 49.3% (7.1 GB) | `EntityList.getKey/newEntity/getID`, `EntityRegistry.*` scans, which mods call per entity per tick (SRParasites: `ParasiteEventEntity.checkEntity`, `EntityParasiteBase.canAttackClass`) | fixed |
| 2 | `MobHost1122.travel` (vanilla travel/move underneath; the host overhead itself is a `new Vec3` and one cached `overrides` check) | 9.7% | 1.5% | `LivingEntity.aiStep` | not the layer's cost (vanilla physics), left |
| 3 | `World.mcc$get$loadedEntityList` (wraps every entity of the level into a new list per call) | 1.3% (self) | small | `getEntities(Class, Predicate)`, `countEntities`, `getLoadedEntityList`: SRParasites `EntityAIFollowBodies.getFather` per mob per tick: O(entities^2) | open (needs a per-tick snapshot with invalidation on add/remove; behaviour risk) |
| 4 | `PathNavigate.getPathToEntityLiving`, `World.getEntitiesWithinAABB*`, `modernEntities`, `EntityBridge1122.wrap` | 4.5%, 3.8%, 3.6%, 3.3% (inclusive of vanilla below) | 2.6% (`getEntitiesWithinAABB`) | AI goals | cheap per call; wrap is already a field read |
| 5 | `ResourceLocation.toString` (string concat per call) | n/a | 2.1% | key lookups | fixed (cached string) |
| 6 | `EnumFacing.values()` (enum clone per call) | n/a | 1.7% | mod code (`for (EnumFacing f : EnumFacing.values())`) | not fixable in the shim (compiler generated); a mod-side pattern |
| 7 | `Chunk.<init>` of the 1.7.10 view (16 ArrayLists + 3 arrays) per ChunkEvent | n/a | 2.5% | `EventBridge1710` Load/Unload/Data events, only when a mod listens | per chunk, not per tick; left |

After the fixes (same suite, cold): server CPU 948 -> 845 samples (-11%), sampled allocation 14.4 -> 6.9 GB (-52%); with a warm translation cache 1091 -> 774
(-29%) and 14.1 -> 7.2 GB (-49%). Layer own cost is now below 1.5% per method; what remains in the server thread is vanilla (`PalettedContainer.get/pack`,
Pehkui scale lookups, pathfinding) under the hosts.

## Loading (main thread and `modloading-sync-worker`), cold cache

| # | Layer frame | CPU share of loading | Note |
|---|---|---|---|
| 1 | `LegacyModLocator.findCandidates` (translation: ASM `ClassReader.readCode` 22% self; `LegacyCoreModTransformers.apply` 14%, `LegacyJarTransformer.transform` 16%, `LegacyRemapper.map` 6%) | 32% | 19 s of the 50 s cold start for 6 jars (Traincraft alone 11 s); once per jar content, cached afterwards |
| 2 | `VanillaFlattening.item` (data fixers per (name, damage)) | 13% | already cached in `.legacyconnector/vanilla-items-*.txt`; the cold run pays it once (Thaumcraft preInit 6.4 s) |
| 3 | `LegacyCoreModTransformers.applyToVanilla` | 5% | runs even warm: re-reads vanilla classes to apply coremod patches; candidate for caching the patched result per mod set |
| 4 | `GeneratedPack$1.openFull` | 5.7% | building the generated pack |

Warm start still shows `findCandidates` at 35% of loading CPU: the inspector (`LegacyJarInspector.constantPoolText`, `Markers.has`) and the coremod scan
re-read jars on every launch; the cache saves the transform, not the inspection. Candidate: key the inspection by jar size+mtime.

## Worker threads (chunk generation)

`LegacyChunkGenerator.lambda$fillFromNoise$3` is the top monitor contention (1.7 to 2.5 s in total over 2 worker threads for the run): it serialises the legacy
chunk provider (`synchronized (provider)` and the biome lock) on purpose, as legacy generators are not thread-safe. 94% of worker CPU is vanilla noise
generation. Not changed.

## Cost of the #293 fix (branch `worktree-agent-ae6861dc47f906f97`, read only)

The change makes `LegacyItemRenderer.renderByItem` and the block handler path call `RenderContext.beginItem/endItem`, which take `CAPTURE_LOCK` even when
`legacyconnector.renderLock` is off, and save/restore `OpenGlHelper.lastBrightness*` (one `float[2]` plus the `Frame` per draw, pushed on a per-thread deque).

- Uncontended per draw: one `ReentrantLock.lock/unlock` (about 20 ns, one CAS each) + a 2-float array and a deque push/pop: well under a microsecond, below the
  existing `synchronized (renderer)` that was already there. A GUI with 36 to 50 legacy item draws adds about 50 microseconds per frame at most.
- Contention: `CAPTURE_LOCK` is held by a chunk-meshing thread for the length of one legacy block capture (`IsbrhBlockModel`, `beginCapture` to `end`): tens
  of microseconds up to milliseconds for heavy ISBRH blocks (pipes, machines). The render thread now waits for at most one capture per item draw, so with
  Sodium's meshing threads busy (flying into new chunks of a pack with many ISBRH blocks) each of the ~40 slot draws can stall for a capture, and in the
  worst case a frame loses N x capture time. Meshing threads in turn wait for item draws (microseconds).
- The lock is not fair (`new ReentrantLock(RENDER_LOCK)`; the comment in the source says it must be fair when the render thread takes it): several mesh
  threads re-taking it back to back can barge ahead of the render thread's queued request, so the stall is unbounded in theory. If a stutter is reported, make
  it fair (`new ReentrantLock(true)`) or make `beginItem` lock only around the shared-state window.
- Needs a client measurement: yes, frame-time percentiles (p99, max) in a hotbar full of Thermal Expansion items while chunks mesh (fly or `/tp` into
  fresh terrain) with Sodium, compared with and without the commit. Not measurable headless (no render thread, no meshing).

## Next candidates (not done)

1. `World.mcc$get$loadedEntityList`: per-tick snapshot per level, invalidated by an entity add/remove counter (unsafe to do blind: 1.12 mods mutate and re-read).
2. `LegacyCoreModTransformers.applyToVanilla` and jar inspection on a warm start: cache by jar fingerprint.
3. Translation: ASM parse flags (`SKIP_DEBUG`/`SKIP_FRAMES` where the pass does not need them) in `LegacyCoreModTransformers.apply`.
