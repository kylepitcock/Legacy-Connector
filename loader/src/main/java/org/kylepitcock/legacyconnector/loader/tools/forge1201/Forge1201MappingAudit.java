package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reports which Forge 47.x symbols have a reviewed package-relocation candidate and which
 * require a semantic adapter or further research for NeoForge 21.1.x.
 *
 * <p>This is a read-only audit. It does not transform or rewrite the input JAR.</p>
 */
public final class Forge1201MappingAudit {
    public record Result(
            int forgeApiClassCount,
            int minecraftApiClassCount,
            Map<Forge1201MappingCatalog.Kind, Integer> classCounts,
            List<Forge1201MappingCatalog.Decision> decisions,
            List<Forge1201ApiUsageAnalyzer.ApiMemberReference> manualAdapterMethods,
            List<Forge1201ApiUsageAnalyzer.ApiMemberReference> manualAdapterFields) {

        public Result {
            classCounts = Map.copyOf(classCounts);
            decisions = List.copyOf(decisions);
            manualAdapterMethods = List.copyOf(manualAdapterMethods);
            manualAdapterFields = List.copyOf(manualAdapterFields);
        }
    }

    private Forge1201MappingAudit() {}

    public static Result audit(Path jarPath) throws IOException {
        return audit(Forge1201ApiUsageAnalyzer.analyze(jarPath));
    }

    public static Result audit(Forge1201ApiUsageAnalyzer.Analysis analysis) {
        Map<Forge1201MappingCatalog.Kind, Integer> counts =
                new EnumMap<>(Forge1201MappingCatalog.Kind.class);
        for (Forge1201MappingCatalog.Kind kind : Forge1201MappingCatalog.Kind.values()) {
            counts.put(kind, 0);
        }

        List<Forge1201MappingCatalog.Decision> decisions = new ArrayList<>();
        int forgeCount = 0;
        int minecraftCount = 0;
        for (String className : analysis.referencedApiClasses()) {
            Forge1201MappingCatalog.Decision decision = Forge1201MappingCatalog.classify(className);
            decisions.add(decision);
            counts.merge(decision.kind(), 1, Integer::sum);
            if (className.startsWith("net/minecraftforge/")) {
                forgeCount++;
            } else if (className.startsWith("net/minecraft/")) {
                minecraftCount++;
            }
        }

        decisions.sort(java.util.Comparator
                .comparing((Forge1201MappingCatalog.Decision decision) -> decision.kind().ordinal())
                .thenComparing(Forge1201MappingCatalog.Decision::sourceInternalName));

        var manualTypes = Forge1201MappingCatalog.manualAdapterTypes();
        List<Forge1201ApiUsageAnalyzer.ApiMemberReference> manualMethods =
                analysis.methodReferences().stream()
                        .filter(reference -> manualTypes.contains(reference.owner()))
                        .sorted(java.util.Comparator.comparing(
                                Forge1201ApiUsageAnalyzer.ApiMemberReference::display))
                        .toList();
        List<Forge1201ApiUsageAnalyzer.ApiMemberReference> manualFields =
                analysis.fieldReferences().stream()
                        .filter(reference -> manualTypes.contains(reference.owner()))
                        .sorted(java.util.Comparator.comparing(
                                Forge1201ApiUsageAnalyzer.ApiMemberReference::display))
                        .toList();

        return new Result(
                forgeCount,
                minecraftCount,
                counts,
                decisions,
                manualMethods,
                manualFields);
    }

    public static void print(Result result) {
        System.out.println("Forge 1.20.1 -> NeoForge 21.1 mapping audit");
        System.out.println("Mode: read-only; no class files or archive entries were modified.");
        System.out.println("Forge API classes referenced: " + result.forgeApiClassCount());
        System.out.println("Minecraft API classes referenced: " + result.minecraftApiClassCount());
        System.out.println("Classification counts:");
        for (Forge1201MappingCatalog.Kind kind : Forge1201MappingCatalog.Kind.values()) {
            System.out.println("  " + kind + ": " + result.classCounts().getOrDefault(kind, 0));
        }

        System.out.println("\nPackage relocation candidates (not yet applied):");
        result.decisions().stream()
                .filter(decision -> decision.kind()
                        == Forge1201MappingCatalog.Kind.PACKAGE_RELOCATION_CANDIDATE)
                .forEach(decision -> System.out.println("  "
                        + decision.sourceInternalName().replace('/', '.') + " -> "
                        + decision.targetInternalName().replace('/', '.')));

        System.out.println("\nTypes requiring manual adapters or source-level rewrites:");
        result.decisions().stream()
                .filter(decision -> decision.kind()
                        == Forge1201MappingCatalog.Kind.MANUAL_ADAPTER_REQUIRED)
                .forEach(decision -> System.out.println("  "
                        + decision.sourceInternalName().replace('/', '.') + ": " + decision.reason()));

        System.out.println("\nUnmapped Forge API types:");
        result.decisions().stream()
                .filter(decision -> decision.kind()
                        == Forge1201MappingCatalog.Kind.UNMAPPED_FORGE_API)
                .forEach(decision -> System.out.println("  " + decision.sourceInternalName().replace('/', '.')));

        if (!result.manualAdapterMethods().isEmpty()) {
            System.out.println("\nReferenced methods owned by manual-adapter types:");
            result.manualAdapterMethods().forEach(reference -> System.out.println("  " + reference.display()));
        }
        if (!result.manualAdapterFields().isEmpty()) {
            System.out.println("\nReferenced fields owned by manual-adapter types:");
            result.manualAdapterFields().forEach(reference -> System.out.println("  " + reference.display()));
        }

        System.out.println("\nMinecraft 1.20.1 -> 1.21.1 member changes are a separate audit.");
        System.out.println("A package relocation candidate is not proof that its target class or member signatures match.");
        System.out.println("Do not load a transformed JAR based on this report.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException(
                    "Usage: Forge1201MappingAudit <path-to-original-forge-1.20.1-jar>");
        }
        print(audit(Path.of(args[0])));
    }
}
