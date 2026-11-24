# Improvement outlines (2026-11-24)

Starting points for workers. Each outline: goal, why, where to look, approach, how to verify, rough size (S < half a day, M about a day, L several days).
Read `BRIEF.md` and `issue-worker.md` first. Mark an outline `DONE <commit>` or `TAKEN <branch>` when you start or finish it.

## Testing and tooling

### T1. Resumable regression runs (S) TAKEN
- Why: `tools/dev/verify.sh` runs of 75-100 suites die midway (Git Bash fork errors, RAM pressure) and the final table is lost; the lead then has to guess which suites ran.
- Where: `tools/dev/verify.sh`, `tools/dev/verify/verify.py`.
- Approach: write each suite's result to `build/devtools/verify-<suite>-result.json` as soon as it finishes; `--resume <run-id>` skips suites with a result from the same run and git HEAD; `verify.py report --run <id>` rebuilds the table from those files. Print the run id and HEAD at the start.
- Verify: kill a run after 3 suites, resume it, and get a full table. Add tests in `tools/dev/verify/test_verify.py`.

### T2. Baselines for unbaselined suites (S)
- Why: 1.12.2 suites (ae21122, bloodmagic1122, botania1122, enderio1122, forestry1122, quark1122, rftools1122, thaumcraft1122, twilight1122, customoregen1122) and others report NEW, so a regression in them never fails a run.
- Approach: on master, after a clean run, `tools/dev/verify.sh --update <suite...>`. Before updating, check that flaky metrics such as srp `entities_alive` have tolerances in `verify.py`.
- Verify: a second run of the same suites PASSes.

### T3. Stale-rating re-rater (M)
- Why: many `docs/data/mods.json` "fails" rows (1.12.2: most of 89; pre-1.7: several) predate fixes. Probe results never override client/corpus rows, so stale rows stay on the site.
- Where: `tools/compat/from_client.py`, `tools/dev/verify/picker.py` (jar resolution), `docs/data/mods.json`.
- Approach: `tools/compat/rerate.py --mc <v> --status fails --source client,corpus` picks those rows. It resolves each mod's jar plus required dependencies through the picker, runs the server self test in small groups (own port, RAM check), and feeds each log to `from_client.py` with label "rerate <date>". It writes a summary of old -> new ratings.
- Verify: run it on 10 1.12.2 rows; the rows change only where the run supports it.

### T4. Shared translation cache across worktrees (M)
- Why: every worktree retranslates the same jars (minutes per suite, GBs of disk). Workers in parallel worktrees pay this again and again.
- Where: the loader cache (`run-*/.legacyconnector/cache`, key = jar hash + mod set + loader build id).
- Approach: optional `-Dlegacyconnector.sharedCache=<dir>` read-through cache; entries are immutable and keyed by the full key; write to a temp name, then atomic rename. Default off for players.
- Verify: two worktrees run the same suite; the second skips translation (log line) and the metrics are identical.

### T5. Two world loads in one JVM (M; also #113)
- Why: #113 crashed only after the second world load in one game session. Static state that survives a server stop (caches keyed by registry objects, per-world maps, holder sets) is a defect class.
- Approach: a client scenario `tools/dev/verify/scenarios/reload-twice.txt`: create a world, quit to title, load it again, then do actions with enchanted and modded items. Also audit static maps in the shim, bridges and `core` that hold `Level`/`Holder`/`Registry` references; clear them on `ServerStoppedEvent`.
- Verify: the scenario passes with 1.7.10 and 1.12.2 sets; `jmap -histo` shows no old `ServerLevel` after the second load.

### T6. Survival "stable" sets per version (M)
- Why: there is no shared definition of "stable". The suggestion still awaiting a decision: a named set of major mods per version that is playable in survival.
- Approach: draft `docs/wiki/Stable-Sets.md` with 8-12 mods per version (1.7.10, 1.12.2, 1.6.4) chosen by popularity and lack of a native 1.21.1 port. Give each a client scenario (`tools/dev/verify/scenarios/`) that crafts the mod's core item, places its main machine, and saves and reloads. Report pass/fail per mod. Ask the maintainer to approve the list before building scenarios for all of them.

### T7. In-game bug report bundle (S)
- Why: reporters often attach incomplete logs; workers spend time asking.
- Approach: `/legacyconnector report` writes `logs/legacyconnector-bundle-<time>.zip` (latest.log, legacyconnector-report.txt, mod list with versions, LC version, settings) and prints the path in chat; the issue template points to it. Strip user paths and names (replace the home dir).
- Verify: a unit test of the bundler; a client run creates the zip.

