package org.kylepitcock.legacyconnector.loader;

/**
 * Registry of the switches (JVM system properties) the loader reads or sets, one declaration each; the loader cannot see runtime classes, so
 * it has its own copy of the shape of {@code org.kylepitcock.legacyconnector.core.flags.Flags}. The accessors have the semantics of the JDK
 * calls they replace and read the property on every call. Properties the loader sets for the runtime are declared in both registries.
 * {@code docs/wiki/Diagnostic-Switches.md} is generated from both.
 */
public enum LoaderFlags {
    /** Translated artifact a Beta provenance unit test inspects. */
    BETA_COMBAT_ARTIFACT("legacyconnector.beta.combatArtifact", Type.PATH, "unset", Category.TEST, "Translated artifact a Beta provenance unit test inspects."),
    /** Compiled shim index a Beta provenance unit test needs. */
    BETA_COMBAT_SHIM_INDEX("legacyconnector.beta.combatShimIndex", Type.PATH, "unset", Category.TEST, "Compiled shim index a Beta provenance unit test needs."),
    /** Set by the loader: legacy libraries replaced by a modern mod (they count as loaded). */
    BRIDGED_MODS("legacyconnector.bridgedMods", Type.STRING, "", Category.INTERNAL, "Set by the loader: legacy libraries replaced by a modern mod (they count as loaded)."),
    /** Set by the loader: ids of the legacy mods and APIs present in the pack. */
    PRESENT_LEGACY_MODS("legacyconnector.presentLegacyMods", Type.STRING, "", Category.INTERNAL, "Set by the loader: ids of the legacy mods and APIs present in the pack."),
    /** Set by the loader: packages of the legacy APIs found in the jars. */
    LEGACY_API_PACKAGES("legacyconnector.legacyApiPackages", Type.STRING, "", Category.INTERNAL, "Set by the loader: packages of the legacy APIs found in the jars."),
    /** Set by the loader: legacy API ids and versions found in the jars. */
    LEGACY_APIS("legacyconnector.legacyApis", Type.STRING, "", Category.INTERNAL, "Set by the loader: legacy API ids and versions found in the jars."),
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
    RENDER_LOCK("legacyconnector.renderLock", Type.FLAG, "unset", Category.WORKAROUND, "true: legacy renderers take the capture lock (the loader sets it when a jar has a render library with static state)."),
    /** Comma list of library bridges to force on (replacement mod does not have to be present). */
    BRIDGES("legacyconnector.bridges", Type.STRING, "", Category.COMPAT, "Comma list of library bridges to force on (replacement mod does not have to be present)."),
    /** true or false: 1.20.1 hosting on or off, wins over the config key. */
    FORGE1201("legacyconnector.forge1201", Type.STRING, "unset", Category.COMPAT, "true or false: 1.20.1 hosting on or off, wins over the config key."),
    /** false: references to missing 1.21.1 symbols fail at link time instead of becoming stand-ins (wins over the config key). */
    FORGE1201_SOFTLINK("legacyconnector.forge1201.softlink", Type.STRING, "true", Category.COMPAT, "false: references to missing 1.21.1 symbols fail at link time instead of becoming stand-ins (wins over the config key)."),
    /** true or false: 1.2.5 hosting on or off, wins over the config key. */
    MC125("legacyconnector.mc125", Type.STRING, "unset", Category.COMPAT, "true or false: 1.2.5 hosting on or off, wins over the config key."),
    /** true or false: side-by-side library versions on or off, wins over the config key. */
    SIDE_BY_SIDE_LIBRARIES("legacyconnector.sideBySideLibraries", Type.STRING, "unset", Category.COMPAT, "true or false: side-by-side library versions on or off, wins over the config key."),
    /** Threads translating jars at once (0 = automatic). */
    TRANSLATION_THREADS("legacyconnector.translationThreads", Type.INT, "0", Category.PERF, "Threads translating jars at once (0 = automatic)."),
    /** Vanilla client jar of a legacy version (key written with the version without dots, for example vanilla1122); default: config coremods.vanillaJar or the launcher's copy. */
    VANILLA_JAR("legacyconnector.vanilla<mc>", Type.PATH, "unset", Category.COMPAT, "Vanilla client jar of a legacy version (key written with the version without dots, for example vanilla1122); default: config coremods.vanillaJar or the launcher's copy."),
    /** Folder with the bundled-library layout used instead of the embedded resources. */
    LIBS_DIR("legacyconnector.libsDir", Type.PATH, "unset", Category.TEST, "Folder with the bundled-library layout used instead of the embedded resources."),
    /** Shim index file used instead of the one in the runtime jar. */
    SHIM_INDEX("legacyconnector.shimIndex", Type.PATH, "unset", Category.TEST, "Shim index file used instead of the one in the runtime jar."),
    /** Development runs: directory of the compiled runtime classes (coremod patches read them). */
    RUNTIME_CLASSES("legacyconnector.runtimeClasses", Type.PATH, "unset", Category.TEST, "Development runs: directory of the compiled runtime classes (coremod patches read them)."),
    /** 1.20.1: keeps the datagen code of a jar in place to measure its unresolved references. */
    KEEP_DATAGEN("legacyconnector.keepDatagen", Type.FLAG, "false", Category.DEBUG, "1.20.1: keeps the datagen code of a jar in place to measure its unresolved references."),
    /** Coremod hook probe prints only the hook sites, one line each. */
    PROBE_SITES("legacyconnector.probe.sites", Type.FLAG, "false", Category.TOOL, "Coremod hook probe prints only the hook sites, one line each."),
    /** 1.2.5: writes the unresolved names of the jar translation to this file. */
    MC125_UNRESOLVED("legacyconnector.mc125.unresolved", Type.PATH, "unset", Category.DEBUG, "1.2.5: writes the unresolved names of the jar translation to this file."),
    /** NeoForge version the 1.20.1 API verifier reports against. */
    TARGET_NEO_FORGE_VERSION("legacyconnector.targetNeoForgeVersion", Type.STRING, "unknown", Category.TOOL, "NeoForge version the 1.20.1 API verifier reports against."),
    /** false: the offline check skips the resource check. */
    CHECK_ASSETS("legacyconnector.check.assets", Type.STRING, "unset", Category.TOOL, "false: the offline check skips the resource check."),
    /** false: the offline check treats the jars as plain mods (no library bridges). */
    CHECK_BRIDGES("legacyconnector.check.bridges", Type.STRING, "unset", Category.TOOL, "false: the offline check treats the jars as plain mods (no library bridges)."),
    /** File the offline check writes its result to as JSON. */
    CHECK_JSON("legacyconnector.check.json", Type.PATH, "unset", Category.TOOL, "File the offline check writes its result to as JSON."),
    /** Directory the offline check keeps the translated jars in. */
    CHECK_KEEP("legacyconnector.check.keep", Type.PATH, "unset", Category.TOOL, "Directory the offline check keeps the translated jars in."),
    /** -q: the offline check does not print the link report. */
    CHECK_QUIET("legacyconnector.check.quiet", Type.STRING, "", Category.TOOL, "-q: the offline check does not print the link report."),
    /** Vanilla jar of the offline check. */
    CHECK_VANILLA_JAR("legacyconnector.check.vanillaJar", Type.PATH, "unset", Category.TOOL, "Vanilla jar of the offline check."),
    /** false: the offline check skips the bytecode verification. */
    CHECK_VERIFY("legacyconnector.check.verify", Type.STRING, "unset", Category.TOOL, "false: the offline check skips the bytecode verification."),
    /** true: a non-zero exit code when the bytecode verification finds anything (CI). */
    CHECK_VERIFY_FAIL("legacyconnector.check.verify.fail", Type.STRING, "unset", Category.TOOL, "true: a non-zero exit code when the bytecode verification finds anything (CI)."),
    /** 1.20.1 gap analyzer: comma list of API package prefixes of libraries assumed to exist natively on 1.21.1. */
    GAP_ASSUME_NATIVE("gap.assumeNative", Type.STRING, "unset", Category.TOOL, "1.20.1 gap analyzer: comma list of API package prefixes of libraries assumed to exist natively on 1.21.1."),
    /** 1.20.1 gap analyzer: also analyzes nested jars that are native library builds. */
    GAP_KEEP_NATIVE_LIBS("gap.keepNativeLibs", Type.FLAG, "false", Category.TOOL, "1.20.1 gap analyzer: also analyzes nested jars that are native library builds."),
    /** 1.20.1 gap analyzer: rows per list in the report. */
    GAP_LIMIT("gap.limit", Type.INT, "400", Category.TOOL, "1.20.1 gap analyzer: rows per list in the report."),
    /** 1.20.1 gap analyzer: Forge version named in the report heading. */
    GAP_MC("gap.mc", Type.STRING, "1.20.1", Category.TOOL, "1.20.1 gap analyzer: Forge version named in the report heading."),
    /** 1.20.1 gap analyzer: the real shim index, so facade classes get their in-game names. */
    GAP_SHIM_INDEX("gap.shimIndex", Type.PATH, "unset", Category.TOOL, "1.20.1 gap analyzer: the real shim index, so facade classes get their in-game names."),
    /** 1.20.1 gap analyzer: lists the soft-linked references per jar. */
    GAP_SOFTLIST("gap.softlist", Type.FLAG, "false", Category.TOOL, "1.20.1 gap analyzer: lists the soft-linked references per jar."),
    /** 1.20.1 gap analyzer: second SRG table whose names win (experimental 1.19.2). */
    GAP_SRG_OVERLAY("gap.srgOverlay", Type.PATH, "unset", Category.TOOL, "1.20.1 gap analyzer: second SRG table whose names win (experimental 1.19.2)."),
    /** 1.20.1 gap analyzer: runs the jars through the real 1.20.1 transformer first. */
    GAP_TRANSLATE("gap.translate", Type.FLAG, "false", Category.TOOL, "1.20.1 gap analyzer: runs the jars through the real 1.20.1 transformer first."),
    /** 1.20.1 gap analyzer: directory to keep the translated jars in. */
    GAP_TRANSLATED_DIR("gap.translated.dir", Type.PATH, "unset", Category.TOOL, "1.20.1 gap analyzer: directory to keep the translated jars in."),
    /** 1.20.1 mixin survey: number of samples per target. */
    SURVEY_SAMPLE("survey.sample", Type.INT, "30", Category.TOOL, "1.20.1 mixin survey: number of samples per target."),
    /** 1.20.1 mixin triage: directory with the rename tables. */
    TRIAGE_TABLES("triage.tables", Type.PATH, "unset", Category.TOOL, "1.20.1 mixin triage: directory with the rename tables."),
    /** Directory of the real 1.2.5 archives the corpus detection test reads. */
    MC_CORPUS("mc.corpus", Type.PATH, "unset", Category.TEST, "Directory of the real 1.2.5 archives the corpus detection test reads."),
    /** Directory of the jars of the placement regression test. */
    MC_ISSUE312("mc.issue312", Type.PATH, "unset", Category.TEST, "Directory of the jars of the placement regression test."),
    /** Directory of the real 1.2.5 archives of the bridge corpus test. */
    MC125_CORPUS("mc125.corpus", Type.PATH, "unset", Category.TEST, "Directory of the real 1.2.5 archives of the bridge corpus test.");

