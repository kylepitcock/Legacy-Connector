package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201SourceRuntimeMemberVerifier;
import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201TargetApiVerifier;
import org.objectweb.asm.Opcodes;

/**
 * Read-only end-to-end audit for the bounded Forge SRG -> source Intermediary -> target
 * Intermediary -> target Mojang-official hierarchy-owner mapping path.
 *
 * <p>The source hierarchy is derived from the pinned Forge 1.20.1 classfiles. A fallback owner
 * is accepted only when that classfile directly declares the exact public instance method and
 * the source SRG/Mojang mapping index maps that declaration back to the same SRG member name and
 * full descriptor. The owner-aware Intermediary resolver then performs its exact-owner-first,
 * hierarchy-bounded mapping join. Finally, the target official mapping and target classfiles
 * independently verify the mapped invocation and declaration owners.</p>
 *
 * <p>This class is diagnostic only. It does not mutate production mappings, rewrite selectors,
 * define or initialize Minecraft classes, or emit a runtime mapping table.</p>
 */
public final class Forge1201SrgIntermediaryHierarchyAudit {
    private static final int MAX_HIERARCHY_NODES = 4096;

    private Forge1201SrgIntermediaryHierarchyAudit() {}

    public enum AuditStatus {
        TARGET_VERIFIED,
        TARGET_VERIFIED_WITH_EXTERNAL_TYPES,
        SOURCE_MEMBER_MAPPING_NOT_UNIQUE,
        SOURCE_INVOCATION_CLASS_MISSING,
        SOURCE_INVOCATION_METHOD_NOT_PUBLIC_INSTANCE,
        SOURCE_HIERARCHY_INCOMPLETE,
        SOURCE_OWNER_FALLBACK_REJECTED,
        TARGET_INVOCATION_CLASS_MAPPING_FAILED,
        TARGET_MAPPING_OWNER_CLASS_MAPPING_FAILED,
        TARGET_METHOD_MAPPING_FAILED,
        TARGET_HIERARCHY_INCOMPLETE,
        TARGET_MAPPING_OWNER_NOT_IN_HIERARCHY,
        TARGET_MAPPING_OWNER_METHOD_NOT_PUBLIC_INSTANCE,
        TARGET_INVOCATION_METHOD_NOT_PUBLIC_INSTANCE
    }

    public record MethodAuditResult(
            String caseId,
            String srgOwner,
            String srgName,
            String srgDescriptor,
            AuditStatus status,
            Forge1201SourceOfficialMappingIndex.Status sourceOfficialStatus,
            Forge1201SrgIntermediaryBridgeResolver.OwnerAwareSymbolResolution ownerAwareResolution,
            String sourceInvocationOfficialOwner,
            String sourceOfficialMethodName,
            String sourceOfficialMethodDescriptor,
            List<String> sourceHierarchyVisited,
            String targetInvocationOfficialOwner,
            String targetMappingOfficialOwner,
            String targetOfficialMethodName,
            String targetOfficialMethodDescriptor,
            List<String> targetHierarchyVisited,
            String detail) {
        public MethodAuditResult {
            sourceHierarchyVisited = List.copyOf(sourceHierarchyVisited);
            targetHierarchyVisited = List.copyOf(targetHierarchyVisited);
        }
    }

    private record HierarchyNode(String owner, Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind kind,
            int distance) {}

    private record SourceHierarchyDiscovery(
            boolean complete,
            List<Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner> mappedAncestors,
            List<String> visited,
            List<String> unresolved,
            String detail) {
        private SourceHierarchyDiscovery {
            mappedAncestors = List.copyOf(mappedAncestors);
            visited = List.copyOf(visited);
            unresolved = List.copyOf(unresolved);
        }
    }

    private record TargetHierarchyDiscovery(boolean complete, List<String> visited, List<String> unresolved) {
        private TargetHierarchyDiscovery {
            visited = List.copyOf(visited);
            unresolved = List.copyOf(unresolved);
        }
    }

    private record TargetVerification(
            AuditStatus status,
            String invocationOwner,
            String mappingOwner,
            String methodName,
            String methodDescriptor,
            List<String> hierarchyVisited,
            String detail) {
        private TargetVerification {
            hierarchyVisited = List.copyOf(hierarchyVisited);
        }
    }