## Compatibility classes

### C1. Animated armor and player arm sync, 1.7.10 (M; #151 Fisk Superheroes, others)
- Why: mods that animate a suit's `ModelBiped` arms move only the suit; the player's own arms stay put. 1.7.10 renderers set the player model's rotations and the armor model copied them (or the reverse) through `ModelBiped.setRotationAngles` / `RenderPlayer`.
- Where: the 1.7.10 player render bridge (Grep `RenderPlayer`, `ModelBiped` under `v1_7_10/client`).
- Approach: find which side the legacy code writes (Fisk changes `bipedRightArm.rotateAngleX` on the main model in `RenderPlayerEvent.Pre`/`SetArmorModel`). Copy the legacy main-model angles to the modern `PlayerModel` before it renders, and the modern ones to the legacy model before legacy armor renders.
- Verify: client with Fisk Superheroes; use a suit animation and take a shot showing the arms moving together.

### C2. Coremod structural additions to another mod's classes (L)
- Why: Logistics Pipes 1.6.4/1.7.10 coremod adds fields and interfaces to Thermal Expansion / BuildCraft classes. The current coremod emulation only handles vanilla targets.
- Where: `coremod-structural-additions.md`, loader coremod hook discovery.
- Approach: when a coremod's transformer targets a class of another translated jar in the same mod set, apply the structural part (added fields, interfaces, methods) at translation time of that jar, and make the translation-cache key include the coremod's jar.
- Verify: Logistics Pipes + TE 1.6.4 link offline with 0 unresolved; the server self test loads both.

### C3. 1.20.1 member remaps inside mixins (M)
- Why: mixin `@Shadow` fields and method targets that name 1.20.1 members renamed or removed in 1.21.1 (Blue Skies `MerchantOffer.baseCostA`) are not covered by the member tables, so the mixin fails or is dropped.
- Where: the 1.20.1 mixin remap pass (Grep `mc-members` users in loader `forward`).
- Approach: apply `mc-members` / `mc-calls` renames to mixin annotations (`@Shadow` names, `@Inject(method=...)`, `@At(target=...)` descriptors). Where a member has no 1.21.1 equivalent, synthesize a field on the target through the access layer and log it.
- Verify: offline mixin apply report for Blue Skies and two other jars; no new "mixin not applied".

### C4. Runtime resource packs for 1.12.2 assets (M; in progress for #342)
- Why: mods that add an `IResourcePack` at run time (GregTech's `AbstractBlockModelFactory`, others through `Minecraft.defaultResourcePacks`) serve blockstates and models that the asset translation never sees.
- Status: TAKEN by the #342 worker; follow its branch.

### C5. Legacy JEI 1.12.2 categories (L; in progress)
- Status: TAKEN by the 1.12.2 JEI gap worker.

### C6. Other-version jars (S; in progress)
- Status: TAKEN by the pre-1.7 worker (the "built for another Minecraft version" report).

## Performance

### P1. Display-list baking follow-up (M; #295, #82)
- Why: baked display lists raised CustomOreGen wireframes from 10 to 70 fps. Other immediate-mode hot paths (TESRs that draw through the Tessellator every frame, entity models of legacy mods) still replay vertex by vertex.
- Approach: profile a 1.7.10 pack with many TESRs (Thaumcraft, Traincraft, Witchery) with JFR (`-Pjfr=...`); rank by frame time; extend baking to TESR draws whose inputs are unchanged between frames (keyed by tile state hash), with a safe fallback.
- Verify: before/after fps at a fixed spot with performance mods on (`tools/dev/quick-shots.sh` with an fps log); picture identical.

### P2. Generation speed of legacy world generators (M)
- Why: worldgen of big legacy packs is slow; C2ME exposes thread-safety bugs.
- Approach: JFR during a fixed chunk pregen (server, 20x20 chunks) with the `mixed` suite; fix the top 3 hotspots (name lookups, generation-region reads, per-chunk allocations). Keep O(1) lookups; no global locks.
- Verify: the suite's chunk time metric before/after; suites PASS.

## Host versions

### H1. NeoForge 1.21.4+ readiness inventory (M) TAKEN
- Why: the roadmap requires following newer hosts. Nobody knows how much of the runtime touches APIs that changed after 1.21.1.
- Approach: list every Minecraft/NeoForge symbol the runtime (not the shims' legacy side) uses, diff against 1.21.4 mappings (renamed, removed, changed signature), group by subsystem, and estimate. Write `docs/wiki/Host-1.21.4-Estimate.md`. No code changes.
