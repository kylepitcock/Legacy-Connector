package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Palladium's 1.20.1-to-1.21.1 addon-pack folder source constructor migration. */
class PalladiumAddonPackManager1201Test implements Opcodes {
    private static final String FOLDER_SOURCE =
            "net/minecraft/server/packs/repository/FolderRepositorySource";
    private static final String LEGACY_DESCRIPTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;)V";
    private static final String TARGET_DESCRIPTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;"
                    + "Lnet/minecraft/world/level/validation/DirectoryValidator;)V";
    private static final String HELPER_DESCRIPTOR =
            "()Lnet/minecraft/world/level/validation/DirectoryValidator;";

    @Test
    void addonPackManagerUsesMinecraftSymlinkValidatorForFolderRepositorySource() {
        ClassNode node = addonPackManagerNode(LEGACY_DESCRIPTOR);

        assertEquals(1, PalladiumAddonPackManager1201.apply(node));

        MethodNode constructor = node.methods.get(0);
        MethodInsnNode rewritten = findFolderSourceConstructor(constructor);
        assertEquals(TARGET_DESCRIPTOR, rewritten.desc);
        assertTrue(hasCallBefore(constructor, rewritten, INVOKESTATIC,
                "legacyconnector/v1_20_1/PalladiumAddonPackCompat",
                "addonPackDirectoryValidator", HELPER_DESCRIPTOR));
        assertEquals(6, constructor.maxStack, "the validator return value adds one stack slot");
        assertFalse(hasLegacyFolderSourceConstructor(constructor));
    }

    @Test
    void unrelatedClassesAreNotRewritten() {
        ClassNode node = addonPackManagerNode(LEGACY_DESCRIPTOR);
        node.name = "example/UnrelatedManager";

        assertEquals(0, PalladiumAddonPackManager1201.apply(node));
        assertEquals(LEGACY_DESCRIPTOR, findFolderSourceConstructor(node.methods.get(0)).desc);
    }

    @Test
    void alreadyModernFolderRepositorySourceCallIsLeftUnchanged() {
        ClassNode node = addonPackManagerNode(TARGET_DESCRIPTOR);

        assertEquals(0, PalladiumAddonPackManager1201.apply(node));
        assertEquals(TARGET_DESCRIPTOR, findFolderSourceConstructor(node.methods.get(0)).desc);
        assertFalse(hasCallBefore(node.methods.get(0), findFolderSourceConstructor(node.methods.get(0)),
                INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumAddonPackCompat",
                "addonPackDirectoryValidator", HELPER_DESCRIPTOR));
    }

    private static ClassNode addonPackManagerNode(String constructorDescriptor) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumAddonPackManager1201.ADDON_PACK_MANAGER;
        node.superName = "java/lang/Object";

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new TypeInsnNode(NEW, FOLDER_SOURCE));
        constructor.instructions.add(new InsnNode(DUP));
        constructor.instructions.add(new VarInsnNode(ALOAD, 1));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        if (TARGET_DESCRIPTOR.equals(constructorDescriptor)) {
            constructor.instructions.add(new InsnNode(ACONST_NULL));
        }
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, FOLDER_SOURCE, "<init>", constructorDescriptor, false));
        constructor.instructions.add(new InsnNode(POP));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxStack = TARGET_DESCRIPTOR.equals(constructorDescriptor) ? 6 : 5;
        constructor.maxLocals = 2;
        node.methods.add(constructor);
        return node;
    }

    private static MethodInsnNode findFolderSourceConstructor(MethodNode method) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(FOLDER_SOURCE)
                    && call.name.equals("<init>")) return call;
        }
        throw new AssertionError("FolderRepositorySource constructor call missing");
    }

    private static boolean hasLegacyFolderSourceConstructor(MethodNode method) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(FOLDER_SOURCE)
                    && call.name.equals("<init>")
                    && call.desc.equals(LEGACY_DESCRIPTOR)) return true;
        }
        return false;
    }

    private static boolean hasCallBefore(
            MethodNode method, MethodInsnNode target, int opcode, String owner, String name, String descriptor) {
        for (var instruction = method.instructions.getFirst(); instruction != null && instruction != target; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == opcode
                    && call.owner.equals(owner)
                    && call.name.equals(name)
                    && call.desc.equals(descriptor)) return true;
        }
        return false;
    }
}
