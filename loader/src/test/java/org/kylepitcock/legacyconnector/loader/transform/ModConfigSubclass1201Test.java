package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

class ModConfigSubclass1201Test {
    private static final String MOD_CONFIG = "net/neoforged/fml/config/ModConfig";
    private static final String LEGACY = "legacyconnector/v1_20_1/net/minecraftforge/fml/config/LegacyModConfig";

    @Test
    void subclassMovesAndItsUsesFollow() {
        ClassNode cfg = new ClassNode();
        cfg.version = Opcodes.V17;
        cfg.name = "mod/MyConfig";
        cfg.superName = MOD_CONFIG;
        MethodNode init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, MOD_CONFIG, "<init>", "()V", false));
        init.instructions.add(new InsnNode(Opcodes.RETURN));
        cfg.methods.add(init);

        ClassNode user = new ClassNode();
        user.version = Opcodes.V17;
        user.name = "mod/Helper";
        user.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_STATIC, "register", "(Lnet/neoforged/fml/ModContainer;Ljava/lang/Object;)Z", null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, "mod/MyConfig"));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/kylepitcock/legacyconnector/v1_20_1/migrate/RoundCompat", "addConfig",
                "(Lnet/neoforged/fml/ModContainer;L" + MOD_CONFIG + ";)V", false));
        m.instructions.add(new InsnNode(Opcodes.ICONST_1));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        user.methods.add(m);

        List<ClassNode> nodes = new ArrayList<>(List.of(cfg, user));
        assertEquals(1, ModConfigSubclass1201.apply(nodes));
        assertEquals(LEGACY, cfg.superName);
        assertEquals(LEGACY, ((MethodInsnNode) init.instructions.get(1)).owner);
        List<String> calls = new ArrayList<>();
        for (AbstractInsnNode in : m.instructions) if (in instanceof MethodInsnNode mi) calls.add(mi.owner + "." + mi.name);
        assertEquals(List.of(LEGACY + ".wrapperOf", LEGACY + ".addTo"), calls);
    }
}
