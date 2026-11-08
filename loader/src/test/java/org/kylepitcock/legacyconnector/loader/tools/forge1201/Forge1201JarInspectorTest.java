package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class Forge1201JarInspectorTest {
    @TempDir
    Path tempDir;

    @Test
    void recognizesForge1201JarFromModsToml() throws IOException {
        Path jar = createJar("META-INF/mods.toml", forge1201Metadata());

        Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(jar);

        assertNotNull(info);
        assertEquals("[1.20.1,1.20.2)", info.minecraftVersionRange());
        assertEquals("[47.3.0,)", info.forgeVersionRange());
        assertEquals("alien_evolution", info.mods().get(0).modId());
        assertEquals("Alien Evolution", info.displayName());
        assertEquals(8, info.mods().get(0).dependencies().size());
        assertEquals("minecraft", info.mods().get(0).dependencies().get(0).modId());
        assertEquals("forge", info.mods().get(0).dependencies().get(1).modId());
        assertEquals("palladium", info.mods().get(0).dependencies().get(2).modId());
        assertEquals("geckolib", info.mods().get(0).dependencies().get(3).modId());
        assertEquals("kubejs", info.mods().get(0).dependencies().get(4).modId());
        assertEquals("rhino", info.mods().get(0).dependencies().get(5).modId());
        assertEquals("architectury", info.mods().get(0).dependencies().get(6).modId());
        assertEquals("pehkui", info.mods().get(0).dependencies().get(7).modId());
        assertEquals("[47.3.0,)", info.mods().get(0).dependencies().get(1).versionRange());
        assertEquals(true, info.mods().get(0).dependencies().get(1).mandatory());
    }

    @Test
    void reportsNativeDependencyCandidatesWithoutClaimingCompatibility() throws IOException {
        Path legacyJar = createJar("META-INF/mods.toml", forge1201Metadata());
        Path nativeGecko = createJar("META-INF/neoforge.mods.toml", """
                modLoader="javafml"
                loaderVersion="[4,)"
                [[mods]]
                modId="geckolib"
                version="4.8.0"

                [[dependencies.geckolib]]
                modId="optional_dependency_not_installed"
                mandatory=false
                versionRange="*"
                ordering="NONE"
                side="BOTH"
                """);
        Path nativeKube = createJar("META-INF/neoforge.mods.toml", """
                modLoader="javafml"
                loaderVersion="[4,)"
                [[mods]]
                modId="kubejs"
                version="2101.7.0"
                """);

        Forge1201JarInfo info = Forge1201JarInspector.inspectForge1201(legacyJar);
        var nativeIds = Forge1201DependencyResolver.findNativeModIds(java.util.List.of(tempDir));
        var resolutions = Forge1201DependencyResolver.resolve(info, nativeIds);

        assertEquals(2, nativeIds.size());
        org.junit.jupiter.api.Assertions.assertFalse(nativeIds.contains("optional_dependency_not_installed"));
        assertEquals(Forge1201DependencyResolver.Status.NATIVE_ID_PRESENT,
                resolutions.stream().filter(r -> r.modId().equals("geckolib")).findFirst().orElseThrow().status());
        assertEquals(Forge1201DependencyResolver.Status.NATIVE_ID_PRESENT,
                resolutions.stream().filter(r -> r.modId().equals("kubejs")).findFirst().orElseThrow().status());
        assertEquals(Forge1201DependencyResolver.Status.REQUIRED_MISSING,
                resolutions.stream().filter(r -> r.modId().equals("palladium")).findFirst().orElseThrow().status());
        assertEquals(Forge1201DependencyResolver.Status.HOST_BRIDGE,
                resolutions.stream().filter(r -> r.modId().equals("forge")).findFirst().orElseThrow().status());
        org.junit.jupiter.api.Assertions.assertTrue(Forge1201DependencyResolver.summarize(info, nativeIds)
                .contains("API/version not yet verified"));
    }

    @Test
    void analyzesForgeApiClassAndMemberReferencesWithoutLoadingTheMod() throws IOException {
        Path jar = createJarWithClass("META-INF/mods.toml", forge1201Metadata(),
                "sample/AlienEvoMod.class", sampleForgeModClass());

        Forge1201ApiUsageAnalyzer.Analysis analysis = Forge1201ApiUsageAnalyzer.analyze(jar);

        assertEquals(1, analysis.classCount());
        assertEquals(java.util.List.of("sample/AlienEvoMod (alien_evolution)"), analysis.modEntrypoints());
        org.junit.jupiter.api.Assertions.assertTrue(analysis.referencedApiClasses()
                .contains("net/minecraftforge/fml/ModList"));
        org.junit.jupiter.api.Assertions.assertTrue(analysis.methodReferences().stream()
                .anyMatch(reference -> reference.owner().equals("net/minecraftforge/fml/ModList")
                        && reference.name().equals("get")));
        assertEquals(2, analysis.methodCallSites().size());
        var forgeCallSite = analysis.methodCallSites().stream()
                .filter(call -> call.targetOwner().equals("net/minecraftforge/fml/ModList"))
                .findFirst().orElseThrow();
        assertEquals("sample/AlienEvoMod", forgeCallSite.callerClass());
        assertEquals("sample", forgeCallSite.callerMethod());
        assertEquals("()V", forgeCallSite.callerDescriptor());
        assertEquals("get", forgeCallSite.targetName());
        assertEquals("()Lnet/minecraftforge/fml/ModList;", forgeCallSite.targetDescriptor());
        assertEquals(Opcodes.INVOKESTATIC, forgeCallSite.opcode());

        var vanillaCallSite = analysis.methodCallSites().stream()
                .filter(call -> call.targetOwner().equals("net/minecraft/world/effect/MobEffectInstance"))
                .findFirst().orElseThrow();
        assertEquals("sample/AlienEvoMod", vanillaCallSite.callerClass());
        assertEquals("sample", vanillaCallSite.callerMethod());
        assertEquals("getDuration", vanillaCallSite.targetName());
        assertEquals("()I", vanillaCallSite.targetDescriptor());
        assertEquals(Opcodes.INVOKEVIRTUAL, vanillaCallSite.opcode());

        assertEquals(2, analysis.fieldCallSites().size());
        var vanillaFieldCallSite = analysis.fieldCallSites().stream()
                .filter(call -> call.targetOwner().equals("net/minecraft/world/effect/MobEffects"))
                .findFirst().orElseThrow();
        assertEquals("sample/AlienEvoMod", vanillaFieldCallSite.callerClass());
        assertEquals("sample", vanillaFieldCallSite.callerMethod());
        assertEquals("MOVEMENT_SPEED", vanillaFieldCallSite.targetName());
        assertEquals("Lnet/minecraft/world/effect/MobEffect;", vanillaFieldCallSite.targetDescriptor());
        assertEquals(Opcodes.GETSTATIC, vanillaFieldCallSite.opcode());

        org.junit.jupiter.api.Assertions.assertTrue(analysis.fieldReferences().stream()
                .anyMatch(reference -> reference.owner().equals("net/minecraftforge/registries/ForgeRegistries")
                        && reference.name().equals("ITEMS")));
    }

    @Test
    void tracesEveryMethodWhenClassWildcardIsSelected() throws IOException {
        Path jar = createJarWithClass("META-INF/mods.toml", forge1201Metadata(),
                "sample/AlienEvoMod.class", sampleForgeModClass());
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream replacementOut = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(replacementOut);
            Forge1201ApiUsageAnalyzer.printSelectedMethodBytecode(
                    jar, java.util.Set.of("sample.AlienEvoMod#*"));
        } finally {
            System.setOut(originalOut);
        }

        String output = captured.toString(StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(output.contains("sample.AlienEvoMod#sample()V"));
        org.junit.jupiter.api.Assertions.assertTrue(output.contains("INVOKESTATIC"));
        org.junit.jupiter.api.Assertions.assertFalse(output.contains("NOT FOUND"));
    }

    @Test
    void analyzesTranslatedNeoForgeMetadataJar() throws IOException {
        Path jar = createJarWithClass("META-INF/neoforge.mods.toml", neoForgeMetadata(),
                "sample/TranslatedMod.class", sampleTranslatedModClass());

        Forge1201ApiUsageAnalyzer.Analysis analysis =
                Forge1201ApiUsageAnalyzer.analyzeTranslated(jar);

        assertNull(analysis.metadata());
        assertEquals(1, analysis.classCount());
        assertEquals(java.util.List.of("sample/TranslatedMod (sample)"), analysis.modEntrypoints());
        org.junit.jupiter.api.Assertions.assertTrue(analysis.referencedApiClasses()
                .contains("net/neoforged/fml/ModList"));
        org.junit.jupiter.api.Assertions.assertTrue(analysis.referencedApiClasses()
                .contains("net/minecraftforge/eventbus/api/Event$Result"));
        org.junit.jupiter.api.Assertions.assertTrue(analysis.methodCallSites().stream()
                .anyMatch(call -> call.targetOwner().equals("net/neoforged/fml/ModList")
                        && call.targetName().equals("get")));
        org.junit.jupiter.api.Assertions.assertTrue(analysis.methodReferences().stream()
                .anyMatch(reference -> reference.owner().equals("net/neoforged/fml/ModList")
                        && reference.name().equals("get")));
    }

    @Test
    void refusesToAnalyzeForgeMetadataAsTranslatedJar() throws IOException {
        Path jar = createJar("META-INF/mods.toml", forge1201Metadata());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Forge1201ApiUsageAnalyzer.analyzeTranslated(jar));
    }

    @Test
    void ignoresNeoForgeMetadata() throws IOException {
        Path jar = createJar("META-INF/neoforge.mods.toml", forge1201Metadata());

        assertNull(Forge1201JarInspector.inspectForge1201(jar));
    }

    @Test
    void ignoresOtherMinecraftVersions() throws IOException {
        String metadata = forge1201Metadata().replace("[1.20.1,1.20.2)", "[1.20.2,1.20.3)");
        Path jar = createJar("META-INF/mods.toml", metadata);

        assertNull(Forge1201JarInspector.inspectForge1201(jar));
    }

    @Test
    void requiresAForgeDependency() throws IOException {
        String metadata = forge1201Metadata().replace("""
                [[dependencies.alien_evolution]]
                modId="forge"
                mandatory=true
                versionRange="[47.3.0,)"
                ordering="NONE"
                side="BOTH"
                """, "");
        Path jar = createJar("META-INF/mods.toml", metadata);

        assertNull(Forge1201JarInspector.inspectForge1201(jar));
    }

    private Path createJarWithClass(String metadataPath, String metadata, String classPath, byte[] classBytes)
            throws IOException {
        Path jar = Files.createTempFile(tempDir, "forge-1.20.1-analysis-", ".jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry(metadataPath));
            out.write(metadata.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry(classPath));
            out.write(classBytes);
            out.closeEntry();
        }
        return jar;
    }

    private static byte[] sampleTranslatedModClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/TranslatedMod", null,
                "java/lang/Object", null);
        AnnotationVisitor mod = writer.visitAnnotation("Lnet/neoforged/fml/common/Mod;", true);
        mod.visit("value", "sample");
        mod.visitEnd();

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "sample",
                "()V", null, null);
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/neoforged/fml/ModList",
                "get", "()Lnet/neoforged/fml/ModList;", false);
        method.visitInsn(Opcodes.POP);
        method.visitFieldInsn(Opcodes.GETSTATIC, "sample/TranslatedMod",
                "RESULT", "Lnet/minecraftforge/eventbus/api/Event$Result;");
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "RESULT",
                "Lnet/minecraftforge/eventbus/api/Event$Result;", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static String neoForgeMetadata() {
        return """
                modLoader="javafml"
                loaderVersion="[1,)"

                [[mods]]
                modId="sample"
                version="1.0.0"
                displayName="Sample"

                [[dependencies.sample]]
                modId="minecraft"
                type="required"
                versionRange="[1.21.1,1.22)"
                ordering="NONE"
                side="BOTH"

                [[dependencies.sample]]
                modId="neoforge"
                type="required"
                versionRange="[21.1.258,)"
                ordering="NONE"
                side="BOTH"
                """;
    }

    private static byte[] sampleForgeModClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/AlienEvoMod", null, "java/lang/Object", null);
        AnnotationVisitor mod = writer.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", true);
        mod.visit("value", "alien_evolution");
        mod.visitEnd();

        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "sample",
                "()V", null, null);
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/ModList",
                "get", "()Lnet/minecraftforge/fml/ModList;", false);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/effect/MobEffectInstance",
                "getDuration", "()I", false);
        method.visitInsn(Opcodes.POP);
        method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraft/world/effect/MobEffects",
                "MOVEMENT_SPEED", "Lnet/minecraft/world/effect/MobEffect;");
        method.visitInsn(Opcodes.POP);
        method.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/registries/ForgeRegistries",
                "ITEMS", "Lnet/minecraftforge/registries/IForgeRegistry;");
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private Path createJar(String metadataPath, String metadata) throws IOException {
        Path jar = Files.createTempFile(tempDir, "forge-1.20.1-", ".jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry(metadataPath));
            out.write(metadata.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

    private static String forge1201Metadata() {
        return """
                modLoader="javafml"
                loaderVersion="[47.2,)"
                license="MIT"

                [[mods]]
                modId="alien_evolution"
                version="1.0.0"
                displayName="Alien Evolution"
                description="Test metadata"

                [[dependencies.alien_evolution]]
                modId="minecraft"
                mandatory=true
                versionRange="[1.20.1,1.20.2)"
                ordering="NONE"
                side="BOTH"

                [[dependencies.alien_evolution]]
                modId="forge"
                mandatory=true
                versionRange="[47.3.0,)"
                ordering="NONE"
                side="BOTH"

                [[dependencies.alien_evolution]]
                modId="palladium"
                mandatory=true
                versionRange="*"
                ordering="AFTER"
                side="BOTH"

                [[dependencies.alien_evolution]]
                modId="geckolib"
                mandatory=true
                versionRange="*"
                ordering="AFTER"
                side="BOTH"

                [[dependencies.alien_evolution]]
                modId="kubejs"
                mandatory=true
                versionRange="*"
                ordering="AFTER"
                side="BOTH"

                [[dependencies.alien_evolution]]
                modId="rhino"
                mandatory=true
                versionRange="*"
                ordering="AFTER"
                side="BOTH"

                [[dependencies.alien_evolution]]
                modId="architectury"
                mandatory=true
                versionRange="*"
                ordering="AFTER"
                side="BOTH"

                [[dependencies.alien_evolution]]
                modId="pehkui"
                mandatory=true
                versionRange="*"
                ordering="AFTER"
                side="BOTH"
                """;
    }
}
