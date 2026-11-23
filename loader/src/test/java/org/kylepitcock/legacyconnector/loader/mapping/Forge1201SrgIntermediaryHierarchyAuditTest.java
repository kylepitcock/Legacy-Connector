package org.kylepitcock.legacyconnector.loader.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201SourceRuntimeMemberVerifier;

final class Forge1201SrgIntermediaryHierarchyAuditTest {
    @TempDir
    Path tempDir;

    @Test
    void buildsSourceHierarchyAndVerifiesTargetInterfaceOwnerEndToEnd() throws Exception {
        Fixture fixture = fixture(false, true, false, true);

        var result = audit(fixture);

        assertEquals(
                Forge1201SrgIntermediaryHierarchyAudit.AuditStatus.TARGET_VERIFIED,
                result.status(), result.detail());
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.MAPPED_VIA_INTERFACE_OWNER,
                result.ownerAwareResolution().status());
        assertEquals("net/minecraft/Entity", result.sourceInvocationOfficialOwner());
        assertEquals("net/minecraft/world/level/entity/EntityAccess", result.ownerAwareResolution()
                .candidates().getFirst().mappingOfficialOwner());
        assertEquals("net/minecraft/world/entity/Entity", result.targetInvocationOfficialOwner());
        assertEquals("net/minecraft/world/level/entity/EntityAccess", result.targetMappingOfficialOwner());
        assertTrue(result.sourceHierarchyVisited().contains("net/minecraft/EntityAccess"));
        assertTrue(!result.sourceHierarchyVisited().contains("net/minecraft/Other"),
                "An unrelated global owner must not enter the verified hierarchy.");
        assertTrue(result.targetHierarchyVisited().contains("net/minecraft/world/level/entity/EntityAccess"));
    }

    @Test
    void refusesSourceFallbackWhenAnyHierarchyClassfileIsMissing() throws Exception {
        Fixture fixture = fixture(false, true, true, true);

        var result = audit(fixture);

        assertEquals(
                Forge1201SrgIntermediaryHierarchyAudit.AuditStatus.SOURCE_HIERARCHY_INCOMPLETE,
                result.status());
        assertTrue(result.detail().contains("hierarchy"));
    }

    @Test
    void refusesTwoMappedInterfaceDeclarationsInsideTheActualHierarchy() throws Exception {
        Fixture fixture = fixture(true, true, false, true);

        var result = audit(fixture);

        assertEquals(
                Forge1201SrgIntermediaryHierarchyAudit.AuditStatus.SOURCE_OWNER_FALLBACK_REJECTED,
                result.status());
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.AMBIGUOUS_HIERARCHY_OWNERS,
                result.ownerAwareResolution().status());
        assertEquals(2, result.ownerAwareResolution().candidates().size());
    }

    @Test
    void refusesFallbackWhenARealAncestorDeclarationHasNoIntermediaryIdentity() throws Exception {
        Fixture fixture = fixture(false, false, false, true);

        var result = audit(fixture);

        assertEquals(
                Forge1201SrgIntermediaryHierarchyAudit.AuditStatus.SOURCE_OWNER_FALLBACK_REJECTED,
                result.status());
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.UNRESOLVED_HIERARCHY_CANDIDATE,
                result.ownerAwareResolution().status());
    }

    @Test
    void refusesTargetMappingOwnerThatIsNotInTheRealTargetInvocationHierarchy() throws Exception {
        Fixture fixture = fixture(false, true, false, false);

        var result = audit(fixture);

        assertEquals(
                Forge1201SrgIntermediaryHierarchyAudit.AuditStatus.TARGET_MAPPING_OWNER_NOT_IN_HIERARCHY,
                result.status());
    }

    private Forge1201SrgIntermediaryHierarchyAudit.MethodAuditResult audit(Fixture fixture)
            throws Exception {
        var intermediary = Forge1201SrgIntermediaryBridgeResolver.readIndex(
                fixture.joinedTsrg(), fixture.sourceTiny(), fixture.targetTiny());
        var sourceMappings = Forge1201SourceOfficialMappingIndex.read(
                fixture.joinedTsrg(), fixture.sourceTiny(), fixture.sourceMojang());
        var targetMappings = Forge1201TargetOfficialMappingIndex.read(fixture.targetOfficialTsrg());
        var sourceClasses = Forge1201SourceRuntimeMemberVerifier.createIndex(
                name -> open(fixture.sourceClassBytes(), name));
        var targetClasses = Forge1201SourceRuntimeMemberVerifier.createIndex(
                name -> open(fixture.targetClassBytes(), name));

        return Forge1201SrgIntermediaryHierarchyAudit.analyzeMethod(
                "ENTITY_GET_THING",
                "net/minecraft/src/C_Entity_",
                "m_10_",
                "()V",
                intermediary,
                sourceMappings,
                sourceClasses,
                targetMappings,
                targetClasses);
    }

    private Fixture fixture(
            boolean addSecondInterface,
            boolean mapFirstInterfaceMethod,
            boolean addMissingSourceInterface,
            boolean connectTargetInterface) throws Exception {
        Path joinedTsrg = write("joined-" + System.nanoTime() + ".tsrg", """
                tsrg2 obf srg id
                a net/minecraft/src/C_Entity_ 1
                \tm ()V m_10_ 2
                b net/minecraft/src/C_EntityAccess_ 3
                \tx ()V m_10_ 4
                c net/minecraft/src/C_AlternateAccess_ 5
                \ty ()V m_10_ 6
                z net/minecraft/src/C_Unrelated_ 7
                \tr ()V m_10_ 8
                """);

        String sourceTinyText = """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                CLASS\tc\tnet/minecraft/class_3
                CLASS\tz\tnet/minecraft/class_4
                """;
        if (mapFirstInterfaceMethod) {
            sourceTinyText += "METHOD\tb\t()V\tx\tmethod_10\n";
        }
        if (addSecondInterface) {
            sourceTinyText += "METHOD\tc\t()V\ty\tmethod_20\n";
        }
        sourceTinyText += "METHOD\tz\t()V\tr\tmethod_99\n";
        Path sourceTiny = write("source-" + System.nanoTime() + ".tiny", sourceTinyText);

        String targetTinyText = """
                v1\tofficial\tintermediary
                CLASS\td\tnet/minecraft/class_1
                CLASS\te\tnet/minecraft/class_2
                CLASS\tf\tnet/minecraft/class_3
                CLASS\tg\tnet/minecraft/class_4
                METHOD\te\t()V\tq\tmethod_10
                METHOD\tg\t()V\ts\tmethod_99
                """;
        if (addSecondInterface) {
            targetTinyText += "METHOD\tf\t()V\tp\tmethod_20\n";
        }
        Path targetTiny = write("target-" + System.nanoTime() + ".tiny", targetTinyText);

        Path sourceMojang = write("source-" + System.nanoTime() + ".txt", """
                net.minecraft.Entity -> a:
                    void getThing() -> m
                net.minecraft.EntityAccess -> b:
                    void getThing() -> x
                net.minecraft.AlternateAccess -> c:
                    void getThing() -> y
                net.minecraft.Other -> z:
                    void getThing() -> r
                """);

        Path targetOfficial = write("target-" + System.nanoTime() + ".tsrg", """
                tsrg2 left right
                d net/minecraft/world/entity/Entity
                \ta ()V getThing
                e net/minecraft/world/level/entity/EntityAccess
                \tq ()V getThing
                f net/minecraft/world/level/entity/AlternateAccess
                \tp ()V getThing
                g net/minecraft/Other
                \ts ()V getThing
                """);

        Map<String, byte[]> sourceBytes = new HashMap<>();
        String[] entityInterfaces;
        if (addSecondInterface && addMissingSourceInterface) {
            entityInterfaces = new String[] {
                    "net/minecraft/EntityAccess",
                    "net/minecraft/AlternateAccess",
                    "sample/MissingSourceInterface"
            };
        } else if (addSecondInterface) {
            entityInterfaces = new String[] {
                    "net/minecraft/EntityAccess", "net/minecraft/AlternateAccess"
            };
        } else if (addMissingSourceInterface) {
            entityInterfaces = new String[] {
                    "net/minecraft/EntityAccess", "sample/MissingSourceInterface"
            };
        } else {
            entityInterfaces = new String[] {"net/minecraft/EntityAccess"};
        }
        addInterface(sourceBytes, "net/minecraft/EntityAccess", "getThing", "()V");
        addInterface(sourceBytes, "net/minecraft/AlternateAccess", "getThing", "()V");
        addClass(sourceBytes, "net/minecraft/Entity", entityInterfaces, "getThing", "()V");
        addClass(sourceBytes, "net/minecraft/Other", new String[0], "getThing", "()V");

        Map<String, byte[]> targetBytes = new HashMap<>();
        addInterface(targetBytes, "net/minecraft/world/level/entity/EntityAccess", "getThing", "()V");
        addInterface(targetBytes, "net/minecraft/world/level/entity/AlternateAccess", "getThing", "()V");
        addClass(targetBytes, "net/minecraft/world/entity/Entity",
                connectTargetInterface
                        ? addSecondInterface
                                ? new String[] {
                                        "net/minecraft/world/level/entity/EntityAccess",
                                        "net/minecraft/world/level/entity/AlternateAccess"
                                }
                                : new String[] {"net/minecraft/world/level/entity/EntityAccess"}
                        : new String[0],
                "getThing", "()V");
        addClass(targetBytes, "net/minecraft/Other", new String[0], "getThing", "()V");

        return new Fixture(joinedTsrg, sourceTiny, targetTiny, sourceMojang, targetOfficial,
                sourceBytes, targetBytes);
    }

    private static ByteArrayInputStream open(Map<String, byte[]> classes, String internalName) {
        byte[] bytes = classes.get(internalName);
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }

    private static void addInterface(
            Map<String, byte[]> classes, String name, String methodName, String descriptor) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                name, null, "java/lang/Object", new String[0]);
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                methodName, descriptor, null, null).visitEnd();
        writer.visitEnd();
        classes.put(name, writer.toByteArray());
    }

    private static void addClass(
            Map<String, byte[]> classes,
            String name,
            String[] interfaces,
            String methodName,
            String descriptor) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", interfaces);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, methodName, descriptor, null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        classes.put(name, writer.toByteArray());
    }

    private Path write(String name, String contents) throws Exception {
        Path path = tempDir.resolve(name);
        Files.writeString(path, contents, StandardCharsets.UTF_8);
        return path;
    }

    private record Fixture(
            Path joinedTsrg,
            Path sourceTiny,
            Path targetTiny,
            Path sourceMojang,
            Path targetOfficialTsrg,
            Map<String, byte[]> sourceClassBytes,
            Map<String, byte[]> targetClassBytes) {}
}
