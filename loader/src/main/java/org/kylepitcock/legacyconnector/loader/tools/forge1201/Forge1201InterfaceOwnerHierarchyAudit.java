package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.objectweb.asm.Opcodes;

/**
 * Read-only classfile verification of three Forge 1.20.1 -> NeoForge 1.21.1
 * interface-owner mapping gaps isolated by the Symbiote bridge audit.
 *
 * <p>The Intermediary/SRG identities embedded in each case are evidence captured
 * by the pinned mapping report. This audit independently verifies that the
 * candidate interface is reachable from the concrete invocation owner in both
 * actual runtime classpaths, that the interface declares the exact Mojmap
 * member, and that the invocation owner resolves the same JVM signature.</p>
 *
 * <p>No class is defined or initialized, no selectors are rewritten, and no
 * runtime mapping table is emitted. The task that invokes this class validates
 * the exact Forge source-runtime manifest before passing the ordered classpaths.</p>
 */
public final class Forge1201InterfaceOwnerHierarchyAudit {
    private static final String UUID_DESCRIPTOR = "()Ljava/util/UUID;";
    private static final String AABB_DESCRIPTOR = "()Lnet/minecraft/world/phys/AABB;";
    private static final String BLOCK_STATE_DESCRIPTOR =
            "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;";

    private static final List<String> LEVEL_OUT_OF_HIERARCHY_CANDIDATES = List.of(
            "net/minecraft/client/renderer/chunk/RenderChunkRegion",
            "net/minecraft/server/level/WorldGenRegion",
            "net/minecraft/world/level/chunk/EmptyLevelChunk",
            "net/minecraft/world/level/chunk/ImposterProtoChunk",
            "net/minecraft/world/level/chunk/LevelChunk",
            "net/minecraft/world/level/chunk/ProtoChunk",
            "net/minecraft/world/level/EmptyBlockGetter",
            "net/minecraft/world/level/PathNavigationRegion");

    private static final List<CaseSpec> CASES = List.of(
            new CaseSpec(
                    "ENTITY_UUID",
                    "net/minecraft/world/entity/Entity",
                    "net/minecraft/world/level/entity/EntityAccess",
                    "m_20148_",
                    "getUUID",
                    UUID_DESCRIPTOR,
                    "net/minecraft/class_1297",
                    "net/minecraft/class_5568",
                    "method_5667",
                    "()Ljava/util/UUID;",
                    List.of()),
            new CaseSpec(
                    "ENTITY_BOUNDING_BOX",
                    "net/minecraft/world/entity/Entity",
                    "net/minecraft/world/level/entity/EntityAccess",
                    "m_20191_",
                    "getBoundingBox",
                    AABB_DESCRIPTOR,
                    "net/minecraft/class_1297",
                    "net/minecraft/class_5568",
                    "method_5829",
                    "()Lnet/minecraft/class_238;",
                    List.of()),
            new CaseSpec(
                    "LEVEL_BLOCK_STATE",
                    "net/minecraft/world/level/Level",
                    "net/minecraft/world/level/BlockGetter",
                    "m_8055_",
                    "getBlockState",
                    BLOCK_STATE_DESCRIPTOR,
                    "net/minecraft/class_1937",
                    "net/minecraft/class_1922",
                    "method_8320",
                    "(Lnet/minecraft/class_2338;)Lnet/minecraft/class_2680;",
                    LEVEL_OUT_OF_HIERARCHY_CANDIDATES));

    private Forge1201InterfaceOwnerHierarchyAudit() {}

    private record CaseSpec(
            String id,
            String invocationOwner,
            String mappingInterfaceOwner,
            String srgName,
            String officialName,
            String officialDescriptor,
            String invocationIntermediaryOwner,
            String mappingIntermediaryOwner,
            String intermediaryMethodId,
            String intermediaryDescriptor,
            List<String> outOfHierarchyCandidates) {
        private CaseSpec {
            outOfHierarchyCandidates = List.copyOf(outOfHierarchyCandidates);
        }
    }

    private record HierarchyResult(
            boolean complete,
            List<String> visited,
            List<String> unresolved) {
        private HierarchyResult {
            visited = List.copyOf(visited);
            unresolved = List.copyOf(unresolved);
        }
    }

