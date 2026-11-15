package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.kylepitcock.legacyconnector.loader.mapping.SrgMojangTable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Offline bytecode inventory for a Forge 1.20.1 mod.
 *
 * <p>This tool does not load or execute mod classes. It inventories the Minecraft/Forge symbols
 * referenced by a jar so a version-specific adapter can be designed from evidence rather than guesses.</p>
 */
public final class Forge1201ApiUsageAnalyzer {
    private static final String FORGE_MOD_ANNOTATION = "Lnet/minecraftforge/fml/common/Mod;";
    private static final String NEOFORGE_MOD_ANNOTATION = "Lnet/neoforged/fml/common/Mod;";
    public static final long MAX_CLASS_ENTRY_BYTES = 8L * 1024 * 1024;
    public static final long MAX_OUTER_CLASS_BYTES = 64L * 1024 * 1024;

    private Forge1201ApiUsageAnalyzer() {}

    public record ApiMemberReference(String owner, String name, String descriptor, int count) {
        public String display() {
            return owner.replace('/', '.') + "#" + name + descriptor + (count > 1 ? "  (x" + count + ")" : "");
        }
    }

    /**
     * A direct bytecode invocation from a mod-owned method to a Minecraft, Forge, or optional API method.
     * This preserves the caller method, unlike the global member inventory.
     */
    public record ApiMethodCallSite(
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor,
            int opcode,
            int count) {
        public String display() {
            return targetOwner.replace('/', '.') + "#" + targetName + targetDescriptor
                    + " <= " + callerClass.replace('/', '.') + "#" + callerMethod + callerDescriptor
                    + " [" + opcodeName(opcode) + "]"
                    + (count > 1 ? " (x" + count + ")" : "");
        }
    }

    public record ApiFieldCallSite(
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor,
            int opcode,
            int count) {
        public String display() {
            return targetOwner.replace('/', '.') + "#" + targetName + ":" + targetDescriptor
                    + " <= " + callerClass.replace('/', '.') + "#" + callerMethod + callerDescriptor
                    + " [" + opcodeName(opcode) + "]"
                    + (count > 1 ? " (x" + count + ")" : "");
        }
    }

    public enum ApiDynamicReferenceKind {
        INVOKEDYNAMIC_SITE,
        METHOD_HANDLE,
        FIELD_HANDLE,
        CLASS_LITERAL,
        METHOD_TYPE,
        CONSTANT_DYNAMIC,
        VALUE_DEPTH_LIMIT
    }

    /**
     * A contextual reference carried by an invokedynamic instruction, LDC value,
     * method handle, or ConstantDynamic. instructionOrdinal is the zero-based
     * position among bytecode instructions, not a byte offset.
     */
    public record ApiDynamicReference(
            String archivePath,
            String classEntryPath,
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            ApiDynamicReferenceKind kind,
            String context,
            String owner,
            String name,
            String descriptor,
            int handleTag,
            boolean interfaceOwner,
            int instructionOrdinal,
            int count) {
        public String display() {
            String target = owner == null ? "" : owner.replace('/', '.') + "#";
            String caller = callerClass == null ? "<unknown-class>" : callerClass.replace('/', '.');
            if (callerMethod != null) caller += "#" + callerMethod
                    + (callerDescriptor == null ? "" : callerDescriptor);
            else caller += "#<class-context>";
            return target + (name == null ? "" : name)
                    + (descriptor == null ? "" : descriptor)
                    + " <= " + archivePath + "!/" + classEntryPath
                    + " :: " + caller
                    + " [" + kind + ", " + context
                    + ((kind == ApiDynamicReferenceKind.METHOD_HANDLE || kind == ApiDynamicReferenceKind.FIELD_HANDLE)
                            ? ", interfaceOwner=" + interfaceOwner : "")
                    + (instructionOrdinal >= 0
                            ? ", instruction " + instructionOrdinal : ", non-code context") + "]";
        }
    }

    private record DynamicReferenceKey(
            String archivePath,
            String classEntryPath,
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            ApiDynamicReferenceKind kind,
            String context,
            String owner,
            String name,
            String descriptor,
            int handleTag,
            boolean interfaceOwner,
            int instructionOrdinal) {}

    public enum ApiMemberKind { METHOD, FIELD }

    public enum SrgMappingStatus { MAPPED, UNMAPPED }

    public record ApiReferenceLocation(String archivePath, String classEntryPath) {}

    /** Source SRG name and its source-version Mojang name; this is not a target-version match. */
    public record ApiSrgMemberReference(
            ApiMemberKind kind,
            String owner,
            String sourceSrgName,
            String mojangName,
            String descriptor,
            int count,
            SrgMappingStatus mappingStatus,
            List<ApiReferenceLocation> sourceLocations) {
        public ApiSrgMemberReference {
            sourceLocations = List.copyOf(sourceLocations);
        }
    }


    private record CallSiteKey(
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor,
            int opcode) {}

    private record FieldCallSiteKey(
            String callerClass,
            String callerMethod,
            String callerDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor,
            int opcode) {}

    public record Analysis(
            Forge1201JarInfo metadata,
            int classCount,
            List<String> modEntrypoints,
            List<String> mixinConfigs,
            String coreModClass,
            Map<String, Integer> packageReferences,
            List<String> referencedApiClasses,
            List<ApiMethodCallSite> methodCallSites,
            List<ApiFieldCallSite> fieldCallSites,
            List<ApiMemberReference> methodReferences,
            List<ApiMemberReference> fieldReferences,
            List<ApiDynamicReference> dynamicReferences) {

        /** Source-compatible constructor for tools that create synthetic Analysis values. */
        public Analysis(
                Forge1201JarInfo metadata,
                int classCount,
                List<String> modEntrypoints,
                List<String> mixinConfigs,
                String coreModClass,
                Map<String, Integer> packageReferences,
                List<String> referencedApiClasses,
                List<ApiMethodCallSite> methodCallSites,
                List<ApiFieldCallSite> fieldCallSites,
                List<ApiMemberReference> methodReferences,
                List<ApiMemberReference> fieldReferences) {
            this(metadata, classCount, modEntrypoints, mixinConfigs, coreModClass,
                    packageReferences, referencedApiClasses, methodCallSites, fieldCallSites,
                    methodReferences, fieldReferences, List.of());
        }

        public Analysis {
            modEntrypoints = List.copyOf(modEntrypoints);
            mixinConfigs = List.copyOf(mixinConfigs);
            packageReferences = Map.copyOf(packageReferences);
            referencedApiClasses = List.copyOf(referencedApiClasses);
            methodCallSites = List.copyOf(methodCallSites);
            fieldCallSites = List.copyOf(fieldCallSites);
            methodReferences = List.copyOf(methodReferences);
            fieldReferences = List.copyOf(fieldReferences);
            dynamicReferences = List.copyOf(dynamicReferences);
        }
    }

