package org.kylepitcock.legacyconnector.loader.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201SrgObfuscatedBridgeAuditTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesClassAndOwnerQualifiedMethodsWithFullDescriptorConversion() throws Exception {
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                \tn (J)V m_10_ 3
                \tp (Lb;[[Lc;)V m_11_ 4
                \tq (Ljava/lang/String;)V m_12_ 5
                \tx f_20_ 6
                b net/minecraft/Component 7
                c net/minecraft/Component$Inner 8
                z net/minecraft/Other 9
                \tr (I)V m_10_ 10
                """);
        Path tiny = write("source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                CLASS\tc\tnet/minecraft/class_3
                CLASS\tz\tnet/minecraft/class_4
                FIELD\ta\tLb;\tx\tfield_20
                """);

        var index = Forge1201SrgObfuscatedBridgeAudit.read(tsrg, tiny);

        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT,
                index.resolveClass("net/minecraft/Entity").status());
        assertEquals("a", index.resolveClass("net/minecraft/Entity").obfuscatedNames().getFirst());

        var intOverload = index.resolveMethod("net/minecraft/Entity", "m_10_", "(I)V");
        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT, intOverload.status());
        assertEquals("m", intOverload.candidates().getFirst().obfuscatedName());

        var longOverload = index.resolveMethod("net/minecraft/Entity", "m_10_", "(J)V");
        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT, longOverload.status());
        assertEquals("n", longOverload.candidates().getFirst().obfuscatedName());

        var nestedArray = index.resolveMethod(
                "net/minecraft/Entity", "m_11_",
                "(Lnet/minecraft/Component;[[Lnet/minecraft/Component$Inner;)V");
        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT, nestedArray.status());
        assertEquals("p", nestedArray.candidates().getFirst().obfuscatedName());
        assertEquals("(Lb;[[Lc;)V", nestedArray.candidates().getFirst().obfuscatedDescriptor());

        var reusedSrgToken = index.resolveMethod("net/minecraft/Other", "m_10_", "(I)V");
        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT, reusedSrgToken.status());
        assertEquals("r", reusedSrgToken.candidates().getFirst().obfuscatedName());

        var external = index.resolveMethod(
                "net/minecraft/Entity", "m_12_", "(Ljava/lang/String;)V");
        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_WITH_EXTERNAL_TYPES, external.status());
        assertEquals("q", external.candidates().getFirst().obfuscatedName());

        var field = index.resolveField("net/minecraft/Entity", "f_20_");
        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT, field.status());
        assertEquals("x", field.candidates().getFirst().obfuscatedName());
        assertEquals("Lb;", field.candidates().getFirst().obfuscatedDescriptor());
        assertEquals("Lnet/minecraft/Component;", field.candidates().getFirst().sourceDescriptor());

        assertEquals(1, index.audit().methods().overloadedOwnerNameGroups());
    }

    @Test
    void doesNotTreatMissingMinecraftDescriptorTypesAsExternal() throws Exception {
        Path tsrg = write("joined-missing-type.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                """);
        Path tiny = write("source-missing-type.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                """);

        var index = Forge1201SrgObfuscatedBridgeAudit.read(tsrg, tiny);
        var result = index.resolveMethod(
                "net/minecraft/Entity", "m_10_", "(Lnet/minecraft/MissingType;)V");

        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.DESCRIPTOR_UNRESOLVED, result.status());
    }

    @Test
    void refusesAmbiguousClassAliasesAndAmbiguousFieldDescriptors() throws Exception {
        Path tsrg = write("joined-ambiguous.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Duplicate 1
                b net/minecraft/Duplicate 2
                x net/minecraft/Entity 3
                \tf f_1_ 4
                """);
        Path tiny = write("source-ambiguous.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                CLASS\tx\tnet/minecraft/class_3
                FIELD\tx\tI\tf\tfield_1
                FIELD\tx\tJ\tf\tfield_2
                """);
        var index = Forge1201SrgObfuscatedBridgeAudit.read(tsrg, tiny);

        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.AMBIGUOUS,
                index.resolveClass("net/minecraft/Duplicate").status());
        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.AMBIGUOUS,
                index.resolveField("net/minecraft/Entity", "f_1_").status());
        assertEquals(1, index.audit().fields().ambiguousTinyFieldJoins());
        assertTrue(index.audit().classes().ambiguousSrgClassNames() > 0);
    }

    @Test
    void reportsMissingTinyFieldDescriptorInsteadOfGuessing() throws Exception {
        Path tsrg = write("joined-no-tiny-field.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tf f_1_ 2
                """);
        Path tiny = write("source-no-tiny-field.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                """);

        var index = Forge1201SrgObfuscatedBridgeAudit.read(tsrg, tiny);

        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.MISSING,
                index.resolveField("net/minecraft/Entity", "f_1_").status());
        assertEquals(1, index.audit().fields().missingTinyFieldJoins());
    }

    @Test
    void detectsConflictingFullMethodSelectorMappings() throws Exception {
        Path tsrg = write("joined-method-collision.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                \tn (I)V m_10_ 3
                """);
        Path tiny = write("source-method-collision.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                """);

        var index = Forge1201SrgObfuscatedBridgeAudit.read(tsrg, tiny);
        var result = index.resolveMethod("net/minecraft/Entity", "m_10_", "(I)V");

        assertEquals(Forge1201SrgObfuscatedBridgeAudit.Status.AMBIGUOUS, result.status());
        assertEquals(2, result.candidates().size());
        assertEquals(1, index.audit().methods().duplicateFullSelectorKeys());
    }

    private Path write(String name, String contents) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}
