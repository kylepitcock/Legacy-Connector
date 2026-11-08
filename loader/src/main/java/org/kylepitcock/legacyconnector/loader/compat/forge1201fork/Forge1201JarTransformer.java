package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201JarInspector;
import org.kylepitcock.legacyconnector.loader.tools.forge1201.Forge1201JarInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs the isolated Forge 1.20.1 preparation stages in order:
 * metadata conversion first, then reviewed ASM package relocations.
 *
 * <p>The output is an intermediate compatibility artifact, not a claim that every
 * referenced API has been adapted or that the JAR is ready for mod discovery.</p>
 */
public final class Forge1201JarTransformer {
    private Forge1201JarTransformer() {}

    public record Result(
            Path input,
            Path output,
            String inputSha256,
            String outputSha256,
            int metadataEntriesCopied,
            int signaturesRemoved,
            Forge1201BytecodeTransformer.Result bytecode) {}

    public static Result transform(
            Path input,
            Path output,
            String neoForgeLoaderRange,
            String neoForgeVersionRange,
            String minecraftVersionRange) throws IOException {
        return transform(input, output, neoForgeLoaderRange, neoForgeVersionRange, minecraftVersionRange, null);
    }

    public static Result transform(
            Path input,
            Path output,
            String neoForgeLoaderRange,
            String neoForgeVersionRange,
            String minecraftVersionRange,
            Path adapterProfile) throws IOException {
        if (input == null || output == null) {
            throw new IllegalArgumentException("Input and output paths are required");
        }

        Path source = input.toAbsolutePath().normalize();
        Path destination = output.toAbsolutePath().normalize();
        if (source.equals(destination)) {
            throw new IllegalArgumentException("The Forge 1.20.1 transform must not overwrite its source JAR");
        }
        Forge1201JarInfo metadata = Forge1201JarInspector.inspectForge1201(source);
        if (metadata == null) {
            throw new IllegalArgumentException("Input is not a recognized Forge 1.20.1 mod JAR: " + source);
        }

        Path parent = destination.getParent();
        if (parent == null) throw new IOException("Output JAR must have a parent directory: " + destination);
        Files.createDirectories(parent);

        String prefix = destination.getFileName().toString();
        if (prefix.length() < 3) prefix = (prefix + "___").substring(0, 3);
        Path metadataStage = Files.createTempFile(parent, prefix, ".metadata.jar");
        try {
            Forge1201JarArchiveTransformer.Result metadataResult = Forge1201JarArchiveTransformer.transform(
                    source,
                    metadataStage,
                    neoForgeLoaderRange,
                    neoForgeVersionRange,
                    minecraftVersionRange);
            Forge1201BytecodeTransformer.Result bytecodeResult =
                    Forge1201BytecodeTransformer.transform(metadataStage, destination, adapterProfile);

            return new Result(
                    source,
                    destination,
                    metadataResult.inputSha256(),
                    bytecodeResult.outputSha256(),
                    metadataResult.copiedEntries(),
                    metadataResult.removedSignatureEntries() + bytecodeResult.signaturesRemoved(),
                    bytecodeResult);
        } finally {
            Files.deleteIfExists(metadataStage);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 5 && args.length != 6) {
            throw new IllegalArgumentException(
                    "Usage: transformForge1201 <input.jar> <output.jar> "
                            + "<javafml-range> <neoforge-range> <minecraft-range> [adapter-profile.json]");
        }

        Result result = transform(
                Path.of(args[0]),
                Path.of(args[1]),
                args[2],
                args[3],
                args[4],
                args.length == 6 ? Path.of(args[5]) : null);
        Forge1201BytecodeTransformer.Result bytecode = result.bytecode();

        System.out.println("Created preliminary Forge 1.20.1 -> NeoForge 1.21.1 artifact: " + result.output());
        System.out.println("  input SHA-256:  " + result.inputSha256());
        System.out.println("  output SHA-256: " + result.outputSha256());
        System.out.println("  classes scanned: " + bytecode.classesScanned());
        System.out.println("  classes rewritten: " + bytecode.classesChanged());
        System.out.println("  class entry paths relocated: " + bytecode.classEntriesRelocated());
        System.out.println("  resources copied: " + bytecode.resourcesCopied());
        System.out.println("  signature entries removed: " + result.signaturesRemoved());
        System.out.println("  reviewed API types package-relocated: " + bytecode.relocatedTypes().size());
        if (bytecode.adapterProfileId() != null) {
            System.out.println("  call-site adapter profile: " + bytecode.adapterProfileId());
            System.out.println("  adapter rules applied: " + bytecode.adapterRulesApplied());
            System.out.println("  adapter operations applied: " + bytecode.adapterOperationsApplied());
        }

        Forge1201RegistryAdapter.Stats registry = bytecode.registryAdapters();
        System.out.println("  RegistryObject#get -> Supplier#get calls: " + registry.registryObjectGetCalls());
        System.out.println("  DeferredRegister.create(IForgeRegistry, String) calls adapted: "
                + registry.deferredRegisterCreateCalls());
        System.out.println("  DeferredRegister.register(String, Supplier) return descriptors adapted: "
                + registry.deferredRegisterRegisterCalls());
        System.out.println("  ForgeRegistries built-in registry fields adapted: "
                + registry.builtInRegistryFieldReads());
        System.out.println("  IForgeRegistry lookup calls adapted to Registry: "
                + registry.registryLookupCalls());
        System.out.println("  Registry#get(ResourceKey) SRG names adapted: "
                + registry.registryResourceKeyLookupCalls());
        System.out.println("  Menu factory lambda buffer signatures adapted: "
                + registry.menuFactoryLambdaAdaptations());

        Forge1201MenuAdapter.Stats menu = bytecode.menuAdapters();
        System.out.println("  NetworkHooks.openScreen calls adapted to ServerPlayer#openMenu: "
                + menu.menuOpenCallsAdapted());
        System.out.println("  Menu extra-data writer lambdas adapted: "
                + menu.extraDataWriterLambdasAdapted());
        System.out.println("  Menu-opening calls skipped (unrecognized writer shape): "
                + menu.menuOpenCallsSkipped());

        printTypes("Types requiring semantic adapters (left unchanged)", bytecode.manualAdapterTypes());
        printTypes("Unreviewed Forge API types (left unchanged)", bytecode.unmappedForgeApiTypes());

        System.out.println();
        System.out.println("WARNING: This is a preliminary mechanical transformation only.");
        System.out.println("Manual Forge API adapters, Minecraft 1.20.1 API changes, mod bootstrap,");
        System.out.println("networking, registration, and runtime behavior are not yet completed.");
        System.out.println("Do not add this artifact to a playable modpack or enable automatic discovery.");
    }

    private static void printTypes(String heading, List<String> types) {
        System.out.println(heading + ": " + types.size());
        for (String type : types) System.out.println("  " + type);
    }
}
