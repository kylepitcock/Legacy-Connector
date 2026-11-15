package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import org.kylepitcock.legacyconnector.loader.mapping.Forge1201SrgIntermediaryBridgeResolver;
import org.kylepitcock.legacyconnector.loader.mapping.Forge1201TargetOfficialMappingIndex;

final class Forge1201TargetOfficialBridgeAuditTest {
    @TempDir
    Path tempDir;

    @Test
    void composesMethodAndFieldThroughMojangNamesAndUsesTheSharedInheritedTargetIndex() throws Exception {
        Path joined = write("joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (Lb;)V m_10_ 2
                \tx f_20_ 3
                b net/minecraft/BlockPos 4
                """);
        Path sourceTiny = write("source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                METHOD\ta\t(Lb;)V\tm\tmethod_1
                FIELD\ta\tLb;\tx\tfield_1
                """);
        Path targetTiny = write("target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tc\tnet/minecraft/class_1
                CLASS\td\tnet/minecraft/class_2
                METHOD\tc\t(Ld;)V\tz\tmethod_1
                FIELD\tc\tLd;\ty\tfield_1
                """);
        Path targetOfficial = write("target-official.tsrg", """
                tsrg2 left right
                c net/minecraft/Entity
                \tz (Ld;)V getBlockState
                \ty Ld; testField
                d net/minecraft/BlockPos
                """);

        Map<String, byte[]> classes = new HashMap<>();
        classes.put("net/minecraft/Entity", entityClass());
        classes.put("net/minecraft/EntityApi", entityApiClass());
        classes.put("net/minecraft/BlockPos", emptyClass("net/minecraft/BlockPos"));

        var upstream = Forge1201SrgIntermediaryBridgeResolver.readIndex(joined, sourceTiny, targetTiny);
        var targetMappings = Forge1201TargetOfficialMappingIndex.read(targetOfficial);
        var targetIndex = Forge1201TargetApiVerifier.createIndex(internalName -> {
            byte[] bytes = classes.get(internalName);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        });

        var report = Forge1201TargetOfficialBridgeAudit.audit(upstream, targetMappings, targetIndex);

        var classResult = report.classes().stream()
                .filter(value -> "net/minecraft/Entity".equals(value.srgName()))
                .findFirst()
                .orElseThrow();
        assertEquals("TARGET_COMPILE_CLASS_FOUND", classResult.status());
        assertEquals("net/minecraft/Entity", classResult.candidates().getFirst().targetOfficialName());

        var method = report.methods().getFirst();
        assertEquals("MAPPED_EXACT", method.upstreamStatus());
        assertEquals("TARGET_API_MEMBER_FOUND_EXACT", method.status());
        assertEquals("getBlockState", method.candidates().getFirst().targetOfficialName());
        assertTrue(method.candidates().getFirst().hierarchyVisited().contains("net/minecraft/EntityApi"));

        var field = report.fields().getFirst();
        assertEquals("TARGET_API_MEMBER_FOUND_EXACT", field.status());
        assertEquals("testField", field.candidates().getFirst().targetOfficialName());
        assertEquals(2, report.classCounts().get("TARGET_COMPILE_CLASS_FOUND"));
    }

    @Test
    void keepsExternalDescriptorTypesNonExactAfterTheTargetApiSignatureMatches() throws Exception {
        Path joined = write("external-joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (Ljava/lang/String;)V m_10_ 2
                """);
        Path sourceTiny = write("external-source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                METHOD\ta\t(Ljava/lang/String;)V\tm\tmethod_1
                """);
        Path targetTiny = write("external-target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tc\tnet/minecraft/class_1
                METHOD\tc\t(Ljava/lang/String;)V\tz\tmethod_1
                """);
        Path targetOfficial = write("external-target-official.tsrg", """
                tsrg2 left right
                c net/minecraft/Entity
                \tz (Ljava/lang/String;)V readText
                """);

        Map<String, byte[]> classes = Map.of(
                "net/minecraft/Entity",
                classWithMethod("net/minecraft/Entity", "readText", "(Ljava/lang/String;)V"));
        var upstream = Forge1201SrgIntermediaryBridgeResolver.readIndex(joined, sourceTiny, targetTiny);
        var targetMappings = Forge1201TargetOfficialMappingIndex.read(targetOfficial);
        var targetIndex = Forge1201TargetApiVerifier.createIndex(internalName -> {
            byte[] bytes = classes.get(internalName);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        });

        var method = Forge1201TargetOfficialBridgeAudit.audit(upstream, targetMappings, targetIndex)
                .methods().getFirst();

        assertEquals("MAPPED_WITH_EXTERNAL_TYPES", method.upstreamStatus());
        assertEquals("TARGET_API_MEMBER_FOUND_WITH_EXTERNAL_TYPES", method.status());
        assertTrue(method.candidates().getFirst().upstreamExternalDescriptorTypeCount() > 0);
        assertEquals(1, method.candidates().getFirst().targetExternalDescriptorTypeCount());
    }

