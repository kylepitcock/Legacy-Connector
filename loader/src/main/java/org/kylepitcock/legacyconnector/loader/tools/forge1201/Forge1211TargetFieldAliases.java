package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Narrow allowlist of known 1.21.1 target-side SRG field aliases.
 *
 * <p>This is deliberately separate from {@link Forge1201SrgNameMappings}, which normalizes
 * source 1.20.1 identifiers. Every alias is keyed by exact target owner, field name, and
 * descriptor; this is not a complete 1.21.1 mapping table.</p>
 */
final class Forge1211TargetFieldAliases {
    static final String TARGET_MINECRAFT_VERSION = "1.21.1";

    private static final String RESOURCE_KEY_DESCRIPTOR =
            "Lnet/minecraft/resources/ResourceKey;";
    private static final String HOLDER_DESCRIPTOR =
            "Lnet/minecraft/core/Holder;";
    private static final Map<FieldKey, String> FIELD_ALIASES = Map.of(
            new FieldKey(
                    "net/minecraft/world/item/enchantment/Enchantments",
                    "f_316758_",
                    RESOURCE_KEY_DESCRIPTOR),
            "EFFICIENCY",
            new FieldKey(
                    "net/minecraft/world/entity/ai/attributes/Attributes",
                    "f_316806_",
                    HOLDER_DESCRIPTOR),
            "STEP_HEIGHT");

    private Forge1211TargetFieldAliases() {}

    static Normalization normalize(Forge1201ApiUsageAnalyzer.Analysis targetAnalysis) {
        if (targetAnalysis == null) {
            throw new IllegalArgumentException("Target API analysis is required");
        }

        List<String> appliedAliases = new ArrayList<>();
        List<Forge1201ApiUsageAnalyzer.ApiMemberReference> fields =
                targetAnalysis.fieldReferences().stream()
                        .map(reference -> {
                            String alias = aliasFor(
                                    reference.owner(), reference.name(), reference.descriptor());
                            if (alias == null) return reference;

                            appliedAliases.add(displayAlias(
                                    reference.owner(), reference.name(), reference.descriptor(), alias));
                            return new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                    reference.owner(), alias, reference.descriptor(), reference.count());
                        })
                        .toList();

        List<Forge1201ApiUsageAnalyzer.ApiFieldCallSite> fieldCallSites =
                targetAnalysis.fieldCallSites().stream()
                        .map(callSite -> {
                            String alias = aliasFor(
                                    callSite.targetOwner(), callSite.targetName(), callSite.targetDescriptor());
                            if (alias == null) return callSite;

                            return new Forge1201ApiUsageAnalyzer.ApiFieldCallSite(
                                    callSite.callerClass(), callSite.callerMethod(), callSite.callerDescriptor(),
                                    callSite.targetOwner(), alias, callSite.targetDescriptor(),
                                    callSite.opcode(), callSite.count());
                        })
                        .toList();

        Forge1201ApiUsageAnalyzer.Analysis normalized =
                new Forge1201ApiUsageAnalyzer.Analysis(
                        targetAnalysis.metadata(),
                        targetAnalysis.classCount(),
                        targetAnalysis.modEntrypoints(),
                        targetAnalysis.mixinConfigs(),
                        targetAnalysis.coreModClass(),
                        targetAnalysis.packageReferences(),
                        targetAnalysis.referencedApiClasses(),
                        targetAnalysis.methodCallSites(),
                        fieldCallSites,
                        targetAnalysis.methodReferences(),
                        fields);

        appliedAliases.sort(String::compareTo);
        return new Normalization(normalized, appliedAliases);
    }

    private static String aliasFor(String owner, String name, String descriptor) {
        return FIELD_ALIASES.get(new FieldKey(owner, name, descriptor));
    }

    private static String displayAlias(String owner, String name, String descriptor, String alias) {
        return owner.replace('/', '.') + "#" + name + ":" + descriptor + " -> " + alias;
    }

    private record FieldKey(String owner, String name, String descriptor) {}

    record Normalization(
            Forge1201ApiUsageAnalyzer.Analysis analysis,
            List<String> fieldAliases) {
        Normalization {
            fieldAliases = List.copyOf(fieldAliases);
        }
    }
}
