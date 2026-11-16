package org.kylepitcock.legacyconnector.loader.transform;

import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Beta mods lived in the game's default package and found each other's classes by bare name ({@code Class.forName("DimensionTwilightForest")},
 * {@code Class.forName("mod_IC2")}); the layer gives every mod its own package. {@code Class.forName} calls of a Beta mod pass their own class to
 * {@code ClassNamesB173}, which finds a bare name in that package, in another loaded Beta mod's package or in the API shim.
 */
public final class BetaClassForName implements Opcodes {
    private static final String HELPER = "org/kylepitcock/legacyconnector/vb1_7_3/ClassNamesB173";

    private BetaClassForName() {}

    public static int apply(ClassNode node, LegacyVersion version) {
        if (version != LegacyVersion.VB1_7_3 || node.name.startsWith("legacyconnector/") || node.name.startsWith("legacyconnector_synth/")) return 0;
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof MethodInsnNode call) || call.getOpcode() != INVOKESTATIC || !call.owner.equals("java/lang/Class") || !call.name.equals("forName")) continue;
                String desc;
                if (call.desc.equals("(Ljava/lang/String;)Ljava/lang/Class;")) desc = "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Class;";
                else if (call.desc.equals("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")) desc = "(Ljava/lang/String;ZLjava/lang/ClassLoader;Ljava/lang/Class;)Ljava/lang/Class;";
                else continue;
                method.instructions.insertBefore(call, new LdcInsnNode(Type.getObjectType(node.name)));
                method.instructions.set(call, new MethodInsnNode(INVOKESTATIC, HELPER, "forName", desc, false));
                method.maxStack += 1;
                count++;
            }
        }
        return count;
    }
}
