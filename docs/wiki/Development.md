# Development

Building and testing Legacy Connector. Requires JDK 21 (newer JDKs fail with "Unsupported class file major version"). Back to [Home](Home.md).

Requires JDK 21.

```sh
./gradlew build                 # runtime mod + loader (also builds the distributable jar)
./gradlew test                  # self-contained unit tests; no local mod corpus is required
./gradlew actualModTest         # opt-in JUnit checks against locally supplied original jars
./gradlew bundleJar             # single distributable jar: build/dist/legacyconnector-<ver>.jar, the ONLY jar for a mods folder
                                # (build/libs/*-runtime-only.jar has no loader: every legacy mod is then rejected as built for an old Minecraft)
./gradlew runClient             # dev client, mods from run/mods
./gradlew runServerTest         # headless smoke test with run-servertest/mods; the fixture mod self-tests and stops
LEVELTYPE=biomesoplenty:biomesop tools/dev/run-subset.sh biomesoplenty   # same, in a mod's world type
tools/dev/run-subset.sh hbm thaumcraft     # server test over a few mods only, with the
                                # block/item/entity self tests; the whole corpus takes minutes
./gradlew runClientTest         # client smoke test (run-clienttest, mods linked from run/mods): reports per mod
                                # which models show the missing texture, then quits
tools/dev/run-screenshot-test.sh agricraft appliedenergistics   # graphical test: creates a world (WORLDTYPE=biomesoplenty:biomesop
                                # for BoP), builds block/item showcases, writes screenshots to run-screenshots/shots
SCENARIO=village tools/dev/run-screenshot-test.sh witchery agricraft   # flies over the nearest village (legacy village components)
SCENARIO=gui MODS=rftools tools/dev/run-screenshot-test.sh rftools mcjtylib   # right-clicks each block of the mods and
                                # screenshots the screens that open (custom GUIs); SCENARIO=portal tests a Twilight Forest portal
tools/dev/docker-headless.sh run-screenshot-test.sh ...        # same, inside a Linux container with a virtual display
                                # (Xvfb + Mesa; needs `colima start`), so no window opens on the desktop;
                                # also works for run-client-test.sh
tools/dev/session.sh start <jar fragments>   # one long-lived headless client you drive without restarting it:
tools/dev/session.sh send cmd "legacyconnector dim 7"   # run a command / open GUIs / `mouse x y` / `click x y` / `useitem` / `inv` / `info` ...
tools/dev/session.sh shot name                 # screenshot -> ~/.cache/legacyconnector-docker/work/run-screenshots/control/out/name.png
tools/dev/survey.sh <namespace> <name>         # places every block of a mod on a sky platform and screenshots it
./gradlew checkLegacyMod -PlegacyJar=/path/a.jar,/path/b.jar   # e.g. every jar in test-mods/
                                # translate offline as one modset, list unlinkable references,
                                # generate + JVM-verify the synthesized API (no game launch)
```

Normal `test`, `check` and `build` exclude JUnit tests tagged `actual-mod`. These checks run only through
`actualModTest`; unavailable local fixtures are reported as skipped. This task deliberately does not cache results because
it discovers developer-supplied archives and translated jars dynamically.

The current original-jar checks use the sibling `Minecraft Beta 1.7.3 Mods Archive - 1000+ Mods` directory for audio,
translated Mo' Creatures/More Creeps fixtures under `run-b173-single/` for renderer capture, and
`build/devtools/b1.7.3/client.jar` for Beta model comparisons. None of these files is required by the normal build or shipped
in the repository. The `testmods` fixture jar is generated from the API stubs and Forge/MCP metadata resolved by Gradle;
`build/devtools/legacy-1.7.10.sig` is not a build prerequisite.

In-game self-tests under the runtime sources are compiled normally, but execute only when their self-test flag is enabled.
The explicit server test tasks enable `legacyconnector.entitySelfTest`; ordinary builds do not launch them.

