package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;


import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201ApiUsageAnalyzer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

final class Forge1201JarTransformerTest {
    @TempDir
    Path tempDir;

    private static final String FORGE_METADATA = """
            modLoader="javafml"
            loaderVersion="[47,)"
            license="All Rights Reserved"

            [[mods]]
            modId="sample"
            version="1.0.0"
            displayName="Sample"
            description="ASM transformer fixture"

            [[dependencies.sample]]
            modId="minecraft"
            mandatory=true
            versionRange="[1.20.1,1.21)"
            ordering="NONE"
            side="BOTH"

            [[dependencies.sample]]
            modId="forge"
            mandatory=true
            versionRange="[47,)"
            ordering="NONE"
            side="BOTH"
            """;

    @Test
    void convertsMetadataAndRelocatesReviewedBytecodeReferences() throws Exception {
        Path original = createSourceJar(false);
        Path output = tempDir.resolve("out").resolve("sample-transformed.jar");
        String originalDigest = sha256(original);

        Forge1201JarTransformer.Result result = Forge1201JarTransformer.transform(
                original, output, "[1,)", "[21.1.258,)", "[1.21.1,1.22)");

        assertTrue(Files.isRegularFile(output));
        assertEquals(originalDigest, sha256(original));
        assertEquals(originalDigest, result.inputSha256());
        assertEquals(sha256(output), result.outputSha256());
        assertNotEquals(result.inputSha256(), result.outputSha256());

        Forge1201BytecodeTransformer.Result bytecode = result.bytecode();
        assertEquals(2, bytecode.classesScanned());
        assertEquals(2, bytecode.classesChanged());
        assertEquals(1, bytecode.classEntriesRelocated());
        assertTrue(bytecode.relocatedTypes().stream()
                .anyMatch(value -> value.startsWith("net/minecraftforge/fml/ModList -> ")));
        assertTrue(bytecode.relocatedTypes().contains(
                "net/minecraftforge/network/NetworkEvent$Context -> "
                        + "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkEvent$Context"));
        assertFalse(bytecode.manualAdapterTypes().contains("net/minecraftforge/registries/ForgeRegistries"));
        assertFalse(bytecode.manualAdapterTypes().contains("net/minecraftforge/registries/IForgeRegistry"));
        assertEquals(1, bytecode.registryAdapters().builtInRegistryFieldReads());
        assertTrue(bytecode.unmappedForgeApiTypes().contains("net/minecraftforge/unknown/MissingApi"));

        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(output.toFile())) {
            assertTrue(zip.getEntry(Forge1201JarArchiveTransformer.SOURCE_METADATA) == null);
            assertTrue(zip.getEntry(Forge1201JarArchiveTransformer.TARGET_METADATA) != null);
            assertTrue(zip.getEntry("net/minecraftforge/fml/legacy/OwnClass.class") == null);
            assertTrue(zip.getEntry("net/neoforged/fml/legacy/OwnClass.class") != null);
            assertTrue(zip.getEntry("META-INF/TEST.SF") == null);
            assertTrue(zip.getEntry("META-INF/services/net.minecraftforge.fml.ModList") == null);

            var service = zip.getEntry("META-INF/services/net.neoforged.fml.ModList");
            assertTrue(service != null);
            String serviceText = new String(zip.getInputStream(service).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(serviceText.contains("# retained provider comment"));
            assertTrue(serviceText.contains("net.neoforged.fml.legacy.OwnClass"));

            ClassNode caller = new ClassNode();
            new org.objectweb.asm.ClassReader(
                    zip.getInputStream(zip.getEntry("sample/Caller.class")).readAllBytes()).accept(caller, 0);
            assertEquals("Lnet/neoforged/fml/common/Mod;", caller.visibleAnnotations.get(0).desc);

            var invoke = caller.methods.stream()
                    .filter(method -> method.name.equals("invoke"))
                    .findFirst().orElseThrow();
            MethodInsnNode methodCall = firstInstruction(invoke, MethodInsnNode.class);
            assertEquals("net/neoforged/fml/ModList", methodCall.owner);
            assertEquals("()Lnet/neoforged/fml/ModList;", methodCall.desc);

            FieldInsnNode field = firstInstruction(invoke, FieldInsnNode.class);
            assertEquals("net/minecraft/core/registries/BuiltInRegistries", field.owner);
            assertEquals("Lnet/minecraft/core/DefaultedRegistry;", field.desc);

            LdcInsnNode classLiteral = firstInstruction(invoke, LdcInsnNode.class);
            assertEquals("Lnet/neoforged/fml/ModList;", ((Type) classLiteral.cst).getDescriptor());

            String shimPrefix = "legacyconnector/v1_20_1/net/minecraftforge/network/";
            String contextType = shimPrefix + "NetworkEvent$Context";
            String simpleChannelType = shimPrefix + "simple/SimpleChannel";
            String messageBuilderType = shimPrefix + "simple/SimpleChannel$MessageBuilder";
            String contextDescriptor = "L" + contextType + ";";
            String messageBuilderDescriptor = "L" + messageBuilderType + ";";

            var networkFactory = caller.methods.stream()
                    .filter(method -> method.name.equals("networkFactory"))
                    .findFirst().orElseThrow();
            assertEquals("(" + contextDescriptor + ")Ljava/util/function/Supplier;", networkFactory.desc);
            InvokeDynamicInsnNode dynamic = firstInstruction(networkFactory, InvokeDynamicInsnNode.class);
            assertEquals("(" + contextDescriptor + ")Ljava/util/function/Supplier;", dynamic.desc);
            Handle lambdaImplementation = (Handle) dynamic.bsmArgs[1];
            assertEquals(simpleChannelType, lambdaImplementation.getOwner());
            assertEquals("(" + contextDescriptor + ")" + messageBuilderDescriptor,
                    lambdaImplementation.getDesc());
            assertEquals(Type.getMethodType("()" + messageBuilderDescriptor), dynamic.bsmArgs[2]);

            var networkHandle = caller.methods.stream()
                    .filter(method -> method.name.equals("networkHandle"))
                    .findFirst().orElseThrow();
            Handle classHandle = (Handle) firstInstruction(networkHandle, LdcInsnNode.class).cst;
            assertEquals(shimPrefix + "NetworkRegistry", classHandle.getOwner());
            assertEquals("()L" + shimPrefix + "simple/SimpleChannel;", classHandle.getDesc());

            var networkTypeLiteral = caller.methods.stream()
                    .filter(method -> method.name.equals("networkTypeLiteral"))
                    .findFirst().orElseThrow();
            assertEquals(contextDescriptor,
                    ((Type) firstInstruction(networkTypeLiteral, LdcInsnNode.class).cst).getDescriptor());

            var missing = caller.methods.stream()
                    .filter(method -> method.name.equals("missingApi"))
                    .findFirst().orElseThrow();
            assertEquals("()Lnet/minecraftforge/unknown/MissingApi;", missing.desc);

            String metadata = new String(
                    zip.getInputStream(zip.getEntry(Forge1201JarArchiveTransformer.TARGET_METADATA)).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertTrue(metadata.contains("modId=\"neoforge\""));
            assertTrue(metadata.contains("type=\"required\""));
            assertFalse(metadata.contains("modId=\"forge\""));
        }
    }

    @Test
    void analyzerTracksConnectorOwnedNetworkShimReferencesInTranslatedJar() throws Exception {
        Path translated = tempDir.resolve("translated-shim-reference.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(translated))) {
            put(out, Forge1201JarArchiveTransformer.TARGET_METADATA, "modLoader=\"javafml\"\n");
            put(out, "sample/TranslatedShimCaller.class", translatedShimReferenceClass());
        }

        Forge1201ApiUsageAnalyzer.Analysis analysis =
                Forge1201ApiUsageAnalyzer.analyzeTranslated(translated);
        String context = "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkEvent$Context";
        String direction = "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkDirection";

        assertTrue(analysis.referencedApiClasses().contains(context));
        assertTrue(analysis.referencedApiClasses().contains(direction));
        assertTrue(analysis.methodReferences().stream().anyMatch(reference ->
                reference.owner().equals(context) && reference.name().equals("isHandled")
                        && reference.descriptor().equals("()Z")));
        assertTrue(analysis.fieldReferences().stream().anyMatch(reference ->
                reference.owner().equals(context) && reference.name().equals("direction")
                        && reference.descriptor().equals("L" + direction + ";")));
        assertTrue(analysis.methodCallSites().stream().anyMatch(callSite ->
                callSite.targetOwner().equals(context) && callSite.targetName().equals("isHandled")));
    }

    @Test
    void refusesToOverwriteOriginalJar() throws Exception {
        Path original = createSourceJar(false);
        String digest = sha256(original);

        assertThrows(IllegalArgumentException.class, () -> Forge1201JarTransformer.transform(
                original, original, "[1,)", "[21.1.258,)", "[1.21.1,1.22)"));

        assertEquals(digest, sha256(original));
    }

    @Test
    void failsOnClassEntryRelocationCollisionWithoutWritingOutput() throws Exception {
        Path original = createSourceJar(true);
        Path output = tempDir.resolve("collision.jar");

        assertThrows(IOException.class, () -> Forge1201JarTransformer.transform(
                original, output, "[1,)", "[21.1.258,)", "[1.21.1,1.22)"));

        assertFalse(Files.exists(output));
    }

    private Path createSourceJar(boolean includeRelocationCollision) throws IOException {
        Path jar = tempDir.resolve(includeRelocationCollision ? "collision-source.jar" : "source.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            put(out, "META-INF/mods.toml", FORGE_METADATA);
            put(out, "META-INF/TEST.SF", "stale signature");
            put(out, "META-INF/services/net.minecraftforge.fml.ModList",
                    "# retained provider comment\nnet.minecraftforge.fml.legacy.OwnClass\n");
            put(out, "sample/Caller.class", callerClass());
            put(out, "net/minecraftforge/fml/legacy/OwnClass.class", simpleClass(
                    "net/minecraftforge/fml/legacy/OwnClass"));
            if (includeRelocationCollision) {
                put(out, "net/neoforged/fml/legacy/OwnClass.class", simpleClass(
                        "net/neoforged/fml/legacy/OwnClass"));
            }
        }
        return jar;
    }

    private static <T> T firstInstruction(org.objectweb.asm.tree.MethodNode method, Class<T> type) {
        for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions) {
            if (type.isInstance(instruction)) return type.cast(instruction);
        }
        throw new AssertionError("Instruction not found: " + type.getName());
    }

