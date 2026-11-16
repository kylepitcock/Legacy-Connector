package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Read-only comparison of the effective finalizeSpawn instance-method implementation
 * across the pinned Forge 1.20.1 and NeoForge 1.21.1 runtime classpaths.
 *
 * <p>Class bytes are parsed by the existing ASM TargetIndex via
 * Forge1201SourceRuntimeMemberVerifier.Index. Classes are never defined, initialized,
 * or executed. The two runtimes are scanned independently; only literal Mojang-official
 * internal class-name equality is used to line up owner candidates.</p>
 */
public final class Forge1201MobFinalizeSpawnEvolutionAudit {
    static final String MOB_OWNER = "net/minecraft/world/entity/Mob";
    static final String METHOD_NAME = "finalizeSpawn";
    static final String SOURCE_BASE_DESCRIPTOR =
            "(Lnet/minecraft/world/level/ServerLevelAccessor;"
                    + "Lnet/minecraft/world/DifficultyInstance;"
                    + "Lnet/minecraft/world/entity/MobSpawnType;"
                    + "Lnet/minecraft/world/entity/SpawnGroupData;"
                    + "Lnet/minecraft/nbt/CompoundTag;)"
                    + "Lnet/minecraft/world/entity/SpawnGroupData;";
    static final String TARGET_BASE_DESCRIPTOR =
            "(Lnet/minecraft/world/level/ServerLevelAccessor;"
                    + "Lnet/minecraft/world/DifficultyInstance;"
                    + "Lnet/minecraft/world/entity/MobSpawnType;"
                    + "Lnet/minecraft/world/entity/SpawnGroupData;)"
                    + "Lnet/minecraft/world/entity/SpawnGroupData;";

    public enum DispatchStatus {
        FOUND,
        MEMBER_ABSENT,
        HIERARCHY_INCOMPLETE
    }

    public record MethodDeclaration(String descriptor, int accessFlags, boolean virtualInstanceMethod) {}

    /**
     * Counts local-load instructions for the source CompoundTag parameter slot.
     * A positive count is bytecode evidence only, not proof of data-flow into behavior.
     */
    public record ParameterSlotEvidence(
            String declaringOwner,
            int parameterIndex,
            int localSlot,
            boolean methodCodePresent,
            int loadInstructionCount) {
        public String status() {
            if (!methodCodePresent) return "NO_METHOD_CODE";
            return loadInstructionCount > 0
                    ? "LOCAL_SLOT_LOAD_INSTRUCTIONS_FOUND"
                    : "NO_LOCAL_SLOT_LOAD_INSTRUCTIONS";
        }
    }

    public record OwnerComparison(
            String owner,
            String classification,
            boolean sourceClassPresent,
            boolean targetClassPresent,
            boolean sourceMobSubclass,
            boolean targetMobSubclass,
            String sourceSuperName,
            String targetSuperName,
            List<String> sourceHierarchyVisited,
            List<String> targetHierarchyVisited,
            List<MethodDeclaration> sourceDeclaredFinalizeSpawnMethods,
            List<MethodDeclaration> targetDeclaredFinalizeSpawnMethods,
            DispatchStatus sourceDispatchStatus,
            String sourceDispatchDeclaringOwner,
            DispatchStatus targetDispatchStatus,
            String targetDispatchDeclaringOwner,
            ParameterSlotEvidence sourceTrailingCompoundTagEvidence) {
        public OwnerComparison {
            sourceHierarchyVisited = List.copyOf(sourceHierarchyVisited);
            targetHierarchyVisited = List.copyOf(targetHierarchyVisited);
            sourceDeclaredFinalizeSpawnMethods = List.copyOf(sourceDeclaredFinalizeSpawnMethods);
            targetDeclaredFinalizeSpawnMethods = List.copyOf(targetDeclaredFinalizeSpawnMethods);
        }
    }

