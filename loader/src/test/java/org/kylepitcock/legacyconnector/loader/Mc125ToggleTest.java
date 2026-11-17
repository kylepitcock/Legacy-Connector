package org.kylepitcock.legacyconnector.loader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** The user-facing toggle {@code experimental.minecraft125}: reading (default, config, system property precedence) and what it does to a 1.2.5 jar. */
class Mc125ToggleTest {
    @BeforeEach
    @AfterEach
    void reset() {
        System.clearProperty(Mc125Jars.PROPERTY);
        Mc125Jars.configure(new Properties());
    }

    private static void config(String value) {
        Properties p = new Properties();
        if (value != null) p.setProperty(Mc125Jars.CONFIG_KEY, value);
        Mc125Jars.configure(p);
    }

    @Test
    void offByDefault() {
        config(null);
        assertFalse(Mc125Jars.enabled());
        config("");
        assertFalse(Mc125Jars.enabled());
    }

    @Test
    void configKeyTurnsItOn() {
        config("true");
        assertTrue(Mc125Jars.enabled());
        config(" TRUE ");
        assertTrue(Mc125Jars.enabled());
        config("false");
        assertFalse(Mc125Jars.enabled());
        config("garbage");
        assertFalse(Mc125Jars.enabled());
    }

    @Test
    void systemPropertyWinsOverConfig() {
        config("true");
        System.setProperty(Mc125Jars.PROPERTY, "false");
        assertFalse(Mc125Jars.enabled());
        config("false");
        System.setProperty(Mc125Jars.PROPERTY, "true");
        assertTrue(Mc125Jars.enabled());
        System.setProperty(Mc125Jars.PROPERTY, " ");
        assertFalse(Mc125Jars.enabled(), "a blank property is ignored");
    }

    @Test
    void readFromTheConfigFileNextToTheModsFolder(@TempDir Path dir) throws IOException {
        Path mods = Files.createDirectories(dir.resolve("mods"));
        Files.createDirectories(dir.resolve("config"));
        Files.writeString(dir.resolve("config").resolve("legacyconnector.properties"), Mc125Jars.CONFIG_KEY + "=true\n");
        Mc125Jars.configure(LibraryBridges.readConfig(mods));
        assertTrue(Mc125Jars.enabled());
    }

    private static Path emptyModLoaderJar(Path dir) throws IOException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "mod_Test", null, "BaseMod", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "BaseMod", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        cw.visitEnd();
        Path folder = Files.createDirectories(dir.resolve(Mc125Jars.FOLDER));
        Path jar = folder.resolve("mod_Test.jar");
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(jar))) {
            z.putNextEntry(new ZipEntry("mod_Test.class"));
            z.write(cw.toByteArray());
            z.closeEntry();
        }
        return jar;
    }

    /** Toggle off (any build): reported with the hint, never hosted. */
    @Test
    void offReportsTheJarAndSaysHowToTurnItOn(@TempDir Path dir) throws IOException {
        Path jar = emptyModLoaderJar(dir);
        assertNull(LegacyJarInspector.inspect(jar));
        String why = Mc125Jars.takeUnsupported(jar);
        assertNotNull(why);
        String line = Mc125Jars.reportLine("a.jar", why);
        assertTrue(line.contains("not loaded"), line);
        assertTrue(line.contains(Mc125Jars.CONFIG_KEY) && line.contains("legacyconnector.properties"), line);
    }

    /** Toggle on, table present: hosted as 1.4.7 with source version 1.2.5 (the report then marks it experimental). */
    @Test
    void onWithTheTableHostsTheJar(@TempDir Path dir) throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(org.kylepitcock.legacyconnector.loader.transform.Notch125To147.available(), "needs -Pmappings125 / a bundled table");
        config("true");
        LegacyJar jar = LegacyJarInspector.inspect(emptyModLoaderJar(dir));
        assertNotNull(jar);
        assertEquals(LegacyVersion.V1_2_5, jar.sourceVersion);
        assertTrue(Mc125Jars.EXPERIMENTAL_NOTE.contains("experimental (Minecraft 1.2.5)"));
    }

    /** Toggle on, table absent: nothing hosted, the report names the missing table. */
    @Test
    void onWithoutTheTableHostsNothing(@TempDir Path dir) throws IOException {
        org.junit.jupiter.api.Assumptions.assumeFalse(org.kylepitcock.legacyconnector.loader.transform.Notch125To147.available(), "build has the table");
        config("true");
        Path jar = emptyModLoaderJar(dir);
        assertNull(LegacyJarInspector.inspect(jar));
        String why = Mc125Jars.takeUnsupported(jar);
        assertNotNull(why);
        assertTrue(why.contains("no 1.2.5 name table"), why);
        assertTrue(Mc125Jars.reportLine("a.jar", why).contains("not loaded"));
    }
}
