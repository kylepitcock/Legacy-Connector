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
