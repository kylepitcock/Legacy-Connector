package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Side-by-side libraries: relocation and renaming of a synthetic library and its dependents, for 1.7.10, 1.12.2 and 1.20.1. */
class SideBySideTest {
    private static final Set<String> ROOTS = Set.of("org/example/lib");

    private static SideBySide planFor(LegacyVersion v) {
        return SideBySide.of(List.of(new SideBySide.Unit(v, "examplelib", SideBySide.newIdFor("examplelib", v), ROOTS, "test")));
    }

    @Test
    void idsCarryTheVersion() {
        assertEquals("examplelib_l1201", SideBySide.newIdFor("examplelib", LegacyVersion.V1_20_1));
        assertEquals("examplelib_l1710", SideBySide.newIdFor("examplelib", LegacyVersion.V1_7_10));
        assertEquals("examplelib_l1122", SideBySide.newIdFor("examplelib", LegacyVersion.V1_12_2));
    }

    @Test
    void classesRelocateOnlyForTheirVersion() {
        for (LegacyVersion v : new LegacyVersion[] {LegacyVersion.V1_7_10, LegacyVersion.V1_12_2, LegacyVersion.V1_20_1}) {
            SideBySide plan = planFor(v);
            String moved = plan.mapClass(v, "org/example/lib/api/Thing$Inner");
            assertEquals(SideBySide.prefix(v) + "org/example/lib/api/Thing$Inner", moved);
            assertNull(plan.mapClass(v, "org/example/libs/Other"), "a sibling name is not in the package");
            assertNull(plan.mapClass(v, "org/example/mod/Dependent"));
            LegacyVersion other = v == LegacyVersion.V1_12_2 ? LegacyVersion.V1_7_10 : LegacyVersion.V1_12_2;
            assertNull(plan.mapClass(other, "org/example/lib/api/Thing"), "another version's mods keep their own copy");
        }
        assertFalse(SideBySide.prefix(LegacyVersion.V1_7_10).equals(SideBySide.prefix(LegacyVersion.V1_12_2)));
    }

    @Test
    void stringsAndTextFollowTheRelocation() {
        SideBySide plan = planFor(LegacyVersion.V1_20_1);
        String p = SideBySide.prefix(LegacyVersion.V1_20_1);
        assertEquals((p + "org/example/lib/Boot").replace('/', '.'), plan.mapString(LegacyVersion.V1_20_1, "org.example.lib.Boot"));
        assertEquals(p + "org/example/lib/Boot", plan.mapString(LegacyVersion.V1_20_1, "org/example/lib/Boot"));
        assertEquals("examplelib", plan.mapString(LegacyVersion.V1_20_1, "examplelib"));
        assertEquals("org.example.mod.Main", plan.mapString(LegacyVersion.V1_20_1, "org.example.mod.Main"));
        String at = plan.rewriteText(LegacyVersion.V1_20_1, "public org.example.lib.Foo bar\npublic org.example.libs.Foo baz\n");
        assertTrue(at.contains(p.replace('/', '.') + "org.example.lib.Foo bar"));
        assertTrue(at.contains("public org.example.libs.Foo baz"));
    }

    @Test
    void servicesFilesAreRenamedAndRewritten() {
        SideBySide plan = planFor(LegacyVersion.V1_12_2);
        Map<String, byte[]> in = new LinkedHashMap<>();
        in.put("META-INF/services/org.example.lib.Spi", "org.example.lib.impl.Impl\n# comment\n".getBytes(StandardCharsets.UTF_8));
        in.put("META-INF/services/javax.sound.sampled.spi.AudioFileReader", "org.example.lib.audio.Reader".getBytes(StandardCharsets.UTF_8));
        in.put("org/example/lib/data.json", new byte[] {1});
        in.put("assets/examplelib/lang/en_us.json", new byte[] {2});
        Map<String, byte[]> out = plan.rewriteResources(LegacyVersion.V1_12_2, in);
        String p = SideBySide.prefix(LegacyVersion.V1_12_2);
        assertNotNull(out.get("META-INF/services/" + (p + "org/example/lib/Spi").replace('/', '.')));
        assertTrue(new String(out.get("META-INF/services/javax.sound.sampled.spi.AudioFileReader"), StandardCharsets.UTF_8).contains(p.replace('/', '.') + "org.example.lib.audio.Reader"));
        assertNotNull(out.get(p + "org/example/lib/data.json"));
        assertNotNull(out.get("assets/examplelib/lang/en_us.json"), "assets keep their namespace");
    }

    @Test
    void modsTomlGetsTheNewIdsEverywhere() {
        SideBySide plan = planFor(LegacyVersion.V1_20_1);
        String lib = "[[mods]]\nmodId=\"examplelib\"\n[[dependencies.examplelib]]\nmodId=\"neoforge\"\n";
        String out = plan.rewriteToml(LegacyVersion.V1_20_1, lib, Set.of("examplelib"));
        assertTrue(out.contains("modId=\"examplelib_l1201\""));
        assertTrue(out.contains("[[dependencies.examplelib_l1201]]"));
        String dependent = "[[mods]]\nmodId=\"user\"\n[[dependencies.user]]\nmodId=\"examplelib\"\ntype=\"required\"\n";
        String out2 = plan.rewriteToml(LegacyVersion.V1_20_1, dependent, Set.of("user"));
        assertTrue(out2.contains("[[dependencies.user]]"));
        assertTrue(out2.contains("modId=\"examplelib_l1201\""));
        assertTrue(out2.contains("modId=\"user\""));
    }

