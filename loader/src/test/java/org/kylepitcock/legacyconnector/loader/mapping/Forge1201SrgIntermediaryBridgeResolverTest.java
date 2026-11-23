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


    @Test
    void preservesBothRealWorldTargetCandidatesWhenIntermediaryIdIsAmbiguousAcrossOwners() throws Exception {
        Path tsrg = write("ambiguous-target.tsrg", """
                tsrg2 obf srg id
                bfj net/minecraft/src/C_507_ 1
                \ta (Lbfo;Lcfz;)V m_8061_ 2
                bfo net/minecraft/src/C_516_ 3
                cfz net/minecraft/src/C_1391_ 4
                """);
        Path sourceTiny = write("ambiguous-source.tiny", """
                v1\tofficial\tintermediary
                CLASS\tbfj\tnet/minecraft/class_1297
                CLASS\tbfo\tnet/minecraft/class_1304
                CLASS\tcfz\tnet/minecraft/class_1799
                METHOD\tbfj\t(Lbfo;Lcfz;)V\ta\tmethod_5673
                """);
        Path targetTiny = write("ambiguous-target.tiny", """
                v1\tofficial\tintermediary
                CLASS\tbtb\tnet/minecraft/class_9460
                CLASS\tbtn\tnet/minecraft/class_1309
                CLASS\tbsy\tnet/minecraft/class_1304
                CLASS\tcuq\tnet/minecraft/class_1799
                METHOD\tbtb\t(Lbsy;Lcuq;)V\ta\tmethod_5673
                METHOD\tbtn\t(Lbsy;Lcuq;)V\ta\tmethod_5673
                """);

        var result = Forge1201SrgIntermediaryBridgeResolver.readIndex(tsrg, sourceTiny, targetTiny)
                .resolveMethod(
                        "net/minecraft/src/C_507_",
                        "m_8061_",
                        "(Lnet/minecraft/src/C_516_;Lnet/minecraft/src/C_1391_;)V");

        assertEquals(Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.INTERMEDIARY_TARGET_AMBIGUOUS,
                result.status());
        assertEquals(2, result.candidates().size());
        assertEquals(java.util.Set.of("net/minecraft/class_9460", "net/minecraft/class_1309"),
                result.candidates().stream()
                        .map(Forge1201SrgIntermediaryBridgeResolver.SymbolCandidate::targetIntermediaryOwner)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of("btb", "btn"),
                result.candidates().stream()
                        .map(Forge1201SrgIntermediaryBridgeResolver.SymbolCandidate::targetOfficialObfuscatedOwner)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of("method_5673"),
                result.candidates().stream()
                        .map(Forge1201SrgIntermediaryBridgeResolver.SymbolCandidate::sourceIntermediaryId)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of("(Lnet/minecraft/class_1304;Lnet/minecraft/class_1799;)V"),
                result.candidates().stream()
                        .map(Forge1201SrgIntermediaryBridgeResolver.SymbolCandidate::targetIntermediaryDescriptor)
                        .collect(java.util.stream.Collectors.toSet()));
    }


    @Test
    void resolvesMissingDirectOwnerThroughOneVerifiedInterfaceOwnerAndPreservesBothOwners() throws Exception {
        var index = readHierarchyFixture(false, false, true);
        var hierarchy = new Forge1201SrgIntermediaryBridgeResolver.VerifiedSourceHierarchy(
                "net/minecraft/src/C_Entity_", "net/minecraft/world/entity/Entity", true,
                java.util.List.of(new Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner(
                        "net/minecraft/src/C_EntityAccess_",
                        "net/minecraft/world/level/entity/EntityAccess",
                        Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.INTERFACE, 1)));

        var result = index.resolveMethodWithVerifiedHierarchy("m_10_", "(I)V", hierarchy);

        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.MAPPED_VIA_INTERFACE_OWNER,
                result.status());
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.INTERMEDIARY_SOURCE_MISSING,
                result.directStatus());
        assertEquals(1, result.candidates().size());
        var candidate = result.candidates().getFirst();
        assertEquals("net/minecraft/src/C_Entity_", candidate.invocationSrgOwner());
        assertEquals("net/minecraft/world/entity/Entity", candidate.invocationOfficialOwner());
        assertEquals("net/minecraft/src/C_EntityAccess_", candidate.mappingSrgOwner());
        assertEquals("net/minecraft/world/level/entity/EntityAccess", candidate.mappingOfficialOwner());
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.INTERFACE,
                candidate.mappingOwnerKind());
        assertEquals("net/minecraft/class_2", candidate.mappingCandidate().sourceIntermediaryOwner());
        assertEquals("method_10", candidate.mappingCandidate().sourceIntermediaryId());
        assertEquals("net/minecraft/class_2", candidate.mappingCandidate().targetIntermediaryOwner());
    }

    @Test
    void doesNotUseAnUnrelatedGlobalOwnerThatIsNotInTheVerifiedHierarchy() throws Exception {
        var index = readHierarchyFixture(false, false, true);
        var hierarchy = new Forge1201SrgIntermediaryBridgeResolver.VerifiedSourceHierarchy(
                "net/minecraft/src/C_Entity_", "net/minecraft/world/entity/Entity", true,
                java.util.List.of(new Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner(
                        "net/minecraft/src/C_EntityAccess_",
                        "net/minecraft/world/level/entity/EntityAccess",
                        Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.INTERFACE, 1)));

        var result = index.resolveMethodWithVerifiedHierarchy("m_10_", "(I)V", hierarchy);

        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.MAPPED_VIA_INTERFACE_OWNER,
                result.status());
        assertEquals(1, result.candidates().size());
        assertEquals("net/minecraft/src/C_EntityAccess_", result.candidates().getFirst().mappingSrgOwner());
    }

    @Test
    void refusesMultipleMappedOwnersInsideTheVerifiedHierarchy() throws Exception {
        var index = readHierarchyFixture(true, false, true);
        var hierarchy = new Forge1201SrgIntermediaryBridgeResolver.VerifiedSourceHierarchy(
                "net/minecraft/src/C_Entity_", "net/minecraft/world/entity/Entity", true,
                java.util.List.of(
                        new Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner(
                                "net/minecraft/src/C_EntityAccess_",
                                "net/minecraft/world/level/entity/EntityAccess",
                                Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.INTERFACE, 1),
                        new Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner(
                                "net/minecraft/src/C_AlternateInterface_",
                                "net/minecraft/world/level/entity/AlternateAccess",
                                Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.INTERFACE, 2)));

        var result = index.resolveMethodWithVerifiedHierarchy("m_10_", "(I)V", hierarchy);

        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.AMBIGUOUS_HIERARCHY_OWNERS,
                result.status());
        assertEquals(2, result.candidates().size());
    }

    @Test
    void refusesFallbackWhenTheSourceHierarchyIsIncomplete() throws Exception {
        var index = readHierarchyFixture(false, false, true);
        var hierarchy = new Forge1201SrgIntermediaryBridgeResolver.VerifiedSourceHierarchy(
                "net/minecraft/src/C_Entity_", "net/minecraft/world/entity/Entity", false,
                java.util.List.of(new Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner(
                        "net/minecraft/src/C_EntityAccess_",
                        "net/minecraft/world/level/entity/EntityAccess",
                        Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.INTERFACE, 1)));

        var result = index.resolveMethodWithVerifiedHierarchy("m_10_", "(I)V", hierarchy);

        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.SOURCE_HIERARCHY_INCOMPLETE,
                result.status());
        assertEquals(0, result.candidates().size());
    }

    @Test
    void directOwnerMappingAlwaysWinsAndOtherDirectFailuresDoNotFallBack() throws Exception {
        var directIndex = readHierarchyFixture(false, true, true);
        var hierarchy = new Forge1201SrgIntermediaryBridgeResolver.VerifiedSourceHierarchy(
                "net/minecraft/src/C_Entity_", "net/minecraft/world/entity/Entity", true,
                java.util.List.of(new Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner(
                        "net/minecraft/src/C_EntityAccess_",
                        "net/minecraft/world/level/entity/EntityAccess",
                        Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.INTERFACE, 1)));

        var direct = directIndex.resolveMethodWithVerifiedHierarchy("m_10_", "(I)V", hierarchy);
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.MAPPED_EXACT,
                direct.status());
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.DIRECT,
                direct.candidates().getFirst().mappingOwnerKind());
        assertEquals("net/minecraft/src/C_Entity_", direct.candidates().getFirst().mappingSrgOwner());

        var missingTargetIndex = readHierarchyFixture(false, true, false);
        var notEligible = missingTargetIndex.resolveMethodWithVerifiedHierarchy("m_10_", "(I)V", hierarchy);
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.DIRECT_STATUS_NOT_ELIGIBLE,
                notEligible.status());
        assertEquals(
                Forge1201SrgIntermediaryBridgeResolver.SymbolStatus.INTERMEDIARY_TARGET_ID_MISSING,
                notEligible.directStatus());
    }

    private Forge1201SrgIntermediaryBridgeResolver.Index readHierarchyFixture(
            boolean includeSecondInterface,
            boolean mapInvocationOwnerDirectly,
            boolean mapAlternateTarget) throws Exception {
        String tsrg = """
                tsrg2 obf srg id
                a net/minecraft/src/C_Entity_ 1
                \tm (I)V m_10_ 2
                b net/minecraft/src/C_EntityAccess_ 3
                \tx (I)V m_10_ 4
                c net/minecraft/src/C_AlternateInterface_ 5
                \tq (I)V m_10_ 6
                z net/minecraft/src/C_Unrelated_ 7
                \tr (I)V m_10_ 8
                """;
        String sourceTiny = """
                v1\tofficial\tintermediary
                CLASS\ta\tnet/minecraft/class_1
                CLASS\tb\tnet/minecraft/class_2
                CLASS\tc\tnet/minecraft/class_3
                CLASS\tz\tnet/minecraft/class_4
                METHOD\tb\t(I)V\tx\tmethod_10
                METHOD\tz\t(I)V\tr\tmethod_99
                """;
        if (includeSecondInterface) {
            sourceTiny += "\nMETHOD\tc\t(I)V\tq\tmethod_20\n";
        }
        if (mapInvocationOwnerDirectly) {
            sourceTiny += "\nMETHOD\ta\t(I)V\tm\tmethod_30\n";
        }
        String targetTiny = """
                v1\tofficial\tintermediary
                CLASS\td\tnet/minecraft/class_1
                CLASS\te\tnet/minecraft/class_2
                CLASS\tf\tnet/minecraft/class_3
                CLASS\tg\tnet/minecraft/class_4
                METHOD\te\t(I)V\ty\tmethod_10
                METHOD\tg\t(I)V\tt\tmethod_99
                """;
        if (includeSecondInterface) {
            targetTiny += "\nMETHOD\tf\t(I)V\tp\tmethod_20\n";
        }
        if (mapAlternateTarget) {
            targetTiny += "\nMETHOD\td\t(I)V\tn\tmethod_30\n";
        }

        Path tsrgPath = write("hierarchy-" + System.nanoTime() + ".tsrg", tsrg);
        Path sourcePath = write("hierarchy-source-" + System.nanoTime() + ".tiny", sourceTiny);
        Path targetPath = write("hierarchy-target-" + System.nanoTime() + ".tiny", targetTiny);
        return Forge1201SrgIntermediaryBridgeResolver.readIndex(tsrgPath, sourcePath, targetPath);
    }

    private Path write(String name, String contents) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }
}
