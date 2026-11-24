package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** A jar whose obfuscated Minecraft names fit none of 1.4.7, 1.5.2 and 1.6.4 was built for another release (1.6.2/1.6.3 ...): it is reported, not linked. */
class OtherVersionJarTest {
    @Test
    void scoresDecideOtherVersion() {
        assertTrue(LegacyJarInspector.otherVersionScores(new double[] {0.3, 0.1, 0.39}));
        assertFalse(LegacyJarInspector.otherVersionScores(new double[] {0.02, 0.0, 0.8}), "a clear match is that version");
        assertFalse(LegacyJarInspector.otherVersionScores(new double[] {0.55, 0.0, 0.0}));
        assertFalse(LegacyJarInspector.otherVersionScores(null), "too few obfuscated references to say");
    }

    /** A bare ModLoader mod (extends BaseMod) with 20 references into obfuscated classes that no supported version has. */
    @Test
    void bareModLoaderModWithForeignNamesIsFlagged(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("mod_Foreign.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry("mod_Foreign.class"));
            out.write(modClass(20));
            out.closeEntry();
        }
        LegacyJar legacy = LegacyJarInspector.inspect(jar);
        assertNotNull(legacy);
        assertEquals(LegacyJarInspector.OTHER_VERSION_REASON, legacy.otherVersionReason);
    }

    @Test
    void fewReferencesAreNotFlagged(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("mod_Small.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry("mod_Small.class"));
            out.write(modClass(3));
            out.closeEntry();
        }
        LegacyJar legacy = LegacyJarInspector.inspect(jar);
        assertNotNull(legacy);
        assertNull(legacy.otherVersionReason);
    }

    private static byte[] modClass(int refs) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "mod_" + (refs > 10 ? "Foreign" : "Small"), null, "BaseMod", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "BaseMod", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        MethodVisitor load = cw.visitMethod(Opcodes.ACC_PUBLIC, "load", "()V", null, null);
        load.visitCode();
        for (int i = 0; i < refs; i++) {
            load.visitMethodInsn(Opcodes.INVOKESTATIC, "zq" + (i % 4), "z" + (char) ('a' + i % 20), "()V", false);
        }
        load.visitInsn(Opcodes.RETURN);
        load.visitMaxs(0, 0);
        load.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
