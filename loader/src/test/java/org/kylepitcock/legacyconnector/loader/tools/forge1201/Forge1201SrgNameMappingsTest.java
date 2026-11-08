package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201SrgNameMappingsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void normalizesSourceSrgMethodsAndFieldsBeforeTargetMemberLookup() throws Exception {
        Path methodsCsv = temporaryDirectory.resolve("methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("fields.csv");
        Files.writeString(methodsCsv,
                "searge,name,side,desc\n"
                        + "m_130070_,writeUtf,0,Writes UTF text\n"
                        + "m_234567_,readSomething,0,Reads UTF text\n");
        Files.writeString(fieldsCsv,
                "searge,name,side,desc\n"
                        + "f_345678_,FIELD_SAMPLE,0,Maximum encoded string length\n");

        Forge1201SrgNameMappings mappings =
                Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);

        Forge1201ApiUsageAnalyzer.Analysis source = new Forge1201ApiUsageAnalyzer.Analysis(
                null,
                1,
                List.of(),
                List.of(),
                null,
                Map.of(),
                List.of("net/minecraft/network/FriendlyByteBuf"),
                List.of(new Forge1201ApiUsageAnalyzer.ApiMethodCallSite(
                        "sample/NetClient", "writeSample", "()V",
                        "net/minecraft/network/FriendlyByteBuf", "m_130070_",
                        "(Ljava/lang/String;)Lnet/minecraft/network/FriendlyByteBuf;",
                        org.objectweb.asm.Opcodes.INVOKEVIRTUAL, 2)),
                List.of(new Forge1201ApiUsageAnalyzer.ApiFieldCallSite(
                        "sample/NetClient", "writeSample", "()V",
                        "net/minecraft/network/FriendlyByteBuf", "f_345678_", "I",
                        org.objectweb.asm.Opcodes.GETSTATIC, 1)),
                List.of(
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraft/network/FriendlyByteBuf",
                                "m_130070_",
                                "(Ljava/lang/String;)Lnet/minecraft/network/FriendlyByteBuf;",
                                2),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraft/network/FriendlyByteBuf",
                                "m_234567_",
                                "()Ljava/lang/String;",
                                1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraft/network/FriendlyByteBuf",
                                "m_999999_",
                                "()V",
                                1)),
                List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                        "net/minecraft/network/FriendlyByteBuf",
                        "f_345678_",
                        "I",
                        1)));

        Forge1201SrgNameMappings.Normalization result = mappings.normalize(source);

        assertEquals("writeUtf", result.analysis().methodReferences().get(0).name());
        assertEquals("readSomething", result.analysis().methodReferences().get(1).name());
        assertEquals("m_999999_", result.analysis().methodReferences().get(2).name());
        assertEquals("FIELD_SAMPLE", result.analysis().fieldReferences().get(0).name());
        assertEquals("writeUtf", result.analysis().methodCallSites().get(0).targetName());
        assertEquals("FIELD_SAMPLE", result.analysis().fieldCallSites().get(0).targetName());

        assertEquals(2, result.methodMappings().size());
        assertEquals(1, result.fieldMappings().size());
        assertEquals(1, result.unmappedMethods().size());
        assertTrue(result.unmappedMethods().get(0).contains("m_999999_"));
        assertTrue(result.unmappedFields().isEmpty());

        assertTrue(result.methodMappings().stream().anyMatch(value -> value.contains("m_130070_")
                && value.contains("writeUtf")));
        assertTrue(result.fieldMappings().get(0).contains("f_345678_"));
    }

    @Test
    void leavesAlreadyReadableNamesAndNonMinecraftOwnersUntouched() throws Exception {
        Path methodsCsv = temporaryDirectory.resolve("methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("fields.csv");
        Files.writeString(methodsCsv, "searge,name,side,desc\nm_130070_,writeUtf,0,\n");
        Files.writeString(fieldsCsv, "searge,name,side,desc\nf_456789_,SAMPLE_FIELD,0,\n");

        Forge1201SrgNameMappings mappings =
                Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);

        Forge1201ApiUsageAnalyzer.Analysis source = new Forge1201ApiUsageAnalyzer.Analysis(
                null, 1, List.of(), List.of(), null, Map.of(), List.of(), List.of(), List.of(),
                List.of(
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraft/network/FriendlyByteBuf", "writeUtf", "()V", 1),
                        new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                "net/minecraftforge/eventbus/api/IEventBus", "m_130070_", "()V", 1)),
                List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                        "net/minecraftforge/eventbus/api/IEventBus", "f_456789_", "I", 1)));

        Forge1201SrgNameMappings.Normalization result = mappings.normalize(source);

        assertEquals("writeUtf", result.analysis().methodReferences().get(0).name());
        assertEquals("m_130070_", result.analysis().methodReferences().get(1).name());
        assertEquals("f_456789_", result.analysis().fieldReferences().get(0).name());
        assertTrue(result.methodMappings().isEmpty());
        assertTrue(result.fieldMappings().isEmpty());
        assertTrue(result.unmappedMethods().isEmpty());
        assertTrue(result.unmappedFields().isEmpty());
    }

    @Test
    void normalizesARealSourceNameAndChecksVanillaMinecraftOwner() throws Exception {
        Path methodsCsv = temporaryDirectory.resolve("methods-real.csv");
        Path fieldsCsv = temporaryDirectory.resolve("fields-real.csv");
        Files.writeString(methodsCsv,
                "searge,name,side,desc\n"
                        + "m_130070_,writeUtf,0,Writes UTF text\n");
        Files.writeString(fieldsCsv,
                "searge,name,side,desc\n"
                        + "f_345678_,FIELD_SAMPLE,0,Fixture field\n");

        Forge1201SrgNameMappings mappings =
                Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv);
        String owner = "net/minecraft/network/FriendlyByteBuf";
        String descriptor = "(Ljava/lang/String;)Lnet/minecraft/network/FriendlyByteBuf;";
        Forge1201ApiUsageAnalyzer.Analysis source = new Forge1201ApiUsageAnalyzer.Analysis(
                null,
                1,
                List.of(),
                List.of(),
                null,
                Map.of(),
                List.of(owner),
                List.of(),
                List.of(),
                List.of(new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                        owner, "m_130070_", descriptor, 1)),
                List.of());

        Forge1201SrgNameMappings.Normalization normalized = mappings.normalize(source);
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        writer.visit(org.objectweb.asm.Opcodes.V17, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                owner, null, "java/lang/Object", null);
        writer.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "writeUtf", descriptor, null, null).visitEnd();
        writer.visitEnd();
        byte[] targetClass = writer.toByteArray();

        Forge1201TargetApiVerifier.Result verified = Forge1201TargetApiVerifier.verify(
                normalized.analysis(),
                internalName -> owner.equals(internalName)
                        ? new java.io.ByteArrayInputStream(targetClass)
                        : null);

        assertEquals(1, normalized.methodMappings().size());
        assertEquals(0, normalized.unmappedMethods().size());
        assertEquals(1, verified.candidateClassCount());
        assertEquals(1, verified.verifiedClassCount());
        assertEquals(1, verified.uniqueMethodReferences());
        assertEquals(1, verified.verifiedMethodReferences());
        assertTrue(verified.methodIssues().isEmpty());
    }

    @Test
    void rejectsConflictingMappingsInsteadOfChoosingOneSilently() throws Exception {
        Path methodsCsv = temporaryDirectory.resolve("methods.csv");
        Path fieldsCsv = temporaryDirectory.resolve("fields.csv");
        Files.writeString(methodsCsv,
                "searge,name,side,desc\nm_12345_,firstName,0,\nm_12345_,secondName,0,\n");
        Files.writeString(fieldsCsv, "searge,name,side,desc\nf_12345_,fieldName,0,\n");

        org.junit.jupiter.api.Assertions.assertThrows(
                java.io.IOException.class,
                () -> Forge1201SrgNameMappings.load(methodsCsv, fieldsCsv));
    }
}
