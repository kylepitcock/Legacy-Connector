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
import java.util.zip.CRC32;
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
    void includesExplicitMappingProfileFindingInUnifiedJsonAndTextReports() throws IOException {
        Path sourceJar = createForgeModJar("mapping-profile-report.jar", false);
        Path missingProfile = tempDir.resolve("missing-mapping-profile.json");

        var result = Forge1201PreflightAnalyzer.analyze(
                sourceJar, List.of(), List.of(), null, missingProfile);

        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_FILE_MISSING,
                result.mappingProfile().status());
        String json = Forge1201PreflightAnalyzer.toJson(result);
        String text = Forge1201PreflightAnalyzer.toText(result);
        assertTrue(json.contains("\"mappingProfile\""));
        assertTrue(text.contains("Mapping profile: PROFILE_FILE_MISSING"));
        assertTrue(text.contains("does not transform selectors"));
    }

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
        assertEquals("1.10", first.schemaVersion());
        assertEquals("1.10.0", first.analyzerVersion());
        assertEquals(Forge1201MixinAnalyzer.TargetIndexStatus.UNAVAILABLE_NO_TARGET_CLASSPATH,
                first.mixinAnalysis().targetIndexStatus());
        assertEquals(Forge1201MappingProfileValidator.ProfileStatus.PROFILE_NOT_SUPPLIED,
                first.mappingProfile().status());
        assertTrue(first.mixinAnalysis().memberAnnotations().isEmpty());
        assertTrue(first.mixinAnalysis().injectors().isEmpty());
        var manifestConfig = first.mixinAnalysis().configs().stream()
                .filter(finding -> finding.configPath().equals("mixins.alpha.json")).findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.ConfigStatus.PARSED, manifestConfig.status());
        assertEquals(List.of(
                Forge1201MixinAnalyzer.DiscoverySource.MANIFEST,
                Forge1201MixinAnalyzer.DiscoverySource.RESOURCE_HEURISTIC),
                manifestConfig.discoverySources());
        var heuristicConfig = first.mixinAnalysis().configs().stream()
                .filter(finding -> finding.configPath().equals("config/feature.json")).findFirst().orElseThrow();
        assertEquals(List.of(Forge1201MixinAnalyzer.DiscoverySource.RESOURCE_HEURISTIC),
                heuristicConfig.discoverySources());
        assertTrue(first.mixinAnalysis().mixinClasses().stream().allMatch(finding ->
                finding.status() == Forge1201MixinAnalyzer.MixinClassStatus.MISSING));
        assertEquals(List.of("alpha", "shared"), first.resourceNamespaces());
        assertTrue(first.jarInJarPaths().contains("META-INF/jarjar/metadata.json"));
        assertTrue(first.jarInJarPaths().contains("META-INF/jarjar/sample-library.jar"));
        assertEquals(1, first.classCount());
        assertEquals(1, first.outerClassScannedCount());
        assertEquals(1, first.nestedClassCount());
        assertEquals(1, first.nestedClassScannedCount());
        assertTrue(first.archiveGraph().classScanSummaries().stream().anyMatch(summary ->
                summary.archivePath().equals("source/sample.jar!/META-INF/jarjar/sample-nested.jar")
                        && summary.discoveredClassEntries() == 1
                        && summary.scannedClassEntries() == 1
                        && summary.skippedClassEntries() == 0));
        assertTrue(first.methodCallSites().stream().anyMatch(call ->
                call.callerClass().equals("nested/LibraryFeature")
                        && call.targetOwner().equals("net/minecraft/world/entity/Entity")
                        && call.targetName().equals("m_2147483647_")));
        assertTrue(first.normalizedMemberReferences().stream().anyMatch(reference ->
                reference.sourceSrgName().equals("m_2147483647_")
                        && reference.mappingStatus() == Forge1201ApiUsageAnalyzer.SrgMappingStatus.UNMAPPED
                        && reference.mojangName() == null));
        var generatedMethodMapping = org.kylepitcock.legacyconnector.loader.mapping.SrgMojangTable.load()
                .entrySet().stream().filter(entry -> entry.getKey().startsWith("m_"))
                .sorted(java.util.Map.Entry.comparingByKey()).findFirst().orElseThrow();
        assertTrue(first.normalizedMemberReferences().stream().anyMatch(reference ->
                reference.sourceSrgName().equals(generatedMethodMapping.getKey())
                        && reference.mojangName().equals(generatedMethodMapping.getValue())
                        && reference.mappingStatus() == Forge1201ApiUsageAnalyzer.SrgMappingStatus.MAPPED));
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
        assertTrue(first.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.INVOKEDYNAMIC_SITE));
        assertTrue(first.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.METHOD_HANDLE
                        && reference.context().equals("invokedynamic-bootstrap")));
        assertTrue(first.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.METHOD_HANDLE
                        && reference.context().equals("invokedynamic-argument-1")));
        assertTrue(first.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.METHOD_HANDLE
                        && reference.context().equals("ldc")));
        assertTrue(first.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.FIELD_HANDLE
                        && reference.context().equals("ldc")));
        assertTrue(first.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL));
        assertTrue(first.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.METHOD_TYPE));
        assertTrue(first.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CONSTANT_DYNAMIC));
        assertTrue(first.dynamicReferences().stream().allMatch(reference ->
                reference.archivePath().equals("source/sample.jar")
                        && reference.classEntryPath().equals("sample/AlphaMod.class")
                        && reference.instructionOrdinal() >= 0));

        assertEquals(Forge1201PreflightAnalyzer.toJson(first), Forge1201PreflightAnalyzer.toJson(second));
        assertTrue(Forge1201PreflightAnalyzer.toJson(first).contains("\"dynamicReferences\""));
        assertTrue(Forge1201PreflightAnalyzer.toJson(first).contains("\"memberAnnotations\""));
        assertTrue(Forge1201PreflightAnalyzer.toJson(first).contains("\"refmapResources\""));
        assertTrue(Forge1201PreflightAnalyzer.toText(first).contains("Mixin refmap resources ("));
        assertTrue(Forge1201PreflightAnalyzer.toText(first).contains("Dynamic / bootstrap references:"));
        assertTrue(Forge1201PreflightAnalyzer.toText(first).contains("Mixin member annotations (0):"));
        assertTrue(Forge1201PreflightAnalyzer.toText(first).contains("Mixin injectors (0):"));
        assertTrue(Forge1201PreflightAnalyzer.toJson(first).contains("injectors"));
        assertTrue(Forge1201PreflightAnalyzer.toText(first).contains("Nested class archive scans: 1"));
        assertTrue(Forge1201PreflightAnalyzer.toText(first).contains("Source SRG -> Minecraft 1.20.1 Mojang member names:"));
        assertTrue(Forge1201PreflightAnalyzer.toText(first).contains("not a compatibility approval"));
        assertTrue(first.limitations().stream().anyMatch(value ->
                value.contains("outer source JAR") && value.contains("source-nested Forge/library archives")));
        assertTrue(first.limitations().stream().anyMatch(value -> value.contains("API compatibility are not verified")));
    }

    @Test
    void resolvesMappedForgeMixinTargetsFromSuppliedCompileClasspathResources() throws IOException {
        Path sourceJar = tempDir.resolve("mixin-target.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("MixinConfigs", "mixins.integration.json");
        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(sourceJar))) {
            put(zip, "META-INF/MANIFEST.MF", manifestBytes.toByteArray());
            put(zip, "mixins.integration.json",
                    """
                    {"package":"sample.mixin","refmap":"sample.refmap.json","mixins":["ModListMixin"]}
                    """.getBytes(StandardCharsets.UTF_8));
            put(zip, "sample.refmap.json",
                    """
                    {
                      "mappings": {
                        "sample/mixin/ModListMixin": {
                          "Lnet/minecraftforge/fml/ModList;get()V": "Lx;get()V",
                          "Lnet/minecraftforge/fml/ModList;tick()V": "Lx;tick()V"
                        }
                      },
                      "data": {
                        "synthetic-context": {
                          "sample/mixin/ModListMixin": {
                            "Lnet/minecraftforge/fml/ModList;get()V": "Lx;func_1()V",
                            "Lnet/minecraftforge/fml/ModList;tick()V": "Lx;func_2()V"
                          }
                        }
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8));
            put(zip, "sample/mixin/ModListMixin.class",
                    mixinTargetClass("sample/mixin/ModListMixin", "net.minecraftforge.fml.ModList"));
        }

        Path targetClasses = Files.createDirectories(
                tempDir.resolve("target-compile-classpath/net/neoforged/fml"));
        Files.write(targetClasses.resolve("ModList.class"), plainTargetClass("net/neoforged/fml/ModList"));
        Path targetClasspathRoot = tempDir.resolve("target-compile-classpath");

        var result = Forge1201PreflightAnalyzer.analyze(
                sourceJar, List.of(), List.of(targetClasspathRoot), "synthetic-context");

        assertEquals(Forge1201MixinAnalyzer.TargetIndexStatus.AVAILABLE,
                result.mixinAnalysis().targetIndexStatus());
        assertEquals(1, result.mixinAnalysis().targets().size());
        var target = result.mixinAnalysis().targets().getFirst();
        assertEquals("net/neoforged/fml/ModList", target.targetInternalName());
        assertEquals(Forge1201MixinAnalyzer.TargetStatus.TARGET_CLASS_PRESENT, target.status());
        assertEquals(1, result.mixinAnalysis().injectors().size());
        assertEquals("synthetic-context", result.mixinAnalysis().requestedRefmapContextKey());
        assertEquals(1, result.mixinAnalysis().injectors().getFirst().slices().size());
        var sliceRefmapLookup = result.mixinAnalysis().refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.AT_TARGET
                        && value.sourceLocation().equals("slice[0].from.target"))
                .findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.ENTRY_FOUND, sliceRefmapLookup.status());
        assertEquals("Lx;func_2()V", sliceRefmapLookup.mappedReferenceValue());
        assertTrue(Forge1201PreflightAnalyzer.toJson(result).contains("\"slices\""));
        assertTrue(Forge1201PreflightAnalyzer.toJson(result).contains("\"mixinAnalysis\""));
        assertTrue(Forge1201PreflightAnalyzer.toText(result).contains("Mixin analysis (1 configs, 1 mixin classes, 1 target selectors)"));
        assertTrue(Forge1201PreflightAnalyzer.toText(result).contains("@Slice[0] id=preflight-slice"));
        assertTrue(Forge1201PreflightAnalyzer.toText(result).contains("@Slice.from declared=true"));
        assertTrue(Forge1201PreflightAnalyzer.toText(result).contains("Mixin refmap lookup correlation"));
        assertEquals(1, result.mixinAnalysis().refmapResources().size());
        var refmap = result.mixinAnalysis().refmapResources().getFirst();
        assertEquals("sample.refmap.json", refmap.resourcePath());
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.PARSED, refmap.status());
        assertEquals(List.of("synthetic-context"), refmap.contextKeys());
        assertTrue(refmap.declaredSizeBytes() > 0);
        assertEquals(refmap.inflatedSizeBytes(), refmap.declaredSizeBytes());
        assertTrue(refmap.compressedSizeBytes() >= 0);
        assertTrue(refmap.inflatedSizeBytes() > 0);
        assertTrue(Forge1201PreflightAnalyzer.toText(result).contains("sample.refmap.json"));
        assertTrue(Forge1201PreflightAnalyzer.toText(result).contains("synthetic-context"));
    }

    @Test
    void classRemapperCapturesStructuralReferencesAndNestedAnnotationValues() {
        var inventory = new Forge1201ApiUsageAnalyzer.Inventory();
        assertTrue(inventory.scanNestedClass("source/fixture.jar!/nested.jar",
                "fixture/StructuralReferences.class", structuralReferenceClass()));
        assertTrue(inventory.scanNestedClass("source/fixture.jar!/nested.jar",
                "fixture/RecordReferences.class", recordComponentReferenceClass()));

        var result = inventory.finish(null);
        List<String> references = result.referencedApiClasses();

        // Class headers, implemented interfaces, generic signatures and descriptors.
        assertTrue(references.contains("net/minecraft/world/entity/HeaderBase"));
        assertTrue(references.contains("net/minecraft/world/entity/HeaderInterface"));
        assertTrue(references.contains("net/minecraft/world/entity/GenericClassBound"));
        assertTrue(references.contains("net/minecraft/world/item/FieldDescriptorType"));
        assertTrue(references.contains("net/minecraft/world/entity/GenericFieldType"));
        assertTrue(references.contains("net/minecraft/world/entity/MethodParameterType"));
        assertTrue(references.contains("net/minecraft/world/entity/MethodReturnType"));
        assertTrue(references.contains("net/minecraft/world/entity/GenericMethodType"));
        assertTrue(references.contains("net/minecraft/world/entity/GenericMethodReturnType"));
        assertTrue(references.contains("net/minecraft/world/entity/DeclaredException"));
        assertTrue(references.contains("net/minecraft/world/entity/CatchException"));
        assertTrue(references.contains("net/minecraft/world/entity/FrameType"));
        assertTrue(references.contains("net/minecraft/world/entity/LocalVariableType"));
        assertTrue(references.contains("net/minecraft/world/entity/NewInstructionType"));
        assertTrue(references.contains("net/minecraft/world/item/ArrayInstructionType"));
        assertTrue(references.contains("net/minecraft/world/entity/RecordComponentType"));
        assertTrue(references.contains("net/minecraft/world/entity/RecordComponentGenericType"));
        assertTrue(references.contains("net/minecraft/world/item/ItemStack"));
        assertTrue(references.contains("net/minecraft/resources/ResourceLocation"));
        assertTrue(references.contains("net/minecraft/client/Minecraft"));
        assertTrue(references.contains("net/minecraft/client/ClassTypeMarker"));

        // Class, field, method, parameter, type and record-component annotation descriptors.
        assertTrue(references.contains("net/minecraft/client/ClientMarker"));
        assertTrue(references.contains("net/minecraft/client/ClassTypeMarker"));
        assertTrue(references.contains("net/minecraft/client/FieldMarker"));
        assertTrue(references.contains("net/minecraft/client/FieldTypeMarker"));
        assertTrue(references.contains("net/minecraft/client/MethodMarker"));
        assertTrue(references.contains("net/minecraft/client/ParameterMarker"));
        assertTrue(references.contains("net/minecraft/client/ReturnTypeMarker"));
        assertTrue(references.contains("net/minecraft/client/RecordMarker"));
        assertTrue(references.contains("net/minecraft/client/RecordTypeMarker"));
        assertTrue(references.contains("net/minecraft/client/InstructionTypeMarker"));
        assertTrue(references.contains("net/minecraft/client/TryCatchTypeMarker"));
        assertTrue(references.contains("net/minecraft/client/LocalVariableTypeMarker"));
        assertTrue(references.contains("net/minecraft/client/NestedClientMarker"));

        // Annotation class literals are separately inventoried with annotation provenance,
        // including nested annotations and arrays (instructionOrdinal = -1 means non-code context).
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.owner().equals("net/minecraft/world/item/ItemStack")
                        && reference.context().startsWith("class-annotation:")
                        && reference.instructionOrdinal() == -1));
        assertTrue(result.dynamicReferences().stream()
                .filter(reference -> reference.context().startsWith("class-annotation:"))
                .allMatch(reference -> reference.archivePath().equals("source/fixture.jar!/nested.jar")
                        && reference.classEntryPath().equals("fixture/StructuralReferences.class")));
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.owner().equals("net/minecraft/client/Minecraft")
                        && reference.context().contains("nested@")));
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.context().startsWith("field-annotation:")));
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.context().startsWith("method-annotation:")));
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.context().startsWith("parameter-annotation:")));
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.context().startsWith("record-component-annotation:")));
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.owner().equals("net/minecraft/world/item/InstructionAnnotationType")
                        && reference.context().startsWith("instruction-annotation:")
                        && reference.instructionOrdinal() == 0));
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.owner().equals("net/minecraft/world/entity/TryCatchAnnotationType")
                        && reference.context().startsWith("try-catch-annotation:")));
        assertTrue(result.dynamicReferences().stream().anyMatch(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CLASS_LITERAL
                        && reference.owner().equals("net/minecraft/world/entity/LocalVariableAnnotationType")
                        && reference.context().startsWith("local-variable-annotation:")));
        assertTrue(result.dynamicReferences().stream()
                .filter(reference -> reference.instructionOrdinal() == -1)
                .anyMatch(reference -> reference.display().contains("#<class-context>")
                        && reference.display().contains("non-code context")));
    }

    @Test
    void reportsDuplicateNestedClassEntryNamesAsAmbiguousAndSkipsAllCopies() throws IOException {
        byte[] classBytes = recordComponentReferenceClass();
        byte[] nestedArchive = rawZipWithDuplicateStoredEntry("fixture/Duplicate.class", classBytes);
        Path sourceJar = createForgeModJar("duplicate-class-entry.jar", false, nestedArchive);

        var result = Forge1201PreflightAnalyzer.analyze(sourceJar, List.of());

        assertEquals(2, result.nestedClassCount());
        assertEquals(0, result.nestedClassScannedCount());
        var summary = result.archiveGraph().classScanSummaries().stream()
                .filter(value -> value.archivePath().endsWith("META-INF/jarjar/sample-nested.jar"))
                .findFirst().orElseThrow();
        assertEquals(2, summary.discoveredClassEntries());
        assertEquals(0, summary.scannedClassEntries());
        assertEquals(2, summary.skippedClassEntries());
        assertTrue(summary.warnings().stream().anyMatch(value ->
                value.contains("fixture/Duplicate.class")
                        && value.contains("duplicated within the archive")));
        assertTrue(result.dynamicReferences().stream().noneMatch(reference ->
                reference.classEntryPath().equals("fixture/Duplicate.class")));
    }

    @Test
    void recordsDeepConstantDynamicNestingAndExplicitlyStopsAtTheDepthLimit() {
        var inventory = new Forge1201ApiUsageAnalyzer.Inventory();
        assertTrue(inventory.scanNestedClass("source/dynamic-fixture.jar!/nested.jar",
                "fixture/DeepDynamic.class", deeplyNestedConstantDynamicClass(17)));

        var result = inventory.finish(null);
        var dynamicRows = result.dynamicReferences();
        assertEquals(17, dynamicRows.stream().filter(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CONSTANT_DYNAMIC).count());
        assertEquals(1, dynamicRows.stream().filter(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.VALUE_DEPTH_LIMIT).count());
        var depthLimit = dynamicRows.stream().filter(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.VALUE_DEPTH_LIMIT)
                .findFirst().orElseThrow();
        assertTrue(depthLimit.context().contains("constant-argument-0"));
        assertEquals(17, depthLimit.context().split("constant-argument-0", -1).length - 1);
        assertEquals("source/dynamic-fixture.jar!/nested.jar", depthLimit.archivePath());
        assertEquals("fixture/DeepDynamic.class", depthLimit.classEntryPath());
        assertTrue(dynamicRows.stream().filter(reference ->
                reference.kind() == Forge1201ApiUsageAnalyzer.ApiDynamicReferenceKind.CONSTANT_DYNAMIC)
                .allMatch(reference -> reference.instructionOrdinal() == 0));
    }

    @Test
    void reportsOversizedNestedClassEntriesAsExplicitlySkipped() throws IOException {
        byte[] oversizedClass = new byte[(int) Forge1201ArchiveGraph.MAX_CLASS_ENTRY_BYTES + 1];
        Path sourceJar = createForgeModJar("oversized-nested.jar", false,
                nestedArchiveWithClassBytes(oversizedClass));

        var result = Forge1201PreflightAnalyzer.analyze(sourceJar, List.of());

        assertEquals(1, result.nestedClassCount());
        assertEquals(0, result.nestedClassScannedCount());
        var summary = result.archiveGraph().classScanSummaries().stream()
                .filter(value -> value.archivePath().endsWith("META-INF/jarjar/sample-nested.jar"))
                .findFirst().orElseThrow();
        assertEquals(1, summary.discoveredClassEntries());
        assertEquals(0, summary.scannedClassEntries());
        assertEquals(1, summary.skippedClassEntries());
        assertTrue(summary.warnings().stream().anyMatch(value -> value.contains("class payload exceeded")));
    }

    @Test
    void normalizesOnlyMinecraftSrgMemberNamesAndKeepsUnknownsExplicit() {
        var location = new Forge1201ApiUsageAnalyzer.ApiReferenceLocation(
                "source/example.jar", "example/UsesMinecraft.class");
        var mapped = Forge1201ApiUsageAnalyzer.normalizeSrgMemberReference(
                Forge1201ApiUsageAnalyzer.ApiMemberKind.METHOD,
                "net/minecraft/world/entity/Entity", "m_12345_", "()V", 2,
                List.of(location), java.util.Map.of("m_12345_", "getExample"));
        assertNotNull(mapped);
        assertEquals("getExample", mapped.mojangName());
        assertEquals(Forge1201ApiUsageAnalyzer.SrgMappingStatus.MAPPED, mapped.mappingStatus());
        assertEquals(List.of(location), mapped.sourceLocations());

        var unknown = Forge1201ApiUsageAnalyzer.normalizeSrgMemberReference(
                Forge1201ApiUsageAnalyzer.ApiMemberKind.FIELD,
                "net/minecraft/world/entity/Entity", "f_987654321_", "I", 1,
                List.of(location), java.util.Map.of());
        assertNotNull(unknown);
        assertEquals(Forge1201ApiUsageAnalyzer.SrgMappingStatus.UNMAPPED, unknown.mappingStatus());
        assertEquals("f_987654321_", unknown.sourceSrgName());
        assertEquals(null, unknown.mojangName());

        var thirdParty = Forge1201ApiUsageAnalyzer.normalizeSrgMemberReference(
                Forge1201ApiUsageAnalyzer.ApiMemberKind.METHOD,
                "com/example/OwnClass", "m_12345_", "()V", 1,
                List.of(location), java.util.Map.of("m_12345_", "getExample"));
        assertEquals(null, thirdParty);

        var wrongMemberKind = Forge1201ApiUsageAnalyzer.normalizeSrgMemberReference(
                Forge1201ApiUsageAnalyzer.ApiMemberKind.FIELD,
                "net/minecraft/world/entity/Entity", "m_12345_", "I", 1,
                List.of(location), java.util.Map.of("m_12345_", "getExample"));
        assertEquals(null, wrongMemberKind);
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
        return createForgeModJar(name, duplicateId, nestedLibraryJar());
    }

    private Path createForgeModJar(String name, boolean duplicateId, byte[] nestedArchive) throws IOException {
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
            put(zip, "META-INF/jarjar/sample-nested.jar", nestedArchive);
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

    private static byte[] nestedLibraryJar() throws IOException {
        return nestedArchiveWithClassBytes(nestedLibraryClass());
    }

    private static byte[] nestedArchiveWithClassBytes(byte[] classBytes) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            put(zip, "nested/LibraryFeature.class", classBytes);
        }
        return bytes.toByteArray();
    }

    private static byte[] plainTargetClass(String internalName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] mixinTargetClass(String internalName, String targetName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        AnnotationVisitor annotation = writer.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/Mixin;", true);
        AnnotationVisitor targets = annotation.visitArray("targets");
        targets.visit(null, targetName);
        targets.visitEnd();
        annotation.visitEnd();

        MethodVisitor handler = writer.visitMethod(Opcodes.ACC_PRIVATE, "onTick", "()V", null, null);
        AnnotationVisitor inject = handler.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/Inject;", false);
        AnnotationVisitor methods = inject.visitArray("method");
        methods.visit(null, "tick()V");
        methods.visitEnd();
        AnnotationVisitor slices = inject.visitArray("slice");
        AnnotationVisitor slice = slices.visitAnnotation(
                null, "Lorg/spongepowered/asm/mixin/injection/Slice;");
        slice.visit("id", "preflight-slice");
        AnnotationVisitor from = slice.visitAnnotation(
                "from", "Lorg/spongepowered/asm/mixin/injection/At;");
        from.visit("value", "INVOKE");
        from.visit("target", "L" + targetName.replace('.', '/') + ";tick()V");
        from.visitEnd();
        slice.visitEnd();
        slices.visitEnd();
        inject.visitEnd();
        handler.visitCode();
        handler.visitInsn(Opcodes.RETURN);
        handler.visitMaxs(0, 0);
        handler.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] nestedLibraryClass() {
        var generatedMethodMapping = org.kylepitcock.legacyconnector.loader.mapping.SrgMojangTable.load()
                .entrySet().stream().filter(entry -> entry.getKey().startsWith("m_"))
                .sorted(java.util.Map.Entry.comparingByKey()).findFirst().orElseThrow();
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "nested/LibraryFeature",
                null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "useMinecraft", "()V", null, null);
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/world/entity/Entity",
                generatedMethodMapping.getKey(), "()V", false);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/world/entity/Entity",
                "m_2147483647_", "()V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] structuralReferenceClass() {
        String headerBase = "net/minecraft/world/entity/HeaderBase";
        String headerInterface = "net/minecraft/world/entity/HeaderInterface";
        String fieldType = "net/minecraft/world/item/FieldDescriptorType";
        String methodParameter = "net/minecraft/world/entity/MethodParameterType";
        String methodReturn = "net/minecraft/world/entity/MethodReturnType";
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "fixture/StructuralReferences",
                "<T:Lnet/minecraft/world/entity/GenericClassBound;>L" + headerBase + ";L"
                        + headerInterface + ";", headerBase, new String[] {headerInterface});

        AnnotationVisitor classAnnotation = writer.visitAnnotation(
                "Lnet/minecraft/client/ClientMarker;", true);
        classAnnotation.visit("type", org.objectweb.asm.Type.getType("Lnet/minecraft/world/item/ItemStack;"));
        AnnotationVisitor nested = classAnnotation.visitAnnotation(
                "nested", "Lnet/minecraft/client/NestedClientMarker;");
        nested.visit("nestedType", org.objectweb.asm.Type.getType("Lnet/minecraft/client/Minecraft;"));
        nested.visitEnd();
        AnnotationVisitor types = classAnnotation.visitArray("types");
        types.visit(null, org.objectweb.asm.Type.getType("Lnet/minecraft/resources/ResourceLocation;"));
        types.visitEnd();
        classAnnotation.visitEnum("reason", "Lnet/minecraft/world/entity/Entity$RemovalReason;", "DISCARDED");
        classAnnotation.visitEnd();
        writer.visitTypeAnnotation(
                org.objectweb.asm.TypeReference.newSuperTypeReference(-1).getValue(),
                null, "Lnet/minecraft/client/ClassTypeMarker;", true).visitEnd();

        var field = writer.visitField(Opcodes.ACC_PRIVATE, "entity", "L" + fieldType + ";",
                "Ljava/util/List<Lnet/minecraft/world/entity/GenericFieldType;>;", null);
        AnnotationVisitor fieldAnnotation = field.visitAnnotation("Lnet/minecraft/client/FieldMarker;", true);
        fieldAnnotation.visit("type", org.objectweb.asm.Type.getType("Lnet/minecraft/world/item/ItemStack;"));
        fieldAnnotation.visitEnd();
        field.visitTypeAnnotation(org.objectweb.asm.TypeReference.newTypeReference(
                org.objectweb.asm.TypeReference.FIELD).getValue(),
                null, "Lnet/minecraft/client/FieldTypeMarker;", true).visitEnd();
        field.visitEnd();

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "resolve",
                "(L" + methodParameter + ";)L" + methodReturn + ";",
                "(Ljava/util/List<Lnet/minecraft/world/entity/GenericMethodType;>;)"
                        + "Lnet/minecraft/world/entity/GenericMethodReturnType;",
                new String[] {"net/minecraft/world/entity/DeclaredException"});
        AnnotationVisitor methodAnnotation = method.visitAnnotation("Lnet/minecraft/client/MethodMarker;", true);
        methodAnnotation.visit("type", org.objectweb.asm.Type.getType("Lnet/minecraft/client/Minecraft;"));
        methodAnnotation.visitEnd();
        AnnotationVisitor parameterAnnotation = method.visitParameterAnnotation(
                0, "Lnet/minecraft/client/ParameterMarker;", true);
        parameterAnnotation.visit("type", org.objectweb.asm.Type.getType("Lnet/minecraft/resources/ResourceLocation;"));
        parameterAnnotation.visitEnd();
        method.visitTypeAnnotation(org.objectweb.asm.TypeReference.newTypeReference(
                org.objectweb.asm.TypeReference.METHOD_RETURN).getValue(),
                null, "Lnet/minecraft/client/ReturnTypeMarker;", true).visitEnd();

        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end = new org.objectweb.asm.Label();
        org.objectweb.asm.Label handler = new org.objectweb.asm.Label();
        method.visitCode();
        method.visitTryCatchBlock(start, end, handler, "net/minecraft/world/entity/CatchException");
        AnnotationVisitor tryCatchAnnotation = method.visitTryCatchAnnotation(
                org.objectweb.asm.TypeReference.newTryCatchReference(0).getValue(),
                null, "Lnet/minecraft/client/TryCatchTypeMarker;", true);
        tryCatchAnnotation.visit("type",
                org.objectweb.asm.Type.getType("Lnet/minecraft/world/entity/TryCatchAnnotationType;"));
        tryCatchAnnotation.visitEnd();
        method.visitLabel(start);
        method.visitTypeInsn(Opcodes.NEW, "net/minecraft/world/entity/NewInstructionType");
        AnnotationVisitor instructionAnnotation = method.visitInsnAnnotation(
                org.objectweb.asm.TypeReference.newTypeReference(org.objectweb.asm.TypeReference.NEW).getValue(),
                null, "Lnet/minecraft/client/InstructionTypeMarker;", true);
        instructionAnnotation.visit("type",
                org.objectweb.asm.Type.getType("Lnet/minecraft/world/item/InstructionAnnotationType;"));
        instructionAnnotation.visitEnd();
        method.visitInsn(Opcodes.POP);
        method.visitMultiANewArrayInsn("[[Lnet/minecraft/world/item/ArrayInstructionType;", 2);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitLabel(end);
        method.visitInsn(Opcodes.ARETURN);
        method.visitLabel(handler);
        method.visitFrame(Opcodes.F_FULL, 2, new Object[] {"fixture/StructuralReferences",
                "net/minecraft/world/entity/FrameType"}, 1,
                new Object[] {"net/minecraft/world/entity/CatchException"});
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitLocalVariable("entity", "Lnet/minecraft/world/entity/LocalVariableType;",
                "Ljava/util/List<Lnet/minecraft/world/entity/LocalVariableGenericType;>;", start, end, 1);
        AnnotationVisitor localVariableAnnotation = method.visitLocalVariableAnnotation(
                org.objectweb.asm.TypeReference.newTypeReference(
                        org.objectweb.asm.TypeReference.LOCAL_VARIABLE).getValue(),
                null, new org.objectweb.asm.Label[] {start}, new org.objectweb.asm.Label[] {end},
                new int[] {1}, "Lnet/minecraft/client/LocalVariableTypeMarker;", true);
        localVariableAnnotation.visit("type",
                org.objectweb.asm.Type.getType("Lnet/minecraft/world/entity/LocalVariableAnnotationType;"));
        localVariableAnnotation.visitEnd();
        method.visitMaxs(2, 2);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] recordComponentReferenceClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_RECORD,
                "fixture/RecordReferences", null, "java/lang/Record", null);
        var component = writer.visitRecordComponent(
                "entity", "Lnet/minecraft/world/entity/RecordComponentType;",
                "Lnet/minecraft/world/entity/RecordComponentGenericType;");
        AnnotationVisitor annotation = component.visitAnnotation("Lnet/minecraft/client/RecordMarker;", true);
        annotation.visit("type", org.objectweb.asm.Type.getType("Lnet/minecraft/world/item/ItemStack;"));
        annotation.visitEnd();
        component.visitTypeAnnotation(org.objectweb.asm.TypeReference.newTypeReference(
                org.objectweb.asm.TypeReference.FIELD).getValue(),
                null, "Lnet/minecraft/client/RecordTypeMarker;", true).visitEnd();
        component.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] deeplyNestedConstantDynamicClass(int levels) {
        org.objectweb.asm.Handle bootstrap = new org.objectweb.asm.Handle(
                Opcodes.H_INVOKESTATIC,
                "net/minecraft/world/entity/Entity",
                "bootstrap",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;",
                false);
        Object value = org.objectweb.asm.Type.getType("Lnet/minecraft/world/item/ItemStack;");
        for (int level = levels - 1; level >= 0; level--) {
            value = new org.objectweb.asm.ConstantDynamic(
                    "nestedConstant" + level,
                    "Lnet/minecraft/world/entity/Entity;",
                    bootstrap,
                    value);
        }

        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "fixture/DeepDynamic", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "load", "()V", null, null);
        method.visitCode();
        method.visitLdcInsn(value);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] rawZipWithDuplicateStoredEntry(String entryName, byte[] entryBytes) {
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        byte[] name = entryName.getBytes(StandardCharsets.UTF_8);
        CRC32 crc = new CRC32();
        crc.update(entryBytes);
        int checksum = (int) crc.getValue();
        int[] localOffsets = new int[2];

        for (int index = 0; index < 2; index++) {
            localOffsets[index] = zip.size();
            writeLittleEndianInt(zip, 0x04034b50);
            writeLittleEndianShort(zip, 20);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianInt(zip, checksum);
            writeLittleEndianInt(zip, entryBytes.length);
            writeLittleEndianInt(zip, entryBytes.length);
            writeLittleEndianShort(zip, name.length);
            writeLittleEndianShort(zip, 0);
            zip.writeBytes(name);
            zip.writeBytes(entryBytes);
        }

        int centralDirectoryOffset = zip.size();
        for (int index = 0; index < 2; index++) {
            writeLittleEndianInt(zip, 0x02014b50);
            writeLittleEndianShort(zip, 20);
            writeLittleEndianShort(zip, 20);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianInt(zip, checksum);
            writeLittleEndianInt(zip, entryBytes.length);
            writeLittleEndianInt(zip, entryBytes.length);
            writeLittleEndianShort(zip, name.length);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianShort(zip, 0);
            writeLittleEndianInt(zip, 0);
            writeLittleEndianInt(zip, localOffsets[index]);
            zip.writeBytes(name);
        }
        int centralDirectorySize = zip.size() - centralDirectoryOffset;
        writeLittleEndianInt(zip, 0x06054b50);
        writeLittleEndianShort(zip, 0);
        writeLittleEndianShort(zip, 0);
        writeLittleEndianShort(zip, 2);
        writeLittleEndianShort(zip, 2);
        writeLittleEndianInt(zip, centralDirectorySize);
        writeLittleEndianInt(zip, centralDirectoryOffset);
        writeLittleEndianShort(zip, 0);
        return zip.toByteArray();
    }

    private static void writeLittleEndianShort(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >>> 8) & 0xff);
    }

    private static void writeLittleEndianInt(ByteArrayOutputStream output, int value) {
        output.write(value & 0xff);
        output.write((value >>> 8) & 0xff);
        output.write((value >>> 16) & 0xff);
        output.write((value >>> 24) & 0xff);
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
        org.objectweb.asm.Handle bootstrap = new org.objectweb.asm.Handle(
                Opcodes.H_INVOKESTATIC,
                "net/minecraft/world/entity/Entity",
                "bootstrap",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",
                false);
        method.visitLdcInsn(new org.objectweb.asm.Handle(
                Opcodes.H_GETFIELD, "net/minecraft/world/entity/Entity", "f_123_", "I", false));
        method.visitInsn(Opcodes.POP);
        method.visitLdcInsn(new org.objectweb.asm.Handle(
                Opcodes.H_INVOKEVIRTUAL, "net/minecraft/world/entity/Entity", "m_789_", "()V", false));
        method.visitInsn(Opcodes.POP);
        method.visitLdcInsn(org.objectweb.asm.Type.getType("Lnet/minecraft/world/entity/Entity;"));
        method.visitInsn(Opcodes.POP);
        method.visitLdcInsn(org.objectweb.asm.Type.getMethodType("()Lnet/minecraft/world/entity/Entity;"));
        method.visitInsn(Opcodes.POP);
        method.visitInvokeDynamicInsn(
                "createEntity",
                "()Ljava/lang/Object;",
                bootstrap,
                org.objectweb.asm.Type.getType("Lnet/minecraft/world/entity/Entity;"),
                new org.objectweb.asm.Handle(
                        Opcodes.H_INVOKEVIRTUAL,
                        "net/minecraft/world/entity/Entity",
                        "m_456_",
                        "()V",
                        false),
                new org.objectweb.asm.ConstantDynamic(
                        "entityConstant",
                        "Lnet/minecraft/world/entity/Entity;",
                        bootstrap,
                        org.objectweb.asm.Type.getType("Lnet/minecraft/world/entity/Entity;")));
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