    private static byte[] translatedShimReferenceClass() {
        String context = "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkEvent$Context";
        String direction = "legacyconnector/v1_20_1/net/minecraftforge/network/NetworkDirection";
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/TranslatedShimCaller", null,
                "java/lang/Object", null);
        var inspect = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "inspect",
                "(L" + context + ";)V",
                null,
                null);
        inspect.visitCode();
        inspect.visitVarInsn(Opcodes.ALOAD, 0);
        inspect.visitMethodInsn(Opcodes.INVOKEVIRTUAL, context, "isHandled", "()Z", false);
        inspect.visitInsn(Opcodes.POP);
        inspect.visitVarInsn(Opcodes.ALOAD, 0);
        inspect.visitFieldInsn(Opcodes.GETFIELD, context, "direction", "L" + direction + ";");
        inspect.visitInsn(Opcodes.POP);
        inspect.visitInsn(Opcodes.RETURN);
        inspect.visitMaxs(1, 1);
        inspect.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] callerClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/Caller", null, "java/lang/Object", null);
        var annotation = writer.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", true);
        annotation.visit("value", "sample");
        annotation.visitEnd();

        var invoke = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "invoke", "()V", null, null);
        invoke.visitCode();
        invoke.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/ModList",
                "get", "()Lnet/minecraftforge/fml/ModList;", false);
        invoke.visitInsn(Opcodes.POP);
        invoke.visitFieldInsn(Opcodes.GETSTATIC, "net/minecraftforge/registries/ForgeRegistries",
                "ITEMS", "Lnet/minecraftforge/registries/IForgeRegistry;");
        invoke.visitInsn(Opcodes.POP);
        invoke.visitLdcInsn(Type.getObjectType("net/minecraftforge/fml/ModList"));
        invoke.visitInsn(Opcodes.POP);
        invoke.visitInsn(Opcodes.RETURN);
        invoke.visitMaxs(1, 0);
        invoke.visitEnd();

        var missing = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "missingApi", "()Lnet/minecraftforge/unknown/MissingApi;", null, null);
        missing.visitCode();
        missing.visitInsn(Opcodes.ACONST_NULL);
        missing.visitInsn(Opcodes.ARETURN);
        missing.visitMaxs(1, 0);
        missing.visitEnd();

        String context = "net/minecraftforge/network/NetworkEvent$Context";
        String channel = "net/minecraftforge/network/simple/SimpleChannel";
        String builder = "net/minecraftforge/network/simple/SimpleChannel$MessageBuilder";
        var networkFactory = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "networkFactory",
                "(L" + context + ";)Ljava/util/function/Supplier;",
                null,
                null);
        networkFactory.visitCode();
        networkFactory.visitVarInsn(Opcodes.ALOAD, 0);
        networkFactory.visitInvokeDynamicInsn(
                "get",
                "(L" + context + ";)Ljava/util/function/Supplier;",
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        "java/lang/invoke/LambdaMetafactory",
                        "metafactory",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                                + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                                + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                        false),
                Type.getMethodType("()Ljava/lang/Object;"),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        channel,
                        "makeBuilder",
                        "(L" + context + ";)L" + builder + ";",
                        false),
                Type.getMethodType("()L" + builder + ";"));
        networkFactory.visitInsn(Opcodes.ARETURN);
        networkFactory.visitMaxs(1, 1);
        networkFactory.visitEnd();

        var networkHandle = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "networkHandle", "()V", null, null);
        networkHandle.visitCode();
        networkHandle.visitLdcInsn(new Handle(
                Opcodes.H_INVOKESTATIC,
                "net/minecraftforge/network/NetworkRegistry",
                "newChannel",
                "()Lnet/minecraftforge/network/simple/SimpleChannel;",
                false));
        networkHandle.visitInsn(Opcodes.POP);
        networkHandle.visitInsn(Opcodes.RETURN);
        networkHandle.visitMaxs(1, 0);
        networkHandle.visitEnd();

        var networkTypeLiteral = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "networkTypeLiteral", "()V", null, null);
        networkTypeLiteral.visitCode();
        networkTypeLiteral.visitLdcInsn(Type.getObjectType(context));
        networkTypeLiteral.visitInsn(Opcodes.POP);
        networkTypeLiteral.visitInsn(Opcodes.RETURN);
        networkTypeLiteral.visitMaxs(1, 0);
        networkTypeLiteral.visitEnd();

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] simpleClass(String internalName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void put(ZipOutputStream out, String name, String text) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }

    private static void put(ZipOutputStream out, String name, byte[] bytes) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(bytes);
        out.closeEntry();
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) >= 0;) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