    /**
     * Audits one selector through source mappings, actual source hierarchy, owner-aware
     * Intermediary resolution, target Mojmap mappings, and actual target classfiles.
     */
    public static MethodAuditResult analyzeMethod(
            String caseId,
            String srgOwner,
            String srgName,
            String srgDescriptor,
            Forge1201SrgIntermediaryBridgeResolver.Index intermediaryBridge,
            Forge1201SourceOfficialMappingIndex sourceMappings,
            Forge1201SourceRuntimeMemberVerifier.Index sourceClasses,
            Forge1201TargetOfficialMappingIndex.Index targetMappings,
            Forge1201SourceRuntimeMemberVerifier.Index targetClasses) throws IOException {
        requireNonNull(caseId, "caseId");
        requireNonNull(srgOwner, "srgOwner");
        requireNonNull(srgName, "srgName");
        requireNonNull(srgDescriptor, "srgDescriptor");
        requireNonNull(intermediaryBridge, "intermediaryBridge");
        requireNonNull(sourceMappings, "sourceMappings");
        requireNonNull(sourceClasses, "sourceClasses");
        requireNonNull(targetMappings, "targetMappings");
        requireNonNull(targetClasses, "targetClasses");

        Forge1201SourceOfficialMappingIndex.SelectorResolution sourceResolution =
                sourceMappings.resolveMethod(srgOwner, srgName, srgDescriptor);
        boolean sourceMapped = sourceResolution.status()
                == Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT
                || sourceResolution.status()
                == Forge1201SourceOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES;
        if (!sourceMapped || sourceResolution.candidates().size() != 1) {
            return failed(caseId, srgOwner, srgName, srgDescriptor,
                    AuditStatus.SOURCE_MEMBER_MAPPING_NOT_UNIQUE, sourceResolution.status(),
                    null, null, null, null, List.of(),
                    "Source SRG -> Mojang-official mapping did not produce exactly one candidate.");
        }

        Forge1201SourceOfficialMappingIndex.SelectorCandidate sourceCandidate =
                sourceResolution.candidates().getFirst();
        Forge1201SourceOfficialMappingIndex.ClassResolution sourceOwnerResolution =
                sourceMappings.resolveClass(srgOwner);
        if (sourceOwnerResolution.status() != Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT
                || sourceOwnerResolution.candidates().size() != 1
                || !sourceOwnerResolution.candidates().getFirst().officialName()
                        .equals(sourceCandidate.officialOwner())) {
            return failed(caseId, srgOwner, srgName, srgDescriptor,
                    AuditStatus.SOURCE_MEMBER_MAPPING_NOT_UNIQUE, sourceResolution.status(),
                    null, sourceCandidate.officialOwner(), sourceCandidate.officialName(),
                    sourceCandidate.officialDescriptor(), List.of(),
                    "The invocation class owner does not have one consistent source Mojang-official identity.");
        }

        Optional<Forge1201TargetApiVerifier.TargetClassInfo> invocationInfo =
                sourceClasses.classInfo(sourceCandidate.officialOwner());
        if (invocationInfo.isEmpty()) {
            return failed(caseId, srgOwner, srgName, srgDescriptor,
                    AuditStatus.SOURCE_INVOCATION_CLASS_MISSING, sourceResolution.status(),
                    null, sourceCandidate.officialOwner(), sourceCandidate.officialName(),
                    sourceCandidate.officialDescriptor(), List.of(sourceCandidate.officialOwner()),
                    "The source invocation owner is absent from the pinned Forge runtime classpath.");
        }

        Forge1201TargetApiVerifier.MemberSignature sourceSignature =
                new Forge1201TargetApiVerifier.MemberSignature(
                        sourceCandidate.officialName(), sourceCandidate.officialDescriptor());
        if (!invocationInfo.get().methods().contains(sourceSignature)
                || !isPublicInstance(sourceClasses.declaredMethodAccess(
                        sourceCandidate.officialOwner(),
                        sourceCandidate.officialName(),
                        sourceCandidate.officialDescriptor()))) {
            return failed(caseId, srgOwner, srgName, srgDescriptor,
                    AuditStatus.SOURCE_INVOCATION_METHOD_NOT_PUBLIC_INSTANCE, sourceResolution.status(),
                    null, sourceCandidate.officialOwner(), sourceCandidate.officialName(),
                    sourceCandidate.officialDescriptor(), List.of(sourceCandidate.officialOwner()),
                    "The source invocation owner does not directly declare the exact public instance method.");
        }

        SourceHierarchyDiscovery discovery = discoverSourceHierarchy(
                sourceCandidate.officialOwner(), sourceCandidate.officialName(),
                sourceCandidate.officialDescriptor(), srgName, srgDescriptor,
                sourceMappings, sourceClasses);
        if (!discovery.complete()) {
            return failed(caseId, srgOwner, srgName, srgDescriptor,
                    AuditStatus.SOURCE_HIERARCHY_INCOMPLETE, sourceResolution.status(),
                    null, sourceCandidate.officialOwner(), sourceCandidate.officialName(),
                    sourceCandidate.officialDescriptor(), discovery.visited(), discovery.detail());
        }

        Forge1201SrgIntermediaryBridgeResolver.VerifiedSourceHierarchy verifiedHierarchy =
                new Forge1201SrgIntermediaryBridgeResolver.VerifiedSourceHierarchy(
                        srgOwner, sourceCandidate.officialOwner(), true, discovery.mappedAncestors());
        Forge1201SrgIntermediaryBridgeResolver.OwnerAwareSymbolResolution ownerAware =
                intermediaryBridge.resolveMethodWithVerifiedHierarchy(srgName, srgDescriptor, verifiedHierarchy);

        if (ownerAware.status()
                        != Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.MAPPED_EXACT
                && ownerAware.status()
                        != Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.MAPPED_WITH_EXTERNAL_TYPES
                && ownerAware.status()
                        != Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.MAPPED_VIA_INTERFACE_OWNER
                && ownerAware.status()
                        != Forge1201SrgIntermediaryBridgeResolver.OwnerAwareStatus.MAPPED_VIA_SUPERCLASS_OWNER) {
            return failed(caseId, srgOwner, srgName, srgDescriptor,
                    AuditStatus.SOURCE_OWNER_FALLBACK_REJECTED, sourceResolution.status(),
                    ownerAware, sourceCandidate.officialOwner(), sourceCandidate.officialName(),
                    sourceCandidate.officialDescriptor(), discovery.visited(),
                    "The owner-aware Intermediary resolver rejected the source hierarchy candidate: "
                            + ownerAware.status());
        }
        if (ownerAware.candidates().size() != 1) {
            return failed(caseId, srgOwner, srgName, srgDescriptor,
                    AuditStatus.SOURCE_OWNER_FALLBACK_REJECTED, sourceResolution.status(),
                    ownerAware, sourceCandidate.officialOwner(), sourceCandidate.officialName(),
                    sourceCandidate.officialDescriptor(), discovery.visited(),
                    "The owner-aware Intermediary resolver did not produce exactly one candidate.");
        }

        TargetVerification target = verifyTarget(
                srgOwner, ownerAware.candidates().getFirst(), intermediaryBridge,
                targetMappings, targetClasses);
        return new MethodAuditResult(
                caseId, srgOwner, srgName, srgDescriptor,
                target.status(), sourceResolution.status(), ownerAware,
                sourceCandidate.officialOwner(), sourceCandidate.officialName(),
                sourceCandidate.officialDescriptor(), discovery.visited(),
                target.invocationOwner(), target.mappingOwner(), target.methodName(),
                target.methodDescriptor(), target.hierarchyVisited(), target.detail());
    }