    /** How the value is read. FLAG: true only for the text "true"; TOGGLE: on unless the text is "false". */
    public enum Type { FLAG, TOGGLE, INT, LONG, STRING, PATH }

    /** What the switch is for. INTERNAL ones are set by the loader for the runtime. */
    public enum Category { DEBUG, TEST, PERF, COMPAT, WORKAROUND, INTERNAL, TOOL }

    private final String property;
    private final Type type;
    private final String defaultText;
    private final Category category;
    private final String description;

    LoaderFlags(String property, Type type, String defaultText, Category category, String description) {
        this.property = property;
        this.type = type;
        this.defaultText = defaultText;
        this.category = category;
        this.description = description;
    }

    public String property() { return property; }
    public Type type() { return type; }
    public String defaultText() { return defaultText; }
    public Category category() { return category; }
    public String description() { return description; }

    /** {@code Boolean.getBoolean}: true only when the property is the text "true". */
    public boolean on() { return Boolean.getBoolean(property); }

    /** {@code System.getProperty}: the value, or null when unset. */
    public String get() { return System.getProperty(property); }

    public String get(String def) { return System.getProperty(property, def); }

    /** {@code Integer.getInteger}: null when unset or not a number. */
    public Integer getInt() { return Integer.getInteger(property); }

    public int getInt(int def) { return Integer.getInteger(property, def); }

    public long getLong(long def) { return Long.getLong(property, def); }

    public void set(String value) { System.setProperty(property, value); }

    public void clear() { System.clearProperty(property); }

    /** For {@link #VANILLA_JAR}: the value of the property of one legacy version, e.g. {@code legacyconnector.vanilla1122}. */
    public String getFor(String suffix, String def) {
        return System.getProperty(property.replace("<mc>", suffix), def);
    }

    /** The value when the switch is set and differs from its default, else null. */
    public String nonDefaultValue() {
        String v = System.getProperty(property);
        return v == null || v.equals(defaultText) ? null : v;
    }

    /** "name=value, ..." for the switches set to something other than their default (internal ones left out); empty when none. */
    public static String nonDefaultSummary() {
        StringBuilder sb = new StringBuilder();
        for (LoaderFlags s : values()) {
            if (s.category == Category.INTERNAL) continue;
            String v = s.nonDefaultValue();
            if (v == null) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.property).append('=').append(v.length() > 80 ? v.substring(0, 77) + "..." : v);
        }
        return sb.toString();
    }
}
