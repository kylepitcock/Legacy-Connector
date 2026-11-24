# Plan: Forge 1.19.2, 1.18.2 and 1.16.5

Design note, 2026-11-13. Nothing here is implemented. The order of versions stays as in the [Version roadmap](Version-Roadmap.md): these three
come after 1.20.1 is stable, and each is built on the one before. Back to [Home](Home.md).

## Where the supported versions stand (probe of 2026-11-13, release 0.4.14)

| Version | Offline link result | What the remaining gap is made of |
|---|---|---|
| 1.7.10 | 560 of 696 jars link | 78 jars with unresolved references (a long tail, plus three regressions); behaviour: several mod fluids do not flow in the self test, structure chests roll their loot when a mod merely looks at them, GL defaults in legacy renderers |
| 1.12.2 | 604 of 706 | Links are nearly closed (5 jars with shim gaps). The gap is behaviour and assets: 1,041 asset references lost in translation in 37 jars, and a backlog of client rendering fixes that were never checked in game |
| 1.6.4, 1.5.2, 1.4.7 | most jars of small sets link | Rendering and GUI of the sprite-sheet era, not symbols. The sets were small; the MCArchive sets added today are the first broad measurement |
| Beta 1.7.3 | 64 of 72 archives link | Links, but mostly through placeholders: 2,822 synthesized members and 296 missing ones ([work list](../../tools/dev/agent-tasks/b1.7.3-missing-stubs.md)) |
| 1.20.1 | 99 of 506 (ATM 9) | The weakest: 264 jars need layer support, 108 report a missing dependency, and mixins whose targets changed are not repaired |

Two conclusions for what follows:

1. **1.20.1 is the foundation.** Every older "modern" version reaches 1.21.1 through the 1.20.1 stage, so each gap closed there is inherited
   by 1.19.2, 1.18.2 and 1.16.5, and each gap left open is multiplied by three.
2. **The two families fail differently.** The shim versions (up to 1.12.2) link well and fail in behaviour; the remap versions (1.20.1 and
   older modern ones) fail at linking first. Verification must measure both: link rate offline, behaviour in the regression suites.

## Why these three are one problem, not three

Forge 1.16.5 to 1.20.1 mods share the modern class model with 1.21.1: registries and `DeferredRegister`, block entities, JSON models and
block states, data-driven recipes, loot and tags, capabilities, `SimpleChannel`, mixins. None of them needs a hand-written shim of the game
as 1.7.10 and 1.12.2 do. What separates them from 1.21.1 is a sequence of renames, signature changes and format changes, which is exactly
what the 1.20.1 pipeline already processes: rename tables, call rewrites, bridge methods for overrides, migration helper classes, data
conversion, soft links, mixin handling.

So the plan is a **chain of stages**, each a set of tables and helpers of the same kind as the 1.20.1 one:

```
1.16.5 --A--> 1.18.2 --B--> 1.19.2 --C--> 1.20.1 --D (exists)--> 1.21.1
```

A mod is taken through the stages from its own version forward. A 1.19.2 mod runs C then D; a 1.16.5 mod runs A, B, C, D.

### Design decisions

- **One name space.** Every jar is first brought to the Mojang names of its own version (1.17 and later ship SRG ids `m_`/`f_` with Mojang
  class names; 1.16.5 ships SRG ids `func_`/`field_` with MCP class names, see stage A). From there on, stages work on Mojang names only.
  SRG ids are not stable across 1.16.5 to 1.17 (they were renumbered), so names, not ids, are the bridge between versions.
- **Stages are data, the engine is shared.** The 1.20.1 transformer classes become the engine, parameterised by a stage directory
  (`forward/<from>/`): class renames, member renames, call rewrites, override bridges, hierarchy, tags, library rules. Stage-specific code is
  limited to migration helpers (`migrate` classes) and data converters. This refactoring is the first piece of work and is useful for 1.20.1
  alone (it forces the tables to be complete instead of patched in code).
- **Tables are generated, then curated.** For each pair of neighbouring versions a tool compares the two games (client jar plus official
  mappings, both fetched at development time and never committed) and emits: classes moved or renamed, members renamed, members whose
  descriptor changed, members removed, hierarchy changes. Rows the tool can prove go into the tables automatically; the rest is the work
  list, ranked by how many jars of the corpus use each symbol. Only the differences are shipped, as today.