    private record MemberKey(String owner, String name, String descriptor) {}

    private static final class ApiReferenceCollector extends Remapper {
        private final Set<String> classes = new LinkedHashSet<>();
        private final Map<String, Integer> packages = new TreeMap<>();
        private final Map<MemberKey, Integer> methods = new HashMap<>();
        private final Map<MemberKey, Integer> fields = new HashMap<>();
        private final Map<CallSiteKey, Integer> callSites = new HashMap<>();
        private final Map<FieldCallSiteKey, Integer> fieldCallSites = new HashMap<>();
        private final Map<DynamicReferenceKey, Integer> dynamicReferences = new HashMap<>();
        private final Map<MemberKey, Set<ApiReferenceLocation>> methodLocations = new HashMap<>();
        private final Map<MemberKey, Set<ApiReferenceLocation>> fieldLocations = new HashMap<>();
        private String currentArchivePath;
        private String currentClassEntryPath;

        void beginClass(String archivePath, String classEntryPath) {
            currentArchivePath = archivePath;
            currentClassEntryPath = classEntryPath;
        }

        private void recordLocation(Map<MemberKey, Set<ApiReferenceLocation>> locations, MemberKey key) {
            if (currentArchivePath == null || currentClassEntryPath == null) return;
            locations.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                    .add(new ApiReferenceLocation(currentArchivePath, currentClassEntryPath));
        }

        private void recordDynamic(
                String archivePath,
                String classEntryPath,
                String callerClass,
                String callerMethod,
                String callerDescriptor,
                ApiDynamicReferenceKind kind,
                String context,
                String owner,
                String name,
                String descriptor,
                int handleTag,
                boolean interfaceOwner,
                int instructionOrdinal) {
            DynamicReferenceKey key = new DynamicReferenceKey(
                    archivePath, classEntryPath, callerClass, callerMethod, callerDescriptor,
                    kind, context, owner, name, descriptor, handleTag, interfaceOwner, instructionOrdinal);
            dynamicReferences.merge(key, 1, Integer::sum);
        }

        private void recordHandle(
                String archivePath,
                String classEntryPath,
                String callerClass,
                String callerMethod,
                String callerDescriptor,
                String context,
                org.objectweb.asm.Handle handle,
                int instructionOrdinal) {
            boolean fieldHandle = switch (handle.getTag()) {
                case Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC -> true;
                default -> false;
            };
            if (!isForgeOrOptionalApi(handle.getOwner()) && !isGameApi(handle.getOwner())) return;
            recordDynamic(archivePath, classEntryPath, callerClass, callerMethod, callerDescriptor,
                    fieldHandle ? ApiDynamicReferenceKind.FIELD_HANDLE : ApiDynamicReferenceKind.METHOD_HANDLE,
                    context, handle.getOwner(), handle.getName(), handle.getDesc(),
                    handle.getTag(), handle.isInterface(), instructionOrdinal);
        }

        private void recordDynamicValue(
                String archivePath,
                String classEntryPath,
                String callerClass,
                String callerMethod,
                String callerDescriptor,
                String context,
                Object value,
                int instructionOrdinal,
                int depth) {
            if (value == null) return;
            if (depth > 16) {
                recordDynamic(archivePath, classEntryPath, callerClass, callerMethod, callerDescriptor,
                        ApiDynamicReferenceKind.VALUE_DEPTH_LIMIT, context, null, null, null,
                        -1, false, instructionOrdinal);
                return;
            }
            if (value instanceof org.objectweb.asm.Handle handle) {
                recordHandle(archivePath, classEntryPath, callerClass, callerMethod,
                        callerDescriptor, context, handle, instructionOrdinal);
                return;
            }
            if (value instanceof org.objectweb.asm.Type type) {
                if (type.getSort() == org.objectweb.asm.Type.METHOD) {
                    if (containsGameApiType(type)) {
                        recordDynamic(archivePath, classEntryPath, callerClass, callerMethod, callerDescriptor,
                                ApiDynamicReferenceKind.METHOD_TYPE, context, null, null,
                                type.getDescriptor(), -1, false, instructionOrdinal);
                    }
                } else {
                    org.objectweb.asm.Type element = type.getSort() == org.objectweb.asm.Type.ARRAY
                            ? type.getElementType() : type;
                    if (element.getSort() == org.objectweb.asm.Type.OBJECT
                            && (isGameApi(element.getInternalName()) || isForgeOrOptionalApi(element.getInternalName()))) {
                        recordDynamic(archivePath, classEntryPath, callerClass, callerMethod, callerDescriptor,
                                ApiDynamicReferenceKind.CLASS_LITERAL, context, element.getInternalName(), null,
                                type.getDescriptor(), -1, false, instructionOrdinal);
                    }
                }
                return;
            }
            if (value instanceof org.objectweb.asm.ConstantDynamic constant) {
                recordDynamic(archivePath, classEntryPath, callerClass, callerMethod, callerDescriptor,
                        ApiDynamicReferenceKind.CONSTANT_DYNAMIC, context, null, constant.getName(),
                        constant.getDescriptor(), -1, false, instructionOrdinal);
                recordHandle(archivePath, classEntryPath, callerClass, callerMethod, callerDescriptor,
                        context + "/bootstrap", constant.getBootstrapMethod(), instructionOrdinal);
                for (int i = 0; i < constant.getBootstrapMethodArgumentCount(); i++) {
                    recordDynamicValue(archivePath, classEntryPath, callerClass, callerMethod,
                            callerDescriptor, context + "/constant-argument-" + i,
                            constant.getBootstrapMethodArgument(i), instructionOrdinal, depth + 1);
                }
            }
        }

        private static boolean containsGameApiType(org.objectweb.asm.Type type) {
            if (type.getSort() == org.objectweb.asm.Type.METHOD) {
                if (containsGameApiType(type.getReturnType())) return true;
                for (org.objectweb.asm.Type argument : type.getArgumentTypes()) {
                    if (containsGameApiType(argument)) return true;
                }
                return false;
            }
            if (type.getSort() == org.objectweb.asm.Type.ARRAY) return containsGameApiType(type.getElementType());
            return type.getSort() == org.objectweb.asm.Type.OBJECT
                    && (isGameApi(type.getInternalName()) || isForgeOrOptionalApi(type.getInternalName()));
        }

        private void recordClass(String internalName) {
            if (!isGameApi(internalName)) return;
            classes.add(internalName);
            String[] pieces = internalName.split("/");
            String packageName = String.join("/", java.util.Arrays.copyOf(pieces, Math.min(3, pieces.length)));
            packages.merge(packageName, 1, Integer::sum);
        }

