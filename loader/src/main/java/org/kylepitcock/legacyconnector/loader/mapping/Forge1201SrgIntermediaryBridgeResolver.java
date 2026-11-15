package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Read-only composition of Forge SRG -> source official-obfuscated -> cross-version Intermediary edges. */
public final class Forge1201SrgIntermediaryBridgeResolver {
    private Forge1201SrgIntermediaryBridgeResolver() {}

    public enum ClassStatus {
        MAPPED_EXACT, SRG_CLASS_MISSING, SRG_CLASS_AMBIGUOUS,
        SOURCE_INTERMEDIARY_CLASS_MISSING, TARGET_INTERMEDIARY_CLASS_MISSING
    }

    public record ClassResolution(
            ClassStatus status, String srgClass, String sourceOfficialObfuscatedClass,
            String sourceIntermediaryClass, String targetOfficialObfuscatedClass) {}

    public enum SymbolStatus {
        MAPPED_EXACT, MAPPED_WITH_EXTERNAL_TYPES,
        SRG_OWNER_MISSING, SRG_OWNER_AMBIGUOUS, SRG_SYMBOL_MISSING, SRG_SYMBOL_AMBIGUOUS,
        SRG_DESCRIPTOR_UNRESOLVED,
        INTERMEDIARY_SOURCE_MISSING, INTERMEDIARY_SOURCE_AMBIGUOUS,
        INTERMEDIARY_SOURCE_DESCRIPTOR_MISMATCH, INTERMEDIARY_SOURCE_OWNER_UNMAPPED,
        INTERMEDIARY_SOURCE_DESCRIPTOR_UNRESOLVED, INTERMEDIARY_TARGET_ID_MISSING,
        INTERMEDIARY_TARGET_OWNER_CHANGED, INTERMEDIARY_TARGET_AMBIGUOUS,
        INTERMEDIARY_TARGET_DESCRIPTOR_UNRESOLVED, INTERMEDIARY_TARGET_DESCRIPTOR_CHANGED
    }

    public record SymbolCandidate(
            String srgOwner, String srgName, String srgDescriptor,
            String sourceOfficialObfuscatedOwner, String sourceOfficialObfuscatedName,
            String sourceOfficialObfuscatedDescriptor, String sourceIntermediaryOwner,
            String sourceIntermediaryId, String sourceIntermediaryDescriptor,
            String targetOfficialObfuscatedOwner, String targetOfficialObfuscatedName,
            String targetOfficialObfuscatedDescriptor, String targetIntermediaryOwner,
            String targetIntermediaryDescriptor, int externalDescriptorTypeCount) {}

    public record SymbolResolution(SymbolStatus status, List<SymbolCandidate> candidates) {
        public SymbolResolution { candidates = List.copyOf(candidates); }
    }

    public static final class Index {
        private final Forge1201SrgObfuscatedBridgeAudit.Index srg;
        private final Forge1201IntermediaryBridgeAudit.Index intermediary;

        private Index(Forge1201SrgObfuscatedBridgeAudit.Index srg,
                Forge1201IntermediaryBridgeAudit.Index intermediary) {
            this.srg = srg;
            this.intermediary = intermediary;
        }

        public List<Forge1201SrgObfuscatedBridgeAudit.ClassSelector> classSelectors() { return srg.classSelectors(); }
        public List<Forge1201SrgObfuscatedBridgeAudit.MethodSelector> methodSelectors() { return srg.methodSelectors(); }
        public List<Forge1201SrgObfuscatedBridgeAudit.FieldSelector> fieldSelectors() { return srg.fieldSelectors(); }

