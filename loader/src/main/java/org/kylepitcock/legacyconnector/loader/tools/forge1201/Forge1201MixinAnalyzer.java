package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Bounded, read-only static Mixin configuration and target discovery for Forge 1.20.1 archives.
 *
 * <p>This implementation analyzes the outer source archive only. It resolves declared config
 * resources and mixin class entries, reads visible/invisible @Mixin and member annotations, and
 * inventories source-side handler and selector data for core injectors, including detailed @At
 * metadata, typed @ModifyConstant discriminators, @Slice boundaries, and bounded Mixin 0.8.5
 * refmap table contents. It checks mapped class targets through the shared target index but never
 * loads or executes mod classes, Mixin plugins, or custom refmap wrappers. Namespace conversion,
 * target instruction resolution, and injector handler stack validation remain later increments.
 * Unified preflight consumes this result API.</p>
 */
public final class Forge1201MixinAnalyzer {
    public static final int MAX_ARCHIVE_ENTRIES = 100_000;
    public static final int MAX_CONFIG_COUNT = 256;
    public static final int MAX_CONFIG_BYTES = 1024 * 1024;
    public static final long MAX_TOTAL_CONFIG_BYTES = 8L * 1024 * 1024;
    public static final int MAX_MIXIN_CLASS_COUNT = 20_000;
    public static final int MAX_CLASS_BYTES = 8 * 1024 * 1024;
    public static final long MAX_TOTAL_CLASS_BYTES = 128L * 1024 * 1024;
    public static final int MAX_REFMAP_RESOURCE_PATHS = 256;
    public static final int MAX_REFMAP_BYTES = 1024 * 1024;
    public static final long MAX_TOTAL_REFMAP_BYTES = 8L * 1024 * 1024;
    public static final int MAX_REFMAP_CONTEXTS_PER_RESOURCE = 64;
    public static final int MAX_REFMAP_OWNER_BUCKETS_PER_RESOURCE = 20_000;
    public static final int MAX_REFMAP_ENTRIES_PER_RESOURCE = 100_000;
    public static final int MAX_REFMAP_JSON_DEPTH = 128;
    public static final String DEFAULT_REFMAP_RESOURCE = "mixin.refmap.json";

    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW_ANNOTATION = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String OVERWRITE_ANNOTATION = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String ACCESSOR_ANNOTATION = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_ANNOTATION = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String INJECT_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String REDIRECT_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String MODIFY_ARG_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/ModifyArg;";
    private static final String MODIFY_ARGS_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;";
    private static final String MODIFY_VARIABLE_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;";
    private static final String MODIFY_CONSTANT_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;";
    private static final String DESC_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/Desc;";
    private static final String AT_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/At;";
    private static final String SLICE_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/Slice;";
    private static final String CONSTANT_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/Constant;";

    private Forge1201MixinAnalyzer() {}

    public enum DiscoverySource {
        MANIFEST,
        FORGE_METADATA,
        RESOURCE_HEURISTIC
    }

    public enum ConfigStatus {
        PARSED,
        CONFIG_MISSING,
        CONFIG_INVALID,
        CONFIG_LIMIT_EXCEEDED,
        CONFIG_DUPLICATE_ENTRY,
        UNSAFE_CONFIG_PATH
    }

    public enum MixinClassStatus {
        PRESENT,
        MISSING,
        INVALID_NAME,
        DUPLICATE_ENTRY,
        CLASS_LIMIT_EXCEEDED,
        INVALID_CLASS
    }

    public enum TargetSelectorForm {
        CLASS_LITERAL,
        STRING_TARGET
    }

    public enum TargetIndexStatus {
        AVAILABLE,
        UNAVAILABLE_NO_TARGET_CLASSPATH
    }

    public enum TargetStatus {
        TARGET_CLASS_PRESENT,
        TARGET_CLASS_MISSING,
        TARGET_REMAP_UNRESOLVED,
        TARGET_INDEX_UNAVAILABLE,
        NON_PLATFORM_TARGET
    }

    public enum ResourceStatus {
        NOT_DECLARED,
        PRESENT_VALID_JSON,
        MISSING,
        INVALID_PATH,
        DUPLICATE_ENTRY,
        LIMIT_EXCEEDED,
        INVALID_JSON,
        UNSUPPORTED_SHAPE,
        ARCHIVE_INDEX_INCOMPLETE
    }

    public enum PluginStatus {
        NOT_DECLARED,
        PRESENT_NOT_EXECUTED,
        MISSING_FROM_SOURCE_ARCHIVE,
        INVALID_CLASS_NAME,
        DUPLICATE_ENTRY,
        ARCHIVE_INDEX_INCOMPLETE
    }

    public enum RefmapSelection {
        NOT_ANALYZED,
        EXPLICIT_RESOURCE,
        DEFAULT_RESOURCE,
        PLUGIN_DEPENDENT,
        INVALID_DECLARATION
    }

    public enum RefmapResourceOrigin {
        EXPLICIT_CONFIG_FIELD,
        MIXIN_DEFAULT_CANDIDATE,
        PLUGIN_DEPENDENT_CANDIDATE
    }

    public enum RefmapResourceStatus {
        PARSED,
        EMPTY,
        MISSING,
        INVALID_PATH,
        DUPLICATE_ENTRY,
        LIMIT_EXCEEDED,
        INVALID_JSON,
        UNSUPPORTED_SHAPE,
        ARCHIVE_INDEX_INCOMPLETE
    }

    public enum RefmapTableKind {
        BASE_MAPPINGS,
        CONTEXT
    }

    public enum RefmapLookupSource {
        INJECTOR_METHOD, INJECTOR_DESC_TARGET, AT_TARGET, AT_DESC_TARGET, AT_NEW_CLASS_ARGUMENT
    }
    public enum RefmapCandidateStatus {
        ENTRY_FOUND, ENTRY_NOT_FOUND_PASSTHROUGH, EXPLICIT_IDENTITY_ENTRY, TABLE_NOT_INVENTORIED
    }
    public enum RefmapContextBehavior {
        CONTEXT_TABLE_SELECTED, CONTEXT_MISSING_DEFAULT_FALLBACK, CONTEXT_NOT_SELECTED, NOT_EVALUATED
    }
    public enum RefmapLookupStatus {
        ENTRY_FOUND, ENTRY_NOT_FOUND_PASSTHROUGH, EXPLICIT_IDENTITY_ENTRY,
        CONTEXT_NOT_SELECTED, CONTEXT_DEPENDENT_VALUES, OWNER_UNAVAILABLE, PLUGIN_DEPENDENT,
        SELECTOR_NOT_EMITTED_AS_REF, ANALYSIS_INCOMPLETE, REMAP_DISABLED
    }
    public record RefmapLookupMatch(String referenceKey, String mappedReferenceValue) {}
    public record RefmapLookupCandidate(
            RefmapTableKind tableKind, String contextKey, RefmapCandidateStatus status,
            String mappedReferenceValue, List<RefmapLookupMatch> matches) {
        public RefmapLookupCandidate { matches = List.copyOf(matches); }
    }
    /** Exact source-owner/reference correlation only; no namespace or target/runtime verdict is implied. */
    public record RefmapLookupFinding(
            String archivePath, String configPath, String section, String mixinClassName,
            String mixinInternalName, RefmapLookupSource source, String sourceLocation,
            String sourceMemberName, String sourceMemberDescriptor, String rawReference,
            String normalizedReference, boolean remap, String resourcePath,
            String requestedContextKey, RefmapContextBehavior contextBehavior,
            String selectedContextKey, RefmapLookupStatus status, String mappedReferenceValue,
            List<RefmapLookupCandidate> candidates, String evidence, List<String> warnings) {
        public RefmapLookupFinding {
            candidates = List.copyOf(candidates);
            warnings = List.copyOf(warnings);
        }
    }

    public record ConfigDeclaration(String path, DiscoverySource source) {
        public ConfigDeclaration {
            if (path == null || path.isBlank()) throw new IllegalArgumentException("Config path is required");
            if (source == null) throw new IllegalArgumentException("Config discovery source is required");
        }
    }

    public record ConfigFinding(
            String archivePath,
            String configPath,
            List<DiscoverySource> discoverySources,
            ConfigStatus status,
            String packageName,
            Boolean required,
            String minimumVersion,
            String compatibilityLevel,
            String pluginClass,
            PluginStatus pluginStatus,
            String refmapPath,
            String refmapWrapperClass,
            RefmapSelection refmapSelection,
            String refmapCandidatePath,
            ResourceStatus refmapStatus,
            List<String> warnings) {
        public ConfigFinding {
            discoverySources = List.copyOf(discoverySources);
            warnings = List.copyOf(warnings);
        }
    }

    public record RefmapEntryFinding(
            String ownerInternalName,
            String referenceKey,
            String mappedReferenceValue) {}

    public record RefmapTableFinding(
            RefmapTableKind kind,
            String contextKey,
            int ownerBucketCount,
            int entryCount,
            List<RefmapEntryFinding> entries,
            List<String> warnings) {
        public RefmapTableFinding {
            entries = List.copyOf(entries);
            warnings = List.copyOf(warnings);
        }
    }

    public record RefmapResourceFinding(
            String archivePath,
            String resourcePath,
            List<String> configPaths,
            List<RefmapResourceOrigin> origins,
            RefmapResourceStatus status,
            long declaredSizeBytes,
            long compressedSizeBytes,
            long inflatedSizeBytes,
            String sha256,
            boolean mappingsPresent,
            boolean dataPresent,
            List<String> contextKeys,
            int ownerBucketCount,
            int entryCount,
            List<String> unknownTopLevelFields,
            List<RefmapTableFinding> tables,
            List<String> warnings) {
        public RefmapResourceFinding {
            configPaths = List.copyOf(configPaths);
            origins = List.copyOf(origins);
            contextKeys = List.copyOf(contextKeys);
            unknownTopLevelFields = List.copyOf(unknownTopLevelFields);
            tables = List.copyOf(tables);
            warnings = List.copyOf(warnings);
        }
    }

    public record MixinClassFinding(
            String archivePath,
            String configPath,
            String section,
            String mixinClassName,
            String mixinInternalName,
            String classEntryPath,
            MixinClassStatus status,
            boolean hasMixinAnnotation,
            List<String> warnings) {
        public MixinClassFinding { warnings = List.copyOf(warnings); }
        public MixinClassFinding(
                String archivePath, String configPath, String section, String mixinClassName,
                String classEntryPath, MixinClassStatus status, boolean hasMixinAnnotation,
                List<String> warnings) {
            this(archivePath, configPath, section, mixinClassName, null,
                    classEntryPath, status, hasMixinAnnotation, warnings);
        }
    }

    public record TargetFinding(
            String archivePath,
            String configPath,
            String mixinClassName,
            String sourceTargetInternalName,
            String targetInternalName,
            TargetSelectorForm selectorForm,
            boolean remap,
            boolean pluginDependent,
            TargetStatus status,
            String mappingVersion,
            String evidence) {}

    public enum MemberAnnotationKind {
        SHADOW,
        OVERWRITE,
        ACCESSOR,
        INVOKER
    }

    public enum MemberElementKind {
        FIELD,
        METHOD
    }

    /**
     * This first member-annotation increment records exact source classfile evidence only.
     * None of these states claims that the target member exists or that the mixin will apply.
     */
    public enum MemberResolutionStatus {
        MEMBER_REMAP_UNRESOLVED,
        MEMBER_REMAP_DISABLED_UNVERIFIED,
        PLUGIN_DEPENDENT,
        INVALID_ANNOTATION_PLACEMENT,
        INVALID_ANNOTATION_VALUE,
        INVALID_MEMBER_DESCRIPTOR,
        INVALID_MEMBER_SELECTOR
    }

    public record MemberAnnotationFinding(
            String archivePath,
            String configPath,
            String section,
            String mixinClassName,
            List<String> sourceTargetInternalNames,
            MemberAnnotationKind annotationKind,
            MemberElementKind elementKind,
            String sourceMemberName,
            String sourceMemberDescriptor,
            String prefix,
            List<String> aliases,
            String explicitValue,
            String constraints,
            List<String> targetMemberNameCandidates,
            boolean remap,
            String operation,
            MemberResolutionStatus status,
            String evidence,
            List<String> warnings) {
        public MemberAnnotationFinding {
            sourceTargetInternalNames = List.copyOf(sourceTargetInternalNames);
            aliases = List.copyOf(aliases);
            targetMemberNameCandidates = List.copyOf(targetMemberNameCandidates);
            warnings = List.copyOf(warnings);
        }
    }

    public enum InjectorAnnotationKind {
        INJECT, REDIRECT, MODIFY_ARG, MODIFY_ARGS, MODIFY_VARIABLE, MODIFY_CONSTANT
    }

    public enum InjectorSelectorSource { METHOD_STRING, DESC_ANNOTATION }

    public enum InjectorSelectorStatus {
        METHOD_NAME_ONLY, METHOD_DESCRIPTOR, WILDCARD, OWNER_ONLY,
        INVALID_DESCRIPTOR, INVALID_SELECTOR, UNSUPPORTED_EXTENSION
    }

    public enum InjectorResolutionStatus {
        INJECTOR_REMAP_UNRESOLVED, INJECTOR_REMAP_DISABLED_UNVERIFIED, PLUGIN_DEPENDENT,
        UNSUPPORTED_SELECTOR_SYNTAX, INVALID_ANNOTATION_PLACEMENT,
        INVALID_HANDLER_DESCRIPTOR, INVALID_INJECTOR_SELECTOR
    }

    /** Captured source selector; this does not assert that the target exists. */
    public record InjectorTargetSelector(
            InjectorSelectorSource source, String rawSelector, String ownerInternalName,
            String memberName, String descriptor, String quantifier,
            Integer minimumMatches, Integer maximumMatches,
            InjectorSelectorStatus status, String evidence) {}

    public enum AtTargetDescriptorKind { NONE, METHOD, FIELD }

    public enum AtTargetSelectorStatus {
        NOT_DECLARED, MEMBER_NAME_ONLY, METHOD_DESCRIPTOR, FIELD_DESCRIPTOR,
        OWNER_ONLY, INVALID_DESCRIPTOR, INVALID_SELECTOR, UNSUPPORTED_SYNTAX
    }

    /** Parsed At.target selector syntax only; it never asserts a target member exists. */
    public record AtTargetSelector(
            String rawSelector, String ownerInternalName, String memberName,
            String descriptor, AtTargetDescriptorKind descriptorKind,
            AtTargetSelectorStatus status, String evidence) {}

    public enum AtValueKind { BUILT_IN, CUSTOM_OR_UNKNOWN }

    public enum AtFindingStatus {
        CAPTURED, INVALID_REQUIRED_VALUE, INVALID_TARGET_SELECTOR, INVALID_DESC_SELECTOR
    }

    /** One source-side @At declaration, including effective Mixin 0.8.5 defaults. */
    public record AtFinding(
            int index, String value, AtValueKind valueKind, String rawTarget,
            AtTargetSelector targetSelector, InjectorTargetSelector descSelector,
            int opcode, int ordinal, String shift, int shiftBy, String sliceId,
            List<String> args, boolean remap, String id,
            AtFindingStatus status, String evidence, List<String> warnings) {
        public AtFinding {
            args = List.copyOf(args);
            warnings = List.copyOf(warnings);
        }
    }

    public enum SliceBoundaryKind { FROM, TO }

    public enum SliceBoundaryStatus {
        EXPLICIT_AT_CAPTURED, DEFAULT_AT_CAPTURED, INVALID_AT,
        UNSUPPORTED_NESTED_ANNOTATION, INVALID_VALUE_TYPE
    }

    /** One @Slice boundary, with explicit-versus-Mixin-default provenance. */
    public record SliceBoundaryFinding(
            SliceBoundaryKind kind, boolean declared, String annotationDescriptor,
            AtFinding at, SliceBoundaryStatus status, String evidence, List<String> warnings) {
        public SliceBoundaryFinding {
            warnings = List.copyOf(warnings);
        }
    }

