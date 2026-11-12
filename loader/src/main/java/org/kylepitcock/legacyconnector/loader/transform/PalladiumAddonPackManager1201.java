package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Palladium 4.5.10 addon-pack compatibility for 1.21.1.
 *
 * The folder repository constructor gained DirectoryValidator. In addition,
 * the old manager mutated Pack.id and Pack.required after discovery; 1.21.1
 * models those values as immutable PackLocationInfo and PackSelectionConfig.
 */
final class PalladiumAddonPackManager1201 implements Opcodes {
    static final String ADDON_PACK_MANAGER = "net/threetag/palladium/addonpack/AddonPackManager";

    private static final String FOLDER_REPOSITORY_SOURCE =
            "net/minecraft/server/packs/repository/FolderRepositorySource";
    private static final String PACK = "net/minecraft/server/packs/repository/Pack";
    private static final String CONSUMER = "java/util/function/Consumer";
    private static final String PACK_WRAPPER_METHOD = "lambda$getWrappedPackFinder$0";
    private static final String PACK_WRAPPER_DESC =
            "(Ljava/util/function/Consumer;L" + PACK + ";)V";

    private static final String LEGACY_CONSTRUCTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;)V";
    private static final String TARGET_CONSTRUCTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;"
                    + "Lnet/minecraft/world/level/validation/DirectoryValidator;)V";
    private static final String VALIDATOR_HELPER =
            "legacyconnector/v1_20_1/PalladiumAddonPackCompat";
    private static final String PACK_HELPER = "legacyconnector/v1_20_1/PalladiumAddonPackCompat";
    private static final String PACK_HELPER_NAME = "prefixAndRequireAddonPack";
    private static final String PACK_HELPER_DESC = "(L" + PACK + ";)L" + PACK + ";";

    // CallRewrites1201 runs immediately before this adapter and may already
    // replace the old FolderRepositorySource constructor with Compat1201.
    private static final String COMPAT_HELPERS = "legacyconnector/v1_20_1/Compat1201";
    private static final String COMPAT_FACTORY_DESCRIPTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;)"
                    + "Lnet/minecraft/server/packs/repository/FolderRepositorySource;";

    private PalladiumAddonPackManager1201() {
    }

    /** Rewrite only Palladium's own AddonPackManager constructor and pack-discovery callback. */
    static int apply(ClassNode node) {
        if (!ADDON_PACK_MANAGER.equals(node.name)) return 0;

        int rewritten = 0;
        boolean alreadyModernConstructor = false;
        boolean alreadyAdaptedConstructor = false;

        for (MethodNode method : node.methods) {
            for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;

                if (call.getOpcode() == INVOKESTATIC
                        && COMPAT_HELPERS.equals(call.owner)
                        && "folderRepositorySource".equals(call.name)
                        && COMPAT_FACTORY_DESCRIPTOR.equals(call.desc)) {
                    alreadyAdaptedConstructor = true;
                    continue;
                }

                if (call.getOpcode() != INVOKESPECIAL
                        || !FOLDER_REPOSITORY_SOURCE.equals(call.owner)
                        || !"<init>".equals(call.name)) {
                    continue;
                }

                if (LEGACY_CONSTRUCTOR.equals(call.desc)) {
                    InsnList validator = new InsnList();
                    validator.add(new MethodInsnNode(
                            INVOKESTATIC,
                            VALIDATOR_HELPER,
                            "addonPackDirectoryValidator",
                            "()Lnet/minecraft/world/level/validation/DirectoryValidator;",
                            false));
                    method.instructions.insertBefore(call, validator);
                    call.desc = TARGET_CONSTRUCTOR;
                    method.maxStack++;
                    rewritten++;
                } else if (TARGET_CONSTRUCTOR.equals(call.desc)) {
                    alreadyModernConstructor = true;
                }
            }
        }

        MethodNode packCallback = find(node, PACK_WRAPPER_METHOD, PACK_WRAPPER_DESC);
        if (packCallback == null) {
            throw new IllegalStateException(
                    "Palladium AddonPackManager no longer has its inspected addon-pack discovery callback");
        }
        int packCallbackChanges = adaptPackMetadataCallback(packCallback);
        rewritten += packCallbackChanges;

        if (rewritten == 0 && !alreadyModernConstructor && !alreadyAdaptedConstructor) {
            throw new IllegalStateException(
                    "Palladium AddonPackManager has no recognized 1.21.1 constructor or pack-metadata migration form");
        }
        return rewritten;
    }

    /**
     * Replaces the two 1.20.1 writes to Pack.id and Pack.required with a copy
     * built using the 1.21.1 PackLocationInfo and PackSelectionConfig records.
     */
    private static int adaptPackMetadataCallback(MethodNode method) {
        if (hasPackWrapperCall(method)) return 0;

        int idWrites = 0;
        int requiredWrites = 0;
        for (var instruction : method.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode field)
                    || field.getOpcode() != PUTFIELD
                    || !PACK.equals(field.owner)) {
                continue;
            }
            if (("f_10401_".equals(field.name) || "id".equals(field.name))
                    && "Ljava/lang/String;".equals(field.desc)) {
                idWrites++;
            } else if (("f_10407_".equals(field.name) || "required".equals(field.name))
                    && "Z".equals(field.desc)) {
                requiredWrites++;
            }
        }

        if (idWrites != 1 || requiredWrites != 1) {
            throw new IllegalStateException(
                    "Palladium AddonPackManager pack callback expected one Pack.id and one Pack.required write; found "
                            + idWrites + " and " + requiredWrites);
        }

        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;
        method.signature = null;
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new MethodInsnNode(
                INVOKESTATIC, PACK_HELPER, PACK_HELPER_NAME, PACK_HELPER_DESC, false));
        method.instructions.add(new VarInsnNode(ASTORE, 1));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new MethodInsnNode(
                INVOKEINTERFACE, CONSUMER, "accept", "(Ljava/lang/Object;)V", true));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 2;
        method.maxLocals = 2;
        return 1;
    }

    private static boolean hasPackWrapperCall(MethodNode method) {
        boolean helper = false;
        boolean consumer = false;
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && PACK_HELPER.equals(call.owner)
                    && PACK_HELPER_NAME.equals(call.name)
                    && PACK_HELPER_DESC.equals(call.desc)) {
                helper = true;
            }
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKEINTERFACE
                    && CONSUMER.equals(call.owner)
                    && "accept".equals(call.name)
                    && "(Ljava/lang/Object;)V".equals(call.desc)) {
                consumer = true;
            }
        }
        return helper && consumer;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
    }
}
