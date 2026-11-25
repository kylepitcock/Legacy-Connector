package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for AlienEvo's shared GUI preview and Badge background paths. */
class AlienEvoGuiRender1201Test implements Opcodes {
    private static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";
    private static final String LEGACY_PREVIEW_DESC =
            "(L" + GUI_GRAPHICS + ";IIIFFLnet/minecraft/world/entity/LivingEntity;)V";
    private static final String PREVIEW_METHOD_DESC =
            "(L" + GUI_GRAPHICS + ";IIFFFLnet/minecraft/world/entity/LivingEntity;FZ)V";
    private static final String BACKGROUND_DESC = "(L" + GUI_GRAPHICS + ";)V";
    private static final String RENDER_DESC = "(L" + GUI_GRAPHICS + ";IIF)V";

    @Test
    void sharedInventoryPreviewCallUsesTheTwentyOneCompatBridge() {
        ClassNode node = previewWidget();

        assertEquals(1, AlienEvoGuiRender1201.apply(node));

        MethodNode renderEntity = find(node, "renderEntity", PREVIEW_METHOD_DESC);
        MethodInsnNode bridged = findCall(renderEntity,
                INVOKESTATIC,
                AlienEvoGuiRender1201.INVENTORY_COMPAT,
                "renderEntityInInventoryFollowsMouse",
                LEGACY_PREVIEW_DESC);
        assertTrue(bridged != null);
        assertFalse(hasCall(renderEntity, INVOKESTATIC,
                AlienEvoGuiRender1201.INVENTORY_SCREEN,
                "renderEntityInInventoryFollowsMouse",
                LEGACY_PREVIEW_DESC));

        assertEquals(0, AlienEvoGuiRender1201.apply(node),
                "the shared preview call adapter must be idempotent");
    }

    @Test
    void acceptsPreviewCallAlreadyRedirectedByGenericMinecraftMigration() {
        ClassNode node = previewWidget();
        MethodNode renderEntity = find(node, "renderEntity", PREVIEW_METHOD_DESC);
        MethodInsnNode redirected = findCall(renderEntity, INVOKESTATIC,
                AlienEvoGuiRender1201.INVENTORY_SCREEN,
                "renderEntityInInventoryFollowsMouse", LEGACY_PREVIEW_DESC);
        assertTrue(redirected != null);

        // Mc1201Migration's mc-calls.tsv row rewrites this owner to MiddleClientCompat first.
        redirected.owner = AlienEvoGuiRender1201.MIDDLE_CLIENT_COMPAT;

        assertEquals(0, AlienEvoGuiRender1201.apply(node),
                "the earlier generic migration has already adapted this call");
        assertEquals(AlienEvoGuiRender1201.MIDDLE_CLIENT_COMPAT, redirected.owner);
        assertEquals(INVOKESTATIC, redirected.getOpcode());
        assertEquals(0, AlienEvoGuiRender1201.apply(node),
                "recognizing the generic bridge must remain idempotent");
    }

    @Test
    void removesOnlyTheBadgeLegacyBackgroundCallAndKeepsOtherRenderWork() {
        ClassNode node = badgeScreen();

        assertEquals(1, AlienEvoGuiRender1201.apply(node));

        MethodNode render = find(node, "render", RENDER_DESC);
        assertFalse(hasCall(render, INVOKEVIRTUAL, node.name, "renderBackground", BACKGROUND_DESC));
        assertFalse(hasCall(render, INVOKESPECIAL, AlienEvoGuiRender1201.SCALED_SCREEN, "renderBackground", BACKGROUND_DESC));
        assertTrue(hasCall(render, INVOKESPECIAL, AlienEvoGuiRender1201.SCALED_SCREEN, "render", RENDER_DESC),
                "the inherited widget pass must remain");
        assertTrue(hasCall(render, INVOKESTATIC, "example/BadgeBackdrop", "draw", "()V"),
                "the Badge's own backdrop must remain");

        assertEquals(0, AlienEvoGuiRender1201.apply(node),
                "the Badge background removal must be idempotent");
    }

