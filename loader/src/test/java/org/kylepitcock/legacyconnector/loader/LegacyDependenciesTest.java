package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Declared dependency strings of legacy mods and the check for required ones that are not installed. */
class LegacyDependenciesTest {
    private static LegacyJar jar(String id, String name, String version, String deps) {
        LegacyJar jar = new LegacyJar(Path.of(id + ".jar"));
        LegacyModDecl m = new LegacyModDecl();
        m.legacyId = id;
        m.name = name;
        m.version = version;
        m.dependencies = deps;
        jar.mods.add(m);
        return jar;
    }

    @Test
    void parsesRealDependencyStrings() {
        var deps = LegacyDependencies.parse("required-after:forge@[14.23.5.2768,);required-after:cofhcore@[4.6.0,4.7.0);");
        assertEquals(2, deps.size());
        assertEquals("forge", deps.get(0).id());
        assertEquals("[14.23.5.2768,)", deps.get(0).range());
        assertEquals("cofhcore", deps.get(1).id());
        assertEquals("[4.6.0,4.7.0)", deps.get(1).range());
        assertTrue(deps.get(1).required());

        var opt = LegacyDependencies.parse("after:jei");
        assertEquals(1, opt.size());
        assertFalse(opt.get(0).required());
        assertEquals("", opt.get(0).range());

        var mixed = LegacyDependencies.parse("required-after:Forge@[10.13.4.1558,);required-after:CodeChickenCore@[1.0.7,);before:NotEnoughItems;required-client:bar;garbage");
        assertEquals(List.of("Forge", "CodeChickenCore", "NotEnoughItems", "bar"), mixed.stream().map(LegacyDependencies.Dep::id).toList());
        assertTrue(mixed.get(3).required());
        assertFalse(mixed.get(2).required());
        assertTrue(LegacyDependencies.parse("").isEmpty());
        assertTrue(LegacyDependencies.parse(null).isEmpty());
    }

    @Test
    void reportsMissingRequiredDependencyInPlainWords() {
        LegacyJar te = jar("thermalexpansion", "Thermal Expansion", "5.5.7.1",
                "required-after:forge@[14.23.5.2768,);required-after:codechickenlib@[3.2.3,);required-after:cofhcore@[4.6.0,4.7.0);after:jei");
        LegacyJar core = jar("cofhcore", "CoFH Core", "4.6.6.1", "required-after:forge@[14.23.5.2768,)");
        Set<String> present = LegacyDependencies.presentIds(List.of(te, core), List.of(), Set.of(), List.of());
        var problems = LegacyDependencies.check(List.of(te, core), present, List.of(), true);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).fatal());
        assertEquals("Thermal Expansion requires codechickenlib ([3.2.3,)), which is not installed", problems.get(0).message());
    }

    @Test
    void warnsOnlyWhenAPresentDependencyIsOlder() {
        LegacyJar a = jar("a", "A", "1.0", "required-after:b@[2.0,)");
        LegacyJar b = jar("b", "B", "1.5", "");
        var problems = LegacyDependencies.check(List.of(a, b), LegacyDependencies.presentIds(List.of(a, b), List.of(), Set.of(), List.of()), List.of(), true);
        assertEquals(1, problems.size());
        assertFalse(problems.get(0).fatal());
        assertTrue(LegacyDependencies.tooOld("1.5", "[2.0,)"));
        assertFalse(LegacyDependencies.tooOld("2.0.1", "[2.0,)"));
        assertFalse(LegacyDependencies.tooOld("${version}", "[2.0,)"));
    }

    @Test
    void bridgedObsoleteAndSideDependenciesAreHandled() {
        LegacyJar m = jar("m", "M", "1", "required-after:Baubles;required-after:fastcraft;required-client:clientlib;required-after:*");
        // Baubles not bridged (no Curios): missing, and the message names the modern mod
        var none = LegacyDependencies.check(List.of(m), LegacyDependencies.presentIds(List.of(m), List.of(), Set.of(), List.of()), List.of(), false);
        assertEquals(1, none.size());
        assertTrue(none.get(0).message().contains("Baubles") && none.get(0).message().contains("curios"), none.get(0).message());
        // the bridge is active, and on the client the client-only dependency counts
        var bridged = LegacyDependencies.check(List.of(m), LegacyDependencies.presentIds(List.of(m), LibraryBridges.KNOWN, Set.of(), List.of()), LibraryBridges.KNOWN, true);
        assertEquals(1, bridged.size());
        assertTrue(bridged.get(0).message().contains("clientlib"));
    }

    @Test
    void buildsTheLoadingWarningText() {
        LegacyJar te = jar("thermalexpansion", "Thermal Expansion", "5.5.7.1", "required-after:codechickenlib@[3.2.3,);required-after:Baubles");
        var problems = LegacyDependencies.check(List.of(te), LegacyDependencies.presentIds(List.of(te), List.of(), Set.of(), List.of()), List.of(), true);
        assertEquals(2, problems.size());
        String plain = java.text.MessageFormat.format(LegacyDependencies.issueKey(problems.get(0)), LegacyDependencies.issueArgs(problems.get(0), "1.12.2"));
        assertEquals("Thermal Expansion (1.12.2) requires codechickenlib [3.2.3,), which is not installed. Put its jar for Minecraft 1.12.2 into mods/1.12.2/. Thermal Expansion may fail to load or miss features without it.", plain);
        String bridged = java.text.MessageFormat.format(LegacyDependencies.issueKey(problems.get(1)), LegacyDependencies.issueArgs(problems.get(1), "1.12.2"));
        assertEquals("Thermal Expansion (1.12.2) requires Baubles (provided on 1.21.1 by curios): install curios. Thermal Expansion may fail to load or miss features without it.", bridged);
    }
}
