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

final class Forge1211TargetFieldAliasesTest {
    private static final String ENCHANTMENTS =
            "net/minecraft/world/item/enchantment/Enchantments";
    private static final String ATTRIBUTES =
            "net/minecraft/world/entity/ai/attributes/Attributes";
    private static final String OTHER_OWNER =
            "net/minecraft/world/item/enchantment/OtherEnchantments";
    private static final String RESOURCE_KEY = "Lnet/minecraft/resources/ResourceKey;";
    private static final String HOLDER = "Lnet/minecraft/core/Holder;";

    @TempDir
    Path temporaryDirectory;

    @Test
    void aliasesOnlyTheExactTargetOwnerNameAndDescriptor() {
        Forge1201ApiUsageAnalyzer.Analysis source = analysis(
                List.of(
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                ENCHANTMENTS, "f_316758_", RESOURCE_KEY, 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                ENCHANTMENTS, "f_316758_", "I", 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                OTHER_OWNER, "f_316758_", RESOURCE_KEY, 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                ATTRIBUTES, "f_316806_", HOLDER, 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                ATTRIBUTES, "f_316806_",
                                "Lnet/minecraft/world/entity/ai/attributes/Attribute;", 1)),
                List.of(
                        fieldCallSite(ENCHANTMENTS, "f_316758_", RESOURCE_KEY),
                        fieldCallSite(ENCHANTMENTS, "f_316758_", "I"),
                        fieldCallSite(OTHER_OWNER, "f_316758_", RESOURCE_KEY),
                        fieldCallSite(ATTRIBUTES, "f_316806_", HOLDER),
                        fieldCallSite(ATTRIBUTES, "f_316806_",
                                "Lnet/minecraft/world/entity/ai/attributes/Attribute;")));

        Forge1211TargetFieldAliases.Normalization result =
                Forge1211TargetFieldAliases.normalize(source);

        assertEquals("EFFICIENCY", result.analysis().fieldReferences().get(0).name());
        assertEquals("f_316758_", result.analysis().fieldReferences().get(1).name());
        assertEquals("f_316758_", result.analysis().fieldReferences().get(2).name());
        assertEquals("STEP_HEIGHT", result.analysis().fieldReferences().get(3).name());
        assertEquals("f_316806_", result.analysis().fieldReferences().get(4).name());

        assertEquals("EFFICIENCY", result.analysis().fieldCallSites().get(0).targetName());
        assertEquals("f_316758_", result.analysis().fieldCallSites().get(1).targetName());
        assertEquals("f_316758_", result.analysis().fieldCallSites().get(2).targetName());
        assertEquals("STEP_HEIGHT", result.analysis().fieldCallSites().get(3).targetName());
        assertEquals("f_316806_", result.analysis().fieldCallSites().get(4).targetName());

        assertEquals(2, result.fieldAliases().size());
        assertTrue(result.fieldAliases().stream().anyMatch(value ->
                value.contains("Enchantments#f_316758_") && value.endsWith(" -> EFFICIENCY")));
        assertTrue(result.fieldAliases().stream().anyMatch(value ->
                value.contains("Attributes#f_316806_") && value.endsWith(" -> STEP_HEIGHT")));
    }

    @Test
    void targetAliasesRunBeforeSourceVersionSrgNormalization() throws Exception {
        Path methodsCsv = temporaryDirectory.resolve("methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("fields.csv");
        Files.writeString(methodsCsv, "searge,name,side,desc\nm_12345_,sampleMethod,0,\n");
        Files.writeString(fieldsCsv, "searge,name,side,desc\nf_12345_,SAMPLE_FIELD,0,\n");

        Forge1201SrgNameMappings sourceMappings =
                Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);
        Forge1211TargetFieldAliases.Normalization targetAliases =
                Forge1211TargetFieldAliases.normalize(analysis(
                        List.of(
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                        ENCHANTMENTS, "f_316758_", RESOURCE_KEY, 1),
                                new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                        ATTRIBUTES, "f_316806_", HOLDER, 1)),
                        List.of()));

        Forge1201SrgNameMappings.Normalization sourceNormalization =
                sourceMappings.normalize(targetAliases.analysis());

        assertEquals("EFFICIENCY", sourceNormalization.analysis().fieldReferences().get(0).name());
        assertEquals("STEP_HEIGHT", sourceNormalization.analysis().fieldReferences().get(1).name());
        assertTrue(sourceNormalization.unmappedFields().isEmpty());
        assertEquals(2, targetAliases.fieldAliases().size());
    }

    @Test
    void aliasedStepHeightFieldMatchesTheTargetClassFileStructurally() throws Exception {
        Forge1211TargetFieldAliases.Normalization aliases =
                Forge1211TargetFieldAliases.normalize(analysis(
                        List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                ATTRIBUTES, "f_316806_", HOLDER, 1)),
                        List.of()));

        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, ATTRIBUTES, null, "java/lang/Object", null);
        writer.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "STEP_HEIGHT",
                HOLDER,
                null,
                null).visitEnd();
        writer.visitEnd();
        byte[] targetClass = writer.toByteArray();

        Forge1201TargetApiVerifier.Result verified = Forge1201TargetApiVerifier.verify(
                aliases.analysis(),
                internalName -> ATTRIBUTES.equals(internalName)
                        ? new ByteArrayInputStream(targetClass)
                        : null);

        assertEquals(1, verified.candidateClassCount());
        assertEquals(1, verified.verifiedClassCount());
        assertEquals(1, verified.uniqueFieldReferences());
        assertEquals(1, verified.verifiedFieldReferences());
        assertTrue(verified.fieldIssues().isEmpty());
    }

    private static Forge1201ApiUsageAnalyzer.ApiFieldCallSite fieldCallSite(
            String owner, String name, String descriptor) {
        return new Forge1201ApiUsageAnalyzer.ApiFieldCallSite(
                "sample/Caller", "read", "()V",
                owner, name, descriptor, Opcodes.GETSTATIC, 1);
    }

    private static Forge1201ApiUsageAnalyzer.Analysis analysis(
            List<Forge1201ApiUsageAnalyzer.ApiMemberReference> fields,
            List<Forge1201ApiUsageAnalyzer.ApiFieldCallSite> fieldCallSites) {
        List<String> classes = new ArrayList<>();
        fields.stream().map(Forge1201ApiUsageAnalyzer.ApiMemberReference::owner).forEach(classes::add);
        fieldCallSites.stream().map(Forge1201ApiUsageAnalyzer.ApiFieldCallSite::targetOwner).forEach(classes::add);
        return new Forge1201ApiUsageAnalyzer.Analysis(
                null,
                1,
                List.of(),
                List.of(),
                null,
                Map.of(),
                classes.stream().distinct().toList(),
                List.of(),
                fieldCallSites,
                List.of(),
                fields);
    }
}
