package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Bounded, read-only Jar-in-Jar and installed-provider inventory for the Forge 1.20.1 preflight report.
 *
 * <p>Nested archives are inspected in memory. No archive entries are extracted to disk. Byte, depth,
 * count, class-payload, and inflated-size limits prevent unbounded recursive work. Source-nested
 * class entries can be streamed to the shared ASM inventory without extraction or a second ZIP walk.</p>
 */
public final class Forge1201ArchiveGraph {
    public static final int MAX_NESTED_DEPTH = 4;
    public static final int MAX_NESTED_ARCHIVES = 128;
    public static final int MAX_ROOT_ARCHIVE_ENTRIES = 100_000;
    public static final int MAX_NESTED_ARCHIVE_ENTRIES = 100_000;
    public static final long MAX_NESTED_ARCHIVE_BYTES = 32L * 1024 * 1024;
    public static final long MAX_ARCHIVE_INFLATED_BYTES = 64L * 1024 * 1024;
    public static final long MAX_TOTAL_INFLATED_BYTES = 256L * 1024 * 1024;
    public static final long MAX_CLASS_ENTRY_BYTES = 8L * 1024 * 1024;
    public static final long MAX_NESTED_CLASS_BYTES_PER_ARCHIVE = 16L * 1024 * 1024;
    public static final long MAX_TOTAL_NESTED_CLASS_BYTES = 128L * 1024 * 1024;
    private static final int MAX_METADATA_BYTES = 1024 * 1024;
    private static final String JARJAR_METADATA = "META-INF/jarjar/metadata.json";

    private Forge1201ArchiveGraph() {}

    public enum ArchiveOrigin {
        SOURCE_ROOT,
        INPUT_NESTED,
        INSTALLED_TOP_LEVEL,
        INSTALLED_NESTED
    }

    public enum ArchiveKind {
        FORGE_1201_MOD,
        NEOFORGE_MOD,
        LIBRARY_OR_UNRECOGNIZED,
        CORRUPT_ARCHIVE,
        UNSAFE_PATH,
        MISSING_DECLARED_ARCHIVE,
        SIZE_LIMIT,
        DEPTH_LIMIT,
        ARCHIVE_COUNT_LIMIT,
        ENTRY_COUNT_LIMIT,
        DUPLICATE_CONTENT_SKIPPED
    }

    public enum ProviderKind {
        SOURCE_ROOT,
        SOURCE_NESTED,
        INSTALLED_TOP_LEVEL_NEOFORGE,
        INSTALLED_TOP_LEVEL_FORGE_1201,
        INSTALLED_NESTED_NEOFORGE,
        INSTALLED_NESTED_FORGE_1201
    }

    public enum DependencyStatus {
        HOST_BRIDGE,
        HOST_VERSION_MISMATCH,
        PROVIDER_PRESENT,
        PROVIDER_VERSION_MISMATCH,
        PROVIDER_AMBIGUOUS,
        REQUIRED_MISSING,
        OPTIONAL_MISSING,
        INCOMPATIBLE_PROVIDER_PRESENT,
        INCOMPATIBLE_PROVIDER_ABSENT,
        DISCOURAGED_PROVIDER_PRESENT,
        DISCOURAGED_PROVIDER_ABSENT,
        UNSUPPORTED_DEPENDENCY_TYPE
    }

    public enum VersionRangeStatus {
        MATCH,
        OUTSIDE_RANGE,
        UNPARSEABLE,
        NOT_EVALUATED
    }

    public record JarJarCoordinate(
            String path, String group, String artifact, String versionRange, String artifactVersion) {}

    public record ArchiveFinding(
            String archivePath,
            String parentArchivePath,
            int depth,
            ArchiveOrigin origin,
            long sizeBytes,
            String sha256,
            ArchiveKind kind,
            String jarJarGroup,
            String jarJarArtifact,
            String declaredVersionRange,
            String declaredArtifactVersion,
            List<Forge1201PreflightAnalyzer.ModMetadata> mods,
            List<String> warnings) {
        public ArchiveFinding {
            mods = List.copyOf(mods);
            warnings = List.copyOf(warnings);
        }
    }

    public record ProviderFinding(
            String modId,
            String version,
            String archivePath,
            ProviderKind providerKind,
            String loaderKind,
            String sha256,
            String jarJarGroup,
            String jarJarArtifact,
            String declaredVersionRange,
            String declaredArtifactVersion) {}

    public record DependencyEdge(
            String declaringArchivePath,
            String declaringModId,
            String dependencyModId,
            String versionRange,
            boolean mandatory,
            Forge1201JarInfo.DependencyType dependencyType,
            String ordering,
            String side,
            DependencyStatus status,
            VersionRangeStatus providerRangeStatus,
            List<String> providerArchivePaths,
            List<String> providerVersions) {
        public DependencyEdge {
            providerArchivePaths = List.copyOf(providerArchivePaths);
            providerVersions = List.copyOf(providerVersions);
        }
    }

    public record TargetRangeFinding(
            String declaringArchivePath,
            String declaringModId,
            String dependencyModId,
            String declaredRange,
            String targetVersion,
            VersionRangeStatus status,
            String explanation) {}

    @FunctionalInterface
    public interface NestedClassConsumer {
        /** Returns true when the supplied class bytes were successfully parsed by the bytecode inventory. */
        boolean scan(String archivePath, String classEntryPath, byte[] classBytes);
    }

    public record ClassScanSummary(
            String archivePath,
            ArchiveKind archiveKind,
            int discoveredClassEntries,
            int scannedClassEntries,
            int skippedClassEntries,
            long submittedClassBytes,
            List<String> warnings) {
        public ClassScanSummary {
            warnings = List.copyOf(warnings);
        }
    }

    public record Result(
            List<ArchiveFinding> archives,
            List<ProviderFinding> providers,
            List<DependencyEdge> dependencyEdges,
            List<TargetRangeFinding> targetRangeFindings,
            List<ClassScanSummary> classScanSummaries,
            List<String> warnings) {
        public Result {
            archives = List.copyOf(archives);
            providers = List.copyOf(providers);
            dependencyEdges = List.copyOf(dependencyEdges);
            targetRangeFindings = List.copyOf(targetRangeFindings);
            classScanSummaries = List.copyOf(classScanSummaries);
            warnings = List.copyOf(warnings);
        }

        /** Source-compatible constructor for callers that do not collect nested class inventory. */
        public Result(
                List<ArchiveFinding> archives,
                List<ProviderFinding> providers,
                List<DependencyEdge> dependencyEdges,
                List<TargetRangeFinding> targetRangeFindings,
                List<String> warnings) {
            this(archives, providers, dependencyEdges, targetRangeFindings, List.of(), warnings);
        }
    }