    public record Result(
            Forge1201SourceRuntimeMemberVerifier.ClasspathSummary sourceClasspath,
            Forge1201SourceRuntimeMemberVerifier.ClasspathSummary targetClasspath,
            int sourceMinecraftClassCount,
            int targetMinecraftClassCount,
            int sourceMobSubclassCount,
            int targetMobSubclassCount,
            int sourceEffectiveSubclassOverrideCount,
            int targetEffectiveSubclassOverrideCount,
            List<OwnerComparison> owners,
            List<String> sourceIncompleteEntityHierarchies,
            List<String> targetIncompleteEntityHierarchies,
            boolean selectorTransformationPerformed,
            boolean runtimeMappingTableProduced) {
        public Result {
            owners = List.copyOf(owners);
            sourceIncompleteEntityHierarchies = List.copyOf(sourceIncompleteEntityHierarchies);
            targetIncompleteEntityHierarchies = List.copyOf(targetIncompleteEntityHierarchies);
        }
    }

    private record HierarchyResult(boolean mobSubclass, boolean complete, List<String> visited) {
        private HierarchyResult {
            visited = List.copyOf(visited);
        }
    }

    private record DispatchResult(DispatchStatus status, String declaringOwner) {}

    private record RuntimeMobClass(
            String owner,
            String superName,
            List<String> hierarchyVisited,
            List<MethodDeclaration> declarations,
            DispatchResult baseDispatch,
            ParameterSlotEvidence trailingCompoundTagEvidence) {
        private RuntimeMobClass {
            hierarchyVisited = List.copyOf(hierarchyVisited);
            declarations = List.copyOf(declarations);
        }
    }

    private static final class RuntimeView {
        private final Forge1201SourceRuntimeMemberVerifier.Index index;
        private final Forge1201SourceRuntimeMemberVerifier.ClasspathSummary classpath;
        private final Map<String, Forge1201TargetApiVerifier.TargetClassInfo> minecraftClasses;
        private final Map<String, HierarchyResult> hierarchies;
        private final Map<String, RuntimeMobClass> mobClasses;
        private final List<String> incompleteEntityHierarchies;

        private RuntimeView(
                Forge1201SourceRuntimeMemberVerifier.Index index,
                Forge1201SourceRuntimeMemberVerifier.ClasspathSummary classpath,
                Map<String, Forge1201TargetApiVerifier.TargetClassInfo> minecraftClasses,
                Map<String, HierarchyResult> hierarchies,
                Map<String, RuntimeMobClass> mobClasses,
                List<String> incompleteEntityHierarchies) {
            this.index = index;
            this.classpath = classpath;
            this.minecraftClasses = Map.copyOf(minecraftClasses);
            this.hierarchies = Map.copyOf(hierarchies);
            this.mobClasses = Map.copyOf(mobClasses);
            this.incompleteEntityHierarchies = List.copyOf(incompleteEntityHierarchies);
        }
    }

    private Forge1201MobFinalizeSpawnEvolutionAudit() {}

    static Result analyze(
            Forge1201SourceRuntimeMemberVerifier.Index sourceIndex,
            Forge1201SourceRuntimeMemberVerifier.Index targetIndex) throws IOException {
        if (sourceIndex == null || targetIndex == null) {
            throw new IllegalArgumentException("Both source and target ordered classpath indexes are required");
        }

        RuntimeView source = inspectRuntime(sourceIndex, SOURCE_BASE_DESCRIPTOR);
        RuntimeView target = inspectRuntime(targetIndex, TARGET_BASE_DESCRIPTOR);

        if (!source.mobClasses.containsKey(MOB_OWNER) || !target.mobClasses.containsKey(MOB_OWNER)) {
            throw new IOException("The pinned source and target classpaths must both contain net.minecraft.world.entity.Mob");
        }

        Set<String> owners = new LinkedHashSet<>();
        owners.addAll(source.mobClasses.keySet());
        owners.addAll(target.mobClasses.keySet());

        List<String> orderedOwners = owners.stream().sorted().toList();
        List<OwnerComparison> comparisons = new ArrayList<>();
        for (String owner : orderedOwners) {
            comparisons.add(compareOwner(source, target, owner));
        }

        int sourceOverrides = (int) source.mobClasses.values().stream()
                .filter(value -> !value.owner().equals(MOB_OWNER))
                .filter(value -> value.baseDispatch().status() == DispatchStatus.FOUND)
                .map(value -> value.baseDispatch().declaringOwner())
                .filter(owner -> !MOB_OWNER.equals(owner))
                .distinct()
                .count();
        int targetOverrides = (int) target.mobClasses.values().stream()
                .filter(value -> !value.owner().equals(MOB_OWNER))
                .filter(value -> value.baseDispatch().status() == DispatchStatus.FOUND)
                .map(value -> value.baseDispatch().declaringOwner())
                .filter(owner -> !MOB_OWNER.equals(owner))
                .distinct()
                .count();

        return new Result(
                source.classpath,
                target.classpath,
                source.minecraftClasses.size(),
                target.minecraftClasses.size(),
                Math.max(0, source.mobClasses.size() - 1),
                Math.max(0, target.mobClasses.size() - 1),
                sourceOverrides,
                targetOverrides,
                comparisons,
                source.incompleteEntityHierarchies,
                target.incompleteEntityHierarchies,
                false,
                false);
    }