    private static SourceHierarchyDiscovery discoverSourceHierarchy(
            String invocationOwner,
            String methodName,
            String methodDescriptor,
            String srgName,
            String srgDescriptor,
            Forge1201SourceOfficialMappingIndex sourceMappings,
            Forge1201SourceRuntimeMemberVerifier.Index sourceClasses) throws IOException {
        Queue<HierarchyNode> pending = new ArrayDeque<>();
        pending.add(new HierarchyNode(invocationOwner,
                Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.DIRECT, 0));
        Set<String> enqueued = new HashSet<>();
        Set<String> visitedSet = new LinkedHashSet<>();
        List<String> visited = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        List<Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner> mappedAncestors =
                new ArrayList<>();
        boolean complete = true;
        String detail = "Source hierarchy fully inspected.";

        while (!pending.isEmpty()) {
            if (visited.size() >= MAX_HIERARCHY_NODES) {
                complete = false;
                detail = "Source hierarchy exceeded the bounded node limit " + MAX_HIERARCHY_NODES + ".";
                break;
            }
            HierarchyNode node = pending.remove();
            if (!visitedSet.add(node.owner())) continue;
            visited.add(node.owner());

            Optional<Forge1201TargetApiVerifier.TargetClassInfo> info = sourceClasses.classInfo(node.owner());
            if (info.isEmpty()) {
                complete = false;
                unresolved.add(node.owner());
                detail = "One or more source hierarchy classfiles could not be resolved.";
                continue;
            }

            if (node.distance() > 0
                    && info.get().methods().contains(
                            new Forge1201TargetApiVerifier.MemberSignature(methodName, methodDescriptor))) {
                if (!isPublicInstance(sourceClasses.declaredMethodAccess(
                        node.owner(), methodName, methodDescriptor))) {
                    complete = false;
                    unresolved.add(node.owner() + "#" + methodName + methodDescriptor
                            + " (not a public instance declaration)");
                    detail = "A matching source hierarchy declaration is not public and non-static.";
                } else {
                    Forge1201SourceOfficialMappingIndex.SelectorResolution reverse =
                            sourceMappings.findMethodsByOfficialIdentity(node.owner(), methodName, methodDescriptor);
                    boolean mapped = (reverse.status()
                            == Forge1201SourceOfficialMappingIndex.Status.MAPPED_EXACT
                            || reverse.status()
                            == Forge1201SourceOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES)
                            && reverse.candidates().size() == 1;
                    if (!mapped) {
                        complete = false;
                        unresolved.add(node.owner() + "#" + methodName + methodDescriptor
                                + " (" + reverse.status() + ", candidates=" + reverse.candidates().size() + ")");
                        detail = "A matching source declaration has no unique SRG/Mojang mapping identity.";
                    } else {
                        Forge1201SourceOfficialMappingIndex.SelectorCandidate mappedDeclaration =
                                reverse.candidates().getFirst();
                        if (!mappedDeclaration.srgName().equals(srgName)
                                || !mappedDeclaration.srgDescriptor().equals(srgDescriptor)
                                || !mappedDeclaration.officialOwner().equals(node.owner())
                                || !mappedDeclaration.officialName().equals(methodName)
                                || !mappedDeclaration.officialDescriptor().equals(methodDescriptor)) {
                            complete = false;
                            unresolved.add(node.owner() + "#" + methodName + methodDescriptor
                                    + " (SRG member identity/descriptor mismatch)");
                            detail = "The source declaration does not map to the requested exact SRG member identity.";
                        } else {
                            mappedAncestors.add(new Forge1201SrgIntermediaryBridgeResolver.SourceHierarchyOwner(
                                    mappedDeclaration.srgOwner(), node.owner(), node.kind(), node.distance()));
                        }
                    }
                }
            }

            List<String> interfaces = info.get().interfaces().stream().sorted().toList();
            for (String implementedInterface : interfaces) {
                if (enqueued.add(implementedInterface)) {
                    pending.add(new HierarchyNode(implementedInterface,
                            Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.INTERFACE,
                            node.distance() + 1));
                }
            }
            String superName = info.get().superName();
            if (superName != null && enqueued.add(superName)) {
                pending.add(new HierarchyNode(superName,
                        Forge1201SrgIntermediaryBridgeResolver.HierarchyOwnerKind.SUPERCLASS,
                        node.distance() + 1));
            }
        }

        return new SourceHierarchyDiscovery(
                complete, mappedAncestors, visited, unresolved, detail);
    }

