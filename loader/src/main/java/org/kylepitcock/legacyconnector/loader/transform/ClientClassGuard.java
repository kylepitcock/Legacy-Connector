package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Mods probed for the client with {@code try { Minecraft.getMinecraft() ... } catch (NoClassDefFoundError e) { server code }} (bspkrsCore's
 * {@code CommonUtils.getMinecraftDir}): on a real dedicated server the client class is absent, so loading it throws NoClassDefFoundError. Here the
 * shim's client classes exist but NeoForge refuses to load them on the server with a RuntimeException ("Attempted to load class ... for invalid dist"),
 * which the mod's handler does not catch. On a dedicated server a call that references a client class inside such a protected range is therefore
 * preceded by {@code JdkCompat.clientClassAbsent(name)}, which throws the NoClassDefFoundError the old server would have thrown. The code flow is
 * unchanged (no new branch targets, so no stack map frames change).
 */
public final class ClientClassGuard {
    private ClientClassGuard() {}

    private static final String HELPER = "org/kylepitcock/legacyconnector/compat/jdk/JdkCompat";

    private static boolean catchesAbsence(String type) {
        return "java/lang/NoClassDefFoundError".equals(type) || "java/lang/LinkageError".equals(type);
    }

    /** @return the number of guards inserted; does nothing on the client */
    public static int apply(ClassNode node, String shimPrefix, String side) {
        if (!"SERVER".equals(side)) return 0;
        String[] clientRoots = {shimPrefix + "net/minecraft/client/", shimPrefix + "cpw/mods/fml/client/", shimPrefix + "net/minecraftforge/client/"};
        int inserted = 0;
        for (MethodNode m : node.methods) {
            if (m.tryCatchBlocks == null || m.tryCatchBlocks.isEmpty() || m.instructions.size() == 0) continue;
            Set<AbstractInsnNode> guarded = new HashSet<>();
            for (TryCatchBlockNode tcb : m.tryCatchBlocks) {
                if (!catchesAbsence(tcb.type)) continue;
                for (AbstractInsnNode insn = tcb.start; insn != null && insn != tcb.end; insn = insn.getNext()) {
                    String owner = clientOwner(insn, clientRoots);
                    if (owner == null) continue;
                    if (guarded.add(insn)) {
                        InsnList guard = new InsnList();
                        guard.add(new LdcInsnNode(owner.replace('/', '.')));
                        guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "clientClassAbsent", "(Ljava/lang/String;)V", false));
                        m.instructions.insertBefore(insn, guard);
                        inserted++;
                    }
                    break; // the first reference decides: the guard throws before anything after it runs
                }
            }
        }
        return inserted;
    }

    private static String clientOwner(AbstractInsnNode insn, String[] roots) {
        String owner = null;
        if (insn instanceof MethodInsnNode mi) owner = mi.owner;
        else if (insn instanceof FieldInsnNode fi) owner = fi.owner;
        else if (insn instanceof TypeInsnNode ti) owner = ti.desc;
        else if (insn instanceof LdcInsnNode l && l.cst instanceof Type t && t.getSort() == Type.OBJECT) owner = t.getInternalName();
        if (owner == null) return null;
        for (String root : roots) if (owner.startsWith(root)) return owner;
        return null;
    }
}