    private static RuntimeView inspectRuntime(
            Forge1201SourceRuntimeMemberVerifier.Index index,
            String baseDescriptor) throws IOException {
        List<String> classNames = index.indexedClassNames("net/minecraft/");
        if (classNames.isEmpty()) throw new IOException("Runtime classpath has no indexed net/minecraft classes");

        Map<String, Forge1201TargetApiVerifier.TargetClassInfo> classes = new HashMap<>();
        for (String name : classNames) {
            Optional<Forge1201TargetApiVerifier.TargetClassInfo> info = index.classInfo(name);
            if (info.isEmpty()) throw new IOException("Indexed class disappeared while auditing: " + name);
            classes.put(name, info.get());
        }

        Map<String, Optional<Forge1201TargetApiVerifier.TargetClassInfo>> infoCache = new HashMap<>();
        classes.forEach((name, info) -> infoCache.put(name, Optional.of(info)));

        Map<String, HierarchyResult> hierarchies = new HashMap<>();
        Map<String, RuntimeMobClass> mobClasses = new HashMap<>();
        List<String> incomplete = new ArrayList<>();

        for (String owner : classNames) {
            HierarchyResult hierarchy = inspectMobHierarchy(index, owner, infoCache);
            hierarchies.put(owner, hierarchy);
            if (hierarchy.mobSubclass()) {
                Forge1201TargetApiVerifier.TargetClassInfo info = classes.get(owner);
                List<MethodDeclaration> declarations =
                        declaredFinalizeSpawnMethods(index, info);
                DispatchResult dispatch = resolveBaseDispatch(
                        index, hierarchy, baseDescriptor, infoCache);
                ParameterSlotEvidence parameterEvidence = null;
                if (baseDescriptor.equals(SOURCE_BASE_DESCRIPTOR)
                        && dispatch.status() == DispatchStatus.FOUND) {
                    parameterEvidence = inspectTrailingCompoundTagParameter(index, dispatch.declaringOwner());
                }
                mobClasses.put(owner, new RuntimeMobClass(
                        owner, info.superName(), hierarchy.visited(), declarations, dispatch, parameterEvidence));
            } else if (!hierarchy.complete() && owner.startsWith("net/minecraft/world/entity/")) {
                incomplete.add(owner + " -> " + String.join(" -> ", hierarchy.visited()));
            }
        }

        incomplete.sort(String::compareTo);
        return new RuntimeView(index, index.classpathSummary(), classes, hierarchies, mobClasses, incomplete);
    }

    private static HierarchyResult inspectMobHierarchy(
            Forge1201SourceRuntimeMemberVerifier.Index index,
            String owner,
            Map<String, Optional<Forge1201TargetApiVerifier.TargetClassInfo>> infoCache) throws IOException {
        List<String> visited = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String current = owner;
        while (current != null) {
            if (!seen.add(current)) return new HierarchyResult(false, false, visited);
            visited.add(current);
            if (current.equals(MOB_OWNER)) return new HierarchyResult(true, true, visited);

            Optional<Forge1201TargetApiVerifier.TargetClassInfo> info =
                    classInfo(index, current, infoCache);
            if (info.isEmpty()) return new HierarchyResult(false, false, visited);
            current = info.get().superName();
        }
        return new HierarchyResult(false, true, visited);
    }

