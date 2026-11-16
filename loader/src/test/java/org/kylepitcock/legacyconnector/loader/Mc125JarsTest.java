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
import java.util.Map;
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
    void optInWithoutTheTableInTheBuildStillOnlyReports(@TempDir Path dir) throws IOException {
        org.junit.jupiter.api.Assumptions.assumeFalse(org.kylepitcock.legacyconnector.loader.transform.Notch125To147.available(), "build made with -Pmappings125");
        System.setProperty("legacyconnector.mc125", "true");
        try {
            Path jar = modJar(dir, "mod_Test.jar", table125().subList(0, 40));
            assertNull(LegacyJarInspector.inspect(jar));
            String why = Mc125Jars.takeUnsupported(jar);
            assertNotNull(why);
            assertTrue(why.contains("-Pmappings125"), why);
        } finally {
            System.clearProperty("legacyconnector.mc125");
        }
    }

    /** Needs the real join table (run with {@code -Pmappings125}): the opted-in jar is a 1.4.7 jar whose game calls resolve in 1.4.7's names. */
    @Test
    void optedInJarIsHostedAs147AndRenamedToItsNames(@TempDir Path dir) throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(org.kylepitcock.legacyconnector.loader.transform.Notch125To147.available(), "needs -Pmappings125");
        System.setProperty("legacyconnector.mc125", "true");
        try {
            List<String[]> calls = table125().subList(0, 60);
            Path jar = modJar(dir, "mod_Test.jar", calls);
            LegacyJar inspected = LegacyJarInspector.inspect(jar);
            assertNotNull(inspected);
            assertEquals(LegacyVersion.V1_4_7, inspected.version);
            assertEquals(LegacyVersion.V1_2_5, inspected.sourceVersion);
            assertEquals('c', inspected.notchSide);
            assertEquals("mod_Test", inspected.mods.get(0).legacyId);
            Map<String, byte[]> classes = new java.util.LinkedHashMap<>();
            try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(jar.toFile())) {
                classes.put("mod_Test", z.getInputStream(z.getEntry("mod_Test.class")).readAllBytes());
            }
            org.kylepitcock.legacyconnector.loader.transform.Notch125To147.apply(inspected, classes);
            org.kylepitcock.legacyconnector.loader.mapping.NotchNames n147 = org.kylepitcock.legacyconnector.loader.mapping.NotchNames.get("1.4.7");
            int[] resolved = {0, 0};
            new org.objectweb.asm.ClassReader(classes.get("mod_Test")).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                            if (owner.indexOf('/') >= 0 || owner.equals("BaseMod")) return;
                            resolved[1]++;
                            if (n147.method(owner, name, desc, null) != null) resolved[0]++;
                        }
                    };
                }
            }, 0);
            assertTrue(resolved[1] >= 50, "calls seen: " + resolved[1]);
            assertTrue(resolved[0] >= resolved[1] * 0.85, resolved[0] + " of " + resolved[1] + " calls resolve in the 1.4.7 notch table");
        } finally {
            System.clearProperty("legacyconnector.mc125");
        }
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

    private static Path namesOnly(Path dir, String file, String... entries) throws IOException {
        Path jar = dir.resolve(file);
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (String e : entries) {
                z.putNextEntry(new ZipEntry(e));
                z.write(new byte[] {1});
                z.closeEntry();
            }
        }
        return jar;
    }

    /** The ModLoader / ModLoaderMP / Forge zips are API distributions (skipped); a mod that bundles a ModLoader copy next to its mod_ class is not. */
    @Test
    void apiDistributionsAreToldFromMods(@TempDir Path dir) throws IOException {
        try (var z = new java.util.zip.ZipFile(namesOnly(dir, "ml.zip", "ModLoader.class", "BaseMod.class", "MLProp.class", "adl.class").toFile())) {
            assertEquals("ModLoader / ModLoaderMP distribution", Mc125Jars.apiDistribution(z));
        }
        try (var z = new java.util.zip.ZipFile(namesOnly(dir, "forge.zip", "ModLoader.class", "BaseMod.class", "forge/MinecraftForge.class", "mod_MinecraftForge.class").toFile())) {
            assertTrue(Mc125Jars.apiDistribution(z).startsWith("Minecraft Forge"));
        }
        try (var z = new java.util.zip.ZipFile(namesOnly(dir, "mp.zip", "BaseModMp.class", "ModLoaderMp.class", "mod_ModLoaderMp.class").toFile())) {
            assertNotNull(Mc125Jars.apiDistribution(z));
        }
        try (var z = new java.util.zip.ZipFile(namesOnly(dir, "mod.zip", "ModLoader.class", "mod_Real.class").toFile())) {
            assertNull(Mc125Jars.apiDistribution(z), "a real mod class means a mod");
        }
        try (var z = new java.util.zip.ZipFile(namesOnly(dir, "plain.zip", "mod_Real.class", "Helper.class").toFile())) {
            assertNull(Mc125Jars.apiDistribution(z));
        }
    }
}