    private static TargetVerification verifyTarget(
            String invocationSrgOwner,
            Forge1201SrgIntermediaryBridgeResolver.OwnerAwareSymbolCandidate ownerCandidate,
            Forge1201SrgIntermediaryBridgeResolver.Index intermediaryBridge,
            Forge1201TargetOfficialMappingIndex.Index targetMappings,
            Forge1201SourceRuntimeMemberVerifier.Index targetClasses) throws IOException {
        Forge1201SrgIntermediaryBridgeResolver.ClassResolution invocationBridge =
                intermediaryBridge.resolveClass(invocationSrgOwner);
        if (invocationBridge.status()
                != Forge1201SrgIntermediaryBridgeResolver.ClassStatus.MAPPED_EXACT) {
            return targetFailure(AuditStatus.TARGET_INVOCATION_CLASS_MAPPING_FAILED,
                    null, null, null, null, List.of(),
                    "The invocation class has no unique source-to-target Intermediary class identity: "
                            + invocationBridge.status());
        }

        Forge1201TargetOfficialMappingIndex.ClassResolution invocationClassMapping =
                targetMappings.resolveClass(invocationBridge.targetOfficialObfuscatedClass());
        if (invocationClassMapping.status() != Forge1201TargetOfficialMappingIndex.Status.MAPPED_EXACT
                || invocationClassMapping.candidates().size() != 1) {
            return targetFailure(AuditStatus.TARGET_INVOCATION_CLASS_MAPPING_FAILED,
                    null, null, null, null, List.of(),
                    "The target invocation class has no unique target official/Mojmap class mapping.");
        }
        String targetInvocationOwner = invocationClassMapping.candidates().getFirst().officialName();

        Forge1201SrgIntermediaryBridgeResolver.SymbolCandidate mappingCandidate =
                ownerCandidate.mappingCandidate();
        Forge1201TargetOfficialMappingIndex.ClassResolution mappingClassMapping =
                targetMappings.resolveClass(mappingCandidate.targetOfficialObfuscatedOwner());
        if (mappingClassMapping.status() != Forge1201TargetOfficialMappingIndex.Status.MAPPED_EXACT
                || mappingClassMapping.candidates().size() != 1) {
            return targetFailure(AuditStatus.TARGET_MAPPING_OWNER_CLASS_MAPPING_FAILED,
                    targetInvocationOwner, null, null, null, List.of(),
                    "The target mapping/declaration owner has no unique target official/Mojmap class mapping.");
        }
        String targetMappingOwner = mappingClassMapping.candidates().getFirst().officialName();

        Forge1201TargetOfficialMappingIndex.MethodResolution methodMapping =
                targetMappings.resolveMethod(
                        mappingCandidate.targetOfficialObfuscatedOwner(),
                        mappingCandidate.targetOfficialObfuscatedName(),
                        mappingCandidate.targetOfficialObfuscatedDescriptor());
        boolean methodMapped = methodMapping.status() == Forge1201TargetOfficialMappingIndex.Status.MAPPED_EXACT
                || methodMapping.status() == Forge1201TargetOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES;
        if (!methodMapped || methodMapping.candidates().size() != 1) {
            return targetFailure(AuditStatus.TARGET_METHOD_MAPPING_FAILED,
                    targetInvocationOwner, targetMappingOwner, null, null, List.of(),
                    "The target Intermediary member does not have one exact target official/Mojmap mapping: "
                            + methodMapping.status());
        }
        Forge1201TargetOfficialMappingIndex.MethodCandidate targetMethod =
                methodMapping.candidates().getFirst();
        if (!targetMethod.officialOwner().equals(targetMappingOwner)) {
            return targetFailure(AuditStatus.TARGET_METHOD_MAPPING_FAILED,
                    targetInvocationOwner, targetMappingOwner, targetMethod.officialName(),
                    targetMethod.officialDescriptor(), List.of(),
                    "The target member owner and target class mapping disagree.");
        }

        TargetHierarchyDiscovery hierarchy = discoverTargetHierarchy(
                targetInvocationOwner, targetClasses);
        if (!hierarchy.complete()) {
            return targetFailure(AuditStatus.TARGET_HIERARCHY_INCOMPLETE,
                    targetInvocationOwner, targetMappingOwner, targetMethod.officialName(),
                    targetMethod.officialDescriptor(), hierarchy.visited(),
                    "The target invocation hierarchy is incomplete: " + hierarchy.unresolved());
        }
        if (!hierarchy.visited().contains(targetMappingOwner)) {
            return targetFailure(AuditStatus.TARGET_MAPPING_OWNER_NOT_IN_HIERARCHY,
                    targetInvocationOwner, targetMappingOwner, targetMethod.officialName(),
                    targetMethod.officialDescriptor(), hierarchy.visited(),
                    "The mapped target declaration owner is not in the target invocation hierarchy.");
        }

        Forge1201TargetApiVerifier.MemberSignature signature =
                new Forge1201TargetApiVerifier.MemberSignature(
                        targetMethod.officialName(), targetMethod.officialDescriptor());
        Optional<Forge1201TargetApiVerifier.TargetClassInfo> invocationInfo =
                targetClasses.classInfo(targetInvocationOwner);
        if (invocationInfo.isEmpty()
                || !invocationInfo.get().methods().contains(signature)
                || !isPublicInstance(targetClasses.declaredMethodAccess(
                        targetInvocationOwner, targetMethod.officialName(), targetMethod.officialDescriptor()))) {
            return targetFailure(AuditStatus.TARGET_INVOCATION_METHOD_NOT_PUBLIC_INSTANCE,
                    targetInvocationOwner, targetMappingOwner, targetMethod.officialName(),
                    targetMethod.officialDescriptor(), hierarchy.visited(),
                    "The target invocation owner does not directly declare the exact public instance method.");
        }

        Optional<Forge1201TargetApiVerifier.TargetClassInfo> mappingInfo =
                targetClasses.classInfo(targetMappingOwner);
        if (mappingInfo.isEmpty()
                || !mappingInfo.get().methods().contains(signature)
                || !isPublicInstance(targetClasses.declaredMethodAccess(
                        targetMappingOwner, targetMethod.officialName(), targetMethod.officialDescriptor()))) {
            return targetFailure(AuditStatus.TARGET_MAPPING_OWNER_METHOD_NOT_PUBLIC_INSTANCE,
                    targetInvocationOwner, targetMappingOwner, targetMethod.officialName(),
                    targetMethod.officialDescriptor(), hierarchy.visited(),
                    "The target mapping owner does not directly declare the exact public instance method.");
        }

        AuditStatus status = methodMapping.status()
                        == Forge1201TargetOfficialMappingIndex.Status.MAPPED_WITH_EXTERNAL_TYPES
                || mappingCandidate.externalDescriptorTypeCount() > 0
                ? AuditStatus.TARGET_VERIFIED_WITH_EXTERNAL_TYPES : AuditStatus.TARGET_VERIFIED;
        return targetFailure(status, targetInvocationOwner, targetMappingOwner,
                targetMethod.officialName(), targetMethod.officialDescriptor(), hierarchy.visited(),
                "Target invocation and mapped declaration owners both declare the exact public instance method.");
    }

