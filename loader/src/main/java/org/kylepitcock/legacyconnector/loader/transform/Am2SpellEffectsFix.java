package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class Am2SpellEffectsFix {
    private static final String SPELL_UTILS = "am2/spell/SpellUtils";
    private static final String STAR_DAMAGE = "mcc$starDamage";

    private Am2SpellEffectsFix() {}

    static void apply(ClassNode node) {
        if (node.name.equals("am2/particles/AMParticle")) {
            for (var method : node.methods) {
                if (!method.name.equals("renderParticle")) continue;
                for (var instruction : method.instructions) {
                    // AM2's sky light is 3840. The modern shader fetches a texel from a 16x16 lightmap.
                    if (instruction instanceof LdcInsnNode constant && Integer.valueOf(0x0F0000F0).equals(constant.cst)) {
                        constant.cst = 0x00F000F0;
                    }
                }
            }
        } else if (node.name.equals("am2/spell/components/FallingStar")) {
            fixFallingStar(node);
        }
    }

    private static void fixFallingStar(ClassNode node) {
        for (var method : node.methods) {
            if (!method.name.equals("spawnStar")) continue;
            for (var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call) || !call.owner.equals(SPELL_UTILS)
                        || !call.name.equals("getModifiedInt_Mul")) continue;
                Type[] args = Type.getArgumentTypes(call.desc);
                if (args.length != 7 || !args[4].getInternalName().endsWith("/net/minecraft/world/World")) continue;
                String descriptor = "(L" + SPELL_UTILS + ";" + call.desc.substring(1);
                // The client has no SkillData for a wave's dummy caster. Damage is used only when spawning on the server.
                var helper = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, STAR_DAMAGE, descriptor, null, null);
                var server = new Label();
                helper.visitVarInsn(Opcodes.ALOAD, 5);
                helper.visitFieldInsn(Opcodes.GETFIELD, args[4].getInternalName(), "isRemote", "Z");
                helper.visitJumpInsn(Opcodes.IFEQ, server);
                helper.visitInsn(Opcodes.ICONST_0);
                helper.visitInsn(Opcodes.IRETURN);
                helper.visitLabel(server);
                helper.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                helper.visitVarInsn(Opcodes.ALOAD, 0);
                for (int i = 0; i < args.length; i++) helper.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), i + 1);
                helper.visitMethodInsn(Opcodes.INVOKEVIRTUAL, call.owner, call.name, call.desc, false);
                helper.visitInsn(Opcodes.IRETURN);
                helper.visitMaxs(8, 8);
                method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, node.name, STAR_DAMAGE, descriptor, false));
                node.methods.add(helper);
                return;
            }
        }
    }
}
