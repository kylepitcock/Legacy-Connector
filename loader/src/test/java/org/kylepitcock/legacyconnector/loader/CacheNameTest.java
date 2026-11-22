package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The translation cache key ({@code LegacyModLocator.cacheName}): any change that must re-translate a jar (its bytes, the set of legacy
 * jars it is translated with) has to change the name, and nothing else may. A stale hit after an update would run an old translation.
 */
class CacheNameTest {
    @TempDir
    Path dir;

    private static String name(LegacyJar jar, byte[] modSet) throws Exception {
        Method m = LegacyModLocator.class.getDeclaredMethod("cacheName", LegacyJar.class, byte[].class);
        m.setAccessible(true);
        return (String) m.invoke(null, jar, modSet);
    }

    private LegacyJar jar(String file, String content) throws Exception {
        Path p = dir.resolve(file);
        Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        return new LegacyJar(p);
    }

    @Test
    void sameJarAndSetGiveTheSameName() throws Exception {
        LegacyJar a = jar("mod-1.0.jar", "abc");
        assertEquals(name(a, new byte[] {1, 2}), name(a, new byte[] {1, 2}));
    }

    @Test
    void nameKeepsTheFileStemAndAShortHashSuffix() throws Exception {
        String n = name(jar("Cool Mod [1.7.10]-2.0.jar", "abc"), null);
        assertTrue(n.matches("Cool_Mod__1.7.10_-2.0-[0-9a-f]{16}\\.jar"), n);
        assertTrue(name(jar("pack.zip", "abc"), null).startsWith("pack-"), "a .zip loses its extension too");
    }

    @Test
    void differentContentGivesADifferentName() throws Exception {
        assertNotEquals(name(jar("mod.jar", "abc"), null), name(jar("mod2.jar", "abd"), null).replace("mod2", "mod"));
    }

    @Test
    void sameContentUnderAnotherFileNameDiffersOnlyInTheStem() throws Exception {
        String a = name(jar("a.jar", "same"), null);
        String b = name(jar("b.jar", "same"), null);
        assertEquals(a.substring(1), b.substring(1));
    }

    @Test
    void theModSetIsPartOfTheKey() throws Exception {
        LegacyJar j = jar("mod.jar", "abc");
        assertNotEquals(name(j, new byte[] {1}), name(j, new byte[] {2}));
        assertNotEquals(name(j, null), name(j, new byte[] {1}));
    }

    @Test
    void anEditedJarIsRehashedWhenItsSizeOrTimeChanges() throws Exception {
        LegacyJar j = jar("mod.jar", "abc");
        String before = name(j, null);
        Files.write(j.path, "abcdef".getBytes(StandardCharsets.UTF_8)); // another size: the memoised digest is not used
        assertNotEquals(before, name(j, null));
    }
}
