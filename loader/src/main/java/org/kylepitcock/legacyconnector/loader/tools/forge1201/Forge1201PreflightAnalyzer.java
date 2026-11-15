package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Deterministic, read-only preflight inventory for one Forge 1.20.1 archive.
 *
 * <p>Slice 1 inventories the outer archive only. It records nested archive paths but does not recursively
 * inspect them, validate mixin injection semantics, or prove runtime compatibility.</p>
 */
public final class Forge1201PreflightAnalyzer {
    public static final String SCHEMA_VERSION = "1.10";
    public static final String ANALYZER_VERSION = "1.10.0";
    public static final String TARGET_MINECRAFT_VERSION = "1.21.1";
    public static final String TARGET_NEOFORGE_VERSION = "21.1.258";
    private static final int MAX_MIXIN_JSON_BYTES = 1024 * 1024;
    private static final int MAX_TARGET_CLASSPATH_ENTRIES = 4096;
    private static final long MAX_TARGET_CLASSPATH_FILE_BYTES = 4L * 1024 * 1024;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private Forge1201PreflightAnalyzer() {}

    public enum MetadataStatus {
        RECOGNIZED_FORGE_1201,
        MISSING_MODS_TOML,
        NOT_RECOGNIZED_AS_FORGE_1201
    }

    public enum AnalysisStatus {
        BYTECODE_INVENTORY_RAN_FOR_RECOGNIZED_OUTER_JAR,
        BYTECODE_INVENTORY_SKIPPED_METADATA_NOT_RECOGNIZED
    }

    public record ForgeMetadata(
            String modLoader, String loaderVersion, String minecraftVersionRange, String forgeVersionRange) {}

    public record ModMetadata(String modId, String version, String displayName) {}

    public record DependencyFinding(
            String declaringModId,
            String modId,
            String versionRange,
            boolean mandatory,
            String ordering,
            String side,
            Forge1201DependencyResolver.Status status) {}

    public record ArchiveEntry(String path, boolean directory, long sizeBytes, long compressedSizeBytes) {}

    public record MixinConfigCandidate(String path, String discoveredBy) {}

    public record ApiTypeFinding(
            String sourceInternalName,
            Forge1201MappingCatalog.Kind classification,
            String candidateTargetInternalName,
            String explanation) {}

    public record ApiMemberFinding(String owner, String name, String descriptor, int count) {}

    public record ApiMethodCallSite(
            String callerClass, String callerMethod, String callerDescriptor,
            String targetOwner, String targetName, String targetDescriptor, int opcode, int count) {}

    public record ApiFieldCallSite(
            String callerClass, String callerMethod, String callerDescriptor,
            String targetOwner, String targetName, String targetDescriptor, int opcode, int count) {}

    /** Immutable report data. All lists and maps are normalized before serialization. */
    public record PreflightResult(
            String schemaVersion,
            String analyzerVersion,
            String sourceJar,
            long sourceSizeBytes,
            String sourceSha256,
            String targetMinecraftVersion,
            String targetNeoForgeVersion,
            ForgeMetadata forgeMetadata,
            MetadataStatus metadataStatus,
            AnalysisStatus analysisStatus,
            List<String> metadataWarnings,
            List<ModMetadata> mods,
            List<String> duplicateModIds,
            List<DependencyFinding> dependencies,
            int archiveEntryCount,
            List<ArchiveEntry> archiveEntries,
            List<String> resourceNamespaces,
            Map<String, String> manifestAttributes,
            List<MixinConfigCandidate> mixinConfigCandidates,
            Forge1201MixinAnalyzer.Result mixinAnalysis,
            Forge1201MappingProfileValidator.Finding mappingProfile,
            String coreModClass,
            List<String> jarInJarPaths,
            int classCount,
            int outerClassScannedCount,
            List<String> modEntrypoints,
            Map<String, Integer> packageReferences,
            List<ApiTypeFinding> apiTypes,
            List<ApiMemberFinding> methodReferences,
            List<ApiMemberFinding> fieldReferences,
            List<ApiMethodCallSite> methodCallSites,
            List<ApiFieldCallSite> fieldCallSites,
            List<Forge1201ApiUsageAnalyzer.ApiDynamicReference> dynamicReferences,
            int nestedClassCount,
            int nestedClassScannedCount,
            List<Forge1201ApiUsageAnalyzer.ApiSrgMemberReference> normalizedMemberReferences,
            List<String> classScanWarnings,
            List<String> observedNativeModIds,
            List<String> limitations,
            Forge1201ArchiveGraph.Result archiveGraph) {
        public PreflightResult {
            metadataWarnings = List.copyOf(metadataWarnings);
            mods = List.copyOf(mods);
            duplicateModIds = List.copyOf(duplicateModIds);
            dependencies = List.copyOf(dependencies);
            archiveEntries = List.copyOf(archiveEntries);
            resourceNamespaces = List.copyOf(resourceNamespaces);
            manifestAttributes = Collections.unmodifiableMap(new TreeMap<>(manifestAttributes));
            mixinConfigCandidates = List.copyOf(mixinConfigCandidates);
            jarInJarPaths = List.copyOf(jarInJarPaths);
            modEntrypoints = List.copyOf(modEntrypoints);
            packageReferences = Collections.unmodifiableMap(new TreeMap<>(packageReferences));
            apiTypes = List.copyOf(apiTypes);
            methodReferences = List.copyOf(methodReferences);
            fieldReferences = List.copyOf(fieldReferences);
            methodCallSites = List.copyOf(methodCallSites);
            fieldCallSites = List.copyOf(fieldCallSites);
            dynamicReferences = List.copyOf(dynamicReferences);
            normalizedMemberReferences = List.copyOf(normalizedMemberReferences);
            classScanWarnings = List.copyOf(classScanWarnings);
            observedNativeModIds = List.copyOf(observedNativeModIds);
            limitations = List.copyOf(limitations);
        }
    }

    /** Inspects one source archive. Installed native-mod directories and target classpath are optional. */
    public static PreflightResult analyze(Path jarPath, Collection<Path> installedModDirectories) throws IOException {
        return analyze(jarPath, installedModDirectories, List.of(), null);
    }

    /**
     * Inspects one source archive. Target classpath entries are used only as class resources; target
     * classes are never defined, initialized, or executed by the analyzer.
     */
    public static PreflightResult analyze(
            Path jarPath,
            Collection<Path> installedModDirectories,
            Collection<Path> targetClasspath) throws IOException {
        return analyze(jarPath, installedModDirectories, targetClasspath, null);
    }

    /** Null context leaves context selection unresolved and inventories all refmap tables. */
    public static PreflightResult analyze(
            Path jarPath,
            Collection<Path> installedModDirectories,
            Collection<Path> targetClasspath,
            String requestedRefmapContextKey) throws IOException {
        return analyze(jarPath, installedModDirectories, targetClasspath, requestedRefmapContextKey, null);
    }

