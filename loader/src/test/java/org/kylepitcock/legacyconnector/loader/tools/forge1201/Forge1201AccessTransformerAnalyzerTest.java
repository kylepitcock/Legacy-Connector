package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201AccessTransformerAnalyzerTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesClassFieldAndMethodDirectivesWithTheirDistinctNamespaceEvidence() {
        String source = """
                # class target
                public net.minecraft.util.Crypt$ByteArrayToKeyFunction

                protected-f net.minecraft.server.MinecraftServer f_129758_ # random
                public net.minecraft.Util m_137477_(Ljava/lang/String;)Ljava/util/concurrent/ExecutorService; # makeExecutor
                """;

        var result = Forge1201AccessTransformerAnalyzer.parse(source, source.length(), "a".repeat(64));

        assertEquals(Forge1201AccessTransformerAnalyzer.ResultStatus.PARSED, result.status());
        assertEquals(3, result.validDirectiveCount());
        assertEquals(0, result.malformedLineCount());
        assertEquals(0, result.wildcardDirectiveCount());

        var classDirective = result.directives().get(0);
        assertEquals(2, classDirective.lineNumber());
        assertEquals(Forge1201AccessTransformerAnalyzer.TargetKind.CLASS, classDirective.targetKind());
        assertEquals("MOJANG_OFFICIAL_1.20.1", classDirective.ownerNamespace());
        assertEquals("NOT_APPLICABLE", classDirective.memberNamespace());

        var field = result.directives().get(1);
        assertEquals(Forge1201AccessTransformerAnalyzer.Visibility.PROTECTED, field.visibility());
        assertEquals(Forge1201AccessTransformerAnalyzer.FinalChange.REMOVE_FINAL, field.finalChange());
        assertEquals("FORGE_SRG_1.20.1", field.memberNamespace());
        assertEquals("f_129758_", field.memberName());
        assertNull(field.descriptor());

        var method = result.directives().get(2);
        assertEquals(Forge1201AccessTransformerAnalyzer.TargetKind.METHOD, method.targetKind());
        assertEquals("m_137477_", method.memberName());
        assertEquals("(Ljava/lang/String;)Ljava/util/concurrent/ExecutorService;", method.descriptor());
        assertEquals("SOURCE_MOJANG_OFFICIAL_1.20.1_WITH_EXTERNAL_BINARY_TYPES", method.descriptorNamespace());
        assertEquals("FORGE_SRG_1.20.1", method.memberNamespace());
        assertTrue(result.sha256().matches("[0-9a-f]{64}"));
    }

    @Test
    void preservesWildcardTargetsAndReportsConflictingModifiersWithoutCollapsingRows() {
        String source = """
                public net.minecraft.world.entity.Entity *
                private net.minecraft.world.entity.Entity *
                public net.minecraft.world.entity.Entity m_12345_()V
                """;

        var result = Forge1201AccessTransformerAnalyzer.parse(source, source.length(), "b".repeat(64));

        assertEquals(3, result.directives().size());
        assertEquals(2, result.wildcardDirectiveCount());
        assertEquals(1, result.validDirectiveCount());
        assertEquals(1, result.duplicateTargetCount());
        assertEquals(1, result.conflictingTargetCount());
        assertEquals(Forge1201AccessTransformerAnalyzer.DirectiveStatus.WILDCARD_REQUIRES_REVIEW,
                result.directives().get(0).status());
        assertNotNull(result.directives().get(0).diagnostic());

        String compatibleChanges = """
                public net.minecraft.world.entity.Entity f_same_
                public-f net.minecraft.world.entity.Entity f_same_
                """;
        var compatible = Forge1201AccessTransformerAnalyzer.parse(
                compatibleChanges, compatibleChanges.length(), "d".repeat(64));
        assertEquals(1, compatible.duplicateTargetCount());
        assertEquals(0, compatible.conflictingTargetCount());
    }

    @Test
    void acceptsJvmConstructorAndClassInitializerMethodNames() {
        String source = """
                public net.minecraft.world.entity.Entity <init>(Lnet/minecraft/world/level/Level;)V
                public net.minecraft.world.entity.Entity <clinit>()V
                """;

        var result = Forge1201AccessTransformerAnalyzer.parse(source, source.length(), "e".repeat(64));

        assertEquals(Forge1201AccessTransformerAnalyzer.ResultStatus.PARSED, result.status());
        assertEquals(2, result.validDirectiveCount());
        assertEquals("<init>", result.directives().get(0).memberName());
        assertEquals("<clinit>", result.directives().get(1).memberName());
    }

    @Test
    void reportsMalformedLinesButRetainsValidDirectivesAndLineLocations() {
        String source = """
                public net.minecraft.world.entity.Entity
                invalid net.minecraft.world.entity.Entity f_123_
                public net.minecraft.world.entity.Entity m_123_(Ljava/lang/String)V
                public bad/name
                """;

        var result = Forge1201AccessTransformerAnalyzer.parse(source, source.length(), "c".repeat(64));

        assertEquals(Forge1201AccessTransformerAnalyzer.ResultStatus.PARTIALLY_PARSED, result.status());
        assertEquals(1, result.validDirectiveCount());
        assertEquals(3, result.malformedLineCount());
        assertEquals(1, result.directives().get(0).lineNumber());
        assertEquals(Forge1201AccessTransformerAnalyzer.TargetKind.MALFORMED,
                result.directives().get(1).targetKind());
        assertTrue(result.warnings().stream().anyMatch(value -> value.contains("Line 3")));
    }

    @Test
    void readsOnlyTheExactProductionResourcePathAndFingerprintsItsBytes() throws Exception {
        Path jar = tempDir.resolve("with-at.jar");
        writeJar(jar, Forge1201AccessTransformerAnalyzer.RESOURCE_PATH,
                "public net.minecraft.world.entity.Entity\n".getBytes(StandardCharsets.UTF_8));

        var result = Forge1201AccessTransformerAnalyzer.analyze(jar);

        assertEquals(Forge1201AccessTransformerAnalyzer.ResultStatus.PARSED, result.status());
        assertEquals(1, result.validDirectiveCount());
        assertEquals(Forge1201AccessTransformerAnalyzer.RESOURCE_PATH, result.resourcePath());
        assertTrue(Files.size(jar) > 0);

        Path misplaced = tempDir.resolve("misplaced-at.jar");
        writeJar(misplaced, "config/accesstransformer.cfg",
                "public net.minecraft.world.entity.Entity\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(Forge1201AccessTransformerAnalyzer.ResultStatus.NOT_PRESENT,
                Forge1201AccessTransformerAnalyzer.analyze(misplaced).status());
    }

    @Test
    void reportsInvalidUtf8AndOversizedResourcesWithoutPartialParsing() throws Exception {
        Path invalid = tempDir.resolve("invalid-utf8.jar");
        writeJar(invalid, Forge1201AccessTransformerAnalyzer.RESOURCE_PATH,
                new byte[] {(byte) 0xC3, (byte) 0x28});
        assertEquals(Forge1201AccessTransformerAnalyzer.ResultStatus.INVALID_UTF8,
                Forge1201AccessTransformerAnalyzer.analyze(invalid).status());

        Path large = tempDir.resolve("oversized.jar");
        byte[] bytes = new byte[4 * 1024 * 1024 + 1];
        writeJar(large, Forge1201AccessTransformerAnalyzer.RESOURCE_PATH, bytes);
        assertEquals(Forge1201AccessTransformerAnalyzer.ResultStatus.OVERSIZED,
                Forge1201AccessTransformerAnalyzer.analyze(large).status());
    }

    private static void writeJar(Path path, String entryName, byte[] bytes) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(bytes);
            zip.closeEntry();
        }
    }
}
