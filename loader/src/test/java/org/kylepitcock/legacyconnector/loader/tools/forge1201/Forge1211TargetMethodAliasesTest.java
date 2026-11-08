package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

final class Forge1211TargetMethodAliasesTest {
    private static final String OWNER = "net/minecraft/resources/ResourceLocation";
    private static final String OTHER_OWNER = "net/minecraft/resources/OtherResourceLocation";
    private static final String TARGET_NAME = "m_339182_";
    private static final String READABLE_NAME = "fromNamespaceAndPath";
    private static final String DESCRIPTOR =
            "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;";
    private static final String WRONG_DESCRIPTOR =
            "(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;";

    @TempDir
    Path temporaryDirectory;

    @Test
    void aliasesOnlyTheExactTargetOwnerNameAndDescriptor() {
        Forge1201ApiUsageAnalyzer.Analysis source = analysis(
                List.of(
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(OWNER, TARGET_NAME, DESCRIPTOR, 3),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(OTHER_OWNER, TARGET_NAME, DESCRIPTOR, 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(OWNER, TARGET_NAME, WRONG_DESCRIPTOR, 1)),
                List.of(
                        callSite(OWNER, TARGET_NAME, DESCRIPTOR),
                        callSite(OTHER_OWNER, TARGET_NAME, DESCRIPTOR),
                        callSite(OWNER, TARGET_NAME, WRONG_DESCRIPTOR)));

        Forge1211TargetMethodAliases.Normalization result =
                Forge1211TargetMethodAliases.normalize(source);

        assertEquals(READABLE_NAME, result.analysis().methodReferences().get(0).name());
        assertEquals(TARGET_NAME, result.analysis().methodReferences().get(1).name());
        assertEquals(TARGET_NAME, result.analysis().methodReferences().get(2).name());
        assertEquals(READABLE_NAME, result.analysis().methodCallSites().get(0).targetName());
        assertEquals(TARGET_NAME, result.analysis().methodCallSites().get(1).targetName());
        assertEquals(TARGET_NAME, result.analysis().methodCallSites().get(2).targetName());
        assertEquals(1, result.methodAliases().size());
        assertTrue(result.methodAliases().get(0).endsWith(" -> "
                + READABLE_NAME + DESCRIPTOR));
    }

    @Test
    void targetAliasesRunBeforeSourceVersionSrgNormalization() throws Exception {
        Path methodsCsv = temporaryDirectory.resolve("methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("fields.csv");
        Files.writeString(methodsCsv,
                "searge,name,side,desc\n" + TARGET_NAME + ",SHOULD_NOT_APPLY,0,\n");
        Files.writeString(fieldsCsv, "searge,name,side,desc\nf_12345_,SAMPLE_FIELD,0,\n");

        Forge1201SrgNameMappings mappings = Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);
        Forge1211TargetMethodAliases.Normalization aliases =
                Forge1211TargetMethodAliases.normalize(analysis(
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                OWNER, TARGET_NAME, DESCRIPTOR, 3)),
                        List.of(callSite(OWNER, TARGET_NAME, DESCRIPTOR))));
        Forge1201SrgNameMappings.Normalization normalized = mappings.normalize(aliases.analysis());

