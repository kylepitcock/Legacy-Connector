package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/** A 1.7.x jar that kept the example mods.toml of a newer template is a legacy mod, not a Forge 1.20.1 one. */
class StrayModsTomlTest {
    private static byte[] classReferencing(String fmlPackage) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "x/Mod", null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PUBLIC, "f", "L" + fmlPackage + "Thing;", null, null).visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static Path jar(Path dir, String toml, String fmlPackage, boolean mcmod) throws IOException {
        Path p = dir.resolve("m.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(p))) {
            out.putNextEntry(new ZipEntry("META-INF/mods.toml"));
            out.write(toml.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            if (mcmod) {
                out.putNextEntry(new ZipEntry("mcmod.info"));
                out.write("[{\"modid\":\"x\",\"mcversion\":\"1.7.10\"}]".getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry("x/Mod.class"));
            out.write(classReferencing(fmlPackage));
            out.closeEntry();
        }
        return p;
    }

    private static final String TEMPLATE = "modLoader=\"javafml\"\nloaderVersion=\"[25,)\"\n[[mods]]\nmodId=\"x\"\n";

    @Test
    void templateTomlNextToAnFml17JarIsLegacy(@TempDir Path dir) throws IOException {
        try (ZipFile z = new ZipFile(jar(dir, TEMPLATE, "cpw/mods/fml/common/", true).toFile())) {
            assertTrue(LegacyJarInspector.strayTemplateModsToml(z));
        }
    }

    @Test
    void realForgeJarsAreNotAffected(@TempDir Path dir) throws IOException {
        String declared = TEMPLATE + "[[dependencies.x]]\nmodId=\"minecraft\"\nversionRange=\"[1.20.1]\"\n";
        try (ZipFile z = new ZipFile(jar(dir, declared, "cpw/mods/fml/common/", true).toFile())) {
            assertFalse(LegacyJarInspector.strayTemplateModsToml(z), "a declared minecraft dependency is a real mods.toml");
        }
        try (ZipFile z = new ZipFile(jar(dir, TEMPLATE, "net/minecraftforge/fml/common/", true).toFile())) {
            assertFalse(LegacyJarInspector.strayTemplateModsToml(z), "no 1.7 FML package");
        }
        try (ZipFile z = new ZipFile(jar(dir, TEMPLATE, "cpw/mods/fml/common/", false).toFile())) {
            assertFalse(LegacyJarInspector.strayTemplateModsToml(z), "no mcmod.info");
        }
    }
}
