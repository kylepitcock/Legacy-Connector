package org.kylepitcock.legacyconnector.loader.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201IntermediaryBridgeAuditTest {
    @TempDir
    Path tempDir;

    @Test
    void joinsMembersByOwnerAndFullDescriptorAndFindsStableTargetSymbols() throws Exception {
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/C_1_ 1
                \tb (La;)V m_1_ 2
                \tc f_2_ 3
                """);
        Path source = write("source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                METHOD\ta\t(La;)V\tb\tmethod_1
                FIELD\ta\tLa;\tc\tfield_2
                """);
        Path target = write("target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tz\tnet/minecraft/class_1
                METHOD\tz\t(Lz;)V\ty\tmethod_1
                FIELD\tz\tLz;\tx\tfield_2
                """);

        var result = Forge1201IntermediaryBridgeAudit.analyze(tsrg, source, target);

        assertEquals(1, result.classes().joinedRows());
        assertEquals(1, result.classes().sourceIntermediaryMappedRows());
        assertEquals(1, result.classes().targetSharedClassIds());
        assertEquals(1, result.methods().sourceIntermediaryMappedRows());
        assertEquals(1, result.methods().targetExactOwnerMemberDescriptorRows());
        assertEquals(0, result.methods().targetSameOwnerMemberDescriptorChangedRows());
        assertEquals(1, result.fields().sourceIntermediaryMappedRows());
        assertEquals(1, result.fields().targetExactOwnerMemberDescriptorRows());
    }

    @Test
    void separatesDescriptorChangesFromOwnerChanges() throws Exception {
        Path tsrg = write("joined-changes.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/C_1_ 1
                \tb (La;)V m_1_ 2
                \tc (I)V m_2_ 3
                """);
        Path source = write("source-changes.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tx\tnet/minecraft/class_2
                METHOD\ta\t(La;)V\tb\tmethod_1
                METHOD\ta\t(I)V\tc\tmethod_2
                """);
        Path target = write("target-changes.tiny", """
                v1\tofficial\tintermediary
                CLASS\tz\tnet/minecraft/class_1
                CLASS\ty\tnet/minecraft/class_2
                METHOD\tz\t(I)V\tp\tmethod_1
                METHOD\ty\t(I)V\tq\tmethod_2
                """);

        var methods = Forge1201IntermediaryBridgeAudit.analyze(tsrg, source, target).methods();

        assertEquals(1, methods.targetSameOwnerMemberDescriptorChangedRows());
        assertEquals(1, methods.targetOwnerChangedRows());
        assertEquals(0, methods.targetExactOwnerMemberDescriptorRows());
    }

    @Test
    void doesNotTreatAnSrgNameAsAGlobalSemanticSymbolId() throws Exception {
        Path tsrg = write("joined-reused-srg.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/C_1_ 1
                \tb (I)V m_7_ 2
                x net/minecraft/C_2_ 3
                \tc (J)V m_7_ 4
                """);
        Path source = write("source-reused-srg.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tx\tnet/minecraft/class_2
                METHOD\ta\t(I)V\tb\tmethod_1
                METHOD\tx\t(J)V\tc\tmethod_2
                """);
        Path target = write("target-reused-srg.tiny", """
                v1\tofficial\tintermediary
                CLASS\tz\tnet/minecraft/class_1
                CLASS\ty\tnet/minecraft/class_2
                METHOD\tz\t(I)V\tp\tmethod_1
                METHOD\ty\t(J)V\tq\tmethod_2
                """);

        var methods = Forge1201IntermediaryBridgeAudit.analyze(tsrg, source, target).methods();

        assertEquals(2, methods.joinedRows());
        assertEquals(1, methods.distinctSourceSrgIds());
        assertEquals(1, methods.sourceSrgIdsWithMultipleIntermediarySymbols());
        assertEquals(2, methods.targetExactOwnerMemberDescriptorRows());
    }

    @Test
    void reportsUnmappedMethodsAndAmbiguousDescriptorlessFieldJoins() throws Exception {
        Path tsrg = write("joined-ambiguous.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/C_1_ 1
                \tmissing (I)V m_1_ 2
                \tb f_2_ 3
                """);
        Path source = write("source-ambiguous.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                FIELD\ta\tI\tb\tfield_1
                FIELD\ta\tJ\tb\tfield_2
                """);
        Path target = write("target-ambiguous.tiny", """
                v1\tofficial\tintermediary
                CLASS\tz\tnet/minecraft/class_1
                FIELD\tz\tI\tx\tfield_1
                FIELD\tz\tJ\tx\tfield_2
                """);

        var result = Forge1201IntermediaryBridgeAudit.analyze(tsrg, source, target);

        assertEquals(1, result.methods().sourceUnmappedRows());
        assertEquals(0, result.methods().sourceIntermediaryMappedRows());
        assertEquals(1, result.fields().ambiguousSourceRows());
        assertEquals(0, result.fields().sourceIntermediaryMappedRows());
    }

    @Test
    void preservesPackageQualifiedExternalDescriptorTypesAsNonExactCandidates() throws Exception {
        Path tsrg = write("joined-external.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/C_1_ 1
                \tb (Ljava/lang/String;Ljava/util/List;)V m_1_ 2
                """);
        Path source = write("source-external.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                METHOD\ta\t(Ljava/lang/String;Ljava/util/List;)V\tb\tmethod_1
                """);
        Path target = write("target-external.tiny", """
                v1\tofficial\tintermediary
                CLASS\tz\tnet/minecraft/class_1
                METHOD\tz\t(Ljava/lang/String;Ljava/util/List;)V\tp\tmethod_1
                """);

        var methods = Forge1201IntermediaryBridgeAudit.analyze(tsrg, source, target).methods();

        assertEquals(1, methods.sourceIntermediaryMappedRows());
        assertEquals(0, methods.sourceDescriptorUnresolvedRows());
        assertEquals(1, methods.sourceDescriptorExternalTypeRows());
        assertEquals(2, methods.sourceDescriptorExternalTypeOccurrences());
        assertEquals(1, methods.targetDescriptorExternalTypeRows());
        assertEquals(4, methods.targetDescriptorExternalTypeOccurrences());
        assertEquals(1, methods.targetSameOwnerMemberDescriptorExternalPassthroughRows());
        assertEquals(0, methods.targetExactOwnerMemberDescriptorRows());
        assertEquals(0, methods.targetSameOwnerMemberDescriptorUnresolvedRows());
    }

    @Test
    void reportsKnownMinecraftDescriptorTypesMissingFromIntermediaryAsUnresolved() throws Exception {
        Path tsrg = write("joined-unresolved.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/C_1_ 1
                x net/minecraft/C_2_ 2
                \tb (Lx;)V m_1_ 3
                """);
        Path source = write("source-unresolved.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                METHOD\ta\t(Lx;)V\tb\tmethod_1
                """);
        Path target = write("target-unresolved.tiny", """
                v1\tofficial\tintermediary
                CLASS\tz\tnet/minecraft/class_1
                METHOD\tz\t(Ly;)V\tp\tmethod_1
                """);

        var methods = Forge1201IntermediaryBridgeAudit.analyze(tsrg, source, target).methods();

        assertEquals(1, methods.sourceDescriptorUnresolvedRows());
        assertEquals(0, methods.sourceDescriptorExternalTypeRows());
        assertEquals(1, methods.targetSameOwnerMemberDescriptorUnresolvedRows());
        assertEquals(0, methods.targetExactOwnerMemberDescriptorRows());
    }

    @Test
    void writesValidBuildLocalJsonAndExplicitlyDisclaimsSelectorTransformation() throws Exception {
        Path tsrg = write("joined-report.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/C_1_ 1
                \tb ()V m_1_ 2
                """);
        Path source = write("source-report.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                METHOD\ta\t()V\tb\tmethod_1
                """);
        Path target = write("target-report.tiny", """
                v1\tofficial\tintermediary
                CLASS\tz\tnet/minecraft/class_1
                METHOD\tz\t()V\tp\tmethod_1
                """);
        Path report = tempDir.resolve("report.json");

        Forge1201IntermediaryBridgeAudit.main(new String[] {
                tsrg.toString(), source.toString(), target.toString(), report.toString()
        });

        JsonObject json = JsonParser.parseString(Files.readString(report, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("PARTIAL", json.get("status").getAsString());
        assertEquals("minecraft:official-obfuscated@1.20.1", json.get("sourceNamespace").getAsString());
        assertEquals("minecraft:forge-srg@1.20.1", json.get("sourceSrgMetadataNamespace").getAsString());
        assertEquals("minecraft:official-obfuscated@1.21.1", json.get("targetNamespace").getAsString());
        assertFalse(json.get("completeForgeSrgSelectorPath").getAsBoolean());
        assertFalse(json.get("selectorTransformationPerformed").getAsBoolean());
        assertFalse(json.get("runtimeMappingTableProduced").getAsBoolean());
        assertTrue(json.getAsJsonObject("coverage").getAsJsonObject("methods")
                .get("targetExactOwnerMemberDescriptorRows").getAsInt() == 1);
    }

    private Path write(String name, String contents) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}