    private static TargetHierarchyDiscovery discoverTargetHierarchy(
            String invocationOwner,
            Forge1201SourceRuntimeMemberVerifier.Index targetClasses) throws IOException {
        Queue<String> pending = new ArrayDeque<>();
        pending.add(invocationOwner);
        Set<String> enqueued = new HashSet<>();
        Set<String> visitedSet = new LinkedHashSet<>();
        List<String> visited = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        boolean complete = true;

        while (!pending.isEmpty()) {
            if (visited.size() >= MAX_HIERARCHY_NODES) {
                complete = false;
                unresolved.add("HIERARCHY_NODE_LIMIT_EXCEEDED");
                break;
            }
            String owner = pending.remove();
            if (!visitedSet.add(owner)) continue;
            visited.add(owner);
            Optional<Forge1201TargetApiVerifier.TargetClassInfo> info = targetClasses.classInfo(owner);
            if (info.isEmpty()) {
                complete = false;
                unresolved.add(owner);
                continue;
            }
            info.get().interfaces().stream().sorted().forEach(name -> {
                if (enqueued.add(name)) pending.add(name);
            });
            String superName = info.get().superName();
            if (superName != null && enqueued.add(superName)) pending.add(superName);
        }
        return new TargetHierarchyDiscovery(complete, visited, unresolved);
    }

