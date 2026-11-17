package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Minecraft 1.2.5 single player had ONE world, and its {@code isRemote} was false. Here a mod reaches the client world through
 * {@code Minecraft.theWorld} even when its code runs on the integrated server's thread (an item callback, a block or tile entity
 * tick), and that world says {@code isRemote == true}: a mod that opens with {@code if (mc.theWorld.isRemote) return;}
 * (Backpack's storage reload) never runs. In 1.2.5 jars every read of {@code World.isRemote} becomes
 * {@code Mc125Host.isRemote(world)}, which answers false for the single-player client world while a server thread reads it.
 */
public final class SingleWorldRemote {
    private SingleWorldRemote() {}

    private static final String WORLD = "legacyconnector/v1_7_10/net/minecraft/world/World";
    private static final String HOST = "org/kylepitcock/legacyconnector/v1_6_4/bridge/Mc125Host";

    public static int apply(ClassNode node) {
        int n = 0;
        for (MethodNode m : node.methods) {
            if (m.instructions == null) continue;
            for (AbstractInsnNode insn : m.instructions.toArray()) {
                if (insn instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETFIELD && f.name.equals("isRemote") && f.desc.equals("Z")
                        && isWorld(f.owner)) {
                    m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOST, "isRemote", "(L" + WORLD + ";)Z", false));
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean isWorld(String owner) {
        return owner.endsWith("net/minecraft/world/World") || owner.endsWith("net/minecraft/world/WorldServer")
                || owner.endsWith("net/minecraft/client/multiplayer/WorldClient");
    }
}
