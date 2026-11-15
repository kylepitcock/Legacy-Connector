package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            addClass(jar, "net/minecraft/Super", "java/lang/Object",
                    new String[][] {{"<init>", "()V"}, {"work", "(I)V"}}, new String[][] {{"health", "I"}});
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
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_HIERARCHY_EXACT,
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

        var constructor = index.inspectMethod("net/minecraft/Child", "<init>", "()V");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_ABSENT,
                constructor.status());

        var field = index.inspectField("net/minecraft/Child", "health", "I");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_HIERARCHY_EXACT,
                field.status());
        assertEquals("net/minecraft/Super", field.declaringOwner());

        var missingClass = index.inspectMethod("net/minecraft/NotPresent", "work", "(I)V");
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_CLASS_MISSING,
                missingClass.status());
    }

    @Test
    void doesNotClassifyMissingMembersWhenTheirSuperclassHierarchyIsUnavailable() throws Exception {
        Path archive = tempDir.resolve("incomplete-hierarchy.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(archive))) {
            addClass(jar, "sample/Child", "sample/UnavailableParent",
                    new String[][] {}, new String[][] {});
        }
        Path classpath = tempDir.resolve("incomplete-classpath.txt");
        Files.writeString(classpath, archive.toAbsolutePath() + System.lineSeparator(), StandardCharsets.UTF_8);

        var index = Forge1201SourceRuntimeMemberVerifier.readClasspath(classpath);
        var result = index.inspectMethod("sample/Child", "work", "()V");

        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_HIERARCHY_INCOMPLETE,
                result.status());
        assertTrue(result.hierarchyVisited().contains("sample/UnavailableParent"));
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

    @Test
    void resolvesOfficialSelectorBackToSrgAndChecksTheForgeSourceClassfile() throws Exception {
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (Lb;)V m_10_ 2
                b net/minecraft/Component 3
                """);
        Path tiny = write("source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                """);
        Path mojmap = write("client.txt", """
                net.minecraft.Entity -> a:
                    void doThing(net.minecraft.Component) -> m
                net.minecraft.Component -> b:
                """);

        Path archive = tempDir.resolve("source.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(archive))) {
            addClass(jar, "net/minecraft/Entity", null,
                    new String[][] {{"doThing", "(Lnet/minecraft/Component;)V"}}, new String[][] {});
            addClass(jar, "net/minecraft/Component", null, new String[][] {}, new String[][] {});
        }
        Path classpath = tempDir.resolve("source-classpath.txt");
        Files.writeString(classpath, archive.toAbsolutePath() + System.lineSeparator(), StandardCharsets.UTF_8);

        var mappings = org.kylepitcock.legacyconnector.loader.mapping.Forge1201SourceOfficialMappingIndex
                .read(tsrg, tiny, mojmap);
        var runtime = Forge1201SourceRuntimeMemberVerifier.readClasspath(classpath);
        var result = runtime.verifyOfficialMethod(
                mappings,
                "net/minecraft/Entity",
                "doThing",
                "(Lnet/minecraft/Component;)V");

        assertEquals(
                org.kylepitcock.legacyconnector.loader.mapping.Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT,
                result.mappingStatus());
        assertEquals("m_10_", result.mappedSelectors().getFirst().srgName());
        assertEquals(
                Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_DECLARED_EXACT,
                result.members().getFirst().status());
    }

    private Path write(String name, String contents) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
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