    private static boolean isPublicInstance(Optional<Integer> access) {
        return access.isPresent()
                && (access.get() & Opcodes.ACC_PUBLIC) != 0
                && (access.get() & Opcodes.ACC_STATIC) == 0;
    }

    private static MethodAuditResult failed(
            String caseId,
            String srgOwner,
            String srgName,
            String srgDescriptor,
            AuditStatus status,
            Forge1201SourceOfficialMappingIndex.Status sourceOfficialStatus,
            Forge1201SrgIntermediaryBridgeResolver.OwnerAwareSymbolResolution ownerAware,
            String sourceInvocationOwner,
            String sourceMethodName,
            String sourceMethodDescriptor,
            List<String> sourceHierarchyVisited,
            String detail) {
        return new MethodAuditResult(
                caseId, srgOwner, srgName, srgDescriptor, status, sourceOfficialStatus,
                ownerAware, sourceInvocationOwner, sourceMethodName, sourceMethodDescriptor,
                sourceHierarchyVisited, null, null, null, null, List.of(), detail);
    }

    private static TargetVerification targetFailure(
            AuditStatus status,
            String invocationOwner,
            String mappingOwner,
            String methodName,
            String methodDescriptor,
            List<String> hierarchyVisited,
            String detail) {
        return new TargetVerification(status, invocationOwner, mappingOwner, methodName,
                methodDescriptor, hierarchyVisited, detail);
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required.");
    }

