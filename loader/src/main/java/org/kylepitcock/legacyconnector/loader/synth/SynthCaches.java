package org.kylepitcock.legacyconnector.loader.synth;

/**
 * The legacy API tables the loader reads while it translates jars and synthesizes the missing API (hierarchy, methods,
 * fields, array initialisers of each legacy version: several MB each as maps of strings). The loader is an FML service whose
 * classes live as long as the game, so its static caches did too; nothing reads them once discovery is over.
 */
public final class SynthCaches {
    private SynthCaches() {}

    /** Drops every table; a later lookup reads its table again. */
    public static void release() {
        LegacyHierarchyData.releaseCaches();
        LegacyMethods.releaseCaches();
        LegacyFields.releaseCaches();
        StaticArrays.releaseCaches();
        InstanceArrays.releaseCaches();
    }
}
