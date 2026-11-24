# Diagnostic switches

Back to [Home](Home.md). The user-facing settings are in [Configuration](Configuration.md); this page lists the JVM system properties (`-Dname=value` in the launcher's JVM arguments, or `systemProperty` in a Gradle run) that switch on diagnostics, drive tests, or override a setting for one run. None of them is needed to play.

How they behave:

- Every switch is declared once, with its type, default, category and description: `org.kylepitcock.legacyconnector.core.flags.Flags` for the runtime (switches that name a specific mod are in `compat/CompatFlags`), `org.kylepitcock.legacyconnector.loader.LoaderFlags` for the loader. The loader runs before the runtime exists and cannot see its classes, so it has its own registry of the same shape; the properties the loader sets for the runtime are declared in both.
- **flag**: true only for the text `true` (`Boolean.getBoolean`). **toggle**: on unless the text is `false`. **int** and **long**: a number, the default when the text is not one. **string** and **path**: the text; "unset" as default means the feature is off until a value is given.
- Most switches are read when the class that uses them loads, so give them on the command line, not at run time. A few are read on every use (`mc125.clientCallbacks`, the script switches) and can be changed from a script or a test.
- At start the log has one line, `Legacy Connector switches set to a non-default value: name=value, ...` (and one for the loader's), so a log shows how a run was configured. The internal switches below are left out of it.
- The internal switches are how the loader hands information to the runtime; do not set them by hand.
- To add a switch: add one constant to the registry, read it through the constant (`Flags.MY_SWITCH.on()`), and run `SwitchRegistryTest` with `SWITCH_DOCS_UPDATE=1` to rewrite the table below. The test fails when a `legacyconnector.*` property is read or set anywhere else, or when this page is out of date.

<!-- switches:begin (generated from Flags, CompatFlags and LoaderFlags; run SwitchRegistryTest with SWITCH_DOCS_UPDATE=1 to rewrite) -->

134 switches. "Read by" says which half of the layer reads (or, for internal ones, sets) the property.

### Debug (25)

| Switch | Type | Default | Read by | What it does |
|---|---|---|---|---|
| `legacyconnector.bufferDiag` | flag | `false` | runtime | 1.20.1: diagnostics in the compat buffer builder. |
| `legacyconnector.coremodGuards` | toggle | `true` | runtime | false switches off the guards around coremod hook calls (for timing). |
| `legacyconnector.coremodHooks.report` | string | `unset` | runtime | full: the coremod hook discovery prints every site instead of a summary. |
| `legacyconnector.debug.palladiumFlight` | flag | `false` | runtime | 1.20.1 Palladium compat: logs the flight key handling. |
| `legacyconnector.debug.palladiumRendererRegistry` | flag | `false` | runtime | 1.20.1 Palladium compat: logs the renderer registry calls. |
| `legacyconnector.debugAsm` | string | `unset` | runtime | 1.12.2: logs the annotations found on classes whose name contains this text. |
| `legacyconnector.debugAssets` | string | `unset` | runtime | 1.12.2: dumps the asset translation of the item or block with this path. |
| `legacyconnector.debugIcons` | string | `unset` | runtime | 1.7.10: logs icon lookups of block classes whose name contains this text. |
| `legacyconnector.debugItemBounds` | flag | `false` | runtime | Logs the inventory bounds of each block item handler once (also on with the LC_DEBUG environment variable). |
| `legacyconnector.debugItems` | string | `unset` | runtime | 1.12.2: any value logs the mod items registered at start. |
| `legacyconnector.dumpQuads` | flag | `false` | runtime | Dumps the quads captured from legacy block renderers. |
| `legacyconnector.entityDataDiag` | flag | `false` | runtime | 1.20.1: logs every synched entity data id a mod defines, with the caller. |
| `legacyconnector.failFast` | flag | `false` | runtime | A legacy mod that throws in a lifecycle phase aborts the load with its exception instead of being logged and skipped. |
| `legacyconnector.genTiming` | flag | `false` | runtime | 1.12.2: logs the time each legacy generator takes per chunk (a line every 20 s while chunks generate). |
| `legacyconnector.keepDatagen` | flag | `false` | loader | 1.20.1: keeps the datagen code of a jar in place to measure its unresolved references. |
| `legacyconnector.mc125.unresolved` | path | `unset` | loader | 1.2.5: writes the unresolved names of the jar translation to this file. |
| `legacyconnector.memoryLog` | flag | `false` | runtime | Every 10 s heap and direct memory, every 30 s a per-level entity and chunk census in the log. |
| `legacyconnector.msptLog` | int | `0` | runtime | Logs mean, median, 99th percentile and maximum server tick time and the heap every N ticks (0 = off). |
| `legacyconnector.particleDebug` | flag | `false` | runtime | Logs which legacy particle classes spawn near the camera. |
| `legacyconnector.placementDiag` | string | `unset` | runtime | Any value: the 1.12.2 placement self test adds the legacy checks to every failure line. |
| `legacyconnector.realLevelGuard` | flag | `false` | runtime | Guards against legacy code reaching the real ServerLevel from generation threads (also on with legacytest.autostop). |
| `legacyconnector.skyDiag` | flag | `false` | runtime | 1.7.10 client: logs which legacy sky, cloud and weather renderers a dimension uses and the camera they get. |
| `legacyconnector.soundDebug` | flag | `false` | runtime | Logs every legacy sound handed to the sound engine. |
| `legacyconnector.traceHandlers` | string | `unset` | runtime | Part of a legacy event handler name: logs the first 60 runs of matching handlers. |
| `legacyconnector.traceMessages` | flag | `false` | runtime | Logs the first 400 legacy network messages as they arrive. |

### Test (55)

| Switch | Type | Default | Read by | What it does |
|---|---|---|---|---|
| `api.docs.update` | flag | `false` | runtime | Unit test rewrites the generated support matrix in docs/wiki/Addon-API.md (same as API_DOCS_UPDATE=1). |
| `legacyconnector.assetsClientProxies` | string | `unset` | runtime | Comma list of 1.12.2 client proxy classes the server self test runs first, so their blockstate mappers and model registrations exist. |
| `legacyconnector.assetsSelfTest` | string | `unset` | runtime | Any value: a dedicated server generates the client assets anyway (1.12.2, Beta 1.7.3) so the self test can check them. |
| `legacyconnector.autostopTicks` | int | `0` | runtime | Stops the server after this many ticks (0 = never). |
| `legacyconnector.b173.actualPlasticCraftEntities` | flag | `false` | runtime | Beta 1.7.3 actual-mod entity fixtures are present (unit test). |
| `legacyconnector.b173.vanillaJar` | path | `build/devtools/b1.7.3/client.jar` | runtime | Beta 1.7.3 vanilla client jar used by unit tests. |
| `legacyconnector.beta.combatArtifact` | path | `unset` | loader | Translated artifact a Beta provenance unit test inspects. |
| `legacyconnector.beta.combatShimIndex` | path | `unset` | loader | Compiled shim index a Beta provenance unit test needs. |
| `legacyconnector.betaEcologySelfTest` | flag | `false` | runtime | Beta 1.7.3 ecology self test (needs entitySelfTest). |
| `legacyconnector.betaSpawnSchedulingControlTest` | flag | `false` | runtime | Spawn scheduling self test also keeps spectator actors eligible (control run). |
| `legacyconnector.betaSpawnSchedulingSelfTest` | flag | `false` | runtime | Beta 1.7.3 spawn scheduling self test (needs entitySelfTest). |
| `legacyconnector.betaSpawnSchedulingTestTicks` | int | `300` | runtime | Duration in ticks of the spawn scheduling self test (at least 80). |
| `legacyconnector.boundsSelfTest` | flag | `false` | runtime | Beta 1.7.3 entity bounds self test (also part of the entity self test). |
| `legacyconnector.clientSelfTest` | string | `unset` | runtime | Any value: client smoke test on the title screen (missing textures and models per mod), then quits. |
| `legacyconnector.dimTestRadius` | int | `2` | runtime | Radius in chunks of the dimension self test scan. |
| `legacyconnector.entitySelfTest` | flag | `false` | runtime | Runs the headless server self tests (blocks, items, entities, dimensions, world generation, placement) after the server starts, and enables the per-level census. |
| `legacyconnector.hotdogPersistenceSelfTest` | flag | `false` | runtime | Beta 1.7.3 roster persistence self test (also part of the entity self test). |
| `legacyconnector.joinSizeTest` | flag | `false` | runtime | Logs the size of the join payload (also part of the entity self test). |
| `legacyconnector.libsDir` | path | `unset` | loader | Folder with the bundled-library layout used instead of the embedded resources. |
| `legacyconnector.naturalSpawnTickSelfTest` | flag | `false` | runtime | Beta 1.7.3 natural spawning tick self test. |
| `legacyconnector.naturalSpawnTickTestTicks` | int | `unset` | runtime | Duration in ticks of the natural spawning tick self test. |
| `legacyconnector.plasticCraftBlocksSelfTest` | flag | `false` | runtime | Beta 1.7.3 actual-mod block self test. |
| `legacyconnector.plasticcraftItemsSelfTest` | flag | `false` | runtime | Beta 1.7.3 actual-mod item self test. |
| `legacyconnector.runtimeClasses` | path | `unset` | loader | Development runs: directory of the compiled runtime classes (coremod patches read them). |
| `legacyconnector.screenshotDir` | path | `<game dir>/shots` | runtime | Where the screenshot test writes its pictures. |
| `legacyconnector.screenshotTest` | string | `unset` | runtime | Any value: runs the screenshot test (world, tour, shots) instead of the normal title screen. |
| `legacyconnector.selfTestFailFast` | flag | `false` | runtime | A failed silent-failure check during the self test throws instead of only being logged. |
| `legacyconnector.serverDevScript` | path | `unset` | runtime | Directory of developer script files for a running server. |
| `legacyconnector.serverScript` | path | `unset` | runtime | Directory of script files a server test runs on startup. |
| `legacyconnector.serverScriptMod` | string | `unset` | runtime | Mod id the server script is limited to. |
| `legacyconnector.serverTickScript` | path | `unset` | runtime | Script file evaluated on server ticks. |
| `legacyconnector.serverTickScriptFrom` | int | `200` | runtime | First server tick the tick script runs on. |
| `legacyconnector.shimIndex` | path | `unset` | loader | Shim index file used instead of the one in the runtime jar. |
| `legacyconnector.shotBlocks` | string | empty | runtime | Comma list of block ids to show. |
| `legacyconnector.shotBlocksPerMod` | int | `24` | runtime | Cap of blocks shown per mod. |
| `legacyconnector.shotCommands` | string | `legacyconnector mods` | runtime | Semicolon list of commands the screenshot test runs. |
| `legacyconnector.shotEntities` | string | `unset` | runtime | Comma list of entity ids to show. |
| `legacyconnector.shotItems` | string | `unset` | runtime | Comma list of item ids to show. |
| `legacyconnector.shotItemsPerMod` | int | `36` | runtime | Cap of items shown per mod. |
| `legacyconnector.shotLoadWorld` | string | empty | runtime | Existing world folder the screenshot test loads instead of creating one. |
| `legacyconnector.shotMetas` | string | `0` | runtime | Block metadata values the block survey shows. |
| `legacyconnector.shotMods` | string | `unset` | runtime | Comma list of mod ids whose blocks and items the screenshot test shows. |
| `legacyconnector.shotRenderDistance` | int | `16` | runtime | Render distance in chunks of the screenshot test. |
| `legacyconnector.shotScenario` | string | `unset` | runtime | Screenshot scenario name (control, title...); setting it also turns the memory log on. |
| `legacyconnector.shotSeed` | long | `20241022` | runtime | World seed of the screenshot test. |
| `legacyconnector.shotServer` | string | empty | runtime | host:port of a remote server the screenshot test joins. |
| `legacyconnector.shotTour` | int | `6` | runtime | Number of stops of the terrain tour. |
| `legacyconnector.shotWorldType` | string | empty | runtime | World type of the screenshot test world (empty = default). |
| `legacyconnector.tileRoundTrip` | string | empty | runtime | place: tile entities with an inventory get a stack and are listed; check: a second run over the same world verifies them. |
| `legacyconnector.worldGenSelfTest` | flag | `false` | runtime | Beta 1.7.3 world generation self test diagnostics. |
| `legacytest.autostop` | flag | `false` | runtime | Marks an unattended test run (the fixture mod stops the server; implies the real-level guard). |
| `mc.corpus` | path | `unset` | loader | Directory of the real 1.2.5 archives the corpus detection test reads. |
| `mc.issue312` | path | `unset` | loader | Directory of the jars of the placement regression test. |
| `mc125.corpus` | path | `unset` | loader | Directory of the real 1.2.5 archives of the bridge corpus test. |
| `shim.client.update` | string | `unset` | runtime | true: unit test rewrites the shim client type baseline when nothing is new; force: also adds new entries. |

### Perf (6)

| Switch | Type | Default | Read by | What it does |
|---|---|---|---|---|
| `legacyconnector.directMemoryGcMb` | long | `1024` | runtime | Direct memory in MB above which the guard requests a garbage collection. |
| `legacyconnector.displayListBake` | toggle | `true` | runtime | false turns off baking legacy display lists into GPU meshes (every call replays the recorded draws again). |
| `legacyconnector.displayListBakeMb` | int | `256` | runtime | Megabytes of GPU geometry the baked display lists may hold in all (more lists are replayed). |
| `legacyconnector.itemLock` | toggle | `true` | runtime | false turns off the render lock that item rendering forces (measurement switch). |
| `legacyconnector.sharedCache` | path | `unset` | loader | Directory of a translation cache shared by several game directories or worktrees (read through on a local miss, published to after a translation); env LEGACYCONNECTOR_SHARED_CACHE is the fallback. |
| `legacyconnector.translationThreads` | int | `0` | loader | Threads translating jars at once (0 = automatic). |

### Compat (14)

| Switch | Type | Default | Read by | What it does |
|---|---|---|---|---|
| `legacyconnector.bridges` | string | empty | loader | Comma list of library bridges to force on (replacement mod does not have to be present). |
| `legacyconnector.forge1201` | string | `unset` | loader | true or false: 1.20.1 hosting on or off, wins over the config key. |
| `legacyconnector.forge1201.softlink` | string | `true` | loader | false: references to missing 1.21.1 symbols fail at link time instead of becoming stand-ins (wins over the config key). |
| `legacyconnector.inPlaceGuard` | toggle | `true` | runtime | false turns off the guard that keeps legacy in-place generators (huts, towers, dungeons) out of modern structures. |
| `legacyconnector.inPlaceGuard.buildMin` | int | `24` | runtime | Build writes (a block into air, water or a plant, a block removed, a block entity) a generator call needs to count as having built something. |
| `legacyconnector.inPlaceGuard.refusedMin` | int | `4` | runtime | Refused writes that make the guard undo a whole generator call that crossed a modern structure. |
| `legacyconnector.keepLegacyOreBackgrounds` | flag | `false` | runtime | Keeps the legacy ore textures as they are instead of replacing their host rock pixels with the modern stone. |
| `legacyconnector.locatable.buildMin` | int | `24` | runtime | Blocks a legacy generator run must build to be recorded as a locatable structure. |
| `legacyconnector.locatable.madeMin` | int | `8` | runtime | Non-natural blocks a legacy generator run must place to be recorded as a locatable structure. |
| `legacyconnector.mc125` | string | `unset` | loader | true or false: 1.2.5 hosting on or off, wins over the config key. |
| `legacyconnector.modernStructures` | toggle | `true` | runtime | false: legacy structure generators keep their 1.7.10 placement instead of becoming modern structure types. |
| `legacyconnector.sideBySideLibraries` | string | `unset` | loader | true or false: side-by-side library versions on or off, wins over the config key. |
| `legacyconnector.structureVillageDistance` | int | `0` | runtime | Chunks (0 to 16) kept between a legacy structure start and a village placement chunk; 0 = off. |
| `legacyconnector.vanilla<mc>` | path | `unset` | loader | Vanilla client jar of a legacy version (key written with the version without dots, for example vanilla1122); default: config coremods.vanillaJar or the launcher's copy. |

### Workaround (2)

| Switch | Type | Default | Read by | What it does |
|---|---|---|---|---|
| `legacyconnector.mc125.clientCallbacks` | flag | `false` | runtime | Switch for A/B checks of 1.2.5 client callbacks (read live). |
| `legacyconnector.renderLock` | flag | `unset` | both | true: legacy renderers take the capture lock (the loader sets it when a jar has a render library with static state). |

### Tool (23)

| Switch | Type | Default | Read by | What it does |
|---|---|---|---|---|
| `gap.assumeNative` | string | `unset` | loader | 1.20.1 gap analyzer: comma list of API package prefixes of libraries assumed to exist natively on 1.21.1. |
| `gap.keepNativeLibs` | flag | `false` | loader | 1.20.1 gap analyzer: also analyzes nested jars that are native library builds. |
| `gap.limit` | int | `400` | loader | 1.20.1 gap analyzer: rows per list in the report. |
| `gap.mc` | string | `1.20.1` | loader | 1.20.1 gap analyzer: Forge version named in the report heading. |
| `gap.shimIndex` | path | `unset` | loader | 1.20.1 gap analyzer: the real shim index, so facade classes get their in-game names. |
| `gap.softlist` | flag | `false` | loader | 1.20.1 gap analyzer: lists the soft-linked references per jar. |
| `gap.source` | string | `1.20.1` | loader | Gap analyzer/transformer: source version of the jars (1.20.1; 1.19.2 runs stage C then D, offline only). |
| `gap.srgOverlay` | path | `unset` | loader | 1.20.1 gap analyzer: second SRG table whose names win (experimental 1.19.2). |
| `gap.translate` | flag | `false` | loader | 1.20.1 gap analyzer: runs the jars through the real 1.20.1 transformer first. |
| `gap.translated.dir` | path | `unset` | loader | 1.20.1 gap analyzer: directory to keep the translated jars in. |
| `legacyconnector.check.assets` | string | `unset` | loader | false: the offline check skips the resource check. |
| `legacyconnector.check.bridges` | string | `unset` | loader | false: the offline check treats the jars as plain mods (no library bridges). |
| `legacyconnector.check.coremods` | string | `unset` | loader | true: the offline check runs the set's coremod class transformers over the jars' classes. |
| `legacyconnector.check.json` | path | `unset` | loader | File the offline check writes its result to as JSON. |
| `legacyconnector.check.keep` | path | `unset` | loader | Directory the offline check keeps the translated jars in. |
| `legacyconnector.check.quiet` | string | empty | loader | -q: the offline check does not print the link report. |
| `legacyconnector.check.vanillaJar` | path | `unset` | loader | Vanilla jar of the offline check. |
| `legacyconnector.check.verify` | string | `unset` | loader | false: the offline check skips the bytecode verification. |
| `legacyconnector.check.verify.fail` | string | `unset` | loader | true: a non-zero exit code when the bytecode verification finds anything (CI). |
| `legacyconnector.probe.sites` | flag | `false` | loader | Coremod hook probe prints only the hook sites, one line each. |
| `legacyconnector.targetNeoForgeVersion` | string | `unknown` | loader | NeoForge version the 1.20.1 API verifier reports against. |
| `survey.sample` | int | `30` | loader | 1.20.1 mixin survey: number of samples per target. |
| `triage.tables` | path | `unset` | loader | 1.20.1 mixin triage: directory with the rename tables. |

### Internal (9)

| Switch | Type | Default | Read by | What it does |
|---|---|---|---|---|
| `legacyconnector.bridgedMods` | string | empty | both | Set by the loader: legacy libraries replaced by a modern mod (they count as loaded). |
| `legacyconnector.coremodHooks` | string | `unset` | both | Set by the loader: files that list the coremod hook sites. |
| `legacyconnector.forge1201.mods` | string | empty | both | Set by the loader: ids of the 1.20.1 mods translated in place. |
| `legacyconnector.legacyApiPackages` | string | empty | both | Set by the loader: packages of the legacy APIs found in the jars. |
| `legacyconnector.legacyApis` | string | empty | loader | Set by the loader: legacy API ids and versions found in the jars. |
| `legacyconnector.mc125.active` | string | `unset` | both | Set by the loader to true once a 1.2.5 jar is hosted. |
| `legacyconnector.missingDependencies` | string | empty | both | Set by the loader: mods with missing dependencies (shown in the load report). |
| `legacyconnector.presentLegacyMods` | string | empty | both | Set by the loader: ids of the legacy mods and APIs present in the pack. |
| `legacyconnector.side` | string | `unset` | both | Set by the loader: SERVER or CLIENT, answered to coremods asking for the side. |

<!-- switches:end -->
