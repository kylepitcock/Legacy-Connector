package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/** A pre-1.13 jar can carry a decoy mods.toml (a joke that stops modern Forge); the inspector must still treat it as a legacy mod. */
class DecoyModsTomlTest {
    private static byte[] modClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "mod_Decoy", null, "net/minecraft/src/BaseMod", null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static Path jar(Path dir, String name, String toml, boolean mcmodInfo) throws IOException {
        Path p = dir.resolve(name);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(p))) {
            put(out, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n");
            if (toml != null) put(out, "META-INF/mods.toml", toml);
            if (mcmodInfo) put(out, "mcmod.info", "[{\"modid\":\"decoy\",\"name\":\"Decoy\",\"version\":\"1.0\",\"mcversion\":\"1.4.7\"}]");
            out.putNextEntry(new ZipEntry("mod_Decoy.class"));
            out.write(modClass());
            out.closeEntry();
        }
        return p;
    }

    private static void put(ZipOutputStream out, String name, String text) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }

    private static final String DECOY = "modLoader = \"javafml\"\nloaderVersion = \"*\"\n[[mods]]\nmodId = \"decoy\"\nversion = \"1.0\"\n"
            + "[[dependencies.decoy]]\nmodId = \"minecraft\"\nmandatory = true\nversionRange = \"[1.4.7]\" # joke\n";

    @Test
    void decoyModsTomlPinningAnOldMinecraftIsStillALegacyMod(@TempDir Path dir) throws IOException {
        assertNotNull(LegacyJarInspector.inspect(jar(dir, "decoy.jar", DECOY, true)));
    }

    @Test
    void realModernMetadataStaysUntouched(@TempDir Path dir) throws IOException {
        // a mods.toml of a modern mod (range of a modern Minecraft, no mcmod.info) is not ours
        String modern = DECOY.replace("[1.4.7]", "[1.21.1]");
        assertNull(LegacyJarInspector.inspect(jar(dir, "modern.jar", modern, true)));
        assertNull(LegacyJarInspector.inspect(jar(dir, "nomcmod.jar", DECOY, false)));
    }
}
