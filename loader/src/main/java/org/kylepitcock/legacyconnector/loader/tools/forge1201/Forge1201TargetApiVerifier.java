package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import org.kylepitcock.legacyconnector.loader.mapping.SrgMojangTable;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201ApiMappings;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

/**
 * Verifies package-relocation candidates against the actual target compile classpath.
 *
 * <p>This is a read-only verifier. It parses class-file headers and member descriptors as data;
 * it never defines, initializes, or executes target classes. It is deliberately conservative:
 * inherited members are checked through the visible class hierarchy, and unresolved hierarchy
 * information is reported separately from a confirmed missing member.</p>
 */
public final class Forge1201TargetApiVerifier {
    @FunctionalInterface
    public interface ClassBytesProvider {
        InputStream open(String internalName) throws IOException;
    }

    public enum MemberStatus {
        VERIFIED,
        MISSING,
        INCOMPLETE_HIERARCHY,
        REQUIRES_MANUAL_DESCRIPTOR_ADAPTER,
        NOT_CHECKED_NO_CONNECTOR_RUNTIME_OUTPUT,
        UNMAPPED_SOURCE_MEMBER_NAME
    }

    public record MemberIssue(
            String sourceOwner,
            String name,
            String sourceDescriptor,
            String targetOwner,
            String targetDescriptor,
            MemberStatus status,
            List<String> hierarchyVisited,
            List<String> callSites,
            String mappingRule,
            String targetName) {
        public MemberIssue {
            hierarchyVisited = List.copyOf(hierarchyVisited);
            callSites = List.copyOf(callSites);
            mappingRule = mappingRule == null ? "CATALOG_METHOD_OR_FIELD_MAPPING" : mappingRule;
            targetName = targetName == null ? name : targetName;
        }

        public MemberIssue(
                String sourceOwner, String name, String sourceDescriptor,
                String targetOwner, String targetDescriptor, MemberStatus status,
                List<String> hierarchyVisited, List<String> callSites, String mappingRule) {
            this(sourceOwner, name, sourceDescriptor, targetOwner, targetDescriptor, status,
                    hierarchyVisited, callSites, mappingRule, name);
        }

        public MemberIssue(
                String sourceOwner, String name, String sourceDescriptor,
                String targetOwner, String targetDescriptor, MemberStatus status,
                List<String> hierarchyVisited, List<String> callSites) {
            this(sourceOwner, name, sourceDescriptor, targetOwner, targetDescriptor, status,
                    hierarchyVisited, callSites, "CATALOG_METHOD_OR_FIELD_MAPPING", name);
        }

        /** Source-compatible constructor for callers that do not retain hierarchy details. */
        public MemberIssue(
                String sourceOwner, String name, String sourceDescriptor,
                String targetOwner, String targetDescriptor, MemberStatus status, List<String> callSites) {
            this(sourceOwner, name, sourceDescriptor, targetOwner, targetDescriptor,
                    status, List.of(), callSites, "CATALOG_METHOD_OR_FIELD_MAPPING", name);
        }

        public String display() {
            return sourceOwner.replace('/', '.') + "#" + name + sourceDescriptor
                    + " -> " + targetOwner.replace('/', '.') + "#" + targetName + targetDescriptor
                    + " [" + status + "; mapping=" + mappingRule + "]";
        }
    }

    /** Exact target-side match, including the inheritance path used to resolve it. */
    public record MemberMatch(
            String sourceOwner,
            String name,
            String sourceDescriptor,
            String targetOwner,
            String targetDescriptor,
            List<String> hierarchyVisited,
            List<String> callSites,
            String mappingRule,
            String targetName) {
        public MemberMatch {
            hierarchyVisited = List.copyOf(hierarchyVisited);
            callSites = List.copyOf(callSites);
            mappingRule = mappingRule == null ? "CATALOG_METHOD_OR_FIELD_MAPPING" : mappingRule;
            targetName = targetName == null ? name : targetName;
        }

        public MemberMatch(
                String sourceOwner, String name, String sourceDescriptor,
                String targetOwner, String targetDescriptor,
                List<String> hierarchyVisited, List<String> callSites, String mappingRule) {
            this(sourceOwner, name, sourceDescriptor, targetOwner, targetDescriptor,
                    hierarchyVisited, callSites, mappingRule, name);
        }

        public MemberMatch(
                String sourceOwner, String name, String sourceDescriptor,
                String targetOwner, String targetDescriptor,
                List<String> hierarchyVisited, List<String> callSites) {
            this(sourceOwner, name, sourceDescriptor, targetOwner, targetDescriptor,
                    hierarchyVisited, callSites, "CATALOG_METHOD_OR_FIELD_MAPPING", name);
        }

        public String display() {
            return sourceOwner.replace('/', '.') + "#" + name + sourceDescriptor
                    + " -> " + targetOwner.replace('/', '.') + "#" + targetName + targetDescriptor
                    + " [FOUND; hierarchy=" + hierarchyVisited + "; mapping=" + mappingRule + "]";
        }
    }

