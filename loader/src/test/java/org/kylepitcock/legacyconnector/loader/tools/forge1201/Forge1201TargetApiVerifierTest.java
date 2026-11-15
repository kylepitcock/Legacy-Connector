package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

final class Forge1201TargetApiVerifierTest {
    private static final String TARGET_MOD = "net/neoforged/fml/common/Mod";
    private static final String TARGET_BUS = "net/neoforged/bus/api/IEventBus";

    @Test
    void verifiesMappedClassAndMemberDescriptorsWithoutLoadingClasses() throws Exception {
        Map<String, byte[]> classes = Map.of(
                TARGET_MOD, targetModClass("net/neoforged/fml/Base", false, true),
                TARGET_BUS, targetInterface(TARGET_BUS),
                "net/neoforged/fml/Base", targetBaseClass());

        Forge1201TargetApiVerifier.Result result = verify(
                analysis(
                        List.of("net/minecraftforge/fml/common/Mod", "net/minecraftforge/eventbus/api/IEventBus"),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/fml/common/Mod",
                                "register",
                                "(Lnet/minecraftforge/eventbus/api/IEventBus;)V",
                                1)),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/fml/common/Mod",
                                "BUS",
                                "Lnet/minecraftforge/eventbus/api/IEventBus;",
                                1))),
                classes);

        assertEquals(2, result.candidateClassCount());
        assertEquals(2, result.verifiedClassCount());
        assertTrue(result.missingTargetClasses().isEmpty());
        assertEquals(1, result.uniqueMethodReferences());
        assertEquals(1, result.verifiedMethodReferences());
        assertTrue(result.methodIssues().isEmpty());
        assertEquals(1, result.uniqueFieldReferences());
        assertEquals(1, result.verifiedFieldReferences());
        assertTrue(result.fieldIssues().isEmpty());
    }

    @Test
    void verifiesAlreadyRelocatedNeoForgeOwnerAndMemberReferences() throws Exception {
        Map<String, byte[]> classes = Map.of(
                TARGET_MOD, targetModClass("net/neoforged/fml/Base", false, true),
                TARGET_BUS, targetInterface(TARGET_BUS),
                "net/neoforged/fml/Base", targetBaseClass());

        Forge1201TargetApiVerifier.Result result = verify(
                analysis(
                        List.of(TARGET_MOD, TARGET_BUS),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                TARGET_MOD, "register",
                                "(Lnet/neoforged/bus/api/IEventBus;)V", 1)),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                TARGET_MOD, "BUS",
                                "Lnet/neoforged/bus/api/IEventBus;", 1))),
                classes);

        assertEquals(2, result.candidateClassCount());
        assertEquals(2, result.verifiedClassCount());
        assertTrue(result.missingTargetClasses().isEmpty());
        assertEquals(1, result.uniqueMethodReferences());
        assertEquals(1, result.verifiedMethodReferences());
        assertTrue(result.methodIssues().isEmpty());
        assertEquals(1, result.uniqueFieldReferences());
        assertEquals(1, result.verifiedFieldReferences());
        assertTrue(result.fieldIssues().isEmpty());
    }

    @Test
    void reportsChangedOrMissingTargetMemberSignatures() throws Exception {
        Map<String, byte[]> classes = Map.of(
                TARGET_MOD, targetModClass("java/lang/Object", false, true),
                TARGET_BUS, targetInterface(TARGET_BUS),
                "java/lang/Object", targetEmptyClass("java/lang/Object", null));

        Forge1201TargetApiVerifier.Result result = verify(
                analysis(
                        List.of("net/minecraftforge/fml/common/Mod", "net/minecraftforge/eventbus/api/IEventBus"),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/fml/common/Mod", "register",
                                "(Lnet/minecraftforge/eventbus/api/IEventBus;)V", 1)),
                        List.of()),
                classes);

        assertEquals(2, result.verifiedClassCount());
        assertEquals(0, result.verifiedMethodReferences());
        assertEquals(1, result.methodIssues().size());
        assertEquals(Forge1201TargetApiVerifier.MemberStatus.MISSING,
                result.methodIssues().get(0).status());
    }

    @Test
    void reportsMissingTargetTypesAndIncompleteHierarchySeparately() throws Exception {
        Map<String, byte[]> classes = Map.of(
                TARGET_MOD, targetModClass("net/neoforged/fml/UnseenParent", false, false),
                TARGET_BUS, targetInterface(TARGET_BUS));

        Forge1201TargetApiVerifier.Result result = verify(
                analysis(
                        List.of(
                                "net/minecraftforge/fml/common/Mod",
                                "net/minecraftforge/eventbus/api/IEventBus",
                                "net/minecraftforge/client/event/DoesNotExist"),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/fml/common/Mod", "inherited",
                                "()V", 1)),
                        List.of()),
                classes);

        assertEquals(2, result.verifiedClassCount());
        assertEquals(List.of("net/neoforged/neoforge/client/event/DoesNotExist"),
                result.missingTargetClasses());
        assertEquals(Forge1201TargetApiVerifier.MemberStatus.INCOMPLETE_HIERARCHY,
                result.methodIssues().get(0).status());
    }

    @Test
    void reportsManualAdapterTypesEmbeddedInCandidateMemberDescriptors() throws Exception {
        String targetFactory = "net/neoforged/neoforge/network/IContainerFactory";
        Map<String, byte[]> classes = Map.of(
                targetFactory, targetClass(targetFactory, "java/lang/Object", false, false));

        Forge1201TargetApiVerifier.Result result = verify(
                analysis(
                        List.of(
                                "net/minecraftforge/network/IContainerFactory",
                                "net/minecraftforge/registries/IForgeRegistry"),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/network/IContainerFactory",
                                "create",
                                "(Lnet/minecraftforge/registries/IForgeRegistry;)V",
                                1)),
                        List.of()),
                classes);

        assertEquals(1, result.candidateClassCount());
        assertEquals(1, result.verifiedClassCount());
        assertEquals(0, result.verifiedMethodReferences());
        assertEquals(1, result.methodIssues().size());
        assertEquals(Forge1201TargetApiVerifier.MemberStatus.REQUIRES_MANUAL_DESCRIPTOR_ADAPTER,
                result.methodIssues().get(0).status());
    }

    @Test
    void mapsExplicitNetworkShimOwnersAndDescriptorsToConnectorRuntimeTypes() throws Exception {
        String sourceContext = "net/minecraftforge/network/NetworkEvent$Context";
        String sourceDirection = "net/minecraftforge/network/NetworkDirection";
        String targetContext = "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkEvent$Context";
        String targetDirection = "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkDirection";
        String memberDescriptor = "(L" + targetDirection + ";)V";
        String fieldDescriptor = "L" + targetDirection + ";";

        ClassWriter contextWriter = new ClassWriter(0);
        contextWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, targetContext, null, "java/lang/Object", null);
        var setter = contextWriter.visitMethod(Opcodes.ACC_PUBLIC, "setDirection", memberDescriptor, null, null);
        setter.visitCode();
        setter.visitInsn(Opcodes.RETURN);
        setter.visitMaxs(0, 2);
        setter.visitEnd();
        contextWriter.visitField(Opcodes.ACC_PUBLIC, "direction", fieldDescriptor, null, null).visitEnd();
        contextWriter.visitEnd();

        Map<String, byte[]> classes = Map.of(
                targetContext, contextWriter.toByteArray(),
                targetDirection, targetEmptyClass(targetDirection, "java/lang/Object"));

        Forge1201TargetApiVerifier.Result result = verify(
                analysis(
                        List.of(sourceContext, sourceDirection),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                sourceContext, "setDirection",
                                "(L" + sourceDirection + ";)V", 1)),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                sourceContext, "direction", "L" + sourceDirection + ";", 1))),
                classes);

        assertEquals(2, result.candidateClassCount());
        assertEquals(2, result.verifiedClassCount());
        assertTrue(result.missingTargetClasses().isEmpty());
        assertEquals(1, result.uniqueMethodReferences());
        assertEquals(1, result.verifiedMethodReferences());
        assertTrue(result.methodIssues().isEmpty());
        assertEquals(1, result.uniqueFieldReferences());
        assertEquals(1, result.verifiedFieldReferences());
        assertTrue(result.fieldIssues().isEmpty());
    }

    @Test
    void ignoresManualAdapterOwnersBecauseTheyAreNotTypeReplacements() throws Exception {
        Forge1201TargetApiVerifier.Result result = verify(
                analysis(
                        List.of("net/minecraftforge/network/NetworkHooks"),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/network/NetworkHooks",
                                "openScreen", "()V", 1)),
                        List.of()),
                Map.of());

        assertEquals(0, result.candidateClassCount());
        assertEquals(0, result.uniqueMethodReferences());
        assertTrue(result.missingTargetClasses().isEmpty());
    }

    @Test
    void attachesDirectCallerLocationsToUnresolvedMethodAndFieldReferences() throws Exception {
        Map<String, byte[]> classes = Map.of(
                TARGET_MOD, targetModClass("java/lang/Object", false, false),
                "java/lang/Object", targetEmptyClass("java/lang/Object", null));

        Forge1201TargetApiVerifier.Result result = verify(
                analysis(
                        List.of("net/minecraftforge/fml/common/Mod"),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/fml/common/Mod", "register", "()V", 2)),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/fml/common/Mod", "BUS", "I", 1)),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMethodCallSite(
                                "fixture/Caller", "invoke", "()V",
                                "net/minecraftforge/fml/common/Mod", "register", "()V", Opcodes.INVOKEVIRTUAL, 2)),
                        List.of(new Forge1201ApiUsageAnalyzer.ApiFieldCallSite(
                                "fixture/Caller", "read", "()V",
                                "net/minecraftforge/fml/common/Mod", "BUS", "I", Opcodes.GETSTATIC, 1))),
                classes);

        assertEquals(1, result.methodIssues().size());
        assertEquals(List.of(
                "net.minecraftforge.fml.common.Mod#register()V <= fixture.Caller#invoke()V [INVOKEVIRTUAL] (x2)"),
                result.methodIssues().get(0).callSites());

        assertEquals(1, result.fieldIssues().size());
        assertEquals(List.of(
                "net.minecraftforge.fml.common.Mod#BUS:I <= fixture.Caller#read()V [GETSTATIC]"),
                result.fieldIssues().get(0).callSites());
    }

    @Test
    void exposesReusableTargetIndexWithExactDescriptorsAndInheritedMembers() throws Exception {
        String base = "fixture/Base";
        String child = "fixture/Child";

        ClassWriter baseWriter = new ClassWriter(0);
        baseWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, base, null, "java/lang/Object", null);
        baseWriter.visitMethod(Opcodes.ACC_PUBLIC, "resolve", "()Ljava/lang/String;", null, null).visitEnd();
        baseWriter.visitField(Opcodes.ACC_PUBLIC, "identifier", "I", null, null).visitEnd();
        baseWriter.visitEnd();

        ClassWriter childWriter = new ClassWriter(0);
        childWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, child, null, base, null);
        childWriter.visitMethod(Opcodes.ACC_PUBLIC, "resolve", "(I)Ljava/lang/String;", null, null).visitEnd();
        childWriter.visitEnd();

        Map<String, byte[]> classes = Map.of(
                child, childWriter.toByteArray(),
                base, baseWriter.toByteArray(),
                "java/lang/Object", targetEmptyClass("java/lang/Object", null));
        Forge1201TargetApiVerifier.TargetIndex index =
                Forge1201TargetApiVerifier.createIndex(internalName -> {
                    byte[] bytes = classes.get(internalName);
                    return bytes == null ? null : new ByteArrayInputStream(bytes);
                });

        var childInfo = index.classInfo(child).orElseThrow();
        assertEquals(base, childInfo.superName());
        assertTrue(childInfo.methods().contains(
                new Forge1201TargetApiVerifier.MemberSignature("resolve", "(I)Ljava/lang/String;")));

        var inheritedMethod = index.lookupMethod(child, "resolve", "()Ljava/lang/String;");
        assertEquals(Forge1201TargetApiVerifier.LookupStatus.FOUND, inheritedMethod.status());
        assertEquals(List.of(child, base), inheritedMethod.hierarchyVisited());

        var inheritedField = index.lookupField(child, "identifier", "I");
        assertEquals(Forge1201TargetApiVerifier.LookupStatus.FOUND, inheritedField.status());

        var wrongMethodDescriptor = index.lookupMethod(child, "resolve", "(Ljava/lang/String;)Ljava/lang/String;");
        assertEquals(Forge1201TargetApiVerifier.LookupStatus.MISSING, wrongMethodDescriptor.status());

        var wrongFieldDescriptor = index.lookupField(child, "identifier", "J");
        assertEquals(Forge1201TargetApiVerifier.LookupStatus.MISSING, wrongFieldDescriptor.status());
        assertTrue(index.classInfo("fixture/Absent").isEmpty());
    }

    @Test
    void targetIndexDistinguishesIncompleteHierarchyAndDoesNotShareCaches() throws Exception {
        String child = "fixture/Child";
        String missingParent = "fixture/NotIndexedParent";
        byte[] incompleteChild = targetEmptyClass(child, missingParent);

        Forge1201TargetApiVerifier.TargetIndex incomplete =
                Forge1201TargetApiVerifier.createIndex(internalName ->
                        child.equals(internalName) ? new ByteArrayInputStream(incompleteChild) : null);
        var unresolved = incomplete.lookupMethod(child, "inherited", "()V");
        assertEquals(Forge1201TargetApiVerifier.LookupStatus.INCOMPLETE_HIERARCHY, unresolved.status());
        assertEquals(List.of(child, missingParent), unresolved.hierarchyVisited());

        byte[] completeChild = targetEmptyClass(child, "java/lang/Object");
        Forge1201TargetApiVerifier.TargetIndex independent =
                Forge1201TargetApiVerifier.createIndex(internalName -> {
                    byte[] bytes = child.equals(internalName)
                            ? completeChild
                            : "java/lang/Object".equals(internalName)
                                    ? targetEmptyClass("java/lang/Object", null)
                                    : null;
                    return bytes == null ? null : new ByteArrayInputStream(bytes);
                });
        var absent = independent.lookupMethod(child, "inherited", "()V");
        assertEquals(Forge1201TargetApiVerifier.LookupStatus.MISSING, absent.status());
        assertTrue(independent.classInfo(missingParent).isEmpty());
    }

    private static Forge1201ApiUsageAnalyzer.Analysis analysis(
            List<String> classes,
            List<Forge1201ApiUsageAnalyzer.ApiMemberReference> methods,
            List<Forge1201ApiUsageAnalyzer.ApiMemberReference> fields) {
        return analysis(classes, methods, fields, List.of(), List.of());
    }

    private static Forge1201ApiUsageAnalyzer.Analysis analysis(
            List<String> classes,
            List<Forge1201ApiUsageAnalyzer.ApiMemberReference> methods,
            List<Forge1201ApiUsageAnalyzer.ApiMemberReference> fields,
            List<Forge1201ApiUsageAnalyzer.ApiMethodCallSite> methodCallSites,
            List<Forge1201ApiUsageAnalyzer.ApiFieldCallSite> fieldCallSites) {
        Forge1201JarInfo metadata = new Forge1201JarInfo(
                Path.of("synthetic.jar"),
                "javafml",
                "[47,)",
                "[1.20.1,1.21)",
                "[47,)",
                List.of(new Forge1201JarInfo.Mod("fixture", "1.0", "Fixture", List.of())));
        return new Forge1201ApiUsageAnalyzer.Analysis(
                metadata, 1, List.of(), List.of(), null, Map.of(), classes,
                methodCallSites, fieldCallSites, methods, fields);
    }

    private static byte[] targetModClass(String superName, boolean registerMethod, boolean busField) {
        return targetClass(TARGET_MOD, superName, registerMethod, busField);
    }

    private static byte[] targetClass(
            String className, String superName, boolean registerMethod, boolean busField) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, className, null, superName, null);
        if (registerMethod) {
            writer.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    "register",
                    "(Lnet/neoforged/bus/api/IEventBus;)V",
                    null,
                    null).visitEnd();
        }
        if (busField) {
            writer.visitField(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    "BUS",
                    "Lnet/neoforged/bus/api/IEventBus;",
                    null,
                    null).visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] targetEmptyClass(String name, String superName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] targetBaseClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "net/neoforged/fml/Base", null,
                "java/lang/Object", null);
        writer.visitMethod(Opcodes.ACC_PUBLIC, "register",
                "(Lnet/neoforged/bus/api/IEventBus;)V", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] targetInterface(String name) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                name, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Forge1201TargetApiVerifier.Result verify(
            Forge1201ApiUsageAnalyzer.Analysis analysis,
            Map<String, byte[]> classes) throws IOException {
        return Forge1201TargetApiVerifier.verify(analysis, internalName -> {
            byte[] bytes = classes.get(internalName);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        });
    }
}
