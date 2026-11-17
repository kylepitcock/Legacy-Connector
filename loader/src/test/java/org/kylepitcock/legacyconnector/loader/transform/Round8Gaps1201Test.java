package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** ATM 9 gap round 8: the call rows of this batch redirect to their helpers or keep a constructor with produced arguments. */
class Round8Gaps1201Test implements Opcodes {
    private static final String MIGRATE = "org/kylepitcock/legacyconnector/v1_20_1/migrate/";

    private static MethodNode run(MethodInsnNode call) {
        ClassNode c = new ClassNode();
        c.version = V17;
        c.access = ACC_PUBLIC;
        c.name = "test/Mod";
        c.superName = "java/lang/Object";
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, "go", "()V", null, null);
        c.methods.add(m);
        m.instructions.add(call);
        m.instructions.add(new InsnNode(RETURN));
        Mc1201Hierarchy h = new Mc1201Hierarchy();
        h.add(c);
        Mc1201Migration.apply(c, h);
        return m;
    }

    private static MethodInsnNode last(MethodNode m) {
        MethodInsnNode out = null;
        for (AbstractInsnNode in : m.instructions) if (in instanceof MethodInsnNode c) out = c;
        return out;
    }

    @Test
    void blockSnapshotRestoreGoesToTheHelper() {
        MethodInsnNode call = last(run(new MethodInsnNode(INVOKEVIRTUAL, "net/neoforged/neoforge/common/util/BlockSnapshot", "restore", "(ZZ)Z", false)));
        assertNotNull(call);
        assertEquals(MIGRATE + "Round8Compat", call.owner);
        assertEquals("restoreSnapshot", call.name);
    }

    @Test
    void conditionConstructorTakesAListAndKeepsTheClass() {
        MethodInsnNode call = last(run(new MethodInsnNode(INVOKESPECIAL, "net/neoforged/neoforge/common/conditions/OrCondition", "<init>",
                "([Lnet/neoforged/neoforge/common/conditions/ICondition;)V", false)));
        assertEquals("net/neoforged/neoforge/common/conditions/OrCondition", call.owner);
        assertEquals("(Ljava/util/List;)V", call.desc);
    }

    @Test
    void armorTrimSheetAndPickupEventAccessors() {
        MethodInsnNode call = last(run(new MethodInsnNode(INVOKESTATIC, "net/minecraft/client/renderer/Sheets", "armorTrimsSheet",
                "()Lnet/minecraft/client/renderer/RenderType;", false)));
        assertEquals(MIGRATE + "Round8ClientCompat", call.owner);
        call = last(run(new MethodInsnNode(INVOKEVIRTUAL, "net/neoforged/neoforge/event/entity/player/ItemEntityPickupEvent$Post", "getStack",
                "()Lnet/minecraft/world/item/ItemStack;", false)));
        assertEquals("getOriginalStack", call.name);
    }
}
