package org.kylepitcock.legacyconnector.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Detection of Minecraft 1.2.5 ModLoader jars on tiny synthetic jars: reported as not supported, never hosted as 1.4.7. */
class Mc125JarsTest {
    /** {owner, name, desc} of obfuscated methods of the 1.2.5 client. */
    private static List<String[]> table125() throws IOException {
        List<String[]> out = new ArrayList<>();
        try (InputStream raw = Mc125JarsTest.class.getClassLoader().getResourceAsStream(Mc125Jars.RESOURCE)) {
            assertNotNull(raw, "the 1.2.5 table is a resource of the loader");
            BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8));
            for (String l = r.readLine(); l != null; l = r.readLine()) {
                String[] p = l.split("\t");
                if (p.length == 5 && p[0].equals("c") && p[1].equals("M")) out.add(new String[] {p[2], p[3], p[4]});
            }
        }
        return out;
    }

    /** {owner, name, desc} of obfuscated methods of the 1.4.7 table that the 1.2.5 table does not have. */
    private static List<String[]> table147Only() throws IOException {
        Set<String> in125 = new HashSet<>();
        for (String[] m : table125()) in125.add(m[0] + "." + m[1] + " " + m[2]);
        List<String[]> out = new ArrayList<>();
        try (InputStream raw = Mc125JarsTest.class.getClassLoader().getResourceAsStream("legacyconnector/legacy/1.4.7/notch.txt")) {
            BufferedReader r = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8));
            for (String l = r.readLine(); l != null; l = r.readLine()) {
                String[] p = l.split(" ");
                if (p.length >= 5 && p[0].equals("M") && p[1].indexOf('/') < 0 && p[2].length() <= 3
                        && !in125.contains(p[1] + "." + p[2] + " " + p[3])) out.add(new String[] {p[1], p[2], p[3]});
            }
        }
        return out;
    }

    /** A ModLoader mod: {@code mod_Test extends BaseMod}, its load() calls the given game members (as the obfuscated names say). */
    private static Path modJar(Path dir, String file, List<String[]> calls) throws IOException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "mod_Test", null, "BaseMod", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "BaseMod", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        MethodVisitor load = cw.visitMethod(Opcodes.ACC_PUBLIC, "load", "()V", null, null);
        load.visitCode();
        for (String[] c : calls) load.visitMethodInsn(Opcodes.INVOKESTATIC, c[0], c[1], c[2], false);
        load.visitInsn(Opcodes.RETURN);
        load.visitMaxs(0, 0);
        load.visitEnd();
        cw.visitEnd();
        Path jar = dir.resolve(file);
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(jar))) {
            z.putNextEntry(new ZipEntry("mod_Test.class"));
            z.write(cw.toByteArray());
            z.closeEntry();
        }
        return jar;
    }

    @Test
    void ownTableIsLoaded() throws IOException {
        assertTrue(table125().size() > 2000, "client methods of 1.2.5");
    }

    @Test
    void modLoaderJarWithMinecraft125NamesIsReportedNotHosted(@TempDir Path dir) throws IOException {
        List<String[]> calls = table125().subList(0, 40);
        Path jar = modJar(dir, "mod_Test.jar", calls);
        assertNull(LegacyJarInspector.inspect(jar), "not hosted");
        String why = Mc125Jars.takeUnsupported(jar);
        assertNotNull(why, "recorded as 1.2.5");
        assertTrue(why.contains("1.2.5"), why);
        assertEquals("a.jar: Minecraft 1.2.5 (not supported yet): " + why + "; not loaded", Mc125Jars.reportLine("a.jar", why));
        assertNull(Mc125Jars.takeUnsupported(jar), "taken once");
    }

    @Test
    void jarInTheFolderOf1_2_5IsReportedWithoutFingerprint(@TempDir Path dir) throws IOException {
        Path folder = Files.createDirectories(dir.resolve(Mc125Jars.FOLDER));
        Path jar = modJar(folder, "mod_Test.jar", List.of());
        assertNull(LegacyJarInspector.inspect(jar));
        assertNotNull(Mc125Jars.takeUnsupported(jar));
    }

    @Test
    void modLoaderJarWithMinecraft147NamesStaysV1_4_7(@TempDir Path dir) throws IOException {
        List<String[]> calls = table147Only();
        assertTrue(calls.size() > 100);
        Path jar = modJar(dir, "mod_Test.jar", calls.subList(0, 40));
        LegacyJar inspected = LegacyJarInspector.inspect(jar);
        assertNotNull(inspected);
        assertEquals(LegacyVersion.V1_4_7, inspected.version);
        assertNull(Mc125Jars.takeUnsupported(jar));
    }

    @Test
    void modLoaderJarWithoutGameReferencesIsNotTakenAs125(@TempDir Path dir) throws IOException {
        Path jar = modJar(dir, "mod_Test.jar", List.of());
        LegacyJarInspector.inspect(jar);
        assertNull(Mc125Jars.takeUnsupported(jar), "too few references to decide: no claim");
    }
}