        @Override
        public String map(String internalName) {
            recordClass(internalName);
            return internalName;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            // ClassRemapper's member-name callback is also an authoritative reference to the owner.
            // Record it here rather than relying on the order in which ASM visits descriptors and instructions.
            recordClass(owner);
            if (isGameApi(owner)) {
                MemberKey key = new MemberKey(owner, name, descriptor);
                methods.merge(key, 1, Integer::sum);
                recordLocation(methodLocations, key);
            }
            return name;
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            recordClass(owner);
            if (isGameApi(owner)) {
                MemberKey key = new MemberKey(owner, name, descriptor);
                fields.merge(key, 1, Integer::sum);
                recordLocation(fieldLocations, key);
            }
            return name;
        }

        private void recordCallSite(
                String callerClass,
                String callerMethod,
                String callerDescriptor,
                int opcode,
                String targetOwner,
                String targetName,
                String targetDescriptor) {
            if (!isGameApi(targetOwner) && !isForgeOrOptionalApi(targetOwner)) return;
            CallSiteKey key = new CallSiteKey(
                    callerClass, callerMethod, callerDescriptor,
                    targetOwner, targetName, targetDescriptor, opcode);
            callSites.merge(key, 1, Integer::sum);
        }

        private void recordFieldCallSite(
                String callerClass,
                String callerMethod,
                String callerDescriptor,
                int opcode,
                String targetOwner,
                String targetName,
                String targetDescriptor) {
            if (!isGameApi(targetOwner) && !isForgeOrOptionalApi(targetOwner)) return;
            FieldCallSiteKey key = new FieldCallSiteKey(
                    callerClass, callerMethod, callerDescriptor,
                    targetOwner, targetName, targetDescriptor, opcode);
            fieldCallSites.merge(key, 1, Integer::sum);
        }

        private static boolean isForgeOrOptionalApi(String name) {
            return name.startsWith("net/minecraftforge/")
                    || name.startsWith("legacyconnector/v1_20_1/net/minecraftforge/")
                    || name.startsWith("net/neoforged/")
                    || name.startsWith("top/theillusivec4/curios/")
                    || name.startsWith("com/illusivesoulworks/")
                    || name.startsWith("com/gelred/");
        }

        private static boolean isGameApi(String name) {
            return name.startsWith("net/minecraft/")
                    || name.startsWith("net/minecraftforge/")
                    || name.startsWith("legacyconnector/v1_20_1/net/minecraftforge/")
                    || name.startsWith("net/neoforged/")
                    || name.startsWith("com/mojang/blaze3d/")
                    || name.startsWith("com/mojang/math/")
                    || name.startsWith("org/spongepowered/asm/mixin/");
        }

        List<String> classList() {
            return classes.stream().sorted().toList();
        }

        Map<String, Integer> packageCounts() {
            return Map.copyOf(packages);
        }

        List<ApiMethodCallSite> methodCallSiteList() {
            return callSites.entrySet().stream()
                    .map(entry -> new ApiMethodCallSite(
                            entry.getKey().callerClass(),
                            entry.getKey().callerMethod(),
                            entry.getKey().callerDescriptor(),
                            entry.getKey().targetOwner(),
                            entry.getKey().targetName(),
                            entry.getKey().targetDescriptor(),
                            entry.getKey().opcode(),
                            entry.getValue()))
                    .sorted(Comparator.comparing(ApiMethodCallSite::targetOwner)
                            .thenComparing(ApiMethodCallSite::targetName)
                            .thenComparing(ApiMethodCallSite::targetDescriptor)
                            .thenComparing(ApiMethodCallSite::callerClass)
                            .thenComparing(ApiMethodCallSite::callerMethod)
                            .thenComparing(ApiMethodCallSite::callerDescriptor)
                            .thenComparingInt(ApiMethodCallSite::opcode))
                    .toList();
        }

        List<ApiFieldCallSite> fieldCallSiteList() {
            return fieldCallSites.entrySet().stream()
                    .map(entry -> new ApiFieldCallSite(
                            entry.getKey().callerClass(),
                            entry.getKey().callerMethod(),
                            entry.getKey().callerDescriptor(),
                            entry.getKey().targetOwner(),
                            entry.getKey().targetName(),
                            entry.getKey().targetDescriptor(),
                            entry.getKey().opcode(),
                            entry.getValue()))
                    .sorted(Comparator.comparing(ApiFieldCallSite::targetOwner)
                            .thenComparing(ApiFieldCallSite::targetName)
                            .thenComparing(ApiFieldCallSite::targetDescriptor)
                            .thenComparing(ApiFieldCallSite::callerClass)
                            .thenComparing(ApiFieldCallSite::callerMethod)
                            .thenComparing(ApiFieldCallSite::callerDescriptor)
                            .thenComparingInt(ApiFieldCallSite::opcode))
                    .toList();
        }

        List<ApiMemberReference> methodList() {
            return toReferences(methods);
        }

        List<ApiMemberReference> fieldList() {
            return toReferences(fields);
        }

        List<ApiSrgMemberReference> srgMemberReferenceList(Map<String, String> mappings) {
            List<ApiSrgMemberReference> result = new ArrayList<>();
            methods.forEach((key, count) -> {
                ApiSrgMemberReference normalized = normalizeSrgMemberReference(
                        ApiMemberKind.METHOD, key.owner(), key.name(), key.descriptor(), count,
                        methodLocations.getOrDefault(key, Set.of()).stream()
                                .sorted(Comparator.comparing(ApiReferenceLocation::archivePath)
                                        .thenComparing(ApiReferenceLocation::classEntryPath)).toList(),
                        mappings);
                if (normalized != null) result.add(normalized);
            });
            fields.forEach((key, count) -> {
                ApiSrgMemberReference normalized = normalizeSrgMemberReference(
                        ApiMemberKind.FIELD, key.owner(), key.name(), key.descriptor(), count,
                        fieldLocations.getOrDefault(key, Set.of()).stream()
                                .sorted(Comparator.comparing(ApiReferenceLocation::archivePath)
                                        .thenComparing(ApiReferenceLocation::classEntryPath)).toList(),
                        mappings);
                if (normalized != null) result.add(normalized);
            });
            return result.stream().sorted(Comparator.comparing((ApiSrgMemberReference value) -> value.kind().name())
                    .thenComparing(ApiSrgMemberReference::owner)
                    .thenComparing(ApiSrgMemberReference::sourceSrgName)
                    .thenComparing(ApiSrgMemberReference::descriptor)).toList();
        }