Development commands (operator, any world): `/legacyconnector mods` lists the loaded legacy mods;
`/legacyconnector dim list|<id> [pos]` switches to a legacy dimension; `/legacyconnector gui <mod> <id> [pos]` opens a mod
GUI by its `IGuiHandler` id; `/legacyconnector place <block> [meta]`, `give <item> [meta] [count]` and `spawn <entity> [count]`
create legacy content with metadata; `/legacyconnector list <blocks|items|entities> <mod>` prints a mod's ids; `/legacyconnector biome <id>` jumps to a legacy biome; `/legacyconnector inspect`
describes the block or entity you look at (legacy class, metadata, tile entity, render type). Ids tab-complete.

Project layout:

| Path | What |
|---|---|
| `loader/` | FML locator service: jar inspection, translation, linking, API synthesis, caching, report |
| `src/main/java/legacyconnector/v1_7_10/` | 1.7.10 shim (legacy package names, relocated) |
| `src/main/java/org/kylepitcock/legacyconnector/` | runtime: bootstrap, bridges to NeoForge, JDK/library compat, synth runtime |
| `testmods/` | fixture mod compiled against legacy API stubs, built like a real 2014 jar |
| `tools/` | developer scripts: reference data download, signature/hierarchy extraction, API lookup, shim hierarchy audit |
| `tools/docker/`, `tools/dev/` | headless-display container image; dev test scripts (subset server test, client/screenshot tests) |
| `test-mods/` | (not committed) local corpus of real legacy mods for `checkLegacyMod` / `runServerTest` |

Reference data (MCP mappings, Forge userdev sources, Mojang client jars) is downloaded into `build/devtools/` by
`tools/fetch_devtools.sh` and the extraction scripts; none of it is committed. Only class names and supertypes
(`hierarchy.txt`) and MCP name tables are shipped.

To regenerate the 1.7.10 hierarchy data:

```sh
tools/fetch_devtools.sh
python3 tools/extract_legacy_signatures.py 1.7.10   # needs build/devtools/mojang/client-1.7.10.jar
python3 tools/extract_legacy_hierarchy.py 1.7.10
```

## Test workflow notes

- `./gradlew compileJava --rerun` is the compile check (compileJava is non-incremental: the shim access widener rewrites class files), then
  `./gradlew generateShimIndex`, which catches "phantom field access" errors: shim code must read legacy fields that exist only as virtual
  fields through the `mcc$get$`/`mcc$set$` accessors.
- `tools/dev/run-corpus.sh [dir]` runs the server self test over a whole directory of jars. Run only one server at a time and never compile
  while one runs.
- Translation results are cached per jar and mod set (`run-*/.legacyconnector/cache`). After changing the loader, linker, stubber or shim,
  delete the cache or the old translation is reused.
- On a Windows desktop (no Docker) `tools/dev/quick-shots.sh` runs the graphical test directly in about a minute (superflat world, short render
  distance, a dozen blocks and items per mod); mods go in `run-screenshots/mods`.
