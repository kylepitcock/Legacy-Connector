package org.kylepitcock.legacyconnector.loader;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Client and server builds of one 1.2.5 mod (same {@code mod_X} class, separate obfuscations): the build of the running side loads. */
class Mc125BuildsTest {
    private static LegacyJar jar(String file, char side, String... ids) {
        LegacyJar j = new LegacyJar(Path.of(file));
        j.version = LegacyVersion.V1_4_7;
        j.sourceVersion = LegacyVersion.V1_2_5;
        j.notchSide = side;
        for (String id : ids) {
            LegacyModDecl d = new LegacyModDecl();
            d.legacyId = id;
            d.mainClass = id;
            j.mods.add(d);
        }
        return j;
    }

    @Test
    void clientRunKeepsTheClientBuild() {
        LegacyJar server = jar("Backpack_SERVER.zip", 's', "mod_Backpack");
        LegacyJar client = jar("Backpack_CLIENT.zip", 'c', "mod_Backpack");
        Map<LegacyJar, String> dropped = Mc125Builds.select(List.of(server, client), "CLIENT");
        assertEquals(List.of(server), List.copyOf(dropped.keySet()));
        assertTrue(dropped.get(server).contains("Backpack_CLIENT.zip"), dropped.toString());
    }

    @Test
    void serverRunKeepsTheServerBuild() {
        LegacyJar server = jar("a_server.zip", 's', "mod_X");
        LegacyJar client = jar("a_client.zip", 'c', "mod_X");
        assertEquals(List.of(client), List.copyOf(Mc125Builds.select(List.of(client, server), "SERVER").keySet()));
    }

    @Test
    void aJarThatRepackagesAnotherModsIdsLoadsWithoutThem() {
        LegacyJar bc = jar("buildcraft-core.zip", 'c', "mod_BuildCraftCore");
        LegacyJar lp = jar("LogisticsPipes-client.jar", 'c', "mod_BuildCraftCore", "mod_LogisticsPipes");
        Map<LegacyJar, String> dropped = Mc125Builds.select(List.of(bc, lp), "CLIENT");
        assertTrue(dropped.isEmpty(), dropped.toString());
        assertEquals(List.of("mod_LogisticsPipes"), lp.mods.stream().map(m -> m.legacyId).toList());
        assertEquals(1, bc.mods.size());
    }

    @Test
    void otherVersionsAreNotTouched() {
        LegacyJar a = jar("a.zip", 's', "mod_X");
        LegacyJar b = jar("b.zip", 'c', "mod_X");
        b.sourceVersion = null; // an ordinary 1.4.7 jar with the same mod id
        assertTrue(Mc125Builds.select(List.of(a, b), "CLIENT").isEmpty());
    }
}