        public ClassResolution resolveClass(String srgClass) {
            var sourceClass = srg.resolveClass(srgClass);
            if (sourceClass.status() == Forge1201SrgObfuscatedBridgeAudit.Status.MISSING) {
                return new ClassResolution(ClassStatus.SRG_CLASS_MISSING, srgClass, null, null, null);
            }
            if (sourceClass.status() == Forge1201SrgObfuscatedBridgeAudit.Status.AMBIGUOUS) {
                return new ClassResolution(ClassStatus.SRG_CLASS_AMBIGUOUS, srgClass, null, null, null);
            }
            String sourceOfficial = sourceClass.obfuscatedNames().getFirst();
            var cross = intermediary.resolveClass(sourceOfficial);
            if (cross.status() == Forge1201IntermediaryBridgeAudit.ClassStatus.SOURCE_CLASS_MISSING) {
                return new ClassResolution(
                        ClassStatus.SOURCE_INTERMEDIARY_CLASS_MISSING, srgClass, sourceOfficial, null, null);
            }
            if (cross.status() == Forge1201IntermediaryBridgeAudit.ClassStatus.TARGET_CLASS_ID_MISSING) {
                return new ClassResolution(
                        ClassStatus.TARGET_INTERMEDIARY_CLASS_MISSING, srgClass, sourceOfficial,
                        cross.intermediaryName(), null);
            }
            return new ClassResolution(
                    ClassStatus.MAPPED_EXACT, srgClass, sourceOfficial,
                    cross.intermediaryName(), cross.targetOfficialName());
        }

        public SymbolResolution resolveMethod(String srgOwner, String srgName, String srgDescriptor) {
            var source = srg.resolveMethod(srgOwner, srgName, srgDescriptor);
            if (!isMapped(source.status())) return new SymbolResolution(mapSrgStatus(source.status()), List.of());
            var row = source.candidates().getFirst();
            var cross = intermediary.resolveMethod(row.obfuscatedOwner(), row.obfuscatedName(), row.obfuscatedDescriptor());
            return compose(srgOwner, srgName, srgDescriptor, row, source.status(), cross);
        }

        public SymbolResolution resolveMethodRow(Forge1201SrgObfuscatedBridgeAudit.MethodSelector selector) {
            if (selector.sourceStatus() != Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT
                    && selector.sourceStatus() != Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_WITH_EXTERNAL_TYPES) {
                return new SymbolResolution(mapSrgStatus(selector.sourceStatus()), List.of());
            }
            return resolveMethod(selector.srgOwner(), selector.srgName(), selector.srgDescriptor());
        }

        public SymbolResolution resolveField(String srgOwner, String srgName) {
            var source = srg.resolveField(srgOwner, srgName);
            if (!isMapped(source.status())) return new SymbolResolution(mapSrgStatus(source.status()), List.of());
            var row = source.candidates().getFirst();
            var cross = intermediary.resolveField(row.obfuscatedOwner(), row.obfuscatedName(), row.obfuscatedDescriptor());
            return compose(srgOwner, srgName, row.sourceDescriptor(), row, source.status(), cross);
        }

        public SymbolResolution resolveFieldRow(Forge1201SrgObfuscatedBridgeAudit.FieldSelector selector) {
            if (selector.sourceStatus() != Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT
                    && selector.sourceStatus() != Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_WITH_EXTERNAL_TYPES) {
                return new SymbolResolution(mapSrgStatus(selector.sourceStatus()), List.of());
            }
            return resolveField(selector.srgOwner(), selector.srgName());
        }

        private static boolean isMapped(Forge1201SrgObfuscatedBridgeAudit.Status status) {
            return status == Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_EXACT
                    || status == Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_WITH_EXTERNAL_TYPES;
        }

