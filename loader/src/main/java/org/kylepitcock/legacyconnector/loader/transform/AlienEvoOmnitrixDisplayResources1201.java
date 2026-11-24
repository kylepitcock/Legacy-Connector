package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Migrates AlienEvo's Omnitrix preview renderer from GeckoLib 4.8's JSON Gson
 * field owner to the GeckoLib 4.9 owner. This is a field-owner-only rewrite:
 * it preserves the same static Gson instance and all parsing behavior.
 */
final class AlienEvoOmnitrixDisplayResources1201 implements Opcodes {
    static final String TARGET =
            "net/habb/alienevo/client/renderer/OmnitrixDisplayResources";
    static final String MODEL_METHOD = "lambda$model$3";
    static final String MODEL_METHOD_DESC =
            "(Lnet/minecraft/resources/ResourceLocation;)"
                    + "Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;";

    static final String LEGACY_OWNER = "software/bernie/geckolib/util/JsonUtil";
    static final String GECKO_49_OWNER =
            "software/bernie/geckolib/loading/json/typeadapter/KeyFramesAdapter";
    static final String FIELD_NAME = "GEO_GSON";
    static final String FIELD_DESC = "Lcom/google/gson/Gson;";

    private AlienEvoOmnitrixDisplayResources1201() {}

    /**
     * The inspected dev.24 class contains exactly one GEO_GSON GETSTATIC in
     * lambda$model$3. Reject partial/unknown shapes instead of rewriting by
     * owner/name alone across unrelated methods.
     */
    static int apply(ClassNode node) {
        if (!TARGET.equals(node.name)) return 0;

        MethodNode modelLoader = findUniqueMethod(node, MODEL_METHOD, MODEL_METHOD_DESC);
        if (modelLoader == null || (modelLoader.access & ACC_SYNTHETIC) == 0) {
            throw new IllegalStateException(
                    "AlienEvo OmnitrixDisplayResources no longer has its inspected synthetic lambda$model$3 method");
        }

        int legacyReferences = 0;
        int migratedReferences = 0;
        FieldInsnNode legacyReference = null;

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;

            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof FieldInsnNode field)
                        || !FIELD_NAME.equals(field.name) || !FIELD_DESC.equals(field.desc)) {
                    continue;
                }

                if (method != modelLoader) {
                    throw new IllegalStateException(
                            "AlienEvo OmnitrixDisplayResources references GEO_GSON outside lambda$model$3");
                }
                if (field.getOpcode() != GETSTATIC) {
                    throw new IllegalStateException(
                            "AlienEvo OmnitrixDisplayResources GEO_GSON access is not the inspected GETSTATIC");
                }

                if (LEGACY_OWNER.equals(field.owner)) {
                    legacyReferences++;
                    legacyReference = field;
                } else if (GECKO_49_OWNER.equals(field.owner)) {
                    migratedReferences++;
                } else {
                    throw new IllegalStateException(
                            "AlienEvo OmnitrixDisplayResources GEO_GSON has an unknown owner: " + field.owner);
                }
            }
        }

        if (legacyReferences == 1 && migratedReferences == 0) {
            legacyReference.owner = GECKO_49_OWNER;
            return 1;
        }
        if (legacyReferences == 0 && migratedReferences == 1) {
            return 0;
        }

        throw new IllegalStateException(
                "AlienEvo OmnitrixDisplayResources expected one legacy or migrated GEO_GSON field access; found "
                        + legacyReferences + " legacy and " + migratedReferences + " GeckoLib 4.9 references");
    }

    private static MethodNode findUniqueMethod(ClassNode node, String name, String descriptor) {
        MethodNode found = null;
        for (MethodNode method : node.methods) {
            if (!name.equals(method.name) || !descriptor.equals(method.desc)) continue;
            if (found != null) {
                throw new IllegalStateException(
                        "Duplicate AlienEvo OmnitrixDisplayResources." + name + descriptor);
            }
            found = method;
        }
        return found;
    }
}