    private record Candidate(String path, byte[] bytes, JarJarCoordinate coordinate, ArchiveKind failure) {}
    private record Snapshot(
            String modsToml,
            String neoForgeModsToml,
            Map<String, Candidate> nestedJars,
            Map<String, byte[]> classEntries,
            int discoveredClassEntries,
            int skippedClassEntries,
            long capturedClassBytes,
            List<String> warnings,
            ArchiveKind incompleteKind,
            String incompleteReason) {}
    private record DeclaringMod(String archivePath, Forge1201JarInfo.Mod mod) {}
    private record ParsedArchive(
            ArchiveKind kind, String loaderKind, List<Forge1201JarInfo.Mod> mods, List<String> warnings) {}
    private record Interval(
            List<BigInteger> lower, boolean lowerInclusive, List<BigInteger> upper, boolean upperInclusive) {}
    private record ParsedRange(List<Interval> intervals) {}

    private static final class Budget {
        private long inflatedBytes;
        private int archiveCount;
        private final Set<String> seenDigests = new HashSet<>();
        private final List<ArchiveFinding> archives = new ArrayList<>();
        private final List<ProviderFinding> providers = new ArrayList<>();
        private final List<DeclaringMod> declaringMods = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<ClassScanSummary> classScanSummaries = new ArrayList<>();
        private long nestedClassBytesCaptured;
        private boolean archiveCountWarningAdded;
    }

    /** Analyze nested archives and installed providers when the Minecraft host version is known. */
    public static Result analyze(
            Path sourceJar,
            Forge1201JarInfo sourceInfo,
            Collection<Path> installedModDirectories,
            String targetMinecraftVersion) throws IOException {
        return analyze(sourceJar, sourceInfo, installedModDirectories, targetMinecraftVersion, null);
    }

    /** Analyze nested archives and evaluate both Minecraft and NeoForge host-provided dependencies. */
    public static Result analyze(
            Path sourceJar,
            Forge1201JarInfo sourceInfo,
            Collection<Path> installedModDirectories,
            String targetMinecraftVersion,
            String targetNeoForgeVersion) throws IOException {
        return analyze(sourceJar, sourceInfo, installedModDirectories,
                targetMinecraftVersion, targetNeoForgeVersion, null);
    }

    /** Analyze providers and stream source-nested class entries to the shared bytecode scanner. */
    public static Result analyze(
            Path sourceJar,
            Forge1201JarInfo sourceInfo,
            Collection<Path> installedModDirectories,
            String targetMinecraftVersion,
            String targetNeoForgeVersion,
            NestedClassConsumer nestedClassConsumer) throws IOException {
        Budget budget = new Budget();
        String sourcePath = "source/" + sourceJar.getFileName();
        String sourceDigest = sha256(sourceJar);
        if (sourceInfo != null) {
            List<Forge1201PreflightAnalyzer.ModMetadata> sourceMods = toReportMods(sourceInfo.mods());
            budget.archives.add(new ArchiveFinding(sourcePath, null, 0, ArchiveOrigin.SOURCE_ROOT,
                    Files.size(sourceJar), sourceDigest, ArchiveKind.FORGE_1201_MOD,
                    null, null, null, null, sourceMods, List.of()));
            addProvidersAndDeclarations(sourcePath, sourceInfo.mods(), ProviderKind.SOURCE_ROOT,
                    "FORGE_1201", sourceDigest, null, null, null, null, budget);
        }

        scanNestedFromZipFile(sourceJar, sourcePath, 1, ArchiveOrigin.INPUT_NESTED, budget, nestedClassConsumer);

        List<Path> directories = installedModDirectories == null ? List.of()
                : installedModDirectories.stream().filter(java.util.Objects::nonNull)
                        .map(path -> path.toAbsolutePath().normalize())
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparing(Path::toString)).toList();
        int directoryIndex = 0;
        for (Path directory : directories) {
            String directoryLabel = "mods-dir-" + directoryIndex++;
            List<Path> jars;
            try (var files = Files.list(directory)) {
                jars = files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
            } catch (IOException | RuntimeException exception) {
                budget.warnings.add(directoryLabel + ": could not enumerate installed mods directory: "
                        + conciseMessage(exception));
                continue;
            }

            for (Path jar : jars) {
                String displayPath = directoryLabel + "/" + jar.getFileName();
                try {
                    InstalledRoot root = readInstalledRoot(jar, displayPath);
                    if (!root.mods().isEmpty()) {
                        ProviderKind providerKind = root.loaderKind().equals("NEOFORGE")
                                ? ProviderKind.INSTALLED_TOP_LEVEL_NEOFORGE
                                : ProviderKind.INSTALLED_TOP_LEVEL_FORGE_1201;
                        String digest = sha256(jar);
                        ArchiveKind archiveKind = root.loaderKind().equals("NEOFORGE")
                                ? ArchiveKind.NEOFORGE_MOD : ArchiveKind.FORGE_1201_MOD;
                        budget.archives.add(new ArchiveFinding(displayPath, null, 0,
                                ArchiveOrigin.INSTALLED_TOP_LEVEL, Files.size(jar), digest, archiveKind,
                                null, null, null, null, toReportMods(root.mods()), List.of()));
                        addProvidersAndDeclarations(displayPath, root.mods(), providerKind, root.loaderKind(),
                                digest, null, null, null, null, budget);
                    }
                    // Installed provider archives are dependency evidence, not source bytecode to migrate.
                    scanNestedFromZipFile(jar, displayPath, 1, ArchiveOrigin.INSTALLED_NESTED, budget, null);
                } catch (IOException | RuntimeException exception) {
                    budget.warnings.add(displayPath + ": provider archive inspection failed: "
                            + conciseMessage(exception));
                }
            }
        }