    public record Result(
            int candidateClassCount,
            int verifiedClassCount,
            List<String> missingTargetClasses,
            int uniqueMethodReferences,
            int verifiedMethodReferences,
            List<MemberIssue> methodIssues,
            int uniqueFieldReferences,
            int verifiedFieldReferences,
            List<MemberIssue> fieldIssues,
            List<MemberMatch> verifiedMethods,
            List<MemberMatch> verifiedFields) {
        public Result {
            missingTargetClasses = List.copyOf(missingTargetClasses);
            methodIssues = List.copyOf(methodIssues);
            fieldIssues = List.copyOf(fieldIssues);
            verifiedMethods = List.copyOf(verifiedMethods);
            verifiedFields = List.copyOf(verifiedFields);
        }

        /** Source-compatible constructor for earlier report consumers. */
        public Result(
                int candidateClassCount,
                int verifiedClassCount,
                List<String> missingTargetClasses,
                int uniqueMethodReferences,
                int verifiedMethodReferences,
                List<MemberIssue> methodIssues,
                int uniqueFieldReferences,
                int verifiedFieldReferences,
                List<MemberIssue> fieldIssues) {
            this(candidateClassCount, verifiedClassCount, missingTargetClasses,
                    uniqueMethodReferences, verifiedMethodReferences, methodIssues,
                    uniqueFieldReferences, verifiedFieldReferences, fieldIssues, List.of(), List.of());
        }
    }

    /** Exact name+descriptor identity for a target-side JVM member. */
    public record MemberSignature(String name, String descriptor) {}

    /** Read-only target class metadata exposed to independent static analyzers. */
    public record TargetClassInfo(
            String name,
            String superName,
            List<String> interfaces,
            Set<MemberSignature> methods,
            Set<MemberSignature> fields) {
        public TargetClassInfo {
            interfaces = List.copyOf(interfaces);
            methods = Set.copyOf(methods);
            fields = Set.copyOf(fields);
        }
    }

    /**
     * Local-variable load instructions observed in one directly-declared method body.
     * This is bytecode evidence only; it does not perform control-flow or value-flow analysis.
     */
    public record MethodCodeSummary(boolean codePresent, Map<Integer, Integer> localLoadCounts) {
        public MethodCodeSummary {
            localLoadCounts = Map.copyOf(localLoadCounts);
        }

        public int loadCount(int localSlot) {
            return localLoadCounts.getOrDefault(localSlot, 0);
        }
    }

    public enum LookupStatus {
        FOUND,
        MISSING,
        INCOMPLETE_HIERARCHY
    }

    /** Structural result only; FOUND does not prove runtime or semantic compatibility. */
    public record MemberLookup(LookupStatus status, List<String> hierarchyVisited) {
        public MemberLookup {
            hierarchyVisited = List.copyOf(hierarchyVisited);
        }
    }

    /**
     * Per-analysis, read-only view over target class bytes. It shares the verifier's exact
     * descriptor and inherited-member logic instead of requiring analyzers to build a second
     * class hierarchy index. Instances intentionally own their own caches.
     */
    public static final class TargetIndex {
        private final Forge1201TargetApiVerifier verifier;
        private final Set<String> unavailablePrefixes;

        private TargetIndex(ClassBytesProvider classBytes, Set<String> unavailablePrefixes) {
            this.verifier = new Forge1201TargetApiVerifier(classBytes, unavailablePrefixes);
            this.unavailablePrefixes = Set.copyOf(unavailablePrefixes);
        }

        /** True when the requested class belongs to a runtime domain not supplied to this index. */
        public boolean isUnavailableClass(String internalName) {
            return unavailablePrefixes.stream().anyMatch(internalName::startsWith);
        }

        /** Returns empty only when the requested target class is absent. */
        public Optional<TargetClassInfo> classInfo(String internalName) throws IOException {
            TargetClass target = verifier.readClass(internalName);
            if (target == null) return Optional.empty();
            return Optional.of(new TargetClassInfo(
                    target.name(),
                    target.superName(),
                    target.interfaces(),
                    signatures(target.methods()),
                    signatures(target.fields())));
        }

        /** Returns the access flags for a directly declared method, or empty when absent. */
        public Optional<Integer> declaredMethodAccess(String owner, String name, String descriptor)
                throws IOException {
            TargetClass target = verifier.readClass(owner);
            return target == null ? Optional.empty()
                    : Optional.ofNullable(target.methodAccess().get(new MemberKey(name, descriptor)));
        }

