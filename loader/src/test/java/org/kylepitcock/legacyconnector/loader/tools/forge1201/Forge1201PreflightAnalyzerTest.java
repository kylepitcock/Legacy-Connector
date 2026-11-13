package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class Forge1201PreflightAnalyzerTest {
    @TempDir
    Path tempDir;

    @Test
    void buildsDeterministicInventoryAndPreservesDependencyEvidence() throws IOException {
        Path modsDirectory = Files.createDirectories(tempDir.resolve("installed-mods"));
        createNativeMod(modsDirectory.resolve("geckolib.jar"), "geckolib");
        Path sourceJar = createForgeModJar("sample.jar", false);

        var first = Forge1201PreflightAnalyzer.analyze(sourceJar, List.of(modsDirectory));
        var second = Forge1201PreflightAnalyzer.analyze(sourceJar, List.of(modsDirectory));

        assertEquals(Forge1201PreflightAnalyzer.MetadataStatus.RECOGNIZED_FORGE_1201, first.metadataStatus());
        assertEquals(Forge1201PreflightAnalyzer.AnalysisStatus.BYTECODE_INVENTORY_RAN_FOR_RECOGNIZED_OUTER_JAR,
                first.analysisStatus());
        assertEquals("[1.20.1,1.20.2)", first.forgeMetadata().minecraftVersionRange());
        assertEquals(List.of("alpha", "beta"), first.mods().stream()
                .map(Forge1201PreflightAnalyzer.ModMetadata::modId).toList());
        assertEquals("sample.CorePlugin", first.coreModClass());
        assertTrue(first.manifestAttributes().containsKey("MixinConfigs"));
        assertTrue(first.mixinConfigCandidates().stream().anyMatch(candidate ->
                candidate.path().equals("mixins.alpha.json")
                        && candidate.discoveredBy().contains("manifest")));
        assertTrue(first.mixinConfigCandidates().stream().anyMatch(candidate ->
                candidate.path().equals("config/feature.json")));
        assertEquals(List.of("alpha", "shared"), first.resourceNamespaces());
        assertTrue(first.jarInJarPaths().contains("META-INF/jarjar/metadata.json"));
        assertTrue(first.jarInJarPaths().contains("META-INF/jarjar/sample-library.jar"));
        assertEquals(1, first.classCount());
        assertTrue(first.sourceSha256().matches("[0-9a-f]{64}"));

        assertEquals(Forge1201DependencyResolver.Status.HOST_BRIDGE, dependency(first, "minecraft").status());
        assertEquals(Forge1201DependencyResolver.Status.HOST_BRIDGE, dependency(first, "forge").status());
        assertEquals(Forge1201DependencyResolver.Status.NATIVE_ID_PRESENT, dependency(first, "geckolib").status());
        assertEquals(Forge1201DependencyResolver.Status.REQUIRED_MISSING, dependency(first, "required_missing").status());
        assertEquals(Forge1201DependencyResolver.Status.OPTIONAL_MISSING, dependency(first, "optional_missing").status());

        var modListType = first.apiTypes().stream()
                .filter(type -> type.sourceInternalName().equals("net/minecraftforge/fml/ModList"))
                .findFirst().orElseThrow();
        assertEquals(Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE, modListType.classification());
        assertEquals("net/neoforged/fml/ModList", modListType.candidateTargetInternalName());
        assertTrue(first.methodCallSites().stream().anyMatch(call ->
                call.targetOwner().equals("net/minecraftforge/fml/ModList") && call.targetName().equals("get")));

        assertEquals(Forge1201PreflightAnalyzer.toJson(first), Forge1201PreflightAnalyzer.toJson(second));
        assertTrue(Forge1201PreflightAnalyzer.toText(first).contains("not a compatibility approval"));
        assertTrue(first.limitations().stream().anyMatch(value -> value.contains("outer JAR only")));
        assertTrue(first.limitations().stream().anyMatch(value -> value.contains("API compatibility are not verified")));
    }

    @Test
    void reportsUnsupportedTomlConstructsAndDuplicateModIds() throws IOException {
        Path jar = createForgeModJar("duplicate.jar", true);
        var result = Forge1201PreflightAnalyzer.analyze(jar, List.of());

        assertEquals(3, result.mods().size());
        assertEquals(List.of("alpha"), result.duplicateModIds());
        assertTrue(result.metadataWarnings().stream().anyMatch(value -> value.contains("array or inline table")));
        assertTrue(result.metadataWarnings().stream().anyMatch(value -> value.contains("multiline string")));
    }

    @Test
    void missingModsTomlStillProducesArchiveAndIdentityReport() throws IOException {
        Path jar = tempDir.resolve("unrelated.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            put(zip, "assets/example/textures/item.png", new byte[] {1, 2, 3});
            put(zip, "data/example/recipes/example.json", "{}".getBytes(StandardCharsets.UTF_8));
        }

        var result = Forge1201PreflightAnalyzer.analyze(jar, List.of());

        assertEquals(Forge1201PreflightAnalyzer.MetadataStatus.MISSING_MODS_TOML, result.metadataStatus());
        assertEquals(Forge1201PreflightAnalyzer.AnalysisStatus.BYTECODE_INVENTORY_SKIPPED_METADATA_NOT_RECOGNIZED,
                result.analysisStatus());
        assertEquals(List.of("example"), result.resourceNamespaces());
        assertEquals(2, result.archiveEntryCount());
        assertNotNull(result.sourceSha256());
        assertTrue(result.metadataWarnings().stream().anyMatch(value -> value.contains("missing")));
    }

    private static Forge1201PreflightAnalyzer.DependencyFinding dependency(
            Forge1201PreflightAnalyzer.PreflightResult result, String modId) {
        return result.dependencies().stream().filter(value -> value.modId().equals(modId)).findFirst().orElseThrow();
    }

    private Path createForgeModJar(String name, boolean duplicateId) throws IOException {
        Path jar = tempDir.resolve(name);
        String metadata = """
                modLoader="javafml"
                loaderVersion="[47.2,)"
                license="MIT"
                authors=["Example", "Author"]
                description='''Multi-line description starts here
                and continues on the next line
                '''

                [[mods]]
                modId="alpha"
                version="1.0.0"
                displayName="Alpha Mod"

                [[mods]]
                modId="beta"
                version="2.0.0"
                displayName="Beta Mod"
                """ + (duplicateId ? """

                [[mods]]
                modId="alpha"
                version="3.0.0"
                displayName="Duplicate Alpha"
                """ : "") + """

                [[dependencies.alpha]]
                modId="minecraft"
                mandatory=true
                versionRange="[1.20.1,1.20.2)"
                ordering="NONE"
                side="BOTH"

                [[dependencies.alpha]]
                modId="forge"
                mandatory=true
                versionRange="[47.3.0,)"
                ordering="NONE"
                side="BOTH"

                [[dependencies.alpha]]
                modId="geckolib"
                mandatory=true
                versionRange="[4.0,)"
                ordering="AFTER"
                side="BOTH"

                [[dependencies.alpha]]
                modId="required_missing"
                mandatory=true
                versionRange="*"
                ordering="AFTER"
                side="BOTH"

                [[dependencies.alpha]]
                modId="optional_missing"
                mandatory=false
                versionRange="*"
                ordering="NONE"
                side="CLIENT"
                """;

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("MixinConfigs", "mixins.alpha.json");
        attributes.putValue("FMLCorePlugin", "sample.CorePlugin");
        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            put(zip, "META-INF/MANIFEST.MF", manifestBytes.toByteArray());
            put(zip, "META-INF/mods.toml", metadata.getBytes(StandardCharsets.UTF_8));
            put(zip, "sample/AlphaMod.class", sampleModClass());
            put(zip, "mixins.alpha.json",
                    "{\"package\":\"sample.mixin\",\"mixins\":[\"ExampleMixin\"]}".getBytes(StandardCharsets.UTF_8));
            put(zip, "config/feature.json",
                    "{\"package\":\"sample.mixin\",\"mixins\":[\"OtherMixin\"]}".getBytes(StandardCharsets.UTF_8));
            put(zip, "assets/alpha/textures/item/example.png", new byte[] {1});
            put(zip, "data/shared/recipes/example.json", "{}".getBytes(StandardCharsets.UTF_8));
            put(zip, "META-INF/jarjar/metadata.json", "{}".getBytes(StandardCharsets.UTF_8));
            put(zip, "META-INF/jarjar/sample-library.jar", new byte[] {0, 1, 2});
        }
        return jar;
    }

    private static void createNativeMod(Path jar, String modId) throws IOException {
        String metadata = """
                modLoader="javafml"
                loaderVersion="[4,)"
                [[mods]]
                modId="%s"
                version="99.0.0"
                """.formatted(modId);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            put(zip, "META-INF/neoforge.mods.toml", metadata.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] sampleModClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/AlphaMod", null, "java/lang/Object", null);
        AnnotationVisitor mod = writer.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", true);
        mod.visit("value", "alpha");
        mod.visitEnd();
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "initialize", "()V", null, null);
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/ModList",
                "get", "()Lnet/minecraftforge/fml/ModList;", false);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void put(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(bytes);
        zip.closeEntry();
    }
}