    @Test
    void mixinConfigNamesNeverClash() {
        assertEquals("lib_l1201.mixins.json", SideBySide.mixinConfigName("lib.mixins.json", "_l1201"));
        assertEquals("lib_l1201.json", SideBySide.mixinConfigName("lib.json", "_l1201"));
    }

    @Test
    void rootsAreTheTopPackagesOfTheJar() {
        Set<String> roots = SideBySide.rootsOf(List.of("com/foo/lib", "com/foo/lib/api", "com/foo/lib/repack/jcodec", "net/minecraft/x", "single", "org/other/thing"));
        assertEquals(Set.of("com/foo/lib", "org/other/thing"), roots);
    }

    private static Path jar(Path dir, String name, String... entries) throws IOException {
        Path p = dir.resolve(name);
        try (OutputStream o = Files.newOutputStream(p); ZipOutputStream z = new ZipOutputStream(o)) {
            for (String e : entries) {
                z.putNextEntry(new ZipEntry(e));
                z.write(new byte[] {1});
                z.closeEntry();
            }
        }
        return p;
    }

    private static LegacyJar legacy(Path path, LegacyVersion v, String id) {
        LegacyJar j = new LegacyJar(path);
        j.version = v;
        LegacyModDecl m = new LegacyModDecl();
        m.legacyId = id;
        m.mainClass = "org/example/lib/Main";
        j.mods.add(m);
        return j;
    }

    @Test
    void plannerIsolatesALibraryThatClashesWithANativeJar(@TempDir Path dir) throws IOException {
        // "citadel" is a table library marked sideBySide; a library of the table without the marker is bridged and left to the native build
        LegacyJar citadel = legacy(jar(dir, "citadel.jar", "com/github/alexthe666/citadel/Citadel.class", "com/github/alexthe666/citadel/server/A.class"), LegacyVersion.V1_20_1, "citadel");
        LegacyJar curios = legacy(jar(dir, "curios.jar", "top/theillusivec4/curios/Curios.class"), LegacyVersion.V1_20_1, "curios");
        LegacyJar other = legacy(jar(dir, "free.jar", "org/free/Free.class"), LegacyVersion.V1_20_1, "freemod");
        SideBySidePlanner.Result r = SideBySidePlanner.plan(List.of(citadel, curios, other), Set.of("citadel", "curios"), new Properties(), List.of());
        assertNotNull(r.plan().unit(LegacyVersion.V1_20_1, "citadel"));
        assertEquals("citadel_l1201", citadel.mods.get(0).fmlId());
        assertNull(r.plan().unit(LegacyVersion.V1_20_1, "curios"), "bridged library: the native build serves it");
        assertEquals("curios", curios.mods.get(0).fmlId());
        assertEquals("freemod", other.mods.get(0).fmlId());
        assertTrue(r.notes().get(citadel).contains("citadel_l1201"));
        assertEquals(null, NativeLibraries1201.replacing(citadel, Set.of("citadel")));
        assertNotNull(NativeLibraries1201.replacing(curios, Set.of("curios")));
        NativeLibraries1201.setIsolated(Set.of());
    }

    @Test
    void configSwitchAndOverrides(@TempDir Path dir) throws IOException {
        LegacyJar citadel = legacy(jar(dir, "citadel.jar", "com/github/alexthe666/citadel/Citadel.class"), LegacyVersion.V1_20_1, "citadel");
        LegacyJar curios = legacy(jar(dir, "curios.jar", "top/theillusivec4/curios/Curios.class"), LegacyVersion.V1_20_1, "curios");
        Properties off = new Properties();
        off.setProperty("experimental.forge1201.sideBySideLibraries", "false"); // the 1.20.1 key is an alias of the generic one
        assertTrue(SideBySidePlanner.plan(List.of(citadel), Set.of("citadel"), off, List.of()).plan().isEmpty());
        Properties on = new Properties();
        on.setProperty("experimental.sideBySideLibraries.include", "curios");
        on.setProperty("experimental.sideBySideLibraries.exclude", "citadel");
        SideBySide plan = SideBySidePlanner.plan(List.of(citadel, curios), Set.of("citadel", "curios"), on, List.of()).plan();
        assertNotNull(plan.unit(LegacyVersion.V1_20_1, "curios"), "include forces a bridged library side by side");
        assertNull(plan.unit(LegacyVersion.V1_20_1, "citadel"), "exclude keeps the old behaviour");
        NativeLibraries1201.setIsolated(Set.of());
    }

    @Test
    void twoLegacyVersionsOfOneLibraryGetDistinctIds() {
        SideBySide plan = SideBySide.of(List.of(new SideBySide.Unit(LegacyVersion.V1_7_10, "ccl", SideBySide.newIdFor("ccl", LegacyVersion.V1_7_10), Set.of(), "1.12.2 copy")));
        assertEquals("ccl_l1710", plan.newId(LegacyVersion.V1_7_10, "ccl"));
        assertEquals("ccl", plan.newId(LegacyVersion.V1_12_2, "ccl"), "the other version keeps the id");
    }
}
