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
        REQUIRES_MANUAL_DESCRIPTOR_ADAPTER
    }

    public record MemberIssue(
            String sourceOwner,
            String name,
            String sourceDescriptor,
            String targetOwner,
            String targetDescriptor,
            MemberStatus status,
            List<String> callSites) {
        public MemberIssue {
            callSites = List.copyOf(callSites);
        }

        public String display() {
            return sourceOwner.replace('/', '.') + "#" + name + sourceDescriptor
                    + " -> " + targetOwner.replace('/', '.') + "#" + name + targetDescriptor
                    + " [" + status + "]";
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
            List<MemberIssue> fieldIssues) {
        public Result {
            missingTargetClasses = List.copyOf(missingTargetClasses);
            methodIssues = List.copyOf(methodIssues);
            fieldIssues = List.copyOf(fieldIssues);
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

        private TargetIndex(ClassBytesProvider classBytes) {
            this.verifier = new Forge1201TargetApiVerifier(classBytes);
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

        public MemberLookup lookupMethod(String owner, String name, String descriptor) throws IOException {
            Set<String> visited = new LinkedHashSet<>();
            Lookup lookup = verifier.findMethod(owner, new MemberKey(name, descriptor), visited);
            return toPublicLookup(lookup, visited);
        }

        public MemberLookup lookupField(String owner, String name, String descriptor) throws IOException {
            Set<String> visited = new LinkedHashSet<>();
            Lookup lookup = verifier.findField(owner, new MemberKey(name, descriptor), visited);
            return toPublicLookup(lookup, visited);
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
        if (classBytes == null) throw new IllegalArgumentException("Target class byte provider is required");
        return new TargetIndex(classBytes);
    }

    private record MemberKey(String name, String descriptor) {}

    private record TargetClass(
            String name,
            String superName,
            List<String> interfaces,
            Set<MemberKey> methods,
            Set<MemberKey> fields) {}

    private record Lookup(boolean found, boolean complete) {}

    private final ClassBytesProvider classBytes;
    private final Map<String, TargetClass> classCache = new HashMap<>();
    private final Set<String> missingClassCache = new HashSet<>();

    private Forge1201TargetApiVerifier(ClassBytesProvider classBytes) {
        this.classBytes = classBytes;
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
        return new Forge1201TargetApiVerifier(classBytes).verify(analysis);
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
                missingClasses.add(targetName);
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
        int verifiedMethods = 0;
        for (var reference : candidateMethods) {
            String targetOwner = mapType(reference.owner());
            String targetDescriptor = mapMethodDescriptor(reference.descriptor());
            if (descriptorNeedsManualAdapter(reference.descriptor(), true)) {
                methodIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, MemberStatus.REQUIRES_MANUAL_DESCRIPTOR_ADAPTER,
                        methodCallSites(analysis, reference)));
                continue;
            }
            Lookup lookup = findMethod(targetOwner, new MemberKey(reference.name(), targetDescriptor), new HashSet<>());
            if (lookup.found()) {
                verifiedMethods++;
            } else {
                methodIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor,
                        lookup.complete() ? MemberStatus.MISSING : MemberStatus.INCOMPLETE_HIERARCHY,
                        methodCallSites(analysis, reference)));
            }
        }

        List<MemberIssue> fieldIssues = new ArrayList<>();
        int verifiedFields = 0;
        for (var reference : candidateFields) {
            String targetOwner = mapType(reference.owner());
            String targetDescriptor = mapFieldDescriptor(reference.descriptor());
            if (descriptorNeedsManualAdapter(reference.descriptor(), false)) {
                fieldIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor, MemberStatus.REQUIRES_MANUAL_DESCRIPTOR_ADAPTER,
                        fieldCallSites(analysis, reference)));
                continue;
            }
            Lookup lookup = findField(targetOwner, new MemberKey(reference.name(), targetDescriptor), new HashSet<>());
            if (lookup.found()) {
                verifiedFields++;
            } else {
                fieldIssues.add(new MemberIssue(
                        reference.owner(), reference.name(), reference.descriptor(),
                        targetOwner, targetDescriptor,
                        lookup.complete() ? MemberStatus.MISSING : MemberStatus.INCOMPLETE_HIERARCHY,
                        fieldCallSites(analysis, reference)));
            }
        }

        methodIssues.sort(java.util.Comparator.comparing(MemberIssue::display));
        fieldIssues.sort(java.util.Comparator.comparing(MemberIssue::display));

        return new Result(
                candidates.size(),
                verifiedClasses,
                missingClasses,
                candidateMethods.size(),
                verifiedMethods,
                methodIssues,
                candidateFields.size(),
                verifiedFields,
                fieldIssues);
    }

    private boolean isCandidate(String sourceInternalName) {
        Forge1201MappingCatalog.Kind kind = Forge1201MappingCatalog.classify(sourceInternalName).kind();
        return kind == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE
                || kind == Forge1201MappingCatalog.Kind.EXPLICIT_TYPE_REPLACEMENT
                || Forge1201MappingCatalog.isReviewedTargetApiType(sourceInternalName)
                || Forge1211TargetMethodAliases.isTargetApiOwner(sourceInternalName);
    }

    private static String mapType(String sourceInternalName) {
        Forge1201MappingCatalog.Decision decision = Forge1201MappingCatalog.classify(sourceInternalName);
        return decision.kind() == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE
                        || decision.kind() == Forge1201MappingCatalog.Kind.EXPLICIT_TYPE_REPLACEMENT
                ? decision.targetInternalName()
                : sourceInternalName;
    }

    private static Remapper descriptorRemapper() {
        return new Remapper() {
            @Override
            public String map(String internalName) {
                // Qualify the verifier helper explicitly: Remapper itself has an instance mapType()
                // that delegates back to map(), so an unqualified call here would recurse forever.
                return Forge1201TargetApiVerifier.mapType(internalName);
            }
        };
    }

    private static String mapMethodDescriptor(String descriptor) {
        return descriptorRemapper().mapMethodDesc(descriptor);
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
                    methods.add(new MemberKey(methodName, descriptor));
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            if (!internalName.equals(name[0])) {
                throw new IOException("Target class resource mismatch: requested " + internalName
                        + " but found " + name[0]);
            }
            TargetClass result = new TargetClass(
                    name[0], superName[0], List.copyOf(interfaces), Set.copyOf(methods), Set.copyOf(fields));
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
                .toList();
    }

    static void printIssue(MemberIssue issue) {
        System.out.println("  " + issue.display());
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
