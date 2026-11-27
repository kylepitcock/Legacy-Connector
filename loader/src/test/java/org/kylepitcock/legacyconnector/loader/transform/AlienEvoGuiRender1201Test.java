package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
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
    private static final String PREVIEW_DRAG_DESC = "(DDI)Z";
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

        assertEquals(1, AlienEvoVertexConsumer1201.apply(node),
                "the existing active per-class adapter must replace the too-small generic viewport");
        assertEquals(AlienEvoGuiRender1201.INVENTORY_COMPAT, redirected.owner);
        assertEquals(INVOKESTATIC, redirected.getOpcode());
        assertEquals(0, AlienEvoMolang1201.apply(node),
                "the active preview adapter must remain idempotent");
    }

    @Test
    void removesOnlyTheBadgeLegacyBackgroundCallAndKeepsOtherRenderWork() {
        ClassNode node = badgeScreen();

        assertEquals(3, AlienEvoVertexConsumer1201.apply(node),
                "the active adapter must remove the legacy call, suppress Screen.render's modern blur hook, and add the opt-in Badge trace");

        MethodNode render = find(node, "render", RENDER_DESC);
        assertFalse(hasCall(render, INVOKEVIRTUAL, node.name, "renderBackground", BACKGROUND_DESC));
        assertFalse(hasCall(render, INVOKESPECIAL, AlienEvoGuiRender1201.SCALED_SCREEN, "renderBackground", BACKGROUND_DESC));
        assertTrue(hasCall(render, INVOKESPECIAL, AlienEvoGuiRender1201.SCALED_SCREEN, "render", RENDER_DESC),
                "the inherited widget pass must remain");
        assertTrue(hasCall(render, INVOKESTATIC, "example/BadgeBackdrop", "draw", "()V"),
                "the Badge's own backdrop must remain");
        assertBadgeModernBackgroundIsNoop(node);
        assertBadgeScreenRenderTraceIsPresent(node);

        assertEquals(0, AlienEvoGuiRender1201.apply(node),
                "the Badge background removal must be idempotent");
    }

    @Test
    void acceptsBadgeBackgroundAlreadyRedirectedToClientCompatNoOp() {
        ClassNode node = badgeScreen();
        MethodNode render = find(node, "render", RENDER_DESC);
        MethodInsnNode legacy = findCall(render, INVOKEVIRTUAL, node.name, "renderBackground", BACKGROUND_DESC);
        assertTrue(legacy != null);
        render.instructions.set(legacy, new MethodInsnNode(INVOKESTATIC,
                AlienEvoGuiRender1201.CLIENT_COMPAT,
                "renderBackground",
                AlienEvoGuiRender1201.MIGRATED_BACKGROUND_DESC,
                false));

        assertEquals(2, AlienEvoGuiRender1201.apply(node),
                "the modern inherited Screen.render hook must be suppressed and the opt-in Badge trace added");
        assertTrue(hasCall(render, INVOKESTATIC,
                AlienEvoGuiRender1201.CLIENT_COMPAT,
                "renderBackground",
                AlienEvoGuiRender1201.MIGRATED_BACKGROUND_DESC));
        assertBadgeModernBackgroundIsNoop(node);
        assertBadgeScreenRenderTraceIsPresent(node);
        assertEquals(0, AlienEvoGuiRender1201.apply(node),
                "the modern no-op override must be idempotent");
    }

    @Test
    void preservesAnExistingNoopModernBadgeBackgroundOverride() {
        ClassNode node = badgeScreen();
        MethodNode background = new MethodNode(ACC_PUBLIC, "renderBackground",
                AlienEvoGuiRender1201.MODERN_BACKGROUND_DESC, null, null);
        background.instructions.add(new InsnNode(RETURN));
        background.maxLocals = 5;
        node.methods.add(background);

        assertEquals(2, AlienEvoVertexConsumer1201.apply(node),
                "the legacy call should be removed and the opt-in Badge render trace added");
        assertBadgeModernBackgroundIsNoop(node);
        assertBadgeScreenRenderTraceIsPresent(node);
        assertEquals(0, AlienEvoGuiRender1201.apply(node));
    }

    @Test
    void rejectsAnUnexpectedModernBadgeBackgroundOverrideBody() {
        ClassNode node = badgeScreen();
        MethodNode background = new MethodNode(ACC_PUBLIC, "renderBackground",
                AlienEvoGuiRender1201.MODERN_BACKGROUND_DESC, null, null);
        background.instructions.add(new InsnNode(ICONST_0));
        background.instructions.add(new InsnNode(POP));
        background.instructions.add(new InsnNode(RETURN));
        background.maxLocals = 5;
        node.methods.add(background);

        assertThrows(IllegalStateException.class, () -> AlienEvoGuiRender1201.apply(node));
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


    @Test
    void allowsDraggingThePreviewOnEveryPreviewTab() {
        ClassNode node = powerScreen();
        MethodNode dragStart = find(node, "handlePreviewDragStart", PREVIEW_DRAG_DESC);
        MethodInsnNode gate = findCall(dragStart, INVOKEVIRTUAL,
                AlienEvoGuiRender1201.POWER_SCREEN, "isColorTab", "()Z");
        assertTrue(gate != null);

        assertEquals(1, AlienEvoGuiRender1201.apply(node));
        assertEquals("isPreviewTab", gate.name,
                "the visible preview is rendered on preview tabs, not just the Color tab");
        assertEquals(0, AlienEvoGuiRender1201.apply(node),
                "preview drag gate adaptation must be idempotent");
    }

    @Test
    void rejectsUnexpectedOmnitrixPreviewDragGateShape() {
        ClassNode node = powerScreen();
        MethodNode dragStart = find(node, "handlePreviewDragStart", PREVIEW_DRAG_DESC);
        MethodInsnNode gate = findCall(dragStart, INVOKEVIRTUAL,
                AlienEvoGuiRender1201.POWER_SCREEN, "isColorTab", "()Z");
        assertTrue(gate != null);
        gate.name = "isSkillsTab";
        assertThrows(IllegalStateException.class, () -> AlienEvoGuiRender1201.apply(node));
    }

    private static ClassNode powerScreen() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = AlienEvoGuiRender1201.POWER_SCREEN;
        node.superName = "java/lang/Object";

        MethodNode dragStart = new MethodNode(ACC_PRIVATE, "handlePreviewDragStart",
                PREVIEW_DRAG_DESC, null, null);
        dragStart.instructions.add(new VarInsnNode(ALOAD, 0));
        dragStart.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                AlienEvoGuiRender1201.POWER_SCREEN, "isColorTab", "()Z", false));
        dragStart.instructions.add(new InsnNode(IRETURN));
        dragStart.maxLocals = 4;
        dragStart.maxStack = 1;
        node.methods.add(dragStart);
        return node;
    }

    private static void assertBadgeScreenRenderTraceIsPresent(ClassNode node) {
        MethodNode render = find(node, "render", RENDER_DESC);
        MethodInsnNode trace = findCall(render, INVOKESTATIC,
                AlienEvoGuiRender1201.RUNTIME_DIAGNOSTICS,
                "traceBadgeScreenRender",
                AlienEvoGuiRender1201.BADGE_RENDER_TRACE_DESC);
        assertTrue(trace != null,
                "Badge render must report its screen state and direct call sites when diagnostics are enabled");

        boolean includesBackdrop = false;
        for (var instruction : render.instructions.toArray()) {
            if (instruction instanceof LdcInsnNode literal
                    && literal.cst instanceof String value
                    && value.contains("example/BadgeBackdrop#draw()V")) {
                includesBackdrop = true;
                break;
            }
        }
        assertTrue(includesBackdrop, "the trace must include the Badge render call-site summary");
    }

    private static void assertBadgeModernBackgroundIsNoop(ClassNode node) {
        MethodNode background = find(node, "renderBackground", AlienEvoGuiRender1201.MODERN_BACKGROUND_DESC);
        assertTrue(background != null, "Badge must override the modern Screen background hook");
        assertTrue((background.access & ACC_PUBLIC) != 0, "Screen background hook override must be public");
        int executable = 0;
        for (var instruction : background.instructions.toArray()) {
            if (instruction.getOpcode() < 0) continue;
            assertEquals(RETURN, instruction.getOpcode(), "modern Badge background hook must be a no-op");
            executable++;
        }
        assertEquals(1, executable, "modern Badge background hook must contain only RETURN");
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