        List<ApiDynamicReference> dynamicReferenceList() {
            return dynamicReferences.entrySet().stream()
                    .map(entry -> new ApiDynamicReference(
                            entry.getKey().archivePath(), entry.getKey().classEntryPath(),
                            entry.getKey().callerClass(), entry.getKey().callerMethod(),
                            entry.getKey().callerDescriptor(), entry.getKey().kind(),
                            entry.getKey().context(), entry.getKey().owner(), entry.getKey().name(),
                            entry.getKey().descriptor(), entry.getKey().handleTag(),
                            entry.getKey().interfaceOwner(), entry.getKey().instructionOrdinal(), entry.getValue()))
                    .sorted(Comparator.comparing(ApiDynamicReference::archivePath)
                            .thenComparing(ApiDynamicReference::classEntryPath)
                            .thenComparing(ApiDynamicReference::callerClass)
                            .thenComparing(reference -> reference.callerMethod() == null ? "" : reference.callerMethod())
                            .thenComparing(reference -> reference.callerDescriptor() == null ? "" : reference.callerDescriptor())
                            .thenComparing(reference -> reference.kind().name())
                            .thenComparing(ApiDynamicReference::context)
                            .thenComparing(reference -> reference.owner() == null ? "" : reference.owner())
                            .thenComparing(reference -> reference.name() == null ? "" : reference.name())
                            .thenComparing(reference -> reference.descriptor() == null ? "" : reference.descriptor())
                            .thenComparingInt(ApiDynamicReference::handleTag)
                            .thenComparing(ApiDynamicReference::interfaceOwner)
                            .thenComparingInt(ApiDynamicReference::instructionOrdinal))
                    .toList();
        }

        private static List<ApiMemberReference> toReferences(Map<MemberKey, Integer> source) {
            return source.entrySet().stream()
                    .map(entry -> new ApiMemberReference(
                            entry.getKey().owner(),
                            entry.getKey().name(),
                            entry.getKey().descriptor(),
                            entry.getValue()))
                    .sorted(Comparator.comparingInt(ApiMemberReference::count).reversed()
                            .thenComparing(ApiMemberReference::owner)
                            .thenComparing(ApiMemberReference::name)
                            .thenComparing(ApiMemberReference::descriptor))
                    .toList();
        }
    }

    public static Analysis analyze(Path jarPath) throws IOException {
        Forge1201JarInfo metadata = Forge1201JarInspector.inspectForge1201(jarPath);
        if (metadata == null) {
            throw new IllegalArgumentException("The file is not recognized as a Forge 1.20.1 mod jar: " + jarPath);
        }
        Inventory inventory = new Inventory(FORGE_MOD_ANNOTATION);
        inventory.scanOuterJar(jarPath);
        return inventory.finish(metadata);
    }

