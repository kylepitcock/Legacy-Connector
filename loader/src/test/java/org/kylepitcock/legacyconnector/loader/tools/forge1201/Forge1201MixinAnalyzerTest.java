package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class Forge1201MixinAnalyzerTest {
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String OVERWRITE_DESC = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_DESC = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String INJECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String REDIRECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String MODIFY_ARG_DESC = "Lorg/spongepowered/asm/mixin/injection/ModifyArg;";
    private static final String MODIFY_ARGS_DESC = "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;";
    private static final String MODIFY_VARIABLE_DESC = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;";
    private static final String MODIFY_CONSTANT_DESC = "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;";
    private static final String AT_DESC = "Lorg/spongepowered/asm/mixin/injection/At;";
    private static final String SLICE_DESC = "Lorg/spongepowered/asm/mixin/injection/Slice;";
    private static final String DESC_DESC = "Lorg/spongepowered/asm/mixin/injection/Desc;";
    private static final String CONSTANT_DESC = "Lorg/spongepowered/asm/mixin/injection/Constant;";
    private static final String CONDITION_DESC = "Lorg/spongepowered/asm/mixin/injection/Constant$Condition;";
    private static final String SOURCE_ENTITY = "net/minecraft/world/entity/Entity";
    private static final String SOURCE_LIVING = "net/minecraft/world/entity/LivingEntity";

    @TempDir
    Path tempDir;


    @Test
    void correlatesExactOwnerScopedRefsAndRespectsContextTableFallbackRules() throws Exception {
        String configPath = "mixins.refmap-lookup.json";
        String mixinOwner = "com/example/mixins/RefmapMixin";
        String atReference = "L" + SOURCE_ENTITY + ";hurt(D)Z";
        String config = """
                {"package":"com.example.mixins","refmap":"lookup.refmap.json","mixins":["RefmapMixin"]}
                """;
        String refmap = """
                {
                  "mappings": {"com/example/mixins/RefmapMixin": {
                    "tick()V": "baseTick()V",
                    "Lnet/minecraft/world/entity/Entity;hurt(D)Z": "baseHurt(D)Z"
                  }},
                  "data": {
                    "ctx-present-miss": {"com/example/other/OtherMixin": {
                      "tick()V": "wrongOwnerTick()V",
                      "Lnet/minecraft/world/entity/Entity;hurt(D)Z": "wrongOwnerHurt(D)Z"
                    }},
                    "ctx-different": {"com/example/mixins/RefmapMixin": {
                      "tick()V": "contextTick()V",
                      "Lnet/minecraft/world/entity/Entity;hurt(D)Z": "contextHurt(D)Z"
                    }}
                  }
                }
                """;
        Path jar = writeJar("refmap-owner-context-lookup.jar", Map.of(
                configPath, config.getBytes(StandardCharsets.UTF_8),
                "lookup.refmap.json", refmap.getBytes(StandardCharsets.UTF_8),
                mixinOwner + ".class",
                refmapLookupMixinClass(mixinOwner, SOURCE_ENTITY, new String[] {"tick ()V"},
                        true, null, "INVOKE", atReference, new String[0])));

        var present = Forge1201MixinAnalyzer.analyze(jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(configPath,
                        Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(), Map.of(), "no-member-map", "ctx-present-miss");
        var method = present.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.INJECTOR_METHOD)
                .findFirst().orElseThrow();
        assertEquals(mixinOwner, method.mixinInternalName());
        assertEquals("tick ()V", method.rawReference());
        assertEquals("tick()V", method.normalizedReference());
        assertEquals(Forge1201MixinAnalyzer.RefmapContextBehavior.CONTEXT_TABLE_SELECTED, method.contextBehavior());
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.ENTRY_NOT_FOUND_PASSTHROUGH, method.status());
        assertNull(method.mappedReferenceValue());
        assertTrue(method.candidates().getFirst().matches().isEmpty());
        var at = present.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.AT_TARGET)
                .findFirst().orElseThrow();
        assertEquals(atReference, at.rawReference());
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.ENTRY_NOT_FOUND_PASSTHROUGH, at.status());
        assertTrue(present.injectors().getFirst().injectionPoints().getFirst().remap());

        var missingContext = Forge1201MixinAnalyzer.analyze(jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(configPath,
                        Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(), Map.of(), "no-member-map", "ctx-not-present");
        var fallback = missingContext.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.INJECTOR_METHOD)
                .findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.RefmapContextBehavior.CONTEXT_MISSING_DEFAULT_FALLBACK,
                fallback.contextBehavior());
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.ENTRY_FOUND, fallback.status());
        assertEquals("baseTick()V", fallback.mappedReferenceValue());
        assertEquals(Forge1201MixinAnalyzer.RefmapTableKind.BASE_MAPPINGS, fallback.candidates().getFirst().tableKind());

        var noContext = Forge1201MixinAnalyzer.analyze(jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(configPath,
                        Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(), Map.of(), "no-member-map");
        var unresolved = noContext.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.INJECTOR_METHOD)
                .findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.RefmapContextBehavior.CONTEXT_NOT_SELECTED, unresolved.contextBehavior());
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.CONTEXT_DEPENDENT_VALUES, unresolved.status());
        assertNull(unresolved.selectedContextKey());
        assertTrue(unresolved.candidates().stream().anyMatch(candidate ->
                "ctx-different".equals(candidate.contextKey())
                        && "contextTick()V".equals(candidate.mappedReferenceValue())));
    }

    @Test
    void distinguishesExplicitIdentityEntriesFromMissingKeysAndNeverSearchesOtherOwners() throws Exception {
        String configPath = "mixins.refmap-identity.json";
        String mixinOwner = "com/example/mixins/IdentityMixin";
        String refmap = """
                {"mappings": {
                  "com/example/mixins/IdentityMixin": {"tick()V": "tick()V"},
                  "com/example/other/OtherMixin": {"missing()V": "wrongOwner()V"}
                }}
                """;
        Path jar = writeJar("refmap-identity-and-miss.jar", Map.of(
                configPath, "{\"package\":\"com.example.mixins\",\"refmap\":\"identity.refmap.json\",\"mixins\":[\"IdentityMixin\"]}".getBytes(StandardCharsets.UTF_8),
                "identity.refmap.json", refmap.getBytes(StandardCharsets.UTF_8),
                mixinOwner + ".class", refmapLookupMixinClass(mixinOwner, SOURCE_ENTITY,
                        new String[] {"tick()V", "missing()V"}, true, null, "HEAD", null, new String[0])));
        var result = Forge1201MixinAnalyzer.analyze(jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(configPath,
                        Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(), Map.of(), "no-member-map", "unlisted-context");
        var tick = result.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.INJECTOR_METHOD
                        && "tick()V".equals(value.rawReference())).findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.EXPLICIT_IDENTITY_ENTRY, tick.status());
        assertEquals("tick()V", tick.mappedReferenceValue());
        var missing = result.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.INJECTOR_METHOD
                        && "missing()V".equals(value.rawReference())).findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.ENTRY_NOT_FOUND_PASSTHROUGH, missing.status());
        assertNull(missing.mappedReferenceValue());
        assertTrue(missing.candidates().getFirst().matches().isEmpty());
    }

    @Test
    void injectionPointRemapInheritsInjectorRemapAndHonorsExplicitAtOverride() throws Exception {
        String configPath = "mixins.refmap-remap.json";
        String mixinOwner = "com/example/mixins/RemapMixin";
        String refmap = """
                {"mappings": {"com/example/mixins/RemapMixin": {
                  "tick()V": "obfTick()V",
                  "Lnet/minecraft/world/entity/Entity;hurt(D)Z": "La;h(D)Z"
                }}}
                """;
        String config = "{\"package\":\"com.example.mixins\",\"refmap\":\"remap.refmap.json\",\"mixins\":[\"RemapMixin\"]}";
        var declarations = List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(configPath,
                Forge1201MixinAnalyzer.DiscoverySource.MANIFEST));
        Path inheritedJar = writeJar("refmap-remap-inherited.jar", Map.of(
                configPath, config.getBytes(StandardCharsets.UTF_8),
                "remap.refmap.json", refmap.getBytes(StandardCharsets.UTF_8),
                mixinOwner + ".class", refmapLookupMixinClass(mixinOwner, SOURCE_ENTITY,
                        new String[] {"tick()V"}, false, null, "INVOKE",
                        "L" + SOURCE_ENTITY + ";hurt(D)Z", new String[0])));
        var inherited = Forge1201MixinAnalyzer.analyze(inheritedJar, declarations,
                emptyTargetIndex(), Map.of(), "no-member-map", "missing-context");
        var inheritedAt = inherited.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.AT_TARGET)
                .findFirst().orElseThrow();
        assertFalse(inheritedAt.remap());
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.REMAP_DISABLED, inheritedAt.status());
        assertTrue(inherited.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.INJECTOR_METHOD)
                .allMatch(value -> value.status() == Forge1201MixinAnalyzer.RefmapLookupStatus.REMAP_DISABLED));

        Path overrideJar = writeJar("refmap-remap-override.jar", Map.of(
                configPath, config.getBytes(StandardCharsets.UTF_8),
                "remap.refmap.json", refmap.getBytes(StandardCharsets.UTF_8),
                mixinOwner + ".class", refmapLookupMixinClass(mixinOwner, SOURCE_ENTITY,
                        new String[] {"tick()V"}, false, true, "INVOKE",
                        "L" + SOURCE_ENTITY + ";hurt(D)Z", new String[0])));
        var override = Forge1201MixinAnalyzer.analyze(overrideJar, declarations,
                emptyTargetIndex(), Map.of(), "no-member-map", "missing-context");
        var at = override.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.AT_TARGET)
                .findFirst().orElseThrow();
        assertTrue(at.remap());
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.ENTRY_FOUND, at.status());
        assertEquals("La;h(D)Z", at.mappedReferenceValue());
    }

    @Test
    void inventoriesBothNewConstructorTargetAndArgsClassRefmapPaths() throws Exception {
        String configPath = "mixins.refmap-new.json";
        String mixinOwner = "com/example/mixins/NewMixin";
        String constructorTarget = "L" + SOURCE_ENTITY + ";<init>()V";
        String classReference = "L" + SOURCE_ENTITY + ";";
        String wrongClassReference = "Lcom/example/NotTheMappedClass;";
        String refmap = "{\"mappings\":{\"" + mixinOwner + "\":{"
                + "\"" + constructorTarget + "\":\"La;.<init>()V\","
                + "\"" + classReference + "\":\"La;\"}}}";
        String config = "{\"package\":\"com.example.mixins\",\"refmap\":\"new.refmap.json\",\"mixins\":[\"NewMixin\"]}";
        Path jar = writeJar("refmap-new-paths.jar", Map.of(
                configPath, config.getBytes(StandardCharsets.UTF_8),
                "new.refmap.json", refmap.getBytes(StandardCharsets.UTF_8),
                mixinOwner + ".class", refmapLookupMixinClass(mixinOwner, SOURCE_ENTITY,
                        new String[] {"create()V"}, true, null, "NEW", constructorTarget,
                        new String[] {"class=" + wrongClassReference, "class=" + classReference})));
        var result = Forge1201MixinAnalyzer.analyze(jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(configPath,
                        Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(), Map.of(), "no-member-map", "missing-context");
        var constructor = result.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.AT_TARGET)
                .findFirst().orElseThrow();
        assertEquals(constructorTarget, constructor.rawReference());
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.ENTRY_FOUND, constructor.status());
        var classLookup = result.refmapLookups().stream()
                .filter(value -> value.source() == Forge1201MixinAnalyzer.RefmapLookupSource.AT_NEW_CLASS_ARGUMENT)
                .findFirst().orElseThrow();
        assertEquals(classReference, classLookup.rawReference());
        assertEquals("at[0].args[1].class", classLookup.sourceLocation());
        assertEquals(Forge1201MixinAnalyzer.RefmapLookupStatus.ENTRY_FOUND, classLookup.status());
        assertEquals("La;", classLookup.mappedReferenceValue());
    }

    @Test
    void inventoriesBaseAndContextRefmapTablesAndPreservesWrapperMetadata() throws Exception {
        String firstConfig = """
                {"package":"com.example.mixins","refmap":"shared.refmap.json",
                 "refmapWrapper":"com.example.CustomMapper","mixins":[]}
                """;
        String secondConfig = """
                {"package":"com.example.other","refmap":"shared.refmap.json","mixins":[]}
                """;
        String refmap = """
                {
                  "mappings": {
                    "com/example/other/OtherMixin": {
                      "Lexample/Target;field:I": "La;f:I"
                    },
                    "com/example/mixins/TestMixin": {
                      "Lexample/Target;run()V": "La;a()V"
                    }
                  },
                  "data": {
                    "searge": {
                      "com/example/mixins/TestMixin": {
                        "Lexample/Target;run()V": "La;func_1()V"
                      }
                    },
                    "notch": {
                      "com/example/mixins/TestMixin": {
                        "Lexample/Target;run()V": "La;b()V"
                      }
                    }
                  },
                  "customMetadata": "retained as an unknown-field warning"
                }
                """;
        Map<String, byte[]> archive = new LinkedHashMap<>();
        archive.put("mixins.a.json", firstConfig.getBytes(StandardCharsets.UTF_8));
        archive.put("mixins.b.json", secondConfig.getBytes(StandardCharsets.UTF_8));
        archive.put("shared.refmap.json", refmap.getBytes(StandardCharsets.UTF_8));
        Path jar = writeJar("shared-refmap.jar", archive);

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.a.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST),
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.b.json", Forge1201MixinAnalyzer.DiscoverySource.RESOURCE_HEURISTIC)),
                null, Map.of(), "not-supplied");

        assertEquals(2, result.configs().size());
        assertEquals("com.example.CustomMapper", result.configs().getFirst().refmapWrapperClass());
        assertEquals(Forge1201MixinAnalyzer.RefmapSelection.EXPLICIT_RESOURCE,
                result.configs().getFirst().refmapSelection());
        assertEquals(Forge1201MixinAnalyzer.ResourceStatus.PRESENT_VALID_JSON,
                result.configs().getFirst().refmapStatus());

        assertEquals(1, result.refmapResources().size(),
                "Two configs sharing one path must share one parsed resource inventory.");
        var resource = result.refmapResources().getFirst();
        assertEquals("shared.refmap.json", resource.resourcePath());
        assertEquals(List.of("mixins.a.json", "mixins.b.json"), resource.configPaths());
        assertEquals(List.of(Forge1201MixinAnalyzer.RefmapResourceOrigin.EXPLICIT_CONFIG_FIELD),
                resource.origins());
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.PARSED, resource.status());
        assertEquals(refmap.getBytes(StandardCharsets.UTF_8).length, resource.inflatedSizeBytes());
        assertEquals(resource.inflatedSizeBytes(), resource.declaredSizeBytes());
        assertTrue(resource.compressedSizeBytes() >= 0);
        assertEquals(64, resource.sha256().length());
        assertTrue(resource.mappingsPresent());
        assertTrue(resource.dataPresent());
        assertEquals(List.of("notch", "searge"), resource.contextKeys());
        assertEquals(List.of("customMetadata"), resource.unknownTopLevelFields());
        assertEquals(3, resource.tables().size());
        assertEquals(Forge1201MixinAnalyzer.RefmapTableKind.BASE_MAPPINGS,
                resource.tables().getFirst().kind());
        assertEquals(2, resource.tables().getFirst().ownerBucketCount());
        assertEquals(2, resource.tables().getFirst().entryCount());
        assertEquals(Forge1201MixinAnalyzer.RefmapTableKind.CONTEXT, resource.tables().get(1).kind());
        assertEquals("notch", resource.tables().get(1).contextKey());
        assertEquals("La;b()V", resource.tables().get(1).entries().getFirst().mappedReferenceValue());
        assertEquals("searge", resource.tables().get(2).contextKey());
        assertTrue(resource.warnings().stream().anyMatch(warning -> warning.contains("Unknown top-level")));

        var secondRun = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.a.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST),
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.b.json", Forge1201MixinAnalyzer.DiscoverySource.RESOURCE_HEURISTIC)),
                null, Map.of(), "not-supplied");
        assertEquals(result.refmapResources(), secondRun.refmapResources());
    }

    @Test
    void distinguishesDefaultRefmapCandidateFromPluginDependentCandidate() throws Exception {
        Map<String, byte[]> archive = new LinkedHashMap<>();
        archive.put("mixins.default.json",
                "{\"package\":\"com.example.default\",\"mixins\":[]}".getBytes(StandardCharsets.UTF_8));
        archive.put("mixins.plugin.json",
                "{\"package\":\"com.example.plugin\",\"plugin\":\"com.example.DynamicPlugin\",\"mixins\":[]}"
                        .getBytes(StandardCharsets.UTF_8));
        archive.put(Forge1201MixinAnalyzer.DEFAULT_REFMAP_RESOURCE, "{}".getBytes(StandardCharsets.UTF_8));
        Path jar = writeJar("default-refmap.jar", archive);

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.default.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST),
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.plugin.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                null, Map.of(), "not-supplied");

        var defaultConfig = config(result, "mixins.default.json");
        var pluginConfig = config(result, "mixins.plugin.json");
        assertEquals(Forge1201MixinAnalyzer.RefmapSelection.DEFAULT_RESOURCE, defaultConfig.refmapSelection());
        assertEquals(Forge1201MixinAnalyzer.RefmapSelection.PLUGIN_DEPENDENT, pluginConfig.refmapSelection());
        assertTrue(pluginConfig.warnings().stream().anyMatch(warning ->
                warning.contains("non-authoritative static candidate")));
        assertEquals(Forge1201MixinAnalyzer.DEFAULT_REFMAP_RESOURCE, defaultConfig.refmapCandidatePath());
        assertEquals(Forge1201MixinAnalyzer.DEFAULT_REFMAP_RESOURCE, pluginConfig.refmapCandidatePath());
        assertNull(defaultConfig.refmapPath());
        assertNull(pluginConfig.refmapPath());
        assertEquals(Forge1201MixinAnalyzer.ResourceStatus.NOT_DECLARED, defaultConfig.refmapStatus());
        assertEquals(Forge1201MixinAnalyzer.ResourceStatus.NOT_DECLARED, pluginConfig.refmapStatus());

        assertEquals(1, result.refmapResources().size());
        var resource = result.refmapResources().getFirst();
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.EMPTY, resource.status());
        assertEquals(List.of("mixins.default.json", "mixins.plugin.json"), resource.configPaths());
        assertEquals(List.of(
                        Forge1201MixinAnalyzer.RefmapResourceOrigin.MIXIN_DEFAULT_CANDIDATE,
                        Forge1201MixinAnalyzer.RefmapResourceOrigin.PLUGIN_DEPENDENT_CANDIDATE),
                resource.origins());
    }

    @Test
    void reportsInvalidRefmapShapesDuplicateJsonMembersAndNonStringValues() throws Exception {
        Map<String, byte[]> archive = new LinkedHashMap<>();
        archive.put("mixins.shape.json",
                "{\"package\":\"com.example.shape\",\"refmap\":\"shape.refmap.json\",\"mixins\":[]}"
                        .getBytes(StandardCharsets.UTF_8));
        archive.put("mixins.duplicate.json",
                "{\"package\":\"com.example.duplicate\",\"refmap\":\"duplicate.refmap.json\",\"mixins\":[]}"
                        .getBytes(StandardCharsets.UTF_8));
        archive.put("mixins.value.json",
                "{\"package\":\"com.example.value\",\"refmap\":\"value.refmap.json\",\"mixins\":[]}"
                        .getBytes(StandardCharsets.UTF_8));
        archive.put("shape.refmap.json", "{\"mappings\":[]}".getBytes(StandardCharsets.UTF_8));
        archive.put("duplicate.refmap.json",
                "{\"mappings\":{},\"mappings\":{}}".getBytes(StandardCharsets.UTF_8));
        archive.put("value.refmap.json",
                "{\"mappings\":{\"com/example/Mixin\":{\"selector\":42}}}".getBytes(StandardCharsets.UTF_8));
        Path jar = writeJar("invalid-refmaps.jar", archive);

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.shape.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST),
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.duplicate.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST),
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "mixins.value.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                null, Map.of(), "not-supplied");

        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.UNSUPPORTED_SHAPE,
                result.refmapResources().stream()
                        .filter(finding -> finding.resourcePath().equals("shape.refmap.json"))
                        .findFirst().orElseThrow().status());
        assertEquals(Forge1201MixinAnalyzer.ResourceStatus.UNSUPPORTED_SHAPE,
                config(result, "mixins.shape.json").refmapStatus());
        var duplicate = result.refmapResources().stream()
                .filter(finding -> finding.resourcePath().equals("duplicate.refmap.json"))
                .findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.INVALID_JSON, duplicate.status());
        assertTrue(duplicate.warnings().stream().anyMatch(warning -> warning.contains("duplicate object member")));
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.UNSUPPORTED_SHAPE,
                result.refmapResources().stream()
                        .filter(finding -> finding.resourcePath().equals("value.refmap.json"))
                        .findFirst().orElseThrow().status());
    }

    @Test
    void enforcesAggregateRefmapInflatedByteBudgetAcrossUniqueResources() throws Exception {
        String exactLimitJson = "{}" + " ".repeat(Forge1201MixinAnalyzer.MAX_REFMAP_BYTES - 2);
        Map<String, byte[]> archive = new LinkedHashMap<>();
        List<Forge1201MixinAnalyzer.ConfigDeclaration> declarations = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            String configPath = "mixins.aggregate-" + i + ".json";
            String refmapPath = "refmap-" + i + ".json";
            archive.put(configPath,
                    ("{\"package\":\"com.example.aggregate" + i + "\",\"refmap\":\""
                            + refmapPath + "\",\"mixins\":[]}")
                            .getBytes(StandardCharsets.UTF_8));
            archive.put(refmapPath, exactLimitJson.getBytes(StandardCharsets.UTF_8));
            declarations.add(new Forge1201MixinAnalyzer.ConfigDeclaration(
                    configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST));
        }
        Path jar = writeJar("aggregate-refmap-budget.jar", archive);

        var result = Forge1201MixinAnalyzer.analyze(jar, declarations, null, Map.of(), "not-supplied");

        assertEquals(9, result.refmapResources().size());
        assertTrue(result.refmapResources().subList(0, 8).stream().allMatch(resource ->
                resource.status() == Forge1201MixinAnalyzer.RefmapResourceStatus.EMPTY
                        && resource.inflatedSizeBytes() == Forge1201MixinAnalyzer.MAX_REFMAP_BYTES));
        var ninth = result.refmapResources().getLast();
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.LIMIT_EXCEEDED, ninth.status());
        assertTrue(ninth.warnings().stream().anyMatch(warning ->
                warning.contains("Aggregate refmap inflated-byte budget was exhausted")));
    }

    @Test
    void enforcesPerResourceRefmapByteLimitAndRecordsConsumedBytes() throws Exception {
        String oversizedJson = "{\"mappings\":{}}" +
                " ".repeat(Forge1201MixinAnalyzer.MAX_REFMAP_BYTES + 1);
        Map<String, byte[]> archive = new LinkedHashMap<>();
        archive.put("mixins.oversized.json",
                "{\"package\":\"com.example.oversized\",\"refmap\":\"oversized.refmap.json\",\"mixins\":[]}"
                        .getBytes(StandardCharsets.UTF_8));
        archive.put("oversized.refmap.json", oversizedJson.getBytes(StandardCharsets.UTF_8));
        Path jar = writeJar("oversized-refmap.jar", archive);

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        "mixins.oversized.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                null, Map.of(), "not-supplied");

        var resource = result.refmapResources().getFirst();
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.LIMIT_EXCEEDED, resource.status());
        assertEquals(Forge1201MixinAnalyzer.MAX_REFMAP_BYTES + 1L, resource.inflatedSizeBytes());
        assertEquals(oversizedJson.getBytes(StandardCharsets.UTF_8).length, resource.declaredSizeBytes());
        assertTrue(resource.tables().isEmpty());
    }

    @Test
    void rejectsExcessiveRefmapJsonNestingBeforeTreeMaterialization() throws Exception {
        StringBuilder deeplyNested = new StringBuilder("{\"mappings\":");
        for (int i = 0; i <= Forge1201MixinAnalyzer.MAX_REFMAP_JSON_DEPTH; i++) deeplyNested.append('[');
        deeplyNested.append("\"leaf\"");
        for (int i = 0; i <= Forge1201MixinAnalyzer.MAX_REFMAP_JSON_DEPTH; i++) deeplyNested.append(']');
        deeplyNested.append('}');

        Map<String, byte[]> archive = new LinkedHashMap<>();
        archive.put("mixins.deep.json",
                "{\"package\":\"com.example.deep\",\"refmap\":\"deep.refmap.json\",\"mixins\":[]}"
                        .getBytes(StandardCharsets.UTF_8));
        archive.put("deep.refmap.json", deeplyNested.toString().getBytes(StandardCharsets.UTF_8));
        Path jar = writeJar("deep-refmap.json.jar", archive);

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        "mixins.deep.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                null, Map.of(), "not-supplied");

        var resource = result.refmapResources().getFirst();
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.INVALID_JSON, resource.status());
        assertTrue(resource.warnings().stream().anyMatch(warning -> warning.contains("nesting exceeds")));
    }

    @Test
    void preservesAllContextKeysButStopsParsingAtTheConfiguredContextLimit() throws Exception {
        StringBuilder data = new StringBuilder("{\"data\":{");
        for (int i = 0; i < Forge1201MixinAnalyzer.MAX_REFMAP_CONTEXTS_PER_RESOURCE + 1; i++) {
            if (i > 0) data.append(',');
            data.append('"').append(String.format(java.util.Locale.ROOT, "ctx%02d", i)).append("\":{}");
        }
        data.append("}}");
        Map<String, byte[]> archive = new LinkedHashMap<>();
        archive.put("mixins.context-limit.json",
                "{\"package\":\"com.example.contextlimit\",\"refmap\":\"contexts.refmap.json\",\"mixins\":[]}"
                        .getBytes(StandardCharsets.UTF_8));
        archive.put("contexts.refmap.json", data.toString().getBytes(StandardCharsets.UTF_8));
        Path jar = writeJar("context-limit.jar", archive);

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        "mixins.context-limit.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                null, Map.of(), "not-supplied");

        var resource = result.refmapResources().getFirst();
        assertEquals(Forge1201MixinAnalyzer.RefmapResourceStatus.LIMIT_EXCEEDED, resource.status());
        assertEquals(Forge1201MixinAnalyzer.MAX_REFMAP_CONTEXTS_PER_RESOURCE + 1, resource.contextKeys().size());
        assertEquals(Forge1201MixinAnalyzer.MAX_REFMAP_CONTEXTS_PER_RESOURCE, resource.tables().size());
        assertEquals("ctx00", resource.tables().getFirst().contextKey());
        assertEquals("ctx63", resource.tables().getLast().contextKey());
    }

    @Test
    void resolvesManifestAndHeuristicConfigsAndVisibleAndInvisibleMixinTargets() throws Exception {
        String configPath = "mixins.sample.json";
        String config = """
                {
                  "package": "com.example.mixins",
                  "required": true,
                  "minVersion": "0.8",
                  "compatibilityLevel": "JAVA_17",
                  "plugin": "com.example.Plugin",
                  "refmap": "mixins.sample.refmap.json",
                  "mixins": ["TestMixin"],
                  "client": ["MissingClient"]
                }
                """;
        Map<String, byte[]> archive = new LinkedHashMap<>();
        archive.put(configPath, config.getBytes(StandardCharsets.UTF_8));
        archive.put("mixins.sample.refmap.json", "{}".getBytes(StandardCharsets.UTF_8));
        archive.put("com/example/Plugin.class", plainClass("com/example/Plugin"));
        archive.put("com/example/mixins/TestMixin.class", mixinClass(
                "com/example/mixins/TestMixin",
                SOURCE_ENTITY,
                SOURCE_LIVING,
                true));

        Path jar = writeJar("visible-invisible.jar", archive);
        Map<String, byte[]> targetClasses = Map.of(
                SOURCE_ENTITY, plainClass(SOURCE_ENTITY),
                SOURCE_LIVING, plainClass(SOURCE_LIVING));
        List<String> targetRequests = new ArrayList<>();
        Forge1201TargetApiVerifier.TargetIndex index = Forge1201TargetApiVerifier.createIndex(name -> {
            targetRequests.add(name);
            byte[] bytes = targetClasses.get(name);
            return bytes == null ? null : new ByteArrayInputStream(bytes);
        });

        Forge1201MixinAnalyzer.Result result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST),
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                configPath, Forge1201MixinAnalyzer.DiscoverySource.RESOURCE_HEURISTIC)),
                index,
                Map.of(SOURCE_ENTITY, SOURCE_ENTITY, SOURCE_LIVING, SOURCE_LIVING),
                "synthetic-mc-class-map-v1");

        assertEquals(1, result.configs().size());
        var configFinding = result.configs().getFirst();
        assertEquals(Forge1201MixinAnalyzer.ConfigStatus.PARSED, configFinding.status());
        assertEquals(List.of(
                Forge1201MixinAnalyzer.DiscoverySource.MANIFEST,
                Forge1201MixinAnalyzer.DiscoverySource.RESOURCE_HEURISTIC),
                configFinding.discoverySources());
        assertEquals("com.example.mixins", configFinding.packageName());
        assertEquals(true, configFinding.required());
        assertEquals("0.8", configFinding.minimumVersion());
        assertEquals("JAVA_17", configFinding.compatibilityLevel());
        assertEquals(Forge1201MixinAnalyzer.PluginStatus.PRESENT_NOT_EXECUTED, configFinding.pluginStatus());
        assertEquals(Forge1201MixinAnalyzer.ResourceStatus.PRESENT_VALID_JSON, configFinding.refmapStatus());

        assertEquals(2, result.mixinClasses().size());
        assertTrue(result.mixinClasses().stream().anyMatch(finding ->
                finding.mixinClassName().equals("com.example.mixins.TestMixin")
                        && finding.status() == Forge1201MixinAnalyzer.MixinClassStatus.PRESENT
                        && finding.hasMixinAnnotation()));
        assertTrue(result.mixinClasses().stream().anyMatch(finding ->
                finding.mixinClassName().equals("MissingClient")
                        && finding.status() == Forge1201MixinAnalyzer.MixinClassStatus.MISSING));

        assertEquals(2, result.targets().size());
        assertTrue(result.targets().stream().allMatch(target ->
                target.status() == Forge1201MixinAnalyzer.TargetStatus.TARGET_CLASS_PRESENT
                        && target.pluginDependent()));
        assertTrue(result.targets().stream().anyMatch(target ->
                target.selectorForm() == Forge1201MixinAnalyzer.TargetSelectorForm.CLASS_LITERAL));
        assertTrue(result.targets().stream().anyMatch(target ->
                target.selectorForm() == Forge1201MixinAnalyzer.TargetSelectorForm.STRING_TARGET));
        assertEquals("synthetic-mc-class-map-v1", result.targets().getFirst().mappingVersion());
        assertFalse(targetRequests.contains("com/example/Plugin"));
    }


    @Test
    void inventoriesDescriptorAwareMixinMemberAnnotationsWithoutClaimingTargetPresence() throws Exception {
        String configPath = "mixins.members.json";
        Path jar = writeJar("member-annotations.jar", Map.of(
                configPath, """
                        {"package":"com.example.mixins","mixins":["MemberMixin"]}
                        """.getBytes(StandardCharsets.UTF_8),
                "com/example/mixins/MemberMixin.class",
                memberMixinClass("com/example/mixins/MemberMixin", SOURCE_ENTITY)));

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(),
                Map.of(),
                "no-member-mapping-supplied");

        assertEquals(11, result.memberAnnotations().size());
        assertTrue(result.memberAnnotations().stream().allMatch(finding ->
                finding.configPath().equals(configPath)
                        && finding.mixinClassName().equals("com.example.mixins.MemberMixin")
                        && finding.sourceTargetInternalNames().equals(List.of(SOURCE_ENTITY))));

        var shadowField = result.memberAnnotations().stream()
                .filter(finding -> finding.annotationKind() == Forge1201MixinAnalyzer.MemberAnnotationKind.SHADOW
                        && finding.elementKind() == Forge1201MixinAnalyzer.MemberElementKind.FIELD)
                .findFirst().orElseThrow();
        assertEquals("shadow$health", shadowField.sourceMemberName());
        assertEquals("D", shadowField.sourceMemberDescriptor());
        assertNull(shadowField.prefix());
        assertEquals(List.of("healthLegacy"), shadowField.aliases());
        assertEquals(List.of("shadow$health", "healthLegacy"), shadowField.targetMemberNameCandidates());
        assertTrue(shadowField.warnings().stream().anyMatch(warning -> warning.contains("prefixes only apply to shadow methods")));
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.INVALID_ANNOTATION_VALUE,
                shadowField.status());

        var shadowMethod = result.memberAnnotations().stream()
                .filter(finding -> finding.annotationKind() == Forge1201MixinAnalyzer.MemberAnnotationKind.SHADOW
                        && finding.elementKind() == Forge1201MixinAnalyzer.MemberElementKind.METHOD)
                .findFirst().orElseThrow();
        assertEquals("(I)V", shadowMethod.sourceMemberDescriptor());
        assertEquals(List.of("heal", "healLegacy"), shadowMethod.targetMemberNameCandidates());

        var overwrite = result.memberAnnotations().stream()
                .filter(finding -> finding.annotationKind() == Forge1201MixinAnalyzer.MemberAnnotationKind.OVERWRITE)
                .findFirst().orElseThrow();
        assertEquals("tick", overwrite.sourceMemberName());
        assertEquals("(I)V", overwrite.sourceMemberDescriptor());
        assertEquals(List.of("tick", "oldTick"), overwrite.targetMemberNameCandidates());
        assertEquals("@env(DEFAULT)", overwrite.constraints());
        assertFalse(overwrite.remap());
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.MEMBER_REMAP_DISABLED_UNVERIFIED,
                overwrite.status());

        var getter = result.memberAnnotations().stream()
                .filter(finding -> finding.annotationKind() == Forge1201MixinAnalyzer.MemberAnnotationKind.ACCESSOR
                        && finding.sourceMemberName().equals("getHealth"))
                .findFirst().orElseThrow();
        assertEquals("()D", getter.sourceMemberDescriptor());
        assertEquals("GETTER", getter.operation());
        assertEquals(List.of("health"), getter.targetMemberNameCandidates());

        var setter = result.memberAnnotations().stream()
                .filter(finding -> finding.annotationKind() == Forge1201MixinAnalyzer.MemberAnnotationKind.ACCESSOR
                        && finding.sourceMemberName().equals("setHealth"))
                .findFirst().orElseThrow();
        assertEquals("(D)V", setter.sourceMemberDescriptor());
        assertEquals("SETTER", setter.operation());
        assertEquals("health", setter.explicitValue());

        var invoker = result.memberAnnotations().stream()
                .filter(finding -> finding.annotationKind() == Forge1201MixinAnalyzer.MemberAnnotationKind.INVOKER)
                .findFirst().orElseThrow();
        assertEquals("callHeal", invoker.sourceMemberName());
        assertEquals("(I)V", invoker.sourceMemberDescriptor());
        assertEquals("METHOD", invoker.operation());
        assertEquals(List.of("heal"), invoker.targetMemberNameCandidates());

        var constructorInvoker = result.memberAnnotations().stream()
                .filter(finding -> finding.sourceMemberName().equals("newEntity"))
                .findFirst().orElseThrow();
        assertEquals("CONSTRUCTOR", constructorInvoker.operation());
        assertEquals(List.of("<init>"), constructorInvoker.targetMemberNameCandidates());

        var unresolvedAccessor = result.memberAnnotations().stream()
                .filter(finding -> finding.sourceMemberName().equals("fetchHealth"))
                .findFirst().orElseThrow();
        assertTrue(unresolvedAccessor.targetMemberNameCandidates().isEmpty());
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.INVALID_MEMBER_SELECTOR,
                unresolvedAccessor.status());

        var lowerCaseAccessor = result.memberAnnotations().stream()
                .filter(finding -> finding.sourceMemberName().equals("gethealth"))
                .findFirst().orElseThrow();
        assertTrue(lowerCaseAccessor.targetMemberNameCandidates().isEmpty());
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.INVALID_MEMBER_SELECTOR,
                lowerCaseAccessor.status());

        var unsupportedAccessor = result.memberAnnotations().stream()
                .filter(finding -> finding.sourceMemberName().equals("getHealthWithArgs"))
                .findFirst().orElseThrow();
        assertEquals("UNSUPPORTED_SHAPE", unsupportedAccessor.operation());
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.INVALID_ANNOTATION_VALUE,
                unsupportedAccessor.status());

        var concreteAccessor = result.memberAnnotations().stream()
                .filter(finding -> finding.sourceMemberName().equals("readHealthConcrete"))
                .findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.INVALID_ANNOTATION_VALUE,
                concreteAccessor.status());
        assertTrue(concreteAccessor.warnings().stream().anyMatch(warning -> warning.contains("must be abstract")));
    }


    @Test
    void inventoriesCoreInjectorHandlersAndDescriptorAwareTargetSelectors() throws Exception {
        String configPath = "mixins.injectors.json";
        Path jar = writeJar("injector-annotations.jar", Map.of(
                configPath, """
                        {"package":"com.example.mixins","mixins":["InjectorMixin"]}
                        """.getBytes(StandardCharsets.UTF_8),
                "com/example/mixins/InjectorMixin.class",
                injectorMixinClass("com/example/mixins/InjectorMixin", SOURCE_ENTITY)));

        var result = Forge1201MixinAnalyzer.analyze(jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(), Map.of(), "no-member-mapping-supplied");

        assertEquals(9, result.injectors().size());
        assertTrue(result.injectors().stream().allMatch(finding ->
                finding.configPath().equals(configPath)
                        && finding.mixinClassName().equals("com.example.mixins.InjectorMixin")
                        && finding.sourceTargetInternalNames().equals(List.of(SOURCE_ENTITY))));

        var inject = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("onTick")).findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.InjectorAnnotationKind.INJECT, inject.annotationKind());
        assertEquals("(L" + SOURCE_ENTITY + ";I)V", inject.sourceMemberDescriptor());
        assertEquals(2, inject.targetSelectors().size());
        assertEquals("tick", inject.targetSelectors().getFirst().memberName());
        assertEquals("()V", inject.targetSelectors().getFirst().descriptor());
        assertEquals("hurt", inject.targetSelectors().get(1).memberName());
        assertEquals(SOURCE_ENTITY, inject.targetSelectors().get(1).ownerInternalName());
        assertEquals("(D)Z", inject.targetSelectors().get(1).descriptor());
        assertEquals(1, inject.atAnnotationCount());
        assertEquals(1, inject.sliceAnnotationCount());
        assertEquals(1, inject.require());
        assertEquals(Boolean.TRUE, inject.cancellable());
        assertFalse(inject.remap());
        assertEquals(Forge1201MixinAnalyzer.InjectorResolutionStatus.INJECTOR_REMAP_DISABLED_UNVERIFIED,
                inject.status());

        var descHandler = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("redirectHurtDesc")).findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.InjectorAnnotationKind.REDIRECT, descHandler.annotationKind());
        var descSelector = descHandler.targetSelectors().stream()
                .filter(selector -> selector.source() == Forge1201MixinAnalyzer.InjectorSelectorSource.DESC_ANNOTATION)
                .findFirst().orElseThrow();
        assertEquals(SOURCE_ENTITY, descSelector.ownerInternalName());
        assertEquals("hurt", descSelector.memberName());
        assertEquals("(D)Z", descSelector.descriptor());
        assertEquals(1, descSelector.minimumMatches());
        assertEquals(1, descSelector.maximumMatches());

        var overloaded = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("modifyOverload")).findFirst().orElseThrow();
        assertEquals(List.of("()V", "(I)V"), overloaded.targetSelectors().stream()
                .map(Forge1201MixinAnalyzer.InjectorTargetSelector::descriptor).toList());

        var wildcard = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("modifyWildcard")).findFirst().orElseThrow();
        assertEquals("tick", wildcard.targetSelectors().getFirst().memberName());
        assertEquals("ANY", wildcard.targetSelectors().getFirst().quantifier());
        assertEquals(2, wildcard.index());
        assertEquals(Boolean.TRUE, wildcard.argsOnly());

        var malformed = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("badSelector")).findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.InjectorResolutionStatus.INVALID_INJECTOR_SELECTOR,
                malformed.status());

        var field = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("invalidInjectorField")).findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.MemberElementKind.FIELD, field.elementKind());
        assertEquals(Forge1201MixinAnalyzer.InjectorResolutionStatus.INVALID_ANNOTATION_PLACEMENT,
                field.status());

        var modifyConstant = result.injectors().stream()
                .filter(f -> f.annotationKind() == Forge1201MixinAnalyzer.InjectorAnnotationKind.MODIFY_CONSTANT)
                .findFirst().orElseThrow();
        assertEquals(1, modifyConstant.sliceAnnotationCount());
        assertTrue(result.injectors().stream().anyMatch(f ->
                f.annotationKind() == Forge1201MixinAnalyzer.InjectorAnnotationKind.MODIFY_ARGS));
    }


    @Test
    void inventoriesDetailedAtSelectorsAndTypedModifyConstantDiscriminators() throws Exception {
        String configPath = "mixins.at-constants.json";
        Path jar = writeJar("at-constants.jar", Map.of(
                configPath, """
                        {"package":"com.example.mixins","mixins":["AtConstantMixin"]}
                        """.getBytes(StandardCharsets.UTF_8),
                "com/example/mixins/AtConstantMixin.class",
                atConstantMixinClass("com/example/mixins/AtConstantMixin", SOURCE_ENTITY)));

        var result = Forge1201MixinAnalyzer.analyze(jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(), Map.of(), "no-member-mapping-supplied");

        var atHandler = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("atMetadata"))
                .findFirst().orElseThrow();
        assertEquals(4, atHandler.injectionPoints().size());
        assertEquals(3, atHandler.sliceAnnotationCount());
        assertEquals(3, atHandler.slices().size());

        var explicitSlice = atHandler.slices().getFirst();
        assertEquals("segment", explicitSlice.id());
        assertEquals(Forge1201MixinAnalyzer.SliceFindingStatus.CAPTURED, explicitSlice.status());
        assertEquals(Forge1201MixinAnalyzer.SliceBoundaryStatus.EXPLICIT_AT_CAPTURED,
                explicitSlice.from().status());
        assertTrue(explicitSlice.from().declared());
        assertEquals("INVOKE", explicitSlice.from().at().value());
        assertEquals(SOURCE_ENTITY, explicitSlice.from().at().targetSelector().ownerInternalName());
        assertEquals("hurt", explicitSlice.from().at().targetSelector().memberName());
        assertEquals("(D)Z", explicitSlice.from().at().targetSelector().descriptor());
        assertEquals(1, explicitSlice.from().at().ordinal());
        assertEquals("AFTER", explicitSlice.from().at().shift());
        assertEquals("RETURN", explicitSlice.to().at().value());
        assertEquals(2, explicitSlice.to().at().ordinal());

        var defaultSlice = atHandler.slices().get(1);
        assertEquals("defaulted", defaultSlice.id());
        assertEquals(Forge1201MixinAnalyzer.SliceFindingStatus.CAPTURED, defaultSlice.status());
        assertFalse(defaultSlice.from().declared());
        assertEquals(Forge1201MixinAnalyzer.SliceBoundaryStatus.DEFAULT_AT_CAPTURED,
                defaultSlice.from().status());
        assertEquals("HEAD", defaultSlice.from().at().value());
        assertFalse(defaultSlice.to().declared());
        assertEquals("TAIL", defaultSlice.to().at().value());

        var unsupportedSlice = atHandler.slices().get(2);
        assertEquals("unsupported", unsupportedSlice.id());
        assertEquals(Forge1201MixinAnalyzer.SliceFindingStatus.BOUNDARY_REQUIRES_REVIEW,
                unsupportedSlice.status());
        assertEquals(Forge1201MixinAnalyzer.SliceBoundaryStatus.UNSUPPORTED_NESTED_ANNOTATION,
                unsupportedSlice.from().status());
        assertEquals("Lcom/example/CustomAt;", unsupportedSlice.from().annotationDescriptor());
        assertEquals("TAIL", unsupportedSlice.to().at().value());
        assertTrue(new com.google.gson.Gson().toJson(result).contains("\"slices\""));

        var fieldAt = atHandler.injectionPoints().getFirst();
        assertEquals("FIELD", fieldAt.value());
        assertEquals(Forge1201MixinAnalyzer.AtValueKind.BUILT_IN, fieldAt.valueKind());
        assertEquals("L" + SOURCE_ENTITY + ";health:D", fieldAt.rawTarget());
        assertEquals(SOURCE_ENTITY, fieldAt.targetSelector().ownerInternalName());
        assertEquals("health", fieldAt.targetSelector().memberName());
        assertEquals("D", fieldAt.targetSelector().descriptor());
        assertEquals(Forge1201MixinAnalyzer.AtTargetDescriptorKind.FIELD,
                fieldAt.targetSelector().descriptorKind());
        assertEquals(Forge1201MixinAnalyzer.AtTargetSelectorStatus.FIELD_DESCRIPTOR,
                fieldAt.targetSelector().status());
        assertEquals(Opcodes.GETFIELD, fieldAt.opcode());
        assertEquals(2, fieldAt.ordinal());
        assertEquals("BY", fieldAt.shift());
        assertEquals(-1, fieldAt.shiftBy());
        assertEquals("segment", fieldAt.sliceId());
        assertEquals(List.of("array=getter", "audit=fixture"), fieldAt.args());
        assertFalse(fieldAt.remap());
        assertEquals("healthLookup", fieldAt.id());

        var descAt = atHandler.injectionPoints().get(1);
        assertEquals("INVOKE", descAt.value());
        assertEquals(SOURCE_ENTITY, descAt.descSelector().ownerInternalName());
        assertEquals("hurt", descAt.descSelector().memberName());
        assertEquals("(D)Z", descAt.descSelector().descriptor());
        assertEquals(1, descAt.descSelector().minimumMatches());
        assertEquals(1, descAt.descSelector().maximumMatches());

        var headAt = atHandler.injectionPoints().get(2);
        assertEquals("HEAD", headAt.value());
        assertEquals(Forge1201MixinAnalyzer.AtTargetSelectorStatus.NOT_DECLARED,
                headAt.targetSelector().status());
        assertEquals(-1, headAt.opcode());
        assertEquals(-1, headAt.ordinal());
        assertEquals("NONE", headAt.shift());
        assertEquals(0, headAt.shiftBy());
        assertTrue(headAt.remap());

        var newAt = atHandler.injectionPoints().get(3);
        assertEquals("NEW", newAt.value());
        assertEquals(SOURCE_ENTITY, newAt.targetSelector().ownerInternalName());
        assertEquals(Forge1201MixinAnalyzer.AtTargetSelectorStatus.OWNER_ONLY,
                newAt.targetSelector().status());

        var constants = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("modifyTypedConstants"))
                .findFirst().orElseThrow().constants();
        assertEquals(7, constants.size());
        assertEquals(Integer.valueOf(7), constants.get(0).intValue());
        assertEquals(3, constants.get(0).ordinal());
        assertEquals("segment", constants.get(0).sliceId());
        assertEquals(List.of("GREATER_THAN_ZERO"), constants.get(0).expandZeroConditions());
        assertEquals(Boolean.TRUE, constants.get(0).log());
        assertEquals(Float.valueOf(1.25F), constants.get(1).floatValue());
        assertEquals(Long.valueOf(9L), constants.get(2).longValue());
        assertEquals(Double.valueOf(2.5D), constants.get(3).doubleValue());
        assertEquals("hello", constants.get(4).stringValue());
        assertEquals("Lcom/example/Thing;", constants.get(5).classValueDescriptor());
        assertEquals(Boolean.TRUE, constants.get(6).nullValue());
        assertTrue(constants.stream().allMatch(constant -> constant.declared()
                && constant.status() == Forge1201MixinAnalyzer.ConstantFindingStatus.EXPLICIT_TYPED_DISCRIMINATOR));

        var implicit = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("modifyImplicitConstant"))
                .findFirst().orElseThrow().constants().getFirst();
        assertFalse(implicit.declared());
        assertEquals(Forge1201MixinAnalyzer.ConstantFindingStatus.MATCH_HANDLER_RETURN_TYPE, implicit.status());

        var conflict = result.injectors().stream()
                .filter(finding -> finding.sourceMemberName().equals("modifyConflictingConstant"))
                .findFirst().orElseThrow().constants().getFirst();
        assertEquals(Forge1201MixinAnalyzer.ConstantFindingStatus.CONFLICTING_DISCRIMINATORS, conflict.status());
    }

    private static byte[] atConstantMixinClass(String internalName, String targetInternalName) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                internalName, null, "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(MIXIN_DESC, true);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType(targetInternalName));
        targets.visitEnd();
        mixin.visitEnd();

        MethodVisitor atMethod = writer.visitMethod(Opcodes.ACC_PRIVATE, "atMetadata", "()V", null, null);
        AnnotationVisitor inject = atMethod.visitAnnotation(INJECT_DESC, false);
        AnnotationVisitor methods = inject.visitArray("method");
        methods.visit(null, "tick()V");
        methods.visitEnd();

        AnnotationVisitor atArray = inject.visitArray("at");
        AnnotationVisitor fieldAt = atArray.visitAnnotation(null, AT_DESC);
        fieldAt.visit("value", "FIELD");
        fieldAt.visit("target", "L" + targetInternalName + ";health:D");
        fieldAt.visit("opcode", Opcodes.GETFIELD);
        fieldAt.visit("ordinal", 2);
        fieldAt.visitEnum("shift", "Lorg/spongepowered/asm/mixin/injection/At$Shift;", "BY");
        fieldAt.visit("by", -1);
        fieldAt.visit("slice", "segment");
        fieldAt.visit("remap", false);
        fieldAt.visit("id", "healthLookup");
        AnnotationVisitor fieldArgs = fieldAt.visitArray("args");
        fieldArgs.visit(null, "array=getter");
        fieldArgs.visit(null, "audit=fixture");
        fieldArgs.visitEnd();
        fieldAt.visitEnd();

        AnnotationVisitor invokeAt = atArray.visitAnnotation(null, AT_DESC);
        invokeAt.visit("value", "INVOKE");
        AnnotationVisitor atDesc = invokeAt.visitAnnotation("desc", DESC_DESC);
        atDesc.visit("owner", Type.getObjectType(targetInternalName));
        atDesc.visit("value", "hurt");
        atDesc.visit("ret", Type.BOOLEAN_TYPE);
        AnnotationVisitor atDescArgs = atDesc.visitArray("args");
        atDescArgs.visit(null, Type.DOUBLE_TYPE);
        atDescArgs.visitEnd();
        atDesc.visit("min", 1);
        atDesc.visit("max", 1);
        atDesc.visitEnd();
        invokeAt.visitEnd();

        AnnotationVisitor headAt = atArray.visitAnnotation(null, AT_DESC);
        headAt.visit("value", "HEAD");
        headAt.visitEnd();

        AnnotationVisitor newAt = atArray.visitAnnotation(null, AT_DESC);
        newAt.visit("value", "NEW");
        newAt.visit("target", "L" + targetInternalName + ";");
        newAt.visitEnd();
        atArray.visitEnd();

        AnnotationVisitor slices = inject.visitArray("slice");
        AnnotationVisitor slice = slices.visitAnnotation(null, SLICE_DESC);
        slice.visit("id", "segment");
        AnnotationVisitor from = slice.visitAnnotation("from", AT_DESC);
        from.visit("value", "INVOKE");
        from.visit("target", "L" + targetInternalName + ";hurt(D)Z");
        from.visit("ordinal", 1);
        from.visitEnum("shift", "Lorg/spongepowered/asm/mixin/injection/At$Shift;", "AFTER");
        from.visitEnd();
        AnnotationVisitor to = slice.visitAnnotation("to", AT_DESC);
        to.visit("value", "RETURN");
        to.visit("ordinal", 2);
        to.visitEnd();
        slice.visitEnd();

        AnnotationVisitor defaultSlice = slices.visitAnnotation(null, SLICE_DESC);
        defaultSlice.visit("id", "defaulted");
        defaultSlice.visitEnd();

        AnnotationVisitor unsupportedSlice = slices.visitAnnotation(null, SLICE_DESC);
        unsupportedSlice.visit("id", "unsupported");
        AnnotationVisitor unsupportedFrom = unsupportedSlice.visitAnnotation(
                "from", "Lcom/example/CustomAt;");
        unsupportedFrom.visitEnd();
        AnnotationVisitor explicitTail = unsupportedSlice.visitAnnotation("to", AT_DESC);
        explicitTail.visit("value", "TAIL");
        explicitTail.visitEnd();
        unsupportedSlice.visitEnd();
        slices.visitEnd();
        inject.visitEnd();
        atMethod.visitCode();
        atMethod.visitInsn(Opcodes.RETURN);
        atMethod.visitMaxs(0, 0);
        atMethod.visitEnd();

        MethodVisitor constantsMethod = writer.visitMethod(
                Opcodes.ACC_PRIVATE, "modifyTypedConstants", "(I)I", null, null);
        AnnotationVisitor modify = constantsMethod.visitAnnotation(MODIFY_CONSTANT_DESC, false);
        AnnotationVisitor targetMethods = modify.visitArray("method");
        targetMethods.visit(null, "tick()V");
        targetMethods.visitEnd();
        AnnotationVisitor constants = modify.visitArray("constant");

        AnnotationVisitor intConstant = constants.visitAnnotation(null, CONSTANT_DESC);
        intConstant.visit("intValue", 7);
        intConstant.visit("ordinal", 3);
        intConstant.visit("slice", "segment");
        intConstant.visit("log", true);
        AnnotationVisitor zeroConditions = intConstant.visitArray("expandZeroConditions");
        zeroConditions.visitEnum(null, CONDITION_DESC, "GREATER_THAN_ZERO");
        zeroConditions.visitEnd();
        intConstant.visitEnd();

        AnnotationVisitor floatConstant = constants.visitAnnotation(null, CONSTANT_DESC);
        floatConstant.visit("floatValue", 1.25F);
        floatConstant.visitEnd();
        AnnotationVisitor longConstant = constants.visitAnnotation(null, CONSTANT_DESC);
        longConstant.visit("longValue", 9L);
        longConstant.visitEnd();
        AnnotationVisitor doubleConstant = constants.visitAnnotation(null, CONSTANT_DESC);
        doubleConstant.visit("doubleValue", 2.5D);
        doubleConstant.visitEnd();
        AnnotationVisitor stringConstant = constants.visitAnnotation(null, CONSTANT_DESC);
        stringConstant.visit("stringValue", "hello");
        stringConstant.visitEnd();
        AnnotationVisitor classConstant = constants.visitAnnotation(null, CONSTANT_DESC);
        classConstant.visit("classValue", Type.getObjectType("com/example/Thing"));
        classConstant.visitEnd();
        AnnotationVisitor nullConstant = constants.visitAnnotation(null, CONSTANT_DESC);
        nullConstant.visit("nullValue", true);
        nullConstant.visitEnd();
        constants.visitEnd();
        modify.visitEnd();
        emitInjectorMethodBody(constantsMethod, "(I)I");

        MethodVisitor implicitMethod = writer.visitMethod(
                Opcodes.ACC_PRIVATE, "modifyImplicitConstant", "(I)I", null, null);
        AnnotationVisitor implicit = implicitMethod.visitAnnotation(MODIFY_CONSTANT_DESC, false);
        AnnotationVisitor implicitMethods = implicit.visitArray("method");
        implicitMethods.visit(null, "tick()V");
        implicitMethods.visitEnd();
        implicit.visitEnd();
        emitInjectorMethodBody(implicitMethod, "(I)I");

        MethodVisitor conflictMethod = writer.visitMethod(
                Opcodes.ACC_PRIVATE, "modifyConflictingConstant", "(I)I", null, null);
        AnnotationVisitor conflict = conflictMethod.visitAnnotation(MODIFY_CONSTANT_DESC, false);
        AnnotationVisitor conflictMethods = conflict.visitArray("method");
        conflictMethods.visit(null, "tick()V");
        conflictMethods.visitEnd();
        AnnotationVisitor conflictArray = conflict.visitArray("constant");
        AnnotationVisitor conflictConstant = conflictArray.visitAnnotation(null, CONSTANT_DESC);
        conflictConstant.visit("intValue", 2);
        conflictConstant.visit("stringValue", "also-int");
        conflictConstant.visitEnd();
        conflictArray.visitEnd();
        conflict.visitEnd();
        emitInjectorMethodBody(conflictMethod, "(I)I");

        writer.visitEnd();
        return writer.toByteArray();
    }


    private static byte[] refmapLookupMixinClass(
            String internalName, String targetInternalName, String[] methodSelectors,
            boolean injectorRemap, Boolean atRemap, String atValue, String atTarget, String[] atArgs) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                internalName, null, "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(MIXIN_DESC, true);
        AnnotationVisitor mixinTargets = mixin.visitArray("value");
        mixinTargets.visit(null, Type.getObjectType(targetInternalName));
        mixinTargets.visitEnd();
        mixin.visitEnd();
        MethodVisitor handler = writer.visitMethod(Opcodes.ACC_PRIVATE, "onTick", "()V", null, null);
        AnnotationVisitor inject = handler.visitAnnotation(INJECT_DESC, false);
        AnnotationVisitor methods = inject.visitArray("method");
        for (String selector : methodSelectors) methods.visit(null, selector);
        methods.visitEnd();
        if (!injectorRemap) inject.visit("remap", false);
        AnnotationVisitor atArray = inject.visitArray("at");
        AnnotationVisitor at = atArray.visitAnnotation(null, AT_DESC);
        at.visit("value", atValue);
        if (atTarget != null) at.visit("target", atTarget);
        if (atRemap != null) at.visit("remap", atRemap);
        if (atArgs != null && atArgs.length > 0) {
            AnnotationVisitor args = at.visitArray("args");
            for (String arg : atArgs) args.visit(null, arg);
            args.visitEnd();
        }
        at.visitEnd();
        atArray.visitEnd();
        inject.visitEnd();
        handler.visitCode();
        handler.visitInsn(Opcodes.RETURN);
        handler.visitMaxs(0, 0);
        handler.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] injectorMixinClass(String internalName, String targetInternalName) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                internalName, null, "java/lang/Object", null);
        AnnotationVisitor mixin = writer.visitAnnotation(MIXIN_DESC, true);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType(targetInternalName));
        targets.visitEnd();
        mixin.visitEnd();

        addInjectorMethod(writer, "onTick", "(L" + targetInternalName + ";I)V", INJECT_DESC,
                new String[] {"tick()V", "L" + targetInternalName + ";hurt(D)Z"},
                false, true, 1, null, true);
        addInjectorMethod(writer, "redirectHurt", "(L" + targetInternalName + ";D)Z", REDIRECT_DESC,
                new String[] {"L" + targetInternalName + ";hurt(D)Z"}, null, true, null, null, false);
        addDescSelectorMethod(writer, "redirectHurtDesc", targetInternalName);
        addInjectorMethod(writer, "modifyOverload", "(I)I", MODIFY_ARG_DESC,
                new String[] {"overloaded()V", "overloaded(I)V"}, null, false, null, 1, false);
        addInjectorMethod(writer, "modifyArgsBundle",
                "(Lorg/spongepowered/asm/mixin/injection/invoke/arg/Args;)V", MODIFY_ARGS_DESC,
                new String[] {"send(I)V"}, null, false, null, null, false);
        addInjectorMethod(writer, "modifyWildcard", "(I)I", MODIFY_VARIABLE_DESC,
                new String[] {"tick*()V"}, null, false, null, 2, false);
        addInjectorMethod(writer, "modifyConstant", "(I)I", MODIFY_CONSTANT_DESC,
                new String[] {"update()V"}, null, true, null, null, false);
        addInjectorMethod(writer, "badSelector", "()V", INJECT_DESC,
                new String[] {"tick(I"}, null, false, null, null, false);

        FieldVisitor field = writer.visitField(Opcodes.ACC_PRIVATE, "invalidInjectorField", "I", null, null);
        AnnotationVisitor invalid = field.visitAnnotation(INJECT_DESC, false);
        AnnotationVisitor methods = invalid.visitArray("method");
        methods.visit(null, "tick()V");
        methods.visitEnd();
        invalid.visitEnd();
        field.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void addDescSelectorMethod(ClassWriter writer, String methodName, String ownerInternalName) {
        String handlerDescriptor = "(L" + ownerInternalName + ";D)Z";
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, methodName, handlerDescriptor, null, null);
        AnnotationVisitor annotation = method.visitAnnotation(REDIRECT_DESC, false);
        AnnotationVisitor targetArray = annotation.visitArray("target");
        AnnotationVisitor desc = targetArray.visitAnnotation(null, DESC_DESC);
        desc.visit("owner", Type.getObjectType(ownerInternalName));
        desc.visit("value", "hurt");
        desc.visit("ret", Type.BOOLEAN_TYPE);
        AnnotationVisitor args = desc.visitArray("args");
        args.visit(null, Type.DOUBLE_TYPE);
        args.visitEnd();
        desc.visit("min", 1);
        desc.visit("max", 1);
        desc.visitEnd();
        targetArray.visitEnd();
        AnnotationVisitor at = annotation.visitAnnotation("at", AT_DESC);
        at.visit("value", "INVOKE");
        at.visitEnd();
        annotation.visitEnd();
        emitInjectorMethodBody(method, handlerDescriptor);
    }

    private static void addInjectorMethod(
            ClassWriter writer, String name, String descriptor, String annotationDescriptor,
            String[] selectors, Boolean remap, boolean includeSlice,
            Integer require, Integer index, boolean cancellable) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PRIVATE, name, descriptor, null, null);
        AnnotationVisitor annotation = method.visitAnnotation(annotationDescriptor, false);
        AnnotationVisitor methodSelectors = annotation.visitArray("method");
        for (String selector : selectors) methodSelectors.visit(null, selector);
        methodSelectors.visitEnd();
        if (remap != null) annotation.visit("remap", remap);
        if (require != null) annotation.visit("require", require);
        if (index != null) annotation.visit("index", index);
        if (MODIFY_VARIABLE_DESC.equals(annotationDescriptor) && index != null) {
            annotation.visit("argsOnly", true);
        }
        if (INJECT_DESC.equals(annotationDescriptor)) {
            annotation.visit("cancellable", cancellable);
            AnnotationVisitor atArray = annotation.visitArray("at");
            AnnotationVisitor at = atArray.visitAnnotation(null, AT_DESC);
            at.visit("value", "HEAD");
            at.visitEnd();
            atArray.visitEnd();
        } else if (!MODIFY_CONSTANT_DESC.equals(annotationDescriptor)) {
            AnnotationVisitor at = annotation.visitAnnotation("at", AT_DESC);
            at.visit("value", "HEAD");
            at.visitEnd();
        }
        if (MODIFY_CONSTANT_DESC.equals(annotationDescriptor)) {
            AnnotationVisitor constants = annotation.visitArray("constant");
            AnnotationVisitor constant = constants.visitAnnotation(null, CONSTANT_DESC);
            constant.visit("intValue", 1);
            constant.visitEnd();
            constants.visitEnd();
        }
        if (includeSlice) {
            if (INJECT_DESC.equals(annotationDescriptor) || MODIFY_CONSTANT_DESC.equals(annotationDescriptor)) {
                AnnotationVisitor slices = annotation.visitArray("slice");
                AnnotationVisitor slice = slices.visitAnnotation(null, SLICE_DESC);
                slice.visit("id", "sliceFixture");
                slice.visitEnd();
                slices.visitEnd();
            } else {
                AnnotationVisitor slice = annotation.visitAnnotation("slice", SLICE_DESC);
                slice.visit("id", "sliceFixture");
                slice.visitEnd();
            }
        }
        annotation.visitEnd();
        emitInjectorMethodBody(method, descriptor);
    }

    private static void emitInjectorMethodBody(MethodVisitor method, String descriptor) {
        method.visitCode();
        switch (Type.getReturnType(descriptor).getSort()) {
            case Type.VOID -> method.visitInsn(Opcodes.RETURN);
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                method.visitInsn(Opcodes.ICONST_0);
                method.visitInsn(Opcodes.IRETURN);
            }
            case Type.LONG -> {
                method.visitInsn(Opcodes.LCONST_0);
                method.visitInsn(Opcodes.LRETURN);
            }
            case Type.FLOAT -> {
                method.visitInsn(Opcodes.FCONST_0);
                method.visitInsn(Opcodes.FRETURN);
            }
            case Type.DOUBLE -> {
                method.visitInsn(Opcodes.DCONST_0);
                method.visitInsn(Opcodes.DRETURN);
            }
            case Type.ARRAY, Type.OBJECT -> {
                method.visitInsn(Opcodes.ACONST_NULL);
                method.visitInsn(Opcodes.ARETURN);
            }
            default -> throw new IllegalArgumentException("Unsupported method descriptor: " + descriptor);
        }
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    @Test
    void memberRemapDefaultsToMixinRemapButCanBeExplicitlyReenabled() throws Exception {
        String configPath = "mixins.remap.json";
        Path jar = writeJar("mixin-remap-defaults.jar", Map.of(
                configPath, """
                        {"package":"com.example.mixins","mixins":["RemapMixin"]}
                        """.getBytes(StandardCharsets.UTF_8),
                "com/example/mixins/RemapMixin.class",
                mixinRemapClass("com/example/mixins/RemapMixin")));

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(),
                Map.of(),
                "no-member-mapping-supplied");

        assertEquals(2, result.memberAnnotations().size());
        var inheritedFalse = result.memberAnnotations().stream()
                .filter(finding -> finding.sourceMemberName().equals("tick"))
                .findFirst().orElseThrow();
        assertFalse(inheritedFalse.remap());
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.MEMBER_REMAP_DISABLED_UNVERIFIED,
                inheritedFalse.status());

        var explicitTrue = result.memberAnnotations().stream()
                .filter(finding -> finding.sourceMemberName().equals("forceRemap"))
                .findFirst().orElseThrow();
        assertTrue(explicitTrue.remap());
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.MEMBER_REMAP_UNRESOLVED,
                explicitTrue.status());
    }


    @Test
    void pluginDeclaredMixinMembersRemainPluginDependentWithoutExecutingPluginCode() throws Exception {
        String configPath = "mixins.plugin.json";
        Path jar = writeJar("mixin-plugin-members.jar", Map.of(
                configPath, """
                        {
                          "package":"com.example.mixins",
                          "plugin":"com.example.DynamicPlugin",
                          "mixins":["MemberMixin"]
                        }
                        """.getBytes(StandardCharsets.UTF_8),
                "com/example/mixins/MemberMixin.class",
                memberMixinClass("com/example/mixins/MemberMixin", SOURCE_ENTITY)));

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(),
                Map.of(),
                "no-member-mapping-supplied");

        assertEquals(Forge1201MixinAnalyzer.PluginStatus.MISSING_FROM_SOURCE_ARCHIVE,
                result.configs().getFirst().pluginStatus());
        var overwrite = result.memberAnnotations().stream()
                .filter(finding -> finding.annotationKind() == Forge1201MixinAnalyzer.MemberAnnotationKind.OVERWRITE)
                .findFirst().orElseThrow();
        assertEquals(Forge1201MixinAnalyzer.MemberResolutionStatus.PLUGIN_DEPENDENT, overwrite.status());
        assertTrue(overwrite.evidence().contains("Plugin code was not executed"));
    }

    @Test
    void reportsUnsafeMissingAndMalformedConfigsWithoutSilentlySkippingThem() throws Exception {
        Path jar = writeJar("config-errors.jar", Map.of(
                "broken.json", "{ not valid json".getBytes(StandardCharsets.UTF_8)));

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "missing.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST),
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "../outside.json", Forge1201MixinAnalyzer.DiscoverySource.MANIFEST),
                        new Forge1201MixinAnalyzer.ConfigDeclaration(
                                "broken.json", Forge1201MixinAnalyzer.DiscoverySource.RESOURCE_HEURISTIC)),
                emptyTargetIndex(),
                Map.of(),
                "mapping-v1");

        assertEquals(3, result.configs().size());
        assertEquals(Forge1201MixinAnalyzer.ConfigStatus.CONFIG_MISSING,
                config(result, "missing.json").status());
        assertEquals(Forge1201MixinAnalyzer.ConfigStatus.UNSAFE_CONFIG_PATH,
                config(result, "../outside.json").status());
        assertEquals(Forge1201MixinAnalyzer.ConfigStatus.CONFIG_INVALID,
                config(result, "broken.json").status());
    }

    @Test
    void reportsMissingMixinClassAndDoesNotAssumeMinecraftIdentityMapping() throws Exception {
        String configPath = "mixins.missing.json";
        String config = """
                {"package":"com.example.mixins","mixins":["NotThere"]}
                """;
        Map<String, byte[]> archive = Map.of(configPath, config.getBytes(StandardCharsets.UTF_8));
        Path jar = writeJar("missing-class.jar", archive);

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(),
                Map.of(),
                "mapping-not-provided");

        assertEquals(Forge1201MixinAnalyzer.MixinClassStatus.MISSING,
                result.mixinClasses().getFirst().status());
        assertTrue(result.targets().isEmpty());
        assertEquals(Forge1201MixinAnalyzer.ConfigStatus.PARSED, result.configs().getFirst().status());
    }

    @Test
    void leavesUnmappedMinecraftTargetsUnresolvedAndHonorsRemapFalse() throws Exception {
        String configPath = "mixins.targets.json";
        Path jar = writeJar("unresolved-targets.jar", Map.of(
                configPath, """
                        {"package":"com.example.mixins","mixins":["UnmappedMixin"]}
                        """.getBytes(StandardCharsets.UTF_8),
                "com/example/mixins/UnmappedMixin.class",
                mixinClass("com/example/mixins/UnmappedMixin", SOURCE_ENTITY, null, false)));

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(),
                Map.of(SOURCE_ENTITY, SOURCE_ENTITY),
                "synthetic-mc-class-map-v1");

        assertEquals(1, result.targets().size());
        var target = result.targets().getFirst();
        assertFalse(target.remap());
        assertNull(target.targetInternalName());
        assertEquals(Forge1201MixinAnalyzer.TargetStatus.TARGET_REMAP_UNRESOLVED, target.status());
    }

    @Test
    void reportsTargetIndexUnavailableInsteadOfCallingMappedClassesMissing() throws Exception {
        String configPath = "mixins.index-unavailable.json";
        Path jar = writeJar("index-unavailable.jar", Map.of(
                configPath, """
                        {"package":"com.example.mixins","mixins":["UnverifiedMixin"]}
                        """.getBytes(StandardCharsets.UTF_8),
                "com/example/mixins/UnverifiedMixin.class",
                mixinClass("com/example/mixins/UnverifiedMixin", SOURCE_ENTITY, null, true)));

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                null,
                Map.of(SOURCE_ENTITY, SOURCE_ENTITY),
                "synthetic-mc-class-map-v1");

        assertEquals(Forge1201MixinAnalyzer.TargetIndexStatus.UNAVAILABLE_NO_TARGET_CLASSPATH,
                result.targetIndexStatus());
        assertEquals(Forge1201MixinAnalyzer.TargetStatus.TARGET_INDEX_UNAVAILABLE,
                result.targets().getFirst().status());
        assertEquals(SOURCE_ENTITY, result.targets().getFirst().targetInternalName());
    }

    @Test
    void distinguishesMappedMissingTargetClassAndRefmapAndPluginAbsence() throws Exception {
        String configPath = "mixins.target-missing.json";
        Path jar = writeJar("missing-target.jar", Map.of(
                configPath, """
                        {
                          "package":"com.example.mixins",
                          "plugin":"com.example.NotInArchive",
                          "refmap":"absent.refmap.json",
                          "mixins":["MissingTargetMixin"]
                        }
                        """.getBytes(StandardCharsets.UTF_8),
                "com/example/mixins/MissingTargetMixin.class",
                mixinClass("com/example/mixins/MissingTargetMixin", SOURCE_ENTITY, null, true)));

        var result = Forge1201MixinAnalyzer.analyze(
                jar,
                List.of(new Forge1201MixinAnalyzer.ConfigDeclaration(
                        configPath, Forge1201MixinAnalyzer.DiscoverySource.MANIFEST)),
                emptyTargetIndex(),
                Map.of(SOURCE_ENTITY, SOURCE_ENTITY),
                "synthetic-mc-class-map-v1");

        assertEquals(Forge1201MixinAnalyzer.PluginStatus.MISSING_FROM_SOURCE_ARCHIVE,
                result.configs().getFirst().pluginStatus());
        assertEquals(Forge1201MixinAnalyzer.ResourceStatus.MISSING,
                result.configs().getFirst().refmapStatus());
        assertEquals(Forge1201MixinAnalyzer.TargetStatus.TARGET_CLASS_MISSING,
                result.targets().getFirst().status());
    }

    private Forge1201MixinAnalyzer.ConfigFinding config(
            Forge1201MixinAnalyzer.Result result, String path) {
        return result.configs().stream().filter(finding -> finding.configPath().equals(path))
                .findFirst().orElseThrow();
    }

    private Path writeJar(String name, Map<String, byte[]> entries) throws Exception {
        Path path = tempDir.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return path;
    }



    private static byte[] mixinRemapClass(String internalName) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        AnnotationVisitor mixin = writer.visitAnnotation(MIXIN_DESC, true);
        AnnotationVisitor targets = mixin.visitArray("targets");
        targets.visit(null, "net.minecraft.world.entity.Entity");
        targets.visitEnd();
        mixin.visit("remap", false);
        mixin.visitEnd();

        addAnnotatedMethod(writer, "tick", "()V", OVERWRITE_DESC, null, null, null, null);
        addAnnotatedMethod(writer, "forceRemap", "()V", OVERWRITE_DESC, null, null, null, true);

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] memberMixinClass(String internalName, String targetInternalName) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, internalName, null, "java/lang/Object", null);

        AnnotationVisitor mixin = writer.visitAnnotation(MIXIN_DESC, true);
        AnnotationVisitor targets = mixin.visitArray("value");
        targets.visit(null, Type.getObjectType(targetInternalName));
        targets.visitEnd();
        mixin.visitEnd();

        FieldVisitor shadowField = writer.visitField(Opcodes.ACC_PRIVATE, "shadow$health", "D", null, null);
        AnnotationVisitor shadowFieldAnnotation = shadowField.visitAnnotation(SHADOW_DESC, false);
        shadowFieldAnnotation.visit("prefix", "shadow$");
        AnnotationVisitor fieldAliases = shadowFieldAnnotation.visitArray("aliases");
        fieldAliases.visit(null, "healthLegacy");
        fieldAliases.visitEnd();
        shadowFieldAnnotation.visitEnd();
        shadowField.visitEnd();

        addAnnotatedMethod(writer, "shadow$heal", "(I)V", SHADOW_DESC,
                null, "shadow$", new String[] {"healLegacy"}, null);
        addAnnotatedMethod(writer, "tick", "(I)V", OVERWRITE_DESC,
                null, null, new String[] {"oldTick"}, false);
        addAnnotatedMethod(writer, "getHealth", "()D", ACCESSOR_DESC, null, null, null, null);
        addAnnotatedMethod(writer, "setHealth", "(D)V", ACCESSOR_DESC, "health", null, null, null);
        addAnnotatedMethod(writer, "callHeal", "(I)V", INVOKER_DESC, null, null, null, null);
        addAnnotatedMethod(writer, "newEntity", "(I)Lcom/example/Target;", INVOKER_DESC,
                "<init>", null, null, null);
        addAnnotatedMethod(writer, "fetchHealth", "()D", ACCESSOR_DESC, null, null, null, null);
        addAnnotatedMethod(writer, "gethealth", "()D", ACCESSOR_DESC, null, null, null, null);
        addAnnotatedMethod(writer, "getHealthWithArgs", "(II)V", ACCESSOR_DESC, null, null, null, null);
        addAnnotatedMethod(writer, "readHealthConcrete", "()D", ACCESSOR_DESC,
                "health", null, null, null, false);

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void addAnnotatedMethod(
            ClassWriter writer,
            String name,
            String descriptor,
            String annotationDescriptor,
            String value,
            String prefix,
            String[] aliases,
            Boolean remap) {
        boolean abstractMethod = ACCESSOR_DESC.equals(annotationDescriptor)
                || INVOKER_DESC.equals(annotationDescriptor);
        addAnnotatedMethod(writer, name, descriptor, annotationDescriptor,
                value, prefix, aliases, remap, abstractMethod);
    }

    private static void addAnnotatedMethod(
            ClassWriter writer,
            String name,
            String descriptor,
            String annotationDescriptor,
            String value,
            String prefix,
            String[] aliases,
            Boolean remap,
            boolean abstractMethod) {
        int access = SHADOW_DESC.equals(annotationDescriptor) && aliases != null && aliases.length > 0
                ? Opcodes.ACC_PRIVATE : Opcodes.ACC_PUBLIC;
        if (abstractMethod) access |= Opcodes.ACC_ABSTRACT;
        MethodVisitor method = writer.visitMethod(access, name, descriptor, null, null);
        AnnotationVisitor annotation = method.visitAnnotation(annotationDescriptor, false);
        if (value != null) annotation.visit("value", value);
        if (prefix != null) annotation.visit("prefix", prefix);
        if (remap != null) annotation.visit("remap", remap);
        if (aliases != null) {
            AnnotationVisitor aliasValues = annotation.visitArray("aliases");
            for (String alias : aliases) aliasValues.visit(null, alias);
            aliasValues.visitEnd();
        }
        if (OVERWRITE_DESC.equals(annotationDescriptor)) {
            annotation.visit("constraints", "@env(DEFAULT)");
        }
        annotation.visitEnd();

        if (abstractMethod) {
            method.visitEnd();
            return;
        }

        method.visitCode();
        switch (Type.getReturnType(descriptor).getSort()) {
            case Type.VOID -> method.visitInsn(Opcodes.RETURN);
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                method.visitInsn(Opcodes.ICONST_0);
                method.visitInsn(Opcodes.IRETURN);
            }
            case Type.LONG -> {
                method.visitInsn(Opcodes.LCONST_0);
                method.visitInsn(Opcodes.LRETURN);
            }
            case Type.FLOAT -> {
                method.visitInsn(Opcodes.FCONST_0);
                method.visitInsn(Opcodes.FRETURN);
            }
            case Type.DOUBLE -> {
                method.visitInsn(Opcodes.DCONST_0);
                method.visitInsn(Opcodes.DRETURN);
            }
            case Type.ARRAY, Type.OBJECT -> {
                method.visitInsn(Opcodes.ACONST_NULL);
                method.visitInsn(Opcodes.ARETURN);
            }
            default -> throw new IllegalArgumentException("Unsupported test method descriptor: " + descriptor);
        }
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static byte[] plainClass(String internalName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] mixinClass(
            String internalName, String classLiteralTarget, String stringTarget, boolean remap) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        if (classLiteralTarget != null) {
            AnnotationVisitor annotation = writer.visitAnnotation(MIXIN_DESC, true);
            AnnotationVisitor values = annotation.visitArray("value");
            values.visit(null, Type.getObjectType(classLiteralTarget));
            values.visitEnd();
            annotation.visit("remap", remap);
            annotation.visitEnd();
        }
        if (stringTarget != null) {
            AnnotationVisitor annotation = writer.visitAnnotation(MIXIN_DESC, false);
            AnnotationVisitor targets = annotation.visitArray("targets");
            targets.visit(null, stringTarget.replace('/', '.'));
            targets.visitEnd();
            annotation.visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Forge1201TargetApiVerifier.TargetIndex emptyTargetIndex() {
        return Forge1201TargetApiVerifier.createIndex(ignored -> null);
    }
}