        List<DependencyEdge> edges = resolveDependencies(budget, targetMinecraftVersion, targetNeoForgeVersion);
        List<TargetRangeFinding> targetRanges = evaluateTargetRanges(budget, targetMinecraftVersion);
        budget.archives.sort(Comparator.comparing(ArchiveFinding::archivePath));
        budget.providers.sort(Comparator.comparing(ProviderFinding::modId)
                .thenComparing(ProviderFinding::archivePath).thenComparing(ProviderFinding::version));
        edges = edges.stream().sorted(Comparator.comparing(DependencyEdge::declaringArchivePath)
                .thenComparing(DependencyEdge::declaringModId)
                .thenComparing(DependencyEdge::dependencyModId)
                .thenComparing(DependencyEdge::versionRange)).toList();
        targetRanges = targetRanges.stream().sorted(Comparator.comparing(TargetRangeFinding::declaringArchivePath)
                .thenComparing(TargetRangeFinding::declaringModId)
                .thenComparing(TargetRangeFinding::dependencyModId)).toList();
        budget.classScanSummaries.sort(Comparator.comparing(ClassScanSummary::archivePath));
        return new Result(budget.archives, budget.providers, edges, targetRanges,
                budget.classScanSummaries, budget.warnings.stream().distinct().sorted().toList());
    }

    private static List<Forge1201PreflightAnalyzer.ModMetadata> toReportMods(
            List<Forge1201JarInfo.Mod> mods) {
        return mods.stream()
                .map(mod -> new Forge1201PreflightAnalyzer.ModMetadata(mod.modId(), mod.version(), mod.displayName()))
                .sorted(Comparator.comparing(Forge1201PreflightAnalyzer.ModMetadata::modId)
                        .thenComparing(Forge1201PreflightAnalyzer.ModMetadata::version)).toList();
    }

    private record InstalledRoot(String loaderKind, List<Forge1201JarInfo.Mod> mods) {}

    private static InstalledRoot readInstalledRoot(Path jar, String displayPath) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry neoEntry = findEntry(zip, "META-INF/neoforge.mods.toml");
            if (neoEntry != null) {
                String toml = readZipEntry(zip, neoEntry, MAX_METADATA_BYTES);
                List<Forge1201JarInfo.Mod> mods = Forge1201JarInspector.readNeoForgeModMetadata(toml);
                return new InstalledRoot("NEOFORGE", mods);
            }
            ZipEntry forgeEntry = findEntry(zip, "META-INF/mods.toml");
            if (forgeEntry != null) {
                String toml = readZipEntry(zip, forgeEntry, MAX_METADATA_BYTES);
                Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201Metadata(Path.of(displayPath), toml);
                if (info != null) return new InstalledRoot("FORGE_1201", info.mods());
            }
            return new InstalledRoot("UNKNOWN", List.of());
        }
    }

    private static void addProvidersAndDeclarations(
            String archivePath,
            List<Forge1201JarInfo.Mod> mods,
            ProviderKind providerKind,
            String loaderKind,
            String digest,
            String group,
            String artifact,
            String versionRange,
            String artifactVersion,
            Budget budget) {
        for (Forge1201JarInfo.Mod mod : mods) {
            budget.providers.add(new ProviderFinding(mod.modId(), mod.version(), archivePath, providerKind,
                    loaderKind, digest, group, artifact, versionRange, artifactVersion));
            budget.declaringMods.add(new DeclaringMod(archivePath, mod));
        }
    }

    private static void scanNestedFromZipFile(
            Path parentJar, String parentDisplayPath, int depth, ArchiveOrigin origin, Budget budget,
            NestedClassConsumer nestedClassConsumer) throws IOException {
        Snapshot snapshot;
        try (ZipFile zip = new ZipFile(parentJar.toFile())) {
            snapshot = snapshotFromZipFile(zip, budget);
        } catch (IOException | RuntimeException exception) {
            budget.warnings.add(parentDisplayPath + ": cannot inspect nested archive entries: "
                    + conciseMessage(exception));
            return;
        }
        budget.warnings.addAll(snapshot.warnings());
        for (Candidate candidate : snapshot.nestedJars().values()) {
            processCandidate(candidate, parentDisplayPath, depth, origin, budget, nestedClassConsumer);
        }
    }

    private static Snapshot snapshotFromZipFile(ZipFile zip, Budget budget) throws IOException {
        Set<String> names = new TreeSet<>();
        var enumeration = zip.entries();
        int scannedEntryCount = 0;
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement().getName();
            scannedEntryCount++;
            if (names.size() < MAX_ROOT_ARCHIVE_ENTRIES) {
                names.add(name);
            }
            if (scannedEntryCount >= MAX_ROOT_ARCHIVE_ENTRIES) {
                if (enumeration.hasMoreElements()) {
                    budget.warnings.add("Archive entry inventory reached the configured "
                            + MAX_ROOT_ARCHIVE_ENTRIES + " entry limit; remaining entries were not inspected.");
                }
                break;
            }
        }

        List<String> warnings = new ArrayList<>();
        String modsToml = safeReadOptionalZipText(zip, findEntry(zip, "META-INF/mods.toml"),
                MAX_METADATA_BYTES, "META-INF/mods.toml", warnings);
        String neoToml = safeReadOptionalZipText(zip, findEntry(zip, "META-INF/neoforge.mods.toml"),
                MAX_METADATA_BYTES, "META-INF/neoforge.mods.toml", warnings);
        ZipEntry metadataEntry = findEntry(zip, JARJAR_METADATA);
        String metadataText = safeReadOptionalZipText(zip, metadataEntry, MAX_METADATA_BYTES, JARJAR_METADATA, warnings);
        ParsedJarJarMetadata parsed = parseJarJarMetadata(metadataText);

        Map<String, Candidate> candidates = new TreeMap<>();
        Map<String, JarJarCoordinate> coordinates = parsed.coordinates();
        Set<String> candidatePaths = new TreeSet<>(coordinates.keySet());
        for (String name : names) {
            if (isJarInJarArchivePath(name)) candidatePaths.add(name);
        }
        for (String path : candidatePaths) {
            JarJarCoordinate coordinate = coordinates.get(path);
            if (!isSafeArchivePath(path)) {
                candidates.put(path, new Candidate(path, null, coordinate, ArchiveKind.UNSAFE_PATH));
                continue;
            }
            ZipEntry entry = zip.getEntry(path);
            if (entry == null) {
                candidates.put(path, new Candidate(path, null, coordinate, ArchiveKind.MISSING_DECLARED_ARCHIVE));
                continue;
            }
            long remainingBudget = MAX_TOTAL_INFLATED_BYTES - budget.inflatedBytes;
            if (entry.getSize() > MAX_NESTED_ARCHIVE_BYTES || remainingBudget <= 0
                    || (entry.getSize() >= 0 && entry.getSize() > remainingBudget)) {
                candidates.put(path, new Candidate(path, null, coordinate, ArchiveKind.SIZE_LIMIT));
                warnings.add(path + " skipped because a nested archive size or total byte budget was reached.");
                continue;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                long readLimit = Math.min(MAX_NESTED_ARCHIVE_BYTES, remainingBudget);
                byte[] bytes = readBounded(in, readLimit);
                budget.inflatedBytes += bytes.length;
                candidates.put(path, new Candidate(path, bytes, coordinate, null));
            } catch (EntryTooLargeException exception) {
                candidates.put(path, new Candidate(path, null, coordinate, ArchiveKind.SIZE_LIMIT));
            }
        }
        warnings.addAll(parsed.warnings());
        return new Snapshot(modsToml, neoToml, candidates, Map.of(),
                0, 0, 0, warnings, null, null);
    }

    private record ParsedJarJarMetadata(Map<String, JarJarCoordinate> coordinates, List<String> warnings) {}

    private static ParsedJarJarMetadata parseJarJarMetadata(String json) {
        Map<String, JarJarCoordinate> result = new TreeMap<>();
        List<String> warnings = new ArrayList<>();
        if (json == null || json.isBlank()) return new ParsedJarJarMetadata(result, warnings);
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray jars = root.has("jars") && root.get("jars").isJsonArray()
                    ? root.getAsJsonArray("jars") : new JsonArray();
            for (JsonElement element : jars) {
                if (!element.isJsonObject()) {
                    warnings.add("Jar-in-Jar metadata contains a non-object entry.");
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String path = jsonString(item, "path");
                if (path == null || path.isBlank()) {
                    warnings.add("Jar-in-Jar metadata entry is missing its path.");
                    continue;
                }
                JsonObject identifier = item.has("identifier") && item.get("identifier").isJsonObject()
                        ? item.getAsJsonObject("identifier") : new JsonObject();
                JsonObject version = item.has("version") && item.get("version").isJsonObject()
                        ? item.getAsJsonObject("version") : new JsonObject();
                result.put(path, new JarJarCoordinate(path, jsonString(identifier, "group"),
                        jsonString(identifier, "artifact"), jsonString(version, "range"),
                        jsonString(version, "artifactVersion")));
            }
        } catch (RuntimeException exception) {
            warnings.add("Jar-in-Jar metadata JSON could not be parsed: " + conciseMessage(exception));
        }
        return new ParsedJarJarMetadata(result, warnings);
    }

    private static String jsonString(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) return null;
        try {
            return object.get(name).getAsString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void processCandidate(
            Candidate candidate, String parentPath, int depth, ArchiveOrigin origin, Budget budget,
            NestedClassConsumer nestedClassConsumer) {
        String displayPath = parentPath + "!/" + candidate.path();
        JarJarCoordinate coordinate = candidate.coordinate();
        String group = coordinate == null ? null : coordinate.group();
        String artifact = coordinate == null ? null : coordinate.artifact();
        String range = coordinate == null ? null : coordinate.versionRange();
        String artifactVersion = coordinate == null ? null : coordinate.artifactVersion();

        if (budget.archiveCount >= MAX_NESTED_ARCHIVES) {
            if (!budget.archiveCountWarningAdded) {
                String warning = "Maximum nested archive count (" + MAX_NESTED_ARCHIVES
                        + ") reached; additional nested archives were not inspected.";
                budget.warnings.add(warning);
                budget.archives.add(new ArchiveFinding(displayPath, parentPath, depth, origin, -1, null,
                        ArchiveKind.ARCHIVE_COUNT_LIMIT, group, artifact, range, artifactVersion, List.of(),
                        List.of(warning)));
                budget.archiveCountWarningAdded = true;
            }
            return;
        }
        budget.archiveCount++;

        if (candidate.failure() != null) {
            budget.archives.add(new ArchiveFinding(displayPath, parentPath, depth, origin,
                    candidate.bytes() == null ? -1 : candidate.bytes().length, null, candidate.failure(),
                    group, artifact, range, artifactVersion, List.of(), List.of(candidate.failure().toString())));
            if (candidate.failure() == ArchiveKind.UNSAFE_PATH) {
                budget.warnings.add(displayPath + ": rejected unsafe nested archive path.");
            } else if (candidate.failure() == ArchiveKind.MISSING_DECLARED_ARCHIVE) {
                budget.warnings.add(displayPath + ": declared Jar-in-Jar archive is missing.");
            } else if (candidate.failure() == ArchiveKind.SIZE_LIMIT) {
                budget.warnings.add(displayPath + ": nested archive exceeds the configured byte limit.");
            }
            return;
        }

        byte[] bytes = candidate.bytes();
        String digest = sha256(bytes);
        if (depth > MAX_NESTED_DEPTH) {
            budget.archives.add(new ArchiveFinding(displayPath, parentPath, depth, origin, bytes.length, digest,
                    ArchiveKind.DEPTH_LIMIT, group, artifact, range, artifactVersion, List.of(),
                    List.of("Maximum nested archive depth exceeded.")));
            return;
        }
        if (!budget.seenDigests.add(digest)) {
            budget.archives.add(new ArchiveFinding(displayPath, parentPath, depth, origin, bytes.length, digest,
                    ArchiveKind.DUPLICATE_CONTENT_SKIPPED, group, artifact, range, artifactVersion, List.of(),
                    List.of("Identical archive content was already inspected; recursive expansion was skipped.")));
            return;
        }

        Snapshot snapshot;
        try {
            snapshot = snapshotFromBytes(bytes, budget, origin == ArchiveOrigin.INPUT_NESTED
                    && nestedClassConsumer != null, displayPath);
        } catch (IOException | RuntimeException exception) {
            ArchiveFinding finding = new ArchiveFinding(displayPath, parentPath, depth, origin, bytes.length, digest,
                    ArchiveKind.CORRUPT_ARCHIVE, group, artifact, range, artifactVersion, List.of(),
                    List.of("Nested ZIP could not be read: " + conciseMessage(exception)));
            budget.archives.add(finding);
            return;
        }
        budget.warnings.addAll(snapshot.warnings());
        if (snapshot.incompleteKind() != null) {
            budget.archives.add(new ArchiveFinding(displayPath, parentPath, depth, origin, bytes.length, digest,
                    snapshot.incompleteKind(), group, artifact, range, artifactVersion, List.of(),
                    List.of(snapshot.incompleteReason())));
            if (origin == ArchiveOrigin.INPUT_NESTED && nestedClassConsumer != null) {
                List<String> classWarnings = new ArrayList<>();
                classWarnings.add("Nested archive parsing stopped before class scanning: " + snapshot.incompleteReason());
                budget.classScanSummaries.add(new ClassScanSummary(displayPath, snapshot.incompleteKind(),
                        snapshot.discoveredClassEntries(), 0,
                        snapshot.discoveredClassEntries(), 0, classWarnings));
                budget.warnings.add(displayPath + ": nested class inventory is incomplete because archive parsing stopped.");
            }
            return;
        }
        ParsedArchive parsed = classifyArchive(displayPath, snapshot);
        List<Forge1201PreflightAnalyzer.ModMetadata> reportMods = parsed.mods().stream()
                .map(mod -> new Forge1201PreflightAnalyzer.ModMetadata(mod.modId(), mod.version(), mod.displayName()))
                .sorted(Comparator.comparing(Forge1201PreflightAnalyzer.ModMetadata::modId)
                        .thenComparing(Forge1201PreflightAnalyzer.ModMetadata::version)).toList();
        budget.archives.add(new ArchiveFinding(displayPath, parentPath, depth, origin, bytes.length, digest,
                parsed.kind(), group, artifact, range, artifactVersion, reportMods, parsed.warnings()));

        if (parsed.kind() == ArchiveKind.FORGE_1201_MOD || parsed.kind() == ArchiveKind.NEOFORGE_MOD) {
            ProviderKind providerKind = providerKind(origin, parsed.loaderKind());
            addProvidersAndDeclarations(displayPath, parsed.mods(), providerKind, parsed.loaderKind(),
                    digest, group, artifact, range, artifactVersion, budget);
        }
        if (origin == ArchiveOrigin.INPUT_NESTED && nestedClassConsumer != null) {
            scanNestedClassEntries(displayPath, parsed.kind(), snapshot, budget, nestedClassConsumer);
        }
        for (Candidate child : snapshot.nestedJars().values()) {
            processCandidate(child, displayPath, depth + 1, origin, budget, nestedClassConsumer);
        }
    }

    private static Snapshot snapshotFromBytes(
            byte[] bytes, Budget budget, boolean captureClassEntries, String archivePath) throws IOException {
        Map<String, byte[]> selected = new TreeMap<>();
        Map<String, byte[]> classEntries = new TreeMap<>();
        Set<String> seenClassEntryNames = new HashSet<>();
        Set<String> duplicateClassEntryNames = new HashSet<>();
        Set<String> oversizedJars = new TreeSet<>();
        List<String> warnings = new ArrayList<>();
        long archiveInflated = 0;
        long capturedClassBytes = 0;
        int discoveredClassEntries = 0;
        int skippedClassEntries = 0;
        int entryCount = 0;
        ArchiveKind incompleteKind = null;
        String incompleteReason = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_NESTED_ARCHIVE_ENTRIES) {
                    throw new BudgetLimitException("nested archive entry-count limit exceeded");
                }
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                boolean metadata = lower.equals("meta-inf/mods.toml")
                        || lower.equals("meta-inf/neoforge.mods.toml")
                        || lower.equals(JARJAR_METADATA.toLowerCase(Locale.ROOT));
                boolean jarCandidate = lower.endsWith(".jar");
                boolean classCandidate = captureClassEntries && !entry.isDirectory() && lower.endsWith(".class");
                boolean safe = isSafeArchivePath(name);
                boolean duplicateClassEntry = false;
                if (classCandidate) {
                    discoveredClassEntries++;
                    if (!seenClassEntryNames.add(name)) {
                        duplicateClassEntry = true;
                        if (duplicateClassEntryNames.add(name)) {
                            byte[] previous = classEntries.remove(name);
                            if (previous != null) {
                                capturedClassBytes -= previous.length;
                                skippedClassEntries += 2;
                            } else {
                                skippedClassEntries++;
                            }
                            warnings.add(archivePath + "!/" + name
                                    + " is duplicated within the archive; all entries with this name were skipped.");
                        } else {
                            skippedClassEntries++;
                        }
                    } else if (!safe) {
                        skippedClassEntries++;
                        warnings.add(archivePath + "!/" + name + " skipped: unsafe class-entry path.");
                    }
                }
                boolean captureClass = classCandidate && safe && !duplicateClassEntry
                        && !duplicateClassEntryNames.contains(name);
                boolean capture = metadata || (jarCandidate && safe) || captureClass;
                long classEntryLimit = Math.min(MAX_CLASS_ENTRY_BYTES,
                        Math.max(0, MAX_NESTED_CLASS_BYTES_PER_ARCHIVE - capturedClassBytes));
                long entryBytes = 0;
                boolean classCaptureLimitExceeded = false;
                ByteArrayOutputStream output = capture ? new ByteArrayOutputStream() : null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    entryBytes += read;
                    archiveInflated += read;
                    budget.inflatedBytes += read;
                    if (entryBytes > MAX_ARCHIVE_INFLATED_BYTES
                            || archiveInflated > MAX_ARCHIVE_INFLATED_BYTES
                            || budget.inflatedBytes > MAX_TOTAL_INFLATED_BYTES) {
                        throw new BudgetLimitException("nested archive inflated-byte budget exceeded");
                    }
                    if (output != null) {
                        long entryLimit = metadata ? MAX_METADATA_BYTES
                                : classCandidate ? classEntryLimit : MAX_NESTED_ARCHIVE_BYTES;
                        if (entryBytes <= entryLimit) {
                            output.write(buffer, 0, read);
                        } else {
                            if (classCandidate) classCaptureLimitExceeded = true;
                            output = null;
                        }
                    }
                }
                if (metadata) {
                    if (entryBytes > MAX_METADATA_BYTES || output == null) {
                        warnings.add(name + " exceeds the metadata read limit and was not parsed.");
                    } else {
                        selected.putIfAbsent(name, output.toByteArray());
                    }
                } else if (jarCandidate && safe) {
                    if (output == null || entryBytes > MAX_NESTED_ARCHIVE_BYTES) {
                        oversizedJars.add(name);
                    } else {
                        selected.putIfAbsent(name, output.toByteArray());
                    }
                } else if (jarCandidate && !safe) {
                    oversizedJars.add(name);
                } else if (classCandidate && captureClass) {
                    if (output == null || classCaptureLimitExceeded
                            || entryBytes > classEntryLimit) {
                        skippedClassEntries++;
                        warnings.add(archivePath + "!/" + name + " skipped: class payload exceeded the "
                                + "per-class or per-archive class-byte capture limit.");
                    } else {
                        byte[] classBytes = output.toByteArray();
                        classEntries.put(name, classBytes);
                        capturedClassBytes += classBytes.length;
                    }
                }
                zip.closeEntry();
            }
        } catch (ZipException exception) {
            throw exception;
        } catch (BudgetLimitException exception) {
            warnings.add(exception.getMessage());
            incompleteKind = exception.getMessage().contains("entry-count")
                    ? ArchiveKind.ENTRY_COUNT_LIMIT : ArchiveKind.SIZE_LIMIT;
            incompleteReason = exception.getMessage();
        }
        if (entryCount == 0 && !hasZipSignature(bytes)) {
            throw new ZipException("nested entry is not a readable ZIP archive");
        }

        String modsToml = findText(selected, "META-INF/mods.toml");
        String neoToml = findText(selected, "META-INF/neoforge.mods.toml");
        String metadata = findText(selected, JARJAR_METADATA);
        ParsedJarJarMetadata parsed = parseJarJarMetadata(metadata);
        warnings.addAll(parsed.warnings());

        Map<String, Candidate> candidates = new TreeMap<>();
        Set<String> paths = new TreeSet<>(parsed.coordinates().keySet());
        for (String name : selected.keySet()) {
            if (isJarInJarArchivePath(name)) paths.add(name);
        }
        paths.addAll(oversizedJars);
        for (String path : paths) {
            JarJarCoordinate coordinate = parsed.coordinates().get(path);
            if (!isSafeArchivePath(path)) {
                candidates.put(path, new Candidate(path, null, coordinate, ArchiveKind.UNSAFE_PATH));
            } else if (oversizedJars.contains(path)) {
                candidates.put(path, new Candidate(path, null, coordinate, ArchiveKind.SIZE_LIMIT));
            } else if (!selected.containsKey(path)) {
                candidates.put(path, new Candidate(path, null, coordinate, ArchiveKind.MISSING_DECLARED_ARCHIVE));
            } else {
                candidates.put(path, new Candidate(path, selected.get(path), coordinate, null));
            }
        }
        return new Snapshot(modsToml, neoToml, candidates, classEntries,
                discoveredClassEntries, skippedClassEntries, capturedClassBytes,
                warnings, incompleteKind, incompleteReason);
    }

    private static void scanNestedClassEntries(
            String archivePath,
            ArchiveKind archiveKind,
            Snapshot snapshot,
            Budget budget,
            NestedClassConsumer consumer) {
        int scanned = 0;
        int skipped = snapshot.skippedClassEntries();
        long captured = 0;
        // Keep class-entry capture warnings with the per-archive class summary as well as
        // the graph-wide warning list. Otherwise skipped oversized/unsafe/duplicate classes
        // are counted, but their reason is missing from the summary that explains those counts.
        List<String> warnings = new ArrayList<>(snapshot.warnings().stream()
                .filter(warning -> warning.contains("class-entry path")
                        || warning.contains("duplicated within the archive")
                        || warning.contains("class payload exceeded"))
                .toList());
        if (archiveKind == ArchiveKind.NEOFORGE_MOD) {
            skipped += snapshot.classEntries().size();
            if (!snapshot.classEntries().isEmpty()) {
                warnings.add("Nested NeoForge mod bytecode was not included in the legacy Forge source inventory.");
            }
        } else {
            for (Map.Entry<String, byte[]> entry : snapshot.classEntries().entrySet()) {
                byte[] classBytes = entry.getValue();
                if (budget.nestedClassBytesCaptured + classBytes.length > MAX_TOTAL_NESTED_CLASS_BYTES) {
                    skipped++;
                    warnings.add(archivePath + "!/" + entry.getKey()
                            + " skipped: aggregate nested class-byte capture limit reached.");
                    continue;
                }
                boolean accepted;
                try {
                    accepted = consumer.scan(archivePath, entry.getKey(), classBytes);
                } catch (RuntimeException exception) {
                    accepted = false;
                    warnings.add(archivePath + "!/" + entry.getKey()
                            + " could not be scanned: " + conciseMessage(exception));
                }
                budget.nestedClassBytesCaptured += classBytes.length;
                captured += classBytes.length;
                if (accepted) {
                    scanned++;
                } else {
                    skipped++;
                    warnings.add(archivePath + "!/" + entry.getKey() + " was rejected by the bytecode scanner.");
                }
            }
        }
        if (!warnings.isEmpty()) {
            budget.warnings.addAll(warnings);
        }
        budget.classScanSummaries.add(new ClassScanSummary(archivePath, archiveKind,
                snapshot.discoveredClassEntries(), scanned, skipped, captured, warnings));
    }

    private static boolean hasZipSignature(byte[] bytes) {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') return false;
        int third = bytes[2] & 0xff;
        int fourth = bytes[3] & 0xff;
        return (third == 0x03 && fourth == 0x04)
                || (third == 0x05 && fourth == 0x06)
                || (third == 0x07 && fourth == 0x08);
    }

    private static String findText(Map<String, byte[]> selected, String name) {
        return selected.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(entry -> new String(entry.getValue(), StandardCharsets.UTF_8)).findFirst().orElse(null);
    }

    private static ParsedArchive classifyArchive(String displayPath, Snapshot snapshot) {
        List<String> warnings = new ArrayList<>();
        warnings.addAll(snapshot.warnings());
        if (snapshot.neoForgeModsToml() != null) {
            List<Forge1201JarInfo.Mod> mods = Forge1201JarInspector.readNeoForgeModMetadata(snapshot.neoForgeModsToml());
            if (!mods.isEmpty()) return new ParsedArchive(ArchiveKind.NEOFORGE_MOD, "NEOFORGE", mods, warnings);
            warnings.add("NeoForge metadata exists but no scalar mod declarations were recognized.");
            return new ParsedArchive(ArchiveKind.LIBRARY_OR_UNRECOGNIZED, "NEOFORGE_UNRECOGNIZED", List.of(), warnings);
        }
        if (snapshot.modsToml() != null) {
            Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201Metadata(Path.of(displayPath), snapshot.modsToml());
            if (info != null) return new ParsedArchive(ArchiveKind.FORGE_1201_MOD, "FORGE_1201", info.mods(), warnings);
            warnings.add("mods.toml exists but the archive was not recognized as a Forge 1.20.1 mod.");
        }
        return new ParsedArchive(ArchiveKind.LIBRARY_OR_UNRECOGNIZED, "UNKNOWN", List.of(), warnings);
    }

    private static ProviderKind providerKind(ArchiveOrigin origin, String loaderKind) {
        if (origin == ArchiveOrigin.INPUT_NESTED) return ProviderKind.SOURCE_NESTED;
        if ("NEOFORGE".equals(loaderKind)) return ProviderKind.INSTALLED_NESTED_NEOFORGE;
        return ProviderKind.INSTALLED_NESTED_FORGE_1201;
    }

    private static List<DependencyEdge> resolveDependencies(
            Budget budget, String targetMinecraftVersion, String targetNeoForgeVersion) {
        Map<String, List<ProviderFinding>> providers = new HashMap<>();
        for (ProviderFinding provider : budget.providers) {
            providers.computeIfAbsent(provider.modId().toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(provider);
        }
        List<DependencyEdge> result = new ArrayList<>();
        for (DeclaringMod declaring : budget.declaringMods) {
            for (Forge1201JarInfo.Dependency dependency : declaring.mod().dependencies()) {
                String id = dependency.modId().toLowerCase(Locale.ROOT);
                String hostVersion = switch (id) {
                    case "minecraft" -> targetMinecraftVersion;
                    case "neoforge" -> targetNeoForgeVersion;
                    default -> null;
                };
                if (id.equals("minecraft") || id.equals("forge") || id.equals("fml") || id.equals("neoforge")) {
                    VersionRangeStatus rangeStatus = hostVersion == null || hostVersion.isBlank()
                            ? VersionRangeStatus.NOT_EVALUATED
                            : evaluateVersionRange(dependency.versionRange(), hostVersion);
                    DependencyStatus hostStatus = rangeStatus == VersionRangeStatus.OUTSIDE_RANGE
                            ? DependencyStatus.HOST_VERSION_MISMATCH : DependencyStatus.HOST_BRIDGE;
                    result.add(new DependencyEdge(declaring.archivePath(), declaring.mod().modId(), id,
                            dependency.versionRange(), dependency.mandatory(), dependency.type(),
                            dependency.ordering(), dependency.side(), hostStatus, rangeStatus,
                            List.of(), hostVersion == null || hostVersion.isBlank() ? List.of() : List.of(hostVersion)));
                    continue;
                }

                List<ProviderFinding> candidates = providers.getOrDefault(id, List.of()).stream()
                        .sorted(Comparator.comparing(ProviderFinding::archivePath)).toList();
                if (candidates.isEmpty()) {
                    DependencyStatus missingStatus = switch (dependency.type()) {
                        case REQUIRED -> DependencyStatus.REQUIRED_MISSING;
                        case OPTIONAL -> DependencyStatus.OPTIONAL_MISSING;
                        case INCOMPATIBLE -> DependencyStatus.INCOMPATIBLE_PROVIDER_ABSENT;
                        case DISCOURAGED -> DependencyStatus.DISCOURAGED_PROVIDER_ABSENT;
                        case UNKNOWN -> DependencyStatus.UNSUPPORTED_DEPENDENCY_TYPE;
                    };
                    result.add(new DependencyEdge(declaring.archivePath(), declaring.mod().modId(), id,
                            dependency.versionRange(), dependency.mandatory(), dependency.type(),
                            dependency.ordering(), dependency.side(), missingStatus,
                            VersionRangeStatus.NOT_EVALUATED, List.of(), List.of()));
                    continue;
                }

                if (dependency.type() == Forge1201JarInfo.DependencyType.INCOMPATIBLE
                        || dependency.type() == Forge1201JarInfo.DependencyType.DISCOURAGED) {
                    DependencyStatus presenceStatus = dependency.type() == Forge1201JarInfo.DependencyType.INCOMPATIBLE
                            ? DependencyStatus.INCOMPATIBLE_PROVIDER_PRESENT
                            : DependencyStatus.DISCOURAGED_PROVIDER_PRESENT;
                    result.add(new DependencyEdge(declaring.archivePath(), declaring.mod().modId(), id,
                            dependency.versionRange(), dependency.mandatory(), dependency.type(),
                            dependency.ordering(), dependency.side(), presenceStatus,
                            aggregateProviderRangeStatus(candidates, dependency.versionRange()),
                            candidates.stream().map(ProviderFinding::archivePath).distinct().sorted().toList(),
                            candidates.stream().map(ProviderFinding::version).distinct().sorted().toList()));
                    continue;
                }

                if (dependency.type() == Forge1201JarInfo.DependencyType.UNKNOWN) {
                    result.add(new DependencyEdge(declaring.archivePath(), declaring.mod().modId(), id,
                            dependency.versionRange(), dependency.mandatory(), dependency.type(),
                            dependency.ordering(), dependency.side(), DependencyStatus.UNSUPPORTED_DEPENDENCY_TYPE,
                            VersionRangeStatus.NOT_EVALUATED,
                            candidates.stream().map(ProviderFinding::archivePath).distinct().sorted().toList(),
                            candidates.stream().map(ProviderFinding::version).distinct().sorted().toList()));
                    continue;
                }

                List<ProviderFinding> matches = new ArrayList<>();
                List<ProviderFinding> unknown = new ArrayList<>();
                for (ProviderFinding candidate : candidates) {
                    VersionRangeStatus status = evaluateVersionRange(dependency.versionRange(), candidate.version());
                    if (status == VersionRangeStatus.MATCH) matches.add(candidate);
                    else if (status == VersionRangeStatus.UNPARSEABLE || status == VersionRangeStatus.NOT_EVALUATED) {
                        unknown.add(candidate);
                    }
                }
                List<ProviderFinding> chosen;
                DependencyStatus status;
                VersionRangeStatus rangeStatus;
                if (candidates.size() > 1) {
                    // Duplicate provider IDs are a pack conflict even if only one candidate happens to satisfy the range.
                    chosen = candidates;
                    status = DependencyStatus.PROVIDER_AMBIGUOUS;
                    rangeStatus = !matches.isEmpty() ? VersionRangeStatus.MATCH
                            : !unknown.isEmpty() ? VersionRangeStatus.UNPARSEABLE : VersionRangeStatus.OUTSIDE_RANGE;
                } else if (matches.size() == 1) {
                    chosen = matches;
                    status = DependencyStatus.PROVIDER_PRESENT;
                    rangeStatus = VersionRangeStatus.MATCH;
                } else if (!unknown.isEmpty()) {
                    chosen = unknown;
                    status = DependencyStatus.PROVIDER_PRESENT;
                    rangeStatus = VersionRangeStatus.UNPARSEABLE;
                } else {
                    chosen = candidates;
                    status = DependencyStatus.PROVIDER_VERSION_MISMATCH;
                    rangeStatus = VersionRangeStatus.OUTSIDE_RANGE;
                }
                result.add(new DependencyEdge(declaring.archivePath(), declaring.mod().modId(), id,
                        dependency.versionRange(), dependency.mandatory(), dependency.type(),
                        dependency.ordering(), dependency.side(), status, rangeStatus,
                        chosen.stream().map(ProviderFinding::archivePath).distinct().sorted().toList(),
                        chosen.stream().map(ProviderFinding::version).distinct().sorted().toList()));
            }
        }
        return result;
    }

    private static VersionRangeStatus aggregateProviderRangeStatus(
            List<ProviderFinding> providers, String versionRange) {
        boolean unparseable = false;
        for (ProviderFinding provider : providers) {
            VersionRangeStatus status = evaluateVersionRange(versionRange, provider.version());
            if (status == VersionRangeStatus.MATCH) return VersionRangeStatus.MATCH;
            if (status == VersionRangeStatus.UNPARSEABLE || status == VersionRangeStatus.NOT_EVALUATED) {
                unparseable = true;
            }
        }
        return unparseable ? VersionRangeStatus.UNPARSEABLE : VersionRangeStatus.OUTSIDE_RANGE;
    }

    private static List<TargetRangeFinding> evaluateTargetRanges(Budget budget, String targetVersion) {
        List<TargetRangeFinding> result = new ArrayList<>();
        if (targetVersion == null || targetVersion.isBlank()) return result;
        for (DeclaringMod declaring : budget.declaringMods) {
            for (Forge1201JarInfo.Dependency dependency : declaring.mod().dependencies()) {
                if (!dependency.modId().equalsIgnoreCase("minecraft")) continue;
                VersionRangeStatus status = evaluateVersionRange(dependency.versionRange(), targetVersion);
                String explanation = switch (status) {
                    case MATCH -> "The declared Minecraft range includes the selected target version.";
                    case OUTSIDE_RANGE -> "The declared Minecraft range excludes the selected target. A metadata-range translation may be required; this is not an unconditional compatibility or load-failure verdict.";
                    case UNPARSEABLE -> "The range or target version could not be evaluated safely; manual range review is required.";
                    case NOT_EVALUATED -> "No target range evaluation was performed.";
                };
                result.add(new TargetRangeFinding(declaring.archivePath(), declaring.mod().modId(),
                        dependency.modId(), dependency.versionRange(), targetVersion, status, explanation));
                if (status == VersionRangeStatus.OUTSIDE_RANGE) {
                    budget.warnings.add(declaring.archivePath() + ": " + declaring.mod().modId()
                            + " declares Minecraft " + dependency.versionRange() + ", which excludes target "
                            + targetVersion + "; metadata translation may be required.");
                }
            }
        }
        return result;
    }

    /** Conservative Maven-style range evaluator. Non-numeric/qualified versions stay explicitly unverified. */
    public static VersionRangeStatus evaluateVersionRange(String range, String version) {
        if (range == null || range.isBlank() || range.trim().equals("*")) return VersionRangeStatus.MATCH;
        List<BigInteger> actual = parseNumericVersion(version);
        ParsedRange parsed = parseRange(range.trim());
        if (actual == null || parsed == null) return VersionRangeStatus.UNPARSEABLE;
        for (Interval interval : parsed.intervals()) {
            boolean afterLower = interval.lower() == null
                    || compareVersions(actual, interval.lower()) > 0
                    || (interval.lowerInclusive() && compareVersions(actual, interval.lower()) == 0);
            boolean beforeUpper = interval.upper() == null
                    || compareVersions(actual, interval.upper()) < 0
                    || (interval.upperInclusive() && compareVersions(actual, interval.upper()) == 0);
            if (afterLower && beforeUpper) return VersionRangeStatus.MATCH;
        }
        return VersionRangeStatus.OUTSIDE_RANGE;
    }

    private static ParsedRange parseRange(String range) {
        if (!(range.startsWith("[") || range.startsWith("("))) {
            List<BigInteger> exact = parseNumericVersion(range);
            return exact == null ? null : new ParsedRange(List.of(new Interval(exact, true, exact, true)));
        }
        List<Interval> intervals = new ArrayList<>();
        int index = 0;
        while (index < range.length()) {
            while (index < range.length() && (Character.isWhitespace(range.charAt(index)) || range.charAt(index) == ',')) index++;
            if (index >= range.length()) break;
            char open = range.charAt(index);
            if (open != '[' && open != '(') return null;
            int close = -1;
            for (int i = index + 1; i < range.length(); i++) {
                if (range.charAt(i) == ']' || range.charAt(i) == ')') {
                    close = i;
                    break;
                }
            }
            if (close < 0) return null;
            String body = range.substring(index + 1, close);
            int comma = body.indexOf(',');
            List<BigInteger> lower;
            List<BigInteger> upper;
            boolean lowerInclusive = open == '[';
            boolean upperInclusive = range.charAt(close) == ']';
            if (comma < 0) {
                List<BigInteger> exact = parseNumericVersion(body.trim());
                if (exact == null || !lowerInclusive || !upperInclusive) return null;
                lower = exact;
                upper = exact;
            } else {
                if (body.indexOf(',', comma + 1) >= 0) return null;
                String lowerText = body.substring(0, comma).trim();
                String upperText = body.substring(comma + 1).trim();
                lower = lowerText.isEmpty() ? null : parseNumericVersion(lowerText);
                upper = upperText.isEmpty() ? null : parseNumericVersion(upperText);
                if ((!lowerText.isEmpty() && lower == null) || (!upperText.isEmpty() && upper == null)) return null;
                if (lower == null && upper == null) return null;
            }
            intervals.add(new Interval(lower, lowerInclusive, upper, upperInclusive));
            index = close + 1;
            while (index < range.length() && Character.isWhitespace(range.charAt(index))) index++;
            if (index < range.length() && range.charAt(index) != ',') return null;
        }
        return intervals.isEmpty() ? null : new ParsedRange(List.copyOf(intervals));
    }

    private static List<BigInteger> parseNumericVersion(String version) {
        if (version == null || version.isBlank()) return null;
        String[] pieces = version.trim().split("\\.", -1);
        List<BigInteger> result = new ArrayList<>();
        for (String piece : pieces) {
            if (!piece.matches("[0-9]+")) return null;
            try {
                result.add(new BigInteger(piece));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return List.copyOf(result);
    }

    private static int compareVersions(List<BigInteger> left, List<BigInteger> right) {
        int count = Math.max(left.size(), right.size());
        for (int i = 0; i < count; i++) {
            BigInteger a = i < left.size() ? left.get(i) : BigInteger.ZERO;
            BigInteger b = i < right.size() ? right.get(i) : BigInteger.ZERO;
            int compared = a.compareTo(b);
            if (compared != 0) return compared;
        }
        return 0;
    }

    private static boolean isJarInJarArchivePath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jar")
                && (lower.startsWith("meta-inf/jarjar/") || lower.startsWith("meta-inf/jars/"));
    }

    private static boolean isSafeArchivePath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.startsWith("\\")
                || path.contains("\\") || path.matches("^[A-Za-z]:.*")) return false;
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) return false;
        }
        return true;
    }

    private static ZipEntry findEntry(ZipFile zip, String name) {
        ZipEntry exact = zip.getEntry(name);
        if (exact != null) return exact;
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().equalsIgnoreCase(name)) return entry;
        }
        return null;
    }

    private static String safeReadOptionalZipText(
            ZipFile zip, ZipEntry entry, int limit, String label, List<String> warnings) throws IOException {
        if (entry == null) return null;
        try {
            return readZipEntry(zip, entry, limit);
        } catch (EntryTooLargeException exception) {
            warnings.add(label + " exceeds the metadata read limit and was not parsed.");
            return null;
        }
    }

    private static String readOptionalZipText(ZipFile zip, ZipEntry entry, int limit) throws IOException {
        return entry == null ? null : readZipEntry(zip, entry, limit);
    }

    private static String readZipEntry(ZipFile zip, ZipEntry entry, int limit) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return new String(readBounded(in, limit), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readBounded(InputStream in, long limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new EntryTooLargeException("entry exceeds " + limit + " bytes");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) >= 0) digest.update(buffer, 0, read);
            }
            return hex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("The JVM does not provide SHA-256", impossible);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("The JVM does not provide SHA-256", impossible);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) out.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return out.toString();
    }

    private static String conciseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private static final class EntryTooLargeException extends IOException {
        private EntryTooLargeException(String message) { super(message); }
    }

    private static final class BudgetLimitException extends IOException {
        private BudgetLimitException(String message) { super(message); }
    }
}
