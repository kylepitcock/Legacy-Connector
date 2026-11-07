package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SplitPackageResolverTest {
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

    @Test
    void hostProvidedPackagesAreDropped() {
        assertTrue(SplitPackageResolver.droppedWith("org/openjdk/nashorn/api/scripting", Set.of()));
        assertTrue(SplitPackageResolver.droppedWith("org/openjdk/nashorn/internal/objects", Set.of()));
        assertTrue(SplitPackageResolver.droppedWith("some/lib", Set.of("some/lib")));
        assertFalse(SplitPackageResolver.droppedWith("some/lib", Set.of("other/lib")));
        assertFalse(SplitPackageResolver.droppedWith("com/example/mod", Set.of()));
    }

    @Test
    void bundledNashornIsStrippedFromTheLegacyJarAndNotShared(@TempDir Path dir) throws IOException {
        Path mod = jar(dir, "huaji.jar", "org/openjdk/nashorn/api/scripting/NashornScriptEngineFactory.class", "com/example/Mod.class");
        Path other = jar(dir, "other.jar", "com/other/Thing.class");
        Path cache = Files.createDirectory(dir.resolve("cache"));
        SplitPackageResolver.Outcome out = SplitPackageResolver.resolve(List.of(mod, other), cache);
        assertNotNull(out.sharedLibrary());
        try (ZipFile z = new ZipFile(out.jars().get(0).toFile())) {
            assertNull(z.getEntry("org/openjdk/nashorn/api/scripting/NashornScriptEngineFactory.class"));
            assertNotNull(z.getEntry("com/example/Mod.class"));
        }
        try (ZipFile z = new ZipFile(out.sharedLibrary().toFile())) {
            assertNull(z.getEntry("org/openjdk/nashorn/api/scripting/NashornScriptEngineFactory.class"));
        }
        assertEquals(other, out.jars().get(1));
    }
}