    /**
     * Inspects one source archive and optionally validates a caller-supplied mapping profile.
     * Profile validation is read-only and never applies mappings to source selectors.
     */
    public static PreflightResult analyze(
            Path jarPath,
            Collection<Path> installedModDirectories,
            Collection<Path> targetClasspath,
            String requestedRefmapContextKey,
            Path mappingProfilePath) throws IOException {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("Preflight input must be an existing JAR file: " + jarPath);
        }

        Path jar = jarPath.toAbsolutePath().normalize();
        List<Path> modDirectories = installedModDirectories == null ? List.of()
                : installedModDirectories.stream().filter(java.util.Objects::nonNull)
                        .map(path -> path.toAbsolutePath().normalize())
                        .sorted(Comparator.comparing(Path::toString)).toList();
        List<Path> normalizedTargetClasspath = targetClasspath == null ? List.of()
                : targetClasspath.stream().filter(java.util.Objects::nonNull)
                        .map(path -> path.toAbsolutePath().normalize()).distinct().toList();
        if (normalizedTargetClasspath.size() > MAX_TARGET_CLASSPATH_ENTRIES) {
            throw new IllegalArgumentException("Target classpath exceeds " + MAX_TARGET_CLASSPATH_ENTRIES + " entries");
        }
        for (Path entry : normalizedTargetClasspath) {
            if (!Files.exists(entry) || (!Files.isDirectory(entry) && !Files.isRegularFile(entry))) {
                throw new IllegalArgumentException("Target classpath entry does not exist or is not a file/directory: " + entry);
            }
        }

        Map<String, String> manifest = readManifest(jar);
        List<String> manifestMixins = splitCommaList(valueIgnoreCase(manifest, "MixinConfigs"));
        String coreModClass = valueIgnoreCase(manifest, "FMLCorePlugin");

