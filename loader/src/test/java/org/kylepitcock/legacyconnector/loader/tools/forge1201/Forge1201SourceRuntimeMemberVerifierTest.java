package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

final class Forge1201SourceRuntimeMemberVerifierTest {
    @TempDir
    Path tempDir;

    @Test
    void inspectsDeclaredInheritedDescriptorChangedAndAbsentMembersFromOrderedJarClasspath() throws Exception {
        Path archive = tempDir.resolve("forge-source-fixture.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(archive))) {
            addClass(jar, "net/minecraft/Super", null,
                    new String[][] {{"work", "(I)V"}}, new String[][] {{"health", "I"}});
            addClass(jar, "net/minecraft/Child", "net/minecraft/Super",
                    new String[][] {{"work", "(J)V"}}, new String[][] {});
        }
        Path classpath = tempDir.resolve("classpath.txt");
        Files.writeString(classpath, archive.toAbsolutePath() + System.lineSeparator(), StandardCharsets.UTF_8);

        var index = Forge1201SourceRuntimeMemberVerifier.readClasspath(classpath);

        var summary = index.classpathSummary();
        assertEquals(1, summary.classpathEntryCount());
        assertEquals(2, summary.indexedClassCount());
        assertEquals(0, summary.duplicateClassNames());

        var declared = index.inspectMethod("net/minecraft/Child", "work", "(J)V");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_DECLARED_EXACT,
                declared.status());
        assertEquals("net/minecraft/Child", declared.declaringOwner());

        var inherited = index.inspectMethod("net/minecraft/Child", "work", "(I)V");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_INHERITED_EXACT,
                inherited.status());
        assertEquals("net/minecraft/Super", inherited.declaringOwner());

        var changed = index.inspectMethod("net/minecraft/Child", "work", "(D)V");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_NAME_FOUND_DESCRIPTOR_CHANGED,
                changed.status());
        assertEquals(2, changed.observedSameNameMembers().size());

        var absent = index.inspectMethod("net/minecraft/Child", "missing", "()V");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_ABSENT,
                absent.status());

        var field = index.inspectField("net/minecraft/Child", "health", "I");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_INHERITED_EXACT,
                field.status());
        assertEquals("net/minecraft/Super", field.declaringOwner());

        var missingClass = index.inspectMethod("net/minecraft/NotPresent", "work", "(I)V");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_CLASS_MISSING,
                missingClass.status());
    }

    @Test
    void preservesFirstClasspathEntryWhenClassNamesAreDuplicated() throws Exception {
        Path first = tempDir.resolve("first.jar");
        Path second = tempDir.resolve("second.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(first))) {
            addClass(jar, "sample/Owner", null, new String[][] {{"first", "()V"}}, new String[][] {});
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(second))) {
            addClass(jar, "sample/Owner", null, new String[][] {{"second", "()V"}}, new String[][] {});
        }
        Path classpath = tempDir.resolve("ordered-classpath.txt");
        Files.writeString(classpath,
                first.toAbsolutePath() + System.lineSeparator()
                        + second.toAbsolutePath() + System.lineSeparator(),
                StandardCharsets.UTF_8);

        var index = Forge1201SourceRuntimeMemberVerifier.readClasspath(classpath);

        assertEquals(2, index.classpathSummary().classpathEntryCount());
        assertEquals(1, index.classpathSummary().indexedClassCount());
        assertEquals(1, index.classpathSummary().duplicateClassNames());
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_DECLARED_EXACT,
                index.inspectMethod("sample/Owner", "first", "()V").status());
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_ABSENT,
                index.inspectMethod("sample/Owner", "second", "()V").status());
    }

    private static void addClass(
            JarOutputStream jar,
            String name,
            String superName,
            String[][] methods,
            String[][] fields) throws IOException {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null);
        for (String[] field : fields) {
            writer.visitField(Opcodes.ACC_PUBLIC, field[0], field[1], null, null).visitEnd();
        }
        for (String[] method : methods) {
            writer.visitMethod(Opcodes.ACC_PUBLIC, method[0], method[1], null, null).visitEnd();
        }
        writer.visitEnd();

        jar.putNextEntry(new JarEntry(name + ".class"));
        jar.write(writer.toByteArray());
        jar.closeEntry();
    }
}
