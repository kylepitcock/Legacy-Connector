# Work plan

How the project makes large steps instead of fixing one report at a time. Written 2026-11-13, after release 0.4.14; update it when a
track finishes or the priorities change. The order of legacy versions stays as in the [Version roadmap](Version-Roadmap.md).

## What the last issue batch showed

Thirteen reports were closed in one day, and almost none of them was about the mod named in its title:

| Report | Actual defect | Scope |
|---|---|---|
| Mo' Pickaxes has no mining speed | no 1.12.2 tool had a mining speed or harvest level | every 1.12.2 tool |
| SRP turns villagers into farmers | building a legacy view of an entity wrote legacy defaults into the real entity | any mod with an entity-join handler |
| Tinkers' + Cynosure cannot load a world | a replaced loot table had no id | any 1.12.2 mod with loot tables next to a mod that copies tables |
| Traincraft is disabled | one bytecode rewrite did not raise the stack limit | any jar using that pattern |
| Ancient Warfare modules fail | a real library jar was not recognised, so a stand-in was linked too | every 1.12.2 user of that library |
| FutureMC fails at construction | 283 event classes had no no-arg constructor | every event class |

So the unit of work is the **defect class**, not the mod. Two things limit progress: defects are only found when a player reports them,
and a fix cannot be confirmed without a person (or the lead session) driving a client.

## Tracks

### 1. Verification that needs nobody (highest leverage)

Every fix of the last batch shipped after build checks and one manual client session. That does not scale and it stops workers from
verifying their own changes.

- `tools/dev/verify.sh`: named suites (a mod set per legacy version from the local, gitignored jar folders), each run as a fresh headless
  server self test in its own game directory, the result lines parsed (mods failed, block / entity / placement / silent-failure tests,
  crash reports) and compared with a committed baseline that may only improve. A missing jar skips the suite, it does not fail it.
- Client scenarios next to it: a scenario file is a list of control verbs, JavaScript probes with an expected answer, and screenshots
  (open the Thaumonomicon, fire a bow, open three machine screens, read a villager's profession: the checks done by hand for 0.4.14).
- Rule once it exists: a merge runs the suites touched by the change; a worker may run a suite in its own game directory.

### 2. Find defect classes before players do

- **Audits by pattern.** Each class found once is searched for everywhere: view constructors that write into the native object,
  modern objects created without their identity (ids, registry names, holders), render-type and sound-type fallbacks, rewrites that
  change stack depth or frames.
- **Triage by class.** The open issues (86 on 2026-11-13) are sorted by the defect class behind them, with the reported version, so the
  classes with the most reports are worked first and reports fixed since their version are closed.
- **Offline probes stay the first filter** (`checkLegacyMod`, the weekly probe, `ResourceCheck`), extended whenever a class can be detected
  without the game (the bytecode verifier caught the Traincraft regression in seconds; it now runs for every jar).

### 3. Generic features that many mods wait for

In the order of how many reports depend on them:

1. 1.12.2 mod fluids as real fluids with buckets (fluid blocks, `FluidRegistry.addBucketForFluid`, `UniversalBucket`, `FluidUtil`).
2. 1.12.2 models from mod loaders and baked-model events (Betweenlands is done; Mekanism, Immersive Engineering, Tinkers' tools,
   EnderIO, AE2 and Extra Utilities 2 are open as #162 to #173): one bridge for `ICustomModelLoader`, `ModelBakeEvent` replacements and
   sprites generated in code, instead of one fix per mod.
3. 1.20.1 mixins whose target methods gained or lost parameters in 1.21.1: adapt the handler signature at translation time where the
   change is known from the migration table, and drop the single injection (not the mod) where it is not.
4. Legacy GUI state (depth, scissor, texture generation): the Thaumonomicon and obelisk fixes showed the emulation still starts from
   modern defaults in places; compare each GL default with 1.7.10's once, in `RenderContext`.

### 4. Releases and data

- Nightly builds carry fixes; stable releases are cut from the Actions tab when a verified batch is ready ([Development](Development.md)).
- The jobs that commit generated data to master (compatibility reports, the weekly probe, the store description) have failed since the
  branch was protected against pushes without a reviewed pull request. Until the maintainer decides how they may write (an exemption for
  the Actions bot, or a separate data branch), the compatibility site shows the data of 2026-11-08.

## Who does what

- **Lead session:** the plan, defect-class analysis, reviews and merges, live debugging in a client, decisions that change behaviour for
  players.
- **Workers** (own worktree, cheaper model, offline verification until track 1 lands): audits, triage tables, features with a written
  plan, shim lists (`tools/dev/agent-tasks`). At most four at a time.
- **Needs the maintainer:** branch protection and bot access, what to do with optional TerraBlender / Pehkui (pull request #242), closing
  one's own issues, and any stable release.