    private static DispatchResult resolveBaseDispatch(
            Forge1201SourceRuntimeMemberVerifier.Index index,
            HierarchyResult hierarchy,
            String baseDescriptor,
            Map<String, Optional<Forge1201TargetApiVerifier.TargetClassInfo>> infoCache) throws IOException {
        Forge1201TargetApiVerifier.MemberSignature expected =
                new Forge1201TargetApiVerifier.MemberSignature(METHOD_NAME, baseDescriptor);
        for (String owner : hierarchy.visited()) {
            Optional<Forge1201TargetApiVerifier.TargetClassInfo> info =
                    classInfo(index, owner, infoCache);
            if (info.isEmpty()) return new DispatchResult(DispatchStatus.HIERARCHY_INCOMPLETE, null);
            if (!info.get().methods().contains(expected)) continue;

            Optional<Integer> access = index.declaredMethodAccess(owner, METHOD_NAME, baseDescriptor);
            if (access.isEmpty()) return new DispatchResult(DispatchStatus.HIERARCHY_INCOMPLETE, null);
            if ((access.get() & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0) {
                return new DispatchResult(DispatchStatus.FOUND, owner);
            }
        }
        return new DispatchResult(
                hierarchy.complete() ? DispatchStatus.MEMBER_ABSENT : DispatchStatus.HIERARCHY_INCOMPLETE,
                null);
    }

    private static ParameterSlotEvidence inspectTrailingCompoundTagParameter(
            Forge1201SourceRuntimeMemberVerifier.Index index,
            String declaringOwner) throws IOException {
        final int parameterIndex = 4;
        Type[] arguments = Type.getArgumentTypes(SOURCE_BASE_DESCRIPTOR);
        if (arguments.length <= parameterIndex
                || arguments[parameterIndex].getSort() != Type.OBJECT
                || !"net/minecraft/nbt/CompoundTag".equals(arguments[parameterIndex].getInternalName())) {
            throw new IOException("Source finalizeSpawn descriptor no longer has CompoundTag at parameter index 4");
        }

        int localSlot = 1; // Instance-method local slot zero is this.
        for (int i = 0; i < parameterIndex; i++) {
            localSlot += arguments[i].getSize();
        }

        Optional<Forge1201TargetApiVerifier.MethodCodeSummary> method =
                index.inspectMethodCode(declaringOwner, METHOD_NAME, SOURCE_BASE_DESCRIPTOR);
        if (method.isEmpty()) {
            throw new IOException("Effective source finalizeSpawn method body is unavailable: "
                    + declaringOwner + "#" + SOURCE_BASE_DESCRIPTOR);
        }
        Forge1201TargetApiVerifier.MethodCodeSummary body = method.get();
        return new ParameterSlotEvidence(
                declaringOwner, parameterIndex, localSlot, body.codePresent(), body.loadCount(localSlot));
    }

    private static List<MethodDeclaration> declaredFinalizeSpawnMethods(
            Forge1201SourceRuntimeMemberVerifier.Index index,
            Forge1201TargetApiVerifier.TargetClassInfo info) throws IOException {
        List<MethodDeclaration> declarations = new ArrayList<>();
        for (Forge1201TargetApiVerifier.MemberSignature member : info.methods()) {
            if (!METHOD_NAME.equals(member.name())) continue;
            Optional<Integer> access = index.declaredMethodAccess(info.name(), member.name(), member.descriptor());
            if (access.isEmpty()) {
                throw new IOException("Method access flags unavailable for "
                        + info.name() + "#" + member.name() + member.descriptor());
            }
            int flags = access.get();
            boolean virtual = (flags & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0;
            declarations.add(new MethodDeclaration(member.descriptor(), flags, virtual));
        }
        declarations.sort(Comparator.comparing(MethodDeclaration::descriptor));
        return List.copyOf(declarations);
    }

    private static Optional<Forge1201TargetApiVerifier.TargetClassInfo> classInfo(
            Forge1201SourceRuntimeMemberVerifier.Index index,
            String owner,
            Map<String, Optional<Forge1201TargetApiVerifier.TargetClassInfo>> cache) throws IOException {
        Optional<Forge1201TargetApiVerifier.TargetClassInfo> info = cache.get(owner);
        if (info != null) return info;
        info = index.classInfo(owner);
        cache.put(owner, info);
        return info;
    }

    private static OwnerComparison compareOwner(RuntimeView source, RuntimeView target, String owner)
            throws IOException {
        Forge1201TargetApiVerifier.TargetClassInfo sourceInfo = source.minecraftClasses.get(owner);
        Forge1201TargetApiVerifier.TargetClassInfo targetInfo = target.minecraftClasses.get(owner);
        RuntimeMobClass sourceMob = source.mobClasses.get(owner);
        RuntimeMobClass targetMob = target.mobClasses.get(owner);

        List<MethodDeclaration> sourceDeclarations = sourceInfo == null
                ? List.of() : declaredFinalizeSpawnMethods(source.index, sourceInfo);
        List<MethodDeclaration> targetDeclarations = targetInfo == null
                ? List.of() : declaredFinalizeSpawnMethods(target.index, targetInfo);

        HierarchyResult sourceHierarchy = source.hierarchies.get(owner);
        HierarchyResult targetHierarchy = target.hierarchies.get(owner);
        List<String> sourceVisited = sourceHierarchy == null ? List.of() : sourceHierarchy.visited();
        List<String> targetVisited = targetHierarchy == null ? List.of() : targetHierarchy.visited();

        DispatchResult sourceDispatch = sourceMob == null
                ? new DispatchResult(DispatchStatus.MEMBER_ABSENT, null) : sourceMob.baseDispatch();
        DispatchResult targetDispatch = targetMob == null
                ? new DispatchResult(DispatchStatus.MEMBER_ABSENT, null) : targetMob.baseDispatch();

        String classification;
        if (owner.equals(MOB_OWNER)) {
            List<String> sourceDescriptors = sourceDeclarations.stream()
                    .map(MethodDeclaration::descriptor).sorted().toList();
            List<String> targetDescriptors = targetDeclarations.stream()
                    .map(MethodDeclaration::descriptor).sorted().toList();
            boolean expectedFound = sourceDescriptors.contains(SOURCE_BASE_DESCRIPTOR)
                    && targetDescriptors.contains(TARGET_BASE_DESCRIPTOR);
            classification = !expectedFound ? "BASE_EXPECTED_SIGNATURE_NOT_FOUND"
                    : sourceDescriptors.equals(targetDescriptors) ? "BASE_SIGNATURE_SET_EXACT"
                    : "BASE_SIGNATURE_SET_CHANGED";
        } else if (sourceMob != null && targetMob == null) {
            classification = targetInfo == null
                    ? "SOURCE_MOB_SUBCLASS_MISSING_IN_TARGET"
                    : "SOURCE_MOB_SUBCLASS_RELATION_REMOVED";
        } else if (sourceMob == null && targetMob != null) {
            classification = sourceInfo == null
                    ? "TARGET_ONLY_MOB_SUBCLASS"
                    : "TARGET_MOB_SUBCLASS_RELATION_ADDED";
        } else if (sourceMob != null) {
            if (sourceDispatch.status() != DispatchStatus.FOUND
                    || targetDispatch.status() != DispatchStatus.FOUND) {
                classification = "EFFECTIVE_DISPATCH_INCOMPLETE";
            } else {
                boolean sourceOverride = !MOB_OWNER.equals(sourceDispatch.declaringOwner());
                boolean targetOverride = !MOB_OWNER.equals(targetDispatch.declaringOwner());
                if (sourceOverride && targetOverride) {
                    classification = sourceDispatch.declaringOwner().equals(targetDispatch.declaringOwner())
                            ? "OVERRIDE_PRESENT_BOTH_SAME_DECLARING_OWNER"
                            : "OVERRIDE_PRESENT_BOTH_DIFFERENT_DECLARING_OWNERS";
                } else if (sourceOverride) {
                    classification = "SOURCE_OVERRIDE_REMOVED_TARGET_INHERITS_BASE";
                } else if (targetOverride) {
                    classification = "TARGET_OVERRIDE_ADDED_SOURCE_INHERITS_BASE";
                } else {
                    classification = "INHERITS_MOB_BASE_BOTH";
                }
            }
        } else {
            classification = "NOT_A_MOB_SUBCLASS_IN_EITHER_RUNTIME";
        }

        return new OwnerComparison(
                owner,
                classification,
                sourceInfo != null,
                targetInfo != null,
                sourceMob != null,
                targetMob != null,
                sourceInfo == null ? null : sourceInfo.superName(),
                targetInfo == null ? null : targetInfo.superName(),
                sourceVisited,
                targetVisited,
                sourceDeclarations,
                targetDeclarations,
                sourceDispatch.status(),
                sourceDispatch.declaringOwner(),
                targetDispatch.status(),
                targetDispatch.declaringOwner(),
                sourceMob == null ? null : sourceMob.trailingCompoundTagEvidence());
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: Forge1201MobFinalizeSpawnEvolutionAudit <forge-source-classpath.txt> "
                            + "<neoforge-target-classpath.txt> <report.json>");
        }

        Path sourceClasspath = Path.of(args[0]);
        Path targetClasspath = Path.of(args[1]);
        Path report = Path.of(args[2]).toAbsolutePath().normalize();

        Forge1201SourceRuntimeMemberVerifier.Index source =
                Forge1201SourceRuntimeMemberVerifier.readClasspath(sourceClasspath);
        Forge1201SourceRuntimeMemberVerifier.Index target =
                Forge1201SourceRuntimeMemberVerifier.readClasspath(targetClasspath);
        Result result = analyze(source, target);

        if (report.getParent() != null) Files.createDirectories(report.getParent());
        Files.writeString(report, toJson(result), StandardCharsets.UTF_8);

        System.out.println("Forge 1.20.1 -> NeoForge 1.21.1 Mob.finalizeSpawn override evolution audit");
        System.out.println("Read-only ASM class-file scan; no Minecraft classes were defined or executed.");
        System.out.println("Minecraft classes scanned: source=" + result.sourceMinecraftClassCount()
                + ", target=" + result.targetMinecraftClassCount());
        System.out.println("Mob subclasses: source=" + result.sourceMobSubclassCount()
                + ", target=" + result.targetMobSubclassCount());
        System.out.println("Effective subclass overrides of the source/target base descriptors: source="
                + result.sourceEffectiveSubclassOverrideCount()
                + ", target=" + result.targetEffectiveSubclassOverrideCount());
        System.out.println("Incomplete entity hierarchies: source="
                + result.sourceIncompleteEntityHierarchies().size()
                + ", target=" + result.targetIncompleteEntityHierarchies().size());

        for (OwnerComparison owner : result.owners()) {
            boolean sourceOverride = owner.sourceMobSubclass()
                    && owner.sourceDispatchStatus() == DispatchStatus.FOUND
                    && !MOB_OWNER.equals(owner.sourceDispatchDeclaringOwner());
            boolean targetOverride = owner.targetMobSubclass()
                    && owner.targetDispatchStatus() == DispatchStatus.FOUND
                    && !MOB_OWNER.equals(owner.targetDispatchDeclaringOwner());
            if (owner.owner().equals(MOB_OWNER)
                    || sourceOverride || targetOverride
                    || owner.classification().contains("REMOVED")
                    || owner.classification().contains("ADDED")
                    || owner.classification().contains("MISSING")) {
                System.out.println("  " + owner.owner() + " [" + owner.classification() + "]");
                if (sourceOverride || owner.owner().equals(MOB_OWNER)) {
                    System.out.println("    Forge dispatch declaration: " + owner.sourceDispatchDeclaringOwner()
                            + " " + owner.sourceDispatchStatus());
                    owner.sourceDeclaredFinalizeSpawnMethods().forEach(method ->
                            System.out.println("    Forge declared: " + method.descriptor()
                                    + " access=0x" + Integer.toHexString(method.accessFlags())
                                    + " virtual=" + method.virtualInstanceMethod()));
                    ParameterSlotEvidence evidence = owner.sourceTrailingCompoundTagEvidence();
                    if (evidence != null) {
                        System.out.println("    Forge trailing CompoundTag parameter local-slot evidence: "
                                + evidence.status() + ", declaringOwner=" + evidence.declaringOwner()
                                + ", localSlot=" + evidence.localSlot()
                                + ", loadInstructions=" + evidence.loadInstructionCount());
                    }
                }
                if (targetOverride || owner.owner().equals(MOB_OWNER)) {
                    System.out.println("    NeoForge dispatch declaration: " + owner.targetDispatchDeclaringOwner()
                            + " " + owner.targetDispatchStatus());
                    owner.targetDeclaredFinalizeSpawnMethods().forEach(method ->
                            System.out.println("    NeoForge declared: " + method.descriptor()
                                    + " access=0x" + Integer.toHexString(method.accessFlags())
                                    + " virtual=" + method.virtualInstanceMethod()));
                }
            }
        }

        System.out.println("Report: " + report);
        System.out.println("Selector transformation performed: " + result.selectorTransformationPerformed());
        System.out.println("Runtime mapping table produced: " + result.runtimeMappingTableProduced());
    }