    @Test
    void preservesAmbiguousUpstreamCandidatesEvenWhenOnlyOneTargetClassIsPresent() throws Exception {
        Path joined = write("ambiguous-joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                """);
        Path sourceTiny = write("ambiguous-source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                METHOD\ta\t(I)V\tm\tmethod_1
                """);
        Path targetTiny = write("ambiguous-target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tc\tnet/minecraft/class_2
                CLASS\td\tnet/minecraft/class_3
                METHOD\tc\t(I)V\tx\tmethod_1
                METHOD\td\t(I)V\ty\tmethod_1
                """);
        Path targetOfficial = write("ambiguous-target-official.tsrg", """
                tsrg2 left right
                c net/minecraft/First
                \tx (I)V tickFirst
                d net/minecraft/Second
                \ty (I)V tickSecond
                """);

        Map<String, byte[]> classes = Map.of(
                "net/minecraft/First", classWithMethod("net/minecraft/First", "tickFirst", "(I)V"));
        var upstream = Forge1201SrgIntermediaryBridgeResolver.readIndex(joined, sourceTiny, targetTiny);
        var targetMappings = Forge1201TargetOfficialMappingIndex.read(targetOfficial);
        var targetIndex = Forge1201TargetApiVerifier.createIndex(internalName -> {
            byte[] bytes = classes.get(internalName);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        });

        var method = Forge1201TargetOfficialBridgeAudit.audit(upstream, targetMappings, targetIndex)
                .methods().getFirst();

        assertEquals("INTERMEDIARY_TARGET_AMBIGUOUS", method.upstreamStatus());
        assertEquals("INTERMEDIARY_TARGET_AMBIGUOUS", method.status());
        assertEquals(2, method.candidates().size());
        assertTrue(method.candidates().stream()
                .anyMatch(candidate -> "TARGET_API_MEMBER_FOUND_EXACT".equals(candidate.targetApiStatus())));
        assertTrue(method.candidates().stream()
                .anyMatch(candidate -> "TARGET_COMPILE_CLASS_MISSING".equals(candidate.targetApiStatus())));
        assertTrue(method.candidates().stream()
                .allMatch(candidate -> "MAPPED_EXACT".equals(candidate.targetOfficialMappingStatus())));
    }

    private Path write(String name, String contents) throws Exception {
        Path path = tempDir.resolve(name);
        Files.writeString(path, contents, StandardCharsets.UTF_8);
        return path;
    }

    private static byte[] emptyClass(String name) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] entityClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "net/minecraft/Entity", null,
                "java/lang/Object", new String[] {"net/minecraft/EntityApi"});
        writer.visitField(Opcodes.ACC_PUBLIC, "testField", "Lnet/minecraft/BlockPos;", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] entityApiClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                "net/minecraft/EntityApi", null, "java/lang/Object", null);
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "getBlockState", "(Lnet/minecraft/BlockPos;)V", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithMethod(String owner, String method, String descriptor) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        var methodWriter = writer.visitMethod(Opcodes.ACC_PUBLIC, method, descriptor, null, null);
        methodWriter.visitCode();
        methodWriter.visitInsn(Opcodes.RETURN);
        methodWriter.visitMaxs(0, 0);
        methodWriter.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }
}
