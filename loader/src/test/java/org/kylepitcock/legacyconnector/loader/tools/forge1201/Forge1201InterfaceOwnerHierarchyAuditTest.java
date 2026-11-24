package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class Forge1201InterfaceOwnerHierarchyAuditTest {
    @Test
    void verifiesTheThreeInterfaceOwnerPathsAndRejectsOutOfHierarchyAlternates() throws Exception {
        var source = index(true, false);
        var target = index(true, false);

        var result = Forge1201InterfaceOwnerHierarchyAudit.analyze(source, target);

        assertEquals(3, result.cases().size());
        for (String caseReport : result.cases()) {
            assertTrue(caseReport.contains("CASE_STATUS=VERIFIED"), caseReport);
            assertTrue(caseReport.contains("HIERARCHY_COMPLETE=true"), caseReport);
            assertTrue(caseReport.contains("MAPPING_OWNER_IN_HIERARCHY=true"), caseReport);
            assertTrue(caseReport.contains("MAPPING_OWNER_DECLARES_EXACT=true"), caseReport);
            assertTrue(caseReport.contains("INVOCATION_METHOD_PUBLIC_INSTANCE=true"), caseReport);
            assertTrue(caseReport.contains("MAPPING_OWNER_METHOD_PUBLIC_INSTANCE=true"), caseReport);
            assertTrue(caseReport.contains("FALSE_ALTERNATES_IN_HIERARCHY=[]"), caseReport);
        }

        String level = result.cases().stream()
                .filter(value -> value.contains("CASE=LEVEL_BLOCK_STATE"))
                .findFirst()
                .orElseThrow();
        assertTrue(level.contains("SOURCE_INTERMEDIARY_MAPPING_OWNER=net/minecraft/class_1922"));
        assertTrue(level.contains("SOURCE_INTERMEDIARY_METHOD_ID=method_8320"));
        assertTrue(level.contains("SOURCE_MAPPING_INTERFACE_OWNER=net/minecraft/world/level/BlockGetter"));
        assertTrue(level.contains("net/minecraft/world/level/LevelReader"));
        assertTrue(level.contains("net/minecraft/world/level/BlockAndTintGetter"));
        assertTrue(level.contains("net/minecraft/world/level/BlockGetter"));
    }

    @Test
    void refusesTheLevelCandidateWhenTheSourceHierarchyDoesNotReachBlockGetter() throws Exception {
        var source = index(false, false);
        var target = index(true, false);

        var result = Forge1201InterfaceOwnerHierarchyAudit.analyze(source, target);

        String level = result.cases().stream()
                .filter(value -> value.contains("CASE=LEVEL_BLOCK_STATE"))
                .findFirst()
                .orElseThrow();
        assertTrue(level.contains("SOURCE_STATUS=MAPPING_OWNER_NOT_IN_HIERARCHY"), level);
        assertTrue(level.contains("CASE_STATUS=SOURCE_MAPPING_OWNER_NOT_IN_HIERARCHY"), level);
        assertTrue(level.contains("TARGET_STATUS=VERIFIED"), level);
    }

    @Test
    void refusesToInferUniquenessWhenTheSourceHierarchyIsIncomplete() throws Exception {
        var source = index(true, true);
        var target = index(true, false);

        var result = Forge1201InterfaceOwnerHierarchyAudit.analyze(source, target);

        String level = result.cases().stream()
                .filter(value -> value.contains("CASE=LEVEL_BLOCK_STATE"))
                .findFirst()
                .orElseThrow();
        assertTrue(level.contains("SOURCE_STATUS=HIERARCHY_INCOMPLETE"), level);
        assertTrue(level.contains("CASE_STATUS=SOURCE_HIERARCHY_INCOMPLETE"), level);
        assertTrue(level.contains("sample/UnresolvedLevelInterface"), level);
    }

    private static Forge1201SourceRuntimeMemberVerifier.Index index(
            boolean connectBlockGetter,
            boolean leaveOneInterfaceMissing) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();

        addInterface(classes, "net/minecraft/world/level/entity/EntityAccess", new String[0],
                new String[][] {
                        {"getUUID", "()Ljava/util/UUID;"},
                        {"getBoundingBox", "()Lnet/minecraft/world/phys/AABB;"}
                });
        addClass(classes, "net/minecraft/world/entity/Entity",
                new String[] {"net/minecraft/world/level/entity/EntityAccess"},
                new String[][] {
                        {"getUUID", "()Ljava/util/UUID;"},
                        {"getBoundingBox", "()Lnet/minecraft/world/phys/AABB;"}
                });

        addInterface(classes, "net/minecraft/world/level/BlockGetter", new String[0],
                new String[][] {
                        {"getBlockState",
                                "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"}
                });
        addInterface(classes, "net/minecraft/world/level/BlockAndTintGetter",
                new String[] {"net/minecraft/world/level/BlockGetter"}, new String[0][]);
        String[] readerParents;
        if (leaveOneInterfaceMissing) {
            readerParents = new String[] {"sample/UnresolvedLevelInterface"};
        } else if (connectBlockGetter) {
            readerParents = new String[] {"net/minecraft/world/level/BlockAndTintGetter"};
        } else {
            readerParents = new String[] {"net/minecraft/world/level/CollisionGetter"};
        }
        addInterface(classes, "net/minecraft/world/level/LevelReader", readerParents, new String[0][]);
        addInterface(classes, "net/minecraft/world/level/EntityGetter", new String[0], new String[0][]);
        addInterface(classes, "net/minecraft/world/level/LevelSimulatedRW", new String[0], new String[0][]);
        addInterface(classes, "net/minecraft/world/level/LevelTimeAccess", new String[0], new String[0][]);
        addInterface(classes, "net/minecraft/world/level/CollisionGetter", new String[0], new String[0][]);
        addInterface(classes, "net/minecraft/world/level/SignalGetter", new String[0], new String[0][]);
        addInterface(classes, "net/minecraft/world/level/BiomeManager$NoiseBiomeSource", new String[0], new String[0][]);
        addInterface(classes, "net/minecraft/world/level/CommonLevelAccessor",
                new String[] {
                        "net/minecraft/world/level/EntityGetter",
                        "net/minecraft/world/level/LevelReader",
                        "net/minecraft/world/level/LevelSimulatedRW"
                }, new String[0][]);
        addInterface(classes, "net/minecraft/world/level/LevelAccessor",
                new String[] {
                        "net/minecraft/world/level/CommonLevelAccessor",
                        "net/minecraft/world/level/LevelTimeAccess"
                }, new String[0][]);
        addClass(classes, "net/minecraft/world/level/Level",
                new String[] {"net/minecraft/world/level/LevelAccessor"},
                new String[][] {
                        {"getBlockState",
                                "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"}
                });

        addClass(classes, "net/minecraft/client/renderer/chunk/RenderChunkRegion", new String[0], new String[0][]);
        addClass(classes, "net/minecraft/server/level/WorldGenRegion", new String[0], new String[0][]);
        addClass(classes, "net/minecraft/world/level/chunk/EmptyLevelChunk", new String[0], new String[0][]);
        addClass(classes, "net/minecraft/world/level/chunk/ImposterProtoChunk", new String[0], new String[0][]);
        addClass(classes, "net/minecraft/world/level/chunk/LevelChunk", new String[0], new String[0][]);
        addClass(classes, "net/minecraft/world/level/chunk/ProtoChunk", new String[0], new String[0][]);
        addClass(classes, "net/minecraft/world/level/EmptyBlockGetter", new String[0], new String[0][]);
        addClass(classes, "net/minecraft/world/level/PathNavigationRegion", new String[0], new String[0][]);

        return Forge1201SourceRuntimeMemberVerifier.createIndex(internalName -> {
            byte[] bytes = classes.get(internalName);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        });
    }

    private static void addInterface(
            Map<String, byte[]> classes,
            String name,
            String[] interfaces,
            String[][] methods) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                name, null, "java/lang/Object", interfaces);
        for (String[] method : methods) {
            writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    method[0], method[1], null, null).visitEnd();
        }
        writer.visitEnd();
        classes.put(name, writer.toByteArray());
    }

    private static void addClass(
            Map<String, byte[]> classes,
            String name,
            String[] interfaces,
            String[][] methods) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", interfaces);
        for (String[] method : methods) {
            MethodVisitor visitor = writer.visitMethod(Opcodes.ACC_PUBLIC, method[0], method[1], null, null);
            visitor.visitCode();
            visitor.visitInsn(Opcodes.ACONST_NULL);
            visitor.visitInsn(Opcodes.ARETURN);
            visitor.visitMaxs(0, 0);
            visitor.visitEnd();
        }
        writer.visitEnd();
        classes.put(name, writer.toByteArray());
    }
}