    public enum SliceFindingStatus {
        CAPTURED, BOUNDARY_REQUIRES_REVIEW, UNSUPPORTED_NESTED_ANNOTATION, INVALID_NESTED_VALUE
    }

    /** Source-side @Slice declaration; capture does not prove instruction ordering or validity. */
    public record SliceFinding(
            int index, String id, String annotationDescriptor,
            SliceBoundaryFinding from, SliceBoundaryFinding to,
            SliceFindingStatus status, String evidence, List<String> warnings) {
        public SliceFinding {
            warnings = List.copyOf(warnings);
        }
    }

    public enum ConstantFindingStatus {
        MATCH_HANDLER_RETURN_TYPE, EXPLICIT_TYPED_DISCRIMINATOR,
        CONFLICTING_DISCRIMINATORS, INVALID_ENUM_VALUE, UNSUPPORTED_NESTED_ANNOTATION
    }

    /** One @ModifyConstant discriminator; declared=false represents Mixin's implicit default. */
    public record ConstantFinding(
            int index, boolean declared, Boolean nullValue, Integer intValue,
            Float floatValue, Long longValue, Double doubleValue, String stringValue,
            String classValueDescriptor, int ordinal, String sliceId,
            List<String> expandZeroConditions, Boolean log,
            ConstantFindingStatus status, String evidence, List<String> warnings) {
        public ConstantFinding {
            expandZeroConditions = List.copyOf(expandZeroConditions);
            warnings = List.copyOf(warnings);
        }
    }

    /** Source-side injector metadata; no target mapping or injection success is implied. */
    public record InjectorFinding(
            String archivePath, String configPath, String section, String mixinClassName,
            String mixinInternalName, List<String> sourceTargetInternalNames,
            InjectorAnnotationKind annotationKind,
            MemberElementKind elementKind, String sourceMemberName, String sourceMemberDescriptor,
            List<InjectorTargetSelector> targetSelectors, List<AtFinding> injectionPoints,
            List<ConstantFinding> constants, List<SliceFinding> slices, boolean remap,
            int atAnnotationCount, int sliceAnnotationCount, Integer index,
            Boolean cancellable, Boolean argsOnly, int require, int expect, int allow,
            String constraints, InjectorResolutionStatus status, String evidence, List<String> warnings) {
        public InjectorFinding {
            sourceTargetInternalNames = List.copyOf(sourceTargetInternalNames);
            targetSelectors = List.copyOf(targetSelectors);
            injectionPoints = List.copyOf(injectionPoints);
            constants = List.copyOf(constants);
            slices = List.copyOf(slices);
            warnings = List.copyOf(warnings);
        }
    }

    public record Result(
            String sourceArchive,
            String targetProfile,
            String classMappingVersion,
            TargetIndexStatus targetIndexStatus,
            List<ConfigFinding> configs,
            List<MixinClassFinding> mixinClasses,
            List<TargetFinding> targets,
            List<MemberAnnotationFinding> memberAnnotations,
            List<InjectorFinding> injectors,
            List<RefmapResourceFinding> refmapResources,
            String requestedRefmapContextKey,
            List<RefmapLookupFinding> refmapLookups,
            List<String> warnings) {
        public Result {
            configs = List.copyOf(configs);
            mixinClasses = List.copyOf(mixinClasses);
            targets = List.copyOf(targets);
            memberAnnotations = List.copyOf(memberAnnotations);
            injectors = List.copyOf(injectors);
            refmapResources = List.copyOf(refmapResources);
            refmapLookups = List.copyOf(refmapLookups);
            warnings = List.copyOf(warnings);
        }
    }

    private record IndexedEntry(ZipEntry entry, int occurrences) {}

    private record ParsedConfig(
            String packageName,
            Boolean required,
            String minimumVersion,
            String compatibilityLevel,
            String pluginClass,
            String refmapPath,
            String refmapWrapperClass,
            boolean refmapFieldPresent,
            boolean refmapFieldValid,
            Map<String, List<String>> mixins,
            List<String> warnings) {}

    private record ParsedTarget(String selector, TargetSelectorForm form) {}

    private static final class RefmapCandidateAccumulator {
        private final Set<String> configPaths = new TreeSet<>();
        private final Set<RefmapResourceOrigin> origins = new TreeSet<>();
    }

    private static final class RefmapBudget {
        private long inflatedBytes;
    }

    private static final class RefmapParseState {
        private int ownerBucketCount;
        private int entryCount;
        private boolean limitExceeded;
        private boolean unsupportedShape;
        private final List<String> warnings = new ArrayList<>();
    }

    /**
     * Analyze declared and heuristic config candidates. Minecraft class targets are only checked
     * when the caller supplies an explicit source-to-target class mapping. An absent mapping is
     * unresolved, never silently treated as an identity mapping.
     */
    public static Result analyze(
            Path sourceJar,
            Collection<ConfigDeclaration> declarations,
            Forge1201TargetApiVerifier.TargetIndex targetIndex,
            Map<String, String> sourceToTargetClassMappings,
            String classMappingVersion) throws IOException {
        return analyze(sourceJar, declarations, targetIndex, sourceToTargetClassMappings,
                classMappingVersion, null);
    }

    /** Null means no runtime refmap context was explicitly supplied. */
    public static Result analyze(
            Path sourceJar,
            Collection<ConfigDeclaration> declarations,
            Forge1201TargetApiVerifier.TargetIndex targetIndex,
            Map<String, String> sourceToTargetClassMappings,
            String classMappingVersion,
            String requestedRefmapContextKey) throws IOException {
        if (sourceJar == null || !java.nio.file.Files.isRegularFile(sourceJar)) {
            throw new IllegalArgumentException("Mixin analysis input must be an existing archive: " + sourceJar);
        }
        if (declarations == null) throw new IllegalArgumentException("Mixin config declarations are required");

        Map<String, String> classMappings = sourceToTargetClassMappings == null
                ? Map.of() : Map.copyOf(sourceToTargetClassMappings);
        String mappingVersion = classMappingVersion == null || classMappingVersion.isBlank()
                ? "unavailable" : classMappingVersion;
        String archivePath = "source/" + sourceJar.getFileName();

        List<String> warnings = new ArrayList<>();
        Map<String, LinkedHashSet<DiscoverySource>> grouped = new TreeMap<>();
        for (ConfigDeclaration declaration : declarations) {
            grouped.computeIfAbsent(declaration.path(), ignored -> new LinkedHashSet<>()).add(declaration.source());
        }

        Map<String, IndexedEntry> entries = new HashMap<>();
        try (ZipFile zip = new ZipFile(sourceJar.toFile())) {
            var enumeration = zip.entries();
            int count = 0;
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (++count > MAX_ARCHIVE_ENTRIES) {
                    warnings.add("Archive entry inventory reached the " + MAX_ARCHIVE_ENTRIES
                            + " entry limit; remaining entries were not indexed.");
                    break;
                }
                entries.compute(entry.getName(), (name, current) ->
                        current == null ? new IndexedEntry(entry, 1)
                                : new IndexedEntry(current.entry(), current.occurrences() + 1));
            }

            List<ConfigFinding> configFindings = new ArrayList<>();
            List<MixinClassFinding> classFindings = new ArrayList<>();
            List<TargetFinding> targetFindings = new ArrayList<>();
            List<MemberAnnotationFinding> memberFindings = new ArrayList<>();
            List<InjectorFinding> injectorFindings = new ArrayList<>();
            boolean archiveEntryIndexIncomplete = count > MAX_ARCHIVE_ENTRIES;
            long totalConfigBytes = 0;
            long totalClassBytes = 0;
            int processedConfigs = 0;
            int processedMixins = 0;
            Map<String, RefmapCandidateAccumulator> refmapCandidates = new TreeMap<>();

            for (Map.Entry<String, LinkedHashSet<DiscoverySource>> candidate : grouped.entrySet()) {
                String configPath = candidate.getKey();
                List<DiscoverySource> sources = candidate.getValue().stream().sorted().toList();
                List<String> configWarnings = new ArrayList<>();

                if (!isSafeArchivePath(configPath)) {
                    configFindings.add(configFailure(archivePath, configPath, sources,
                            ConfigStatus.UNSAFE_CONFIG_PATH, "Config path is absolute, traversing, or malformed."));
                    continue;
                }
                IndexedEntry configEntry = entries.get(configPath);
                if (configEntry == null) {
                    ConfigStatus missingStatus = archiveEntryIndexIncomplete
                            ? ConfigStatus.CONFIG_LIMIT_EXCEEDED : ConfigStatus.CONFIG_MISSING;
                    String missingEvidence = archiveEntryIndexIncomplete
                            ? "Archive entry inventory was truncated; config presence cannot be established."
                            : "Discovered config resource is absent from the source archive.";
                    configFindings.add(configFailure(archivePath, configPath, sources,
                            missingStatus, missingEvidence));
                    continue;
                }
                if (configEntry.occurrences() != 1) {
                    configFindings.add(configFailure(archivePath, configPath, sources,
                            ConfigStatus.CONFIG_DUPLICATE_ENTRY, "Multiple ZIP entries use the same config path."));
                    continue;
                }
                if (processedConfigs >= MAX_CONFIG_COUNT) {
                    configFindings.add(configFailure(archivePath, configPath, sources,
                            ConfigStatus.CONFIG_LIMIT_EXCEEDED, "Maximum config count was reached."));
                    continue;
                }
                processedConfigs++;

                byte[] configBytes;
                try {
                    configBytes = readBounded(zip, configEntry.entry(), MAX_CONFIG_BYTES);
                } catch (EntryLimitException exception) {
                    configFindings.add(configFailure(archivePath, configPath, sources,
                            ConfigStatus.CONFIG_LIMIT_EXCEEDED, exception.getMessage()));
                    continue;
                }
                totalConfigBytes += configBytes.length;
                if (totalConfigBytes > MAX_TOTAL_CONFIG_BYTES) {
                    configFindings.add(configFailure(archivePath, configPath, sources,
                            ConfigStatus.CONFIG_LIMIT_EXCEEDED, "Aggregate config byte budget was exceeded."));
                    continue;
                }

                ParsedConfig parsed;
                try {
                    parsed = parseConfig(configBytes);
                } catch (RuntimeException exception) {
                    configFindings.add(configFailure(archivePath, configPath, sources,
                            ConfigStatus.CONFIG_INVALID, "Invalid Mixin JSON: " + conciseMessage(exception)));
                    continue;
                }
                configWarnings.addAll(parsed.warnings());

                PluginStatus pluginStatus = inspectPlugin(parsed.pluginClass(), entries, archiveEntryIndexIncomplete);
                RefmapSelection refmapSelection;
                RefmapResourceOrigin refmapOrigin = null;
                String refmapCandidatePath = null;
                if (parsed.refmapFieldPresent() && !parsed.refmapFieldValid()) {
                    refmapSelection = RefmapSelection.INVALID_DECLARATION;
                } else if (parsed.refmapFieldPresent()) {
                    refmapSelection = RefmapSelection.EXPLICIT_RESOURCE;
                    refmapCandidatePath = parsed.refmapPath();
                    refmapOrigin = RefmapResourceOrigin.EXPLICIT_CONFIG_FIELD;
                } else if (parsed.pluginClass() != null && !parsed.pluginClass().isBlank()) {
                    refmapSelection = RefmapSelection.PLUGIN_DEPENDENT;
                    refmapCandidatePath = DEFAULT_REFMAP_RESOURCE;
                    refmapOrigin = RefmapResourceOrigin.PLUGIN_DEPENDENT_CANDIDATE;
                    configWarnings.add("A declared Mixin plugin may provide a different refmap resource path at runtime; "
                            + "mixin.refmap.json is only a non-authoritative static candidate. Plugin code was not executed.");
                } else {
                    refmapSelection = RefmapSelection.DEFAULT_RESOURCE;
                    refmapCandidatePath = DEFAULT_REFMAP_RESOURCE;
                    refmapOrigin = RefmapResourceOrigin.MIXIN_DEFAULT_CANDIDATE;
                }
                if (refmapCandidatePath != null) {
                    RefmapCandidateAccumulator accumulator = refmapCandidates.computeIfAbsent(
                            refmapCandidatePath, ignored -> new RefmapCandidateAccumulator());
                    accumulator.configPaths.add(configPath);
                    accumulator.origins.add(refmapOrigin);
                }
                if (parsed.refmapWrapperClass() != null) {
                    configWarnings.add("Custom refmapWrapper '" + parsed.refmapWrapperClass()
                            + "' is preserved as metadata but is never loaded or executed by preflight.");
                }
                configFindings.add(new ConfigFinding(
                        archivePath, configPath, sources, ConfigStatus.PARSED,
                        parsed.packageName(), parsed.required(), parsed.minimumVersion(),
                        parsed.compatibilityLevel(), parsed.pluginClass(), pluginStatus,
                        parsed.refmapPath(), parsed.refmapWrapperClass(), refmapSelection,
                        refmapCandidatePath, ResourceStatus.NOT_DECLARED, configWarnings));

                boolean pluginDependent = parsed.pluginClass() != null;
                for (String section : List.of("mixins", "client", "server")) {
                    for (String mixinName : parsed.mixins().getOrDefault(section, List.of())) {
                        if (processedMixins >= MAX_MIXIN_CLASS_COUNT) {
                            classFindings.add(new MixinClassFinding(
                                    archivePath, configPath, section, mixinName, null,
                                    MixinClassStatus.CLASS_LIMIT_EXCEEDED, false,
                                    List.of("Maximum mixin class count was reached.")));
                            continue;
                        }
                        processedMixins++;

                        String classEntryPath = mixinClassEntry(parsed.packageName(), mixinName);
                        if (classEntryPath == null) {
                            classFindings.add(new MixinClassFinding(
                                    archivePath, configPath, section, mixinName, null,
                                    MixinClassStatus.INVALID_NAME, false,
                                    List.of("Mixin class name cannot be safely resolved from the config package.")));
                            continue;
                        }
                        IndexedEntry classEntry = entries.get(classEntryPath);
                        if (classEntry == null) {
                            MixinClassStatus missingStatus = archiveEntryIndexIncomplete
                                    ? MixinClassStatus.CLASS_LIMIT_EXCEEDED : MixinClassStatus.MISSING;
                            String evidence = archiveEntryIndexIncomplete
                                    ? "Archive entry inventory was truncated; mixin class presence cannot be established."
                                    : "Mixin class named by the config is absent from the source archive.";
                            classFindings.add(new MixinClassFinding(
                                    archivePath, configPath, section, mixinName, classEntryPath,
                                    missingStatus, false, List.of(evidence)));
                            continue;
                        }
                        if (classEntry.occurrences() != 1) {
                            classFindings.add(new MixinClassFinding(
                                    archivePath, configPath, section, mixinName, classEntryPath,
                                    MixinClassStatus.DUPLICATE_ENTRY, false,
                                    List.of("Multiple ZIP entries use the same mixin class path.")));
                            continue;
                        }

                        byte[] classBytes;
                        try {
                            classBytes = readBounded(zip, classEntry.entry(), MAX_CLASS_BYTES);
                        } catch (EntryLimitException exception) {
                            classFindings.add(new MixinClassFinding(
                                    archivePath, configPath, section, mixinName, classEntryPath,
                                    MixinClassStatus.CLASS_LIMIT_EXCEEDED, false,
                                    List.of(exception.getMessage())));
                            continue;
                        }
                        totalClassBytes += classBytes.length;
                        if (totalClassBytes > MAX_TOTAL_CLASS_BYTES) {
                            classFindings.add(new MixinClassFinding(
                                    archivePath, configPath, section, mixinName, classEntryPath,
                                    MixinClassStatus.CLASS_LIMIT_EXCEEDED, false,
                                    List.of("Aggregate mixin class byte budget was exceeded.")));
                            continue;
                        }

                        ClassNode mixin = new ClassNode();
                        try {
                            new ClassReader(classBytes).accept(
                                    mixin, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        } catch (RuntimeException exception) {
                            classFindings.add(new MixinClassFinding(
                                    archivePath, configPath, section, mixinName, classEntryPath,
                                    MixinClassStatus.INVALID_CLASS, false,
                                    List.of("Invalid class file: " + conciseMessage(exception))));
                            continue;
                        }

                        if (!classEntryPath.equals(mixin.name + ".class")) {
                            classFindings.add(new MixinClassFinding(
                                    archivePath, configPath, section, mixinName, classEntryPath,
                                    MixinClassStatus.INVALID_CLASS, false,
                                    List.of("Class internal name does not match its config-resolved archive path.")));
                            continue;
                        }

                        List<ParsedTarget> parsedTargets = readMixinTargets(mixin);
                        boolean mixinRemap = parsedTargetsRemap(mixin);
                        memberFindings.addAll(readMemberAnnotations(
                                archivePath, configPath, section, mixin.name.replace('/', '.'),
                                parsedTargets, mixin, pluginDependent, mixinRemap));
                        injectorFindings.addAll(readInjectorAnnotations(
                                archivePath, configPath, section, mixin.name.replace('/', '.'), mixin.name,
                                parsedTargets, mixin, pluginDependent, mixinRemap));
                        boolean hasMixinAnnotation = hasMixinAnnotation(mixin);
                        List<String> classWarnings = new ArrayList<>();
                        if (!hasMixinAnnotation) {
                            classWarnings.add("Resolved class has no visible or invisible @Mixin annotation.");
                        } else if (parsedTargets.isEmpty()) {
                            classWarnings.add("The @Mixin annotation has no statically readable class or string targets.");
                        }
                        classFindings.add(new MixinClassFinding(
                                archivePath, configPath, section, mixin.name.replace('/', '.'), mixin.name,
                                classEntryPath, MixinClassStatus.PRESENT, hasMixinAnnotation, classWarnings));

                        for (ParsedTarget target : parsedTargets) {
                            targetFindings.add(resolveTarget(
                                    archivePath, configPath, mixin.name.replace('/', '.'),
                                    target, mixinRemap, pluginDependent,
                                    classMappings, mappingVersion, targetIndex));
                        }
                    }
                }
            }

            List<RefmapResourceFinding> refmapFindings = inspectRefmapResources(
                    archivePath, refmapCandidates, entries, zip, archiveEntryIndexIncomplete);
            Map<String, RefmapResourceFinding> refmapsByPath = new HashMap<>();
            for (RefmapResourceFinding finding : refmapFindings) {
                refmapsByPath.put(finding.resourcePath(), finding);
            }
            configFindings = new ArrayList<>(configFindings.stream().map(finding -> {
                if (finding.refmapPath() == null) return finding;
                RefmapResourceFinding resource = refmapsByPath.get(finding.refmapPath());
                ResourceStatus legacyStatus = resource == null
                        ? ResourceStatus.NOT_DECLARED : toLegacyResourceStatus(resource.status());
                return new ConfigFinding(
                        finding.archivePath(), finding.configPath(), finding.discoverySources(), finding.status(),
                        finding.packageName(), finding.required(), finding.minimumVersion(),
                        finding.compatibilityLevel(), finding.pluginClass(), finding.pluginStatus(),
                        finding.refmapPath(), finding.refmapWrapperClass(), finding.refmapSelection(),
                        finding.refmapCandidatePath(), legacyStatus, finding.warnings());
            }).toList());

            List<RefmapLookupFinding> refmapLookupFindings = correlateRefmapLookups(
                    archivePath, configFindings, injectorFindings, refmapFindings, requestedRefmapContextKey)
                    .stream()
                    .sorted(Comparator.comparing(RefmapLookupFinding::configPath)
                            .thenComparing(value -> value.mixinInternalName() == null ? "" : value.mixinInternalName())
                            .thenComparing(RefmapLookupFinding::source)
                            .thenComparing(RefmapLookupFinding::sourceLocation)
                            .thenComparing(value -> value.rawReference() == null ? "" : value.rawReference()))
                    .toList();

            configFindings.sort(Comparator.comparing(ConfigFinding::configPath));
            classFindings.sort(Comparator.comparing(MixinClassFinding::configPath)
                    .thenComparing(MixinClassFinding::section)
                    .thenComparing(MixinClassFinding::mixinClassName));
            targetFindings.sort(Comparator.comparing(TargetFinding::configPath)
                    .thenComparing(TargetFinding::mixinClassName)
                    .thenComparing(TargetFinding::sourceTargetInternalName)
                    .thenComparing(TargetFinding::selectorForm));
            memberFindings.sort(Comparator.comparing(MemberAnnotationFinding::configPath)
                    .thenComparing(MemberAnnotationFinding::mixinClassName)
                    .thenComparing(MemberAnnotationFinding::annotationKind)
                    .thenComparing(MemberAnnotationFinding::elementKind)
                    .thenComparing(MemberAnnotationFinding::sourceMemberName)
                    .thenComparing(MemberAnnotationFinding::sourceMemberDescriptor));
            injectorFindings.sort(Comparator.comparing(InjectorFinding::configPath)
                    .thenComparing(InjectorFinding::mixinClassName)
                    .thenComparing(InjectorFinding::annotationKind)
                    .thenComparing(InjectorFinding::elementKind)
                    .thenComparing(InjectorFinding::sourceMemberName)
                    .thenComparing(InjectorFinding::sourceMemberDescriptor));
            return new Result(archivePath, "Minecraft 1.21.1 / NeoForge 21.1.258",
                    mappingVersion,
                    targetIndex == null ? TargetIndexStatus.UNAVAILABLE_NO_TARGET_CLASSPATH
                            : TargetIndexStatus.AVAILABLE,
                    configFindings, classFindings, targetFindings, memberFindings, injectorFindings,
                    refmapFindings, requestedRefmapContextKey, refmapLookupFindings,
                    warnings.stream().distinct().sorted().toList());
        }
    }