    private static String toJson(Result result) {
        StringBuilder json = new StringBuilder(24_576);
        json.append("{\n")
                .append("  \"schemaVersion\": 1,\n")
                .append("  \"status\": \"READ_ONLY_MOB_FINALIZE_SPAWN_OVERRIDE_EVOLUTION_AUDIT\",\n")
                .append("  \"method\": \"").append(METHOD_NAME).append("\",\n")
                .append("  \"sourceNamespace\": \"MOJANG_OFFICIAL_1_20_1\",\n")
                .append("  \"targetNamespace\": \"MOJANG_OFFICIAL_1_21_1_COMPILE_CLASSPATH\",\n")
                .append("  \"ownerComparisonPolicy\": \"LITERAL_INTERNAL_NAME_EQUALITY_ONLY; NOT A CROSS_VERSION CLASS MAPPING\",\n")
                .append("  \"sourceBaseDescriptor\": \"").append(escape(SOURCE_BASE_DESCRIPTOR)).append("\",\n")
                .append("  \"targetBaseDescriptor\": \"").append(escape(TARGET_BASE_DESCRIPTOR)).append("\",\n")
                .append("  \"sourceClasspath\": ");
        appendClasspath(json, result.sourceClasspath());
        json.append(",\n  \"targetClasspath\": ");
        appendClasspath(json, result.targetClasspath());
        json.append(",\n")
                .append("  \"sourceMinecraftClassCount\": ").append(result.sourceMinecraftClassCount()).append(",\n")
                .append("  \"targetMinecraftClassCount\": ").append(result.targetMinecraftClassCount()).append(",\n")
                .append("  \"sourceMobSubclassCount\": ").append(result.sourceMobSubclassCount()).append(",\n")
                .append("  \"targetMobSubclassCount\": ").append(result.targetMobSubclassCount()).append(",\n")
                .append("  \"sourceEffectiveSubclassOverrideCount\": ")
                .append(result.sourceEffectiveSubclassOverrideCount()).append(",\n")
                .append("  \"targetEffectiveSubclassOverrideCount\": ")
                .append(result.targetEffectiveSubclassOverrideCount()).append(",\n")
                .append("  \"owners\": [\n");
        for (int i = 0; i < result.owners().size(); i++) {
            OwnerComparison owner = result.owners().get(i);
            json.append("    {\"owner\": \"").append(escape(owner.owner()))
                    .append("\", \"classification\": \"").append(owner.classification())
                    .append("\", \"sourceClassPresent\": ").append(owner.sourceClassPresent())
                    .append(", \"targetClassPresent\": ").append(owner.targetClassPresent())
                    .append(", \"sourceMobSubclass\": ").append(owner.sourceMobSubclass())
                    .append(", \"targetMobSubclass\": ").append(owner.targetMobSubclass())
                    .append(", \"sourceSuperName\": ").append(nullableString(owner.sourceSuperName()))
                    .append(", \"targetSuperName\": ").append(nullableString(owner.targetSuperName()))
                    .append(", \"sourceHierarchyVisited\": ").append(stringArray(owner.sourceHierarchyVisited()))
                    .append(", \"targetHierarchyVisited\": ").append(stringArray(owner.targetHierarchyVisited()))
                    .append(", \"sourceDeclaredFinalizeSpawnMethods\": ");
            appendMethods(json, owner.sourceDeclaredFinalizeSpawnMethods());
            json.append(", \"targetDeclaredFinalizeSpawnMethods\": ");
            appendMethods(json, owner.targetDeclaredFinalizeSpawnMethods());
            json.append(", \"sourceDispatchStatus\": \"").append(owner.sourceDispatchStatus())
                    .append("\", \"sourceDispatchDeclaringOwner\": ")
                    .append(nullableString(owner.sourceDispatchDeclaringOwner()))
                    .append(", \"targetDispatchStatus\": \"").append(owner.targetDispatchStatus())
                    .append("\", \"targetDispatchDeclaringOwner\": ")
                    .append(nullableString(owner.targetDispatchDeclaringOwner()))
                    .append(", \"sourceTrailingCompoundTagEvidence\": ");
            appendParameterEvidence(json, owner.sourceTrailingCompoundTagEvidence());
            json.append("}");
            json.append(i + 1 == result.owners().size() ? "\n" : ",\n");
        }
        json.append("  ],\n")
                .append("  \"sourceIncompleteEntityHierarchies\": ")
                .append(stringArray(result.sourceIncompleteEntityHierarchies())).append(",\n")
                .append("  \"targetIncompleteEntityHierarchies\": ")
                .append(stringArray(result.targetIncompleteEntityHierarchies())).append(",\n")
                .append("  \"limitations\": [")
                .append("\"Only net/minecraft class owners are scanned for Mob descendants; mod-specific subclasses are not in the vanilla runtime classpaths.\",")
                .append("\"Overrides are resolved by the exact per-runtime base descriptor and JVM class-superclass dispatch path; same-name overloads and private/static declarations do not count as overrides.\",")
                .append("\"Owner comparison uses literal Mojang-official internal names only and is not a complete cross-version class mapping.\",")
                .append("\"Static declaration and signature evidence does not prove behavioral equivalence or gameplay compatibility.\",")
                .append("\"Trailing CompoundTag evidence counts local-load opcodes for the source parameter slot only; it does not perform control-flow or value-flow analysis and a load may follow a local overwrite.\")
                .append("],\n")
                .append("  \"selectorTransformationPerformed\": ").append(result.selectorTransformationPerformed()).append(",\n")
                .append("  \"runtimeMappingTableProduced\": ").append(result.runtimeMappingTableProduced()).append("\n")
                .append("}\n");
        return json.toString();
    }

    private static void appendParameterEvidence(StringBuilder json, ParameterSlotEvidence evidence) {
        if (evidence == null) {
            json.append("null");
            return;
        }
        json.append("{\"declaringOwner\": \"").append(escape(evidence.declaringOwner()))
                .append("\", \"parameterIndex\": ").append(evidence.parameterIndex())
                .append(", \"localSlot\": ").append(evidence.localSlot())
                .append(", \"methodCodePresent\": ").append(evidence.methodCodePresent())
                .append(", \"loadInstructionCount\": ").append(evidence.loadInstructionCount())
                .append(", \"status\": \"").append(evidence.status()).append("\"}");
    }

    private static void appendClasspath(
            StringBuilder json, Forge1201SourceRuntimeMemberVerifier.ClasspathSummary classpath) {
        json.append("{\"listSha256\": \"").append(classpath.classpathListSha256())
                .append("\", \"entryCount\": ").append(classpath.classpathEntryCount())
                .append(", \"indexedClassCount\": ").append(classpath.indexedClassCount())
                .append(", \"duplicateClassNames\": ").append(classpath.duplicateClassNames()).append("}");
    }

    private static void appendMethods(StringBuilder json, List<MethodDeclaration> methods) {
        json.append("[");
        for (int i = 0; i < methods.size(); i++) {
            MethodDeclaration method = methods.get(i);
            if (i > 0) json.append(", ");
            json.append("{\"descriptor\": \"").append(escape(method.descriptor()))
                    .append("\", \"accessFlags\": ").append(method.accessFlags())
                    .append(", \"virtualInstanceMethod\": ").append(method.virtualInstanceMethod()).append("}");
        }
        json.append("]");
    }

    private static String stringArray(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) json.append(", ");
            json.append("\"").append(escape(values.get(i))).append("\"");
        }
        return json.append("]").toString();
    }

    private static String nullableString(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
