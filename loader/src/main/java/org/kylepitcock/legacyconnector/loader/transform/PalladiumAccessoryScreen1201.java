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
 * existing init() implementation; the new vanilla options body is omitted.
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
    private static final String ADD_CONTENTS = "addContents";
    private static final String ADD_TITLE = "addTitle";
    private static final String ADD_FOOTER = "addFooter";
    private static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";
    private static final String RENDER = "render";
    private static final String RENDER_DESC = "(L" + GUI_GRAPHICS + ";IIF)V";
    private static final String DIRT_BACKGROUND = "renderDirtBackground";
    private static final String DIRT_BACKGROUND_DESC = "(II)V";
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
        changed |= supplyEmptyChromeHook(node, ADD_CONTENTS);
        changed |= supplyEmptyChromeHook(node, ADD_TITLE);
        changed |= supplyEmptyChromeHook(node, ADD_FOOTER);
        changed |= removeLegacyDirtBackgroundCall(node);
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
     * The legacy screen constructs its own lists and draws its own title and Done
     * button. The 1.21.1 OptionsSubScreen hooks would otherwise create an
     * unused vanilla OptionsList and add duplicate title/footer widgets.
     */
    private static boolean supplyEmptyChromeHook(ClassNode node, String hookName) {
        for (MethodNode method : node.methods) {
            if (!hookName.equals(method.name) || !DESCRIPTOR.equals(method.desc)) {
                continue;
            }
            int visibility = method.access & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
            if (visibility == ACC_PROTECTED
                    && (method.access & (ACC_ABSTRACT | ACC_STATIC)) == 0
                    && method.instructions != null
                    && method.instructions.size() == 1
                    && method.instructions.getFirst().getOpcode() == RETURN) {
                return false; // Already adapted.
            }
            throw new IllegalStateException(
                    "Palladium AccessoryScreen has an unexpected existing " + hookName + "() override");
        }

        MethodNode hook = new MethodNode(ACC_PROTECTED, hookName, DESCRIPTOR, null, null);
        hook.instructions.add(new InsnNode(RETURN));
        hook.maxStack = 0;
        hook.maxLocals = 1;
        node.methods.add(hook);
        return true;
    }

    /**
     * The legacy render method calls Screen.renderBackground() and then paints
     * its own opaque tiled dirt background. 1.21.1's background already
     * supplies the modern in-world blur/dimming, so remove only the verified
     * legacy renderDirtBackground(0, 160) call.
     */
    private static boolean removeLegacyDirtBackgroundCall(ClassNode node) {
        MethodNode render = null;
        for (MethodNode method : node.methods) {
            if (RENDER.equals(method.name) && RENDER_DESC.equals(method.desc)) {
                if (render != null) {
                    throw new IllegalStateException("Palladium AccessoryScreen has duplicate render methods");
                }
                render = method;
            }
        }
        if (render == null) {
            throw new IllegalStateException("Palladium AccessoryScreen is missing its 1.21.1 GuiGraphics render method");
        }

        MethodInsnNode dirtCall = null;
        for (AbstractInsnNode instruction : render.instructions) {
            if (!(instruction instanceof MethodInsnNode call)
                    || !ACCESSORY_SCREEN.equals(call.owner)
                    || !DIRT_BACKGROUND.equals(call.name)) {
                continue;
            }
            if (!DIRT_BACKGROUND_DESC.equals(call.desc) || call.getOpcode() != INVOKEVIRTUAL) {
                throw new IllegalStateException("Palladium AccessoryScreen dirt-background call has an unexpected signature");
            }
            if (dirtCall != null) {
                throw new IllegalStateException("Palladium AccessoryScreen has multiple legacy dirt-background calls");
            }
            dirtCall = call;
        }
        if (dirtCall == null) {
            return false; // Already removed.
        }

        AbstractInsnNode yArgument = previousReal(dirtCall);
        AbstractInsnNode xArgument = previousReal(yArgument);
        AbstractInsnNode receiver = previousReal(xArgument);
        if (!(yArgument instanceof IntInsnNode y && y.getOpcode() == SIPUSH && y.operand == 160)
                || !(xArgument instanceof InsnNode x && x.getOpcode() == ICONST_0)
                || !(receiver instanceof VarInsnNode self && self.getOpcode() == ALOAD && self.var == 0)) {
            throw new IllegalStateException(
                    "Palladium AccessoryScreen dirt-background call no longer uses the verified (0, 160) arguments");
        }

        render.instructions.remove(receiver);
        render.instructions.remove(xArgument);
        render.instructions.remove(yArgument);
        render.instructions.remove(dirtCall);
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