    private static ConfigFinding configFailure(
            String archivePath, String configPath, List<DiscoverySource> sources,
            ConfigStatus status, String warning) {
        return new ConfigFinding(archivePath, configPath, sources, status,
                null, null, null, null, null, PluginStatus.NOT_DECLARED,
                null, null, RefmapSelection.NOT_ANALYZED, null,
                ResourceStatus.NOT_DECLARED, List.of(warning));
    }

    private static ParsedConfig parseConfig(byte[] bytes) {
        JsonElement root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        if (!root.isJsonObject()) throw new IllegalArgumentException("Config root must be a JSON object");
        JsonObject object = root.getAsJsonObject();
        String packageName = stringValue(object, "package");
        if (packageName == null || !isValidDottedName(packageName)) {
            throw new IllegalArgumentException("Config must contain a valid package name");
        }

        Map<String, List<String>> mixins = new TreeMap<>();
        List<String> warnings = new ArrayList<>();
        for (String section : List.of("mixins", "client", "server")) {
            List<String> names = new ArrayList<>();
            JsonElement value = object.get(section);
            if (value != null) {
                if (!value.isJsonArray()) {
                    warnings.add("Config section '" + section + "' is not an array and was not resolved.");
                } else {
                    for (JsonElement item : value.getAsJsonArray()) {
                        if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                            names.add(item.getAsString());
                        } else {
                            warnings.add("Config section '" + section + "' contains a non-string mixin entry.");
                        }
                    }
                }
            }
            names.sort(String::compareTo);
            mixins.put(section, List.copyOf(names));
        }
        boolean refmapFieldPresent = object.has("refmap");
        boolean refmapFieldValid = !refmapFieldPresent || isJsonString(object.get("refmap"));
        if (refmapFieldPresent && !refmapFieldValid) {
            warnings.add("Config field 'refmap' is present but is not a JSON string; resource selection is unresolved.");
        }
        boolean wrapperFieldPresent = object.has("refmapWrapper");
        boolean wrapperFieldValid = !wrapperFieldPresent || isJsonString(object.get("refmapWrapper"));
        if (wrapperFieldPresent && !wrapperFieldValid) {
            warnings.add("Config field 'refmapWrapper' is present but is not a JSON string; wrapper metadata is unavailable.");
        }
        return new ParsedConfig(
                packageName,
                booleanValue(object, "required"),
                stringValue(object, "minVersion"),
                stringValue(object, "compatibilityLevel"),
                stringValue(object, "plugin"),
                stringValue(object, "refmap"),
                stringValue(object, "refmapWrapper"),
                refmapFieldPresent,
                refmapFieldValid,
                Map.copyOf(mixins),
                List.copyOf(warnings));
    }

    private static PluginStatus inspectPlugin(
            String pluginClass, Map<String, IndexedEntry> entries, boolean archiveIndexIncomplete) {
        if (pluginClass == null || pluginClass.isBlank()) return PluginStatus.NOT_DECLARED;
        String path = classResourcePath(pluginClass);
        if (path == null) return PluginStatus.INVALID_CLASS_NAME;
        IndexedEntry entry = entries.get(path);
        if (entry == null) return archiveIndexIncomplete
                ? PluginStatus.ARCHIVE_INDEX_INCOMPLETE : PluginStatus.MISSING_FROM_SOURCE_ARCHIVE;
        if (entry.occurrences() != 1) return PluginStatus.DUPLICATE_ENTRY;
        // Static analysis intentionally does not load, instantiate, or execute the plugin.
        return PluginStatus.PRESENT_NOT_EXECUTED;
    }

    private static List<RefmapResourceFinding> inspectRefmapResources(
            String archivePath,
            Map<String, RefmapCandidateAccumulator> candidates,
            Map<String, IndexedEntry> entries,
            ZipFile zip,
            boolean archiveIndexIncomplete) {
        List<RefmapResourceFinding> findings = new ArrayList<>();
        RefmapBudget budget = new RefmapBudget();
        int resourceIndex = 0;
        for (Map.Entry<String, RefmapCandidateAccumulator> candidate : new TreeMap<>(candidates).entrySet()) {
            String path = candidate.getKey();
            RefmapCandidateAccumulator accumulator = candidate.getValue();
            List<String> configPaths = accumulator.configPaths.stream().sorted().toList();
            List<RefmapResourceOrigin> origins = accumulator.origins.stream().sorted().toList();
            if (resourceIndex++ >= MAX_REFMAP_RESOURCE_PATHS) {
                findings.add(refmapResourceFailure(
                        archivePath, path, configPaths, origins, RefmapResourceStatus.LIMIT_EXCEEDED,
                        "Distinct refmap resource path limit (" + MAX_REFMAP_RESOURCE_PATHS + ") was reached."));
                continue;
            }
            findings.add(inspectRefmapResource(
                    archivePath, path, configPaths, origins, entries, zip, archiveIndexIncomplete, budget));
        }
        return findings.stream().sorted(Comparator.comparing(RefmapResourceFinding::resourcePath)).toList();
    }

    private static RefmapResourceFinding inspectRefmapResource(
            String archivePath,
            String path,
            List<String> configPaths,
            List<RefmapResourceOrigin> origins,
            Map<String, IndexedEntry> entries,
            ZipFile zip,
            boolean archiveIndexIncomplete,
            RefmapBudget budget) {
        if (!isSafeArchivePath(path)) {
            return refmapResourceFailure(archivePath, path, configPaths, origins,
                    RefmapResourceStatus.INVALID_PATH, "Refmap resource path is absolute, traversing, empty, or malformed.");
        }
        IndexedEntry indexed = entries.get(path);
        if (indexed == null) {
            return refmapResourceFailure(archivePath, path, configPaths, origins,
                    archiveIndexIncomplete ? RefmapResourceStatus.ARCHIVE_INDEX_INCOMPLETE : RefmapResourceStatus.MISSING,
                    archiveIndexIncomplete
                            ? "Archive entry index was truncated; refmap presence cannot be established."
                            : "Candidate refmap resource is absent from the source archive.");
        }
        if (indexed.occurrences() != 1) {
            return refmapResourceFailure(archivePath, path, configPaths, origins,
                    RefmapResourceStatus.DUPLICATE_ENTRY, "Multiple ZIP entries use the same refmap path.");
        }

        long remainingBudget = MAX_TOTAL_REFMAP_BYTES - budget.inflatedBytes;
        int readLimit = (int) Math.min((long) MAX_REFMAP_BYTES, Math.max(0L, remainingBudget));
        if (readLimit == 0) {
            return refmapResourceFailure(archivePath, path, configPaths, origins,
                    RefmapResourceStatus.LIMIT_EXCEEDED, "Aggregate refmap inflated-byte budget was exhausted.");
        }
        final byte[] bytes;
        try {
            bytes = readBounded(zip, indexed.entry(), readLimit);
        } catch (EntryLimitException exception) {
            budget.inflatedBytes += exception.bytesRead();
            return new RefmapResourceFinding(
                    archivePath, path, configPaths, origins, RefmapResourceStatus.LIMIT_EXCEEDED,
                    indexed.entry().getSize(), indexed.entry().getCompressedSize(),
                    exception.bytesRead(), null, false, false, List.of(), 0, 0, List.of(), List.of(),
                    List.of(exception.getMessage() + " Refmap parsing was not completed."));
        } catch (IOException exception) {
            return refmapResourceFailure(archivePath, path, configPaths, origins,
                    RefmapResourceStatus.INVALID_JSON, "Refmap resource could not be read: " + conciseMessage(exception));
        }
        budget.inflatedBytes += bytes.length;
        String digest = sha256(bytes);

        final JsonElement parsed;
        try {
            String jsonText = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            validateJsonWithoutDuplicateMembers(jsonText);
            parsed = JsonParser.parseString(jsonText);
        } catch (IOException | RuntimeException exception) {
            return new RefmapResourceFinding(
                    archivePath, path, configPaths, origins, RefmapResourceStatus.INVALID_JSON,
                    indexed.entry().getSize(), indexed.entry().getCompressedSize(),
                    bytes.length, digest, false, false, List.of(), 0, 0, List.of(), List.of(),
                    List.of("Refmap is not strict UTF-8 JSON or contains duplicate object member names: "
                            + conciseMessage(exception)));
        }
        if (!parsed.isJsonObject()) {
            return new RefmapResourceFinding(
                    archivePath, path, configPaths, origins, RefmapResourceStatus.UNSUPPORTED_SHAPE,
                    indexed.entry().getSize(), indexed.entry().getCompressedSize(),
                    bytes.length, digest, false, false, List.of(), 0, 0, List.of(), List.of(),
                    List.of("Refmap root must be a JSON object."));
        }

        JsonObject root = parsed.getAsJsonObject();
        boolean mappingsPresent = root.has("mappings");
        boolean dataPresent = root.has("data");
        List<String> unknownFields = root.keySet().stream()
                .filter(key -> !key.equals("mappings") && !key.equals("data"))
                .sorted().toList();
        RefmapParseState state = new RefmapParseState();
        List<RefmapTableFinding> tables = new ArrayList<>();
        if (!unknownFields.isEmpty()) {
            state.warnings.add("Unknown top-level refmap fields were retained as names only: " + unknownFields);
        }

        JsonElement mappingsElement = root.get("mappings");
        if (mappingsPresent) {
            if (mappingsElement != null && mappingsElement.isJsonObject()) {
                tables.add(parseRefmapTable(
                        mappingsElement.getAsJsonObject(), RefmapTableKind.BASE_MAPPINGS, null, state));
            } else {
                state.unsupportedShape = true;
                state.warnings.add("Top-level 'mappings' must be an owner-to-reference JSON object.");
            }
        }

        List<String> contextKeys = List.of();
        JsonElement dataElement = root.get("data");
        if (dataPresent) {
            if (dataElement == null || !dataElement.isJsonObject()) {
                state.unsupportedShape = true;
                state.warnings.add("Top-level 'data' must be a context-to-owner JSON object.");
            } else {
                JsonObject contextObject = dataElement.getAsJsonObject();
                contextKeys = contextObject.keySet().stream().sorted().toList();
                boolean contextLimitExceeded = contextKeys.size() > MAX_REFMAP_CONTEXTS_PER_RESOURCE;
                if (contextLimitExceeded) {
                    state.warnings.add("Context table limit (" + MAX_REFMAP_CONTEXTS_PER_RESOURCE
                            + ") was reached; remaining context tables are inventoried by key only.");
                }
                int parsedContexts = 0;
                for (String contextKey : contextKeys) {
                    if (parsedContexts >= MAX_REFMAP_CONTEXTS_PER_RESOURCE) break;
                    JsonElement tableElement = contextObject.get(contextKey);
                    if (tableElement == null || !tableElement.isJsonObject()) {
                        state.unsupportedShape = true;
                        state.warnings.add("Context '" + contextKey + "' must contain an owner-to-reference JSON object.");
                        parsedContexts++;
                        continue;
                    }
                    tables.add(parseRefmapTable(
                            tableElement.getAsJsonObject(), RefmapTableKind.CONTEXT, contextKey, state));
                    parsedContexts++;
                    if (state.limitExceeded) break;
                }
                if (contextLimitExceeded) state.limitExceeded = true;
            }
        }

        RefmapResourceStatus status = state.limitExceeded ? RefmapResourceStatus.LIMIT_EXCEEDED
                : state.unsupportedShape ? RefmapResourceStatus.UNSUPPORTED_SHAPE
                : state.entryCount == 0 ? RefmapResourceStatus.EMPTY : RefmapResourceStatus.PARSED;
        return new RefmapResourceFinding(
                archivePath, path, configPaths, origins, status,
                indexed.entry().getSize(), indexed.entry().getCompressedSize(), bytes.length, digest,
                mappingsPresent, dataPresent, contextKeys, state.ownerBucketCount, state.entryCount,
                unknownFields, tables,
                state.warnings.stream().distinct().sorted().toList());
    }

    private static RefmapTableFinding parseRefmapTable(
            JsonObject table,
            RefmapTableKind kind,
            String contextKey,
            RefmapParseState state) {
        List<RefmapEntryFinding> entries = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int ownerCount = 0;
        int entryCount = 0;
        for (String owner : table.keySet().stream().sorted().toList()) {
            if (state.ownerBucketCount >= MAX_REFMAP_OWNER_BUCKETS_PER_RESOURCE) {
                state.limitExceeded = true;
                warnings.add("Owner-bucket limit (" + MAX_REFMAP_OWNER_BUCKETS_PER_RESOURCE + ") was reached.");
                break;
            }
            JsonElement ownerElement = table.get(owner);
            if (owner.isBlank() || ownerElement == null || !ownerElement.isJsonObject()) {
                state.unsupportedShape = true;
                warnings.add("Owner bucket '" + owner + "' must be a non-empty owner name mapped to a JSON object.");
                break;
            }
            JsonObject references = ownerElement.getAsJsonObject();
            ownerCount++;
            state.ownerBucketCount++;
            for (String reference : references.keySet().stream().sorted().toList()) {
                if (state.entryCount >= MAX_REFMAP_ENTRIES_PER_RESOURCE) {
                    state.limitExceeded = true;
                    warnings.add("Mapping-entry limit (" + MAX_REFMAP_ENTRIES_PER_RESOURCE + ") was reached.");
                    break;
                }
                JsonElement mapped = references.get(reference);
                if (mapped == null || !isJsonString(mapped)) {
                    state.unsupportedShape = true;
                    warnings.add("Mapping value for owner '" + owner + "' and reference '" + reference
                            + "' must be a JSON string.");
                    break;
                }
                entries.add(new RefmapEntryFinding(owner, reference, mapped.getAsString()));
                entryCount++;
                state.entryCount++;
            }
            if (state.limitExceeded || state.unsupportedShape) break;
        }
        return new RefmapTableFinding(
                kind, contextKey, ownerCount, entryCount, entries,
                warnings.stream().distinct().sorted().toList());
    }


    private static List<RefmapLookupFinding> correlateRefmapLookups(
            String archivePath, List<ConfigFinding> configs, List<InjectorFinding> injectors,
            List<RefmapResourceFinding> resources, String requestedContextKey) {
        Map<String, ConfigFinding> configsByPath = new HashMap<>();
        for (ConfigFinding config : configs) configsByPath.put(config.configPath(), config);
        Map<String, RefmapResourceFinding> resourcesByPath = new HashMap<>();
        for (RefmapResourceFinding resource : resources) resourcesByPath.put(resource.resourcePath(), resource);
        List<RefmapLookupFinding> findings = new ArrayList<>();
        for (InjectorFinding injector : injectors) {
            ConfigFinding config = configsByPath.get(injector.configPath());
            for (int i = 0; i < injector.targetSelectors().size(); i++) {
                InjectorTargetSelector selector = injector.targetSelectors().get(i);
                if (selector.source() == InjectorSelectorSource.METHOD_STRING) {
                    boolean supported = injector.elementKind() == MemberElementKind.METHOD
                            && (selector.status() == InjectorSelectorStatus.METHOD_NAME_ONLY
                                || selector.status() == InjectorSelectorStatus.METHOD_DESCRIPTOR
                                || selector.status() == InjectorSelectorStatus.WILDCARD);
                    findings.add(createRefmapLookup(archivePath, injector, config, resourcesByPath,
                            requestedContextKey, RefmapLookupSource.INJECTOR_METHOD, "method[" + i + "]",
                            selector.rawSelector(), injector.remap(), supported,
                            "Mixin 0.8.5 processes string injector method[] selectors; descriptor inference is not done here."));
                } else {
                    findings.add(createRefmapLookup(archivePath, injector, config, resourcesByPath,
                            requestedContextKey, RefmapLookupSource.INJECTOR_DESC_TARGET, "target[" + i + "]",
                            null, injector.remap(), false,
                            "Mixin 0.8.5 @Desc target processing has no string refmap key; no key is synthesized."));
                }
            }
            for (AtFinding at : injector.injectionPoints()) {
                addAtRefmapLookups(findings, archivePath, injector, config, resourcesByPath,
                        requestedContextKey, at, "at[" + at.index() + "]");
            }
            for (SliceFinding slice : injector.slices()) {
                if (slice.from() != null && slice.from().at() != null) {
                    addAtRefmapLookups(findings, archivePath, injector, config, resourcesByPath,
                            requestedContextKey, slice.from().at(), "slice[" + slice.index() + "].from");
                }
                if (slice.to() != null && slice.to().at() != null) {
                    addAtRefmapLookups(findings, archivePath, injector, config, resourcesByPath,
                            requestedContextKey, slice.to().at(), "slice[" + slice.index() + "].to");
                }
            }
        }
        return findings;
    }

    private static void addAtRefmapLookups(
            List<RefmapLookupFinding> findings, String archivePath, InjectorFinding injector,
            ConfigFinding config, Map<String, RefmapResourceFinding> resourcesByPath,
            String requestedContextKey, AtFinding at, String location) {
        String atKind = at.value() == null ? "" : at.value().split(":", 2)[0];
        boolean methodPlacement = injector.elementKind() == MemberElementKind.METHOD;
        if (at.rawTarget() != null && !at.rawTarget().isEmpty()) {
            AtTargetSelector selector = at.targetSelector();
            boolean ownerAndDescriptor = selector != null && selector.ownerInternalName() != null
                    && selector.descriptor() != null
                    && (selector.status() == AtTargetSelectorStatus.METHOD_DESCRIPTOR
                        || selector.status() == AtTargetSelectorStatus.FIELD_DESCRIPTOR);
            boolean constructor = "NEW".equals(atKind) && ownerAndDescriptor
                    && selector.descriptorKind() == AtTargetDescriptorKind.METHOD
                    && "<init>".equals(selector.memberName());
            boolean ordinaryMember = !"NEW".equals(atKind) && ownerAndDescriptor;
            boolean canEmit = methodPlacement && (constructor || ordinaryMember);
            String evidence = "Mixin 0.8.5 requires a fully-qualified remappable @At.target member selector.";
            if ("NEW".equals(atKind)) evidence = constructor
                    ? "Mixin 0.8.5 has a distinct NEW constructor-target refmap path."
                    : "The AP only maps NEW targets parsed as constructor selectors; this form was not recognized.";
            findings.add(createRefmapLookup(archivePath, injector, config, resourcesByPath,
                    requestedContextKey, RefmapLookupSource.AT_TARGET, location + ".target",
                    at.rawTarget(), at.remap(), canEmit, evidence));
        }
        if (at.descSelector() != null) {
            findings.add(createRefmapLookup(archivePath, injector, config, resourcesByPath,
                    requestedContextKey, RefmapLookupSource.AT_DESC_TARGET, location + ".desc",
                    null, at.remap(), false,
                    "Nested @At.desc is not a string refmap key in the pinned Mixin 0.8.5 AP path."));
        }
        if ("NEW".equals(atKind)) {
            String classReference = null;
            int classIndex = -1;
            for (int i = 0; i < at.args().size(); i++) {
                String argument = at.args().get(i);
                if (argument == null) continue;
                int equals = argument.indexOf('=');
                String key = equals < 0 ? argument : argument.substring(0, equals);
                if ("class".equals(key)) {
                    classReference = equals < 0 ? "" : argument.substring(equals + 1);
                    classIndex = i;
                }
            }
            if (classIndex >= 0) {
                boolean canEmit = methodPlacement && isNewClassArgumentSelector(classReference);
                findings.add(createRefmapLookup(archivePath, injector, config, resourcesByPath,
                        requestedContextKey, RefmapLookupSource.AT_NEW_CLASS_ARGUMENT,
                        location + ".args[" + classIndex + "].class", classReference, at.remap(), canEmit,
                        "Mixin 0.8.5 processes NEW args[class] separately through constructor-selector parsing; the last class= value is retained."));
            }
        }
    }

    private static boolean isNewClassArgumentSelector(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) return false;
        AtTargetSelector selector = parseAtTargetSelector(rawReference);
        boolean ownerOnlyClass = selector.ownerInternalName() != null
                && selector.status() == AtTargetSelectorStatus.OWNER_ONLY
                && selector.memberName() != null && selector.memberName().isBlank();
        boolean explicitConstructor = selector.ownerInternalName() != null
                && selector.status() == AtTargetSelectorStatus.METHOD_DESCRIPTOR
                && selector.descriptorKind() == AtTargetDescriptorKind.METHOD
                && "<init>".equals(selector.memberName());
        return ownerOnlyClass || explicitConstructor;
    }

    private static RefmapLookupFinding createRefmapLookup(
            String archivePath, InjectorFinding injector, ConfigFinding config,
            Map<String, RefmapResourceFinding> resourcesByPath, String requestedContextKey,
            RefmapLookupSource source, String sourceLocation, String rawReference,
            boolean remap, boolean apCanEmitReference, String sourceEvidence) {
        String normalizedReference = rawReference == null ? null : normalizeRefmapReferenceKey(rawReference);
        String resourcePath = config == null ? null : config.refmapCandidatePath();
        List<RefmapLookupCandidate> candidates = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        RefmapContextBehavior behavior = RefmapContextBehavior.NOT_EVALUATED;
        String selectedContextKey = null, mappedReferenceValue = null;
        RefmapLookupStatus status;
        String evidence;
        boolean pluginDependent = config != null
                && (config.refmapSelection() == RefmapSelection.PLUGIN_DEPENDENT
                    || config.refmapWrapperClass() != null);
        if (!apCanEmitReference) {
            status = RefmapLookupStatus.SELECTOR_NOT_EMITTED_AS_REF;
            evidence = sourceEvidence;
        } else if (!remap) {
            status = RefmapLookupStatus.REMAP_DISABLED;
            evidence = "Effective remap=false; Mixin's annotation processor does not generate this refmap entry.";
        } else if (injector.mixinInternalName() == null || injector.mixinInternalName().isBlank()) {
            status = RefmapLookupStatus.OWNER_UNAVAILABLE;
            evidence = "The mixin classfile internal name is unavailable; ownerless refmap lookup is prohibited.";
        } else if (config == null) {
            status = RefmapLookupStatus.ANALYSIS_INCOMPLETE;
            evidence = "The owning Mixin config finding is unavailable; its refmap resource cannot be selected.";
        } else if (resourcePath == null) {
            status = pluginDependent ? RefmapLookupStatus.PLUGIN_DEPENDENT : RefmapLookupStatus.ANALYSIS_INCOMPLETE;
            evidence = pluginDependent
                    ? "A plugin or custom wrapper may choose the effective resource or lookup behavior."
                    : "The config has no valid refmap resource candidate.";
        } else {
            RefmapResourceFinding resource = resourcesByPath.get(resourcePath);
            boolean complete = resource != null
                    && (resource.status() == RefmapResourceStatus.PARSED || resource.status() == RefmapResourceStatus.EMPTY);
            if (!complete) {
                if (resource == null) warnings.add("The selected refmap candidate has no resource inventory record.");
                else warnings.add("Refmap resource status is " + resource.status()
                        + "; incomplete inventory cannot establish an absent mapping.");
                status = RefmapLookupStatus.ANALYSIS_INCOMPLETE;
                evidence = "The selected refmap candidate is missing, malformed, unsupported, or incompletely inventoried.";
            } else if (requestedContextKey != null) {
                if (resource.contextKeys().contains(requestedContextKey)) {
                    behavior = RefmapContextBehavior.CONTEXT_TABLE_SELECTED;
                    selectedContextKey = requestedContextKey;
                    RefmapTableFinding table = resource.tables().stream()
                            .filter(value -> value.kind() == RefmapTableKind.CONTEXT
                                    && requestedContextKey.equals(value.contextKey()))
                            .findFirst().orElse(null);
                    RefmapLookupCandidate candidate = lookupRefmapTable(table, RefmapTableKind.CONTEXT,
                            requestedContextKey, injector.mixinInternalName(), normalizedReference, true);
                    candidates.add(candidate);
                    status = topLevelLookupStatus(candidate.status());
                    mappedReferenceValue = candidate.mappedReferenceValue();
                    evidence = candidate.status() == RefmapCandidateStatus.TABLE_NOT_INVENTORIED
                            ? "The requested context exists but was not inventoried; it is selected and does not fall back to base."
                            : "The explicitly requested context table is selected; a missing owner/reference within it does not fall back to base.";
                } else {
                    behavior = RefmapContextBehavior.CONTEXT_MISSING_DEFAULT_FALLBACK;
                    RefmapTableFinding baseTable = resource.tables().stream()
                            .filter(value -> value.kind() == RefmapTableKind.BASE_MAPPINGS)
                            .findFirst().orElse(null);
                    RefmapLookupCandidate candidate = lookupRefmapTable(baseTable, RefmapTableKind.BASE_MAPPINGS,
                            null, injector.mixinInternalName(), normalizedReference, false);
                    candidates.add(candidate);
                    status = topLevelLookupStatus(candidate.status());
                    mappedReferenceValue = candidate.mappedReferenceValue();
                    evidence = "The requested context table is absent, so Mixin 0.8.5 falls back to base mappings.";
                }
            } else {
                behavior = RefmapContextBehavior.CONTEXT_NOT_SELECTED;
                RefmapTableFinding baseTable = resource.tables().stream()
                        .filter(value -> value.kind() == RefmapTableKind.BASE_MAPPINGS)
                        .findFirst().orElse(null);
                candidates.add(lookupRefmapTable(baseTable, RefmapTableKind.BASE_MAPPINGS,
                        null, injector.mixinInternalName(), normalizedReference, false));
                resource.tables().stream()
                        .filter(value -> value.kind() == RefmapTableKind.CONTEXT)
                        .sorted(Comparator.comparing(RefmapTableFinding::contextKey))
                        .forEach(table -> candidates.add(lookupRefmapTable(table, RefmapTableKind.CONTEXT,
                                table.contextKey(), injector.mixinInternalName(), normalizedReference, true)));
                Set<String> results = new TreeSet<>();
                for (RefmapLookupCandidate candidate : candidates) results.add(
                        candidate.mappedReferenceValue() == null ? normalizedReference : candidate.mappedReferenceValue());
                status = results.size() > 1 ? RefmapLookupStatus.CONTEXT_DEPENDENT_VALUES
                        : RefmapLookupStatus.CONTEXT_NOT_SELECTED;
                evidence = status == RefmapLookupStatus.CONTEXT_DEPENDENT_VALUES
                        ? "Available tables produce different exact owner/reference results; no runtime context was supplied."
                        : "No runtime context was supplied; all base/context candidate results are shown without selecting one.";
            }
        }
        if (pluginDependent && apCanEmitReference && remap) {
            status = RefmapLookupStatus.PLUGIN_DEPENDENT;
            evidence += " A declared plugin or custom refmapWrapper may alter resource selection or lookup; code was not executed.";
        }
        if (requestedContextKey != null && behavior == RefmapContextBehavior.NOT_EVALUATED) {
            warnings.add("The requested context was not evaluated because this source element/resource was not eligible for exact lookup.");
        }
        return new RefmapLookupFinding(archivePath, injector.configPath(), injector.section(),
                injector.mixinClassName(), injector.mixinInternalName(), source, sourceLocation,
                injector.sourceMemberName(), injector.sourceMemberDescriptor(), rawReference,
                normalizedReference, remap, resourcePath, requestedContextKey, behavior,
                selectedContextKey, status, mappedReferenceValue, candidates, evidence, warnings);
    }

    private static RefmapLookupCandidate lookupRefmapTable(
            RefmapTableFinding table, RefmapTableKind kind, String contextKey,
            String ownerInternalName, String normalizedReference, boolean expectedTable) {
        if (table == null) return new RefmapLookupCandidate(kind, contextKey,
                expectedTable ? RefmapCandidateStatus.TABLE_NOT_INVENTORIED
                        : RefmapCandidateStatus.ENTRY_NOT_FOUND_PASSTHROUGH, null, List.of());
        List<RefmapEntryFinding> matches = table.entries().stream()
                .filter(entry -> ownerInternalName.equals(entry.ownerInternalName())
                        && normalizedReference.equals(entry.referenceKey()))
                .toList();
        List<RefmapLookupMatch> matchRows = matches.stream()
                .map(entry -> new RefmapLookupMatch(entry.referenceKey(), entry.mappedReferenceValue())).toList();
        if (matches.isEmpty()) return new RefmapLookupCandidate(kind, contextKey,
                RefmapCandidateStatus.ENTRY_NOT_FOUND_PASSTHROUGH, null, matchRows);
        RefmapEntryFinding match = matches.getFirst();
        RefmapCandidateStatus status = normalizedReference.equals(match.mappedReferenceValue())
                ? RefmapCandidateStatus.EXPLICIT_IDENTITY_ENTRY : RefmapCandidateStatus.ENTRY_FOUND;
        return new RefmapLookupCandidate(kind, contextKey, status, match.mappedReferenceValue(), matchRows);
    }

    private static RefmapLookupStatus topLevelLookupStatus(RefmapCandidateStatus status) {
        return switch (status) {
            case ENTRY_FOUND -> RefmapLookupStatus.ENTRY_FOUND;
            case ENTRY_NOT_FOUND_PASSTHROUGH -> RefmapLookupStatus.ENTRY_NOT_FOUND_PASSTHROUGH;
            case EXPLICIT_IDENTITY_ENTRY -> RefmapLookupStatus.EXPLICIT_IDENTITY_ENTRY;
            case TABLE_NOT_INVENTORIED -> RefmapLookupStatus.ANALYSIS_INCOMPLETE;
        };
    }

    /** Mirrors Mixin 0.8.5 ReferenceMapper.addMapping's exact source-key normalization. */
    private static String normalizeRefmapReferenceKey(String raw) {
        return raw == null ? null : raw.replaceAll("\\s", "");
    }

    private static RefmapResourceFinding refmapResourceFailure(
            String archivePath,
            String path,
            List<String> configPaths,
            List<RefmapResourceOrigin> origins,
            RefmapResourceStatus status,
            String warning) {
        return new RefmapResourceFinding(
                archivePath, path, configPaths, origins, status, -1L, -1L, -1L, null,
                false, false, List.of(), 0, 0, List.of(), List.of(), List.of(warning));
    }

    private static ResourceStatus toLegacyResourceStatus(RefmapResourceStatus status) {
        return switch (status) {
            case PARSED, EMPTY -> ResourceStatus.PRESENT_VALID_JSON;
            case UNSUPPORTED_SHAPE -> ResourceStatus.UNSUPPORTED_SHAPE;
            case MISSING -> ResourceStatus.MISSING;
            case INVALID_PATH -> ResourceStatus.INVALID_PATH;
            case DUPLICATE_ENTRY -> ResourceStatus.DUPLICATE_ENTRY;
            case LIMIT_EXCEEDED -> ResourceStatus.LIMIT_EXCEEDED;
            case INVALID_JSON -> ResourceStatus.INVALID_JSON;
            case ARCHIVE_INDEX_INCOMPLETE -> ResourceStatus.ARCHIVE_INDEX_INCOMPLETE;
        };
    }

    private static boolean isJsonString(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private static void validateJsonWithoutDuplicateMembers(String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            scanJsonValue(reader, 0);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonParseException("Trailing data after the refmap JSON value.");
            }
        }
    }

    private static void scanJsonValue(JsonReader reader, int depth) throws IOException {
        if (depth > MAX_REFMAP_JSON_DEPTH) {
            throw new JsonParseException("Refmap JSON nesting exceeds " + MAX_REFMAP_JSON_DEPTH + " levels.");
        }
        switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject();
                Set<String> names = new HashSet<>();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (!names.add(name)) {
                        throw new JsonParseException("Duplicate JSON object member '" + name + "'.");
                    }
                    scanJsonValue(reader, depth + 1);
                }
                reader.endObject();
            }
            case BEGIN_ARRAY -> {
                reader.beginArray();
                while (reader.hasNext()) scanJsonValue(reader, depth + 1);
                reader.endArray();
            }
            case STRING, NUMBER -> reader.nextString();
            case BOOLEAN -> reader.nextBoolean();
            case NULL -> reader.nextNull();
            default -> throw new JsonParseException("Unexpected JSON token: " + reader.peek());
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }


    private static List<MemberAnnotationFinding> readMemberAnnotations(
            String archivePath,
            String configPath,
            String section,
            String mixinClassName,
            List<ParsedTarget> parsedTargets,
            ClassNode mixin,
            boolean pluginDependent,
            boolean mixinRemap) {
        List<String> sourceTargets = parsedTargets.stream()
                .map(ParsedTarget::selector).distinct().sorted().toList();
        List<MemberAnnotationFinding> findings = new ArrayList<>();

        if (mixin.fields != null) {
            for (FieldNode field : mixin.fields) {
                for (List<AnnotationNode> annotations : annotationLists(
                        field.visibleAnnotations, field.invisibleAnnotations)) {
                    for (AnnotationNode annotation : annotations) {
                        MemberAnnotationKind kind = memberAnnotationKind(annotation.desc);
                        if (kind == null) continue;
                        findings.add(memberAnnotationFinding(
                                archivePath, configPath, section, mixinClassName, sourceTargets,
                                kind, MemberElementKind.FIELD, field.name, field.desc,
                                field.access, annotation, pluginDependent, mixinRemap));
                    }
                }
            }
        }

        if (mixin.methods != null) {
            for (MethodNode method : mixin.methods) {
                for (List<AnnotationNode> annotations : annotationLists(
                        method.visibleAnnotations, method.invisibleAnnotations)) {
                    for (AnnotationNode annotation : annotations) {
                        MemberAnnotationKind kind = memberAnnotationKind(annotation.desc);
                        if (kind == null) continue;
                        findings.add(memberAnnotationFinding(
                                archivePath, configPath, section, mixinClassName, sourceTargets,
                                kind, MemberElementKind.METHOD, method.name, method.desc,
                                method.access, annotation, pluginDependent, mixinRemap));
                    }
                }
            }
        }
        return findings;
    }

    private static List<InjectorFinding> readInjectorAnnotations(
            String archivePath, String configPath, String section, String mixinClassName,
            String mixinInternalName, List<ParsedTarget> parsedTargets,
            ClassNode mixin, boolean pluginDependent, boolean mixinRemap) {
        List<String> targets = parsedTargets.stream().map(ParsedTarget::selector).distinct().sorted().toList();
        List<InjectorFinding> findings = new ArrayList<>();
        if (mixin.fields != null) {
            for (FieldNode field : mixin.fields) {
                for (List<AnnotationNode> annotations : annotationLists(
                        field.visibleAnnotations, field.invisibleAnnotations)) {
                    for (AnnotationNode annotation : annotations) {
                        InjectorAnnotationKind kind = injectorAnnotationKind(annotation.desc);
                        if (kind != null) findings.add(createInjectorFinding(archivePath, configPath, section,
                                mixinClassName, mixinInternalName, targets, kind,
                                MemberElementKind.FIELD, field.name, field.desc,
                                annotation, pluginDependent, mixinRemap));
                    }
                }
            }
        }
        if (mixin.methods != null) {
            for (MethodNode method : mixin.methods) {
                for (List<AnnotationNode> annotations : annotationLists(
                        method.visibleAnnotations, method.invisibleAnnotations)) {
                    for (AnnotationNode annotation : annotations) {
                        InjectorAnnotationKind kind = injectorAnnotationKind(annotation.desc);
                        if (kind != null) findings.add(createInjectorFinding(archivePath, configPath, section,
                                mixinClassName, mixinInternalName, targets, kind,
                                MemberElementKind.METHOD, method.name, method.desc,
                                annotation, pluginDependent, mixinRemap));
                    }
                }
            }
        }
        return findings;
    }

    private static InjectorAnnotationKind injectorAnnotationKind(String descriptor) {
        return switch (descriptor) {
            case INJECT_ANNOTATION -> InjectorAnnotationKind.INJECT;
            case REDIRECT_ANNOTATION -> InjectorAnnotationKind.REDIRECT;
            case MODIFY_ARG_ANNOTATION -> InjectorAnnotationKind.MODIFY_ARG;
            case MODIFY_ARGS_ANNOTATION -> InjectorAnnotationKind.MODIFY_ARGS;
            case MODIFY_VARIABLE_ANNOTATION -> InjectorAnnotationKind.MODIFY_VARIABLE;
            case MODIFY_CONSTANT_ANNOTATION -> InjectorAnnotationKind.MODIFY_CONSTANT;
            default -> null;
        };
    }

    private static InjectorFinding createInjectorFinding(
            String archivePath, String configPath, String section, String mixinClassName,
            String mixinInternalName, List<String> targets,
            InjectorAnnotationKind kind, MemberElementKind elementKind,
            String sourceName, String sourceDescriptor, AnnotationNode annotation,
            boolean pluginDependent, boolean mixinRemap) {
        List<String> warnings = new ArrayList<>();
        List<InjectorTargetSelector> selectors = new ArrayList<>();
        for (String raw : stringArrayValues(annotation, "method")) selectors.add(parseMethodSelector(raw));
        for (AnnotationNode desc : annotationArrayValue(annotation, "target")) {
            if (DESC_ANNOTATION.equals(desc.desc)) selectors.add(parseDescSelector(desc));
            else selectors.add(new InjectorTargetSelector(InjectorSelectorSource.DESC_ANNOTATION, desc.desc,
                    null, null, null, null, null, null, InjectorSelectorStatus.UNSUPPORTED_EXTENSION,
                    "Unsupported nested selector annotation; not interpreted as Mixin @Desc."));
        }

        boolean placementValid = elementKind == MemberElementKind.METHOD;
        boolean handlerValid = placementValid && isValidMethodDescriptor(sourceDescriptor);
        if (!placementValid) warnings.add("Injector annotation is on a field; injector handlers must be methods.");
        if (!handlerValid) warnings.add("Handler does not have a valid JVM method descriptor.");
        if (selectors.isEmpty()) warnings.add("No method string or @Desc target selector was declared.");
        if (selectors.stream().anyMatch(s -> s.status() == InjectorSelectorStatus.INVALID_SELECTOR
                || s.status() == InjectorSelectorStatus.INVALID_DESCRIPTOR)) {
            warnings.add("At least one target selector is malformed.");
        }
        if (selectors.stream().anyMatch(s -> s.status() == InjectorSelectorStatus.UNSUPPORTED_EXTENSION)) {
            warnings.add("At least one selector extension is retained but not interpreted.");
        }

        boolean remap = booleanAnnotationValue(annotation, "remap", mixinRemap);
        List<AtFinding> injectionPoints = readAtFindings(annotation, remap);
        List<ConstantFinding> constants = readConstantFindings(annotation, kind);
        List<SliceFinding> slices = readSliceFindings(annotation, remap);
        int atCount = injectionPoints.size();
        int sliceCount = slices.size();
        Integer index = nullableIntegerAnnotationValue(annotation, "index");
        Object cancellableValue = annotationValue(annotation, "cancellable");
        Boolean cancellable = cancellableValue instanceof Boolean b ? b : null;
        Object argsOnlyValue = annotationValue(annotation, "argsOnly");
        Boolean argsOnly = argsOnlyValue instanceof Boolean b ? b : null;
        int require = integerAnnotationValue(annotation, "require", -1);
        int expect = integerAnnotationValue(annotation, "expect", 1);
        int allow = integerAnnotationValue(annotation, "allow", -1);
        String constraints = stringAnnotationValue(annotation, "constraints");

        InjectorResolutionStatus status;
        if (!placementValid) status = InjectorResolutionStatus.INVALID_ANNOTATION_PLACEMENT;
        else if (!handlerValid) status = InjectorResolutionStatus.INVALID_HANDLER_DESCRIPTOR;
        else if (selectors.isEmpty() || selectors.stream().anyMatch(s ->
                s.status() == InjectorSelectorStatus.INVALID_SELECTOR
                        || s.status() == InjectorSelectorStatus.INVALID_DESCRIPTOR)) {
            status = InjectorResolutionStatus.INVALID_INJECTOR_SELECTOR;
        } else if (selectors.stream().anyMatch(s ->
                s.status() == InjectorSelectorStatus.UNSUPPORTED_EXTENSION)) {
            status = InjectorResolutionStatus.UNSUPPORTED_SELECTOR_SYNTAX;
        } else if (pluginDependent) status = InjectorResolutionStatus.PLUGIN_DEPENDENT;
        else if (remap) status = InjectorResolutionStatus.INJECTOR_REMAP_UNRESOLVED;
        else status = InjectorResolutionStatus.INJECTOR_REMAP_DISABLED_UNVERIFIED;

        String evidence = switch (status) {
            case INVALID_ANNOTATION_PLACEMENT -> "Injector handler annotation was found on a field.";
            case INVALID_HANDLER_DESCRIPTOR -> "The annotated method has a malformed JVM handler descriptor.";
            case INVALID_INJECTOR_SELECTOR -> "No valid statically readable target method selector is available.";
            case UNSUPPORTED_SELECTOR_SYNTAX -> "Raw selector data is retained, but chained selector syntax is not interpreted.";
            case PLUGIN_DEPENDENT -> "The config declares a Mixin plugin; it can alter target behavior. Plugin code was not executed.";
            case INJECTOR_REMAP_UNRESOLVED -> "Source handler and target selectors were inventoried; no reviewed versioned member mapping was supplied, so target presence is not asserted.";
            case INJECTOR_REMAP_DISABLED_UNVERIFIED -> "remap=false; source handler and target selectors were inventoried, but target presence and parity remain unverified.";
        };
        return new InjectorFinding(archivePath, configPath, section, mixinClassName, mixinInternalName, targets,
                kind, elementKind, sourceName, sourceDescriptor, selectors, injectionPoints, constants, slices, remap,
                atCount, sliceCount, index, cancellable, argsOnly,
                require, expect, allow, constraints, status, evidence, warnings);
    }


    private static List<AtFinding> readAtFindings(AnnotationNode injector, boolean defaultRemap) {
        List<AtFinding> findings = new ArrayList<>();
        List<AnnotationNode> annotations = annotationArrayValue(injector, "at");
        for (int i = 0; i < annotations.size(); i++) {
            findings.add(createAtFinding(i, annotations.get(i), defaultRemap));
        }
        return List.copyOf(findings);
    }

    private static AtFinding createAtFinding(int index, AnnotationNode annotation, boolean defaultRemap) {
        List<String> warnings = new ArrayList<>();
        String value = stringAnnotationValue(annotation, "value");
        AtValueKind valueKind = isBuiltInAtValue(value) ? AtValueKind.BUILT_IN : AtValueKind.CUSTOM_OR_UNKNOWN;
        String rawTarget = stringAnnotationValue(annotation, "target");
        if (rawTarget == null) rawTarget = "";
        AtTargetSelector targetSelector = parseAtTargetSelector(rawTarget);
        Object rawDesc = annotationValue(annotation, "desc");
        InjectorTargetSelector descSelector = null;
        boolean invalidDesc = false;
        if (rawDesc instanceof AnnotationNode nested) {
            if (DESC_ANNOTATION.equals(nested.desc)) {
                descSelector = parseDescSelector(nested);
                invalidDesc = descSelector.status() == InjectorSelectorStatus.INVALID_SELECTOR;
            } else {
                descSelector = new InjectorTargetSelector(InjectorSelectorSource.DESC_ANNOTATION,
                        nested.desc, null, null, null, null, null, null,
                        InjectorSelectorStatus.UNSUPPORTED_EXTENSION,
                        "The nested At.desc annotation is not the Mixin 0.8.5 @Desc type.");
                warnings.add("At.desc uses an unsupported nested annotation type.");
            }
        }
        if (value == null || value.isBlank()) {
            warnings.add("@At.value is required but is absent or blank.");
        }
        if (targetSelector.status() == AtTargetSelectorStatus.INVALID_DESCRIPTOR
                || targetSelector.status() == AtTargetSelectorStatus.INVALID_SELECTOR
                || targetSelector.status() == AtTargetSelectorStatus.UNSUPPORTED_SYNTAX) {
            warnings.add("The @At target selector is malformed or uses syntax not interpreted by this inventory.");
        }
        if (invalidDesc) warnings.add("The nested @At.desc selector is malformed.");
        if (descSelector != null && !rawTarget.isBlank()) {
            warnings.add("Both @At.target and @At.desc are declared; both raw selector forms are retained without choosing one.");
        }

        String baseValue = value == null ? "" : value.split(":", 2)[0];
        if (Set.of("INVOKE", "INVOKE_ASSIGN", "INVOKE_STRING", "FIELD", "NEW").contains(baseValue)
                && rawTarget.isBlank() && descSelector == null) {
            warnings.add("@At.value commonly requires a member target for this injection-point kind; no target is declared.");
        }

        AtFindingStatus status;
        if (value == null || value.isBlank()) status = AtFindingStatus.INVALID_REQUIRED_VALUE;
        else if (targetSelector.status() == AtTargetSelectorStatus.INVALID_DESCRIPTOR
                || targetSelector.status() == AtTargetSelectorStatus.INVALID_SELECTOR
                || targetSelector.status() == AtTargetSelectorStatus.UNSUPPORTED_SYNTAX) {
            status = AtFindingStatus.INVALID_TARGET_SELECTOR;
        } else if (invalidDesc) status = AtFindingStatus.INVALID_DESC_SELECTOR;
        else status = AtFindingStatus.CAPTURED;

        String evidence = switch (status) {
            case INVALID_REQUIRED_VALUE -> "The required @At.value element is missing or blank.";
            case INVALID_TARGET_SELECTOR -> "The raw @At.target is preserved, but its selector syntax could not be safely parsed.";
            case INVALID_DESC_SELECTOR -> "The nested @At.desc data is preserved as a finding but its source signature is malformed.";
            case CAPTURED -> "Source-side @At metadata and effective Mixin 0.8.5 defaults were captured; target bytecode matching is not evaluated.";
        };

        return new AtFinding(
                index, value, valueKind, rawTarget, targetSelector, descSelector,
                integerAnnotationValue(annotation, "opcode", -1),
                integerAnnotationValue(annotation, "ordinal", -1),
                enumAnnotationValue(annotation, "shift", "NONE"),
                integerAnnotationValue(annotation, "by", 0),
                stringAnnotationValue(annotation, "slice") == null ? "" : stringAnnotationValue(annotation, "slice"),
                stringArrayValues(annotation, "args"),
                booleanAnnotationValue(annotation, "remap", defaultRemap),
                stringAnnotationValue(annotation, "id") == null ? "" : stringAnnotationValue(annotation, "id"),
                status, evidence, warnings);
    }

    private static boolean isBuiltInAtValue(String value) {
        if (value == null || value.isBlank()) return false;
        String base = value.split(":", 2)[0];
        return Set.of("HEAD", "RETURN", "TAIL", "INVOKE", "INVOKE_ASSIGN", "FIELD",
                "NEW", "INVOKE_STRING", "JUMP", "CONSTANT").contains(base);
    }

    private static AtTargetSelector parseAtTargetSelector(String raw) {
        if (raw == null || raw.isBlank()) {
            return new AtTargetSelector(raw == null ? "" : raw, null, null, null,
                    AtTargetDescriptorKind.NONE, AtTargetSelectorStatus.NOT_DECLARED,
                    "No @At.target string was declared.");
        }
        String compact = raw.replaceAll("\\s", "");
        if (compact.contains("->")) {
            return new AtTargetSelector(raw, null, null, null, AtTargetDescriptorKind.NONE,
                    AtTargetSelectorStatus.UNSUPPORTED_SYNTAX,
                    "Chained selector syntax is retained raw and not interpreted.");
        }

        String owner = null;
        String memberAndDescriptor = compact;
        if (compact.startsWith("L")) {
            int semi = compact.indexOf(';');
            if (semi <= 1) {
                return new AtTargetSelector(raw, null, null, null, AtTargetDescriptorKind.NONE,
                        AtTargetSelectorStatus.INVALID_SELECTOR, "Owner-qualified selector has no valid Lowner; prefix.");
            }
            owner = compact.substring(1, semi).replace('.', '/');
            memberAndDescriptor = compact.substring(semi + 1);
        } else {
            int signatureStart = firstPositive(compact.indexOf('('), compact.indexOf(':'));
            String head = signatureStart < 0 ? compact : compact.substring(0, signatureStart);
            int split = head.lastIndexOf('.');
            if (split < 0) split = head.lastIndexOf('/');
            if (split > 0 && split < head.length() - 1) {
                owner = head.substring(0, split).replace('.', '/');
                memberAndDescriptor = head.substring(split + 1)
                        + (signatureStart < 0 ? "" : compact.substring(signatureStart));
            }
        }

        int methodStart = memberAndDescriptor.indexOf('(');
        int fieldSeparator = memberAndDescriptor.indexOf(':');
        String name;
        String descriptor = null;
        AtTargetDescriptorKind descriptorKind = AtTargetDescriptorKind.NONE;
        AtTargetSelectorStatus status;
        if (methodStart >= 0) {
            name = memberAndDescriptor.substring(0, methodStart);
            descriptor = memberAndDescriptor.substring(methodStart);
            descriptorKind = AtTargetDescriptorKind.METHOD;
            status = isValidMethodDescriptor(descriptor)
                    ? AtTargetSelectorStatus.METHOD_DESCRIPTOR : AtTargetSelectorStatus.INVALID_DESCRIPTOR;
        } else if (fieldSeparator >= 0) {
            name = memberAndDescriptor.substring(0, fieldSeparator);
            descriptor = memberAndDescriptor.substring(fieldSeparator + 1);
            descriptorKind = AtTargetDescriptorKind.FIELD;
            status = isValidFieldDescriptor(descriptor)
                    ? AtTargetSelectorStatus.FIELD_DESCRIPTOR : AtTargetSelectorStatus.INVALID_DESCRIPTOR;
        } else {
            name = memberAndDescriptor;
            status = name.isBlank() && owner != null
                    ? AtTargetSelectorStatus.OWNER_ONLY : AtTargetSelectorStatus.MEMBER_NAME_ONLY;
        }

        if (name != null && ((name.isBlank() && status != AtTargetSelectorStatus.OWNER_ONLY)
                || name.contains("*") || name.contains("+")
                || name.contains("{") || name.contains("}") || name.contains("->"))) {
            status = AtTargetSelectorStatus.UNSUPPORTED_SYNTAX;
        }
        if (owner != null && (owner.isBlank() || owner.startsWith("/") || owner.endsWith("/")
                || owner.contains("//") || owner.contains("..") || owner.indexOf(';') >= 0)) {
            status = AtTargetSelectorStatus.INVALID_SELECTOR;
        }
        if (name != null && (name.contains("/") || name.contains(";") || name.contains(":"))) {
            status = AtTargetSelectorStatus.INVALID_SELECTOR;
        }
        String evidence = switch (status) {
            case NOT_DECLARED -> "No target is required for several built-in injection-point codes.";
            case MEMBER_NAME_ONLY -> "Member name captured without an explicit owner or descriptor.";
            case METHOD_DESCRIPTOR -> "Owner/name and an exact JVM method descriptor were captured.";
            case FIELD_DESCRIPTOR -> "Owner/name and an exact JVM field descriptor were captured.";
            case OWNER_ONLY -> "Owner-only selector captured; a target member is not specified.";
            case INVALID_DESCRIPTOR -> "The target descriptor is not a valid canonical JVM method or field descriptor.";
            case INVALID_SELECTOR -> "The owner/member selector has invalid syntax.";
            case UNSUPPORTED_SYNTAX -> "The raw selector was retained, but wildcard, quantifier, or chain syntax is not interpreted.";
        };
        return new AtTargetSelector(raw, owner, name, descriptor, descriptorKind, status, evidence);
    }

    private static int firstPositive(int first, int second) {
        if (first < 0) return second;
        if (second < 0) return first;
        return Math.min(first, second);
    }

    private static boolean isValidFieldDescriptor(String descriptor) {
        try {
            if (descriptor == null || descriptor.isBlank()) return false;
            Type type = Type.getType(descriptor);
            return type.getSort() != Type.VOID && type.getSort() != Type.METHOD
                    && type.getDescriptor().equals(descriptor);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static List<SliceFinding> readSliceFindings(AnnotationNode injector, boolean defaultRemap) {
        Object raw = annotationValue(injector, "slice");
        if (raw == null) return List.of();
        List<?> entries = raw instanceof List<?> values ? values : List.of(raw);
        List<SliceFinding> findings = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Object item = entries.get(i);
            if (!(item instanceof AnnotationNode annotation)) {
                findings.add(new SliceFinding(i, "", null, null, null,
                        SliceFindingStatus.INVALID_NESTED_VALUE,
                        "The @Slice array entry is not an annotation node and cannot be interpreted.",
                        List.of("Unexpected @Slice array entry type: "
                                + (item == null ? "null" : item.getClass().getName()) + ".")));
            } else if (!SLICE_ANNOTATION.equals(annotation.desc)) {
                String id = stringAnnotationValue(annotation, "id");
                findings.add(new SliceFinding(i, id == null ? "" : id, annotation.desc, null, null,
                        SliceFindingStatus.UNSUPPORTED_NESTED_ANNOTATION,
                        "A nested annotation was retained, but it is not the Mixin 0.8.5 @Slice type.",
                        List.of("Expected " + SLICE_ANNOTATION + " but found " + annotation.desc + ".")));
            } else {
                findings.add(createSliceFinding(i, annotation, defaultRemap));
            }
        }
        return List.copyOf(findings);
    }

    private static SliceFinding createSliceFinding(int index, AnnotationNode annotation, boolean defaultRemap) {
        String id = stringAnnotationValue(annotation, "id");
        if (id == null) id = "";
        SliceBoundaryFinding from = createSliceBoundary(annotation, SliceBoundaryKind.FROM, defaultRemap);
        SliceBoundaryFinding to = createSliceBoundary(annotation, SliceBoundaryKind.TO, defaultRemap);
        List<String> warnings = new ArrayList<>();
        warnings.addAll(from.warnings());
        warnings.addAll(to.warnings());
        boolean boundariesValid = (from.status() == SliceBoundaryStatus.EXPLICIT_AT_CAPTURED
                || from.status() == SliceBoundaryStatus.DEFAULT_AT_CAPTURED)
                && (to.status() == SliceBoundaryStatus.EXPLICIT_AT_CAPTURED
                || to.status() == SliceBoundaryStatus.DEFAULT_AT_CAPTURED);
        SliceFindingStatus status = boundariesValid
                ? SliceFindingStatus.CAPTURED : SliceFindingStatus.BOUNDARY_REQUIRES_REVIEW;
        String evidence = boundariesValid
                ? "Explicit @Slice boundaries and Mixin 0.8.5 defaults were captured; target instruction ordering and slice validity are not evaluated."
                : "The @Slice declaration was retained, but at least one boundary is malformed or unsupported; no instruction matching is attempted.";
        return new SliceFinding(index, id, annotation.desc, from, to, status, evidence, warnings);
    }

    private static SliceBoundaryFinding createSliceBoundary(
            AnnotationNode slice, SliceBoundaryKind kind, boolean defaultRemap) {
        String key = kind == SliceBoundaryKind.FROM ? "from" : "to";
        boolean declared = hasAnnotationValue(slice, key);
        Object raw = annotationValue(slice, key);
        if (!declared) {
            AnnotationNode defaultAt = new AnnotationNode(AT_ANNOTATION);
            defaultAt.visit("value", kind == SliceBoundaryKind.FROM ? "HEAD" : "TAIL");
            defaultAt.visitEnd();
            AtFinding at = createAtFinding(0, defaultAt, defaultRemap);
            String boundaryName = kind == SliceBoundaryKind.FROM ? "@Slice.from" : "@Slice.to";
            String defaultValue = kind == SliceBoundaryKind.FROM ? "HEAD" : "TAIL";
            return new SliceBoundaryFinding(kind, false, AT_ANNOTATION, at,
                    SliceBoundaryStatus.DEFAULT_AT_CAPTURED,
                    boundaryName + " was not explicitly declared; Mixin 0.8.5 defaults it to @At(\"" + defaultValue + "\").",
                    List.of());
        }
        if (!(raw instanceof AnnotationNode nested)) {
            return new SliceBoundaryFinding(kind, true, null, null,
                    SliceBoundaryStatus.INVALID_VALUE_TYPE,
                    "The declared " + key + " boundary is not an annotation value.",
                    List.of("@Slice." + key + " must contain an @At annotation."));
        }
        if (!AT_ANNOTATION.equals(nested.desc)) {
            return new SliceBoundaryFinding(kind, true, nested.desc, null,
                    SliceBoundaryStatus.UNSUPPORTED_NESTED_ANNOTATION,
                    "The declared " + key + " boundary uses an annotation type other than Mixin @At.",
                    List.of("Expected " + AT_ANNOTATION + " for @Slice." + key + " but found " + nested.desc + "."));
        }
        AtFinding at = createAtFinding(0, nested, defaultRemap);
        if (at.status() != AtFindingStatus.CAPTURED) {
            return new SliceBoundaryFinding(kind, true, nested.desc, at,
                    SliceBoundaryStatus.INVALID_AT,
                    "The explicit " + key + " @At was captured but contains invalid source-side metadata.",
                    at.warnings());
        }
        return new SliceBoundaryFinding(kind, true, nested.desc, at,
                SliceBoundaryStatus.EXPLICIT_AT_CAPTURED,
                "The explicit " + key + " @At metadata was captured; target instruction matching is not evaluated.",
                List.of());
    }

    private static List<ConstantFinding> readConstantFindings(AnnotationNode injector, InjectorAnnotationKind kind) {
        List<ConstantFinding> findings = new ArrayList<>();
        List<AnnotationNode> annotations = annotationArrayValue(injector, "constant");
        for (int i = 0; i < annotations.size(); i++) {
            AnnotationNode annotation = annotations.get(i);
            if (CONSTANT_ANNOTATION.equals(annotation.desc)) {
                findings.add(createConstantFinding(i, annotation));
            } else {
                findings.add(new ConstantFinding(i, true, null, null, null, null, null, null,
                        null, -1, "", List.of(), null,
                        ConstantFindingStatus.UNSUPPORTED_NESTED_ANNOTATION,
                        "A nested annotation with an unsupported type was found in @ModifyConstant.constant.",
                        List.of("Expected Mixin 0.8.5 @Constant but found " + annotation.desc + ".")));
            }
        }
        if (kind == InjectorAnnotationKind.MODIFY_CONSTANT && findings.isEmpty()) {
            findings.add(new ConstantFinding(0, false, null, null, null, null, null, null,
                    null, -1, "", List.of(), null,
                    ConstantFindingStatus.MATCH_HANDLER_RETURN_TYPE,
                    "No @Constant discriminator was declared; Mixin matches constants using the handler return type.",
                    List.of()));
        }
        return List.copyOf(findings);
    }

    private static ConstantFinding createConstantFinding(int index, AnnotationNode annotation) {
        List<String> warnings = new ArrayList<>();
        Boolean nullValue = nullableBooleanAnnotationValue(annotation, "nullValue");
        Integer intValue = nullableIntegerAnnotationValue(annotation, "intValue");
        Object rawFloat = annotationValue(annotation, "floatValue");
        Float floatValue = rawFloat instanceof Float number ? number : null;
        Object rawLong = annotationValue(annotation, "longValue");
        Long longValue = rawLong instanceof Long number ? number : null;
        Object rawDouble = annotationValue(annotation, "doubleValue");
        Double doubleValue = rawDouble instanceof Double number ? number : null;
        Object rawString = annotationValue(annotation, "stringValue");
        String stringValue = rawString instanceof String text ? text : null;
        Object rawClass = annotationValue(annotation, "classValue");
        String classValueDescriptor = rawClass instanceof Type type ? type.getDescriptor() : null;
        int ordinal = integerAnnotationValue(annotation, "ordinal", -1);
        String sliceId = stringAnnotationValue(annotation, "slice");
        if (sliceId == null) sliceId = "";
        List<String> conditions = enumArrayAnnotationValues(annotation, "expandZeroConditions");
        List<String> validConditions = List.of("LESS_THAN_ZERO", "LESS_THAN_OR_EQUAL_TO_ZERO",
                "GREATER_THAN_OR_EQUAL_TO_ZERO", "GREATER_THAN_ZERO");
        List<String> invalidConditions = conditions.stream().filter(value -> !validConditions.contains(value)).toList();
        if (!invalidConditions.isEmpty()) warnings.add("Unsupported expandZeroConditions enum values: " + invalidConditions);

        Set<String> discriminators = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(nullValue)) discriminators.add("NULL");
        if (intValue != null || !conditions.isEmpty()) discriminators.add("INT");
        if (floatValue != null) discriminators.add("FLOAT");
        if (longValue != null) discriminators.add("LONG");
        if (doubleValue != null) discriminators.add("DOUBLE");
        if (stringValue != null) discriminators.add("STRING");
        if (classValueDescriptor != null) discriminators.add("CLASS");
        ConstantFindingStatus status;
        if (!invalidConditions.isEmpty()) status = ConstantFindingStatus.INVALID_ENUM_VALUE;
        else if (discriminators.size() > 1) status = ConstantFindingStatus.CONFLICTING_DISCRIMINATORS;
        else if (discriminators.isEmpty()) status = ConstantFindingStatus.MATCH_HANDLER_RETURN_TYPE;
        else status = ConstantFindingStatus.EXPLICIT_TYPED_DISCRIMINATOR;

        String evidence = switch (status) {
            case MATCH_HANDLER_RETURN_TYPE -> "No explicit constant type/value discriminator was encoded; Mixin uses the handler return type.";
            case EXPLICIT_TYPED_DISCRIMINATOR -> "Explicit constant discriminator values were captured; target bytecode constants are not searched.";
            case CONFLICTING_DISCRIMINATORS -> "More than one constant type discriminator is declared; Mixin's typed matching contract may reject this annotation.";
            case INVALID_ENUM_VALUE -> "At least one expandZeroConditions enum value is not recognized by Mixin 0.8.5.";
            case UNSUPPORTED_NESTED_ANNOTATION -> "The nested annotation type is not Mixin 0.8.5 @Constant.";
        };
        return new ConstantFinding(index, true, nullValue, intValue, floatValue, longValue, doubleValue,
                stringValue, classValueDescriptor, ordinal, sliceId, conditions,
                nullableBooleanAnnotationValue(annotation, "log"), status, evidence, warnings);
    }

    private static Boolean nullableBooleanAnnotationValue(AnnotationNode annotation, String key) {
        Object value = annotationValue(annotation, key);
        return value instanceof Boolean bool ? bool : null;
    }

    private static String enumAnnotationValue(AnnotationNode annotation, String key, String defaultValue) {
        Object value = annotationValue(annotation, key);
        if (value instanceof String[] enumValue && enumValue.length >= 2) return enumValue[1];
        return defaultValue;
    }

    private static List<String> enumArrayAnnotationValues(AnnotationNode annotation, String key) {
        Object value = annotationValue(annotation, key);
        if (!(value instanceof List<?> values)) return List.of();
        List<String> result = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof String[] enumValue && enumValue.length >= 2) result.add(enumValue[1]);
            else result.add(String.valueOf(item));
        }
        return List.copyOf(result);
    }

    private static InjectorTargetSelector parseMethodSelector(String raw) {
        if (raw == null || raw.isBlank()) return invalidSelector(raw,
                InjectorSelectorStatus.INVALID_SELECTOR, "Selector is empty.");
        String compact = raw.replaceAll("\\s", "");
        String tail = null;
        int arrow = compact.indexOf("->");
        if (arrow >= 0) {
            tail = compact.substring(arrow + 2);
            compact = compact.substring(0, arrow);
        }
        String descriptor = null;
        int paren = compact.indexOf('(');
        int colon = compact.indexOf(':');
        if (paren >= 0) {
            descriptor = compact.substring(paren);
            compact = compact.substring(0, paren);
        } else if (colon >= 0) {
            descriptor = compact.substring(colon + 1);
            compact = compact.substring(0, colon);
        }

        String owner = null;
        String name = compact;
        int semi = compact.indexOf(';');
        if (compact.startsWith("L") && semi > 0) {
            owner = compact.substring(1, semi).replace('.', '/');
            name = compact.substring(semi + 1);
        } else {
            int dot = compact.lastIndexOf('.');
            if (dot > 0) {
                owner = compact.substring(0, dot).replace('.', '/');
                name = compact.substring(dot + 1);
            } else if (compact.contains("/")) {
                owner = compact.replace('.', '/');
                name = null;
            }
        }
        String quantifier = "DEFAULT";
        boolean invalidQuantifier = false;
        if (name != null && name.endsWith("*")) {
            quantifier = "ANY";
            name = name.substring(0, name.length() - 1);
        } else if (name != null && name.endsWith("+")) {
            quantifier = "PLUS";
            name = name.substring(0, name.length() - 1);
        } else if (name != null && name.endsWith("}")) {
            int brace = name.lastIndexOf('{');
            String q = brace < 0 ? "" : name.substring(brace);
            if (validQuantifier(q)) {
                quantifier = q;
                name = name.substring(0, brace);
            } else invalidQuantifier = true;
        } else if (name != null && name.contains("{")) invalidQuantifier = true;

        boolean badDescriptor = descriptor != null && !isValidMethodDescriptor(descriptor);
        InjectorSelectorStatus status;
        if (badDescriptor) status = InjectorSelectorStatus.INVALID_DESCRIPTOR;
        else if (invalidQuantifier || (name != null && name.isBlank())) status = InjectorSelectorStatus.INVALID_SELECTOR;
        else if (tail != null && !tail.isBlank()) status = InjectorSelectorStatus.UNSUPPORTED_EXTENSION;
        else if (!"DEFAULT".equals(quantifier)) status = InjectorSelectorStatus.WILDCARD;
        else if (name == null || name.isEmpty()) status = owner == null
                ? InjectorSelectorStatus.INVALID_SELECTOR : InjectorSelectorStatus.OWNER_ONLY;
        else if (descriptor != null) status = InjectorSelectorStatus.METHOD_DESCRIPTOR;
        else status = InjectorSelectorStatus.METHOD_NAME_ONLY;

        String evidence = switch (status) {
            case METHOD_NAME_ONLY -> "Method name selector captured without descriptor.";
            case METHOD_DESCRIPTOR -> "Syntactically valid JVM method descriptor captured.";
            case WILDCARD -> "Wildcard/quantifier retained without resolving matches.";
            case OWNER_ONLY -> "Owner-only selector retained; method selection is unresolved.";
            case INVALID_DESCRIPTOR -> "Selector has an invalid JVM method descriptor.";
            case INVALID_SELECTOR -> "Selector name/quantifier syntax is invalid.";
            case UNSUPPORTED_EXTENSION -> "Selector chain retained in raw input but not interpreted.";
        };
        return new InjectorTargetSelector(InjectorSelectorSource.METHOD_STRING, raw, owner, name,
                descriptor, quantifier, null, null, status, evidence);
    }

    private static boolean validQuantifier(String value) {
        if (value == null || value.length() < 3 || value.charAt(0) != '{'
                || value.charAt(value.length() - 1) != '}') return false;
        String body = value.substring(1, value.length() - 1);
        int comma = body.indexOf(',');
        if (comma < 0) return !body.isEmpty() && body.chars().allMatch(Character::isDigit);
        if (body.indexOf(',', comma + 1) >= 0) return false;
        String low = body.substring(0, comma), high = body.substring(comma + 1);
        return (low.isEmpty() || low.chars().allMatch(Character::isDigit))
                && (high.isEmpty() || high.chars().allMatch(Character::isDigit))
                && !(low.isEmpty() && high.isEmpty());
    }

    private static InjectorTargetSelector parseDescSelector(AnnotationNode desc) {
        Object rawName = annotationValue(desc, "value");
        String name = rawName instanceof String s ? s : null;
        Object rawOwner = annotationValue(desc, "owner");
        Type ownerType = rawOwner == null ? Type.VOID_TYPE : rawOwner instanceof Type t ? t : null;
        String owner = null;
        boolean invalid = name == null || name.isBlank() || ownerType == null;
        if (ownerType != null && ownerType.getSort() != Type.VOID && ownerType.getSort() != Type.OBJECT) invalid = true;
        else if (ownerType != null && ownerType.getSort() == Type.OBJECT) owner = ownerType.getInternalName();

        Object rawReturn = annotationValue(desc, "ret");
        Type returnType = rawReturn == null ? Type.VOID_TYPE : rawReturn instanceof Type t ? t : null;
        if (returnType == null || returnType.getSort() == Type.METHOD) invalid = true;
        Object rawArgs = annotationValue(desc, "args");
        List<Type> args = new ArrayList<>();
        if (rawArgs instanceof List<?> values) {
            for (Object value : values) {
                if (value instanceof Type t && t.getSort() != Type.VOID && t.getSort() != Type.METHOD) args.add(t);
                else invalid = true;
            }
        } else if (rawArgs != null) invalid = true;

        String descriptor = null;
        if (!invalid) {
            descriptor = Type.getMethodDescriptor(returnType, args.toArray(Type[]::new));
            invalid = !isValidMethodDescriptor(descriptor);
        }
        int min = integerAnnotationValue(desc, "min", 0);
        int max = integerAnnotationValue(desc, "max", Integer.MAX_VALUE);
        // Mixin treats negative min values as ignored and max values below 1 as unbounded.
        if (max >= 1 && min > max) invalid = true;
        String raw = "@Desc(owner=" + (owner == null ? "<mixin-target>" : owner)
                + ", value=" + name + ", descriptor=" + descriptor + ", min=" + min + ", max=" + max + ")";
        InjectorSelectorStatus status = invalid ? InjectorSelectorStatus.INVALID_SELECTOR
                : InjectorSelectorStatus.METHOD_DESCRIPTOR;
        return new InjectorTargetSelector(InjectorSelectorSource.DESC_ANNOTATION, raw, owner,
                name, descriptor, "DESC", min, max, status,
                invalid ? "Invalid @Desc owner/name/signature/match range."
                        : "Normalized to an exact source-side JVM method descriptor.");
    }

    private static InjectorTargetSelector invalidSelector(
            String raw, InjectorSelectorStatus status, String evidence) {
        return new InjectorTargetSelector(InjectorSelectorSource.METHOD_STRING, raw,
                null, null, null, null, null, null, status, evidence);
    }

    private static boolean isValidMethodDescriptor(String descriptor) {
        try {
            if (descriptor == null || !descriptor.startsWith("(")) return false;
            Type method = Type.getMethodType(descriptor);
            Type[] arguments = method.getArgumentTypes();
            for (Type argument : arguments) {
                if (argument.getSort() == Type.VOID || argument.getSort() == Type.METHOD) return false;
            }
            Type returnType = method.getReturnType();
            return returnType.getSort() != Type.METHOD
                    && Type.getMethodDescriptor(returnType, arguments).equals(descriptor);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static List<String> stringArrayValues(AnnotationNode annotation, String key) {
        Object value = annotationValue(annotation, key);
        if (!(value instanceof List<?> values)) return List.of();
        List<String> result = new ArrayList<>();
        for (Object item : values) if (item instanceof String s) result.add(s);
        return List.copyOf(result);
    }

    private static List<AnnotationNode> annotationArrayValue(AnnotationNode annotation, String key) {
        Object value = annotationValue(annotation, key);
        if (value instanceof AnnotationNode one) return List.of(one);
        if (!(value instanceof List<?> values)) return List.of();
        List<AnnotationNode> result = new ArrayList<>();
        for (Object item : values) if (item instanceof AnnotationNode nested) result.add(nested);
        return List.copyOf(result);
    }

    private static Integer nullableIntegerAnnotationValue(AnnotationNode annotation, String key) {
        Object value = annotationValue(annotation, key);
        return value instanceof Integer number ? number : null;
    }

    private static int integerAnnotationValue(AnnotationNode annotation, String key, int defaultValue) {
        Integer value = nullableIntegerAnnotationValue(annotation, key);
        return value == null ? defaultValue : value;
    }

    private static MemberAnnotationKind memberAnnotationKind(String descriptor) {
        return switch (descriptor) {
            case SHADOW_ANNOTATION -> MemberAnnotationKind.SHADOW;
            case OVERWRITE_ANNOTATION -> MemberAnnotationKind.OVERWRITE;
            case ACCESSOR_ANNOTATION -> MemberAnnotationKind.ACCESSOR;
            case INVOKER_ANNOTATION -> MemberAnnotationKind.INVOKER;
            default -> null;
        };
    }

    private static MemberAnnotationFinding memberAnnotationFinding(
            String archivePath,
            String configPath,
            String section,
            String mixinClassName,
            List<String> sourceTargets,
            MemberAnnotationKind kind,
            MemberElementKind elementKind,
            String sourceName,
            String sourceDescriptor,
            int sourceAccess,
            AnnotationNode annotation,
            boolean pluginDependent,
            boolean mixinRemap) {
        List<String> warnings = new ArrayList<>();
        List<String> aliases = stringArrayAnnotationValue(annotation, "aliases");
        LinkedHashSet<String> targetNames = new LinkedHashSet<>();
        String prefix = null;
        String explicitValue = stringAnnotationValue(annotation, "value");
        String constraints = stringAnnotationValue(annotation, "constraints");
        boolean remap = booleanAnnotationValue(annotation, "remap", mixinRemap);
        String operation = elementKind.name();

        boolean validPlacement = kind == MemberAnnotationKind.SHADOW
                || elementKind == MemberElementKind.METHOD;
        boolean invalidAnnotationValue = false;
        if (!validPlacement) {
            warnings.add(kind + " is expected on a method, but was found on a field.");
        }
        if (kind == MemberAnnotationKind.SHADOW && !aliases.isEmpty()
                && (sourceAccess & Opcodes.ACC_PRIVATE) == 0) {
            warnings.add("Shadow aliases are only supported for private members; this source member is not private.");
            invalidAnnotationValue = true;
        }
        if ((kind == MemberAnnotationKind.ACCESSOR || kind == MemberAnnotationKind.INVOKER)
                && (sourceAccess & Opcodes.ACC_ABSTRACT) == 0) {
            warnings.add(kind + " methods must be abstract.");
            invalidAnnotationValue = true;
        }

        boolean validDescriptor = isValidMemberDescriptor(elementKind, sourceDescriptor);
        if (!validDescriptor) {
            warnings.add("Member descriptor is not a valid JVM " + elementKind.name().toLowerCase() + " descriptor.");
        }

        switch (kind) {
            case SHADOW -> {
                String declaredPrefix = stringAnnotationValue(annotation, "prefix");
                if (elementKind == MemberElementKind.METHOD) {
                    prefix = declaredPrefix == null ? "shadow$" : declaredPrefix;
                    String targetName = sourceName;
                    if (!prefix.isEmpty() && sourceName.startsWith(prefix)) {
                        targetName = sourceName.substring(prefix.length());
                    } else if (!prefix.isEmpty()) {
                        warnings.add("Source method name does not start with the declared/default shadow prefix; the unstripped name is retained as a candidate.");
                    }
                    addSelectorCandidate(targetNames, targetName);
                } else {
                    // Shadow prefixes are method-only. An explicit field prefix is an error condition;
                    // the default annotation value is not materialized when it was not written.
                    if (declaredPrefix != null) {
                        warnings.add("A @Shadow prefix was explicitly supplied on a field; prefixes only apply to shadow methods and were not stripped.");
                        invalidAnnotationValue = true;
                    }
                    addSelectorCandidate(targetNames, sourceName);
                }
                aliases.forEach(alias -> addSelectorCandidate(targetNames, alias));
                operation = elementKind == MemberElementKind.FIELD ? "FIELD" : "METHOD";
            }
            case OVERWRITE -> {
                addSelectorCandidate(targetNames, sourceName);
                aliases.forEach(alias -> addSelectorCandidate(targetNames, alias));
                operation = "METHOD";
            }
            case ACCESSOR -> {
                operation = accessorOperation(sourceDescriptor, warnings);
                if (operation.equals("UNSUPPORTED_SHAPE")) invalidAnnotationValue = true;
                if (explicitValue != null && !explicitValue.isBlank()) {
                    addSelectorCandidate(targetNames, explicitValue);
                } else {
                    String inferred = inferAccessorTarget(sourceName);
                    if (inferred == null) {
                        warnings.add("Accessor target name is neither explicit nor inferable from a get/is/set method prefix.");
                    } else {
                        addSelectorCandidate(targetNames, inferred);
                    }
                }
            }
            case INVOKER -> {
                if (explicitValue != null && !explicitValue.isBlank()) {
                    addSelectorCandidate(targetNames, explicitValue);
                } else {
                    String inferred = inferInvokerTarget(sourceName);
                    if (inferred == null) {
                        warnings.add("Invoker target name is neither explicit nor inferable from a call/invoke/new method prefix.");
                    } else {
                        addSelectorCandidate(targetNames, inferred);
                    }
                }
                operation = targetNames.stream().anyMatch(name -> name.equals("<init>") || name.contains("."))
                        ? "CONSTRUCTOR" : "METHOD";
            }
        }

        MemberResolutionStatus status;
        if (!validPlacement) {
            status = MemberResolutionStatus.INVALID_ANNOTATION_PLACEMENT;
        } else if (!validDescriptor) {
            status = MemberResolutionStatus.INVALID_MEMBER_DESCRIPTOR;
        } else if (invalidAnnotationValue) {
            status = MemberResolutionStatus.INVALID_ANNOTATION_VALUE;
        } else if (targetNames.isEmpty()) {
            status = MemberResolutionStatus.INVALID_MEMBER_SELECTOR;
        } else if (pluginDependent) {
            status = MemberResolutionStatus.PLUGIN_DEPENDENT;
        } else if (remap) {
            status = MemberResolutionStatus.MEMBER_REMAP_UNRESOLVED;
        } else {
            status = MemberResolutionStatus.MEMBER_REMAP_DISABLED_UNVERIFIED;
        }

        String evidence = switch (status) {
            case INVALID_ANNOTATION_PLACEMENT ->
                    "The annotation was captured on an element kind that is not supported for this Mixin annotation.";
            case INVALID_ANNOTATION_VALUE ->
                    "The annotation contains a value or member modifier combination that violates a known Mixin contract; details are listed in warnings.";
            case INVALID_MEMBER_DESCRIPTOR ->
                    "The annotation was captured, but the source member descriptor is malformed and cannot be compared.";
            case INVALID_MEMBER_SELECTOR ->
                    "The annotation was captured, but no explicit or statically inferable target member selector is available.";
            case PLUGIN_DEPENDENT ->
                    "The config declares a Mixin plugin; runtime plugin decisions may alter targets or member behavior. Plugin code was not executed.";
            case MEMBER_REMAP_UNRESOLVED ->
                    "Exact source member name and descriptor were captured. No versioned 1.20.1-to-1.21.1 member mapping was supplied, so target member presence is not asserted.";
            case MEMBER_REMAP_DISABLED_UNVERIFIED ->
                    "The annotation sets remap=false. Exact source member data was captured, but target member and descriptor parity are not verified.";
        };

        return new MemberAnnotationFinding(
                archivePath, configPath, section, mixinClassName, sourceTargets,
                kind, elementKind, sourceName, sourceDescriptor, prefix, aliases, explicitValue,
                constraints, List.copyOf(targetNames), remap, operation, status, evidence, warnings);
    }

    private static boolean isValidMemberDescriptor(MemberElementKind kind, String descriptor) {
        try {
            if (descriptor == null || descriptor.isBlank()) return false;
            if (kind == MemberElementKind.FIELD) {
                Type type = Type.getType(descriptor);
                return type.getSort() != Type.METHOD && type.getSort() != Type.VOID;
            }
            return descriptor.startsWith("(") && Type.getMethodType(descriptor) != null;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String accessorOperation(String descriptor, List<String> warnings) {
        try {
            Type method = Type.getMethodType(descriptor);
            Type[] arguments = method.getArgumentTypes();
            Type result = method.getReturnType();
            if (arguments.length == 0 && result.getSort() != Type.VOID) return "GETTER";
            if (arguments.length == 1 && result.getSort() == Type.VOID) return "SETTER";
        } catch (RuntimeException ignored) {
            // The invalid descriptor is already represented by the caller's explicit status.
        }
        warnings.add("Accessor method descriptor is not a zero-argument getter or one-argument void setter shape.");
        return "UNSUPPORTED_SHAPE";
    }

    private static String inferAccessorTarget(String methodName) {
        for (String prefix : List.of("get", "is", "set")) {
            if (methodName.startsWith(prefix) && methodName.length() > prefix.length()
                    && Character.isUpperCase(methodName.charAt(prefix.length()))) {
                return decapitalize(methodName.substring(prefix.length()));
            }
        }
        return null;
    }

    private static String inferInvokerTarget(String methodName) {
        for (String prefix : List.of("call", "invoke")) {
            if (methodName.startsWith(prefix) && methodName.length() > prefix.length()
                    && Character.isUpperCase(methodName.charAt(prefix.length()))) {
                return decapitalize(methodName.substring(prefix.length()));
            }
        }
        return null;
    }

    private static String decapitalize(String value) {
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0))
                && Character.isUpperCase(value.charAt(1))) return value;
        if (value.isEmpty()) return value;
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static void addSelectorCandidate(Set<String> candidates, String value) {
        if (value != null && !value.isBlank()) candidates.add(value);
    }

    private static boolean hasAnnotationValue(AnnotationNode annotation, String name) {
        if (annotation.values == null) return false;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) return true;
        }
        return false;
    }

    private static Object annotationValue(AnnotationNode annotation, String name) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static String stringAnnotationValue(AnnotationNode annotation, String name) {
        Object value = annotationValue(annotation, name);
        return value instanceof String string ? string : null;
    }

    private static boolean booleanAnnotationValue(AnnotationNode annotation, String name, boolean defaultValue) {
        Object value = annotationValue(annotation, name);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    private static List<String> stringArrayAnnotationValue(AnnotationNode annotation, String name) {
        Object value = annotationValue(annotation, name);
        if (!(value instanceof List<?> values)) return List.of();
        return values.stream().filter(String.class::isInstance).map(String.class::cast).distinct().toList();
    }

    private static List<ParsedTarget> readMixinTargets(ClassNode mixin) {
        List<ParsedTarget> targets = new ArrayList<>();
        for (List<AnnotationNode> annotations : annotationLists(mixin)) {
            for (AnnotationNode annotation : annotations) {
                if (!MIXIN_ANNOTATION.equals(annotation.desc) || annotation.values == null) continue;
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    String key = String.valueOf(annotation.values.get(i));
                    Object value = annotation.values.get(i + 1);
                    if (!(value instanceof List<?> values)) continue;
                    for (Object item : values) {
                        if (key.equals("value") && item instanceof Type type
                                && type.getSort() == Type.OBJECT) {
                            targets.add(new ParsedTarget(type.getInternalName(), TargetSelectorForm.CLASS_LITERAL));
                        } else if (key.equals("targets") && item instanceof String name) {
                            targets.add(new ParsedTarget(name.replace('.', '/'), TargetSelectorForm.STRING_TARGET));
                        }
                    }
                }
            }
        }
        return targets.stream().distinct()
                .sorted(Comparator.comparing(ParsedTarget::selector).thenComparing(ParsedTarget::form)).toList();
    }

    private static List<List<AnnotationNode>> annotationLists(ClassNode mixin) {
        return annotationLists(mixin.visibleAnnotations, mixin.invisibleAnnotations);
    }

    private static List<List<AnnotationNode>> annotationLists(
            List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        List<List<AnnotationNode>> result = new ArrayList<>(2);
        if (visible != null) result.add(visible);
        if (invisible != null) result.add(invisible);
        return result;
    }

    private static boolean hasMixinAnnotation(ClassNode mixin) {
        return hasAnnotation(mixin.visibleAnnotations) || hasAnnotation(mixin.invisibleAnnotations);
    }

    private static boolean hasAnnotation(List<AnnotationNode> annotations) {
        return annotations != null && annotations.stream().anyMatch(annotation -> MIXIN_ANNOTATION.equals(annotation.desc));
    }

    private static boolean parsedTargetsRemap(ClassNode mixin) {
        for (List<AnnotationNode> annotations : annotationLists(mixin)) {
            for (AnnotationNode annotation : annotations) {
                if (!MIXIN_ANNOTATION.equals(annotation.desc) || annotation.values == null) continue;
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    if ("remap".equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof Boolean value) {
                        return value;
                    }
                }
            }
        }
        return true;
    }

    private static TargetFinding resolveTarget(
            String archivePath, String configPath, String mixinClassName,
            ParsedTarget selector, boolean remap, boolean pluginDependent,
            Map<String, String> classMappings, String mappingVersion,
            Forge1201TargetApiVerifier.TargetIndex targetIndex) throws IOException {
        String source = selector.selector();
        String target = null;
        String evidence;
        boolean platformTarget = isPlatformTarget(source);

        if (!remap && !source.startsWith("net/neoforged/")) {
            evidence = "The @Mixin annotation disables remapping; this source selector is not assumed to be a 1.21.1 target name.";
        } else if (source.startsWith("net/minecraftforge/")) {
            Forge1201MappingCatalog.Decision decision = Forge1201MappingCatalog.classify(source);
            if (decision.targetInternalName() != null
                    && (decision.kind() == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE
                            || decision.kind() == Forge1201MappingCatalog.Kind.EXPLICIT_TYPE_REPLACEMENT)) {
                target = decision.targetInternalName();
                evidence = decision.reason();
            } else {
                evidence = decision.reason();
            }
        } else if (source.startsWith("net/neoforged/")) {
            target = source;
            evidence = "Selector already uses the NeoForge target namespace.";
        } else if (source.startsWith("net/minecraft/") || source.startsWith("com/mojang/")) {
            target = classMappings.get(source);
            evidence = target == null
                    ? "No explicit source-to-target class mapping was supplied; identity was not assumed."
                    : "Resolved by the supplied versioned source-to-target class mapping.";
        } else {
            evidence = platformTarget
                    ? "Target belongs to a platform namespace without a reviewed mapping."
                    : "Target is outside the Minecraft/Forge/NeoForge namespaces inspected by this analyzer.";
        }

        TargetStatus status;
        if (!platformTarget) {
            status = TargetStatus.NON_PLATFORM_TARGET;
        } else if (target == null) {
            status = TargetStatus.TARGET_REMAP_UNRESOLVED;
        } else if (targetIndex == null) {
            status = TargetStatus.TARGET_INDEX_UNAVAILABLE;
        } else {
            status = targetIndex.classInfo(target).isPresent()
                    ? TargetStatus.TARGET_CLASS_PRESENT : TargetStatus.TARGET_CLASS_MISSING;
        }
        return new TargetFinding(archivePath, configPath, mixinClassName, source, target,
                selector.form(), remap, pluginDependent, status, mappingVersion, evidence);
    }

    private static boolean isPlatformTarget(String internalName) {
        return internalName.startsWith("net/minecraft/")
                || internalName.startsWith("com/mojang/")
                || internalName.startsWith("net/minecraftforge/")
                || internalName.startsWith("net/neoforged/");
    }

    private static String mixinClassEntry(String packageName, String className) {
        if (className == null || className.isBlank() || className.indexOf('\0') >= 0) return null;
        String dotted = packageName + "." + className.replace('/', '.');
        if (!isValidDottedName(dotted)) return null;
        return dotted.replace('.', '/') + ".class";
    }

    private static String classResourcePath(String dottedClassName) {
        if (dottedClassName == null || !isValidDottedName(dottedClassName)) return null;
        return dottedClassName.replace('.', '/') + ".class";
    }

    private static boolean isValidDottedName(String name) {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")
                || name.contains("..") || name.indexOf('\0') >= 0) return false;
        for (String segment : name.split("\\.", -1)) {
            if (segment.isEmpty() || !Character.isJavaIdentifierStart(segment.charAt(0))) return false;
            for (int i = 1; i < segment.length(); i++) {
                if (!Character.isJavaIdentifierPart(segment.charAt(i)) && segment.charAt(i) != '$') return false;
            }
        }
        return true;
    }

    private static boolean isSafeArchivePath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.startsWith("\\")
                || path.contains("\\") || path.indexOf('\0') >= 0 || path.matches("^[A-Za-z]:.*")) return false;
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) return false;
        }
        return true;
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }

    private static Boolean booleanValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean() : null;
    }

    private static byte[] readBounded(ZipFile zip, ZipEntry entry, int limit) throws IOException {
        try (InputStream input = zip.getInputStream(entry);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer, 0, Math.min(buffer.length, limit - total + 1))) != -1) {
                total += read;
                if (total > limit) {
                    throw new EntryLimitException("Archive entry exceeded " + limit + " bytes.", total);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }

    private static final class EntryLimitException extends IOException {
        private final int bytesRead;

        private EntryLimitException(String message, int bytesRead) {
            super(message);
            this.bytesRead = bytesRead;
        }

        private int bytesRead() {
            return bytesRead;
        }
    }
}
