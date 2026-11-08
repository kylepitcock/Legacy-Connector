package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only verifier for the output of {@code :loader:transformForge1201}.
 *
 * <p>Source 1.20.1 SRG member names are normalized to their Mojang-readable
 * names before reviewed vanilla and relocated NeoForge members are compared
 * against the target 1.21.1 class hierarchy. A separate, exact-match allowlist
 * normalizes known 1.21.1 target-side field aliases before source normalization.
 * This is structural verification, not proof of semantic compatibility.</p>
 */
public final class Forge1201TranslatedJarVerifier {
    private Forge1201TranslatedJarVerifier() {}

    static List<Forge1201ApiUsageAnalyzer.ApiMethodCallSite> findCallSitesForUnmappedMethods(
            Forge1201SrgNameMappings.Normalization normalization) {
        if (normalization == null) {
            throw new IllegalArgumentException("Source mapping normalization is required");
        }

        Set<String> unmappedKeys = new HashSet<>();
        for (Forge1201ApiUsageAnalyzer.ApiMemberReference reference
                : normalization.analysis().methodReferences()) {
            if (normalization.unmappedMethods().contains(reference.display())) {
                unmappedKeys.add(methodKey(reference.owner(), reference.name(), reference.descriptor()));
            }
        }

        return normalization.analysis().methodCallSites().stream()
                .filter(callSite -> unmappedKeys.contains(methodKey(
                        callSite.targetOwner(), callSite.targetName(), callSite.targetDescriptor())))
                .toList();
    }

    private static String methodKey(String owner, String name, String descriptor) {
        return owner + "#" + name + descriptor;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || args[0].isBlank() || args[1].isBlank() || args[2].isBlank()) {
            throw new IllegalArgumentException(
                    "Usage: Forge1201TranslatedJarVerifier <translated-jar> <1.20.1-methods.csv> <1.20.1-fields.csv>");
        }

        Path jar = Path.of(args[0]);
        Forge1201SrgNameMappings sourceMappings = Forge1201SrgNameMappings.load(
                Path.of(args[1]), Path.of(args[2]));
        Forge1201ApiUsageAnalyzer.Analysis rawAnalysis =
                Forge1201ApiUsageAnalyzer.analyzeTranslated(jar);
        Forge1211TargetMethodAliases.Normalization targetMethodAliases =
                Forge1211TargetMethodAliases.normalize(rawAnalysis);
        Forge1211TargetFieldAliases.Normalization targetFieldAliases =
                Forge1211TargetFieldAliases.normalize(targetMethodAliases.analysis());
        Forge1201SrgNameMappings.Normalization normalization =
                sourceMappings.normalize(targetFieldAliases.analysis());
        Forge1201TargetApiVerifier.Result result = Forge1201TargetApiVerifier.verify(
                normalization.analysis(),
                internalName -> ClassLoader.getSystemClassLoader().getResourceAsStream(internalName + ".class"));

        String version = System.getProperty("legacyconnector.targetNeoForgeVersion", "unknown");
        System.out.println("Preliminary Forge 1.20.1 -> NeoForge " + version + " translated JAR audit");
        System.out.println("Mode: read-only ASM inspection; no mod or target classes were executed.");
        System.out.println("JAR: " + jar.toAbsolutePath().normalize());
        System.out.println("Classes scanned: " + rawAnalysis.classCount());
        System.out.println("Source member mapping set: Forge MCPConfig "
                + Forge1201SrgNameMappings.SOURCE_MAPPING_VERSION);
        System.out.println("Source namespace: SRG method/field identifiers normalized to 1.20.1 Mojang names.");

        System.out.println("\nTarget-side field aliases (exact "
                + Forge1211TargetFieldAliases.TARGET_MINECRAFT_VERSION + " owner/name/descriptor matches):");
        System.out.println("  applied aliases: " + targetFieldAliases.fieldAliases().size());
        targetFieldAliases.fieldAliases().forEach(value -> System.out.println("  TARGET FIELD ALIAS: " + value));

