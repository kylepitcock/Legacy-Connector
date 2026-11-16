package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/** A zip that only wraps mod archives ("put the contents in /mods" bundles) is unpacked into its inner mods. */
class BundledModsTest {
    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (var e : files.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                out.write(e.getValue());
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] cls(String name, String superName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, name, null, superName, null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] modZip(String cls) throws IOException {
        Map<String, byte[]> f = new LinkedHashMap<>();
        f.put("mcmod.info", ("[{\"modid\":\"" + cls + "\",\"name\":\"" + cls + "\",\"version\":\"1\",\"mcversion\":\"1.4.7\"}]").getBytes(StandardCharsets.UTF_8));
        f.put(cls + ".class", cls(cls, "net/minecraft/src/BaseMod"));
        return zip(f);
    }

    @Test
    void anArchiveWithoutClassesOfItsOwnYieldsItsInnerMods(@TempDir Path dir) throws IOException {
        Map<String, byte[]> f = new LinkedHashMap<>();
        f.put("readme.txt", "put the contents in /mods".getBytes(StandardCharsets.UTF_8));
        f.put("Pack/mod_One.zip", modZip("mod_One"));
        f.put("mod_Two.jar", modZip("mod_Two"));
        f.put("resources.zip", zip(Map.of("pack.mcmeta", new byte[] {'{', '}'})));
        Path outer = dir.resolve("Bundle.zip");
        Files.write(outer, zip(f));
        List<Path> inner = LegacyModLocator.bundledMods(outer, dir.resolve("nested"));
        assertEquals(List.of("Bundle-mod_One.zip", "Bundle-mod_Two.jar"), inner.stream().map(p -> p.getFileName().toString()).toList());
        assertTrue(Files.notExists(dir.resolve("nested").resolve("Bundle-resources.zip")), "a non-mod inner archive is not kept");
        for (Path p : inner) assertTrue(LegacyJarInspector.inspect(p) != null);
    }

    @Test
    void anArchiveWithClassesOfItsOwnIsNotABundle(@TempDir Path dir) throws IOException {
        Map<String, byte[]> f = new LinkedHashMap<>();
        f.put("mod_Outer.class", cls("mod_Outer", "net/minecraft/src/BaseMod"));
        f.put("lib/mod_One.zip", modZip("mod_One"));
        Path outer = dir.resolve("Own.zip");
        Files.write(outer, zip(f));
        assertTrue(LegacyModLocator.bundledMods(outer, dir.resolve("nested")).isEmpty());
    }
}
