package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** AM2 draws the book behind its pages by depth. The connector's default background clears depth, so draw the book first. */
final class Am2CompendiumFix {
    private static final String SCREEN = "am2/guis/GuiArcaneCompendium";

    private Am2CompendiumFix() {}

    static void apply(ClassNode node) {
        if (!node.name.equals(SCREEN)) return;
        for (var method : node.methods) {
            if (!method.name.equals("drawScreen") || !method.desc.equals("(IIF)V")) continue;
            MethodInsnNode left = null;
            MethodInsnNode start = null;
            boolean drawsBackdrop = false;
            for (var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.owner.equals(SCREEN) && call.name.equals("drawLeftPage") && call.desc.equals("(II)V")) {
                    left = call;
                } else if (left != null && lighting(call, "disableStandardItemLighting")) {
                    start = call;
                    drawsBackdrop = false;
                } else if (start != null && call.name.equals("drawDefaultBackground") && call.desc.equals("()V")) {
                    drawsBackdrop = true;
                } else if (drawsBackdrop && lighting(call, "enableStandardItemLighting")) {
                    moveBackdrop(method.instructions, left, start, call);
                    return;
                }
            }
        }
    }

    private static boolean lighting(MethodInsnNode call, String name) {
        return call.owner.endsWith("/client/renderer/RenderHelper") && call.name.equals(name) && call.desc.equals("()V");
    }

    private static void moveBackdrop(InsnList code, MethodInsnNode left, MethodInsnNode start, MethodInsnNode end) {
        AbstractInsnNode before = left;
        for (int i = 0; i < 3; i++) {
            do {
                before = before.getPrevious();
            } while (before != null && before.getOpcode() < 0);
            if (before == null || before.getOpcode() != (i == 2 ? Opcodes.ALOAD : Opcodes.ILOAD)) return;
        }
        var backdrop = new InsnList();
        var after = end.getNext();
        for (var current = (AbstractInsnNode) start; current != after; current = current.getNext()) {
            if (current instanceof FrameNode || current instanceof JumpInsnNode) return;
        }
        for (var current = (AbstractInsnNode) start; current != after; ) {
            var next = current.getNext();
            if (current.getOpcode() >= 0) {
                code.remove(current);
                backdrop.add(current);
            }
            current = next;
        }
        code.insertBefore(before, backdrop);
    }
}