        System.out.println("\nTarget-side method aliases (exact "
                + Forge1211TargetMethodAliases.TARGET_MINECRAFT_VERSION
                + " owner/name/descriptor matches):");
        System.out.println("  applied aliases: " + targetMethodAliases.methodAliases().size());
        targetMethodAliases.methodAliases().forEach(value ->
                System.out.println("  TARGET METHOD ALIAS: " + value));

        System.out.println("\nSource SRG member-name normalization:");
        System.out.println("  method references normalized: " + normalization.methodMappings().size());
        normalization.methodMappings().forEach(value -> System.out.println("  METHOD MAP: " + value));
        System.out.println("  field references normalized: " + normalization.fieldMappings().size());
        normalization.fieldMappings().forEach(value -> System.out.println("  FIELD MAP: " + value));
        System.out.println("  unmapped source SRG method references: " + normalization.unmappedMethods().size());
        normalization.unmappedMethods().forEach(value -> System.out.println("  UNMAPPED METHOD: " + value));
        List<Forge1201ApiUsageAnalyzer.ApiMethodCallSite> unmappedMethodCallSites =
                findCallSitesForUnmappedMethods(normalization);
        System.out.println("  direct callsites for unmapped methods: " + unmappedMethodCallSites.size());
        unmappedMethodCallSites.forEach(value ->
                System.out.println("  UNMAPPED METHOD CALLSITE: " + value.display()));
        System.out.println("  unmapped source SRG field references: " + normalization.unmappedFields().size());
        normalization.unmappedFields().forEach(value -> System.out.println("  UNMAPPED FIELD: " + value));

        System.out.println("\nReviewed target API classes:");
        System.out.println("  distinct references: " + result.candidateClassCount());
        System.out.println("  target class files found: " + result.verifiedClassCount());
        System.out.println("  missing target classes: " + result.missingTargetClasses().size());
        result.missingTargetClasses().forEach(name ->
                System.out.println("  MISSING CLASS: " + name.replace('/', '.')));

        System.out.println("\nReviewed target API method references (source names normalized):");
        System.out.println("  unique references: " + result.uniqueMethodReferences());
        System.out.println("  exact target/inherited matches: " + result.verifiedMethodReferences());
        System.out.println("  unresolved or changed: " + result.methodIssues().size());
        result.methodIssues().forEach(Forge1201TargetApiVerifier::printIssue);

        System.out.println("\nReviewed target API field references (source names normalized):");
        System.out.println("  unique references: " + result.uniqueFieldReferences());
        System.out.println("  exact target/inherited matches: " + result.verifiedFieldReferences());
        System.out.println("  unresolved or changed: " + result.fieldIssues().size());
        result.fieldIssues().forEach(Forge1201TargetApiVerifier::printIssue);

        List<String> remainingForgeTypes = rawAnalysis.referencedApiClasses().stream()
                .filter(name -> name.startsWith("net/minecraftforge/"))
                .sorted()
                .toList();
        List<String> manualTypes = remainingForgeTypes.stream()
                .filter(name -> Forge1201MappingCatalog.classify(name).kind()
                        == Forge1201MappingCatalog.Kind.MANUAL_ADAPTER_REQUIRED)
                .toList();
        List<String> unreviewedTypes = remainingForgeTypes.stream()
                .filter(name -> Forge1201MappingCatalog.classify(name).kind()
                        == Forge1201MappingCatalog.Kind.UNMAPPED_FORGE_API)
                .toList();

        System.out.println("\nForge API references remaining after package relocation:");
        System.out.println("  distinct types remaining: " + remainingForgeTypes.size());
        System.out.println("  known semantic-adapter types: " + manualTypes.size());
        manualTypes.forEach(name -> System.out.println("  MANUAL: " + name.replace('/', '.')));
        System.out.println("  unreviewed Forge API types: " + unreviewedTypes.size());
        unreviewedTypes.forEach(name -> System.out.println("  UNREVIEWED: " + name.replace('/', '.')));

        System.out.println("\nA matching target signature proves only structural compatibility.");
        System.out.println("A remaining issue may be a changed descriptor, rename/replacement, or removed member; review it manually.");
        System.out.println("Do not load this preliminary artifact as a supported mod.");
    }
}
