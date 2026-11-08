package org.kylepitcock.legacyconnector.loader.tools.forge1201;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Exact 1.21.1 target-side SRG method aliases, separate from source 1.20.1 mappings. */
final class Forge1211TargetMethodAliases {
    static final String TARGET_MINECRAFT_VERSION = "1.21.1";

    private static final String RESOURCE_LOCATION = "net/minecraft/resources/ResourceLocation";
    private static final String RESOURCE_LOCATION_DESCRIPTOR =
            "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;";
    private static final String ENTITY_DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";
    private static final String HOLDER = "net/minecraft/core/Holder";
    private static final String HOLDER_VALUE_DESCRIPTOR = "()Ljava/lang/Object;";
    private static final String WITH_EYE_HEIGHT_DESCRIPTOR =
            "(F)Lnet/minecraft/world/entity/EntityDimensions;";
    private static final String FLOAT_ACCESSOR_DESCRIPTOR = "()F";
    private static final String BOOLEAN_ACCESSOR_DESCRIPTOR = "()Z";
    private static final Map<MethodKey, String> METHOD_ALIASES = Map.of(
            new MethodKey(RESOURCE_LOCATION, "m_339182_", RESOURCE_LOCATION_DESCRIPTOR),
            "fromNamespaceAndPath",
            new MethodKey(ENTITY_DIMENSIONS, "m_320568_", WITH_EYE_HEIGHT_DESCRIPTOR),
            "withEyeHeight",
            new MethodKey(ENTITY_DIMENSIONS, "f_20377_", FLOAT_ACCESSOR_DESCRIPTOR),
            "width",
            new MethodKey(ENTITY_DIMENSIONS, "f_20378_", FLOAT_ACCESSOR_DESCRIPTOR),
            "height",
            new MethodKey(ENTITY_DIMENSIONS, "f_20379_", BOOLEAN_ACCESSOR_DESCRIPTOR),
            "fixed",
            new MethodKey(ENTITY_DIMENSIONS, "f_316573_", FLOAT_ACCESSOR_DESCRIPTOR),
            "eyeHeight",
            new MethodKey(HOLDER, "m_203334_", HOLDER_VALUE_DESCRIPTOR),
            "value");

    private Forge1211TargetMethodAliases() {}

    static Normalization normalize(Forge1201ApiUsageAnalyzer.Analysis targetAnalysis) {
        if (targetAnalysis == null) {
            throw new IllegalArgumentException("Target API analysis is required");
        }

        List<String> appliedAliases = new ArrayList<>();
        List<Forge1201ApiUsageAnalyzer.ApiMemberReference> methods =
                targetAnalysis.methodReferences().stream()
                        .map(reference -> {
                            String alias = aliasFor(reference.owner(), reference.name(), reference.descriptor());
                            if (alias == null) return reference;
                            appliedAliases.add(displayAlias(
                                    reference.owner(), reference.name(), reference.descriptor(), alias));
                            return new Forge1201ApiUsageAnalyzer.ApiMemberReference(
                                    reference.owner(), alias, reference.descriptor(), reference.count());
                        })
                        .toList();
        List<Forge1201ApiUsageAnalyzer.ApiMethodCallSite> callSites =
                targetAnalysis.methodCallSites().stream()
                        .map(callSite -> {
                            String alias = aliasFor(
                                    callSite.targetOwner(), callSite.targetName(), callSite.targetDescriptor());
                            if (alias == null) return callSite;
                            return new Forge1201ApiUsageAnalyzer.ApiMethodCallSite(
                                    callSite.callerClass(), callSite.callerMethod(), callSite.callerDescriptor(),
                                    callSite.targetOwner(), alias, callSite.targetDescriptor(),
                                    callSite.opcode(), callSite.count());
                        })
                        .toList();

        Forge1201ApiUsageAnalyzer.Analysis normalized = new Forge1201ApiUsageAnalyzer.Analysis(
                targetAnalysis.metadata(),
                targetAnalysis.classCount(),
                targetAnalysis.modEntrypoints(),
                targetAnalysis.mixinConfigs(),
                targetAnalysis.coreModClass(),
                targetAnalysis.packageReferences(),
                targetAnalysis.referencedApiClasses(),
                callSites,
                targetAnalysis.fieldCallSites(),
                methods,
                targetAnalysis.fieldReferences());
        appliedAliases.sort(String::compareTo);
        return new Normalization(normalized, appliedAliases);
    }

    static boolean isTargetApiOwner(String owner) {
        return owner != null && METHOD_ALIASES.keySet().stream()
                .anyMatch(key -> key.owner().equals(owner));
    }

    private static String aliasFor(String owner, String name, String descriptor) {
        return METHOD_ALIASES.get(new MethodKey(owner, name, descriptor));
    }

    private static String displayAlias(String owner, String name, String descriptor, String alias) {
        return owner.replace('/', '.') + "#" + name + descriptor + " -> " + alias + descriptor;
    }

    private record MethodKey(String owner, String name, String descriptor) {}

    record Normalization(
            Forge1201ApiUsageAnalyzer.Analysis analysis,
            List<String> methodAliases) {
        Normalization {
            methodAliases = List.copyOf(methodAliases);
        }
    }
}