        /** Inspects local load opcodes in a directly declared method body without defining the class. */
        public Optional<MethodCodeSummary> inspectMethodCode(String owner, String name, String descriptor)
                throws IOException {
            return verifier.inspectMethodCode(owner, name, descriptor);
        }

        public MemberLookup lookupMethod(String owner, String name, String descriptor) throws IOException {
            if (verifier.readClass(owner) == null) {
                return new MemberLookup(LookupStatus.MISSING, List.of(owner));
            }
            Set<String> visited = new LinkedHashSet<>();
            Lookup lookup = verifier.findMethod(owner, new MemberKey(name, descriptor), visited);
            return toPublicLookup(lookup, visited);
        }

        public MemberLookup lookupField(String owner, String name, String descriptor) throws IOException {
            if (verifier.readClass(owner) == null) {
                return new MemberLookup(LookupStatus.MISSING, List.of(owner));
            }
            Set<String> visited = new LinkedHashSet<>();
            Lookup lookup = verifier.findField(owner, new MemberKey(name, descriptor), visited);
            return toPublicLookup(lookup, visited);
        }

        /** Verify through this index's existing class-byte provider and metadata caches. */
        public Result verify(Forge1201ApiUsageAnalyzer.Analysis analysis) throws IOException {
            if (analysis == null) throw new IllegalArgumentException("API analysis is required");
            return verifier.verify(analysis);
        }

        private static MemberLookup toPublicLookup(Lookup lookup, Set<String> visited) {
            LookupStatus status = lookup.found()
                    ? LookupStatus.FOUND
                    : lookup.complete() ? LookupStatus.MISSING : LookupStatus.INCOMPLETE_HIERARCHY;
            return new MemberLookup(status, new ArrayList<>(visited));
        }

        private static Set<MemberSignature> signatures(Set<MemberKey> members) {
            Set<MemberSignature> result = new HashSet<>();
            for (MemberKey member : members) {
                result.add(new MemberSignature(member.name(), member.descriptor()));
            }
            return Set.copyOf(result);
        }
    }

    /** Creates a fresh target index whose metadata cache is isolated to this analysis. */
    public static TargetIndex createIndex(ClassBytesProvider classBytes) {
        return createIndex(classBytes, Set.of());
    }

    /**
     * Creates an index with explicitly unavailable class-name domains. Members in those domains
     * are reported as not checked instead of being mislabeled as missing.
     */
    public static TargetIndex createIndex(
            ClassBytesProvider classBytes, Set<String> unavailablePrefixes) {
        if (classBytes == null) throw new IllegalArgumentException("Target class byte provider is required");
        if (unavailablePrefixes == null) throw new IllegalArgumentException("Unavailable prefixes are required");
        return new TargetIndex(classBytes, unavailablePrefixes);
    }

    private record MemberKey(String name, String descriptor) {}

    private record TargetClass(
            String name,
            String superName,
            List<String> interfaces,
            Set<MemberKey> methods,
            Set<MemberKey> fields,
            Map<MemberKey, Integer> methodAccess) {}

    private record Lookup(boolean found, boolean complete) {}

    private final ClassBytesProvider classBytes;
    private final Set<String> unavailablePrefixes;
    private final Map<String, TargetClass> classCache = new HashMap<>();
    private final Set<String> missingClassCache = new HashSet<>();
    private final Map<MethodCodeKey, Optional<MethodCodeSummary>> methodCodeCache = new HashMap<>();

    private Forge1201TargetApiVerifier(ClassBytesProvider classBytes, Set<String> unavailablePrefixes) {
        this.classBytes = classBytes;
        this.unavailablePrefixes = Set.copyOf(unavailablePrefixes);
    }

    private boolean isUnavailableClass(String internalName) {
        return unavailablePrefixes.stream().anyMatch(internalName::startsWith);
    }

    public static Result verify(Path sourceJar, ClassLoader targetClassLoader) throws IOException {
        if (targetClassLoader == null) throw new IllegalArgumentException("Target classloader is required");
        Forge1201ApiUsageAnalyzer.Analysis analysis = Forge1201ApiUsageAnalyzer.analyze(sourceJar);
        return verify(analysis, internalName ->
                targetClassLoader.getResourceAsStream(internalName + ".class"));
    }

    /**
     * Verifies the package-relocated NeoForge API references in a metadata-translated JAR.
     * Unlike {@link #verify(Path, ClassLoader)}, this path accepts the output of the
     * preliminary transformer and does not try to reapply source package mappings.
     */
    public static Result verifyTranslated(Path translatedJar, ClassLoader targetClassLoader) throws IOException {
        if (targetClassLoader == null) throw new IllegalArgumentException("Target classloader is required");
        Forge1201ApiUsageAnalyzer.Analysis analysis =
                Forge1201ApiUsageAnalyzer.analyzeTranslated(translatedJar);
        return verify(analysis, internalName ->
                targetClassLoader.getResourceAsStream(internalName + ".class"));
    }