    private record SideResult(
            String status,
            boolean invocationClassFound,
            boolean mappingOwnerClassFound,
            boolean hierarchyComplete,
            boolean mappingOwnerInHierarchy,
            boolean invocationOwnerDeclaresExact,
            boolean mappingOwnerDeclaresExact,
            boolean invocationMethodPublicInstance,
            boolean mappingOwnerMethodPublicInstance,
            String invocationLookupStatus,
            String invocationDeclaringOwner,
            String mappingOwnerLookupStatus,
            String mappingOwnerDeclaringOwner,
            List<String> hierarchyVisited,
            List<String> unresolvedHierarchyClasses,
            List<String> outOfHierarchyCandidatesFoundInHierarchy) {
        private SideResult {
            hierarchyVisited = List.copyOf(hierarchyVisited);
            unresolvedHierarchyClasses = List.copyOf(unresolvedHierarchyClasses);
            outOfHierarchyCandidatesFoundInHierarchy =
                    List.copyOf(outOfHierarchyCandidatesFoundInHierarchy);
        }
    }

    private record CaseResult(CaseSpec spec, SideResult source, SideResult target, String status) {}

    public record Result(
            Forge1201SourceRuntimeMemberVerifier.ClasspathSummary sourceClasspath,
            Forge1201SourceRuntimeMemberVerifier.ClasspathSummary targetClasspath,
            List<String> cases,
            boolean selectorTransformationPerformed,
            boolean runtimeMappingTableProduced) {
        public Result {
            cases = List.copyOf(cases);
        }
    }

    static Result analyze(
            Forge1201SourceRuntimeMemberVerifier.Index source,
            Forge1201SourceRuntimeMemberVerifier.Index target) throws IOException {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Both ordered source and target runtime indexes are required");
        }
        if (source.classpathSummary() == null || target.classpathSummary() == null) {
            throw new IllegalArgumentException("Both runtime indexes must come from readClasspath()");
        }

        List<String> caseReports = new ArrayList<>();
        for (CaseSpec spec : CASES) {
            SideResult sourceResult = inspectSide(source, spec);
            SideResult targetResult = inspectSide(target, spec);
            String status = "VERIFIED";
            if (!"VERIFIED".equals(sourceResult.status())) {
                status = "SOURCE_" + sourceResult.status();
            } else if (!"VERIFIED".equals(targetResult.status())) {
                status = "TARGET_" + targetResult.status();
            }
            caseReports.add(formatCase(spec, sourceResult, targetResult, status));
        }