- **Compose offline where it is pure renaming.** Rename rows of consecutive stages are composed when the tables are built, so a jar is
  rewritten once for names. Semantic steps (helpers, bridges, data conversion) stay per stage and run in order.
- **Proof before rewrite.** The mixin repair now being added for 1.20.1 resolves every target against an index of the real target version.
  The same index, built per intermediate version, lets each stage prove its output against the next version's classes, so an error is
  reported at the stage that caused it rather than as an unexplained link failure at the end.
- **Measure first, per version.** Before any stage is written: collect a corpus (the largest abandoned packs of that version, as ATM 9 serves
  1.20.1, plus the most downloaded Forge mods that have no newer version), run the generated tables over it, and rank the remaining symbols.
  The 1.20.1 work showed that expectations about "what mods use most" were wrong until measured.

## The stages

Sizes are relative to stage D (1.20.1 to 1.21.1, 25 to 30 agent-days to broad coverage). **Milestone 2 (measurement) is done for all three stages** (2026-11-16 for C, 2026-11-24 for B and A); measured relative sizes, in
distinct symbols that need real adapters and in agent-days to the link ceiling:

| Stage | Symbols (b, own sample) | Agent-days | Relative to D | Link today / ceiling | Page |
|---|---|---|---|---|---|
| D: 1.20.1 to 1.21.1 | | 25 to 30 | 1.0 | 28% of ATM 9 | [1.20.1 estimate](Legacy-1.20.1-Estimate.md) |
| C: 1.19.2 to 1.20.1 | 992 (1.19.2 sample) / 655 (1.18.2 sample) | 22 to 28 | 0.9 | 11.7% / 29.7% | [1.19.2 estimate](Legacy-1.19.2-Estimate.md) |
| B: 1.18.2 to 1.19.2 | 743 (1.18.2 sample) / 709 (1.16.5 sample) | 14 to 18 | 0.5 to 0.6 | 7.7% / 22.7% (with C) | [1.18.2 estimate](Legacy-1.18.2-Estimate.md) |
| A: 1.16.5 to 1.18.2 | 1,212 (+1,287 Forge-patch members) | 35 to 50 | 1.4 to 2 | 5.5% (raw) / 12.8% (raw, lower bound) | [1.16.5 estimate](Legacy-1.16.5-Estimate.md) |

