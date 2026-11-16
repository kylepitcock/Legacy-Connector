package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashSet;
import java.util.Set;

import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * ModLoader read a mod's {@code @MLProp} fields only after touching them had run the class's static initializer, so a mod that made its
 * blocks there ({@code public static Block ore = new BlockOre(oreID)}, NetherCraft) always used the default ids: its config file had no
 * effect, and two such mods with the same defaults could never be combined. The static initializer's own assignments of those fields now
 * pass through {@code MLPropsB173.early}, which answers the value the config file holds (the same value ModLoader would have set a moment
 * later), so the blocks are made with the configured ids.
 */
public final class BetaMLPropEarly implements Opcodes {
    private static final String HELPER = "org/kylepitcock/legacyconnector/vb1_7_3/MLPropsB173";

    private BetaMLPropEarly() {}

    public static int apply(ClassNode node, LegacyVersion version) {
        if (version != LegacyVersion.VB1_7_3 || node.name.startsWith("legacyconnector/") || node.name.startsWith("legacyconnector_synth/")) return 0;
        Set<String> props = new HashSet<>();
        for (FieldNode field : node.fields) {
            if ((field.access & ACC_STATIC) == 0 || field.visibleAnnotations == null) continue;
            for (AnnotationNode annotation : field.visibleAnnotations)
                if (annotation.desc.endsWith("/MLProp;") || annotation.desc.equals("LMLProp;")) props.add(field.name + ":" + field.desc);
        }
        if (props.isEmpty()) return 0;
        int count = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<clinit>")) continue;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof FieldInsnNode put) || put.getOpcode() != PUTSTATIC || !put.owner.equals(node.name)
                        || !props.contains(put.name + ":" + put.desc)) continue;
                String desc = switch (put.desc) {
                    case "I" -> "(ILjava/lang/Class;Ljava/lang/String;)I";
                    case "Z" -> "(ZLjava/lang/Class;Ljava/lang/String;)Z";
                    case "Ljava/lang/String;" -> "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;";
                    default -> null;
                };
                if (desc == null) continue;
                InsnList call = new InsnList();
                call.add(new LdcInsnNode(Type.getObjectType(node.name)));
                call.add(new LdcInsnNode(put.name));
                call.add(new MethodInsnNode(INVOKESTATIC, HELPER, "early", desc, false));
                method.instructions.insertBefore(put, call);
                count++;
            }
            if (count > 0) method.maxStack += 2;
        }
        return count;
    }
}