        return new Result(
                source.classpathSummary(),
                target.classpathSummary(),
                caseReports,
                false,
                false);
    }

    private static SideResult inspectSide(
            Forge1201SourceRuntimeMemberVerifier.Index index,
            CaseSpec spec) throws IOException {
        String invocationOwner = spec.invocationOwner();
        String mappingOwner = spec.mappingInterfaceOwner();
        String name = spec.officialName();
        String descriptor = spec.officialDescriptor();
        Forge1201TargetApiVerifier.MemberSignature signature =
                new Forge1201TargetApiVerifier.MemberSignature(name, descriptor);

        Optional<Forge1201TargetApiVerifier.TargetClassInfo> invocationInfo = index.classInfo(invocationOwner);
        Optional<Forge1201TargetApiVerifier.TargetClassInfo> mappingInfo = index.classInfo(mappingOwner);
        HierarchyResult hierarchy = walkHierarchy(index, invocationOwner);

        boolean invocationDeclares = invocationInfo
                .map(info -> info.methods().contains(signature)).orElse(false);
        boolean mappingDeclares = mappingInfo
                .map(info -> info.methods().contains(signature)).orElse(false);
        boolean mappingInHierarchy = hierarchy.visited().contains(mappingOwner);

        Forge1201SourceRuntimeMemberVerifier.MemberResult invocationLookup =
                invocationInfo.isPresent() ? index.inspectMethod(invocationOwner, name, descriptor) : null;
        Forge1201SourceRuntimeMemberVerifier.MemberResult mappingLookup =
                mappingInfo.isPresent() ? index.inspectMethod(mappingOwner, name, descriptor) : null;
        Optional<Integer> invocationAccess = invocationLookup == null
                || invocationLookup.declaringOwner() == null
                ? Optional.empty()
                : index.declaredMethodAccess(invocationLookup.declaringOwner(), name, descriptor);
        Optional<Integer> mappingAccess = mappingInfo.isPresent()
                ? index.declaredMethodAccess(mappingOwner, name, descriptor)
                : Optional.empty();
        boolean invocationPublicInstance = isPublicInstanceMethod(invocationAccess);
        boolean mappingPublicInstance = isPublicInstanceMethod(mappingAccess);

        List<String> falseCandidates = spec.outOfHierarchyCandidates().stream()
                .filter(hierarchy.visited()::contains)
                .toList();

        String status;
        if (invocationInfo.isEmpty() || mappingInfo.isEmpty()) {
            status = "CLASS_MISSING";
        } else if (!hierarchy.complete()) {
            status = "HIERARCHY_INCOMPLETE";
        } else if (!mappingInHierarchy) {
            status = "MAPPING_OWNER_NOT_IN_HIERARCHY";
        } else if (!mappingDeclares
                || mappingLookup.status()
                        != Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_DECLARED_EXACT) {
            status = "MAPPING_OWNER_MEMBER_NOT_DECLARED_EXACT";
        } else if (!mappingPublicInstance) {
            status = "MAPPING_OWNER_METHOD_NOT_PUBLIC_INSTANCE";
        } else if (invocationLookup.status()
                        != Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_DECLARED_EXACT
                && invocationLookup.status()
                        != Forge1201SourceRuntimeMemberVerifier.Status.SOURCE_RUNTIME_MEMBER_HIERARCHY_EXACT) {
            status = "INVOCATION_MEMBER_NOT_RESOLVABLE";
        } else if (!invocationPublicInstance) {
            status = "INVOCATION_METHOD_NOT_PUBLIC_INSTANCE";
        } else if (!falseCandidates.isEmpty()) {
            status = "OUT_OF_HIERARCHY_CANDIDATE_ENTERED_HIERARCHY";
        } else {
            status = "VERIFIED";
        }

        return new SideResult(
                status,
                invocationInfo.isPresent(),
                mappingInfo.isPresent(),
                hierarchy.complete(),
                mappingInHierarchy,
                invocationDeclares,
                mappingDeclares,
                invocationPublicInstance,
                mappingPublicInstance,
                invocationLookup == null ? "NOT_CHECKED" : invocationLookup.status().name(),
                invocationLookup == null ? null : invocationLookup.declaringOwner(),
                mappingLookup == null ? "NOT_CHECKED" : mappingLookup.status().name(),
                mappingLookup == null ? null : mappingLookup.declaringOwner(),
                hierarchy.visited(),
                hierarchy.unresolved(),
                falseCandidates);
    }

    private static boolean isPublicInstanceMethod(Optional<Integer> access) {
        if (access.isEmpty()) return false;
        int flags = access.get();
        return (flags & Opcodes.ACC_PUBLIC) != 0
                && (flags & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0;
    }

    private static HierarchyResult walkHierarchy(
            Forge1201SourceRuntimeMemberVerifier.Index index,
            String startOwner) throws IOException {
        ArrayDeque<String> pending = new ArrayDeque<>();
        Set<String> scheduled = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        Set<String> unresolved = new LinkedHashSet<>();
        pending.add(startOwner);
        scheduled.add(startOwner);

        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            Optional<Forge1201TargetApiVerifier.TargetClassInfo> info = index.classInfo(current);
            if (info.isEmpty()) {
                unresolved.add(current);
                continue;
            }
            visited.add(current);

            for (String implementedInterface : info.get().interfaces()) {
                if (scheduled.add(implementedInterface)) pending.addLast(implementedInterface);
            }
            String parent = info.get().superName();
            if (parent != null && scheduled.add(parent)) pending.addLast(parent);
        }
        return new HierarchyResult(unresolved.isEmpty(), new ArrayList<>(visited), new ArrayList<>(unresolved));
    }

    private static String formatCase(
            CaseSpec spec, SideResult source, SideResult target, String status) {
        StringBuilder text = new StringBuilder(2048);
        text.append("CASE=").append(spec.id()).append('\n');
        text.append("  SOURCE_SRG_NAME=").append(spec.srgName()).append('\n');
        text.append("  SOURCE_INVOCATION_OWNER=").append(spec.invocationOwner()).append('\n');
        text.append("  SOURCE_MAPPING_INTERFACE_OWNER=").append(spec.mappingInterfaceOwner()).append('\n');
        text.append("  SOURCE_INTERMEDIARY_INVOCATION_OWNER=").append(spec.invocationIntermediaryOwner()).append('\n');
        text.append("  SOURCE_INTERMEDIARY_MAPPING_OWNER=").append(spec.mappingIntermediaryOwner()).append('\n');
        text.append("  SOURCE_INTERMEDIARY_METHOD_ID=").append(spec.intermediaryMethodId()).append('\n');
        text.append("  SOURCE_INTERMEDIARY_DESCRIPTOR=").append(spec.intermediaryDescriptor()).append('\n');
        text.append("  MOJMAP_MEMBER=").append(spec.officialName()).append(spec.officialDescriptor()).append('\n');
        text.append("  SOURCE_STATUS=").append(source.status()).append('\n');
        appendSide(text, "SOURCE", source);
        text.append("  TARGET_STATUS=").append(target.status()).append('\n');
        appendSide(text, "TARGET", target);
        text.append("  CASE_STATUS=").append(status).append('\n');
        if (!spec.outOfHierarchyCandidates().isEmpty()) {
            text.append("  EXPECTED_OUT_OF_HIERARCHY_ALTERNATES=")
                    .append(spec.outOfHierarchyCandidates()).append('\n');
        }
        text.append('\n');
        return text.toString();
    }

    private static void appendSide(StringBuilder text, String label, SideResult result) {
        text.append("    INVOCATION_CLASS_FOUND=").append(result.invocationClassFound()).append('\n');
        text.append("    MAPPING_OWNER_CLASS_FOUND=").append(result.mappingOwnerClassFound()).append('\n');
        text.append("    HIERARCHY_COMPLETE=").append(result.hierarchyComplete()).append('\n');
        text.append("    MAPPING_OWNER_IN_HIERARCHY=").append(result.mappingOwnerInHierarchy()).append('\n');
        text.append("    INVOCATION_OWNER_DECLARES_EXACT=").append(result.invocationOwnerDeclaresExact()).append('\n');
        text.append("    MAPPING_OWNER_DECLARES_EXACT=").append(result.mappingOwnerDeclaresExact()).append('\n');
        text.append("    INVOCATION_METHOD_PUBLIC_INSTANCE=").append(result.invocationMethodPublicInstance()).append('\n');
        text.append("    MAPPING_OWNER_METHOD_PUBLIC_INSTANCE=").append(result.mappingOwnerMethodPublicInstance()).append('\n');
        text.append("    INVOCATION_LOOKUP=").append(result.invocationLookupStatus())
                .append(" DECLARING_OWNER=").append(result.invocationDeclaringOwner()).append('\n');
        text.append("    MAPPING_OWNER_LOOKUP=").append(result.mappingOwnerLookupStatus())
                .append(" DECLARING_OWNER=").append(result.mappingOwnerDeclaringOwner()).append('\n');
        text.append("    HIERARCHY_VISITED=").append(result.hierarchyVisited()).append('\n');
        text.append("    UNRESOLVED_HIERARCHY_CLASSES=").append(result.unresolvedHierarchyClasses()).append('\n');
        text.append("    FALSE_ALTERNATES_IN_HIERARCHY=")
                .append(result.outOfHierarchyCandidatesFoundInHierarchy()).append('\n');
    }

    private static String report(Result result) {
        StringBuilder text = new StringBuilder(8192);
        text.append("FORGE 1.20.1 -> NEOFORGE 1.21.1 INTERFACE-OWNER HIERARCHY AUDIT\n");
        text.append("MODE=READ_ONLY_ASM_CLASSFILE_INSPECTION\n");
        text.append("SOURCE_CLASSPATH_LIST=").append(result.sourceClasspath().classpathList()).append('\n');
        text.append("SOURCE_CLASSPATH_LIST_SHA256=").append(result.sourceClasspath().classpathListSha256()).append('\n');
        text.append("SOURCE_CLASSPATH_ENTRIES=").append(result.sourceClasspath().classpathEntryCount()).append('\n');
        text.append("SOURCE_INDEXED_CLASSES=").append(result.sourceClasspath().indexedClassCount()).append('\n');
        text.append("SOURCE_DUPLICATE_CLASS_NAMES=").append(result.sourceClasspath().duplicateClassNames()).append('\n');
        text.append("TARGET_CLASSPATH_LIST=").append(result.targetClasspath().classpathList()).append('\n');
        text.append("TARGET_CLASSPATH_LIST_SHA256=").append(result.targetClasspath().classpathListSha256()).append('\n');
        text.append("TARGET_CLASSPATH_ENTRIES=").append(result.targetClasspath().classpathEntryCount()).append('\n');
        text.append("TARGET_INDEXED_CLASSES=").append(result.targetClasspath().indexedClassCount()).append('\n');
        text.append("TARGET_DUPLICATE_CLASS_NAMES=").append(result.targetClasspath().duplicateClassNames()).append('\n');
        text.append("MAPPING_EVIDENCE_TARGET_BRIDGE_JSON_SHA256=")
                .append("74F1C6E443F9B4F411C741B985D58561C47CF45DFE02DB6EA128EAF98D02621E").append('\n');
        text.append("MAPPING_EVIDENCE_RECONCILIATION_JSON_SHA256=")
                .append("09E61501B9DA3B0C6B3CBCA272BA2593DFCA0A6164D5F679F64E335E1DA4A1B7").append('\n');
        text.append("CASES=").append(result.cases().size()).append('\n');
        text.append("SELECTOR_TRANSFORMATION_PERFORMED=").append(result.selectorTransformationPerformed()).append('\n');
        text.append("RUNTIME_MAPPING_TABLE_PRODUCED=").append(result.runtimeMappingTableProduced()).append("\n\n");
        result.cases().forEach(text::append);
        text.append("INTERPRETATION_LIMITS\n");
        text.append("  This report checks actual classfile hierarchy and exact declared/resolved JVM signatures only.\n");
        text.append("  Embedded Intermediary identities come from the separately pinned mapping bridge evidence above.\n");
        text.append("  A verified hierarchy candidate does not by itself authorize a production mapping fallback.\n");
        text.append("  No selector transformation, runtime mapping table, class definition, or class initialization occurred.\n");
        return text.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: Forge1201InterfaceOwnerHierarchyAudit <source-classpath.txt> "
                            + "<target-classpath.txt> <report.txt>");
        }
        Forge1201SourceRuntimeMemberVerifier.Index source =
                Forge1201SourceRuntimeMemberVerifier.readClasspath(Path.of(args[0]));
        Forge1201SourceRuntimeMemberVerifier.Index target =
                Forge1201SourceRuntimeMemberVerifier.readClasspath(Path.of(args[1]));
        Result result = analyze(source, target);
        Path output = Path.of(args[2]).toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(output, report(result), StandardCharsets.UTF_8);
        System.out.println("SOURCE_CLASSPATH_LIST_SHA256=" + result.sourceClasspath().classpathListSha256());
        System.out.println("TARGET_CLASSPATH_LIST_SHA256=" + result.targetClasspath().classpathListSha256());
        System.out.println("CASES=" + result.cases().size());
        for (String item : result.cases()) {
            String firstLine = item.lines().findFirst().orElse("CASE=UNKNOWN");
            String caseStatus = item.lines()
                    .filter(line -> line.trim().startsWith("CASE_STATUS="))
                    .findFirst().orElse("CASE_STATUS=UNKNOWN").trim();
            System.out.println(firstLine);
            System.out.println("  " + caseStatus);
        }
        System.out.println("REPORT=" + output);
        System.out.println("INTERFACE_OWNER_HIERARCHY_AUDIT=COMPLETE");
    }
}
