package org.kylepitcock.legacyconnector.loader.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201SourceOfficialMappingIndexTest {
    @TempDir
    Path tempDir;

    @Test
    void composesOwnerQualifiedSrgObfuscatedAndMojangMappingsForMethodsAndFields() throws Exception {
        Path tsrg = write("joined.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (Lb;)V m_10_ 2
                \tn (J)V m_10_ 3
                \tx f_20_ 4
                b net/minecraft/Component 5
                z net/minecraft/Other 6
                """);
        Path tiny = write("source.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                CLASS\tz\tnet/minecraft/class_3
                FIELD\ta\tLb;\tx\tfield_20
                """);
        Path mojmap = write("client.txt", """
                net.minecraft.Entity -> a:
                    void doThing(net.minecraft.Component) -> m
                    void doThing(long) -> n
                    net.minecraft.Component component -> x
                net.minecraft.Component -> b:
                net.minecraft.Other -> z:
                """);

        var index = Forge1201SourceOfficialMappingIndex.read(tsrg, tiny, mojmap);

        var classResult = index.resolveClass("net/minecraft/Entity");
        assertEquals(Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT, classResult.status());
        assertEquals("net/minecraft/Entity", classResult.candidates().getFirst().officialName());

        var objectMethod = index.resolveMethod(
                "net/minecraft/Entity", "m_10_", "(Lnet/minecraft/Component;)V");
        assertEquals(Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT, objectMethod.status());
        assertEquals("doThing", objectMethod.candidates().getFirst().officialName());
        assertEquals("(Lnet/minecraft/Component;)V",
                objectMethod.candidates().getFirst().officialDescriptor());
        assertEquals("(Lb;)V", objectMethod.candidates().getFirst().obfuscatedDescriptor());

        var primitiveOverload = index.resolveMethod("net/minecraft/Entity", "m_10_", "(J)V");
        assertEquals(Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT, primitiveOverload.status());
        assertEquals("n", primitiveOverload.candidates().getFirst().obfuscatedName());

        var field = index.resolveField("net/minecraft/Entity", "f_20_");
        assertEquals(Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT, field.status());
        assertEquals("component", field.candidates().getFirst().officialName());
        assertEquals("Lnet/minecraft/Component;", field.candidates().getFirst().officialDescriptor());

        assertEquals(3, index.summary().mojangClassRows());
        assertTrue(index.summary().mojangMappingSha256().matches("[0-9a-f]{64}"));
    }

    @Test
    void reportsDescriptorMismatchInsteadOfMatchingMethodNameAlone() throws Exception {
        Path tsrg = write("joined-mismatch.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (I)V m_10_ 2
                """);
        Path tiny = write("source-mismatch.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                """);
        Path mojmap = write("client-mismatch.txt", """
                net.minecraft.Entity -> a:
                    long doThing(long) -> m
                """);

        var index = Forge1201SourceOfficialMappingIndex.read(tsrg, tiny, mojmap);
        var result = index.resolveMethod("net/minecraft/Entity", "m_10_", "(I)V");

        assertEquals(Forge1201SourceOfficialMappingIndex.Status.DESCRIPTOR_MISMATCH, result.status());
        assertTrue(result.candidates().isEmpty());
    }

    @Test
    void preservesExternalTypesAndRejectsUnmappedMinecraftTypes() throws Exception {
        Path tsrg = write("joined-external.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Entity 1
                \tm (Ljava/lang/String;)V m_10_ 2
                """);
        Path tiny = write("source-external.tiny", """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                """);
        Path mojmap = write("client-external.txt", """
                net.minecraft.Entity -> a:
                    void withText(java.lang.String) -> m
                """);

        var index = Forge1201SourceOfficialMappingIndex.read(tsrg, tiny, mojmap);
        var external = index.resolveMethod(
                "net/minecraft/Entity", "m_10_", "(Ljava/lang/String;)V");
        assertEquals(Forge1201SourceOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES, external.status());
        assertEquals("(Ljava/lang/String;)V", external.candidates().getFirst().officialDescriptor());
    }

    private Path write(String name, String contents) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}
