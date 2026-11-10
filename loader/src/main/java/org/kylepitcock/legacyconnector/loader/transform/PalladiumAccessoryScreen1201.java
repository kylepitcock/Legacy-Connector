package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Palladium 4.5.10's AccessoryScreen extends OptionsSubScreen and builds its
 * own accessory widgets in its legacy init() override. Minecraft 1.21.1 added
 * the abstract OptionsSubScreen.addOptions() hook, which its init() invokes
 * before returning to AccessoryScreen.init(). The old screen has no such
 * method, so opening it otherwise throws AbstractMethodError.
 *
 * The screen does not use the vanilla OptionInstance list: its slot list,
 * accessory list, and rotation slider are created by its existing init()
 * implementation. This compatibility hook therefore intentionally leaves
 * the new vanilla options list empty and lets the original UI continue.
 */
final class PalladiumAccessoryScreen1201 implements Opcodes {
    static final String ACCESSORY_SCREEN =
            "net/threetag/palladium/client/screen/AccessoryScreen";

    private static final String OPTIONS_SUB_SCREEN =
            "net/minecraft/client/gui/screens/options/OptionsSubScreen";
    private static final String ADD_OPTIONS = "addOptions";
    private static final String DESCRIPTOR = "()V";

    private PalladiumAccessoryScreen1201() {
    }

    /**
     * Returns one when the 1.21.1 abstract hook is supplied, or zero when the
     * class is not Palladium's AccessoryScreen or already has a concrete hook.
     * A changed superclass is rejected so this exact migration cannot silently
     * apply to a different screen architecture.
     */
    static int apply(ClassNode node) {
        if (!ACCESSORY_SCREEN.equals(node.name)) {
            return 0;
        }
        if (!OPTIONS_SUB_SCREEN.equals(node.superName)) {
            throw new IllegalStateException(
                    "Palladium AccessoryScreen no longer extends the expected 1.21.1 OptionsSubScreen");
        }

        for (MethodNode method : node.methods) {
            if (!ADD_OPTIONS.equals(method.name) || !DESCRIPTOR.equals(method.desc)) {
                continue;
            }
            if ((method.access & ACC_ABSTRACT) == 0) {
                return 0;
            }

            method.access &= ~ACC_ABSTRACT;
            method.access = (method.access & ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED)) | ACC_PROTECTED;
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.instructions.add(new InsnNode(RETURN));
            method.maxStack = 0;
            method.maxLocals = 1;
            return 1;
        }

        MethodNode addOptions = new MethodNode(ACC_PROTECTED, ADD_OPTIONS, DESCRIPTOR, null, null);
        addOptions.instructions.add(new InsnNode(RETURN));
        addOptions.maxStack = 0;
        addOptions.maxLocals = 1;
        node.methods.add(addOptions);
        return 1;
    }
}
