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

    public enum HierarchyOwnerKind {
        DIRECT, INTERFACE, SUPERCLASS
    }

    public enum OwnerAwareStatus {
        MAPPED_EXACT,
        MAPPED_WITH_EXTERNAL_TYPES,
        MAPPED_VIA_INTERFACE_OWNER,
        MAPPED_VIA_SUPERCLASS_OWNER,
        SOURCE_HIERARCHY_INCOMPLETE,
        DIRECT_STATUS_NOT_ELIGIBLE,
        NO_MAPPED_HIERARCHY_OWNER,
        AMBIGUOUS_HIERARCHY_OWNERS,
        UNRESOLVED_HIERARCHY_CANDIDATE
    }

    /**
     * One SRG-mapped owner from a complete source classfile hierarchy. The official owner is
     * the Mojang-named source runtime classfile name; srgOwner is its unique Forge SRG class name.
     * The invocation owner itself must not appear in ancestors.
     */
    public record SourceHierarchyOwner(
            String srgOwner, String officialOwner, HierarchyOwnerKind kind, int distance) {
        public SourceHierarchyOwner {
            if (srgOwner == null || srgOwner.isBlank()
                    || officialOwner == null || officialOwner.isBlank()
                    || kind == null || kind == HierarchyOwnerKind.DIRECT || distance < 1) {
                throw new IllegalArgumentException("A hierarchy ancestor needs distinct SRG/official owners, "
                        + "an interface/superclass kind, and a positive distance.");
            }
        }
    }

    /**
     * Caller-supplied source hierarchy evidence. Construct this only from a complete, pinned
     * source classfile hierarchy walk and unique source official -> SRG class mapping joins.
     * This value is an audit input, not a runtime remapping configuration.
     */
    public record VerifiedSourceHierarchy(
            String invocationSrgOwner, String invocationOfficialOwner,
            boolean complete, List<SourceHierarchyOwner> ancestors) {
        public VerifiedSourceHierarchy {
            if (invocationSrgOwner == null || invocationSrgOwner.isBlank()
                    || invocationOfficialOwner == null || invocationOfficialOwner.isBlank()) {
                throw new IllegalArgumentException("Invocation owner identities are required.");
            }
            ancestors = List.copyOf(ancestors);
        }
    }

    /** Retains the call-site owner separately from the owner that supplied the mapping identity. */
    public record OwnerAwareSymbolCandidate(
            String invocationSrgOwner,
            String invocationOfficialOwner,
            String mappingSrgOwner,
            String mappingOfficialOwner,
            HierarchyOwnerKind mappingOwnerKind,
            int hierarchyDistance,
            SymbolCandidate mappingCandidate) {}

    public record OwnerAwareSymbolResolution(
            OwnerAwareStatus status,
            SymbolStatus directStatus,
            List<OwnerAwareSymbolCandidate> candidates) {
        public OwnerAwareSymbolResolution {
            candidates = List.copyOf(candidates);
        }
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


        /**
         * Performs an exact-owner-first, hierarchy-bounded method mapping join.
         *
         * <p>Only a direct {@link SymbolStatus#INTERMEDIARY_SOURCE_MISSING} result is eligible
         * for ancestor fallback. The supplied hierarchy must be complete and must contain only
         * source class/interface owners proven by the pinned source classfile index and uniquely
         * mapped back to SRG class owners. Every eligible ancestor is checked using the full SRG
         * descriptor. Any unresolved potential candidate or more than one mapped ancestor fails
         * closed. This method does not verify target runtime classfile resolution and does not
         * create a runtime mapping table.</p>
         */
        public OwnerAwareSymbolResolution resolveMethodWithVerifiedHierarchy(
                String srgName, String srgDescriptor, VerifiedSourceHierarchy hierarchy) {
            if (hierarchy == null) {
                throw new IllegalArgumentException("Verified source hierarchy is required.");
            }

            SymbolResolution direct = resolveMethod(
                    hierarchy.invocationSrgOwner(), srgName, srgDescriptor);
            if (direct.status() == SymbolStatus.MAPPED_EXACT
                    || direct.status() == SymbolStatus.MAPPED_WITH_EXTERNAL_TYPES) {
                List<OwnerAwareSymbolCandidate> candidates = direct.candidates().stream()
                        .map(candidate -> new OwnerAwareSymbolCandidate(
                                hierarchy.invocationSrgOwner(), hierarchy.invocationOfficialOwner(),
                                hierarchy.invocationSrgOwner(), hierarchy.invocationOfficialOwner(),
                                HierarchyOwnerKind.DIRECT, 0, candidate))
                        .toList();
                return new OwnerAwareSymbolResolution(
                        direct.status() == SymbolStatus.MAPPED_EXACT
                                ? OwnerAwareStatus.MAPPED_EXACT : OwnerAwareStatus.MAPPED_WITH_EXTERNAL_TYPES,
                        direct.status(), candidates);
            }

            if (direct.status() != SymbolStatus.INTERMEDIARY_SOURCE_MISSING) {
                return new OwnerAwareSymbolResolution(
                        OwnerAwareStatus.DIRECT_STATUS_NOT_ELIGIBLE, direct.status(), List.of());
            }
            if (!hierarchy.complete()) {
                return new OwnerAwareSymbolResolution(
                        OwnerAwareStatus.SOURCE_HIERARCHY_INCOMPLETE, direct.status(), List.of());
            }

            List<SourceHierarchyOwner> orderedAncestors = hierarchy.ancestors().stream()
                    .sorted(java.util.Comparator.comparingInt(SourceHierarchyOwner::distance)
                            .thenComparing(SourceHierarchyOwner::officialOwner)
                            .thenComparing(SourceHierarchyOwner::srgOwner))
                    .toList();
            java.util.Set<String> seenOwners = new java.util.HashSet<>();
            java.util.Map<String, String> officialOwnerBySrgOwner = new java.util.HashMap<>();
            java.util.Map<String, String> srgOwnerByOfficialOwner = new java.util.HashMap<>();
            java.util.Map<String, OwnerAwareSymbolCandidate> mapped = new java.util.LinkedHashMap<>();
            boolean unresolvedCandidate = false;

            for (SourceHierarchyOwner ancestor : orderedAncestors) {
                if (ancestor.srgOwner().equals(hierarchy.invocationSrgOwner())
                        || ancestor.officialOwner().equals(hierarchy.invocationOfficialOwner())) {
                    throw new IllegalArgumentException(
                            "The invocation owner must not be repeated in the ancestor list.");
                }
                String priorOfficial = officialOwnerBySrgOwner.putIfAbsent(
                        ancestor.srgOwner(), ancestor.officialOwner());
                String priorSrg = srgOwnerByOfficialOwner.putIfAbsent(
                        ancestor.officialOwner(), ancestor.srgOwner());
                if ((priorOfficial != null && !priorOfficial.equals(ancestor.officialOwner()))
                        || (priorSrg != null && !priorSrg.equals(ancestor.srgOwner()))) {
                    unresolvedCandidate = true;
                    continue;
                }
                String ownerKey = ancestor.srgOwner() + "|" + ancestor.officialOwner();
                if (!seenOwners.add(ownerKey)) continue;

                SymbolResolution inherited = resolveMethod(ancestor.srgOwner(), srgName, srgDescriptor);
                if (inherited.status() == SymbolStatus.SRG_SYMBOL_MISSING) {
                    continue;
                }
                if (inherited.status() != SymbolStatus.MAPPED_EXACT
                        && inherited.status() != SymbolStatus.MAPPED_WITH_EXTERNAL_TYPES) {
                    unresolvedCandidate = true;
                    continue;
                }
                if (inherited.candidates().size() != 1) {
                    unresolvedCandidate = true;
                    continue;
                }

                SymbolCandidate mapping = inherited.candidates().getFirst();
                OwnerAwareSymbolCandidate candidate = new OwnerAwareSymbolCandidate(
                        hierarchy.invocationSrgOwner(), hierarchy.invocationOfficialOwner(),
                        ancestor.srgOwner(), ancestor.officialOwner(), ancestor.kind(),
                        ancestor.distance(), mapping);
                mapped.putIfAbsent(ownerKey, candidate);
            }

            if (unresolvedCandidate) {
                return new OwnerAwareSymbolResolution(
                        OwnerAwareStatus.UNRESOLVED_HIERARCHY_CANDIDATE,
                        direct.status(), List.copyOf(mapped.values()));
            }
            if (mapped.isEmpty()) {
                return new OwnerAwareSymbolResolution(
                        OwnerAwareStatus.NO_MAPPED_HIERARCHY_OWNER, direct.status(), List.of());
            }
            if (mapped.size() != 1) {
                return new OwnerAwareSymbolResolution(
                        OwnerAwareStatus.AMBIGUOUS_HIERARCHY_OWNERS,
                        direct.status(), List.copyOf(mapped.values()));
            }

            OwnerAwareSymbolCandidate candidate = mapped.values().iterator().next();
            OwnerAwareStatus status = switch (candidate.mappingOwnerKind()) {
                case INTERFACE -> OwnerAwareStatus.MAPPED_VIA_INTERFACE_OWNER;
                case SUPERCLASS -> OwnerAwareStatus.MAPPED_VIA_SUPERCLASS_OWNER;
                case DIRECT -> throw new IllegalStateException("Ancestor resolution cannot produce a direct owner.");
            };
            return new OwnerAwareSymbolResolution(status, direct.status(), List.of(candidate));
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