    /**
     * Usage:
     * Forge1201SrgIntermediaryHierarchyAudit
     *   <joined.tsrg> <source-intermediary.tiny> <target-intermediary.tiny>
     *   <mojang-1.20.1-client.txt> <target-official.tsrg> <target-official-sha256>
     *   <forge-source-classpath.txt> <target-classpath.txt> <report.txt>
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 9) {
            throw new IllegalArgumentException(
                    "Expected 9 arguments: joined.tsrg source-tiny target-tiny source-mojang.txt "
                            + "target-official.tsrg target-sha256 source-classpath target-classpath report.txt");
        }
        Path joinedTsrg = Path.of(args[0]);
        Path sourceTiny = Path.of(args[1]);
        Path targetTiny = Path.of(args[2]);
        Path sourceMojang = Path.of(args[3]);
        Path targetOfficialTsrg = Path.of(args[4]);
        String targetOfficialSha256 = args[5];
        Path sourceClasspathFile = Path.of(args[6]);
        Path targetClasspathFile = Path.of(args[7]);
        Path report = Path.of(args[8]);

        Forge1201SrgIntermediaryBridgeResolver.Index intermediaryBridge =
                Forge1201SrgIntermediaryBridgeResolver.readIndex(joinedTsrg, sourceTiny, targetTiny);
        Forge1201SourceOfficialMappingIndex sourceMappings =
                Forge1201SourceOfficialMappingIndex.read(joinedTsrg, sourceTiny, sourceMojang);
        Forge1201TargetOfficialMappingIndex.Index targetMappings =
                Forge1201TargetOfficialMappingIndex.read(targetOfficialTsrg, targetOfficialSha256);
        Forge1201SourceRuntimeMemberVerifier.Index sourceClasses =
                Forge1201SourceRuntimeMemberVerifier.readClasspath(sourceClasspathFile);
        Forge1201SourceRuntimeMemberVerifier.Index targetClasses =
                Forge1201SourceRuntimeMemberVerifier.readClasspath(targetClasspathFile);

        List<MethodAuditResult> results = List.of(
                analyzeMethod("ENTITY_UUID", "net/minecraft/src/C_507_", "m_20148_",
                        "()Ljava/util/UUID;", intermediaryBridge, sourceMappings, sourceClasses,
                        targetMappings, targetClasses),
                analyzeMethod("ENTITY_BOUNDING_BOX", "net/minecraft/src/C_507_", "m_20191_",
                        "()Lnet/minecraft/src/C_3040_;", intermediaryBridge, sourceMappings, sourceClasses,
                        targetMappings, targetClasses),
                analyzeMethod("LEVEL_BLOCK_STATE", "net/minecraft/src/C_1596_", "m_8055_",
                        "(Lnet/minecraft/src/C_4675_;)Lnet/minecraft/src/C_2064_;",
                        intermediaryBridge, sourceMappings, sourceClasses, targetMappings, targetClasses));

        Files.createDirectories(report.toAbsolutePath().getParent());
        Files.writeString(report, formatReport(results, sourceClasses, targetClasses,
                sourceMappings, targetMappings), StandardCharsets.UTF_8);

        for (MethodAuditResult result : results) {
            System.out.println("CASE=" + result.caseId() + " STATUS=" + result.status());
            System.out.println("  SOURCE_INVOCATION_OWNER=" + result.sourceInvocationOfficialOwner());
            if (result.ownerAwareResolution() != null) {
                System.out.println("  OWNER_AWARE_STATUS=" + result.ownerAwareResolution().status());
                result.ownerAwareResolution().candidates().forEach(candidate -> {
                    System.out.println("  MAPPING_OWNER=" + candidate.mappingOfficialOwner());
                    System.out.println("  SOURCE_INTERMEDIARY_ID="
                            + candidate.mappingCandidate().sourceIntermediaryId());
                });
            }
            System.out.println("  TARGET_INVOCATION_OWNER=" + result.targetInvocationOfficialOwner());
            System.out.println("  TARGET_MAPPING_OWNER=" + result.targetMappingOfficialOwner());
            System.out.println("  DETAIL=" + result.detail());
        }
        System.out.println("REPORT=" + report.toAbsolutePath());
        System.out.println("ALL_CASES_TARGET_VERIFIED=" + results.stream().allMatch(result ->
                result.status() == AuditStatus.TARGET_VERIFIED
                        || result.status() == AuditStatus.TARGET_VERIFIED_WITH_EXTERNAL_TYPES));
        System.out.println("SELECTOR_TRANSFORMATION_PERFORMED=false");
        System.out.println("RUNTIME_MAPPING_TABLE_PRODUCED=false");
    }

    private static String formatReport(
            List<MethodAuditResult> results,
            Forge1201SourceRuntimeMemberVerifier.Index sourceClasses,
            Forge1201SourceRuntimeMemberVerifier.Index targetClasses,
            Forge1201SourceOfficialMappingIndex sourceMappings,
            Forge1201TargetOfficialMappingIndex targetMappings) {
        StringBuilder report = new StringBuilder();
        report.append("FORGE 1.20.1 -> NEOFORGE 1.21.1 HIERARCHY-AWARE INTERMEDIARY AUDIT\n");
        report.append("MODE=READ_ONLY_MAPPING_AND_CLASSFILE_INSPECTION\n");
        report.append("SOURCE_CLASSPATH_SHA256=")
                .append(sourceClasses.classpathSummary().classpathListSha256()).append('\n');
        report.append("SOURCE_CLASSPATH_ENTRIES=")
                .append(sourceClasses.classpathSummary().classpathEntryCount()).append('\n');
        report.append("SOURCE_INDEXED_CLASSES=")
                .append(sourceClasses.classpathSummary().indexedClassCount()).append('\n');
        report.append("TARGET_CLASSPATH_SHA256=")
                .append(targetClasses.classpathSummary().classpathListSha256()).append('\n');
        report.append("TARGET_CLASSPATH_ENTRIES=")
                .append(targetClasses.classpathSummary().classpathEntryCount()).append('\n');
        report.append("TARGET_INDEXED_CLASSES=")
                .append(targetClasses.classpathSummary().indexedClassCount()).append('\n');
        report.append("SOURCE_MOJANG_MAPPING_SHA256=")
                .append(sourceMappings.summary().mojangMappingSha256()).append('\n');
        report.append("TARGET_OFFICIAL_MAPPING_SHA256=")
                .append(targetMappings.summary().sha256()).append('\n');
        report.append("CASES=").append(results.size()).append('\n');
        report.append("ALL_CASES_TARGET_VERIFIED=").append(results.stream().allMatch(result ->
                result.status() == AuditStatus.TARGET_VERIFIED
                        || result.status() == AuditStatus.TARGET_VERIFIED_WITH_EXTERNAL_TYPES)).append('\n');
        report.append("SELECTOR_TRANSFORMATION_PERFORMED=false\n");
        report.append("RUNTIME_MAPPING_TABLE_PRODUCED=false\n\n");

        for (MethodAuditResult result : results) {
            report.append("CASE=").append(result.caseId()).append('\n');
            report.append("  SRG_SELECTOR=").append(result.srgOwner()).append('#')
                    .append(result.srgName()).append(result.srgDescriptor()).append('\n');
            report.append("  STATUS=").append(result.status()).append('\n');
            report.append("  SOURCE_OFFICIAL_STATUS=").append(result.sourceOfficialStatus()).append('\n');
            report.append("  SOURCE_INVOCATION_OWNER=").append(result.sourceInvocationOfficialOwner()).append('\n');
            report.append("  SOURCE_METHOD=").append(result.sourceOfficialMethodName())
                    .append(result.sourceOfficialMethodDescriptor()).append('\n');
            report.append("  SOURCE_HIERARCHY_VISITED=").append(result.sourceHierarchyVisited()).append('\n');
            if (result.ownerAwareResolution() != null) {
                report.append("  OWNER_AWARE_STATUS=").append(result.ownerAwareResolution().status()).append('\n');
                report.append("  DIRECT_INTERMEDIARY_STATUS=")
                        .append(result.ownerAwareResolution().directStatus()).append('\n');
                for (var candidate : result.ownerAwareResolution().candidates()) {
                    report.append("  INVOCATION_SRG_OWNER=").append(candidate.invocationSrgOwner()).append('\n');
                    report.append("  INVOCATION_OFFICIAL_OWNER=").append(candidate.invocationOfficialOwner()).append('\n');
                    report.append("  MAPPING_SRG_OWNER=").append(candidate.mappingSrgOwner()).append('\n');
                    report.append("  MAPPING_OFFICIAL_OWNER=").append(candidate.mappingOfficialOwner()).append('\n');
                    report.append("  MAPPING_OWNER_KIND=").append(candidate.mappingOwnerKind()).append('\n');
                    report.append("  SOURCE_INTERMEDIARY_METHOD_ID=")
                            .append(candidate.mappingCandidate().sourceIntermediaryId()).append('\n');
                    report.append("  TARGET_INTERMEDIARY_OWNER=")
                            .append(candidate.mappingCandidate().targetIntermediaryOwner()).append('\n');
                    report.append("  TARGET_OFFICIAL_OBFUSCATED_OWNER=")
                            .append(candidate.mappingCandidate().targetOfficialObfuscatedOwner()).append('\n');
                }
            }
            report.append("  TARGET_INVOCATION_OWNER=").append(result.targetInvocationOfficialOwner()).append('\n');
            report.append("  TARGET_MAPPING_OWNER=").append(result.targetMappingOfficialOwner()).append('\n');
            report.append("  TARGET_METHOD=").append(result.targetOfficialMethodName())
                    .append(result.targetOfficialMethodDescriptor()).append('\n');
            report.append("  TARGET_HIERARCHY_VISITED=").append(result.targetHierarchyVisited()).append('\n');
            report.append("  DETAIL=").append(result.detail()).append("\n\n");
        }
        return report.toString();
    }
}
