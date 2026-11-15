package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

final class Forge1201TargetApiInventoryTest {
    @TempDir
    Path tempDir;

    @Test
    void inventoriesResolvedClassesMembersDescriptorsAndDuplicatePrecedence() throws Exception {
        Path first = tempDir.resolve("first.jar");
        Path second = tempDir.resolve("second.jar");

        Map<String, byte[]> firstClasses = new LinkedHashMap<>();
        firstClasses.put("net/minecraft/Fixture.class",
                fixtureClass("net/minecraft/Fixture", "java/lang/Object", true));
        firstClasses.put("net/minecraft/core/BlockPos.class",
                emptyClass("net/minecraft/core/BlockPos"));
        firstClasses.put("net/minecraft/world/item/ItemStack.class",
                emptyClass("net/minecraft/world/item/ItemStack"));
        writeJar(first, firstClasses);

        writeJar(second, Map.of(
                "net/minecraft/Fixture.class",
                fixtureClass("net/minecraft/Fixture", "java/lang/Object", false)));

        Forge1201TargetApiInventory.Report report =
                Forge1201TargetApiInventory.inspect(List.of(first, second));

        assertTrue(report.complete());
        assertEquals(2, report.classpathEntryCount());
        assertEquals(4, report.classEntryOccurrenceCount());
        assertEquals(3, report.uniqueClassCount());
        assertEquals(1, report.duplicateClassNameCount());
        assertEquals(3, report.resolvedClassCount());
        assertEquals(0, report.unresolvedClassCount());
        assertEquals(1, report.declaredMethodCount());
        assertEquals(1, report.declaredFieldCount());
        assertEquals(List.of(
                "net/minecraft/core/BlockPos",
                "net/minecraft/world/item/ItemStack"),
                report.minecraftReferencedTypes());
        assertTrue(report.missingMinecraftReferencedTypes().isEmpty());

        Forge1201TargetApiInventory.ClassRecord fixture = report.classes().stream()
                .filter(value -> value.internalName().equals("net/minecraft/Fixture"))
                .findFirst()
                .orElseThrow();
        assertEquals("resolve", fixture.methods().get(0).name());
        assertEquals(
                "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/item/ItemStack;",
                fixture.methods().get(0).descriptor());
        assertEquals("position", fixture.fields().get(0).name());
        assertTrue(fixture.resolvedResource().contains("first.jar"));
        assertEquals(2, fixture.classpathOccurrences().size());
        assertNotNull(report.classpathEntries().get(0).sha256());
        assertNotNull(report.classpathEntries().get(1).sha256());

        assertEquals(
                Forge1201TargetApiInventory.toJson(report),
                Forge1201TargetApiInventory.toJson(
                        Forge1201TargetApiInventory.inspect(List.of(first, second))));
    }

    @Test
    void reportsMinecraftTypesMissingFromTheResolvedClasspath() throws Exception {
        Path jar = tempDir.resolve("missing-type.jar");
        writeJar(jar, Map.of(
                "net/minecraft/Fixture.class",
                fixtureClass("net/minecraft/Fixture", "java/lang/Object", true)));

        Forge1201TargetApiInventory.Report report =
                Forge1201TargetApiInventory.inspect(List.of(jar));

        assertFalse(report.complete());
        assertEquals(List.of(
                "net/minecraft/core/BlockPos",
                "net/minecraft/world/item/ItemStack"),
                report.missingMinecraftReferencedTypes());
        assertEquals(2, report.missingMinecraftReferencedTypeCount());
    }

    @Test
    void scansDirectoryClasspathEntriesAndPreservesOrderedClasspathInput() throws Exception {
        Path classes = tempDir.resolve("classes");
        Path classFile = classes.resolve("net/minecraft/DirectoryFixture.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, emptyClass("net/minecraft/DirectoryFixture"));

        Path classpathFile = tempDir.resolve("target-classpath.txt");
        Files.writeString(classpathFile,
                classes.toAbsolutePath() + System.lineSeparator()
                        + classes.toAbsolutePath() + System.lineSeparator(),
                StandardCharsets.UTF_8);

        List<Path> entries = Forge1201TargetApiInventory.readClasspathFile(classpathFile);
        assertEquals(List.of(classes.toAbsolutePath().normalize()), entries);

        Forge1201TargetApiInventory.Report report =
                Forge1201TargetApiInventory.inspect(entries);
        assertTrue(report.complete());
        assertEquals(1, report.classpathEntryCount());
        assertEquals(1, report.uniqueClassCount());
        assertEquals(1, report.resolvedClassCount());
        assertEquals(1, report.classpathEntries().get(0).minecraftClassEntryOccurrences());
        assertNotNull(report.classpathEntries().get(0).sha256());
    }

    private static byte[] fixtureClass(String internalName, String superName, boolean includeApiMembers) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                internalName, null, superName, null);
        if (includeApiMembers) {
            writer.visitField(Opcodes.ACC_PUBLIC, "position",
                    "Lnet/minecraft/core/BlockPos;", null, null).visitEnd();
            writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "resolve",
                    "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/item/ItemStack;",
                    null, null).visitEnd();
        } else {
            writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    "shadowed", "()V", null, null).visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] emptyClass(String internalName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                internalName, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void writeJar(Path jar, Map<String, byte[]> entries) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new JarEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
    }
}
