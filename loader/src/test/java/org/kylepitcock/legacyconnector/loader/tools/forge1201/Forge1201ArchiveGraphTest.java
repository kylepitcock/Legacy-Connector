package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class Forge1201ArchiveGraphTest {
    @TempDir
    Path tempDir;

    @Test
    void recursivelyInventoriesNestedModsAndLibrariesAndBuildsTargetRangeFinding() throws IOException {
        byte[] embeddedMod = forgeModJar("embedded", "2.1.0", List.of(
                dep("minecraft", "[1.20.1,)", true), dep("forge", "[47,)", true)));
        byte[] library = zip(Map.of("META-INF/NOTICE.txt", bytes("library")));
        String metadata = jarJarMetadata(
                "META-INF/jarjar/embedded-mod.jar", "com.example", "embedded-mod", "[2.0,3.0)", "2.1.0",
                "META-INF/jarjar/library.jar", "org.example", "shared-library", "[1.0,)", "1.4.0");
        Path source = writeSourceJar("source.jar",
                forgeModJar("alpha", "1.0.0", List.of(
                        dep("minecraft", "[1.20.1,1.21)", true),
                        dep("forge", "[47,)", true),
                        dep("embedded", "[2.0,3.0)", true))),
                Map.of(
                        "META-INF/jarjar/metadata.json", bytes(metadata),
                        "META-INF/jarjar/embedded-mod.jar", embeddedMod,
                        "META-INF/jarjar/library.jar", library));

        Forge1201JarInfo sourceInfo = Forge1201JarInspector.inspectForge1201(source);
        assertNotNull(sourceInfo);
        Forge1201ArchiveGraph.Result graph = Forge1201ArchiveGraph.analyze(
                source, sourceInfo, List.of(), "1.21.1");

        var sourceArchive = graph.archives().stream()
                .filter(archive -> archive.origin() == Forge1201ArchiveGraph.ArchiveOrigin.SOURCE_ROOT)
                .findFirst().orElseThrow();
        assertEquals(0, sourceArchive.depth());
        assertTrue(sourceArchive.sha256().matches("[0-9a-f]{64}"));

        var nestedMod = graph.archives().stream()
                .filter(archive -> archive.archivePath().endsWith("embedded-mod.jar")).findFirst().orElseThrow();
        assertEquals(Forge1201ArchiveGraph.ArchiveKind.FORGE_1201_MOD, nestedMod.kind());
        assertEquals("com.example", nestedMod.jarJarGroup());
        assertEquals("embedded-mod", nestedMod.jarJarArtifact());
        assertEquals("[2.0,3.0)", nestedMod.declaredVersionRange());
        assertEquals("2.1.0", nestedMod.declaredArtifactVersion());
        assertEquals("embedded", nestedMod.mods().getFirst().modId());
        assertTrue(nestedMod.sha256().matches("[0-9a-f]{64}"));

        var nestedLibrary = graph.archives().stream()
                .filter(archive -> archive.archivePath().endsWith("library.jar")).findFirst().orElseThrow();
        assertEquals(Forge1201ArchiveGraph.ArchiveKind.LIBRARY_OR_UNRECOGNIZED, nestedLibrary.kind());

        var dependency = graph.dependencyEdges().stream()
                .filter(edge -> edge.declaringModId().equals("alpha") && edge.dependencyModId().equals("embedded"))
                .findFirst().orElseThrow();
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.PROVIDER_PRESENT, dependency.status());
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.MATCH, dependency.providerRangeStatus());
        assertTrue(dependency.providerArchivePaths().getFirst().endsWith("embedded-mod.jar"));
        assertEquals("2.1.0", dependency.providerVersions().getFirst());

        var targetRange = graph.targetRangeFindings().stream()
                .filter(finding -> finding.declaringModId().equals("alpha")).findFirst().orElseThrow();
        assertEquals("[1.20.1,1.21)", targetRange.declaredRange());
        assertEquals("1.21.1", targetRange.targetVersion());
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.OUTSIDE_RANGE, targetRange.status());
        assertTrue(targetRange.explanation().contains("metadata-range translation"));
    }

    @Test
    void resolvesActualTopLevelAndNestedInstalledProviderVersions() throws IOException {
        Path mods = Files.createDirectories(tempDir.resolve("mods"));
        Path gecko = mods.resolve("geckolib.jar");
        Files.write(gecko, zip(Map.of("META-INF/neoforge.mods.toml",
                bytes(neoForgeModToml("geckolib", "4.9.3")))));

        byte[] nestedProvider = zip(Map.of("META-INF/neoforge.mods.toml",
                bytes(neoForgeModToml("nesteddep", "2.4.0"))));
        Map<String, byte[]> providerEntries = new LinkedHashMap<>();
        providerEntries.put("META-INF/neoforge.mods.toml", bytes(neoForgeModToml("provider_container", "1.0.0")));
        providerEntries.put("META-INF/jarjar/metadata.json",
                bytes(jarJarMetadata("META-INF/jarjar/nesteddep.jar", "com.test", "nesteddep", "[2.0,3.0)", "2.4.0")));
        providerEntries.put("META-INF/jarjar/nesteddep.jar", nestedProvider);
        Files.write(mods.resolve("provider-container.jar"), zip(providerEntries));

        Path source = writeSourceJar("dependencies.jar",
                forgeModJar("alpha", "1.0.0", List.of(
                        dep("minecraft", "[1.20.1,)", true),
                        dep("forge", "[47,)", true),
                        dep("geckolib", "[4.0,)", true),
                        dep("nesteddep", "[2.0,3.0)", true),
                        dep("missing_required", "[1.0,)", true),
                        dep("missing_optional", "*", false))),
                Map.of());
        Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(source);
        assertNotNull(info);

        Forge1201ArchiveGraph.Result graph = Forge1201ArchiveGraph.analyze(
                source, info, List.of(mods), "1.21.1");

        var geckoEdge = edge(graph, "geckolib");
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.PROVIDER_PRESENT, geckoEdge.status());
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.MATCH, geckoEdge.providerRangeStatus());
        assertTrue(geckoEdge.providerArchivePaths().getFirst().endsWith("geckolib.jar"));
        assertEquals("4.9.3", geckoEdge.providerVersions().getFirst());

        var nestedEdge = edge(graph, "nesteddep");
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.PROVIDER_PRESENT, nestedEdge.status());
        assertEquals("2.4.0", nestedEdge.providerVersions().getFirst());
        assertTrue(nestedEdge.providerArchivePaths().getFirst().contains("provider-container.jar!/"));

        assertEquals(Forge1201ArchiveGraph.DependencyStatus.REQUIRED_MISSING, edge(graph, "missing_required").status());
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.OPTIONAL_MISSING, edge(graph, "missing_optional").status());
        assertTrue(graph.providers().stream().anyMatch(provider ->
                provider.modId().equals("geckolib")
                        && provider.providerKind() == Forge1201ArchiveGraph.ProviderKind.INSTALLED_TOP_LEVEL_NEOFORGE));
        assertTrue(graph.archives().stream().anyMatch(archive ->
                archive.archivePath().endsWith("geckolib.jar")
                        && archive.origin() == Forge1201ArchiveGraph.ArchiveOrigin.INSTALLED_TOP_LEVEL
                        && archive.sha256().matches("[0-9a-f]{64}")));
        assertTrue(graph.providers().stream().anyMatch(provider ->
                provider.modId().equals("nesteddep")
                        && provider.providerKind() == Forge1201ArchiveGraph.ProviderKind.INSTALLED_NESTED_NEOFORGE));
    }

    @Test
    void distinguishesAmbiguousProvidersAndVersionMismatchesAcrossLoaderKinds() throws IOException {
        Path mods = Files.createDirectories(tempDir.resolve("mixed-mods"));
        Files.write(mods.resolve("duplicate-one.jar"), zip(Map.of("META-INF/neoforge.mods.toml",
                bytes(neoForgeModToml("dupeprovider", "1.2.0")))));
        Files.write(mods.resolve("duplicate-two.jar"), zip(Map.of("META-INF/neoforge.mods.toml",
                bytes(neoForgeModToml("dupeprovider", "1.3.0")))));
        Files.write(mods.resolve("legacy-provider.jar"), forgeModJar("legacyprovider", "2.7.0", List.of(
                dep("minecraft", "[1.20.1,)", true), dep("forge", "[47,)", true))));
        Files.write(mods.resolve("too-old.jar"), zip(Map.of("META-INF/neoforge.mods.toml",
                bytes(neoForgeModToml("versioned", "1.5.0")))));

        Path source = writeSourceJar("provider-ambiguity.jar",
                forgeModJar("alpha", "1.0.0", List.of(
                        dep("minecraft", "[1.20.1,)", true),
                        dep("forge", "[47,)", true),
                        dep("dupeprovider", "[1.0,2.0)", true),
                        dep("legacyprovider", "[2.0,3.0)", true),
                        dep("versioned", "[3.0,4.0)", true))),
                Map.of());
        Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(source);
        assertNotNull(info);

        Forge1201ArchiveGraph.Result graph = Forge1201ArchiveGraph.analyze(
                source, info, List.of(mods), "1.21.1");

        var ambiguous = edge(graph, "dupeprovider");
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.PROVIDER_AMBIGUOUS, ambiguous.status());
        assertEquals(List.of("1.2.0", "1.3.0"), ambiguous.providerVersions());
        assertEquals(2, ambiguous.providerArchivePaths().size());

        var legacy = edge(graph, "legacyprovider");
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.PROVIDER_PRESENT, legacy.status());
        assertTrue(graph.providers().stream().anyMatch(provider ->
                provider.modId().equals("legacyprovider")
                        && provider.providerKind()
                        == Forge1201ArchiveGraph.ProviderKind.INSTALLED_TOP_LEVEL_FORGE_1201));

        var mismatch = edge(graph, "versioned");
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.PROVIDER_VERSION_MISMATCH, mismatch.status());
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.OUTSIDE_RANGE, mismatch.providerRangeStatus());
        assertEquals(List.of("1.5.0"), mismatch.providerVersions());
    }

    @Test
    void reportsMissingCorruptTraversalDuplicateAndDepthLimitedArchives() throws IOException {
        byte[] repeated = zip(Map.of("META-INF/NOTICE.txt", bytes("same bytes")));
        byte[] corrupt = new byte[] {0x01, 0x02, 0x03, 0x04};
        byte[] deep = nestedJarChain(4);
        byte[] oversized = new byte[(int) Forge1201ArchiveGraph.MAX_NESTED_ARCHIVE_BYTES + 1];
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/jarjar/metadata.json", bytes(jarJarMetadata(
                "META-INF/jarjar/missing.jar", "com.test", "missing", "[1.0,)", "1.0.0",
                "../outside.jar", "com.test", "unsafe", "[1.0,)", "1.0.0")));
        entries.put("META-INF/jarjar/../../outside.jar", repeated);
        entries.put("META-INF/jarjar/corrupt.jar", corrupt);
        entries.put("META-INF/jarjar/duplicate-a.jar", repeated);
        entries.put("META-INF/jarjar/duplicate-b.jar", repeated);
        entries.put("META-INF/jarjar/deep1.jar", deep);
        entries.put("META-INF/jarjar/oversized.jar", oversized);
        Path source = writeSourceJar("guarded.jar",
                forgeModJar("alpha", "1.0.0", List.of(
                        dep("minecraft", "[1.20.1,)", true), dep("forge", "[47,)", true))),
                entries);
        Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(source);
        assertNotNull(info);

        Forge1201ArchiveGraph.Result graph = Forge1201ArchiveGraph.analyze(
                source, info, List.of(), "1.21.1");

        assertKind(graph, "outside.jar", Forge1201ArchiveGraph.ArchiveKind.UNSAFE_PATH);
        assertKind(graph, "missing.jar", Forge1201ArchiveGraph.ArchiveKind.MISSING_DECLARED_ARCHIVE);
        assertKind(graph, "corrupt.jar", Forge1201ArchiveGraph.ArchiveKind.CORRUPT_ARCHIVE);
        assertKind(graph, "oversized.jar", Forge1201ArchiveGraph.ArchiveKind.SIZE_LIMIT);
        assertTrue(graph.archives().stream().anyMatch(archive ->
                archive.kind() == Forge1201ArchiveGraph.ArchiveKind.DUPLICATE_CONTENT_SKIPPED));
        assertTrue(graph.archives().stream().anyMatch(archive ->
                archive.kind() == Forge1201ArchiveGraph.ArchiveKind.DEPTH_LIMIT));
    }

    @Test
    void reportsNestedArchiveCountLimit() throws IOException {
        byte[] library = zip(Map.of("META-INF/NOTICE.txt", bytes("library")));
        Map<String, byte[]> nested = new LinkedHashMap<>();
        for (int index = 0; index < Forge1201ArchiveGraph.MAX_NESTED_ARCHIVES + 1; index++) {
            nested.put("META-INF/jarjar/archive-" + index + ".jar", library);
        }
        Path source = writeSourceJar("archive-count-limit.jar",
                forgeModJar("alpha", "1.0.0", List.of(
                        dep("minecraft", "[1.20.1,)", true), dep("forge", "[47,)", true))),
                nested);
        Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(source);
        assertNotNull(info);

        Forge1201ArchiveGraph.Result graph = Forge1201ArchiveGraph.analyze(
                source, info, List.of(), "1.21.1");

        assertTrue(graph.archives().stream().anyMatch(archive ->
                archive.kind() == Forge1201ArchiveGraph.ArchiveKind.ARCHIVE_COUNT_LIMIT));
        assertTrue(graph.warnings().stream().anyMatch(value ->
                value.contains("Maximum nested archive count")));
    }

    @Test
    void parsesNeoForgeDependencyTypesAndUsesTheirDistinctResolutionSemantics() throws IOException {
        Path mods = Files.createDirectories(tempDir.resolve("typed-neoforge-mods"));
        List<NeoDep> dependencies = List.of(
                neoDep("optional_missing", "[1.0,)", "optional"),
                neoDep("required_missing", "[1.0,)", "required"),
                neoDep("default_required", "*", null, false),
                neoDep("incompatible_present", "*", "incompatible"),
                neoDep("incompatible_absent", "*", "incompatible"),
                neoDep("discouraged_present", "*", "discouraged"),
                neoDep("discouraged_absent", "*", "discouraged"));
        Files.write(mods.resolve("native-owner.jar"), zip(Map.of("META-INF/neoforge.mods.toml",
                bytes(neoForgeModToml("nativeowner", "1.0.0", dependencies)))));
        Files.write(mods.resolve("incompatible-provider.jar"), zip(Map.of("META-INF/neoforge.mods.toml",
                bytes(neoForgeModToml("incompatible_present", "1.2.0")))));
        Files.write(mods.resolve("discouraged-provider.jar"), zip(Map.of("META-INF/neoforge.mods.toml",
                bytes(neoForgeModToml("discouraged_present", "1.2.0")))));

        Path source = writeSourceJar("neo-dependency-types.jar",
                forgeModJar("alpha", "1.0.0", List.of(
                        dep("minecraft", "[1.20.1,)", true), dep("forge", "[47,)", true))),
                Map.of());
        Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(source);
        assertNotNull(info);
        Forge1201ArchiveGraph.Result graph = Forge1201ArchiveGraph.analyze(
                source, info, List.of(mods), "1.21.1", "21.1.258");

        var optional = edge(graph, "nativeowner", "optional_missing");
        assertEquals(Forge1201JarInfo.DependencyType.OPTIONAL, optional.dependencyType());
        assertEquals(false, optional.mandatory());
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.OPTIONAL_MISSING, optional.status());

        var required = edge(graph, "nativeowner", "required_missing");
        assertEquals(Forge1201JarInfo.DependencyType.REQUIRED, required.dependencyType());
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.REQUIRED_MISSING, required.status());

        var defaultRequired = edge(graph, "nativeowner", "default_required");
        assertEquals(Forge1201JarInfo.DependencyType.REQUIRED, defaultRequired.dependencyType());
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.REQUIRED_MISSING, defaultRequired.status());

        var incompatiblePresent = edge(graph, "nativeowner", "incompatible_present");
        assertEquals(Forge1201JarInfo.DependencyType.INCOMPATIBLE, incompatiblePresent.dependencyType());
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.INCOMPATIBLE_PROVIDER_PRESENT,
                incompatiblePresent.status());

        var incompatibleAbsent = edge(graph, "nativeowner", "incompatible_absent");
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.INCOMPATIBLE_PROVIDER_ABSENT,
                incompatibleAbsent.status());

        var discouragedPresent = edge(graph, "nativeowner", "discouraged_present");
        assertEquals(Forge1201JarInfo.DependencyType.DISCOURAGED, discouragedPresent.dependencyType());
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.DISCOURAGED_PROVIDER_PRESENT,
                discouragedPresent.status());

        var discouragedAbsent = edge(graph, "nativeowner", "discouraged_absent");
        assertEquals(Forge1201ArchiveGraph.DependencyStatus.DISCOURAGED_PROVIDER_ABSENT,
                discouragedAbsent.status());
    }

    @Test
    void evaluatesNeoForgeHostDependencyAgainstTheSelectedLoaderVersion() throws IOException {
        String[][] cases = {
                {"[21.1.150,)", "MATCH", "HOST_BRIDGE"},
                {"[21.1.259,)", "OUTSIDE_RANGE", "HOST_VERSION_MISMATCH"},
                {"[21.0.110-beta,)", "UNPARSEABLE", "HOST_BRIDGE"}
        };
        for (int i = 0; i < cases.length; i++) {
            Path source = writeSourceJar("neoforge-host-range-" + i + ".jar",
                    forgeModJar("alpha", "1.0.0", List.of(
                            dep("minecraft", "[1.20.1,)", true),
                            dep("forge", "[47,)", true),
                            dep("neoforge", cases[i][0], true))),
                    Map.of());
            Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(source);
            assertNotNull(info);

            Forge1201ArchiveGraph.Result graph = Forge1201ArchiveGraph.analyze(
                    source, info, List.of(), "1.21.1", "21.1.258");
            var edge = edge(graph, "alpha", "neoforge");
            assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.valueOf(cases[i][1]),
                    edge.providerRangeStatus());
            assertEquals(Forge1201ArchiveGraph.DependencyStatus.valueOf(cases[i][2]), edge.status());
            assertEquals(List.of("21.1.258"), edge.providerVersions());
            assertTrue(edge.providerArchivePaths().isEmpty());
        }
    }

    @Test
    void evaluatesNumericMavenStyleRangesConservatively() {
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.MATCH,
                Forge1201ArchiveGraph.evaluateVersionRange("[4.0,)", "4.9.3"));
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.OUTSIDE_RANGE,
                Forge1201ArchiveGraph.evaluateVersionRange("[1.20.1,1.21)", "1.21.1"));
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.MATCH,
                Forge1201ArchiveGraph.evaluateVersionRange("[1.21.1]", "1.21.1"));
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.MATCH,
                Forge1201ArchiveGraph.evaluateVersionRange("[1.0,2.0),[3.0,4.0)", "3.5.0"));
        assertEquals(Forge1201ArchiveGraph.VersionRangeStatus.UNPARSEABLE,
                Forge1201ArchiveGraph.evaluateVersionRange("[4.0,)", "4.9.3-beta"));
    }

    private static Forge1201ArchiveGraph.DependencyEdge edge(
            Forge1201ArchiveGraph.Result graph, String dependencyId) {
        return edge(graph, "alpha", dependencyId);
    }

    private static Forge1201ArchiveGraph.DependencyEdge edge(
            Forge1201ArchiveGraph.Result graph, String declaringModId, String dependencyId) {
        return graph.dependencyEdges().stream()
                .filter(value -> value.declaringModId().equals(declaringModId)
                        && value.dependencyModId().equals(dependencyId))
                .findFirst().orElseThrow();
    }

    private static void assertKind(
            Forge1201ArchiveGraph.Result graph, String archiveName, Forge1201ArchiveGraph.ArchiveKind kind) {
        assertTrue(graph.archives().stream().anyMatch(archive ->
                archive.archivePath().endsWith(archiveName) && archive.kind() == kind),
                "Expected " + archiveName + " to have kind " + kind + "; found " + graph.archives());
    }

    private Path writeSourceJar(String name, byte[] rootJar, Map<String, byte[]> extraEntries) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/mods.toml", bytes(forgeModToml("alpha", "1.0.0", List.of(
                dep("minecraft", "[1.20.1,)", true), dep("forge", "[47,)", true)))));
        entries.put("placeholder.bin", bytes("placeholder"));
        entries.putAll(extraEntries);
        // The caller supplies the source root metadata and any embedded archives.
        entries.put("META-INF/mods.toml", extractMetadata(rootJar));
        Path path = tempDir.resolve(name);
        Files.write(path, zip(entries));
        return path;
    }

    private static byte[] extractMetadata(byte[] jarBytes) throws IOException {
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("META-INF/mods.toml")) return zip.readAllBytes();
            }
        }
        throw new IOException("Fixture root JAR has no mods.toml");
    }

    private static byte[] nestedJarChain(int nestedChildren) throws IOException {
        if (nestedChildren <= 0) return zip(Map.of("META-INF/NOTICE.txt", bytes("deep")));
        return zip(Map.of("META-INF/jarjar/child.jar", nestedJarChain(nestedChildren - 1)));
    }

    private static byte[] forgeModJar(String modId, String version, List<Dep> dependencies) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/mods.toml", bytes(forgeModToml(modId, version, dependencies)));
        entries.put("META-INF/placeholder.txt", bytes("nested mod"));
        return zip(entries);
    }

    private static String forgeModToml(String modId, String version, List<Dep> dependencies) {
        StringBuilder toml = new StringBuilder()
                .append("modLoader=\"javafml\"\n")
                .append("loaderVersion=\"[47, )\"\n")
                .append("license=\"MIT\"\n\n")
                .append("[[mods]]\n")
                .append("modId=\"").append(modId).append("\"\n")
                .append("version=\"").append(version).append("\"\n")
                .append("displayName=\"").append(modId).append(" Mod\"\n");
        for (Dep dependency : dependencies) {
            toml.append("\n[[dependencies.").append(modId).append("]]\n")
                    .append("modId=\"").append(dependency.id()).append("\"\n")
                    .append("mandatory=").append(dependency.mandatory()).append('\n')
                    .append("versionRange=\"").append(dependency.range()).append("\"\n")
                    .append("ordering=\"AFTER\"\n")
                    .append("side=\"BOTH\"\n");
        }
        return toml.toString();
    }

    private static String neoForgeModToml(String modId, String version) {
        return neoForgeModToml(modId, version, List.of());
    }

    private static String neoForgeModToml(String modId, String version, List<NeoDep> dependencies) {
        StringBuilder toml = new StringBuilder()
                .append("modLoader=\"javafml\"\nloaderVersion=\"[4,)\"\nlicense=\"MIT\"\n\n")
                .append("[[mods]]\nmodId=\"").append(modId).append("\"\nversion=\"").append(version)
                .append("\"\ndisplayName=\"").append(modId).append("\"\n");
        for (NeoDep dependency : dependencies) {
            toml.append("\n[[dependencies.").append(modId).append("]]\n")
                    .append("modId=\"").append(dependency.id()).append("\"\n");
            if (dependency.type() != null) {
                toml.append("type=\"").append(dependency.type()).append("\"\n");
            }
            if (dependency.legacyMandatory() != null) {
                toml.append("mandatory=").append(dependency.legacyMandatory()).append('\n');
            }
            toml.append("versionRange=\"").append(dependency.range()).append("\"\n")
                    .append("ordering=\"NONE\"\nside=\"BOTH\"\n");
        }
        return toml.toString();
    }

    private static NeoDep neoDep(String id, String range, String type) {
        return neoDep(id, range, type, null);
    }

    private static NeoDep neoDep(String id, String range, String type, Boolean legacyMandatory) {
        return new NeoDep(id, range, type, legacyMandatory);
    }

    private static Dep dep(String id, String range, boolean mandatory) {
        return new Dep(id, range, mandatory);
    }

    private record Dep(String id, String range, boolean mandatory) {}
    private record NeoDep(String id, String range, String type, Boolean legacyMandatory) {}

    private static String jarJarMetadata(String... values) {
        StringBuilder json = new StringBuilder("{\"jars\":[");
        for (int i = 0; i < values.length; i += 5) {
            if (i > 0) json.append(',');
            json.append("{\"identifier\":{\"group\":\"").append(values[i + 1])
                    .append("\",\"artifact\":\"").append(values[i + 2])
                    .append("\"},\"version\":{\"range\":\"").append(values[i + 3])
                    .append("\",\"artifactVersion\":\"").append(values[i + 4])
                    .append("\"},\"path\":\"").append(values[i]).append("\"}");
        }
        return json.append("]}").toString();
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
