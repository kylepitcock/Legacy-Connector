package org.kylepitcock.legacyconnector.core.flags;

import java.util.ArrayList;
import java.util.List;
import org.kylepitcock.legacyconnector.compat.CompatFlags;

/**
 * Registry of the switches (JVM system properties) of the runtime, one declaration each. Read a switch with
 * {@code Flags.ENTITY_SELF_TEST.on()}; see {@link Switch} for the accessors. The loader has its own registry
 * ({@code org.kylepitcock.legacyconnector.loader.LoaderFlags}) because it cannot see runtime classes; the properties the loader sets for the
 * runtime are declared in both. {@code docs/wiki/Diagnostic-Switches.md} is generated from both.
 */
public enum Flags implements Switch {
    /** Runs the headless server self tests (blocks, items, entities, dimensions, world generation, placement) after the server starts, and enables the per-level census. */
    ENTITY_SELF_TEST("legacyconnector.entitySelfTest", Type.FLAG, "false", Category.TEST, "Runs the headless server self tests (blocks, items, entities, dimensions, world generation, placement) after the server starts, and enables the per-level census."),
    /** Any value: client smoke test on the title screen (missing textures and models per mod), then quits. */
    CLIENT_SELF_TEST("legacyconnector.clientSelfTest", Type.STRING, "unset", Category.TEST, "Any value: client smoke test on the title screen (missing textures and models per mod), then quits."),
    /** A failed silent-failure check during the self test throws instead of only being logged. */
    SELF_TEST_FAIL_FAST("legacyconnector.selfTestFailFast", Type.FLAG, "false", Category.TEST, "A failed silent-failure check during the self test throws instead of only being logged."),
    /** Any value: a dedicated server generates the client assets anyway (1.12.2, Beta 1.7.3) so the self test can check them. */
    ASSETS_SELF_TEST("legacyconnector.assetsSelfTest", Type.STRING, "unset", Category.TEST, "Any value: a dedicated server generates the client assets anyway (1.12.2, Beta 1.7.3) so the self test can check them."),
    /** Comma list of 1.12.2 client proxy classes the server self test runs first, so their blockstate mappers and model registrations exist. */
    ASSETS_CLIENT_PROXIES("legacyconnector.assetsClientProxies", Type.STRING, "unset", Category.TEST, "Comma list of 1.12.2 client proxy classes the server self test runs first, so their blockstate mappers and model registrations exist."),
    /** Stops the server after this many ticks (0 = never). */
    AUTOSTOP_TICKS("legacyconnector.autostopTicks", Type.INT, "0", Category.TEST, "Stops the server after this many ticks (0 = never)."),
    /** Marks an unattended test run (the fixture mod stops the server; implies the real-level guard). */
    LEGACYTEST_AUTOSTOP("legacytest.autostop", Type.FLAG, "false", Category.TEST, "Marks an unattended test run (the fixture mod stops the server; implies the real-level guard)."),
    /** Guards against legacy code reaching the real ServerLevel from generation threads (also on with legacytest.autostop). */
    REAL_LEVEL_GUARD("legacyconnector.realLevelGuard", Type.FLAG, "false", Category.DEBUG, "Guards against legacy code reaching the real ServerLevel from generation threads (also on with legacytest.autostop)."),
    /** Logs the size of the join payload (also part of the entity self test). */
    JOIN_SIZE_TEST("legacyconnector.joinSizeTest", Type.FLAG, "false", Category.TEST, "Logs the size of the join payload (also part of the entity self test)."),
    /** Radius in chunks of the dimension self test scan. */
    DIM_TEST_RADIUS("legacyconnector.dimTestRadius", Type.INT, "2", Category.TEST, "Radius in chunks of the dimension self test scan."),
    /** place: tile entities with an inventory get a stack and are listed; check: a second run over the same world verifies them. */
    TILE_ROUND_TRIP("legacyconnector.tileRoundTrip", Type.STRING, "", Category.TEST, "place: tile entities with an inventory get a stack and are listed; check: a second run over the same world verifies them."),
    /** Any value: the 1.12.2 placement self test adds the legacy checks to every failure line. */
    PLACEMENT_DIAG("legacyconnector.placementDiag", Type.STRING, "unset", Category.DEBUG, "Any value: the 1.12.2 placement self test adds the legacy checks to every failure line."),
    /** Beta 1.7.3 world generation self test diagnostics. */
    WORLD_GEN_SELF_TEST("legacyconnector.worldGenSelfTest", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 world generation self test diagnostics."),
    /** Beta 1.7.3 entity bounds self test (also part of the entity self test). */
    BOUNDS_SELF_TEST("legacyconnector.boundsSelfTest", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 entity bounds self test (also part of the entity self test)."),
    /** Beta 1.7.3 ecology self test (needs entitySelfTest). */
    BETA_ECOLOGY_SELF_TEST("legacyconnector.betaEcologySelfTest", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 ecology self test (needs entitySelfTest)."),
    /** Beta 1.7.3 spawn scheduling self test (needs entitySelfTest). */
    BETA_SPAWN_SCHEDULING_SELF_TEST("legacyconnector.betaSpawnSchedulingSelfTest", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 spawn scheduling self test (needs entitySelfTest)."),
    /** Spawn scheduling self test also keeps spectator actors eligible (control run). */
    BETA_SPAWN_SCHEDULING_CONTROL_TEST("legacyconnector.betaSpawnSchedulingControlTest", Type.FLAG, "false", Category.TEST, "Spawn scheduling self test also keeps spectator actors eligible (control run)."),
    /** Duration in ticks of the spawn scheduling self test (at least 80). */
    BETA_SPAWN_SCHEDULING_TEST_TICKS("legacyconnector.betaSpawnSchedulingTestTicks", Type.INT, "300", Category.TEST, "Duration in ticks of the spawn scheduling self test (at least 80)."),
    /** Beta 1.7.3 natural spawning tick self test. */
    NATURAL_SPAWN_TICK_SELF_TEST("legacyconnector.naturalSpawnTickSelfTest", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 natural spawning tick self test."),
    /** Duration in ticks of the natural spawning tick self test. */
    NATURAL_SPAWN_TICK_TEST_TICKS("legacyconnector.naturalSpawnTickTestTicks", Type.INT, "unset", Category.TEST, "Duration in ticks of the natural spawning tick self test."),
    /** Beta 1.7.3 roster persistence self test (also part of the entity self test). */
    HOTDOG_PERSISTENCE_SELF_TEST("legacyconnector.hotdogPersistenceSelfTest", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 roster persistence self test (also part of the entity self test)."),
    /** Beta 1.7.3 actual-mod block self test. */
    PLASTIC_CRAFT_BLOCKS_SELF_TEST("legacyconnector.plasticCraftBlocksSelfTest", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 actual-mod block self test."),
    /** Beta 1.7.3 actual-mod item self test. */
    PLASTICCRAFT_ITEMS_SELF_TEST("legacyconnector.plasticcraftItemsSelfTest", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 actual-mod item self test."),
    /** Beta 1.7.3 actual-mod entity fixtures are present (unit test). */
    B173_ACTUAL_PLASTIC_CRAFT_ENTITIES("legacyconnector.b173.actualPlasticCraftEntities", Type.FLAG, "false", Category.TEST, "Beta 1.7.3 actual-mod entity fixtures are present (unit test)."),
    /** Beta 1.7.3 vanilla client jar used by unit tests. */
    B173_VANILLA_JAR("legacyconnector.b173.vanillaJar", Type.PATH, "build/devtools/b1.7.3/client.jar", Category.TEST, "Beta 1.7.3 vanilla client jar used by unit tests."),
    /** Unit test rewrites the generated support matrix in docs/wiki/Addon-API.md (same as API_DOCS_UPDATE=1). */
    API_DOCS_UPDATE("api.docs.update", Type.FLAG, "false", Category.TEST, "Unit test rewrites the generated support matrix in docs/wiki/Addon-API.md (same as API_DOCS_UPDATE=1)."),
    /** true: unit test rewrites the shim client type baseline when nothing is new; force: also adds new entries. */
    SHIM_CLIENT_UPDATE("shim.client.update", Type.STRING, "unset", Category.TEST, "true: unit test rewrites the shim client type baseline when nothing is new; force: also adds new entries."),
    /** Directory of script files a server test runs on startup. */
    SERVER_SCRIPT("legacyconnector.serverScript", Type.PATH, "unset", Category.TEST, "Directory of script files a server test runs on startup."),
    /** Mod id the server script is limited to. */
    SERVER_SCRIPT_MOD("legacyconnector.serverScriptMod", Type.STRING, "unset", Category.TEST, "Mod id the server script is limited to."),
    /** Directory of developer script files for a running server. */
    SERVER_DEV_SCRIPT("legacyconnector.serverDevScript", Type.PATH, "unset", Category.TEST, "Directory of developer script files for a running server."),
    /** Script file evaluated on server ticks. */
    SERVER_TICK_SCRIPT("legacyconnector.serverTickScript", Type.PATH, "unset", Category.TEST, "Script file evaluated on server ticks."),
    /** First server tick the tick script runs on. */
    SERVER_TICK_SCRIPT_FROM("legacyconnector.serverTickScriptFrom", Type.INT, "200", Category.TEST, "First server tick the tick script runs on."),
    /** Any value: runs the screenshot test (world, tour, shots) instead of the normal title screen. */
    SCREENSHOT_TEST("legacyconnector.screenshotTest", Type.STRING, "unset", Category.TEST, "Any value: runs the screenshot test (world, tour, shots) instead of the normal title screen."),
    /** Where the screenshot test writes its pictures. */
    SCREENSHOT_DIR("legacyconnector.screenshotDir", Type.PATH, "<game dir>/shots", Category.TEST, "Where the screenshot test writes its pictures."),
    /** Screenshot scenario name (control, title...); setting it also turns the memory log on. */
    SHOT_SCENARIO("legacyconnector.shotScenario", Type.STRING, "unset", Category.TEST, "Screenshot scenario name (control, title...); setting it also turns the memory log on."),
    /** Comma list of mod ids whose blocks and items the screenshot test shows. */
    SHOT_MODS("legacyconnector.shotMods", Type.STRING, "unset", Category.TEST, "Comma list of mod ids whose blocks and items the screenshot test shows."),
    /** Comma list of block ids to show. */
    SHOT_BLOCKS("legacyconnector.shotBlocks", Type.STRING, "", Category.TEST, "Comma list of block ids to show."),
    /** Comma list of item ids to show. */
    SHOT_ITEMS("legacyconnector.shotItems", Type.STRING, "unset", Category.TEST, "Comma list of item ids to show."),
    /** Comma list of entity ids to show. */
    SHOT_ENTITIES("legacyconnector.shotEntities", Type.STRING, "unset", Category.TEST, "Comma list of entity ids to show."),
    /** Cap of blocks shown per mod. */
    SHOT_BLOCKS_PER_MOD("legacyconnector.shotBlocksPerMod", Type.INT, "24", Category.TEST, "Cap of blocks shown per mod."),
    /** Cap of items shown per mod. */
    SHOT_ITEMS_PER_MOD("legacyconnector.shotItemsPerMod", Type.INT, "36", Category.TEST, "Cap of items shown per mod."),
    /** Block metadata values the block survey shows. */
    SHOT_METAS("legacyconnector.shotMetas", Type.STRING, "0", Category.TEST, "Block metadata values the block survey shows."),
    /** Number of stops of the terrain tour. */
    SHOT_TOUR("legacyconnector.shotTour", Type.INT, "6", Category.TEST, "Number of stops of the terrain tour."),
    /** Render distance in chunks of the screenshot test. */
    SHOT_RENDER_DISTANCE("legacyconnector.shotRenderDistance", Type.INT, "16", Category.TEST, "Render distance in chunks of the screenshot test."),
    /** Semicolon list of commands the screenshot test runs. */
    SHOT_COMMANDS("legacyconnector.shotCommands", Type.STRING, "legacyconnector mods", Category.TEST, "Semicolon list of commands the screenshot test runs."),
    /** Existing world folder the screenshot test loads instead of creating one. */
    SHOT_LOAD_WORLD("legacyconnector.shotLoadWorld", Type.STRING, "", Category.TEST, "Existing world folder the screenshot test loads instead of creating one."),
    /** World seed of the screenshot test. */
    SHOT_SEED("legacyconnector.shotSeed", Type.LONG, "20241022", Category.TEST, "World seed of the screenshot test."),
    /** host:port of a remote server the screenshot test joins. */
    SHOT_SERVER("legacyconnector.shotServer", Type.STRING, "", Category.TEST, "host:port of a remote server the screenshot test joins."),
    /** World type of the screenshot test world (empty = default). */
    SHOT_WORLD_TYPE("legacyconnector.shotWorldType", Type.STRING, "", Category.TEST, "World type of the screenshot test world (empty = default)."),
    /** 1.20.1: diagnostics in the compat buffer builder. */
    BUFFER_DIAG("legacyconnector.bufferDiag", Type.FLAG, "false", Category.DEBUG, "1.20.1: diagnostics in the compat buffer builder."),
    SKY_DIAG("legacyconnector.skyDiag", Type.FLAG, "false", Category.DEBUG, "1.7.10 client: logs which legacy sky, cloud and weather renderers a dimension uses and the camera they get."),
    /** 1.12.2: logs the annotations found on classes whose name contains this text. */
    DEBUG_ASM("legacyconnector.debugAsm", Type.STRING, "unset", Category.DEBUG, "1.12.2: logs the annotations found on classes whose name contains this text."),
    /** 1.12.2: dumps the asset translation of the item or block with this path. */
    DEBUG_ASSETS("legacyconnector.debugAssets", Type.STRING, "unset", Category.DEBUG, "1.12.2: dumps the asset translation of the item or block with this path."),
    /** 1.7.10: logs icon lookups of block classes whose name contains this text. */
    DEBUG_ICONS("legacyconnector.debugIcons", Type.STRING, "unset", Category.DEBUG, "1.7.10: logs icon lookups of block classes whose name contains this text."),
    /** Logs the inventory bounds of each block item handler once (also on with the LC_DEBUG environment variable). */
    DEBUG_ITEM_BOUNDS("legacyconnector.debugItemBounds", Type.FLAG, "false", Category.DEBUG, "Logs the inventory bounds of each block item handler once (also on with the LC_DEBUG environment variable)."),
    /** 1.12.2: any value logs the mod items registered at start. */
    DEBUG_ITEMS("legacyconnector.debugItems", Type.STRING, "unset", Category.DEBUG, "1.12.2: any value logs the mod items registered at start."),
    /** Dumps the quads captured from legacy block renderers. */
    DUMP_QUADS("legacyconnector.dumpQuads", Type.FLAG, "false", Category.DEBUG, "Dumps the quads captured from legacy block renderers."),
    /** 1.20.1: logs every synched entity data id a mod defines, with the caller. */
    ENTITY_DATA_DIAG("legacyconnector.entityDataDiag", Type.FLAG, "false", Category.DEBUG, "1.20.1: logs every synched entity data id a mod defines, with the caller."),
    /** 1.12.2: logs the time each legacy generator takes per chunk (a line every 20 s while chunks generate). */
    GEN_TIMING("legacyconnector.genTiming", Type.FLAG, "false", Category.DEBUG, "1.12.2: logs the time each legacy generator takes per chunk (a line every 20 s while chunks generate)."),
    /** Every 10 s heap and direct memory, every 30 s a per-level entity and chunk census in the log. */
    MEMORY_LOG("legacyconnector.memoryLog", Type.FLAG, "false", Category.DEBUG, "Every 10 s heap and direct memory, every 30 s a per-level entity and chunk census in the log."),
    /** Logs mean, median, 99th percentile and maximum server tick time and the heap every N ticks (0 = off). */
    MSPT_LOG("legacyconnector.msptLog", Type.INT, "0", Category.DEBUG, "Logs mean, median, 99th percentile and maximum server tick time and the heap every N ticks (0 = off)."),
    /** Logs which legacy particle classes spawn near the camera. */
    PARTICLE_DEBUG("legacyconnector.particleDebug", Type.FLAG, "false", Category.DEBUG, "Logs which legacy particle classes spawn near the camera."),
    /** Logs every legacy sound handed to the sound engine. */
    SOUND_DEBUG("legacyconnector.soundDebug", Type.FLAG, "false", Category.DEBUG, "Logs every legacy sound handed to the sound engine."),
    /** Part of a legacy event handler name: logs the first 60 runs of matching handlers. */
    TRACE_HANDLERS("legacyconnector.traceHandlers", Type.STRING, "unset", Category.DEBUG, "Part of a legacy event handler name: logs the first 60 runs of matching handlers."),
    /** Logs the first 400 legacy network messages as they arrive. */
    TRACE_MESSAGES("legacyconnector.traceMessages", Type.FLAG, "false", Category.DEBUG, "Logs the first 400 legacy network messages as they arrive."),
    /** A legacy mod that throws in a lifecycle phase aborts the load with its exception instead of being logged and skipped. */
    FAIL_FAST("legacyconnector.failFast", Type.FLAG, "false", Category.DEBUG, "A legacy mod that throws in a lifecycle phase aborts the load with its exception instead of being logged and skipped."),
    /** full: the coremod hook discovery prints every site instead of a summary. */
    COREMOD_HOOKS_REPORT("legacyconnector.coremodHooks.report", Type.STRING, "unset", Category.DEBUG, "full: the coremod hook discovery prints every site instead of a summary."),
    /** false switches off the guards around coremod hook calls (for timing). */
    COREMOD_GUARDS("legacyconnector.coremodGuards", Type.TOGGLE, "true", Category.DEBUG, "false switches off the guards around coremod hook calls (for timing)."),
    /** Direct memory in MB above which the guard requests a garbage collection. */
    DIRECT_MEMORY_GC_MB("legacyconnector.directMemoryGcMb", Type.LONG, "1024", Category.PERF, "Direct memory in MB above which the guard requests a garbage collection."),
    /** false turns off the render lock that item rendering forces (measurement switch). */
    ITEM_LOCK("legacyconnector.itemLock", Type.TOGGLE, "true", Category.PERF, "false turns off the render lock that item rendering forces (measurement switch)."),
    /** false turns off baking legacy display lists into GPU meshes (every call replays the recorded draws again). */
    DISPLAY_LIST_BAKE("legacyconnector.displayListBake", Type.TOGGLE, "true", Category.PERF, "false turns off baking legacy display lists into GPU meshes (every call replays the recorded draws again)."),
    /** Megabytes of GPU geometry the baked display lists may hold in all (more lists are replayed). */
    DISPLAY_LIST_BAKE_MB("legacyconnector.displayListBakeMb", Type.INT, "256", Category.PERF, "Megabytes of GPU geometry the baked display lists may hold in all (more lists are replayed)."),
    /** Keeps the legacy ore textures as they are instead of replacing their host rock pixels with the modern stone. */
    KEEP_LEGACY_ORE_BACKGROUNDS("legacyconnector.keepLegacyOreBackgrounds", Type.FLAG, "false", Category.COMPAT, "Keeps the legacy ore textures as they are instead of replacing their host rock pixels with the modern stone."),
    /** false turns off the guard that keeps legacy in-place generators (huts, towers, dungeons) out of modern structures. */
    IN_PLACE_GUARD("legacyconnector.inPlaceGuard", Type.TOGGLE, "true", Category.COMPAT, "false turns off the guard that keeps legacy in-place generators (huts, towers, dungeons) out of modern structures."),
    /** Build writes (a block into air, water or a plant, a block removed, a block entity) a generator call needs to count as having built something. */
    IN_PLACE_GUARD_BUILD_MIN("legacyconnector.inPlaceGuard.buildMin", Type.INT, "24", Category.COMPAT, "Build writes (a block into air, water or a plant, a block removed, a block entity) a generator call needs to count as having built something."),
    /** Refused writes that make the guard undo a whole generator call that crossed a modern structure. */
    IN_PLACE_GUARD_REFUSED_MIN("legacyconnector.inPlaceGuard.refusedMin", Type.INT, "4", Category.COMPAT, "Refused writes that make the guard undo a whole generator call that crossed a modern structure."),
    /** Blocks a legacy generator run must build to be recorded as a locatable structure. */
    LOCATABLE_BUILD_MIN("legacyconnector.locatable.buildMin", Type.INT, "24", Category.COMPAT, "Blocks a legacy generator run must build to be recorded as a locatable structure."),
    /** Non-natural blocks a legacy generator run must place to be recorded as a locatable structure. */
    LOCATABLE_MADE_MIN("legacyconnector.locatable.madeMin", Type.INT, "8", Category.COMPAT, "Non-natural blocks a legacy generator run must place to be recorded as a locatable structure."),
    /** false: legacy structure generators keep their 1.7.10 placement instead of becoming modern structure types. */
    MODERN_STRUCTURES("legacyconnector.modernStructures", Type.TOGGLE, "true", Category.COMPAT, "false: legacy structure generators keep their 1.7.10 placement instead of becoming modern structure types."),
    /** Chunks (0 to 16) kept between a legacy structure start and a village placement chunk; 0 = off. */
    STRUCTURE_VILLAGE_DISTANCE("legacyconnector.structureVillageDistance", Type.INT, "0", Category.COMPAT, "Chunks (0 to 16) kept between a legacy structure start and a village placement chunk; 0 = off."),
    /** Switch for A/B checks of 1.2.5 client callbacks (read live). */
    MC125_CLIENT_CALLBACKS("legacyconnector.mc125.clientCallbacks", Type.FLAG, "false", Category.WORKAROUND, "Switch for A/B checks of 1.2.5 client callbacks (read live)."),
    /** Set by the loader: legacy libraries replaced by a modern mod (they count as loaded). */
    BRIDGED_MODS("legacyconnector.bridgedMods", Type.STRING, "", Category.INTERNAL, "Set by the loader: legacy libraries replaced by a modern mod (they count as loaded)."),
    /** Set by the loader: ids of the legacy mods and APIs present in the pack. */
    PRESENT_LEGACY_MODS("legacyconnector.presentLegacyMods", Type.STRING, "", Category.INTERNAL, "Set by the loader: ids of the legacy mods and APIs present in the pack."),
    /** Set by the loader: packages of the legacy APIs found in the jars. */
    LEGACY_API_PACKAGES("legacyconnector.legacyApiPackages", Type.STRING, "", Category.INTERNAL, "Set by the loader: packages of the legacy APIs found in the jars."),
    /** Set by the loader: files that list the coremod hook sites. */
    COREMOD_HOOKS("legacyconnector.coremodHooks", Type.STRING, "unset", Category.INTERNAL, "Set by the loader: files that list the coremod hook sites."),
    /** Set by the loader: ids of the 1.20.1 mods translated in place. */
    FORGE1201_MODS("legacyconnector.forge1201.mods", Type.STRING, "", Category.INTERNAL, "Set by the loader: ids of the 1.20.1 mods translated in place."),
    /** Set by the loader: mods with missing dependencies (shown in the load report). */
    MISSING_DEPENDENCIES("legacyconnector.missingDependencies", Type.STRING, "", Category.INTERNAL, "Set by the loader: mods with missing dependencies (shown in the load report)."),
    /** Set by the loader to true once a 1.2.5 jar is hosted. */
    MC125_ACTIVE("legacyconnector.mc125.active", Type.STRING, "unset", Category.INTERNAL, "Set by the loader to true once a 1.2.5 jar is hosted."),
    /** Set by the loader: SERVER or CLIENT, answered to coremods asking for the side. */
    SIDE("legacyconnector.side", Type.STRING, "unset", Category.INTERNAL, "Set by the loader: SERVER or CLIENT, answered to coremods asking for the side."),
    /** true: legacy renderers take the capture lock (the loader sets it when a jar has a render library with static state). */
    RENDER_LOCK("legacyconnector.renderLock", Type.FLAG, "unset", Category.WORKAROUND, "true: legacy renderers take the capture lock (the loader sets it when a jar has a render library with static state).");

    private final String property;
    private final Type type;
    private final String defaultText;
    private final Category category;
    private final String description;

    Flags(String property, Type type, String defaultText, Category category, String description) {
        this.property = property;
        this.type = type;
        this.defaultText = defaultText;
        this.category = category;
        this.description = description;
    }

    @Override public String property() { return property; }
    @Override public Type type() { return type; }
    @Override public String defaultText() { return defaultText; }
    @Override public Category category() { return category; }
    @Override public String description() { return description; }

    /** Every switch of the runtime, including the ones declared in compat packages. */
    public static List<Switch> all() {
        List<Switch> all = new ArrayList<>(List.of(values()));
        all.addAll(List.of(CompatFlags.values()));
        return all;
    }

    /** "name=value, ..." for the switches that are set to something other than their default (internal ones left out); empty when none. */
    public static String nonDefaultSummary() {
        StringBuilder sb = new StringBuilder();
        for (Switch s : all()) {
            if (s.category() == Category.INTERNAL) continue;
            String v = s.nonDefaultValue();
            if (v == null) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.property()).append('=').append(v.length() > 80 ? v.substring(0, 77) + "..." : v);
        }
        return sb.toString();
    }
}