    public static Result verify(
            Forge1201ApiUsageAnalyzer.Analysis analysis,
            ClassBytesProvider classBytes) throws IOException {
        if (analysis == null) throw new IllegalArgumentException("API analysis is required");
        if (classBytes == null) throw new IllegalArgumentException("Target class byte provider is required");
        return new Forge1201TargetApiVerifier(classBytes, Set.of()).verify(analysis);
    }

    private Result verify(Forge1201ApiUsageAnalyzer.Analysis analysis) throws IOException {
        List<String> candidates = analysis.referencedApiClasses().stream()
                .filter(this::isCandidate)
                .map(Forge1201TargetApiVerifier::mapType)
                .distinct()
                .sorted()
                .toList();

        List<String> missingClasses = new ArrayList<>();
        int verifiedClasses = 0;
        for (String targetName : candidates) {
            if (readClass(targetName) == null) {
                if (!isUnavailableClass(targetName)) missingClasses.add(targetName);
            } else {
                verifiedClasses++;
            }
        }
        missingClasses.sort(String::compareTo);

        List<Forge1201ApiUsageAnalyzer.ApiMemberReference> candidateMethods =
                analysis.methodReferences().stream()
                        .filter(reference -> isCandidate(reference.owner()))
                        .toList();
        List<Forge1201ApiUsageAnalyzer.ApiMemberReference> candidateFields =
                analysis.fieldReferences().stream()
                        .filter(reference -> isCandidate(reference.owner()))
                        .toList();

        List<MemberIssue> methodIssues = new ArrayList<>();
        List<MemberMatch> verifiedMethodMatches = new ArrayList<>();
        int verifiedMethods = 0;
        for (var reference : candidateMethods) {
            String targetOwner = mapType(reference.owner());
            String targetDescriptor = mapMethodDescriptor(reference.owner(), reference.name(), reference.descriptor());
            List<String> callSites = methodCallSites(analysis, reference);
            SourceMemberName sourceName = normalizeSourceMemberName(reference.name(), true);
            if (!sourceName.mapped()) {
                methodIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, MemberStatus.UNMAPPED_SOURCE_MEMBER_NAME,
                        List.of(), callSites, "SRG_TO_MOJANG_MAPPING_NOT_FOUND", reference.name()));
                continue;
            }
            String targetName = Mc1201ApiMappings.targetMemberName(
                    reference.owner(), sourceName.name(), reference.descriptor());
            String mappingRule = combineMappingRules(
                    sourceName.mappingRule(),
                    methodDescriptorMappingRule(reference.owner(), sourceName.name(), reference.descriptor()),
                    !sourceName.name().equals(targetName) ? "MC1201_MEMBER_NAME_MAPPING" : null);
            if (descriptorNeedsManualAdapter(reference.descriptor(), true)) {
                methodIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, MemberStatus.REQUIRES_MANUAL_DESCRIPTOR_ADAPTER,
                        List.of(), callSites, "MANUAL_ADAPTER_CATALOG", targetName));
                continue;
            }
            if (isUnavailableClass(targetOwner)) {
                methodIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, MemberStatus.NOT_CHECKED_NO_CONNECTOR_RUNTIME_OUTPUT,
                        List.of(), callSites, mappingRule, targetName));
                continue;
            }
            Set<String> hierarchyVisited = new LinkedHashSet<>();
            Lookup lookup = readClass(targetOwner) == null
                    ? new Lookup(false, true)
                    : findMethod(targetOwner, new MemberKey(targetName, targetDescriptor), hierarchyVisited);
            if (lookup.found()) {
                verifiedMethods++;
                verifiedMethodMatches.add(new MemberMatch(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, new ArrayList<>(hierarchyVisited),
                        callSites, mappingRule, targetName));
            } else {
                methodIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor,
                        lookup.complete() ? MemberStatus.MISSING : MemberStatus.INCOMPLETE_HIERARCHY,
                        new ArrayList<>(hierarchyVisited), callSites, mappingRule, targetName));
            }
        }

        List<MemberIssue> fieldIssues = new ArrayList<>();
        List<MemberMatch> verifiedFieldMatches = new ArrayList<>();
        int verifiedFields = 0;
        for (var reference : candidateFields) {
            String targetOwner = mapType(reference.owner());
            String targetDescriptor = mapFieldDescriptor(reference.descriptor());
            List<String> callSites = fieldCallSites(analysis, reference);
            SourceMemberName sourceName = normalizeSourceMemberName(reference.name(), false);
            if (!sourceName.mapped()) {
                fieldIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, MemberStatus.UNMAPPED_SOURCE_MEMBER_NAME,
                        List.of(), callSites, "SRG_TO_MOJANG_MAPPING_NOT_FOUND", reference.name()));
                continue;
            }

            String targetName = Mc1201ApiMappings.targetMemberName(
                    reference.owner(), sourceName.name(), ":" + reference.descriptor());
            String mappingRule = combineMappingRules(
                    sourceName.mappingRule(), "CATALOG_FIELD_DESCRIPTOR_MAPPING",
                    !sourceName.name().equals(targetName) ? "MC1201_MEMBER_NAME_MAPPING" : null);

            Mc1201ApiMappings.StaticFieldMapping staticFieldMapping =
                    Mc1201ApiMappings.staticFieldMapping(reference.owner(), sourceName.name(), reference.descriptor());
            if (staticFieldMapping != null) {
                if (!allFieldCallSitesAreGetStatic(analysis, reference)) {
                    // mc-fields.tsv is a GETSTATIC-only bytecode rewrite. Without that opcode evidence,
                    // retain the unmodified shape rather than applying a static-field rule to handles
                    // or other reference forms.
                } else if ("none".equals(staticFieldMapping.targetDescriptor())) {
                    fieldIssues.add(new MemberIssue(
                            reference.owner(), reference.name(), reference.descriptor(),
                            targetOwner, targetDescriptor, MemberStatus.REQUIRES_MANUAL_DESCRIPTOR_ADAPTER,
                            List.of(), callSites, "MC1201_STATIC_FIELD_REPLACED_BY_HELPER:" + staticFieldMapping.postAdapter(),
                            targetName));
                    continue;
                } else {
                    targetName = staticFieldMapping.targetName();
                    targetDescriptor = mapFieldDescriptor(staticFieldMapping.targetDescriptor());
                    mappingRule = combineMappingRules(mappingRule,
                            "MC1201_STATIC_FIELD_ADAPTER:" + staticFieldMapping.postAdapter());
                }
            }

            if (descriptorNeedsManualAdapter(reference.descriptor(), false)) {
                fieldIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, MemberStatus.REQUIRES_MANUAL_DESCRIPTOR_ADAPTER,
                        List.of(), callSites, "MANUAL_ADAPTER_CATALOG", targetName));
                continue;
            }
            if (isUnavailableClass(targetOwner)) {
                fieldIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, MemberStatus.NOT_CHECKED_NO_CONNECTOR_RUNTIME_OUTPUT,
                        List.of(), callSites, mappingRule, targetName));
                continue;
            }
            Set<String> hierarchyVisited = new LinkedHashSet<>();
            Lookup lookup = readClass(targetOwner) == null
                    ? new Lookup(false, true)
                    : findField(targetOwner, new MemberKey(targetName, targetDescriptor), hierarchyVisited);
            if (lookup.found()) {
                verifiedFields++;
                verifiedFieldMatches.add(new MemberMatch(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, new ArrayList<>(hierarchyVisited),
                        callSites, mappingRule, targetName));
            } else {
                fieldIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor,
                        lookup.complete() ? MemberStatus.MISSING : MemberStatus.INCOMPLETE_HIERARCHY,
                        new ArrayList<>(hierarchyVisited), callSites, mappingRule, targetName));
            }
        }

        methodIssues.sort(java.util.Comparator.comparing(MemberIssue::display));
        fieldIssues.sort(java.util.Comparator.comparing(MemberIssue::display));
        verifiedMethodMatches.sort(java.util.Comparator.comparing(MemberMatch::display));
        verifiedFieldMatches.sort(java.util.Comparator.comparing(MemberMatch::display));

        return new Result(
                candidates.size(),
                verifiedClasses,
                missingClasses,
                candidateMethods.size(),
                verifiedMethods,
                methodIssues,
                candidateFields.size(),
                verifiedFields,
                fieldIssues,
                verifiedMethodMatches,
                verifiedFieldMatches);
    }

    private boolean isCandidate(String sourceInternalName) {
        Forge1201MappingCatalog.Kind kind = Forge1201MappingCatalog.classify(sourceInternalName).kind();
        return kind == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE
                || kind == Forge1201MappingCatalog.Kind.EXPLICIT_TYPE_REPLACEMENT
                || Forge1201MappingCatalog.isReviewedTargetApiType(sourceInternalName)
                || Forge1211TargetMethodAliases.isTargetApiOwner(sourceInternalName);
    }

    private static String mapType(String sourceInternalName) {
        String minecraftMapped = Mc1201ApiMappings.targetClassName(sourceInternalName);
        Forge1201MappingCatalog.Decision decision = Forge1201MappingCatalog.classify(minecraftMapped);
        return decision.kind() == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE
                        || decision.kind() == Forge1201MappingCatalog.Kind.EXPLICIT_TYPE_REPLACEMENT
                ? decision.targetInternalName()
                : minecraftMapped;
    }

    private record SourceMemberName(String name, boolean mapped, String mappingRule) {}

    private static SourceMemberName normalizeSourceMemberName(String sourceName, boolean method) {
        boolean srgName = sourceName != null && (method
                ? sourceName.matches("m_\\d+_")
                : sourceName.matches("f_\\d+_"));
        if (!srgName) return new SourceMemberName(sourceName, true, null);
        String mapped = SrgMojangTable.load().get(sourceName);
        return mapped == null
                ? new SourceMemberName(sourceName, false, "SRG_TO_MOJANG_MAPPING_NOT_FOUND")
                : new SourceMemberName(mapped, true, "SRG_TO_MOJANG");
    }

    private static String combineMappingRules(String... rules) {
        List<String> present = new ArrayList<>();
        for (String rule : rules) {
            if (rule != null && !rule.isBlank() && !present.contains(rule)) present.add(rule);
        }
        return present.isEmpty() ? "CATALOG_METHOD_OR_FIELD_MAPPING" : String.join("+", present);
    }

    private static boolean allFieldCallSitesAreGetStatic(
            Forge1201ApiUsageAnalyzer.Analysis analysis,
            Forge1201ApiUsageAnalyzer.ApiMemberReference reference) {
        List<Forge1201ApiUsageAnalyzer.ApiFieldCallSite> sites = analysis.fieldCallSites().stream()
                .filter(callSite -> callSite.targetOwner().equals(reference.owner())
                        && callSite.targetName().equals(reference.name())
                        && callSite.targetDescriptor().equals(reference.descriptor()))
                .toList();
        return !sites.isEmpty() && sites.stream().allMatch(site -> site.opcode() == Opcodes.GETSTATIC);
    }

    private static Remapper descriptorRemapper() {
        return new Remapper() {
            @Override
            public String map(String internalName) {
                // Apply Minecraft class migrations first, then reviewed Forge-to-NeoForge type moves.
                return Forge1201TargetApiVerifier.mapType(internalName);
            }
        };
    }

    private static String mapMethodDescriptor(String sourceOwner, String methodName, String descriptor) {
        String mappedDescriptor = descriptorRemapper().mapMethodDesc(descriptor);
        Type sourceReturn = Type.getReturnType(descriptor);
        if ("net/minecraftforge/registries/DeferredRegister".equals(sourceOwner)
                && "register".equals(methodName)
                && sourceReturn.getSort() == Type.OBJECT
                && "net/minecraftforge/registries/RegistryObject".equals(sourceReturn.getInternalName())) {
            // This one factory method returns DeferredHolder in NeoForge. Keep the general
            // RegistryObject -> Supplier rule for unrelated get-only references.
            return Type.getMethodDescriptor(
                    Type.getObjectType("net/neoforged/neoforge/registries/DeferredHolder"),
                    Type.getArgumentTypes(mappedDescriptor));
        }
        return mappedDescriptor;
    }

    private static String methodDescriptorMappingRule(String sourceOwner, String methodName, String descriptor) {
        Type sourceReturn = Type.getReturnType(descriptor);
        if ("net/minecraftforge/registries/DeferredRegister".equals(sourceOwner)
                && "register".equals(methodName)
                && sourceReturn.getSort() == Type.OBJECT
                && "net/minecraftforge/registries/RegistryObject".equals(sourceReturn.getInternalName())) {
            return "DEFERRED_REGISTER_REGISTER_RETURNS_DEFERRED_HOLDER";
        }
        return "CATALOG_METHOD_OWNER_AND_DESCRIPTOR_MAPPING";
    }

    private static String mapFieldDescriptor(String descriptor) {
        return descriptorRemapper().mapDesc(descriptor);
    }

    private static boolean descriptorNeedsManualAdapter(String descriptor, boolean methodDescriptor) {
        if (methodDescriptor) {
            Type method = Type.getMethodType(descriptor);
            if (containsManualAdapterType(method.getReturnType())) return true;
            for (Type argument : method.getArgumentTypes()) {
                if (containsManualAdapterType(argument)) return true;
            }
            return false;
        }
        return containsManualAdapterType(Type.getType(descriptor));
    }

    private static boolean containsManualAdapterType(Type type) {
        if (type.getSort() == Type.ARRAY) {
            return containsManualAdapterType(type.getElementType());
        }
        if (type.getSort() != Type.OBJECT) return false;

        String internalName = type.getInternalName();
        if (!internalName.startsWith("net/minecraftforge/")) return false;
        Forge1201MappingCatalog.Kind kind = Forge1201MappingCatalog.classify(internalName).kind();
        return kind == Forge1201MappingCatalog.Kind.MANUAL_ADAPTER_REQUIRED
                || kind == Forge1201MappingCatalog.Kind.UNMAPPED_FORGE_API;
    }

    private record MethodCodeKey(String owner, MemberKey member) {}

    private Optional<MethodCodeSummary> inspectMethodCode(String owner, String name, String descriptor)
            throws IOException {
        MethodCodeKey cacheKey = new MethodCodeKey(owner, new MemberKey(name, descriptor));
        Optional<MethodCodeSummary> cached = methodCodeCache.get(cacheKey);
        if (cached != null) return cached;

        TargetClass classInfo = readClass(owner);
        if (classInfo == null || !classInfo.methods().contains(cacheKey.member())) {
            methodCodeCache.put(cacheKey, Optional.empty());
            return Optional.empty();
        }

        final String[] actualOwner = new String[1];
        final boolean[] methodFound = new boolean[1];
        final boolean[] codePresent = new boolean[1];
        Map<Integer, Integer> localLoadCounts = new HashMap<>();
        try (InputStream input = classBytes.open(owner)) {
            if (input == null) {
                throw new IOException("Class resource disappeared while inspecting method code: " + owner);
            }
            ClassReader reader = new ClassReader(input);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String className, String signature,
                                  String parentName, String[] implementedInterfaces) {
                    actualOwner[0] = className;
                }

                @Override
                public MethodVisitor visitMethod(
                        int access, String methodName, String methodDescriptor,
                        String signature, String[] exceptions) {
                    if (!name.equals(methodName) || !descriptor.equals(methodDescriptor)) return null;
                    methodFound[0] = true;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitCode() {
                            codePresent[0] = true;
                        }

                        @Override
                        public void visitVarInsn(int opcode, int local) {
                            if (opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD
                                    || opcode == Opcodes.FLOAD || opcode == Opcodes.DLOAD
                                    || opcode == Opcodes.ALOAD) {
                                localLoadCounts.merge(local, 1, Integer::sum);
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IllegalArgumentException malformedClass) {
            throw new IOException("Cannot inspect method code in class resource " + owner, malformedClass);
        }

        if (!owner.equals(actualOwner[0])) {
            throw new IOException("Target class resource mismatch while inspecting method code: requested "
                    + owner + " but found " + actualOwner[0]);
        }
        if (!methodFound[0]) {
            throw new IOException("Method metadata disappeared while inspecting bytecode: "
                    + owner + "#" + name + descriptor);
        }

        Optional<MethodCodeSummary> result =
                Optional.of(new MethodCodeSummary(codePresent[0], localLoadCounts));
        methodCodeCache.put(cacheKey, result);
        return result;
    }

    private TargetClass readClass(String internalName) throws IOException {
        if (classCache.containsKey(internalName)) return classCache.get(internalName);
        if (missingClassCache.contains(internalName)) return null;

        try (InputStream input = classBytes.open(internalName)) {
            if (input == null) {
                missingClassCache.add(internalName);
                return null;
            }

            final String[] superName = new String[1];
            final String[] name = new String[1];
            final List<String> interfaces = new ArrayList<>();
            final Set<MemberKey> methods = new HashSet<>();
            final Set<MemberKey> fields = new HashSet<>();
            final Map<MemberKey, Integer> methodAccess = new HashMap<>();
            ClassReader reader = new ClassReader(input);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String className, String signature,
                                  String parentName, String[] implementedInterfaces) {
                    name[0] = className;
                    superName[0] = parentName;
                    if (implementedInterfaces != null) {
                        interfaces.addAll(List.of(implementedInterfaces));
                    }
                }

                @Override
                public FieldVisitor visitField(int access, String fieldName, String descriptor,
                                               String signature, Object value) {
                    fields.add(new MemberKey(fieldName, descriptor));
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                                                 String signature, String[] exceptions) {
                    MemberKey key = new MemberKey(methodName, descriptor);
                    methods.add(key);
                    methodAccess.put(key, access);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            if (!internalName.equals(name[0])) {
                throw new IOException("Target class resource mismatch: requested " + internalName
                        + " but found " + name[0]);
            }
            TargetClass result = new TargetClass(
                    name[0], superName[0], List.copyOf(interfaces), Set.copyOf(methods), Set.copyOf(fields),
                    Map.copyOf(methodAccess));
            classCache.put(internalName, result);
            return result;
        } catch (IllegalArgumentException malformedClass) {
            throw new IOException("Cannot parse target class resource " + internalName, malformedClass);
        }
    }

    private Lookup findMethod(String owner, MemberKey member, Set<String> visited) throws IOException {
        if (!visited.add(owner)) return new Lookup(false, true);
        TargetClass info = readClass(owner);
        if (info == null) return new Lookup(false, false);
        if (info.methods().contains(member)) return new Lookup(true, true);

        boolean complete = true;
        for (String implementedInterface : info.interfaces()) {
            Lookup lookup = findMethod(implementedInterface, member, visited);
            if (lookup.found()) return lookup;
            complete &= lookup.complete();
        }
        if (info.superName() != null) {
            Lookup lookup = findMethod(info.superName(), member, visited);
            if (lookup.found()) return lookup;
            complete &= lookup.complete();
        }
        return new Lookup(false, complete);
    }

    private Lookup findField(String owner, MemberKey member, Set<String> visited) throws IOException {
        if (!visited.add(owner)) return new Lookup(false, true);
        TargetClass info = readClass(owner);
        if (info == null) return new Lookup(false, false);
        if (info.fields().contains(member)) return new Lookup(true, true);

        boolean complete = true;
        for (String implementedInterface : info.interfaces()) {
            Lookup lookup = findField(implementedInterface, member, visited);
            if (lookup.found()) return lookup;
            complete &= lookup.complete();
        }
        if (info.superName() != null) {
            Lookup lookup = findField(info.superName(), member, visited);
            if (lookup.found()) return lookup;
            complete &= lookup.complete();
        }
        return new Lookup(false, complete);
    }

    private static List<String> methodCallSites(
            Forge1201ApiUsageAnalyzer.Analysis analysis,
            Forge1201ApiUsageAnalyzer.ApiMemberReference reference) {
        return analysis.methodCallSites().stream()
                .filter(callSite -> callSite.targetOwner().equals(reference.owner())
                        && callSite.targetName().equals(reference.name())
                        && callSite.targetDescriptor().equals(reference.descriptor()))
                .map(Forge1201ApiUsageAnalyzer.ApiMethodCallSite::display)
                .sorted()
                .toList();
    }

    private static List<String> fieldCallSites(
            Forge1201ApiUsageAnalyzer.Analysis analysis,
            Forge1201ApiUsageAnalyzer.ApiMemberReference reference) {
        return analysis.fieldCallSites().stream()
                .filter(callSite -> callSite.targetOwner().equals(reference.owner())
                        && callSite.targetName().equals(reference.name())
                        && callSite.targetDescriptor().equals(reference.descriptor()))
                .map(Forge1201ApiUsageAnalyzer.ApiFieldCallSite::display)
                .sorted()
                .toList();
    }

    static void printIssue(MemberIssue issue) {
        System.out.println("  " + issue.display());
        if (!issue.hierarchyVisited().isEmpty()) {
            System.out.println("    HIERARCHY: " + issue.hierarchyVisited());
        }
        if (issue.callSites().isEmpty()) {
            System.out.println("    CALLSITE: none captured (the reference may come from a handle or metadata)");
            return;
        }
        issue.callSites().forEach(callSite -> System.out.println("    CALLSITE: " + callSite));
    }

    public static void print(Result result, String targetVersion) {
        System.out.println("Forge 1.20.1 -> NeoForge " + targetVersion + " target API verification");
        System.out.println("Mode: read-only ASM class-file inspection; no target classes were loaded or executed.");
        System.out.println("Relocation candidate classes: " + result.candidateClassCount());
        System.out.println("Target classes found: " + result.verifiedClassCount());
        System.out.println("Target classes missing: " + result.missingTargetClasses().size());
        result.missingTargetClasses().forEach(name ->
                System.out.println("  MISSING CLASS: " + name.replace('/', '.')));

        System.out.println("\nCandidate method signatures:");
        System.out.println("  unique references: " + result.uniqueMethodReferences());
        System.out.println("  exact target/inherited matches: " + result.verifiedMethodReferences());
        System.out.println("  unresolved or changed: " + result.methodIssues().size());
        result.methodIssues().forEach(Forge1201TargetApiVerifier::printIssue);

        System.out.println("\nCandidate field signatures:");
        System.out.println("  unique references: " + result.uniqueFieldReferences());
        System.out.println("  exact target/inherited matches: " + result.verifiedFieldReferences());
        System.out.println("  unresolved or changed: " + result.fieldIssues().size());
        result.fieldIssues().forEach(Forge1201TargetApiVerifier::printIssue);

        System.out.println("\nA matching class/member signature is only structural evidence, not semantic compatibility.");
        System.out.println("Manual-adapter types and Minecraft 1.20.1 -> 1.21.1 member changes require separate work.");
        System.out.println("Do not load a translated JAR based on this report.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException(
                    "Usage: Forge1201TargetApiVerifier <path-to-original-forge-1.20.1-jar>");
        }
        String targetVersion = System.getProperty("legacyconnector.targetNeoForgeVersion", "unknown");
        Result result = verify(Path.of(args[0]), ClassLoader.getSystemClassLoader());
        print(result, targetVersion);
    }
}