        private static SymbolResolution compose(
                String srgOwner, String srgName, String srgDescriptor,
                Forge1201SrgObfuscatedBridgeAudit.Candidate row,
                Forge1201SrgObfuscatedBridgeAudit.Status srgStatus,
                Forge1201IntermediaryBridgeAudit.SymbolResolution cross) {
            SymbolStatus status = switch (cross.status()) {
                case MAPPED_EXACT -> srgStatus == Forge1201SrgObfuscatedBridgeAudit.Status.MAPPED_WITH_EXTERNAL_TYPES
                        ? SymbolStatus.MAPPED_WITH_EXTERNAL_TYPES : SymbolStatus.MAPPED_EXACT;
                case MAPPED_WITH_EXTERNAL_TYPES -> SymbolStatus.MAPPED_WITH_EXTERNAL_TYPES;
                case SOURCE_MISSING -> SymbolStatus.INTERMEDIARY_SOURCE_MISSING;
                case SOURCE_AMBIGUOUS -> SymbolStatus.INTERMEDIARY_SOURCE_AMBIGUOUS;
                case SOURCE_DESCRIPTOR_MISMATCH -> SymbolStatus.INTERMEDIARY_SOURCE_DESCRIPTOR_MISMATCH;
                case SOURCE_OWNER_UNMAPPED -> SymbolStatus.INTERMEDIARY_SOURCE_OWNER_UNMAPPED;
                case SOURCE_DESCRIPTOR_UNRESOLVED -> SymbolStatus.INTERMEDIARY_SOURCE_DESCRIPTOR_UNRESOLVED;
                case TARGET_ID_MISSING -> SymbolStatus.INTERMEDIARY_TARGET_ID_MISSING;
                case TARGET_OWNER_CHANGED -> SymbolStatus.INTERMEDIARY_TARGET_OWNER_CHANGED;
                case TARGET_AMBIGUOUS -> SymbolStatus.INTERMEDIARY_TARGET_AMBIGUOUS;
                case TARGET_DESCRIPTOR_UNRESOLVED -> SymbolStatus.INTERMEDIARY_TARGET_DESCRIPTOR_UNRESOLVED;
                case TARGET_DESCRIPTOR_CHANGED -> SymbolStatus.INTERMEDIARY_TARGET_DESCRIPTOR_CHANGED;
            };
            List<SymbolCandidate> candidates = cross.candidates().stream().map(candidate ->
                    new SymbolCandidate(
                            srgOwner, srgName, srgDescriptor,
                            row.obfuscatedOwner(), row.obfuscatedName(), row.obfuscatedDescriptor(),
                            candidate.sourceOwnerIntermediary(), candidate.intermediaryId(),
                            candidate.sourceDescriptorIntermediary(),
                            candidate.targetOwnerOfficial(), candidate.targetNameOfficial(),
                            candidate.targetDescriptorOfficial(), candidate.targetOwnerIntermediary(),
                            candidate.targetDescriptorIntermediary(), candidate.externalDescriptorTypeCount()))
                    .toList();
            return new SymbolResolution(status, candidates);
        }

        private static SymbolStatus mapSrgStatus(Forge1201SrgObfuscatedBridgeAudit.Status status) {
            return switch (status) {
                case OWNER_MISSING -> SymbolStatus.SRG_OWNER_MISSING;
                case OWNER_AMBIGUOUS -> SymbolStatus.SRG_OWNER_AMBIGUOUS;
                case MISSING -> SymbolStatus.SRG_SYMBOL_MISSING;
                case AMBIGUOUS -> SymbolStatus.SRG_SYMBOL_AMBIGUOUS;
                case DESCRIPTOR_UNRESOLVED -> SymbolStatus.SRG_DESCRIPTOR_UNRESOLVED;
                case MAPPED_EXACT, MAPPED_WITH_EXTERNAL_TYPES ->
                        throw new IllegalArgumentException("Mapped SRG result must proceed to Intermediary.");
            };
        }
    }

    public static Index readIndex(Path joinedTsrg, Path sourceTiny, Path targetTiny) throws IOException {
        return new Index(
                Forge1201SrgObfuscatedBridgeAudit.read(joinedTsrg, sourceTiny),
                Forge1201IntermediaryBridgeAudit.readIndex(joinedTsrg, sourceTiny, targetTiny));
    }
}
