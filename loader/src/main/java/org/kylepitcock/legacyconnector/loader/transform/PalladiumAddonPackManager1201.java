package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Palladium 4.5.10 was compiled against the 1.20.1 three-argument
 * FolderRepositorySource constructor. In 1.21.1, the constructor requires
 * Minecraft's DirectoryValidator, so retain the folder repository while
 * supplying the validator parsed from the game's allowed_symlinks.txt.
 */
final class PalladiumAddonPackManager1201 {
    static final String ADDON_PACK_MANAGER = "net/threetag/palladium/addonpack/AddonPackManager";

    private static final String FOLDER_REPOSITORY_SOURCE =
            "net/minecraft/server/packs/repository/FolderRepositorySource";
    private static final String LEGACY_CONSTRUCTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;)V";
    private static final String TARGET_CONSTRUCTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;"
                    + "Lnet/minecraft/world/level/validation/DirectoryValidator;)V";
    private static final String VALIDATOR_HELPER =
            "legacyconnector/v1_20_1/PalladiumAddonPackCompat";

    private PalladiumAddonPackManager1201() {
    }

    /** Rewrite only Palladium's own AddonPackManager class and its old constructor call. */
    static int apply(ClassNode node) {
        if (!ADDON_PACK_MANAGER.equals(node.name)) return 0;

        int rewritten = 0;
        boolean alreadyModern = false;
        for (MethodNode method : node.methods) {
            for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || call.getOpcode() != Opcodes.INVOKESPECIAL
                        || !FOLDER_REPOSITORY_SOURCE.equals(call.owner)
                        || !"<init>".equals(call.name)) {
                    continue;
                }

                if (LEGACY_CONSTRUCTOR.equals(call.desc)) {
                    InsnList validator = new InsnList();
                    validator.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            VALIDATOR_HELPER,
                            "addonPackDirectoryValidator",
                            "()Lnet/minecraft/world/level/validation/DirectoryValidator;",
                            false));
                    method.instructions.insertBefore(call, validator);
                    call.desc = TARGET_CONSTRUCTOR;
                    // The returned validator is pushed while the existing constructor operands remain on the stack.
                    method.maxStack++;
                    rewritten++;
                } else if (TARGET_CONSTRUCTOR.equals(call.desc)) {
                    alreadyModern = true;
                }
            }
        }

        if (rewritten == 0 && !alreadyModern) {
            throw new IllegalStateException(
                    "Palladium AddonPackManager no longer contains the inspected FolderRepositorySource constructor call");
        }
        return rewritten;
    }
}