    @Test
    void rejectsAnUnexpectedBadgeBackgroundStackShape() {
        ClassNode node = badgeScreen();
        MethodNode render = find(node, "render", RENDER_DESC);
        boolean changed = false;
        for (var instruction : render.instructions.toArray()) {
            if (instruction instanceof VarInsnNode variable
                    && variable.getOpcode() == ALOAD && variable.var == 1) {
                variable.var = 2;
                changed = true;
                break;
            }
        }
        assertTrue(changed);
        assertThrows(IllegalStateException.class, () -> AlienEvoGuiRender1201.apply(node));
    }

    @Test
    void rejectsAnUnexpectedInventoryPreviewOwner() {
        ClassNode node = previewWidget();
        MethodNode renderEntity = find(node, "renderEntity", PREVIEW_METHOD_DESC);
        MethodInsnNode call = findCall(renderEntity, INVOKESTATIC,
                AlienEvoGuiRender1201.INVENTORY_SCREEN,
                "renderEntityInInventoryFollowsMouse", LEGACY_PREVIEW_DESC);
        assertTrue(call != null);
        call.owner = "example/OtherInventoryScreen";
        assertThrows(IllegalStateException.class, () -> AlienEvoGuiRender1201.apply(node));
    }

    private static ClassNode previewWidget() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = AlienEvoGuiRender1201.PREVIEW_WIDGET;
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode(ACC_PRIVATE, "renderEntity", PREVIEW_METHOD_DESC, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new VarInsnNode(ILOAD, 2));
        method.instructions.add(new VarInsnNode(ILOAD, 3));
        method.instructions.add(new VarInsnNode(ILOAD, 2));
        method.instructions.add(new VarInsnNode(FLOAD, 4));
        method.instructions.add(new VarInsnNode(FLOAD, 5));
        method.instructions.add(new VarInsnNode(ALOAD, 7));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC,
                AlienEvoGuiRender1201.INVENTORY_SCREEN,
                "renderEntityInInventoryFollowsMouse",
                LEGACY_PREVIEW_DESC,
                false));
        method.instructions.add(new InsnNode(RETURN));
        method.maxLocals = 10;
        method.maxStack = 8;
        node.methods.add(method);
        return node;
    }

    private static ClassNode badgeScreen() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = AlienEvoGuiRender1201.BADGE_SCREEN;
        node.superName = AlienEvoGuiRender1201.SCALED_SCREEN;

        MethodNode render = new MethodNode(ACC_PUBLIC, "render", RENDER_DESC, null, null);
        render.instructions.add(new VarInsnNode(ALOAD, 0));
        render.instructions.add(new VarInsnNode(ALOAD, 1));
        render.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, node.name,
                "renderBackground", BACKGROUND_DESC, false));
        render.instructions.add(new MethodInsnNode(INVOKESTATIC,
                "example/BadgeBackdrop", "draw", "()V", false));
        render.instructions.add(new VarInsnNode(ALOAD, 0));
        render.instructions.add(new VarInsnNode(ALOAD, 1));
        render.instructions.add(new VarInsnNode(ILOAD, 2));
        render.instructions.add(new VarInsnNode(ILOAD, 3));
        render.instructions.add(new VarInsnNode(FLOAD, 4));
        render.instructions.add(new MethodInsnNode(INVOKESPECIAL,
                AlienEvoGuiRender1201.SCALED_SCREEN, "render", RENDER_DESC, false));
        render.instructions.add(new InsnNode(RETURN));
        render.maxLocals = 5;
        render.maxStack = 5;
        node.methods.add(render);
        return node;
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static MethodInsnNode findCall(
            MethodNode method, int opcode, String owner, String name, String descriptor) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == opcode
                    && owner.equals(call.owner)
                    && name.equals(call.name)
                    && descriptor.equals(call.desc)) {
                return call;
            }
        }
        return null;
    }

    private static boolean hasCall(
            MethodNode method, int opcode, String owner, String name, String descriptor) {
        return findCall(method, opcode, owner, name, descriptor) != null;
    }
}
