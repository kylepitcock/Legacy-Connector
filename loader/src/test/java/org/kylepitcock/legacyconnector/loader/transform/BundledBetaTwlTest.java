package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.mapping.McpMappings;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.synth.SynthDemand;
import org.kylepitcock.legacyconnector.loader.synth.SynthJarBuilder;

/** A Beta jar that bundles TWL must not split TWL packages with the shared synth module. */
class BundledBetaTwlTest {
    private static ShimIndex nonEmptyShim() {
        ClassInfo marker = new ClassInfo(
                "legacyconnector/vb1_7_3/example/Present",
                "java/lang/Object",
                List.of(),
                0x0001);
        return new ShimIndex(Map.of(marker.name, marker));
    }

    private static LegacyRemapper remapper(ShimIndex shim, SynthDemand demand) {
        return new LegacyRemapper(
                LegacyVersion.VB1_7_3,
                McpMappings.forVersion(LegacyVersion.VB1_7_3),
                shim,
                demand);
    }

    @Test
    void bundledTwlDefinitionsAreProvidedByTheTranslatedJarAndTheirPackagesAreNotSynthesized() throws Exception {
        LegacyJar jar = new LegacyJar(Path.of("GuiAPI.jar"));
        jar.version = LegacyVersion.VB1_7_3;
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put("de/matthiasmann/twl/Widget", new byte[0]);
        classes.put("de/matthiasmann/twl/model/Table", new byte[0]);
        classes.put("example/GuiApiMod", new byte[0]);

        ShimIndex shim = nonEmptyShim();
        SynthDemand demand = new SynthDemand();
        LegacyRemapper remapper = remapper(shim, demand);
        LegacyJarTransformer transformer = new LegacyJarTransformer(shim, getClass().getClassLoader());

        transformer.relocateBundled(jar, classes, remapper);

        String widget = LegacyVersion.VB1_7_3.synthPrefix() + "de/matthiasmann/twl/Widget";
        String table = LegacyVersion.VB1_7_3.synthPrefix() + "de/matthiasmann/twl/model/Table";
        String missingInProvidedPackage = LegacyVersion.VB1_7_3.synthPrefix()
                + "de/matthiasmann/twl/MissingWidget";

        assertEquals(widget, remapper.map("de/matthiasmann/twl/Widget"));
        assertEquals(table, remapper.map("de/matthiasmann/twl/model/Table"));
        assertTrue(demand.providedClasses.contains(widget));
        assertTrue(demand.providedClasses.contains(table));
        assertFalse(demand.classes.contains(widget), "a bundled definition is not a synth demand");

        // Other classes can still demand a type from the package. It must not create a second module export.
        assertEquals(missingInProvidedPackage, remapper.map("de/matthiasmann/twl/MissingWidget"));
        assertTrue(demand.classes.contains(missingInProvidedPackage));

        Path demandFile = Files.createTempFile("beta-twl-demand", ".txt");
        try {
            demand.write(demandFile);
            SynthDemand cachedDemand = SynthDemand.read(demandFile);
            assertTrue(cachedDemand.providedClasses.contains(widget), "provided classes survive the per-jar demand cache");

            Map<String, byte[]> generated = new SynthJarBuilder(
                    LegacyVersion.VB1_7_3,
                    shim,
                    getClass().getClassLoader(),
                    cachedDemand).build();
            assertFalse(generated.keySet().stream().anyMatch(name ->
                    name.startsWith(LegacyVersion.VB1_7_3.synthPrefix() + "de/matthiasmann/twl/")),
                    "the synth module must not export packages already supplied by a translated jar");
        } finally {
            Files.deleteIfExists(demandFile);
        }
    }

    @Test
    void unbundledTwlReferencesStillUseTheSharedSynthNamespace() {
        LegacyJar jar = new LegacyJar(Path.of("ExampleMod.jar"));
        jar.version = LegacyVersion.VB1_7_3;
        Map<String, byte[]> classes = new LinkedHashMap<>();
        classes.put("example/Mod", new byte[0]);

        ShimIndex shim = nonEmptyShim();
        SynthDemand demand = new SynthDemand();
        LegacyRemapper remapper = remapper(shim, demand);
        LegacyJarTransformer transformer = new LegacyJarTransformer(shim, getClass().getClassLoader());

        transformer.relocateBundled(jar, classes, remapper);

        String expected = LegacyVersion.VB1_7_3.synthPrefix() + "de/matthiasmann/twl/Widget";
        assertEquals(expected, remapper.map("de/matthiasmann/twl/Widget"));
        assertTrue(demand.classes.contains(expected), "unbundled TWL API types should still be synthesized");
        assertTrue(demand.providedClasses.isEmpty());
    }
}
