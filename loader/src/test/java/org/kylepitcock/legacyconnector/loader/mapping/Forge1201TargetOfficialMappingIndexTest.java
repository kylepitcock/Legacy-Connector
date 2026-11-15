package org.kylepitcock.legacyconnector.loader.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201TargetOfficialMappingIndexTest {
    @TempDir
    Path tempDir;

    @Test
    void mapsClassMethodOwnerAndEveryObjectTypeInArrayDescriptor() throws Exception {
        Path mappings = write("target.tsrg", """
                tsrg2 left right
                a net/minecraft/world/level/Level
                \tm (Lb;[Lb;)Lc; getBlockState
                b net/minecraft/core/BlockPos
                c net/minecraft/world/level/block/state/BlockState
                """);

        var index = Forge1201TargetOfficialMappingIndex.read(mappings);
        var result = index.resolveMethod("a", "m", "(Lb;[Lb;)Lc;");

        assertEquals(Forge1201TargetOfficialMappingIndex.Status.MAPPED_EXACT, result.status());
        assertEquals(1, result.candidates().size());
        var candidate = result.candidates().getFirst();
        assertEquals("net/minecraft/world/level/Level", candidate.officialOwner());
        assertEquals("getBlockState", candidate.officialName());
        assertEquals(
                "(Lnet/minecraft/core/BlockPos;[Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                candidate.officialDescriptor());
        assertEquals(0, candidate.externalDescriptorTypeCount());
        assertEquals(3, index.summary().classRows());
        assertEquals(1, index.summary().methodRows());
        assertEquals(64, index.summary().sha256().length());
    }

    @Test
    void passesPackageQualifiedExternalDescriptorTypesWithoutCallingThemExact() throws Exception {
        Path mappings = write("external.tsrg", """
                tsrg2 left right
                a net/minecraft/Entity
                \tm (Ljava/lang/String;Lb;)V externalMethod
                \tn (LunmappedObfuscatedType;)V unresolvedMethod
                b net/minecraft/BlockPos
                """);

        var index = Forge1201TargetOfficialMappingIndex.read(mappings);
        var external = index.resolveMethod("a", "m", "(Ljava/lang/String;Lb;)V");
        assertEquals(Forge1201TargetOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES,
                external.status());
        assertEquals("(Ljava/lang/String;Lnet/minecraft/BlockPos;)V",
                external.candidates().getFirst().officialDescriptor());
        assertEquals(1, external.candidates().getFirst().externalDescriptorTypeCount());

        var unresolved = index.resolveMethod("a", "n", "(LunmappedObfuscatedType;)V");
        assertEquals(Forge1201TargetOfficialMappingIndex.Status.DESCRIPTOR_UNRESOLVED, unresolved.status());
    }

    @Test
    void preservesConflictingClassAndMethodMappingsAsAmbiguous() throws Exception {
        Path mappings = write("ambiguous.tsrg", """
                tsrg2 left right
                a net/minecraft/First
                \tm ()I firstName
                \tm ()I secondName
                b net/minecraft/Second
                b net/minecraft/OtherSecond
                """);

        var index = Forge1201TargetOfficialMappingIndex.read(mappings);

        assertEquals(Forge1201TargetOfficialMappingIndex.Status.OWNER_AMBIGUOUS,
                index.resolveClass("b").status());
        assertEquals(Forge1201TargetOfficialMappingIndex.Status.OWNER_AMBIGUOUS,
                index.resolveMethod("b", "m", "()I").status());
        assertEquals(Forge1201TargetOfficialMappingIndex.Status.MEMBER_AMBIGUOUS,
                index.resolveMethod("a", "m", "()I").status());
        assertEquals(1, index.summary().ambiguousClassKeys());
        assertEquals(1, index.summary().ambiguousMethodKeys());
    }

    @Test
    void mapsDescriptorlessAndTypedFieldsUsingCallerSuppliedTargetDescriptor() throws Exception {
        Path mappings = write("fields.tsrg", """
                tsrg2 left right
                a net/minecraft/Fixture
                \tf Lb; typedField
                \tg descriptorlessField
                b net/minecraft/BlockPos
                """);

        var index = Forge1201TargetOfficialMappingIndex.read(mappings);

        var typed = index.resolveField("a", "f", "Lb;");
        assertEquals(Forge1201TargetOfficialMappingIndex.Status.MAPPED_EXACT, typed.status());
        assertEquals("typedField", typed.candidates().getFirst().officialName());
        assertEquals("Lnet/minecraft/BlockPos;", typed.candidates().getFirst().officialDescriptor());

        var mismatch = index.resolveField("a", "f", "I");
        assertEquals(Forge1201TargetOfficialMappingIndex.Status.DESCRIPTOR_MISMATCH, mismatch.status());

        var descriptorless = index.resolveField("a", "g", "[Lb;");
        assertEquals(Forge1201TargetOfficialMappingIndex.Status.MAPPED_EXACT, descriptorless.status());
        assertEquals("[Lnet/minecraft/BlockPos;",
                descriptorless.candidates().getFirst().officialDescriptor());
    }

    @Test
    void rejectsWrongHeaderAndMalformedRows() throws Exception {
        Path wrongHeader = write("wrong-header.tsrg", """
                tsrg2 obf srg id
                a net/minecraft/Fixture
                """);
        Path malformed = write("malformed.tsrg", """
                tsrg2 left right
                a net/minecraft/Fixture
                \tm not-a-descriptor mappedName
                """);

        assertThrows(java.io.IOException.class,
                () -> Forge1201TargetOfficialMappingIndex.read(wrongHeader));
        assertThrows(java.io.IOException.class,
                () -> Forge1201TargetOfficialMappingIndex.read(malformed));
    }

    @Test
    void enforcesTheCallerSuppliedMappingFingerprint() throws Exception {
        Path mappings = write("pinned.tsrg", """
                tsrg2 left right
                a net/minecraft/Fixture
                """);

        assertThrows(java.io.IOException.class,
                () -> Forge1201TargetOfficialMappingIndex.read(mappings, "0".repeat(64)));
    }

    private Path write(String name, String content) throws Exception {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }
}
