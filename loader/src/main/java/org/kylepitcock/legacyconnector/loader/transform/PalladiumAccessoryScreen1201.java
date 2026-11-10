package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * Palladium 4.5.10's AccessoryScreen extends OptionsSubScreen and builds its
 * own accessory widgets in its legacy init() override. Minecraft 1.21.1 added
 * the abstract OptionsSubScreen.addOptions() hook, which its init() invokes
 * before returning to AccessoryScreen.init(). The old screen has no such
 * method, so opening it otherwise throws AbstractMethodError.
 *
 * The old Palladium constructor also passes null for Options because it never
 * used the vanilla OptionsSubScreen list. Minecraft 1.21.1 now dereferences
 * that argument while building the screen's content (for example, narrator
 * options), so pass Minecraft.getInstance().options instead.
 *
 * Palladium's slot list, accessory list, and rotation slider remain in its
 * existing init() implementation; the new vanilla options list stays empty.
 */
final class PalladiumAccessoryScreen1201 implements Opcodes {
    static final String ACCESSORY_SCREEN =
            "net/threetag/palladium/client/screen/AccessoryScreen";

    private static final String OPTIONS_SUB_SCREEN =
            "net/minecraft/client/gui/screens/options/OptionsSubScreen";
    private static final String SCREEN =
            "net/minecraft/client/gui/screens/Screen";
    private static final String MINECRAFT =
            "net/minecraft/client/Minecraft";
    private static final String OPTIONS =
            "net/minecraft/client/Options";
    private static final String COMPONENT =
            "net/minecraft/network/chat/Component";
    private static final String MUTABLE_COMPONENT =
            "net/minecraft/network/chat/MutableComponent";
    private static final String ADD_OPTIONS = "addOptions";
    private static final String DESCRIPTOR = "()V";
    private static final String SCREEN_CONSTRUCTOR = "(L" + SCREEN + ";)V";
    private static final String OPTIONS_SUB_SCREEN_CONSTRUCTOR =
            "(L" + SCREEN + ";L" + OPTIONS + ";L" + COMPONENT + ";)V";
    private static final String TRANSLATABLE =
            "(Ljava/lang/String;)L" + MUTABLE_COMPONENT + ";";
    private static final String OPTIONS_FIELD = "L" + OPTIONS + ";";

    private PalladiumAccessoryScreen1201() {
    }

    /**
     * Returns one when either the new abstract hook is supplied or the legacy
     * null Options constructor argument is replaced. An unexpected constructor
     * shape is rejected rather than silently retaining a runtime null.
     */
    static int apply(ClassNode node) {
        if (!ACCESSORY_SCREEN.equals(node.name)) {
            return 0;
        }
        if (!OPTIONS_SUB_SCREEN.equals(node.superName)) {
            throw new IllegalStateException(
                    "Palladium AccessoryScreen no longer extends the expected 1.21.1 OptionsSubScreen");
        }

        boolean changed = supplyCurrentOptions(node);
        changed |= supplyAddOptions(node);
        return changed ? 1 : 0;
    }

    private static boolean supplyAddOptions(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!ADD_OPTIONS.equals(method.name) || !DESCRIPTOR.equals(method.desc)) {
                continue;
            }
            if ((method.access & ACC_ABSTRACT) == 0) {
                return false;
            }

            method.access &= ~ACC_ABSTRACT;
            method.access = (method.access & ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED)) | ACC_PROTECTED;
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.instructions.add(new InsnNode(RETURN));
            method.maxStack = 0;
            method.maxLocals = 1;
            return true;
        }

        MethodNode addOptions = new MethodNode(ACC_PROTECTED, ADD_OPTIONS, DESCRIPTOR, null, null);
        addOptions.instructions.add(new InsnNode(RETURN));
        addOptions.maxStack = 0;
        addOptions.maxLocals = 1;
        node.methods.add(addOptions);
        return true;
    }

    /**
     * 1.20.1 Palladium calls OptionsSubScreen(Screen, null, Component).
     * Replace only that verified null argument with Minecraft.getInstance().options.
     */
    private static boolean supplyCurrentOptions(ClassNode node) {
        MethodNode constructor = null;
        for (MethodNode method : node.methods) {
            if ("<init>".equals(method.name) && SCREEN_CONSTRUCTOR.equals(method.desc)) {
                if (constructor != null) {
                    throw new IllegalStateException("Palladium AccessoryScreen has duplicate Screen constructors");
                }
                constructor = method;
            }
        }
        if (constructor == null) {
            throw new IllegalStateException("Palladium AccessoryScreen is missing its Screen constructor");
        }

        MethodInsnNode superConstructor = null;
        for (AbstractInsnNode instruction : constructor.instructions) {
            if (!(instruction instanceof MethodInsnNode call)
                    || call.getOpcode() != INVOKESPECIAL
                    || !OPTIONS_SUB_SCREEN.equals(call.owner)
                    || !"<init>".equals(call.name)
                    || !OPTIONS_SUB_SCREEN_CONSTRUCTOR.equals(call.desc)) {
                continue;
            }
            if (superConstructor != null) {
                throw new IllegalStateException("Palladium AccessoryScreen has multiple OptionsSubScreen constructor calls");
            }
            superConstructor = call;
        }
        if (superConstructor == null) {
            throw new IllegalStateException("Palladium AccessoryScreen no longer calls the expected OptionsSubScreen constructor");
        }

        AbstractInsnNode componentCallNode = previousReal(superConstructor);
        AbstractInsnNode titleNode = previousReal(componentCallNode);
        AbstractInsnNode optionsNode = previousReal(titleNode);
        if (!(componentCallNode instanceof MethodInsnNode componentCall)
                || componentCall.getOpcode() != INVOKESTATIC
                || !COMPONENT.equals(componentCall.owner)
                || !"translatable".equals(componentCall.name)
                || !TRANSLATABLE.equals(componentCall.desc)
                || !(titleNode instanceof LdcInsnNode title)
                || !"gui.palladium.accessories".equals(title.cst)) {
            throw new IllegalStateException("Palladium AccessoryScreen constructor title/argument layout changed");
        }

        if (optionsNode instanceof FieldInsnNode field
                && field.getOpcode() == GETFIELD
                && MINECRAFT.equals(field.owner)
                && "options".equals(field.name)
                && OPTIONS_FIELD.equals(field.desc)) {
            AbstractInsnNode instanceCallNode = previousReal(optionsNode);
            if (instanceCallNode instanceof MethodInsnNode instanceCall
                    && instanceCall.getOpcode() == INVOKESTATIC
                    && MINECRAFT.equals(instanceCall.owner)
                    && "getInstance".equals(instanceCall.name)
                    && ("()L" + MINECRAFT + ";").equals(instanceCall.desc)) {
                return false; // Already adapted.
            }
            throw new IllegalStateException("Palladium AccessoryScreen options field has an unexpected receiver");
        }

        if (optionsNode == null || optionsNode.getOpcode() != ACONST_NULL) {
            throw new IllegalStateException("Palladium AccessoryScreen Options constructor argument is neither null nor Minecraft.options");
        }

        InsnList replacement = new InsnList();
        replacement.add(new MethodInsnNode(INVOKESTATIC, MINECRAFT, "getInstance",
                "()L" + MINECRAFT + ";", false));
        replacement.add(new FieldInsnNode(GETFIELD, MINECRAFT, "options", OPTIONS_FIELD));
        constructor.instructions.insertBefore(optionsNode, replacement);
        constructor.instructions.remove(optionsNode);
        return true;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }
}
