package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
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
    public static final String SCHEMA_VERSION = "1.1";
    public static final String ANALYZER_VERSION = "1.1.0";
    private static final int MAX_MIXIN_JSON_BYTES = 1024 * 1024;
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
            String coreModClass,
            List<String> jarInJarPaths,
            int classCount,
            List<String> modEntrypoints,
            Map<String, Integer> packageReferences,
            List<ApiTypeFinding> apiTypes,
            List<ApiMemberFinding> methodReferences,
            List<ApiMemberFinding> fieldReferences,
            List<ApiMethodCallSite> methodCallSites,
            List<ApiFieldCallSite> fieldCallSites,
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
            observedNativeModIds = List.copyOf(observedNativeModIds);
            limitations = List.copyOf(limitations);
        }
    }

    /** Inspects one source archive. Installed native-mod directories are optional. */
    public static PreflightResult analyze(Path jarPath, Collection<Path> installedModDirectories) throws IOException {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("Preflight input must be an existing JAR file: " + jarPath);
        }

        Path jar = jarPath.toAbsolutePath().normalize();
        List<Path> modDirectories = installedModDirectories == null ? List.of()
                : installedModDirectories.stream().filter(java.util.Objects::nonNull)
                        .map(path -> path.toAbsolutePath().normalize())
                        .sorted(Comparator.comparing(Path::toString)).toList();

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
        int classCount = 0;
        AnalysisStatus analysisStatus = AnalysisStatus.BYTECODE_INVENTORY_SKIPPED_METADATA_NOT_RECOGNIZED;
        ForgeMetadata forgeMetadata = null;

        Set<String> nativeIds = Forge1201DependencyResolver.findNativeModIds(modDirectories);
        Forge1201ArchiveGraph.Result archiveGraph = Forge1201ArchiveGraph.analyze(
                jar, jarInfo, modDirectories, "1.21.1");
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

            Forge1201ApiUsageAnalyzer.Analysis usage = Forge1201ApiUsageAnalyzer.analyze(jar);
            classCount = usage.classCount();
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
            analysisStatus = AnalysisStatus.BYTECODE_INVENTORY_RAN_FOR_RECOGNIZED_OUTER_JAR;
        }

        Map<String, String> mixinSources = new TreeMap<>();
        manifestMixins.forEach(path -> mixinSources.put(path, "manifest MixinConfigs attribute"));
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".json")) continue;
                if (isMixinPathCandidate(entry.getName()) || looksLikeMixinConfig(zip, entry)) {
                    mixinSources.putIfAbsent(entry.getName(), "resource path/content heuristic");
                }
            }
        }
        List<MixinConfigCandidate> mixinCandidates = mixinSources.entrySet().stream()
                .map(entry -> new MixinConfigCandidate(entry.getKey(), entry.getValue())).toList();

        List<String> observedNativeIds = nativeIds.stream().sorted().toList();
        List<String> limitations = List.of(
                "This is static preflight evidence only; it does not prove initialization, gameplay, client rendering, synchronization, or stability.",
                "Slice 2 recursively inventories bounded nested archives and metadata, but bytecode references are still scanned in the outer JAR only; nested bytecode analysis is deferred to Slice 3.",
                "Nested archive traversal is bounded by depth, archive count, per-archive size, and total inflated-byte limits. Archives skipped by a limit or rejected as unsafe/corrupt remain explicit findings.",
                "Mixin discovery uses manifest declarations and resource path/content heuristics. Injection targets, handler descriptors, slices, refmaps, and dynamic plugins are not validated in Slice 2.",
                "The current bytecode inventory is not exhaustive for every reflection, method-handle, invokedynamic, or constant-dynamic reference.",
                "Installed provider discovery records top-level and nested archive paths, mod IDs, and declared versions. A provider ID/version match does not establish API or behavioral compatibility; installed version ranges and API compatibility are not verified.",
                "Maven-style version ranges with numeric dotted versions are evaluated conservatively. Qualified or otherwise unsupported version strings remain explicitly unverified.",
                "Target class/member verification, shim behavior checks, translated-resource comparison, and runtime feedback are later analyzer slices."
        );

        return new PreflightResult(
                SCHEMA_VERSION, ANALYZER_VERSION, jar.getFileName().toString(), Files.size(jar), sha256(jar),
                "1.21.1", "21.1.258", forgeMetadata, metadataStatus, analysisStatus,
                metadataWarnings.stream().distinct().sorted().toList(), mods, duplicateModIds, dependencies,
                archiveEntries.size(), archiveEntries, resourceNamespaces.stream().toList(), manifest,
                mixinCandidates, coreModClass, jarInJarPaths.stream().toList(), classCount, entrypoints,
                packageReferences, apiTypes, methodReferences, fieldReferences, methodCallSites, fieldCallSites,
                observedNativeIds, limitations, archiveGraph);
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
                    .append(edge.mandatory() ? " [required" : " [optional")
                    .append(", ").append(edge.ordering()).append(", ").append(edge.side())
                    .append("] ").append(edge.status())
                    .append(" provider-range=").append(edge.providerRangeStatus());
            if (!edge.providerArchivePaths().isEmpty()) {
                out.append(" providers=").append(edge.providerArchivePaths())
                        .append(" versions=").append(edge.providerVersions());
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
        out.append("\nBytecode: ").append(result.classCount()).append(" classes scanned\n")
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
                .append(" | field call sites: ").append(result.fieldCallSites().size()).append('\n');
        if (!result.metadataWarnings().isEmpty()) {
            out.append("\nMetadata warnings:\n");
            result.metadataWarnings().forEach(warning -> out.append("  - ").append(warning).append('\n'));
        }
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

    /** CLI: <jar> [--mods-dir <directory>]... [--json <path>] [--text <path>] */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: <jar> [--mods-dir <directory>]... [--json <path>] [--text <path>]");
        }
        Path jar = Path.of(args[0]);
        List<Path> modDirectories = new ArrayList<>();
        Path json = null;
        Path text = null;
        for (int i = 1; i < args.length; i++) {
            String option = args[i];
            if (++i >= args.length) throw new IllegalArgumentException("Missing value after " + option);
            Path value = Path.of(args[i]);
            switch (option) {
                case "--mods-dir" -> modDirectories.add(value);
                case "--json" -> json = value;
                case "--text" -> text = value;
                default -> throw new IllegalArgumentException("Unknown preflight option: " + option);
            }
        }
        PreflightResult result = analyze(jar, modDirectories);
        if (json != null) write(json, toJson(result));
        if (text != null) write(text, toText(result));
        if (json == null && text == null) {
            System.out.print(toText(result));
            System.out.print(toJson(result));
        }
    }
}