        assertEquals(READABLE_NAME, normalized.analysis().methodReferences().get(0).name());
        assertEquals(READABLE_NAME, normalized.analysis().methodCallSites().get(0).targetName());
        assertTrue(normalized.unmappedMethods().isEmpty());
        assertTrue(normalized.methodMappings().isEmpty());
        assertEquals(1, aliases.methodAliases().size());
    }

    @Test
    void aliasedFactoryMethodMatchesTheTargetClassFileStructurally() throws Exception {
        Forge1211TargetMethodAliases.Normalization aliases =
                Forge1211TargetMethodAliases.normalize(analysis(
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                OWNER, TARGET_NAME, DESCRIPTOR, 3)),
                        List.of()));

        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                OWNER, null, "java/lang/Object", null);
        var method = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, READABLE_NAME, DESCRIPTOR, null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 2);
        method.visitEnd();
        writer.visitEnd();

        byte[] targetClass = writer.toByteArray();
        Forge1201TargetApiVerifier.Result verified = Forge1201TargetApiVerifier.verify(
                aliases.analysis(),
                name -> OWNER.equals(name) ? new ByteArrayInputStream(targetClass) : null);

        assertEquals(1, verified.candidateClassCount());
        assertEquals(1, verified.verifiedClassCount());
        assertEquals(1, verified.uniqueMethodReferences());
        assertEquals(1, verified.verifiedMethodReferences());
        assertTrue(verified.methodIssues().isEmpty());
    }

    @Test
    void aliasesOnlyTheExactEntityDimensionsWithEyeHeightSignature() {
        String owner = "net/minecraft/world/entity/EntityDimensions";
        String otherOwner = "net/minecraft/world/entity/OtherDimensions";
        String targetName = "m_320568_";
        String readableName = "withEyeHeight";
        String descriptor = "(F)Lnet/minecraft/world/entity/EntityDimensions;";
        String wrongDescriptor = "(D)Lnet/minecraft/world/entity/EntityDimensions;";
        Forge1201ApiUsageAnalyzer.Analysis source = analysis(
                List.of(
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                owner, targetName, descriptor, 2),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                otherOwner, targetName, descriptor, 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                owner, targetName, wrongDescriptor, 1)),
                List.of(
                        callSite(owner, targetName, descriptor),
                        callSite(otherOwner, targetName, descriptor),
                        callSite(owner, targetName, wrongDescriptor)));

        Forge1211TargetMethodAliases.Normalization result =
                Forge1211TargetMethodAliases.normalize(source);

        assertEquals(readableName, result.analysis().methodReferences().get(0).name());
        assertEquals(targetName, result.analysis().methodReferences().get(1).name());
        assertEquals(targetName, result.analysis().methodReferences().get(2).name());
        assertEquals(readableName, result.analysis().methodCallSites().get(0).targetName());
        assertEquals(targetName, result.analysis().methodCallSites().get(1).targetName());
        assertEquals(targetName, result.analysis().methodCallSites().get(2).targetName());
        assertEquals(1, result.methodAliases().size());
        assertTrue(result.methodAliases().get(0).endsWith(" -> " + readableName + descriptor));
    }

    @Test
    void entityDimensionsTargetAliasRunsBeforeSourceVersionSrgNormalization() throws Exception {
        String owner = "net/minecraft/world/entity/EntityDimensions";
        String descriptor = "(F)Lnet/minecraft/world/entity/EntityDimensions;";
        Path methodsCsv = temporaryDirectory.resolve("dimensions-methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("dimensions-fields.csv");
        Files.writeString(methodsCsv,
                "searge,name,side,desc\n" + "m_320568_,SHOULD_NOT_APPLY,0,\n");
        Files.writeString(fieldsCsv, "searge,name,side,desc\nf_12345_,SAMPLE_FIELD,0,\n");

        Forge1201SrgNameMappings mappings = Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);
        Forge1211TargetMethodAliases.Normalization aliases =
                Forge1211TargetMethodAliases.normalize(analysis(
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                owner, "m_320568_", descriptor, 1)),
                        List.of(callSite(owner, "m_320568_", descriptor))));
        Forge1201SrgNameMappings.Normalization normalized = mappings.normalize(aliases.analysis());

        assertEquals("withEyeHeight", normalized.analysis().methodReferences().get(0).name());
        assertEquals("withEyeHeight", normalized.analysis().methodCallSites().get(0).targetName());
        assertTrue(normalized.unmappedMethods().isEmpty());
        assertTrue(normalized.methodMappings().isEmpty());
        assertEquals(1, aliases.methodAliases().size());
    }

    @Test
    void aliasedWithEyeHeightMethodMatchesTheTargetClassFileStructurally() throws Exception {
        String owner = "net/minecraft/world/entity/EntityDimensions";
        String descriptor = "(F)Lnet/minecraft/world/entity/EntityDimensions;";
        Forge1211TargetMethodAliases.Normalization aliases =
                Forge1211TargetMethodAliases.normalize(analysis(
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                owner, "m_320568_", descriptor, 1)),
                        List.of()));

        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                owner, null, "java/lang/Object", null);
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "withEyeHeight", descriptor, null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 2);
        method.visitEnd();
        writer.visitEnd();

        byte[] targetClass = writer.toByteArray();
        Forge1201TargetApiVerifier.Result verified = Forge1201TargetApiVerifier.verify(
                aliases.analysis(),
                internalName -> owner.equals(internalName) ? new ByteArrayInputStream(targetClass) : null);

        assertEquals(1, verified.candidateClassCount());
        assertEquals(1, verified.verifiedClassCount());
        assertEquals(1, verified.uniqueMethodReferences());
        assertEquals(1, verified.verifiedMethodReferences());
        assertTrue(verified.methodIssues().isEmpty());
    }

    @Test
    void aliasesOnlyTheExactEntityDimensionsRecordAccessorSignatures() {
        String owner = "net/minecraft/world/entity/EntityDimensions";
        String otherOwner = "net/minecraft/world/entity/OtherDimensions";
        Forge1201ApiUsageAnalyzer.Analysis source = analysis(
                List.of(
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20377_", "()F", 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20378_", "()F", 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20379_", "()Z", 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_316573_", "()F", 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(otherOwner, "f_20377_", "()F", 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20377_", "(D)F", 1)),
                List.of(
                        callSite(owner, "f_20377_", "()F"),
                        callSite(owner, "f_20378_", "()F"),
                        callSite(owner, "f_20379_", "()Z"),
                        callSite(owner, "f_316573_", "()F"),
                        callSite(otherOwner, "f_20377_", "()F"),
                        callSite(owner, "f_20377_", "(D)F")));

        Forge1211TargetMethodAliases.Normalization result =
                Forge1211TargetMethodAliases.normalize(source);

        assertEquals(List.of("width", "height", "fixed", "eyeHeight", "f_20377_", "f_20377_"),
                result.analysis().methodReferences().stream()
                        .map(Forge1201ApiUsageAnalyzer.ApiMemberReference::name)
                        .toList());
        assertEquals(List.of("width", "height", "fixed", "eyeHeight", "f_20377_", "f_20377_"),
                result.analysis().methodCallSites().stream()
                        .map(Forge1201ApiUsageAnalyzer.ApiMethodCallSite::targetName)
                        .toList());
        assertEquals(4, result.methodAliases().size());
    }

    @Test
    void entityDimensionsRecordAccessorAliasesSurviveSourceSrgNormalization() throws Exception {
        String owner = "net/minecraft/world/entity/EntityDimensions";
        Path methodsCsv = temporaryDirectory.resolve("dimensions-accessor-methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("dimensions-accessor-fields.csv");
        Files.writeString(methodsCsv,
                "searge,name,side,desc\n"
                        + "m_12345_,UNRELATED_METHOD,0,\n");
        Files.writeString(fieldsCsv, "searge,name,side,desc\nf_12345_,SAMPLE_FIELD,0,\n");

        Forge1201SrgNameMappings mappings = Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);
        Forge1211TargetMethodAliases.Normalization aliases =
                Forge1211TargetMethodAliases.normalize(analysis(
                        List.of(
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20377_", "()F", 1),
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20378_", "()F", 1),
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20379_", "()Z", 1),
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_316573_", "()F", 1)),
                        List.of()));
        Forge1201SrgNameMappings.Normalization normalized = mappings.normalize(aliases.analysis());

        assertEquals(List.of("width", "height", "fixed", "eyeHeight"),
                normalized.analysis().methodReferences().stream()
                        .map(Forge1201ApiUsageAnalyzer.ApiMemberReference::name)
                        .toList());
        assertTrue(normalized.unmappedMethods().isEmpty());
        assertTrue(normalized.methodMappings().isEmpty());
        assertEquals(4, aliases.methodAliases().size());
    }

    @Test
    void aliasedEntityDimensionsRecordAccessorsMatchTargetClassStructurally() throws Exception {
        String owner = "net/minecraft/world/entity/EntityDimensions";
        Forge1211TargetMethodAliases.Normalization aliases =
                Forge1211TargetMethodAliases.normalize(analysis(
                        List.of(
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20377_", "()F", 1),
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20378_", "()F", 1),
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_20379_", "()Z", 1),
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, "f_316573_", "()F", 1)),
                        List.of()));

        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                owner, null, "java/lang/Object", null);
        writeConstantAccessor(writer, "width", "()F", Opcodes.FCONST_0, Opcodes.FRETURN);
        writeConstantAccessor(writer, "height", "()F", Opcodes.FCONST_0, Opcodes.FRETURN);
        writeConstantAccessor(writer, "fixed", "()Z", Opcodes.ICONST_0, Opcodes.IRETURN);
        writeConstantAccessor(writer, "eyeHeight", "()F", Opcodes.FCONST_0, Opcodes.FRETURN);
        writer.visitEnd();

        byte[] targetClass = writer.toByteArray();
        Forge1201TargetApiVerifier.Result verified = Forge1201TargetApiVerifier.verify(
                aliases.analysis(),
                internalName -> owner.equals(internalName) ? new ByteArrayInputStream(targetClass) : null);

        assertEquals(1, verified.candidateClassCount());
        assertEquals(1, verified.verifiedClassCount());
        assertEquals(4, verified.uniqueMethodReferences());
        assertEquals(4, verified.verifiedMethodReferences());
        assertTrue(verified.methodIssues().isEmpty());
    }

    @Test
    void aliasesOnlyTheExactHolderValueOwnerAndDescriptor() {
        String owner = "net/minecraft/core/Holder";
        String otherOwner = "net/minecraft/core/OtherHolder";
        String targetName = "m_203334_";
        String descriptor = "()Ljava/lang/Object;";
        String wrongDescriptor = "()Ljava/lang/String;";
        Forge1201ApiUsageAnalyzer.Analysis source = analysis(
                List.of(
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, targetName, descriptor, 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(otherOwner, targetName, descriptor, 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(owner, targetName, wrongDescriptor, 1)),
                List.of(
                        callSite(owner, targetName, descriptor),
                        callSite(otherOwner, targetName, descriptor),
                        callSite(owner, targetName, wrongDescriptor)));

        Forge1211TargetMethodAliases.Normalization result =
                Forge1211TargetMethodAliases.normalize(source);

        assertEquals(List.of("value", targetName, targetName),
                result.analysis().methodReferences().stream()
                        .map(Forge1201ApiUsageAnalyzer.ApiMemberReference::name)
                        .toList());
        assertEquals(List.of("value", targetName, targetName),
                result.analysis().methodCallSites().stream()
                        .map(Forge1201ApiUsageAnalyzer.ApiMethodCallSite::targetName)
                        .toList());
        assertEquals(1, result.methodAliases().size());
        assertTrue(result.methodAliases().get(0).endsWith(" -> value" + descriptor));
    }

    @Test
    void holderValueTargetAliasRunsBeforeSourceVersionSrgNormalization() throws Exception {
        String owner = "net/minecraft/core/Holder";
        String descriptor = "()Ljava/lang/Object;";
        Path methodsCsv = temporaryDirectory.resolve("holder-methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("holder-fields.csv");
        Files.writeString(methodsCsv,
                "searge,name,side,desc\n" + "m_203334_,SHOULD_NOT_APPLY,0,\n");
        Files.writeString(fieldsCsv, "searge,name,side,desc\nf_12345_,SAMPLE_FIELD,0,\n");

        Forge1201SrgNameMappings mappings = Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);
        Forge1211TargetMethodAliases.Normalization aliases =
                Forge1211TargetMethodAliases.normalize(analysis(
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                owner, "m_203334_", descriptor, 1)),
                        List.of(callSite(owner, "m_203334_", descriptor))));
        Forge1201SrgNameMappings.Normalization normalized = mappings.normalize(aliases.analysis());

        assertEquals("value", normalized.analysis().methodReferences().get(0).name());
        assertEquals("value", normalized.analysis().methodCallSites().get(0).targetName());
        assertTrue(normalized.unmappedMethods().isEmpty());
        assertTrue(normalized.methodMappings().isEmpty());
        assertEquals(1, aliases.methodAliases().size());
    }

    @Test
    void aliasedHolderValueMethodMatchesTargetInterfaceStructurally() throws Exception {
        String owner = "net/minecraft/core/Holder";
        String descriptor = "()Ljava/lang/Object;";
        Forge1211TargetMethodAliases.Normalization aliases =
                Forge1211TargetMethodAliases.normalize(analysis(
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                owner, "m_203334_", descriptor, 1)),
                        List.of()));

        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                owner, null, "java/lang/Object", null);
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "value", descriptor, null, null);
        writer.visitEnd();

        byte[] targetInterface = writer.toByteArray();
        Forge1201TargetApiVerifier.Result verified = Forge1201TargetApiVerifier.verify(
                aliases.analysis(),
                internalName -> owner.equals(internalName)
                        ? new ByteArrayInputStream(targetInterface) : null);

        assertEquals(1, verified.candidateClassCount());
        assertEquals(1, verified.verifiedClassCount());
        assertEquals(1, verified.uniqueMethodReferences());
        assertEquals(1, verified.verifiedMethodReferences());
        assertTrue(verified.methodIssues().isEmpty());
    }

    private static void writeConstantAccessor(
            ClassWriter writer, String name, String descriptor, int constantOpcode, int returnOpcode) {
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
        method.visitCode();
        method.visitInsn(constantOpcode);
        method.visitInsn(returnOpcode);
        method.visitMaxs(1, 1);
        method.visitEnd();
    }

    private static Forge1201ApiUsageAnalyzer.ApiMethodCallSite callSite(
            String owner, String name, String descriptor) {
        return new Forge1201ApiUsageAnalyzer.ApiMethodCallSite(
                "sample/Caller", "create", "()V", owner, name, descriptor, Opcodes.INVOKESTATIC, 1);
    }

    private static Forge1201ApiUsageAnalyzer.Analysis analysis(
            List<Forge1201ApiUsageAnalyzer.ApiMemberReference> methods,
            List<Forge1201ApiUsageAnalyzer.ApiMethodCallSite> callSites) {
        List<String> classes = new ArrayList<>();
        methods.stream().map(Forge1201ApiUsageAnalyzer.ApiMemberReference::owner).forEach(classes::add);
        callSites.stream().map(Forge1201ApiUsageAnalyzer.ApiMethodCallSite::targetOwner).forEach(classes::add);
        return new Forge1201ApiUsageAnalyzer.Analysis(
                null, 1, List.of(), List.of(), null, Map.of(),
                classes.stream().distinct().toList(), callSites, List.of(), methods, List.of());
    }
}
