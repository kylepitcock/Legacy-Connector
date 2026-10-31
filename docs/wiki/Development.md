# Development

Building and testing Legacy Connector. Requires JDK 21 (newer JDKs fail with "Unsupported class file major version"). Back to [Home](Home.md).

Requires JDK 21.

```sh
./gradlew build                 # runtime mod + loader (also builds the distributable jar)
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
- Live probing: start a session with `SCENARIO=control tools/dev/quick-shots.sh`, then `tools/dev/js.sh '<JavaScript>'` evaluates in the running
  game (bindings `mc`, `server`, `log`; any class via `Java.type(...)`).
- Legacy mod jars are supplied by the developer and never committed (`test-mods/` is gitignored).
