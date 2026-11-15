package org.kylepitcock.legacyconnector.loader.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201SrgIntermediaryBridgeResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesClassMethodAndFieldThroughBothSourceEdges() throws Exception {
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                \tx f_20_ 3
                b net/minecraft/Component 4
                """);
        Path sourceTiny = write("source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                METHOD\ta\t(I)V\tm\tmethod_1
                FIELD\ta\tLb;\tx\tfield_1
                """);
        Path targetTiny = write("target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tc\tnet/minecraft/class_1
                CLASS\td\tnet/minecraft/class_2
                METHOD\tc\t(I)V\tz\tmethod_1
                FIELD\tc\tLd;\ty\tfield_1
                """);

        var index = Forge1201SrgIntermediaryBridgeResolver.readIndex(tsrg, sourceTiny, targetTiny);
        var classResult = index.resolveClass("net/minecraft/Entity");
        assertEquals(Forge1201SrgIntermediaryBridgeResolver.ClassStatus.MAPPED_EXACT, classResult.status());
        assertEquals("a", classResult.sourceOfficialObfuscatedClass());
        assertEquals("c", classResult.targetOfficialObfuscatedClass());

        var method = index.resolveMethod("net/minecraft/Entity", "m_10_", "(I)V");
        assertEquals(Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.MAPPED_EXACT, method.status());
        assertEquals("m", method.candidates().getFirst().sourceOfficialObfuscatedName());
        assertEquals("z", method.candidates().getFirst().targetOfficialObfuscatedName());
        assertEquals("c", method.candidates().getFirst().targetOfficialObfuscatedOwner());

        var field = index.resolveField("net/minecraft/Entity", "f_20_");
        assertEquals(Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.MAPPED_EXACT, field.status());
        assertEquals("x", field.candidates().getFirst().sourceOfficialObfuscatedName());
        assertEquals("y", field.candidates().getFirst().targetOfficialObfuscatedName());
        assertEquals("Ld;", field.candidates().getFirst().targetOfficialObfuscatedDescriptor());
    }

    @Test
    void keepsExternalTypesExplicitlyNonExact() throws Exception {
        Path tsrg = write("external.tsrg", """
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
                CLASS\tb\tnet/minecraft/class_1
                METHOD\tb\t(Ljava/lang/String;)V\tz\tmethod_1
                """);

        var index = Forge1201SrgIntermediaryBridgeResolver.readIndex(tsrg, sourceTiny, targetTiny);
        var result = index.resolveMethod("net/minecraft/Entity", "m_10_", "(Ljava/lang/String;)V");
        assertEquals(Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.MAPPED_WITH_EXTERNAL_TYPES, result.status());
        assertEquals(2, result.candidates().getFirst().externalDescriptorTypeCount());
    }

    @Test
    void reportsDescriptorChangeAndMovedOwnerWithoutTreatingThemAsExact() throws Exception {
        Path tsrg = write("changed.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                b net/minecraft/Other 3
                """);
        Path sourceTiny = write("changed-source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                METHOD\ta\t(I)V\tm\tmethod_1
                """);
        Path targetTiny = write("changed-target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tc\tnet/minecraft/class_1
                CLASS\td\tnet/minecraft/class_2
                METHOD\tc\t(J)V\tz\tmethod_1
                """);
        var changed = Forge1201SrgIntermediaryBridgeResolver.readIndex(tsrg, sourceTiny, targetTiny)
                .resolveMethod("net/minecraft/Entity", "m_10_", "(I)V");
        assertEquals(Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.INTERMEDIARY_TARGET_DESCRIPTOR_CHANGED,
                changed.status());

        Path movedTargetTiny = write("moved-target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tc\tnet/minecraft/class_1
                CLASS\td\tnet/minecraft/class_2
                METHOD\td\t(I)V\tz\tmethod_1
                """);
        var moved = Forge1201SrgIntermediaryBridgeResolver.readIndex(tsrg, sourceTiny, movedTargetTiny)
                .resolveMethod("net/minecraft/Entity", "m_10_", "(I)V");
        assertEquals(Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.INTERMEDIARY_TARGET_OWNER_CHANGED,
                moved.status());
    }

    @Test
    void distinguishesMissingTargetMemberId() throws Exception {
        Path tsrg = write("missing.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                """);
        Path sourceTiny = write("missing-source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                METHOD\ta\t(I)V\tm\tmethod_1
                """);
        Path targetTiny = write("missing-target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tb\tnet/minecraft/class_1
                """);
        var result = Forge1201SrgIntermediaryBridgeResolver.readIndex(tsrg, sourceTiny, targetTiny)
                .resolveMethod("net/minecraft/Entity", "m_10_", "(I)V");
        assertEquals(Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.INTERMEDIARY_TARGET_ID_MISSING,
                result.status());
    }

    private Path write(String name, String contents) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}