- Headless server probing (no window): `./gradlew runServerSubTest -PautostopTicks=0 -PserverScript=probe -PserverScriptMod=<legacy mod id>` runs a long-lived server that executes
  the `*.js` files dropped into `run-subtest/probe` on the server thread (Nashorn, `core/ServerScript.java`); `tools/dev/server-script.sh -f file.js` submits one and prints the answer
  (globals persist; a function `onTick(n)` runs every tick). `tools/dev/fisk/lib.js` is a library for it: it joins fake clients (a captured connection that answers keep-alives and
  summarises every packet the server sends, so data syncing and custom packets can be checked), puts a hero suit on, drives data vars and interactions and runs multi-tick scenarios
  (`scenario([step, ...])`, a step may return the ticks to wait; `tools/dev/fisk/wait.sh` waits and prints). The `probe-*.js` and `*-sweep.js` files there are the Fisk's Heroes
  ability checks (damage, beams, web, shield throw, sizes, gravity, healing, recipes, machines, items). A fake client never moves its chunk view by itself: `tp()` tells the chunk map.
  `tools/dev/fisk/probe-client-keys.js` is the client-side counterpart (run with `tools/dev/js.sh -f` in a control session wearing the Mk50 chest piece): it records, per client tick, every change of the worn suit, the nanites data, sprint/sneak state, the ability keys and the key hint list the HUD draws, and `FLK.report()` prints them (the key hint flicker and the dead Iron Man blade/cannon of issue #118).
- Live probing: start a session with `SCENARIO=control tools/dev/quick-shots.sh`, then `tools/dev/js.sh '<JavaScript>'` evaluates in the running
  game (bindings `mc`, `server`, `log`; any class via `Java.type(...)`).
- Headless server probing: `./gradlew runServerSubTest -PserverTickScript=<file.js> [-PserverTickScriptFrom=200]` evaluates the file on every server tick
  (bindings `server`, `tick` counted from the first run, `log`; globals persist, so place on one tick and read on the next); output is logged as
  `SERVERSCRIPT:`. Example: count a mod's blocks per height over the loaded overworld chunks.
  `tools/dev/server-scripts/lib.js` (prepend it: `cat lib.js script.js > run.js`) adds `realPlayer(name)` (a real, non-fake `ServerPlayer` on an embedded
  channel, so mods that skip fake players keep progression data), `placeTile` (right-click placement with the item), `P(label, fn)` (catch + short stack) and
  `legacy(...)`/`lworld(...)` wrappers; `chromaticraft-*.js` there are worked examples (research walk and a finished casting-table cast, dimension 60 stay, machines).
- `tools/dev/thaumcraft/lib.js` (fake player, `scanAt` = what the Thaumometer computes for a block) and `scan-thaumometer.js` (real `ItemThaumometer.doScan` on placed blocks, PASS/FAIL per block) run on a `-PserverScript=run-subtest/probe -PserverScriptMod=thaumcraft` server (submit `lib.js` first; the directory is relative to `run-subtest`). Place blocks inside chunk 0,0 only: a block in an unloaded chunk makes the script block the server thread on chunk generation.
  `SERVERSCRIPT:`. Example: count a mod's blocks per height over the loaded overworld chunks. Ready-made gameplay rigs live in `tools/dev/server-scripts`
  (`lib.js` places items through a fake player's `useOn`, forceloads the area and reads tiles/fields; ElectriCraft generator -> wire -> motor/battery/RF cable
  chains and ReactorCraft fission reactor, steam turbine -> shaft and radiation rigs). Gotchas: `forceload add` takes block coordinates, not chunk coordinates
  (otherwise the rig unloads after one tick); legacy mods that kill entities outside y 0..256 (DragonAPI particle entities such as ReactorCraft neutrons) need
  rigs at y >= 0; calling methods on a tile whose class mentions an absent optional API (BuildCraft) from Nashorn throws `NoClassDefFoundError`, so read fields via `f()`.
- Gameplay scenarios without a window: `tools/dev/run-server-script.sh <scenario> [autostopTicks] [firstScriptTick] [--fresh]` runs a file of
  `tools/dev/server-scripts/scenarios` with the helpers of `lib.js` (platform, placement through the real item use with a fake player, field access,
  a task scheduler) and `rig.js` (engine, shafts, machine in a line, also machines fed from below through a bevel gear). The RotaryCraft set builds
  real engines, gearboxes, clutches and machines, powers them and asserts power, fluids, items, GUIs (containers and shift-click), capabilities
  (items, fluids, RF), persistence across a restart (`persist`, run twice) and the handbook data. Jars go in `run-subtest/mods/1.7.10`.
- Legacy mod jars are supplied by the developer and never committed (`test-mods/` is gitignored).

## Regression suites

`tools/dev/verify.sh [--update] [suite ...]` runs the headless server self test unattended over named jar sets and compares the result with a
committed baseline (no argument runs every suite; about 2 to 5 minutes per suite, 15 minutes at most).

- **Suites** are `tools/dev/verify/suites/<name>.txt`, one `<version-folder> <jar-name-fragment>` per line (`1.7.10`, `1.12.2`, `1.6.4`, `1.5.2`,
  `1.4.7`, `1.20.1`, `b1.7.3`; the fragment is matched case-insensitively against jar file names; `#` starts a comment). Use a version in the
  fragment when other versions ship the same mod name (`CodeChickenCore-1.7.10`).
- **Jars** are never downloaded or committed. They are looked up (first match wins) in the gitignored folders of the main checkout
  (`JAR_ROOT`, default `Y:/Programming/Minecraft/Retrofit`: `test-mods*`, `run-subtest/mods`, `run-screenshots/mods`, ...; the list is `SEARCH` in the
  script). A suite with a missing jar is reported as SKIP with the missing fragment, not as a failure.
- **Run:** each suite gets a fresh game directory `run-verify/<suite>` (mods copied into `mods/<version>/`, eula and `server.properties` taken from the
  main checkout's `run-subtest` with a free port, no world, no translation cache) and runs `./gradlew runServerSubTest -PsubTestDir=... -PautostopTicks=700`
  (`AUTOSTOP`, `SUITE_TIMEOUT` override). Only the server of this checkout is killed on a timeout (`verify/kill-server.ps1`).
- **Metrics** are parsed from `logs/latest.log` by `tools/dev/verify/verify.py` (`python verify.py parse <log> <crash-dir>`; unit tests:
  `python -m unittest discover -s tools/dev/verify`): failed mods, block self test (placed, still there, with problems), entity self test, silent
  failure checks, 1.12.2 item placement, legacy content counts, crash reports, `VerifyError` count and whether the block self test was reached.
- **Baseline:** `tools/dev/verify/baseline.json`. A suite FAILS when a "bad" metric (failed mods, problems, failed or unspawned entities, silent
  failures, crash reports, VerifyErrors) is higher than the baseline, when a "good" metric (blocks still there, entities alive, placed items, content
  counts) is more than 2 percent lower (10 percent for entities alive, which varies between identical runs), or when the self test was not reached. A suite without a baseline entry is NEW. After an intended change run
  `tools/dev/verify.sh --update [suite]` and commit `baseline.json`. The table is also written to `build/devtools/verify-report.txt`; the exit code is
  non-zero when a suite failed.

## Client scenarios

`tools/dev/verify-client.sh [scenario ...]` runs scripted checks in a real client (a game window opens for a few minutes per scenario; one client at a
time; no argument runs every scenario). Scenarios are `tools/dev/verify/scenarios/<name>.txt`:

```
mods: 1.7.10 Thaumcraft-1.7.10        one line per jar: <version-folder> <jar fragment>, looked up like the server suites (SEARCH in verify/client.py)
world: minecraft:flat                 optional (default flat)
cmd /give @s thaumcraft:itemthaumonomicon   any RemoteControl verb as is (cmd, wait n, slot n, look yaw pitch, use, useitem, holduse n, ...)
expect LegacyScreen :: mc.screen == null ? "none" : mc.screen.getClass().getName()
shot tc4-book                         screenshot
logabsent StackOverflowError          fails when logs/latest.log contains the pattern
```

`expect <regex> :: <JavaScript>` evaluates the JavaScript in the game (`js` verb, bindings `mc`, `server`, `log`, Nashorn: numbers come back as `15.0`)
and passes when `re.search(regex, answer)` matches; it is retried for 15 s. A step whose answer starts with `error:`, `unknown command` or
`use: no block` fails too. A missing jar gives SKIP; the first failing step gives `FAIL step line N '<step>': <answer>` and a `failure.png`.

The runner (`tools/dev/verify/client.py`, unit tests `python -m unittest discover -s tools/dev/verify`) prepares `run-verify-client/<scenario>`
(gitignored: jars in `mods/<version>/`, the jars of the main checkout's `run-perfmods/` except Iris Veil Compat, which needs the Veil library; `NOPERF=1`
leaves them out; no translation cache, no world), starts `tools/dev/quick-shots.sh` with `SCENARIO=control`, polls until the player is in the world
(8 minutes), runs the steps through `control/in/*.cmd`, then quits the game and kills only the java process of this checkout and game directory
(`verify/kill-client.ps1`). Results: `build/devtools/verify-client/<scenario>/` (screenshots, `latest.log`, `gradle.log`, and `report.txt` one level up);
exit code non-zero when a scenario failed. Shipped: `tc4-book`, `tc4-bow`, `villager-professions`, `fluid-bucket`, `te147-gauge`. The player spawns in
the air at y=150: scenarios that need ground fill a stone floor at y=148 and `tp @s 0.5 150 0.5` first.

## Silent-failure checks

The server self test (`-Dlegacyconnector.entitySelfTest=true`, on in `runServerSubTest`) also runs `core/SilentFailureChecks` once at server start. They are read-only and target bugs that crash nothing and log nothing, which link and translation checks cannot see (Sinytra Connector's "attribute registration bypassed, SWIM_SPEED stayed 0, nobody can swim" class). Output: `SILENT FAILURE CHECKS: N passed, M failed`, then one `SILENT FAILURE CHECK FAILED: <check>: <offenders by registry name>` line at ERROR level per failing check (`tools/dev/run-subset.sh` prints them). Add `-Dlegacyconnector.selfTestFailFast=true` to halt the JVM with exit code 3 on any failure.

What is asserted:

- Attributes: the default attribute map of every legacy living-entity host type of both versions (and the player map) exists and has a positive base value for max health, movement speed, step height, gravity, safe fall distance, fall damage multiplier, scale, NeoForge `SWIM_SPEED` and `NAMETAG_DISTANCE`, plus follow range and attack damage for mobs (attack damage and block break speed for the player).
- Network: no `legacyconnector` payload is registered for the configuration phase (login-phase payloads are not possible through NeoForge's registrar; a handler in either early phase would break proxies and vanilla-compatible joins), the play-phase `LegacyPayload` is registered, encoding a `LegacyPayload` twice gives identical bytes (NeoForge's packet splitter may run an encoder more than once), encoding does not mutate the payload, and decode(encode(p)) equals p.
- Registries: every host entity type is registered under its host id; every non-vanilla block has a default state, every non-vanilla item is not air and has a non-empty default stack, block items place registered blocks, and every non-vanilla sound event resolves to its own location.

Not covered: music and sound playback (client only; a global hook that cancels or swallows sound events cannot be seen on a dedicated server), proxy login (Velocity modern forwarding needs a real proxy), client rendering (missing models, renderers, textures; see the screenshot tests).

## Link probe (automated compatibility results)

Like the Sinytra Connector checker, `tools/compat/probe.py` translates mod jars offline, without starting the game, and records whether every
reference links:

```
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
python3 tools/compat/probe.py test-mods2 --mc 1.7.10 --version 0.3.18      # --dry-run to preview, --json out.json keeps the raw records
python3 tools/compat/probe.py --from-json out.json --mc 1.7.10             # convert an earlier run
```

- `--mc 1.20.1` (Forge 1.20.1 jars) runs `./gradlew gap1201` instead (no shim to link against: the jar is renamed like the layer does and every reference is resolved against Minecraft 1.21.1 + NeoForge + the other jars) and records the same kinds; corpus: `tools/compat/corpus1201.py test-mods-1201`, ranking: `tools/compat/gap1201_rank.py`, results in [Legacy-1.20.1-Corpus](Legacy-1.20.1-Corpus.md).
- It runs `./gradlew checkLegacyMod -PlegacyJar=... -PcheckJson=<file>` on all jars as ONE mod set (mods that extend each other link), then
  merges the per-jar records into `docs/data/mods.json` with source `probe`.
- `links` (every reference resolves; missing legacy API is synthesized) becomes status `loads`, `unresolved` becomes `fails` with the first
  unresolved references in the note. Engine patches (`obsolete`) and library jars without a mod are not listed.
- A probe proves only that a jar translates and links, never that it works in game. The site ranks it below player reports, maintainer, client
  and startup-test results, and the script never overwrites those. Re-running refreshes earlier probe entries.
- Every run is also appended to `docs/data/probe.json` (counts per result plus the per-mod list of the last 10 runs per version), which the
  pie chart page `docs/probe.html` ("Automated results" on the compatibility site) shows with a run selector and a history table.
- **Jar sources.** `tools/compat/fetch_probe_mods.py <mc> <dir> [--limit N]` downloads the most downloaded Forge mods of a version from Modrinth's
  public API (SHA-1 checked). The jars are only for the probe: keep them in a gitignored or temporary folder, never commit or redistribute them.
- **CI.** `.github/workflows/probe.yml` runs fetch + probe for 1.7.10 and 1.12.2 on every published release, weekly and on demand
  (`workflow_dispatch`, input `limit`), then commits `docs/data/mods.json` and `docs/data/probe.json`. The jars exist only on the runner.
  Local runs against your own corpus (`test-mods2`) work the same way, only the committed results matter.

## Keeping the core mod-agnostic

Fixes are generic first (see `CLAUDE.md`). `tools/dev/check_mod_specific.py` enforces it: it scans the Java sources (comments ignored) for the
names in `tools/dev/mod-names.txt` and fails when one appears outside the designated places, which are any `compat/` package and the shim of
another mod's API (`legacyconnector/v*/codechicken`, `cofh`, `baubles`, `thaumcraft`, ...). Mentions that existed when the check was added are frozen
in `tools/dev/mod-specific-baseline.txt` per file and can only go down: `python3 tools/dev/check_mod_specific.py --update` lowers the numbers after you
remove some. A new mod-specific fix goes under a `compat/` package, labelled and keyed by mod id, so it stays easy to remove. If outside contributors
start sending per-mod fixes, the next step is a plugin interface plus a separate optional jar for them (how Sinytra Connector does it with ConnectorExtras).

## Bytecode check (VerifyError smoke test without the game)

`checkLegacyMod` also runs `loader/.../tools/BytecodeCheck` on every translated jar: ASM's data-flow analyzer over each method, with type relations answered by
`Hierarchy` (translated mod, shim, synthesized API, libraries). It finds what the JVM rejects as a `VerifyError` when the class first loads (a retyped value
used as the wrong type); methods touching a type nobody knows (a mod that is not installed) are skipped. The line `bytecode verification: N method(s)
analysed, M failing` is printed per jar, and failures go into the JSON record as `verifyFailures` / `verifyErrors`.

## Resource check (rendering smoke test without the game)

`./gradlew checkLegacyMod` (and so `tools/compat/probe.py`) also runs `loader/.../tools/ResourceCheck` on the converted assets of every jar, against the
jars of the same set and the vanilla 1.21.1 client jar (found in the Gradle cache, or `-Dlegacyconnector.check.vanillaJar=...`; switch off with
`-Dlegacyconnector.check.assets=false`). It finds the references that point at nothing, which is how nearly every purple-and-black texture and invisible
block starts: a model's `parent`, a literal texture, an item model override, a blockstate variant's model, and an element face's `#variable` that the parent
chain never defines (checked only for models a blockstate or the item folder uses directly, since templates are completed by their children); plus JSON that
does not parse and PNGs without a readable header.

Every problem gets a verdict so the layer is not blamed for the mods' own defects: **layer** (the original jar ships the file, or it is a vanilla file the
rename tables miss, but it is not there after translation) and **mod** (no original jar ships it, so the game showed it missing in 1.12 too). References into
the namespace of a mod that is not in the set are ignored (optional integrations). The probe record keeps both counts and the first examples (`[layer]` ones
first); the site shows them in the run summary and in each mod's detail, and `tools/compat/missing_report.py` lists the mods that lose the most.

What it cannot see: what a texture looks like, animations, lighting, translucency order, shaders, anything drawn by code, and 1.12 Forge-format blockstates
(translated at runtime, so they are skipped; blockstate models in the vanilla namespace that the runtime generates can show up as possible layer problems).
For those use the headless client tests (`tools/dev/survey.sh`, `quick-shots.sh`).


## Mod survey (per-mod evidence from a real client)

`tools/survey/survey.py` turns mods that are only rated "loads" (they translate offline, nobody looked at them in the game) into evidence-based ratings. Design goal: roughly 5 seconds per mod, so the whole set (about 1,400 jars on 1.7.10 and 1.12.2) fits in two hours.

- **Batches.** One real client (the `runScreenshotTest` control session, perf mods included, `--noperf` opts out) loads 30-60 jars at once into one superflat world; the translation cache is reused between batches. A batch that never reaches the world is bisected down to the offending jar (rated "fails", with the log line). Library jars listed in `tools/survey/libs.txt` are added to every batch. A batch is capped (`--budget`, default 8 minutes of staging, `--load-timeout`); mods not reached are "unknown" and retried by the next run.
- **Bulk scripts.** `enumerate.js` reads the block, item, entity, biome and dimension registries once per batch (attributed by namespace; the namespace-to-jar map comes from `legacyconnector-report.txt`). Per mod, `showcase.js.tpl` runs once on the server thread: it clears the stage, places up to 27 sampled blocks in a grid (exceptions are counted) or spawns up to 12 entity types in a ring, then a control `shot` follows. All commands of a batch are queued at once as control files, so there are no per-item waits.
- **Rating.** From the log scan per namespace (missing texture/model lines, exceptions with the mod's packages in the stack, load failures), placement and spawn exceptions, and a cheap image check against the empty stage (share of changed pixels, share of missing-texture magenta). `works` = content present and no evidence of trouble; `partial` = any problem; `fails` = load failure or the batch crash isolated to this jar; `loads` = no content of its own (nothing to show); `unknown` = not reached. Results are one JSON per jar in `run-survey/results/<mc>/`, JPEG q90 shots in `run-survey/shots/<mc>/` (both gitignored).
- **Commands.** `python tools/survey/survey.py run --mc 1.12.2 --jars "<dir>" [--group 40]` (resumable: jars with a result are skipped; run it again in a loop for unattended use), `stats`, and `merge [--dry]` which adds source "client" rows to `docs/data/mods.json` (never touching maintainer or player entries; "loads" results add nothing). It waits for a free game slot (`--max-games`, default 2 machine-wide) and kills only its own client.
`tools/survey/` turns mods that are only rated "loads" (they translate offline, nobody looked at them in the game) into evidence-based ratings, unattended and without an agent. Goal: about 5 seconds per mod, so all jars (1.12.2, then 1.7.10) fit in a couple of hours.

- **Start**: `python tools/survey/survey.py run --all` starts a fully detached background process (survives closing the terminal and the agent session) and returns at once. It loops over all 1.12.2 jars (`1.12.2 test mods`, `test-mods-1122`, `run-subtest/mods/1.12.2`) and then the 1.7.10 jars (`test-mods*`), skips jars that already have a result, restarts crashed or hung games itself (hard per-batch time limits), kills only its own client (by exact PID; the command line carries `run-survey-main`), waits politely while other agents already run 3 games (`--max-games`), and exits when done (file `DONE`). A single instance only (`lock`).
- **Stop / progress**: `python tools/survey/survey.py stop` writes the `STOP` file the driver polls (delete it before starting again); `status` prints `heartbeat.json` (updated every 10 s: phase, jars done, jars/min, own game PIDs), `progress.log` is the driver's log, `timing.csv` the batch timings.
- **Where things land** (`run-survey-main/` beside the main checkout, gitignored): `results/<mc>/<jar>.json` (raw evidence per jar), `shots/<mc>/` (JPEG q90 per mod), `client-rows.json` (the rated rows), `game/` (the client's run directory and `logs/latest.log`, `last-batch.log` the previous batch's log). The scripts are copied to `run-survey-main/tools` at start, so removing a worktree does not stop a run.
- **Apply**: `python tools/survey/apply.py [--all|--mc 1.12.2] [--dry]` rates the evidence and merges source "client" rows into `docs/data/mods.json` (idempotent; the driver runs it every ~50 jars and at the end; maintainer and player entries are never touched; "loads"/unknown results add nothing). Rating: `works` = content present and nothing wrong; `partial` = a placement or spawn threw, missing texture/model or error log lines for the mod, missing-texture magenta or nothing visible in its shot; `fails` = a load phase failed with nothing to show, or the jar alone crashes the client before the world; `crashes` = the client crashed while showing its sample.
- **Method.** One real client (the `runScreenshotTest` control session, perf mods included, `--noperf` opts out) loads 30-60 jars at once into one superflat world, the translation cache is reused between batches, and a batch that never reaches the world is bisected down to the jar. Library jars listed in `libs.txt` are added to every batch. `enumerate.js` reads the registries once per batch (namespace to jar from `legacyconnector-report.txt`); per mod `showcase.js.tpl` runs once on the server thread (clears the stage, places up to 27 sampled blocks or spawns up to 12 entity types) and one control `shot` follows; all commands of a batch are queued as control files, so there are no per-item waits. The latest.log is scanned per namespace (missing texture/model lines, exceptions with the mod's packages in the stack) and each shot is compared with the empty stage (changed pixels, missing-texture magenta).