    /**
     * Inventories the output of the preliminary Forge 1.20.1 -> NeoForge 1.21.1
     * transformer. The returned Analysis has null source metadata because the input
     * is expected to contain neoforge.mods.toml rather than mods.toml.
     */
    public static Analysis analyzeTranslated(Path jarPath) throws IOException {
        if (jarPath == null || !java.nio.file.Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("Translated JAR does not exist: " + jarPath);
        }
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            if (zip.getEntry("META-INF/neoforge.mods.toml") == null
                    || zip.getEntry("META-INF/mods.toml") != null) {
                throw new IllegalArgumentException(
                        "Input must be a metadata-translated JAR containing "
                                + "META-INF/neoforge.mods.toml");
            }
        }
        Inventory inventory = new Inventory(NEOFORGE_MOD_ANNOTATION);
        inventory.scanOuterJar(jarPath);
        return inventory.finish(null);
    }

    /**
     * Reusable one-run scanner. The outer archive and source-nested archive class entries are
     * submitted to one ASM collector so aggregate references retain their original provenance.
     */
    public static final class Inventory {
        private final String modAnnotation;
        private final ApiReferenceCollector references = new ApiReferenceCollector();
        private final List<String> entrypoints = new ArrayList<>();
        private final List<String> mixinConfigs = new ArrayList<>();
        private final List<String> scanWarnings = new ArrayList<>();
        private String coreModClass;
        private int outerClassCount;
        private int outerClassScannedCount;
        private int nestedClassScannedCount;
        private long outerCapturedClassBytes;

        public Inventory() {
            this(FORGE_MOD_ANNOTATION);
        }

        public Inventory(String modAnnotation) {
            this.modAnnotation = java.util.Objects.requireNonNull(modAnnotation, "modAnnotation");
        }

        /** Scans unique, bounded .class entries from the outer source JAR. */
        public void scanOuterJar(Path jarPath) throws IOException {
            String archivePath = "source/" + jarPath.getFileName();
            try (ZipFile zip = new ZipFile(jarPath.toFile())) {
                ZipEntry manifestEntry = zip.getEntry(JarFile.MANIFEST_NAME);
                if (manifestEntry != null) {
                    try (InputStream in = zip.getInputStream(manifestEntry)) {
                        Manifest manifest = new Manifest(in);
                        coreModClass = manifest.getMainAttributes().getValue("FMLCorePlugin");
                        String configs = manifest.getMainAttributes().getValue("MixinConfigs");
                        if (configs != null) {
                            for (String config : configs.split(",")) {
                                if (!config.isBlank()) mixinConfigs.add(config.trim());
                            }
                        }
                    }
                }

                Map<String, ZipEntry> classEntries = new TreeMap<>();
                Set<String> duplicateNames = new HashSet<>();
                int discovered = 0;
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".class")) {
                        continue;
                    }
                    discovered++;
                    String name = entry.getName();
                    if (!isSafeClassEntryPath(name)) {
                        scanWarnings.add(archivePath + "!/" + name + " skipped: unsafe class-entry path.");
                        continue;
                    }
                    if (duplicateNames.contains(name)) continue;
                    if (classEntries.putIfAbsent(name, entry) != null) {
                        classEntries.remove(name);
                        duplicateNames.add(name);
                        scanWarnings.add(archivePath + "!/" + name
                                + " is duplicated within the outer archive; all entries with this name were skipped.");
                    }
                }

                outerClassCount += discovered;
                int successfullyScannedClassEntries = 0;
                for (Map.Entry<String, ZipEntry> candidate : classEntries.entrySet()) {
                    ZipEntry entry = candidate.getValue();
                    String entryPath = candidate.getKey();
                    if (entry.getSize() > MAX_CLASS_ENTRY_BYTES) {
                        scanWarnings.add(archivePath + "!/" + entryPath + " skipped: class payload exceeds "
                                + MAX_CLASS_ENTRY_BYTES + " bytes.");
                        continue;
                    }
                    if (outerCapturedClassBytes >= MAX_OUTER_CLASS_BYTES) {
                        scanWarnings.add(archivePath + "!/" + entryPath
                                + " skipped: aggregate outer class-byte capture limit reached.");
                        continue;
                    }
                    byte[] bytes;
                    try (InputStream in = zip.getInputStream(entry)) {
                        bytes = readBounded(in, Math.min(MAX_CLASS_ENTRY_BYTES,
                                MAX_OUTER_CLASS_BYTES - outerCapturedClassBytes));
                    } catch (IOException tooLarge) {
                        scanWarnings.add(archivePath + "!/" + entryPath + " skipped: "
                                + conciseMessage(tooLarge));
                        continue;
                    }
                    outerCapturedClassBytes += bytes.length;
                    if (!scanClassBytes(archivePath, entryPath, bytes, true)) {
                        throw new IOException("Cannot analyze class " + entryPath + " in " + jarPath);
                    }
                    successfullyScannedClassEntries++;
                }
                outerClassScannedCount += successfullyScannedClassEntries;
                if (discovered > successfullyScannedClassEntries) {
                    // Individual skips are listed above; this line only supplies a concise archive-level cue.
                    scanWarnings.add(archivePath + ": outer class inventory is incomplete; see per-entry warnings.");
                }
            }
        }

        /** Receives one already-bounded nested class entry from Forge1201ArchiveGraph. */
        public boolean scanNestedClass(String archivePath, String classEntryPath, byte[] classBytes) {
            if (classBytes == null || classBytes.length > MAX_CLASS_ENTRY_BYTES) {
                scanWarnings.add(archivePath + "!/" + classEntryPath + " skipped: invalid or oversized class payload.");
                return false;
            }
            if (!isSafeClassEntryPath(classEntryPath)) {
                scanWarnings.add(archivePath + "!/" + classEntryPath + " skipped: unsafe class-entry path.");
                return false;
            }
            if (!scanClassBytes(archivePath, classEntryPath, classBytes, false)) return false;
            nestedClassScannedCount++;
            return true;
        }

        private boolean scanClassBytes(
                String archivePath, String classEntryPath, byte[] classBytes, boolean collectEntrypoint) {
            references.beginClass(archivePath, classEntryPath);
            try {
                ClassReader reader = new ClassReader(classBytes);
                reader.accept(new ClassRemapper(new ClassVisitor(Opcodes.ASM9) {
                    private String className;

                    private AnnotationVisitor recordingAnnotation(
                            String context, String callerClass, String callerMethod, String callerDescriptor) {
                        return recordingAnnotation(context, callerClass, callerMethod, callerDescriptor, -1);
                    }

                    private AnnotationVisitor recordingAnnotation(
                            String context, String callerClass, String callerMethod,
                            String callerDescriptor, int instructionOrdinal) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(String name, Object value) {
                                references.recordDynamicValue(archivePath, classEntryPath,
                                        callerClass, callerMethod, callerDescriptor,
                                        context + "/" + (name == null ? "<value>" : name),
                                        value, instructionOrdinal, 0);
                            }

                            @Override
                            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                                return recordingAnnotation(context + "/" + name + "@" + descriptor,
                                        callerClass, callerMethod, callerDescriptor, instructionOrdinal);
                            }

                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                return recordingAnnotation(context + "/" + (name == null ? "<array>" : name),
                                        callerClass, callerMethod, callerDescriptor, instructionOrdinal);
                            }
                        };
                    }

                    @Override
                    public void visit(int version, int access, String name, String signature,
                                      String superName, String[] interfaces) {
                        className = name;
                    }

                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        AnnotationVisitor values = recordingAnnotation(
                                "class-annotation:" + descriptor, className, null, null);
                        if (!collectEntrypoint || !modAnnotation.equals(descriptor)) return values;
                        return new AnnotationVisitor(Opcodes.ASM9, values) {
                            @Override
                            public void visit(String name, Object value) {
                                if ("value".equals(name) || "modid".equals(name)) {
                                    entrypoints.add(className + " (" + value + ")");
                                }
                                super.visit(name, value);
                            }
                        };
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(
                            int typeRef, org.objectweb.asm.TypePath typePath,
                            String descriptor, boolean visible) {
                        return recordingAnnotation(
                                "class-type-annotation:" + descriptor, className, null, null);
                    }

                    @Override
                    public FieldVisitor visitField(int access, String name, String descriptor,
                                                   String signature, Object value) {
                        return new FieldVisitor(Opcodes.ASM9) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                                return recordingAnnotation("field-annotation:" + name + ":" + annotationDescriptor,
                                        className, null, null);
                            }

                            @Override
                            public AnnotationVisitor visitTypeAnnotation(
                                    int typeRef, org.objectweb.asm.TypePath typePath,
                                    String annotationDescriptor, boolean visible) {
                                return recordingAnnotation(
                                        "field-type-annotation:" + name + ":" + annotationDescriptor,
                                        className, null, null);
                            }
                        };
                    }

                    @Override
                    public RecordComponentVisitor visitRecordComponent(
                            String name, String descriptor, String signature) {
                        return new RecordComponentVisitor(Opcodes.ASM9) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                                return recordingAnnotation(
                                        "record-component-annotation:" + name + ":" + annotationDescriptor,
                                        className, null, null);
                            }

                            @Override
                            public AnnotationVisitor visitTypeAnnotation(
                                    int typeRef, org.objectweb.asm.TypePath typePath,
                                    String annotationDescriptor, boolean visible) {
                                return recordingAnnotation(
                                        "record-component-type-annotation:" + name + ":" + annotationDescriptor,
                                        className, null, null);
                            }
                        };
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        String callerClass = className;
                        String callerMethod = name;
                        String callerDescriptor = descriptor;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                                return recordingAnnotation("method-annotation:" + annotationDescriptor,
                                        callerClass, callerMethod, callerDescriptor);
                            }

                            @Override
                            public AnnotationVisitor visitTypeAnnotation(
                                    int typeRef, org.objectweb.asm.TypePath typePath,
                                    String annotationDescriptor, boolean visible) {
                                return recordingAnnotation("method-type-annotation:" + annotationDescriptor,
                                        callerClass, callerMethod, callerDescriptor);
                            }

                            @Override
                            public AnnotationVisitor visitParameterAnnotation(
                                    int parameter, String annotationDescriptor, boolean visible) {
                                return recordingAnnotation("parameter-annotation:" + parameter + ":"
                                        + annotationDescriptor, callerClass, callerMethod, callerDescriptor);
                            }

                            @Override
                            public AnnotationVisitor visitInsnAnnotation(
                                    int typeRef, org.objectweb.asm.TypePath typePath,
                                    String annotationDescriptor, boolean visible) {
                                return recordingAnnotation("instruction-annotation:" + annotationDescriptor,
                                        callerClass, callerMethod, callerDescriptor,
                                        Math.max(0, instructionOrdinal - 1));
                            }

                            @Override
                            public AnnotationVisitor visitTryCatchAnnotation(
                                    int typeRef, org.objectweb.asm.TypePath typePath,
                                    String annotationDescriptor, boolean visible) {
                                return recordingAnnotation("try-catch-annotation:" + annotationDescriptor,
                                        callerClass, callerMethod, callerDescriptor);
                            }

                            @Override
                            public AnnotationVisitor visitLocalVariableAnnotation(
                                    int typeRef, org.objectweb.asm.TypePath typePath,
                                    org.objectweb.asm.Label[] start, org.objectweb.asm.Label[] end,
                                    int[] index, String annotationDescriptor, boolean visible) {
                                return recordingAnnotation("local-variable-annotation:" + annotationDescriptor,
                                        callerClass, callerMethod, callerDescriptor);
                            }

                            @Override
                            public AnnotationVisitor visitAnnotationDefault() {
                                return recordingAnnotation("annotation-default",
                                        callerClass, callerMethod, callerDescriptor);
                            }

                            private int instructionOrdinal;

                            private int nextInstruction() {
                                return instructionOrdinal++;
                            }

                            @Override public void visitInsn(int opcode) { nextInstruction(); }
                            @Override public void visitIntInsn(int opcode, int operand) { nextInstruction(); }
                            @Override public void visitVarInsn(int opcode, int var) { nextInstruction(); }
                            @Override public void visitTypeInsn(int opcode, String type) { nextInstruction(); }

                            @Override
                            public void visitMethodInsn(int opcode, String owner, String targetName,
                                                        String targetDescriptor, boolean isInterface) {
                                nextInstruction();
                                references.recordCallSite(callerClass, callerMethod, callerDescriptor,
                                        opcode, owner, targetName, targetDescriptor);
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String owner, String targetName,
                                                       String targetDescriptor) {
                                nextInstruction();
                                references.recordFieldCallSite(callerClass, callerMethod, callerDescriptor,
                                        opcode, owner, targetName, targetDescriptor);
                            }

                            @Override
                            public void visitInvokeDynamicInsn(String targetName, String targetDescriptor,
                                                               org.objectweb.asm.Handle bootstrapMethodHandle,
                                                               Object... bootstrapMethodArguments) {
                                int ordinal = nextInstruction();
                                references.recordDynamic(archivePath, classEntryPath,
                                        callerClass, callerMethod, callerDescriptor,
                                        ApiDynamicReferenceKind.INVOKEDYNAMIC_SITE, "instruction",
                                        null, targetName, targetDescriptor, -1, false, ordinal);
                                references.recordHandle(archivePath, classEntryPath,
                                        callerClass, callerMethod, callerDescriptor,
                                        "invokedynamic-bootstrap", bootstrapMethodHandle, ordinal);
                                for (int i = 0; i < bootstrapMethodArguments.length; i++) {
                                    references.recordDynamicValue(archivePath, classEntryPath,
                                            callerClass, callerMethod, callerDescriptor,
                                            "invokedynamic-argument-" + i,
                                            bootstrapMethodArguments[i], ordinal, 0);
                                }
                            }

                            @Override public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {
                                nextInstruction();
                            }

                            @Override
                            public void visitLdcInsn(Object value) {
                                int ordinal = nextInstruction();
                                references.recordDynamicValue(archivePath, classEntryPath,
                                        callerClass, callerMethod, callerDescriptor, "ldc", value, ordinal, 0);
                            }

                            @Override public void visitIincInsn(int var, int increment) { nextInstruction(); }

                            @Override
                            public void visitTableSwitchInsn(int min, int max,
                                                              org.objectweb.asm.Label defaultLabel,
                                                              org.objectweb.asm.Label... labels) {
                                nextInstruction();
                            }

                            @Override
                            public void visitLookupSwitchInsn(org.objectweb.asm.Label defaultLabel,
                                                              int[] keys, org.objectweb.asm.Label[] labels) {
                                nextInstruction();
                            }

                            @Override
                            public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
                                nextInstruction();
                            }
                        };
                    }

                }, references), 0);
                return true;
            } catch (RuntimeException malformedClass) {
                scanWarnings.add(archivePath + "!/" + classEntryPath
                        + " could not be parsed as bytecode: " + conciseMessage(malformedClass));
                return false;
            }
        }

        public int outerClassCount() { return outerClassCount; }
        public int outerClassScannedCount() { return outerClassScannedCount; }
        public int nestedClassScannedCount() { return nestedClassScannedCount; }
        public List<String> classScanWarnings() {
            return scanWarnings.stream().distinct().sorted().toList();
        }
        public List<ApiSrgMemberReference> normalizedSrgMemberReferences() {
            return references.srgMemberReferenceList(SrgMojangTable.load());
        }

        public Analysis finish(Forge1201JarInfo metadata) {
            return new Analysis(
                    metadata,
                    outerClassCount,
                    entrypoints.stream().distinct().sorted().toList(),
                    mixinConfigs.stream().distinct().sorted().toList(),
                    coreModClass,
                    references.packageCounts(),
                    references.classList(),
                    references.methodCallSiteList(),
                    references.fieldCallSiteList(),
                    references.methodList(),
                    references.fieldList(),
                    references.dynamicReferenceList());
        }
    }

    static ApiSrgMemberReference normalizeSrgMemberReference(
            ApiMemberKind kind,
            String owner,
            String sourceName,
            String descriptor,
            int count,
            List<ApiReferenceLocation> locations,
            Map<String, String> mappings) {
        if (owner == null || !owner.startsWith("net/minecraft/") || sourceName == null) return null;
        boolean correctSrgKind = kind == ApiMemberKind.METHOD
                ? sourceName.matches("^m_\\d+_$") : sourceName.matches("^f_\\d+_$");
        if (!correctSrgKind) return null;
        String mojangName = mappings.get(sourceName);
        return new ApiSrgMemberReference(kind, owner, sourceName, mojangName, descriptor, count,
                mojangName == null ? SrgMappingStatus.UNMAPPED : SrgMappingStatus.MAPPED,
                locations == null ? List.of() : locations);
    }

    private static boolean isSafeClassEntryPath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.startsWith("\\")
                || path.contains("\\") || path.matches("^[A-Za-z]:.*")) return false;
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) return false;
        }
        return true;
    }

    private static byte[] readBounded(InputStream in, long limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new IOException("class entry exceeds " + limit + " bytes");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private static String opcodeName(int opcode) {
        return switch (opcode) {
            case Opcodes.NOP -> "NOP";
            case Opcodes.ACONST_NULL -> "ACONST_NULL";
            case Opcodes.ICONST_M1 -> "ICONST_M1";
            case Opcodes.ICONST_0 -> "ICONST_0";
            case Opcodes.ICONST_1 -> "ICONST_1";
            case Opcodes.ICONST_2 -> "ICONST_2";
            case Opcodes.ICONST_3 -> "ICONST_3";
            case Opcodes.ICONST_4 -> "ICONST_4";
            case Opcodes.ICONST_5 -> "ICONST_5";
            case Opcodes.BIPUSH -> "BIPUSH";
            case Opcodes.SIPUSH -> "SIPUSH";
            case Opcodes.ILOAD -> "ILOAD";
            case Opcodes.LLOAD -> "LLOAD";
            case Opcodes.FLOAD -> "FLOAD";
            case Opcodes.DLOAD -> "DLOAD";
            case Opcodes.ALOAD -> "ALOAD";
            case Opcodes.ISTORE -> "ISTORE";
            case Opcodes.LSTORE -> "LSTORE";
            case Opcodes.FSTORE -> "FSTORE";
            case Opcodes.DSTORE -> "DSTORE";
            case Opcodes.ASTORE -> "ASTORE";
            case Opcodes.POP -> "POP";
            case Opcodes.POP2 -> "POP2";
            case Opcodes.DUP -> "DUP";
            case Opcodes.DUP_X1 -> "DUP_X1";
            case Opcodes.DUP_X2 -> "DUP_X2";
            case Opcodes.DUP2 -> "DUP2";
            case Opcodes.IADD -> "IADD";
            case Opcodes.LADD -> "LADD";
            case Opcodes.FADD -> "FADD";
            case Opcodes.DADD -> "DADD";
            case Opcodes.ISUB -> "ISUB";
            case Opcodes.LSUB -> "LSUB";
            case Opcodes.FSUB -> "FSUB";
            case Opcodes.DSUB -> "DSUB";
            case Opcodes.IMUL -> "IMUL";
            case Opcodes.LMUL -> "LMUL";
            case Opcodes.FMUL -> "FMUL";
            case Opcodes.DMUL -> "DMUL";
            case Opcodes.IRETURN -> "IRETURN";
            case Opcodes.LRETURN -> "LRETURN";
            case Opcodes.FRETURN -> "FRETURN";
            case Opcodes.DRETURN -> "DRETURN";
            case Opcodes.ARETURN -> "ARETURN";
            case Opcodes.RETURN -> "RETURN";
            case Opcodes.GETSTATIC -> "GETSTATIC";
            case Opcodes.PUTSTATIC -> "PUTSTATIC";
            case Opcodes.GETFIELD -> "GETFIELD";
            case Opcodes.PUTFIELD -> "PUTFIELD";
            case Opcodes.INVOKEVIRTUAL -> "INVOKEVIRTUAL";
            case Opcodes.INVOKESPECIAL -> "INVOKESPECIAL";
            case Opcodes.INVOKESTATIC -> "INVOKESTATIC";
            case Opcodes.INVOKEINTERFACE -> "INVOKEINTERFACE";
            case Opcodes.INVOKEDYNAMIC -> "INVOKEDYNAMIC";
            case Opcodes.NEW -> "NEW";
            case Opcodes.NEWARRAY -> "NEWARRAY";
            case Opcodes.ANEWARRAY -> "ANEWARRAY";
            case Opcodes.ARRAYLENGTH -> "ARRAYLENGTH";
            case Opcodes.ATHROW -> "ATHROW";
            case Opcodes.CHECKCAST -> "CHECKCAST";
            case Opcodes.INSTANCEOF -> "INSTANCEOF";
            case Opcodes.IFEQ -> "IFEQ";
            case Opcodes.IFNE -> "IFNE";
            case Opcodes.IFLT -> "IFLT";
            case Opcodes.IFGE -> "IFGE";
            case Opcodes.IFGT -> "IFGT";
            case Opcodes.IFLE -> "IFLE";
            case Opcodes.IF_ICMPEQ -> "IF_ICMPEQ";
            case Opcodes.IF_ICMPNE -> "IF_ICMPNE";
            case Opcodes.IF_ICMPLT -> "IF_ICMPLT";
            case Opcodes.IF_ICMPGE -> "IF_ICMPGE";
            case Opcodes.IF_ICMPGT -> "IF_ICMPGT";
            case Opcodes.IF_ICMPLE -> "IF_ICMPLE";
            case Opcodes.IF_ACMPEQ -> "IF_ACMPEQ";
            case Opcodes.IF_ACMPNE -> "IF_ACMPNE";
            case Opcodes.GOTO -> "GOTO";
            case Opcodes.IFNULL -> "IFNULL";
            case Opcodes.IFNONNULL -> "IFNONNULL";
            default -> "OPCODE_" + opcode;
        };
    }

    public static void print(Analysis analysis) {
        System.out.println("Forge 1.20.1 API inventory");
        if (analysis.metadata() == null) {
            System.out.println("Jar: translated NeoForge 1.21.1 artifact");
            System.out.println("Metadata: " + "META-INF/neoforge.mods.toml");
        } else {
            System.out.println("Jar: " + analysis.metadata().path().getFileName());
            System.out.println("Mods: " + analysis.metadata().mods().stream()
                    .map(mod -> mod.modId() + " " + mod.version() + " (" + mod.displayName() + ")")
                    .reduce((a, b) -> a + ", " + b).orElse("none"));
            System.out.println("Declared dependencies:");
            for (Forge1201JarInfo.Mod mod : analysis.metadata().mods()) {
                for (Forge1201JarInfo.Dependency dependency : mod.dependencies()) {
                    System.out.println("  " + (dependency.mandatory() ? "required " : "optional ")
                            + dependency.modId() + " " + dependency.versionRange()
                            + " [" + dependency.ordering() + ", " + dependency.side() + "]");
                }
            }
        }
        System.out.println("Classes scanned: " + analysis.classCount());
        System.out.println("Forge @Mod entrypoints: " + analysis.modEntrypoints());
        System.out.println("Mixin configs: " + (analysis.mixinConfigs().isEmpty() ? "none declared" : analysis.mixinConfigs()));
        System.out.println("Coremod class: " + (analysis.coreModClass() == null ? "none" : analysis.coreModClass()));

        System.out.println("\nReferenced API packages:");
        analysis.packageReferences().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> System.out.println("  " + entry.getKey().replace('/', '.') + ": " + entry.getValue()));

        System.out.println("\nReferenced API classes (" + analysis.referencedApiClasses().size() + "):");
        analysis.referencedApiClasses().forEach(name -> System.out.println("  " + name.replace('/', '.')));

        System.out.println("\nMinecraft / Forge / optional API method call sites (" + analysis.methodCallSites().size() + "):");
        analysis.methodCallSites().forEach(callSite -> System.out.println("  " + callSite.display()));

        System.out.println("\nMinecraft / Forge / optional API field access call sites (" + analysis.fieldCallSites().size() + "):");
        analysis.fieldCallSites().forEach(callSite -> System.out.println("  " + callSite.display()));

        System.out.println("\nMost-used API method references:");
        analysis.methodReferences().stream().limit(80).forEach(reference -> System.out.println("  " + reference.display()));

        System.out.println("\nMost-used API field references:");
        analysis.fieldReferences().stream().limit(50).forEach(reference -> System.out.println("  " + reference.display()));
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException(
                    "Usage: inspectForge1201 <path-to-forge-1.20.1-jar> [comma-separated-class#method-trace-filter]");
        }
        Path jarPath = Path.of(args[0]);
        print(analyze(jarPath));
        if (args.length == 2 && !args[1].isBlank()) {
            printSelectedMethodBytecode(jarPath, parseTraceFilter(args[1]));
        }
    }

    private static Set<String> parseTraceFilter(String value) {
        Set<String> result = new LinkedHashSet<>();
        for (String entry : value.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) result.add(trimmed.replace('/', '.'));
        }
        return Set.copyOf(result);
    }

    /**
     * Prints a compact ASM disassembly for explicitly selected methods. This is an inspection aid:
     * it does not execute or define classes and does not modify the input archive.
     */
    static void printSelectedMethodBytecode(Path jarPath, Set<String> selectedMethods) throws IOException {
        Set<String> found = new LinkedHashSet<>();
        System.out.println("\nSelected method bytecode traces (read-only ASM disassembly):");
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                try (var in = zip.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(in);
                    reader.accept(new ClassVisitor(Opcodes.ASM9) {
                        private String className;

                        @Override
                        public void visit(int version, int access, String name, String signature,
                                          String superName, String[] interfaces) {
                            className = name.replace('/', '.');
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                         String signature, String[] exceptions) {
                            String key = className + "#" + name;
                            if (!selectedMethods.contains(key) && !selectedMethods.contains(className + "#*")) {
                                return null;
                            }
                            found.add(key + descriptor);
                            return new MethodVisitor(Opcodes.ASM9) {
                                private final List<String> instructions = new ArrayList<>();
                                private final Map<org.objectweb.asm.Label, String> labels = new java.util.IdentityHashMap<>();
                                private int nextLabel;
                                private int index;

                                private String label(org.objectweb.asm.Label label) {
                                    return labels.computeIfAbsent(label, ignored -> "L" + nextLabel++);
                                }

                                private void add(String instruction) {
                                    instructions.add(String.format("%04d: %s", index++, instruction));
                                }

                                @Override
                                public void visitInsn(int opcode) {
                                    add(opcodeName(opcode));
                                }

                                @Override
                                public void visitIntInsn(int opcode, int operand) {
                                    add(opcodeName(opcode) + " " + operand);
                                }

                                @Override
                                public void visitVarInsn(int opcode, int var) {
                                    add(opcodeName(opcode) + " " + var);
                                }

                                @Override
                                public void visitTypeInsn(int opcode, String type) {
                                    add(opcodeName(opcode) + " " + type.replace('/', '.'));
                                }

                                @Override
                                public void visitFieldInsn(int opcode, String owner, String fieldName,
                                                           String fieldDescriptor) {
                                    add(opcodeName(opcode) + " " + owner.replace('/', '.') + "#"
                                            + fieldName + " " + fieldDescriptor);
                                }

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String methodName,
                                                            String methodDescriptor, boolean isInterface) {
                                    add(opcodeName(opcode) + " " + owner.replace('/', '.') + "#"
                                            + methodName + methodDescriptor);
                                }

                                @Override
                                public void visitInvokeDynamicInsn(String methodName, String methodDescriptor,
                                                                   org.objectweb.asm.Handle bootstrapMethodHandle,
                                                                   Object... bootstrapMethodArguments) {
                                    add("INVOKEDYNAMIC " + methodName + methodDescriptor
                                            + " bootstrap=" + bootstrapMethodHandle
                                            + " args=" + java.util.Arrays.toString(bootstrapMethodArguments));
                                }

                                @Override
                                public void visitJumpInsn(int opcode, org.objectweb.asm.Label target) {
                                    add(opcodeName(opcode) + " " + label(target));
                                }

                                @Override
                                public void visitLabel(org.objectweb.asm.Label label) {
                                    add(label(label) + ":");
                                }

                                @Override
                                public void visitLdcInsn(Object value) {
                                    add("LDC " + String.valueOf(value));
                                }

                                @Override
                                public void visitIincInsn(int var, int increment) {
                                    add("IINC " + var + " " + increment);
                                }

                                @Override
                                public void visitTableSwitchInsn(int min, int max,
                                                                 org.objectweb.asm.Label defaultLabel,
                                                                 org.objectweb.asm.Label... switchLabels) {
                                    List<String> targets = new ArrayList<>();
                                    for (org.objectweb.asm.Label target : switchLabels) targets.add(label(target));
                                    add("TABLESWITCH " + min + ".." + max + " default=" + label(defaultLabel)
                                            + " targets=" + targets);
                                }

                                @Override
                                public void visitLookupSwitchInsn(org.objectweb.asm.Label defaultLabel,
                                                                  int[] keys,
                                                                  org.objectweb.asm.Label[] switchLabels) {
                                    List<String> targets = new ArrayList<>();
                                    for (int i = 0; i < switchLabels.length; i++) {
                                        targets.add(keys[i] + ":" + label(switchLabels[i]));
                                    }
                                    add("LOOKUPSWITCH default=" + label(defaultLabel) + " targets=" + targets);
                                }

                                @Override
                                public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
                                    add("MULTIANEWARRAY " + descriptor + " " + dimensions);
                                }

                                @Override
                                public void visitEnd() {
                                    System.out.println("\n  " + key + descriptor);
                                    if (instructions.isEmpty()) {
                                        System.out.println("    [no instructions]");
                                    } else {
                                        instructions.forEach(instruction -> System.out.println("    " + instruction));
                                    }
                                }
                            };
                        }
                    }, 0);
                }
            }
        }

        for (String selected : selectedMethods) {
            boolean wildcard = selected.endsWith("#*");
            String prefix = wildcard ? selected.substring(0, selected.length() - 1) : selected + "(";
            boolean matched = found.stream().anyMatch(value -> value.startsWith(prefix));
            if (!matched) System.out.println("  NOT FOUND: " + selected);
        }
    }
}
