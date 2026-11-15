package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

class CoremodAdditionsTest {
    private static ClassNode chunk() {
        ClassNode c = new ClassNode();
        c.name = "net/minecraft/world/chunk/Chunk";
        c.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "func_76588_a", "()V", null, null));
        return c;
    }

    @Test
    void recordsAddedInterfaceFieldsAndMethods() {
        ClassNode before = chunk();
        ClassNode after = chunk();
        after.interfaces.add("mod/IExtraChunkData");
        after.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "extra", "[Ljava/lang/Object;", null, null));
        after.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "getExtra", "()[Ljava/lang/Object;", null, null));
        List<CoremodHookDiscovery.Addition> added = CoremodHookDiscovery.additions(before, after);
        assertEquals(3, added.size());
        assertEquals("interface", added.get(0).kind());
        assertEquals("field\tnet/minecraft/world/chunk/Chunk\textra\t[Ljava/lang/Object;\t1", added.get(1).toString());
        assertEquals("getExtra", added.get(2).name());
    }

    @Test
    void unchangedClassHasNoAdditions() {
        assertEquals(0, CoremodHookDiscovery.additions(chunk(), chunk()).size());
    }
}