        List<ArchiveEntry> archiveEntries = new ArrayList<>();
        Set<String> resourceNamespaces = new TreeSet<>();
        Set<String> jarInJarPaths = new TreeSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                archiveEntries.add(new ArchiveEntry(
                        entry.getName(), entry.isDirectory(), entry.getSize(), entry.getCompressedSize()));
                collectResourceNamespace(entry.getName(), resourceNamespaces);
                if (isJarInJarPath(entry.getName())) jarInJarPaths.add(entry.getName());
            }
        }
        archiveEntries.sort(Comparator.comparing(ArchiveEntry::path));

        String modsToml = readEntry(jar, "META-INF/mods.toml");
        Forge1201JarInfo jarInfo = null;
        List<String> metadataWarnings = new ArrayList<>();
        if (modsToml == null) {
            metadataWarnings.add("META-INF/mods.toml is missing; this archive is not identified as a Forge 1.20.1 mod.");
        } else {
            metadataWarnings.addAll(inspectUnsupportedTomlConstructs(modsToml));
            try {
                jarInfo = Forge1201JarInspector.inspectForge1201(jar);
            } catch (RuntimeException exception) {
                metadataWarnings.add("Forge metadata inspection failed: " + conciseMessage(exception));
            }
            if (jarInfo == null) {
                metadataWarnings.add("The existing Forge 1.20.1 metadata inspector did not recognize this archive. "
                        + "No Forge 1.20.1 bytecode inventory was run.");
            }
        }

        MetadataStatus metadataStatus = modsToml == null ? MetadataStatus.MISSING_MODS_TOML
                : jarInfo == null ? MetadataStatus.NOT_RECOGNIZED_AS_FORGE_1201
                : MetadataStatus.RECOGNIZED_FORGE_1201;

        List<ModMetadata> mods = new ArrayList<>();
        List<String> duplicateModIds = new ArrayList<>();
        List<DependencyFinding> dependencies = new ArrayList<>();
        List<String> entrypoints = new ArrayList<>();
        Map<String, Integer> packageReferences = new TreeMap<>();
        List<ApiTypeFinding> apiTypes = new ArrayList<>();
        List<ApiMemberFinding> methodReferences = new ArrayList<>();
        List<ApiMemberFinding> fieldReferences = new ArrayList<>();
        List<ApiMethodCallSite> methodCallSites = new ArrayList<>();
        List<ApiFieldCallSite> fieldCallSites = new ArrayList<>();
        List<Forge1201ApiUsageAnalyzer.ApiDynamicReference> dynamicReferences = new ArrayList<>();
        List<Forge1201ApiUsageAnalyzer.ApiSrgMemberReference> normalizedMemberReferences = new ArrayList<>();
        List<String> classScanWarnings = new ArrayList<>();
        int classCount = 0;
        int outerClassScannedCount = 0;
        int nestedClassCount = 0;
        int nestedClassScannedCount = 0;
        AnalysisStatus analysisStatus = AnalysisStatus.BYTECODE_INVENTORY_SKIPPED_METADATA_NOT_RECOGNIZED;
        ForgeMetadata forgeMetadata = null;

        Set<String> nativeIds = Forge1201DependencyResolver.findNativeModIds(modDirectories);
        Forge1201ApiUsageAnalyzer.Inventory usageInventory = new Forge1201ApiUsageAnalyzer.Inventory();
        Forge1201ArchiveGraph.NestedClassConsumer nestedClassConsumer =
                jarInfo == null ? null : usageInventory::scanNestedClass;
        Forge1201ArchiveGraph.Result archiveGraph = Forge1201ArchiveGraph.analyze(
                jar, jarInfo, modDirectories, TARGET_MINECRAFT_VERSION, TARGET_NEOFORGE_VERSION,
                nestedClassConsumer);
        if (jarInfo != null) {
            forgeMetadata = new ForgeMetadata(
                    jarInfo.modLoader(), jarInfo.loaderVersion(),
                    jarInfo.minecraftVersionRange(), jarInfo.forgeVersionRange());

            for (Forge1201JarInfo.Mod mod : jarInfo.mods()) {
                mods.add(new ModMetadata(mod.modId(), mod.version(), mod.displayName()));
            }
            mods.sort(Comparator.comparing(ModMetadata::modId).thenComparing(ModMetadata::version));

            Map<String, Integer> idCounts = new TreeMap<>();
            jarInfo.mods().forEach(mod -> idCounts.merge(mod.modId(), 1, Integer::sum));
            idCounts.forEach((id, count) -> {
                if (count > 1) duplicateModIds.add(id);
            });

            List<Forge1201DependencyResolver.Resolution> resolutions =
                    Forge1201DependencyResolver.resolve(jarInfo, nativeIds);
            int resolutionIndex = 0;
            for (Forge1201JarInfo.Mod mod : jarInfo.mods()) {
                for (Forge1201JarInfo.Dependency dependency : mod.dependencies()) {
                    Forge1201DependencyResolver.Resolution resolution = resolutions.get(resolutionIndex++);
                    dependencies.add(new DependencyFinding(
                            mod.modId(), dependency.modId(), dependency.versionRange(), dependency.mandatory(),
                            dependency.ordering(), dependency.side(), resolution.status()));
                }
            }
            dependencies.sort(Comparator.comparing(DependencyFinding::declaringModId)
                    .thenComparing(DependencyFinding::modId)
                    .thenComparing(DependencyFinding::versionRange)
                    .thenComparing(DependencyFinding::mandatory));

            usageInventory.scanOuterJar(jar);
            Forge1201ApiUsageAnalyzer.Analysis usage = usageInventory.finish(jarInfo);
            classCount = usage.classCount();
            outerClassScannedCount = usageInventory.outerClassScannedCount();
            nestedClassCount = archiveGraph.classScanSummaries().stream()
                    .mapToInt(Forge1201ArchiveGraph.ClassScanSummary::discoveredClassEntries).sum();
            nestedClassScannedCount = usageInventory.nestedClassScannedCount();
            normalizedMemberReferences.addAll(usageInventory.normalizedSrgMemberReferences());
            classScanWarnings.addAll(usageInventory.classScanWarnings());
            entrypoints.addAll(usage.modEntrypoints());
            entrypoints.sort(String::compareTo);
            usage.packageReferences().entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> packageReferences.put(entry.getKey(), entry.getValue()));

            for (String sourceType : usage.referencedApiClasses().stream().distinct().sorted().toList()) {
                Forge1201MappingCatalog.Decision decision = Forge1201MappingCatalog.classify(sourceType);
                apiTypes.add(new ApiTypeFinding(decision.sourceInternalName(), decision.kind(),
                        decision.targetInternalName(), decision.reason()));
            }
            methodReferences.addAll(usage.methodReferences().stream()
                    .map(ref -> new ApiMemberFinding(ref.owner(), ref.name(), ref.descriptor(), ref.count()))
                    .sorted(Comparator.comparing(ApiMemberFinding::owner).thenComparing(ApiMemberFinding::name)
                            .thenComparing(ApiMemberFinding::descriptor)).toList());
            fieldReferences.addAll(usage.fieldReferences().stream()
                    .map(ref -> new ApiMemberFinding(ref.owner(), ref.name(), ref.descriptor(), ref.count()))
                    .sorted(Comparator.comparing(ApiMemberFinding::owner).thenComparing(ApiMemberFinding::name)
                            .thenComparing(ApiMemberFinding::descriptor)).toList());
            methodCallSites.addAll(usage.methodCallSites().stream()
                    .map(call -> new ApiMethodCallSite(call.callerClass(), call.callerMethod(),
                            call.callerDescriptor(), call.targetOwner(), call.targetName(),
                            call.targetDescriptor(), call.opcode(), call.count()))
                    .sorted(Comparator.comparing(ApiMethodCallSite::callerClass)
                            .thenComparing(ApiMethodCallSite::callerMethod)
                            .thenComparing(ApiMethodCallSite::callerDescriptor)
                            .thenComparing(ApiMethodCallSite::targetOwner)
                            .thenComparing(ApiMethodCallSite::targetName)
                            .thenComparing(ApiMethodCallSite::targetDescriptor)
                            .thenComparingInt(ApiMethodCallSite::opcode)).toList());
            fieldCallSites.addAll(usage.fieldCallSites().stream()
                    .map(call -> new ApiFieldCallSite(call.callerClass(), call.callerMethod(),
                            call.callerDescriptor(), call.targetOwner(), call.targetName(),
                            call.targetDescriptor(), call.opcode(), call.count()))
                    .sorted(Comparator.comparing(ApiFieldCallSite::callerClass)
                            .thenComparing(ApiFieldCallSite::callerMethod)
                            .thenComparing(ApiFieldCallSite::callerDescriptor)
                            .thenComparing(ApiFieldCallSite::targetOwner)
                            .thenComparing(ApiFieldCallSite::targetName)
                            .thenComparing(ApiFieldCallSite::targetDescriptor)
                            .thenComparingInt(ApiFieldCallSite::opcode)).toList());
            dynamicReferences.addAll(usage.dynamicReferences());
            analysisStatus = AnalysisStatus.BYTECODE_INVENTORY_RAN_FOR_RECOGNIZED_OUTER_JAR;
        }

        Map<String, Set<Forge1201MixinAnalyzer.DiscoverySource>> mixinSources = new TreeMap<>();
        manifestMixins.forEach(path -> mixinSources.computeIfAbsent(path, ignored -> new TreeSet<>())
                .add(Forge1201MixinAnalyzer.DiscoverySource.MANIFEST));
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".json")) continue;
                if (isMixinPathCandidate(entry.getName()) || looksLikeMixinConfig(zip, entry)) {
                    mixinSources.computeIfAbsent(entry.getName(), ignored -> new TreeSet<>())
                            .add(Forge1201MixinAnalyzer.DiscoverySource.RESOURCE_HEURISTIC);
                }
            }
        }
        List<MixinConfigCandidate> mixinCandidates = mixinSources.entrySet().stream()
                .map(entry -> new MixinConfigCandidate(entry.getKey(), entry.getValue().stream()
                        .map(Forge1201PreflightAnalyzer::mixinDiscoveryLabel)
                        .collect(java.util.stream.Collectors.joining("; "))))
                .toList();
        List<Forge1201MixinAnalyzer.ConfigDeclaration> mixinDeclarations = mixinSources.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(source -> new Forge1201MixinAnalyzer.ConfigDeclaration(entry.getKey(), source)))
                .toList();
        Forge1201MixinAnalyzer.Result mixinAnalysis;
        try (URLClassLoader targetLoader = createTargetClassLoader(normalizedTargetClasspath)) {
            Forge1201TargetApiVerifier.TargetIndex targetIndex = targetLoader == null ? null
                    : Forge1201TargetApiVerifier.createIndex(internalName ->
                            targetLoader.getResourceAsStream(internalName + ".class"));
            // No 1.20.1 -> 1.21.1 Minecraft class map is supplied yet; Mojang names are never assumed identical.
            mixinAnalysis = Forge1201MixinAnalyzer.analyze(
                    jar, mixinDeclarations, targetIndex, Map.of(), "not-supplied", requestedRefmapContextKey);
        }

        Forge1201MappingProfileValidator.Finding mappingProfile =
                Forge1201MappingProfileValidator.inspect(mappingProfilePath);

        List<String> observedNativeIds = nativeIds.stream().sorted().toList();
        List<String> limitations = List.of(
                "This is static preflight evidence only; it does not prove initialization, gameplay, client rendering, synchronization, or stability.",
                "Slice 3 inventories bytecode in the outer source JAR and bounded, safe source-nested Forge/library archives. Installed provider JAR bytecode and embedded native NeoForge mod bytecode are intentionally excluded.",
                "Source Minecraft SRG member names are normalized to 1.20.1 Mojang names where the generated mapping table has an entry; missing entries remain explicitly UNMAPPED. This source-name normalization is not a 1.21.1 target API or semantic compatibility verdict.",
                "Nested archive traversal is bounded by depth, archive count, per-archive size, and total inflated-byte limits. Class capture is bounded to 8 MiB per class, 16 MiB per nested archive, 128 MiB aggregate nested class bytes submitted, and 64 MiB aggregate outer class bytes. Archives/classes skipped by a limit or rejected as unsafe/corrupt remain explicit findings.",
                "Mixin configs discovered by the manifest and outer-archive resource heuristics are parsed in the current preflight schema. Target-class presence is checked only when the root project's resolved target compile classpath is supplied; no 1.20.1-to-1.21.1 Minecraft class identity mapping is assumed.",
                "Mixin analysis covers outer-archive configs, mixin class entries and internal owner names, static @Mixin targets, member annotations, six core Mixin 0.8.5 injector annotations, @Slice boundaries, bounded refmap tables, and owner-scoped exact correlation candidates for AP-emitted string references. A caller may explicitly supply a refmap context; otherwise base/context candidates are inventoried without selection. Plugins and custom wrappers remain inert and may make effective resource selection dynamic. No source/target namespace conversion, target selector/member/instruction resolution, slice ordering, handler stack validation, or runtime injection success is claimed. Source-nested JAR config/class provenance remains a future increment.",
                "Schema 1.10 mapping profiles are parsed and structurally validated only when explicitly supplied. Profile validation checks declarations, explicit context/base bindings, directed per-edge Minecraft-version continuity, complete selector symbol scope, bounded graph ambiguity, artifact SHA-256 and optional upstream checksums, and license evidence; it never loads mapping tables, applies a mapping, or rewrites selectors. A structurally validated profile is not proof of semantic mapping correctness or runtime compatibility.",
                "Reflection/resource string candidates remain heuristic and are not treated as confirmed bytecode references.",
                "Installed provider discovery records top-level and nested archive paths, mod IDs, and declared versions. A provider ID/version match does not establish API or behavioral compatibility; installed version ranges and API compatibility are not verified.",
                "Maven-style version ranges with numeric dotted versions are evaluated conservatively. Qualified or otherwise unsupported version strings remain explicitly unverified.",
                "General source API reference-to-target member verification, shim behavior checks, translated-resource comparison, and runtime feedback remain later analyzer slices. The current Mixin class-presence check is limited to statically mapped @Mixin class targets and does not validate target members or injection semantics."
        );

        return new PreflightResult(
                SCHEMA_VERSION, ANALYZER_VERSION, jar.getFileName().toString(), Files.size(jar), sha256(jar),
                TARGET_MINECRAFT_VERSION, TARGET_NEOFORGE_VERSION, forgeMetadata, metadataStatus, analysisStatus,
                metadataWarnings.stream().distinct().sorted().toList(), mods, duplicateModIds, dependencies,
                archiveEntries.size(), archiveEntries, resourceNamespaces.stream().toList(), manifest,
                mixinCandidates, mixinAnalysis, mappingProfile, coreModClass, jarInJarPaths.stream().toList(), classCount,
                outerClassScannedCount, entrypoints, packageReferences, apiTypes, methodReferences,
                fieldReferences, methodCallSites, fieldCallSites, dynamicReferences, nestedClassCount,
                nestedClassScannedCount, normalizedMemberReferences, classScanWarnings,
                observedNativeIds, limitations, archiveGraph);
    }

    private static void appendSliceBoundaryText(
            StringBuilder out, Forge1201MixinAnalyzer.SliceBoundaryFinding boundary) {
        if (boundary == null) return;
        out.append("      @Slice.").append(boundary.kind().name().toLowerCase(java.util.Locale.ROOT))
                .append(" declared=").append(boundary.declared())
                .append(" annotation=").append(boundary.annotationDescriptor())
                .append(" @At=").append(boundary.at())
                .append(" [").append(boundary.status()).append("] — ")
                .append(boundary.evidence()).append('\n');
        boundary.warnings().forEach(warning -> out.append("        warning: ").append(warning).append('\n'));
    }

    public static String toJson(PreflightResult result) {
        return GSON.toJson(result) + "\n";
    }

    public static String toText(PreflightResult result) {
        StringBuilder out = new StringBuilder();
        out.append("Legacy Connector Forge 1.20.1 Preflight\n")
                .append("Schema: ").append(result.schemaVersion())
                .append(" | Analyzer: ").append(result.analyzerVersion()).append('\n')
                .append("Source: ").append(result.sourceJar()).append('\n')
                .append("Size: ").append(result.sourceSizeBytes()).append(" bytes\n")
                .append("SHA-256: ").append(result.sourceSha256()).append('\n')
                .append("Target: Minecraft ").append(result.targetMinecraftVersion())
                .append(" / NeoForge ").append(result.targetNeoForgeVersion()).append('\n')
                .append("Metadata: ").append(result.metadataStatus()).append('\n');
        if (result.forgeMetadata() != null) {
            out.append("Forge loader: ").append(result.forgeMetadata().modLoader())
                    .append(' ').append(result.forgeMetadata().loaderVersion()).append('\n')
                    .append("Declared Minecraft range: ").append(result.forgeMetadata().minecraftVersionRange()).append('\n')
                    .append("Declared Forge range: ").append(result.forgeMetadata().forgeVersionRange()).append('\n');
        }
        out.append("Bytecode inventory: ").append(result.analysisStatus()).append('\n');
        out.append("\nMods (").append(result.mods().size()).append("):\n");
        if (result.mods().isEmpty()) out.append("  none recognized\n");
        for (ModMetadata mod : result.mods()) {
            out.append("  ").append(mod.modId()).append(' ').append(mod.version())
                    .append(" — ").append(mod.displayName()).append('\n');
        }
        if (!result.duplicateModIds().isEmpty()) {
            out.append("Duplicate mod IDs: ").append(String.join(", ", result.duplicateModIds())).append('\n');
        }
        out.append("\nDependencies (").append(result.dependencies().size()).append("):\n");
        if (result.dependencies().isEmpty()) out.append("  none parsed\n");
        for (DependencyFinding dep : result.dependencies()) {
            out.append("  ").append(dep.declaringModId()).append(" -> ").append(dep.modId()).append(' ')
                    .append(dep.versionRange()).append(dep.mandatory() ? " [required" : " [optional")
                    .append(", ").append(dep.ordering()).append(", ").append(dep.side())
                    .append("]: ").append(dep.status()).append('\n');
        }
        out.append("\nArchive: ").append(result.archiveEntryCount()).append(" entries\n")
                .append("Resource namespaces: ")
                .append(result.resourceNamespaces().isEmpty() ? "none" : String.join(", ", result.resourceNamespaces()))
                .append('\n')
                .append("Manifest attributes: ").append(result.manifestAttributes()).append('\n')
                .append("Coremod class: ").append(result.coreModClass() == null ? "none" : result.coreModClass()).append('\n')
                .append("Mixin config candidates: ").append(result.mixinConfigCandidates().size()).append('\n');
        for (MixinConfigCandidate candidate : result.mixinConfigCandidates()) {
            out.append("  ").append(candidate.path()).append(" (").append(candidate.discoveredBy()).append(")\n");
        }
        Forge1201MixinAnalyzer.Result mixins = result.mixinAnalysis();
        out.append("\nMixin analysis (").append(mixins.configs().size()).append(" configs, ")
                .append(mixins.mixinClasses().size()).append(" mixin classes, ")
                .append(mixins.targets().size()).append(" target selectors): target index=")
                .append(mixins.targetIndexStatus()).append(", class map=").append(mixins.classMappingVersion())
                .append('\n');
        for (Forge1201MixinAnalyzer.ConfigFinding finding : mixins.configs()) {
            out.append("  config ").append(finding.configPath()).append(" [").append(finding.status())
                    .append(", discovered by ").append(finding.discoverySources())
                    .append(", refmap=").append(finding.refmapSelection())
                    .append(", candidate=").append(finding.refmapCandidatePath())
                    .append(", wrapper=").append(finding.refmapWrapperClass()).append("]\n");
            finding.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        out.append("\nMixin refmap resources (").append(mixins.refmapResources().size()).append("):\n");
        if (mixins.refmapResources().isEmpty()) out.append("  none discovered\n");
        for (Forge1201MixinAnalyzer.RefmapResourceFinding resource : mixins.refmapResources()) {
            out.append("  refmap ").append(resource.resourcePath()).append(" [").append(resource.status())
                    .append(", origins=").append(resource.origins())
                    .append(", configs=").append(resource.configPaths())
                    .append(", declared-bytes=").append(resource.declaredSizeBytes())
                    .append(", compressed-bytes=").append(resource.compressedSizeBytes())
                    .append(", inflated-bytes=").append(resource.inflatedSizeBytes())
                    .append(", sha256=").append(resource.sha256())
                    .append(", contexts=").append(resource.contextKeys())
                    .append(", owners=").append(resource.ownerBucketCount())
                    .append(", entries=").append(resource.entryCount()).append("]\n");
            for (Forge1201MixinAnalyzer.RefmapTableFinding table : resource.tables()) {
                out.append("    table ").append(table.kind())
                        .append(table.contextKey() == null ? "" : " context=" + table.contextKey())
                        .append(" [owners=").append(table.ownerBucketCount())
                        .append(", entries=").append(table.entryCount()).append("]\n");
                for (Forge1201MixinAnalyzer.RefmapEntryFinding entry : table.entries()) {
                    out.append("      ").append(entry.ownerInternalName()).append(" :: ")
                            .append(entry.referenceKey()).append(" -> ")
                            .append(entry.mappedReferenceValue()).append('\n');
                }
                table.warnings().forEach(warning -> out.append("      warning: ").append(warning).append('\n'));
            }
            resource.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        out.append("\nMixin refmap lookup correlation (").append(mixins.refmapLookups().size())
                .append("), requested context=")
                .append(mixins.requestedRefmapContextKey() == null ? "<not supplied>" : mixins.requestedRefmapContextKey())
                .append(":\n");
        if (mixins.refmapLookups().isEmpty()) out.append("  none discovered\n");
        for (Forge1201MixinAnalyzer.RefmapLookupFinding lookup : mixins.refmapLookups()) {
            out.append("  ").append(lookup.source()).append(' ').append(lookup.mixinInternalName()).append('#')
                    .append(lookup.sourceMemberName()).append(lookup.sourceMemberDescriptor())
                    .append(" at ").append(lookup.sourceLocation()).append(" [").append(lookup.status())
                    .append(", remap=").append(lookup.remap()).append(", resource=").append(lookup.resourcePath())
                    .append(", context=").append(lookup.selectedContextKey()).append("] raw=")
                    .append(lookup.rawReference()).append(" normalized=").append(lookup.normalizedReference())
                    .append(" mapped=").append(lookup.mappedReferenceValue()).append(" — ")
                    .append(lookup.evidence()).append('\n');
            for (Forge1201MixinAnalyzer.RefmapLookupCandidate candidate : lookup.candidates()) {
                out.append("    candidate ").append(candidate.tableKind())
                        .append(candidate.contextKey() == null ? "" : " context=" + candidate.contextKey())
                        .append(" [").append(candidate.status()).append("] mapped=")
                        .append(candidate.mappedReferenceValue()).append(" matches=").append(candidate.matches()).append('\n');
            }
            lookup.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        for (Forge1201MixinAnalyzer.MixinClassFinding finding : mixins.mixinClasses()) {
            out.append("  mixin ").append(finding.mixinClassName()).append(" [").append(finding.status())
                    .append(", internal=").append(finding.mixinInternalName())
                    .append(", section=").append(finding.section()).append("]\n");
            finding.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        for (Forge1201MixinAnalyzer.TargetFinding finding : mixins.targets()) {
            out.append("  target ").append(finding.sourceTargetInternalName()).append(" -> ")
                    .append(finding.targetInternalName() == null ? "unresolved" : finding.targetInternalName())
                    .append(" [").append(finding.status()).append(", remap=").append(finding.remap())
                    .append(", plugin-dependent=").append(finding.pluginDependent()).append("] ")
                    .append(finding.evidence()).append('\n');
        }
        out.append("\nMixin member annotations (").append(mixins.memberAnnotations().size()).append("):\n");
        if (mixins.memberAnnotations().isEmpty()) out.append("  none discovered\n");
        for (Forge1201MixinAnalyzer.MemberAnnotationFinding finding : mixins.memberAnnotations()) {
            out.append("  ").append(finding.annotationKind()).append(' ')
                    .append(finding.mixinClassName()).append('#')
                    .append(finding.sourceMemberName()).append(finding.sourceMemberDescriptor())
                    .append(" [").append(finding.elementKind()).append(", ")
                    .append(finding.status()).append(", remap=").append(finding.remap())
                    .append(", operation=").append(finding.operation())
                    .append(", prefix=").append(finding.prefix())
                    .append(", aliases=").append(finding.aliases())
                    .append(", value=").append(finding.explicitValue())
                    .append(", constraints=").append(finding.constraints()).append("] candidates=")
                    .append(finding.targetMemberNameCandidates())
                    .append(" source-targets=").append(finding.sourceTargetInternalNames())
                    .append(" — ").append(finding.evidence()).append('\n');
            finding.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        out.append("\nMixin injectors (").append(mixins.injectors().size()).append("):\n");
        if (mixins.injectors().isEmpty()) out.append("  none discovered\n");
        for (Forge1201MixinAnalyzer.InjectorFinding finding : mixins.injectors()) {
            out.append("  ").append(finding.annotationKind()).append(' ')
                    .append(finding.mixinClassName()).append('#')
                    .append(finding.sourceMemberName()).append(finding.sourceMemberDescriptor())
                    .append(" [").append(finding.elementKind()).append(", ").append(finding.status())
                    .append(", remap=").append(finding.remap())
                    .append(", @At=").append(finding.atAnnotationCount())
                    .append(", @Slice=").append(finding.sliceAnnotationCount())
                    .append(", require=").append(finding.require())
                    .append(", expect=").append(finding.expect())
                    .append(", allow=").append(finding.allow())
                    .append("] selectors=").append(finding.targetSelectors())
                    .append(" source-targets=").append(finding.sourceTargetInternalNames())
                    .append(" — ").append(finding.evidence()).append('\n');
            for (Forge1201MixinAnalyzer.AtFinding at : finding.injectionPoints()) {
                out.append("    @At[").append(at.index()).append("] value=").append(at.value())
                        .append(" kind=").append(at.valueKind()).append(" target=").append(at.rawTarget())
                        .append(" parsed-target=").append(at.targetSelector())
                        .append(" desc=").append(at.descSelector())
                        .append(" opcode=").append(at.opcode()).append(" ordinal=").append(at.ordinal())
                        .append(" shift=").append(at.shift()).append(" by=").append(at.shiftBy())
                        .append(" slice=").append(at.sliceId()).append(" args=").append(at.args())
                        .append(" remap=").append(at.remap()).append(" id=").append(at.id())
                        .append(" [").append(at.status()).append("] — ").append(at.evidence()).append('\n');
                at.warnings().forEach(warning -> out.append("      warning: ").append(warning).append('\n'));
            }
            for (Forge1201MixinAnalyzer.SliceFinding slice : finding.slices()) {
                out.append("    @Slice[").append(slice.index()).append("] id=").append(slice.id())
                        .append(" annotation=").append(slice.annotationDescriptor())
                        .append(" [").append(slice.status()).append("] — ").append(slice.evidence()).append('\n');
                appendSliceBoundaryText(out, slice.from());
                appendSliceBoundaryText(out, slice.to());
                slice.warnings().forEach(warning -> out.append("      warning: ").append(warning).append('\n'));
            }
            for (Forge1201MixinAnalyzer.ConstantFinding constant : finding.constants()) {
                out.append("    @Constant[").append(constant.index()).append("] declared=")
                        .append(constant.declared()).append(" null=").append(constant.nullValue())
                        .append(" int=").append(constant.intValue()).append(" float=").append(constant.floatValue())
                        .append(" long=").append(constant.longValue()).append(" double=").append(constant.doubleValue())
                        .append(" string=").append(constant.stringValue()).append(" class=")
                        .append(constant.classValueDescriptor()).append(" ordinal=").append(constant.ordinal())
                        .append(" slice=").append(constant.sliceId()).append(" expandZeroConditions=")
                        .append(constant.expandZeroConditions()).append(" log=").append(constant.log())
                        .append(" [").append(constant.status()).append("] — ").append(constant.evidence()).append('\n');
                constant.warnings().forEach(warning -> out.append("      warning: ").append(warning).append('\n'));
            }
            finding.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        mixins.warnings().forEach(warning -> out.append("  warning: ").append(warning).append('\n'));
        out.append("Jar-in-Jar archive paths: ")
                .append(result.jarInJarPaths().isEmpty() ? "none" : String.join(", ", result.jarInJarPaths())).append('\n');
        Forge1201ArchiveGraph.Result graph = result.archiveGraph();
        out.append("\nNested / embedded archives (").append(graph.archives().size()).append("):\n");
        if (graph.archives().isEmpty()) out.append("  none discovered\n");
        for (Forge1201ArchiveGraph.ArchiveFinding archive : graph.archives()) {
            out.append("  ").append(archive.archivePath()).append(" [").append(archive.kind()).append("]")
                    .append(" depth=").append(archive.depth())
                    .append(" size=").append(archive.sizeBytes());
            if (archive.sha256() != null) out.append(" sha256=").append(archive.sha256());
            if (!archive.mods().isEmpty()) {
                out.append(" mods=").append(archive.mods().stream()
                        .map(mod -> mod.modId() + "@" + mod.version()).toList());
            }
            out.append('\n');
            archive.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        out.append("\nInstalled / embedded providers (").append(graph.providers().size()).append("):\n");
        if (graph.providers().isEmpty()) out.append("  none discovered\n");
        for (Forge1201ArchiveGraph.ProviderFinding provider : graph.providers()) {
            out.append("  ").append(provider.modId()).append(" ").append(provider.version())
                    .append(" [").append(provider.providerKind()).append(", ").append(provider.loaderKind())
                    .append("] at ").append(provider.archivePath()).append('\n');
        }
        out.append("\nDependency graph edges (").append(graph.dependencyEdges().size()).append("):\n");
        if (graph.dependencyEdges().isEmpty()) out.append("  none parsed\n");
        for (Forge1201ArchiveGraph.DependencyEdge edge : graph.dependencyEdges()) {
            out.append("  ").append(edge.declaringArchivePath()).append(" :: ")
                    .append(edge.declaringModId()).append(" -> ").append(edge.dependencyModId())
                    .append(" ").append(edge.versionRange())
                    .append(" [").append(edge.dependencyType().name().toLowerCase(Locale.ROOT))
                    .append(", ").append(edge.ordering()).append(", ").append(edge.side())
                    .append("] ").append(edge.status())
                    .append(" provider-range=").append(edge.providerRangeStatus());
            if (!edge.providerArchivePaths().isEmpty()) {
                out.append(" providers=").append(edge.providerArchivePaths());
            }
            if (!edge.providerVersions().isEmpty()) {
                out.append(" versions=").append(edge.providerVersions());
            }
            out.append('\n');
        }
        out.append("\nTarget Minecraft range checks (").append(graph.targetRangeFindings().size()).append("):\n");
        if (graph.targetRangeFindings().isEmpty()) out.append("  no Minecraft dependency ranges parsed\n");
        for (Forge1201ArchiveGraph.TargetRangeFinding finding : graph.targetRangeFindings()) {
            out.append("  ").append(finding.declaringArchivePath()).append(" :: ")
                    .append(finding.declaringModId()).append(" declares ")
                    .append(finding.declaredRange()).append(" for ").append(finding.dependencyModId())
                    .append("; target ").append(finding.targetVersion()).append(": ")
                    .append(finding.status()).append(" — ").append(finding.explanation()).append('\n');
        }
        if (!graph.warnings().isEmpty()) {
            out.append("\nArchive graph warnings:\n");
            graph.warnings().forEach(warning -> out.append("  - ").append(warning).append('\n'));
        }
        int totalDiscoveredClasses = result.classCount() + result.nestedClassCount();
        int totalScannedClasses = result.outerClassScannedCount() + result.nestedClassScannedCount();
        out.append("\nBytecode: ").append(result.classCount()).append(" outer class entries / ")
                .append(result.outerClassScannedCount()).append(" scanned; ")
                .append(result.nestedClassCount()).append(" nested class entries / ")
                .append(result.nestedClassScannedCount()).append(" scanned; ")
                .append(totalDiscoveredClasses).append(" total discovered / ")
                .append(totalScannedClasses).append(" total scanned\n")
                .append("Mod entrypoints: ")
                .append(result.modEntrypoints().isEmpty() ? "none detected" : String.join(", ", result.modEntrypoints()))
                .append('\n')
                .append("Referenced API packages: ").append(result.packageReferences()).append('\n')
                .append("Referenced API types: ").append(result.apiTypes().size()).append('\n');
        for (ApiTypeFinding type : result.apiTypes()) {
            out.append("  ").append(type.sourceInternalName()).append(" -> ").append(type.classification());
            if (type.candidateTargetInternalName() != null) {
                out.append(" (candidate ").append(type.candidateTargetInternalName()).append(')');
            }
            out.append(": ").append(type.explanation()).append('\n');
        }
        out.append("Method references: ").append(result.methodReferences().size())
                .append(" | field references: ").append(result.fieldReferences().size()).append('\n')
                .append("Method call sites: ").append(result.methodCallSites().size())
                .append(" | field call sites: ").append(result.fieldCallSites().size()).append('\n')
                .append("Dynamic / bootstrap references: ").append(result.dynamicReferences().size()).append('\n');
        for (Forge1201ApiUsageAnalyzer.ApiDynamicReference reference : result.dynamicReferences()) {
            out.append("  ").append(reference.display()).append(" (x")
                    .append(reference.count()).append(") tag=").append(reference.handleTag()).append('\n');
        }
        out.append("\nNested class archive scans: ").append(graph.classScanSummaries().size()).append('\n');
        for (Forge1201ArchiveGraph.ClassScanSummary summary : graph.classScanSummaries()) {
            out.append("  ").append(summary.archivePath()).append(" [").append(summary.archiveKind()).append("] ")
                    .append(summary.scannedClassEntries()).append('/').append(summary.discoveredClassEntries())
                    .append(" scanned, ").append(summary.skippedClassEntries()).append(" skipped, ")
                    .append(summary.submittedClassBytes()).append(" submitted bytes\n");
            summary.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        out.append("\nSource SRG -> Minecraft 1.20.1 Mojang member names: ")
                .append(result.normalizedMemberReferences().size()).append('\n');
        for (Forge1201ApiUsageAnalyzer.ApiSrgMemberReference reference : result.normalizedMemberReferences()) {
            out.append("  ").append(reference.kind()).append(' ').append(reference.owner()).append('#')
                    .append(reference.sourceSrgName()).append(" -> ")
                    .append(reference.mojangName() == null ? "UNMAPPED" : reference.mojangName())
                    .append(reference.descriptor()).append(" [").append(reference.mappingStatus())
                    .append(", count=").append(reference.count()).append("] locations=")
                    .append(reference.sourceLocations()).append('\n');
        }
        if (!result.classScanWarnings().isEmpty()) {
            out.append("\nOuter / bytecode scanner warnings:\n");
            result.classScanWarnings().forEach(warning -> out.append("  - ").append(warning).append('\n'));
        }
        if (!result.metadataWarnings().isEmpty()) {
            out.append("\nMetadata warnings:\n");
            result.metadataWarnings().forEach(warning -> out.append("  - ").append(warning).append('\n'));
        }
        Forge1201MappingProfileValidator.Finding profile = result.mappingProfile();
        out.append("\nMapping profile: ").append(profile.status());
        if (profile.profileId() != null) out.append(" id=").append(profile.profileId());
        if (profile.profilePath() != null) out.append(" file=").append(profile.profilePath());
        out.append('\n');
        if (profile.profileSha256() != null) {
            out.append("  Profile SHA-256: ").append(profile.profileSha256())
                    .append(" (").append(profile.profileSizeBytes()).append(" bytes)\n");
        }
        if (profile.status() == Forge1201MappingProfileValidator.ProfileStatus.PROFILE_NOT_SUPPLIED) {
            out.append("  No mapping profile supplied; no namespace mapping is selected or inferred.\n");
        } else {
            if (profile.sourceCompileNamespaceId() != null) {
                out.append("  Source namespace: ").append(profile.sourceCompileNamespaceId())
                        .append(" (Minecraft ").append(profile.sourceMinecraftVersion()).append(")\n");
            }
            if (profile.targetCompileNamespaceId() != null) {
                out.append("  Target namespace: ").append(profile.targetCompileNamespaceId())
                        .append(" (Minecraft ").append(profile.targetMinecraftVersion()).append(")\n");
            }
            out.append("  Context bindings: ").append(profile.contextBindings().size())
                    .append(" | artifacts: ").append(profile.artifacts().size())
                    .append(" | edges: ").append(profile.edges().size()).append('\n');
        }
        for (Forge1201MappingProfileValidator.ArtifactFinding artifact : profile.artifacts()) {
            out.append("  Artifact ").append(artifact.artifactId()).append(" [").append(artifact.status())
                    .append("] provider=").append(artifact.provider()).append('@').append(artifact.version())
                    .append(" format=").append(artifact.format())
                    .append(" sha256=").append(artifact.expectedSha256())
                    .append(" actual=").append(artifact.actualSha256())
                    .append(" upstream=").append(artifact.upstreamChecksum())
                    .append(" upstreamActual=").append(artifact.actualUpstreamChecksum())
                    .append(" license=").append(artifact.licenseId()).append('/')
                    .append(artifact.licenseReviewStatus()).append('\n');
            artifact.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        for (Forge1201MappingProfileValidator.EdgeFinding edge : profile.edges()) {
            out.append("  Edge ").append(edge.edgeId()).append(" [").append(edge.status()).append("] ")
                    .append(edge.fromNamespace()).append('@').append(edge.sourceMinecraftVersion())
                    .append(" -> ").append(edge.toNamespace()).append('@').append(edge.targetMinecraftVersion())
                    .append(" direction=").append(edge.direction())
                    .append(" scope=").append(edge.scope())
                    .append(" artifacts=").append(edge.artifactIds()).append('\n');
            edge.warnings().forEach(warning -> out.append("    warning: ").append(warning).append('\n'));
        }
        for (Forge1201MappingProfileValidator.ContextPathFinding path : profile.contextPaths()) {
            out.append("  Context ").append(path.contextKey()).append(": ")
                    .append(path.sourceNamespace()).append('@').append(path.sourceMinecraftVersion())
                    .append(" -> ").append(path.targetNamespace()).append('@').append(path.targetMinecraftVersion())
                    .append(" [").append(path.status()).append(", paths=").append(path.completePathCount())
                    .append("]\n");
        }
        profile.validationErrors().forEach(value -> out.append("  error: ").append(value).append('\n'));
        profile.warnings().forEach(value -> out.append("  warning: ").append(value).append('\n'));
        out.append("  Note: profile validation does not transform selectors or prove mapping semantics.\n");
        out.append("\nLimitations:\n");
        result.limitations().forEach(value -> out.append("  - ").append(value).append('\n'));
        return out.append("\nConclusion: this report is not a compatibility approval. Runtime validation is still required.\n")
                .toString();
    }

    private static Map<String, String> readManifest(Path jar) throws IOException {
        Map<String, String> result = new TreeMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(JarFile.MANIFEST_NAME);
            if (entry == null) return result;
            try (InputStream in = zip.getInputStream(entry)) {
                Manifest manifest = new Manifest(in);
                for (Map.Entry<Object, Object> item : manifest.getMainAttributes().entrySet()) {
                    result.put(item.getKey().toString(), item.getValue().toString());
                }
            }
        }
        return result;
    }

    private static String readEntry(Path jar, String name) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(name);
            if (entry == null) return null;
            try (InputStream in = zip.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static String valueIgnoreCase(Map<String, String> values, String key) {
        return values.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private static List<String> splitCommaList(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(",")).map(String::trim)
                .filter(part -> !part.isEmpty()).distinct().sorted().toList();
    }

    private static void collectResourceNamespace(String name, Set<String> namespaces) {
        String[] parts = name.split("/");
        if (parts.length >= 3 && (parts[0].equals("assets") || parts[0].equals("data"))) namespaces.add(parts[1]);
    }

    private static boolean isJarInJarPath(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.equals("meta-inf/jarjar/metadata.json")
                || ((lower.startsWith("meta-inf/jarjar/") || lower.startsWith("meta-inf/jars/"))
                        && lower.endsWith(".jar"));
    }

    private static boolean isMixinPathCandidate(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        String name = lower.substring(lower.lastIndexOf('/') + 1);
        return lower.contains("mixin") || name.startsWith("mixins")
                || name.endsWith(".mixins.json") || name.endsWith(".mixin.json");
    }

    private static boolean looksLikeMixinConfig(ZipFile zip, ZipEntry entry) {
        if (entry.getSize() > MAX_MIXIN_JSON_BYTES) return false;
        try (InputStream in = zip.getInputStream(entry)) {
            String json = new String(in.readNBytes(MAX_MIXIN_JSON_BYTES), StandardCharsets.UTF_8);
            return hasJsonKey(json, "mixins")
                    && (hasJsonKey(json, "package") || hasJsonKey(json, "plugin")
                            || hasJsonKey(json, "client") || hasJsonKey(json, "server"));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean hasJsonKey(String json, String key) {
        String quote = String.valueOf('"');
        return json.contains(quote + key + quote);
    }

    private static List<String> inspectUnsupportedTomlConstructs(String toml) {
        List<String> warnings = new ArrayList<>();
        String[] lines = toml.split("\\R", -1);
        String tripleDoubleQuote = String.valueOf('"').repeat(3);
        for (int i = 0; i < lines.length; i++) {
            String line = stripTomlComment(lines[i]).trim();
            int equals = findUnquotedEquals(line);
            if (equals < 0) continue;
            String value = line.substring(equals + 1).trim();
            if (value.startsWith("[") || value.startsWith("{")) {
                warnings.add("mods.toml line " + (i + 1)
                        + " uses an array or inline table; the scalar metadata inspector may not interpret it.");
            }
            if (value.startsWith(tripleDoubleQuote) || value.startsWith("'''")) {
                warnings.add("mods.toml line " + (i + 1)
                        + " starts a multiline string; the scalar metadata inspector does not parse multiline values.");
            }
        }
        return warnings;
    }

    private static String stripTomlComment(String line) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if ((ch == '"' || ch == '\'') && (quote == 0 || quote == ch)) quote = quote == 0 ? ch : 0;
            else if (ch == '#' && quote == 0) return line.substring(0, i);
        }
        return line;
    }

    private static int findUnquotedEquals(String line) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if ((ch == '"' || ch == '\'') && (quote == 0 || quote == ch)) quote = quote == 0 ? ch : 0;
            else if (ch == '=' && quote == 0) return i;
        }
        return -1;
    }

    private static URLClassLoader createTargetClassLoader(List<Path> classpathEntries) throws IOException {
        if (classpathEntries.isEmpty()) return null;
        URL[] urls = new URL[classpathEntries.size()];
        for (int i = 0; i < classpathEntries.size(); i++) {
            urls[i] = classpathEntries.get(i).toUri().toURL();
        }
        // Resource lookup only: callers use getResourceAsStream and never invoke loadClass.
        return new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
    }

    private static List<Path> readTargetClasspathFile(Path classpathFile) throws IOException {
        if (classpathFile == null) return List.of();
        if (!Files.isRegularFile(classpathFile)) {
            throw new IllegalArgumentException("Target classpath list is not a file: " + classpathFile);
        }
        if (Files.size(classpathFile) > MAX_TARGET_CLASSPATH_FILE_BYTES) {
            throw new IllegalArgumentException("Target classpath list exceeds "
                    + MAX_TARGET_CLASSPATH_FILE_BYTES + " bytes");
        }
        Set<Path> entries = new java.util.LinkedHashSet<>();
        for (String line : Files.readAllLines(classpathFile, StandardCharsets.UTF_8)) {
            String value = line.trim();
            if (value.isEmpty()) continue;
            Path entry = Path.of(value).toAbsolutePath().normalize();
            if (!Files.exists(entry) || (!Files.isDirectory(entry) && !Files.isRegularFile(entry))) {
                throw new IllegalArgumentException("Target classpath entry does not exist or is not a file/directory: " + entry);
            }
            entries.add(entry);
            if (entries.size() > MAX_TARGET_CLASSPATH_ENTRIES) {
                throw new IllegalArgumentException("Target classpath exceeds "
                        + MAX_TARGET_CLASSPATH_ENTRIES + " entries");
            }
        }
        return List.copyOf(entries);
    }

    private static String mixinDiscoveryLabel(Forge1201MixinAnalyzer.DiscoverySource source) {
        return switch (source) {
            case MANIFEST -> "manifest MixinConfigs attribute";
            case FORGE_METADATA -> "Forge metadata";
            case RESOURCE_HEURISTIC -> "resource path/content heuristic";
        };
    }

    private static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) >= 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("The JVM does not provide SHA-256", impossible);
        }
    }

    private static void write(Path path, String content) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        if (absolute.getParent() != null) Files.createDirectories(absolute.getParent());
        Files.writeString(absolute, content, StandardCharsets.UTF_8);
    }

    /** CLI: <jar> [--mods-dir <directory>]... [--target-classpath-file <path>] [--refmap-context <key>] [--mapping-profile <path>] [--json <path>] [--text <path>] */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: <jar> [--mods-dir <directory>]... "
                    + "[--target-classpath-file <path>] [--refmap-context <key>] "
                    + "[--mapping-profile <path>] [--json <path>] [--text <path>]");
        }
        Path jar = Path.of(args[0]);
        List<Path> modDirectories = new ArrayList<>();
        Path targetClasspathFile = null;
        Path json = null;
        Path text = null;
        String requestedRefmapContextKey = null;
        Path mappingProfile = null;
        for (int i = 1; i < args.length; i++) {
            String option = args[i];
            if (++i >= args.length) throw new IllegalArgumentException("Missing value after " + option);
            if ("--refmap-context".equals(option)) {
                requestedRefmapContextKey = args[i];
                continue;
            }
            if ("--mapping-profile".equals(option)) {
                mappingProfile = Path.of(args[i]);
                continue;
            }
            Path value = Path.of(args[i]);
            switch (option) {
                case "--mods-dir" -> modDirectories.add(value);
                case "--target-classpath-file" -> targetClasspathFile = value;
                case "--json" -> json = value;
                case "--text" -> text = value;
                default -> throw new IllegalArgumentException("Unknown preflight option: " + option);
            }
        }
        PreflightResult result = analyze(
                jar, modDirectories, readTargetClasspathFile(targetClasspathFile),
                requestedRefmapContextKey, mappingProfile);
        if (json != null) write(json, toJson(result));
        if (text != null) write(text, toText(result));
        if (json == null && text == null) {
            System.out.print(toText(result));
            System.out.print(toJson(result));
        }
    }
}
