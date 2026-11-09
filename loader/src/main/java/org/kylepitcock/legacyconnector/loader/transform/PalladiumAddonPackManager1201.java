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
    // CallRewrites1201 runs immediately before this adapter and normally has
    // already replaced this exact constructor with Compat1201.folderRepositorySource.
    private static final String COMPAT_HELPERS = "legacyconnector/v1_20_1/Compat1201";
    private static final String COMPAT_FACTORY_DESCRIPTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;)"
                    + "Lnet/minecraft/server/packs/repository/FolderRepositorySource;" ;

    private PalladiumAddonPackManager1201() {
    }

    /** Rewrite only Palladium's own AddonPackManager class and its old constructor call. */
    static int apply(ClassNode node) {
        if (!ADDON_PACK_MANAGER.equals(node.name)) return 0;

        int rewritten = 0;
        boolean alreadyModern = false;
        boolean alreadyAdapted = false;
        for (MethodNode method : node.methods) {
            for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode call)) {
                    continue;
                }
                if (call.getOpcode() == Opcodes.INVOKESTATIC
                        && COMPAT_HELPERS.equals(call.owner)
                        && "folderRepositorySource".equals(call.name)
                        && COMPAT_FACTORY_DESCRIPTOR.equals(call.desc)) {
                    // The general call rewriter has already supplied the game-root symlink validator.
                    alreadyAdapted = true;
                    continue;
                }
                if (call.getOpcode() != Opcodes.INVOKESPECIAL
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

        if (rewritten == 0 && !alreadyModern && !alreadyAdapted) {
            throw new IllegalStateException(
                    "Palladium AddonPackManager has no recognized FolderRepositorySource migration form");
        }
        return rewritten;
    }
}