The samples differ (451, 446 and 442 jars of each version's most downloaded content mods), so the columns are comparable in order of magnitude only (plus or minus 40%).

### Stage C: 1.19.2 to 1.20.1 (smallest; do first)

Measured on 451 jars (2026-11-16): [Legacy-1.19.2-Estimate](Legacy-1.19.2-Estimate.md). Mostly changed API rather than renames; the table below stays the right shape list.

Renames and moves dominate, with four semantic areas:

| Area | Change | Approach |
|---|---|---|
| GUI drawing | `GuiComponent` statics with a `PoseStack` became `GuiGraphics` (1.20) | Bridge overrides `render(PoseStack, ...)` to `render(GuiGraphics, ...)` with a helper that wraps the pose; call rewrites for `blit`, `fill`, `drawString` |
| Registries | `Registry.BLOCK` and friends moved to `BuiltInRegistries`; `Registry` keys to `Registries` (1.19.3) | Field and call rewrites (pure table) |
| Math | `com.mojang.math` vectors, matrices and quaternions replaced by JOML (1.19.3) | Class renames plus helpers for the methods whose semantics differ (`Vector3f.XP.rotationDegrees`, in-place versus returning operations) |
| Content declaration | Creative tabs (`Item.Properties.tab`, `CreativeModeTab` subclasses) became a registry and an event; `Material` removed (1.20); damage sources became data (1.19.4); smithing recipes; sound event factory methods | Helpers: collect `tab(...)` calls into tab contents, map `Material` constants to block properties, map damage source constants to the level's sources |

Data: block/item texture folders are already handled; atlas rules of 1.19.3 make loose textures invisible unless listed, so the converter
adds them to the block atlas (the 1.12.2 layer already does this for code-referenced sprites).

### Stage B: 1.18.2 to 1.19.2

Measured on 446 jars (2026-11-24): [Legacy-1.18.2-Estimate](Legacy-1.18.2-Estimate.md). The 1.20.1 layer links **34 of 441** of them today (7.7%), **14.5%** would link against 1.19.2's names once pure renames are applied
(22.2% counting Minecraft alone), and the ceiling with all of B and C done is 100 jars (22.7%). Of 3,024 distinct unresolved symbols 580 are one table row (the SRG ids the 1.20.1 table lacks), 100 are renames of stage B,
**743 need real adapters** (467 Minecraft, 276 Forge: the Forge 40 to 43 break removes 297 classes, about 100 of them events), 704 belong to stage C. **Size: about 0.6 to 0.7 of stage C, 14 to 18 agent-days, 0.5 to 0.6 of
stage D**; the table below keeps the right shape list, and `Random` to `RandomSource` (177 symbols, 1,103 occurrences) is the most error-prone item.

| Area | Change | Approach |
|---|---|---|
| Text | `TextComponent`, `TranslatableComponent` and other classes became `Component` factory methods | Constructor-to-factory call rewrites |
| Randomness | `java.util.Random` parameters became `RandomSource` in most game methods | Signature bridges both ways (an override taking `Random` is bridged; a call passing `Random` is wrapped) |
| Forge events and registration | Mass rename of Forge 1.19 events (`World` to `Level` in names, input and overlay events), `ForgeRegistryEntry` and `setRegistryName` removed, `FluidAttributes` became `FluidType` | Event rename table generated like the NeoForge one; registry-name helper that records the name set in the constructor chain; fluid helper |
| World generation | `StructureFeature` became `Structure`, configured structures changed format; biome tags | Class renames plus a data converter for structure JSON; code-registered features go through the stage's helper |

### Stage A: 1.16.5 to 1.18.2 (largest; three eras in one)

1.16.5 is the second largest pool of abandoned mods and packs, and the hardest stage.

Measured on 442 jars (2026-11-24): [Legacy-1.16.5-Estimate](Legacy-1.16.5-Estimate.md). The name step is solved by data: the opt-in table matches 100% of the 1.16.5 methods (35,370) and fields (19,841) and
5,003 class rows (MCP to Mojang). Raw (no 1.16.5 translation exists to run) **24 of 439 jars link** (5.5%); 12.8% would link against 1.18.2's names after pure renames (32.3% counting Minecraft alone). Of 7,223 distinct unresolved symbols
167 are stage A renames, **1,212 need real work at stage A** (1,017 Minecraft, 195 Forge), 1,287 are members added by Forge's binary patches that cannot be diffed offline, and 709 (B) and 637 (C) belong to the later stages.
Stage A removes 5,512 methods and 295 classes against 2,024 and 140 at stage B. **Size: about 1.7 times stage B and 1.9 times stage C by symbol count, 35 to 50 agent-days alone (1.4 to 2 times stage D), 71 to 96 agent-days for a 1.16.5 mod through all four stages**
(2.5 to 3.5 times stage D). Rendering (206 symbols, fixed-function GL), world generation (313) and the block entity model are the three big items. The table below stays the right shape list:

| Area | Change | Approach |
|---|---|---|
| Names | MCP class names (`net.minecraft.block.Block` stays, but `PlayerEntity`, `World`, `TileEntity`, `CompoundNBT`, `MatrixStack`, `IVertexBuilder`...) and `func_`/`field_` ids | A separate first step: 1.16.5 SRG to the 1.16.5 Mojang names (official mappings), then the MCP-to-Mojang class table. After it the jar looks like a 1.17-style jar |
| Block entities | Constructor gained `(BlockPos, BlockState)` (1.17); `ITickableTileEntity` replaced by tickers supplied by the block | Bridge: generated constructor and a holder for position and state set before the legacy constructor runs; the layer supplies a ticker for types whose class implemented the tick interface |
| Rendering | Fixed-function calls removed in 1.17 (`RenderSystem.color4f`, alpha test, lighting, `GlStateManager` state), render type shards changed, core shaders required | Reuse of what exists: the GL state emulation written for 1.7.10 and 1.12.2 (`RenderContext`) already turns such calls into modern render state; 1.16.5 calls are routed there. Custom `RenderType`s are rebuilt from their legacy state |
| World | Height limits and section layout (1.18); world generation rebuilt (biomes through `BiomeLoadingEvent`, features and surface builders registered in code, no multi-noise) | Reuse again: biome additions become biome modifiers, code-registered features become placed features through the same generator the 1.12.2 layer uses; surface builders have no counterpart and are dropped with a report line; height handled as for 1.12.2 chunks |
| Java | Mods were built for Java 8 | Already handled for the older versions |

Stage A depends on two pieces that only exist for the shim versions today (GL emulation, legacy world-generation bridges), so it is the
first place where the two families meet. That is the main design risk and the reason it comes last.

## Milestones and exit criteria

1. **Engine refactoring (within 1.20.1 work).** Stage directory as a parameter, the version index tool, generated tables with a diff
   against the hand-made ones. Exit: the 1.20.1 probe result is unchanged or better with generated tables.
2. **Measurement for all three. Done 2026-11-24.** Corpus per version, generated tables only, gap report and ranking, written up like the 1.20.1 estimate
   ([1.19.2](Legacy-1.19.2-Estimate.md), [1.18.2](Legacy-1.18.2-Estimate.md), [1.16.5](Legacy-1.16.5-Estimate.md)). Exit: a link rate per version and a ranked symbol list.
   Result: the estimates hold in order (B smallest, then C, then A); the "1 to 2 days each" is off by an order of magnitude, and stage A is larger than stage D.
3. **Stage C, then B, then A.** For each: link rate target first (the share of content mods that link completely, client-only mods not
   counted), then a regression suite of five to ten mods of that version in `tools/dev/verify.sh`, then a client scenario per mod family.
   A version is announced as alpha when its suite passes and as beta when a real pack of that version reaches the title screen and a world.

Not estimated in days on purpose: stage D is at 20 percent of ATM 9 linking after its first weeks, and the earlier "1 to 2 days each"
figure in the roadmap assumed a finished 1.20.1 stage. A realistic statement after milestone 2: each stage costs a fraction of stage D
proportional to its ranked symbol count, and stage A costs at least as much as stage D. **Measured (milestone 2):** B 0.5 to 0.6, C 0.9, A 1.4 to 2 times D in agent-days (table above).
What the measurement cannot show: behaviour, library APIs, Forge's binary patches (1,287 symbols of 1.16.5), the 1.20.1 translation of 1.16.5 jars (no such run exists), and abandoned-pack bias.

## What to decide before starting

- **When 1.20.1 counts as stable enough** to build on. Proposal: content mods of ATM 9 link at 60 percent or more, the mixin repair is
  merged, and the regression suites include a 1.20.1 set.
- **Whether 1.16.5 may jump the queue** as 1.6.4 once did. It has the most players and mods of the three; it is also the stage that needs
  the other two finished to keep its tables small. A middle way: do milestone 2 (measurement) for 1.16.5 early, because the corpus and the
  ranking cost little and tell how large stage A really is.
- **Mapping data.** The stage tables are derived from Mojang's official mappings, as the 1.20.1 ones are; only differences are shipped.
  Intermediate full mappings are fetched by the tool and never committed.

## Milestone 1 status (2026-11-24)

**Inventory: where stage D (1.20.1 to 1.21.1) was hardcoded.** The tables sit in `loader/src/main/resources/legacyconnector/forward/1.20.1/`:
`mc-classes`, `mc-members`, `mc-calls`, `mc-fields`, `mc-bridges`, `mc-overrides` (+ `lib-*` twins for library bridges), `mc-hierarchy.tsv.gz`,
`mc-closed.txt.gz`, `mc-protected.tsv.gz`, `mc-members/static/private.index.gz` (index of the 1.21.1 classes), `mixin-targets.tsv`, the Forge side
(`forge-call-rewrites`, `forge-curated-renames`, `forge-event-renames`, `forge-fml-renames`, `forge-members`, `forge-c-tag-pairs`, `c-tags`,
`neoforge-abstract-events`) and `libraries.tsv`. Eleven readers opened them by a string literal of the folder (`Mc1201Tables`, `Mc1201Hierarchy`,
`McMembers1201`, `ClassShape1201`, `ProtectedCalls1201`, `Subscribers1201`, `ForgeApiRewrites1201`, `Forge1201Transformer`, `mixin1201/TargetRemap`,
`DataConverter1201`, `NativeLibraries1201`), two generators wrote into it (`EventTables1201Tool`, `McMembers1201Tool`), and the helper package of
`mc-calls` rows (`v1_20_1/migrate/`) was a constant of `Mc1201Tables`. Still stage specific by design, not by accident: the class names (`*1201`),
the `Forge1201Transformer` special cases (library compat under `transform/compat`), the migration helpers (`migrate` classes), the data converters
(`resource/*1201`), the SRG table (`genSrgMojang1201`, one per source version) and `Gap1201` (`LegacyJar.version = V1_20_1`).

**Done.** `transform/ForwardStage` is `(source, target, dir, helperPackage)`; every reader above and both generators now go through it
(`ForwardStage.D_1201.open("mc-classes.tsv")`). `ForwardStage.chainFrom(version)` returns the stages a jar of that version passes in order
(`1.19.2` gives C then D; C is declared but has no tables and no loader detection), `composeRenames` composes the class renames of consecutive stages
(a rename of stage C followed by one of stage D is one row), and `Mc1201Tables.loadChain` loads a chain (class renames composed, the other tables
merged, later stages winning). One stage loads exactly as before. Tests: `ForwardStageTest`.

**Byte identity.** `python tools/dev/identity_1201.py run <label>` translates 40 ATM 9 jars (every 13th of the sorted instance folder) with the real
transformer (`gap1201 -Pgap.translate=true`) and `compare <a> <b>` compares every entry of every translated jar (SHA-256, except
`provenance.json`, which carries a timestamp). Before and after the refactoring: 40 jars, 64,497 entries, identical; the gap counts are unchanged
(`classes 509/509 fields 6/6 methods 30/37`); two runs of the same build are identical too, so the check is deterministic.

**Version index and generator.** `python tools/dev/version_index.py diff|compare <from> <to>` fetches Mojang's official mappings and client jars into
`build/version-index/` (gitignored) and writes candidate rows: class moves (unique simple name) and renames (same package and similar name, or similar
member shape), member renames (the only vanished and the only new member of a class with the same descriptor), descriptor changes, removed members,
hierarchy changes. It needs no SRG data. `compare` measures it against the hand-made tables of a stage. For 1.20.1 to 1.21.1 (10 seconds):
8,269 classes against 7,436, 506 gone, 92 class candidates (40 moves, 52 renames), 386 member renames, 4,252 descriptor changes, 341 hierarchy changes.

| Hand-made rows | Applicable | Reproduced | Contradicted | Detected, not proposed | Missed |
|---|---|---|---|---|---|
| `mc-classes` + `lib-classes` (207) | 71 (69 are libraries or Forge, 67 point at layer helpers) | 39 | 2 | 2 (old name still exists: deliberate redirect) | 28 (renames by a changed name only, e.g. `BootstapContext`, `BlockPathTypes` to `PathType`, `FriendlyByteBuf$Writer` to `StreamEncoder`) |
| `mc-members` + `mc-fields` (569) | 472 | 27 | 1 | 281 (descriptor changed: needs a call row, not a rename) | 163 (no change visible by name: enum constants of other types, behaviour) |
| `mc-calls` (1,174) | 1,048 | n/a | n/a | 821 flagged (member gone, renamed or descriptor changed) | 216 (behaviour change or inherited member) + 11 constructors |

Reading: the generator proposes about half of the applicable class rows and 6 percent of the member rows by itself; the rest of the hand work is
semantic (helpers), which is what the plan says stays per stage. Its real use is the other direction: the 4,252 descriptor changes and 5,129 removed
members are the work list for a new stage, ranked by corpus usage in milestone 2. **Left for milestone 1:** feeding the generated candidates into a
stage directory with a provenance column (hand rows win), the per-stage SRG tables for 1.16.5 to 1.19.2, and moving the shared `Forge1201Transformer`
special cases that are not stage D specific into the engine.
